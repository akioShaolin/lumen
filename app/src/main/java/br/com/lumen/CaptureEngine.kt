package br.com.lumen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.media.audiofx.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import br.com.lumen.dsp.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

data class CaptureFrame(val analysis: Analysis, val drum: DrumResult?, val rate: Int, val source: Int, val route: String, val overruns: Int, val generation: Int = 0)
class CaptureException(message: String) : IllegalStateException(message)

/** Two workers with a bounded, recycled block pool. stop() unblocks AudioRecord.read. */
class CaptureEngine(private val context: Context, private val frame: (CaptureFrame) -> Unit, private val failure: (Throwable) -> Unit) {
    private val running = AtomicBoolean(false)
    private var recorder: AudioRecord? = null
    private var captureThread: Thread? = null
    private var analysisThread: Thread? = null
    private val ready = ArrayBlockingQueue<DoubleArray>(8)
    private val free = ArrayBlockingQueue<DoubleArray>(10)
    @Volatile var gateDb = -65.0
    @Volatile var drumMode = false
    @Volatile var drumIgnoreMs = 50
    @Volatile var selectedDrumHz: Double? = null
    @Volatile var overruns = 0
    private val routeGeneration = java.util.concurrent.atomic.AtomicInteger(0)

    @Synchronized fun start() {
        if (!running.compareAndSet(false, true)) return
        routeGeneration.incrementAndGet()
        ready.clear(); free.clear(); repeat(10) { free.offer(DoubleArray(1024)) }; overruns = 0
        captureThread = Thread({
            var record: AudioRecord? = null
            val effects = ArrayList<AudioEffect>()
            val manager = context.getSystemService(AudioManager::class.java)
            var callback: AudioManager.AudioRecordingCallback? = null
            try {
                if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    throw CaptureException(context.getString(R.string.permission))
                }
                val sources = if (manager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true") listOf(MediaRecorder.AudioSource.UNPROCESSED, MediaRecorder.AudioSource.VOICE_RECOGNITION, MediaRecorder.AudioSource.MIC) else listOf(MediaRecorder.AudioSource.VOICE_RECOGNITION, MediaRecorder.AudioSource.MIC)
                var source = 0
                search@ for (rate in listOf(48000,44100)) for (s in sources) {
                    val minimum = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                    if (minimum <= 0) continue
                    val candidate = try {
                        AudioRecord(s, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum*2,8192))
                    } catch (_: SecurityException) {
                        // Permission may be revoked between the check and recorder creation.
                        throw CaptureException(context.getString(R.string.permission))
                    } catch (_: RuntimeException) { continue }
                    if (candidate.state == AudioRecord.STATE_INITIALIZED) { record = candidate; source = s; break@search }
                    candidate.release()
                }
                val active = record ?: throw CaptureException(context.getString(R.string.capture_unavailable))
                synchronized(this) { if (!running.get()) return@Thread; recorder = active }
                active.addOnRoutingChangedListener(AudioRouting.OnRoutingChangedListener { routeGeneration.incrementAndGet() }, Handler(Looper.getMainLooper()))
                if (Build.VERSION.SDK_INT >= 29) {
                    callback = object : AudioManager.AudioRecordingCallback() {
                        override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>) {
                            if (running.get() && configs.any { it.clientAudioSessionId == active.audioSessionId && it.isClientSilenced }) {
                                failure(CaptureException(context.getString(R.string.mic_silenced))); stop()
                            }
                        }
                    }
                    manager.registerAudioRecordingCallback(callback, Handler(Looper.getMainLooper()))
                }
                fun disable(effect: AudioEffect?) { if (effect != null) { effects.add(effect); try { effect.enabled = false } catch (_: RuntimeException) { } } }
                try {
                    if (AutomaticGainControl.isAvailable()) disable(AutomaticGainControl.create(active.audioSessionId))
                    if (NoiseSuppressor.isAvailable()) disable(NoiseSuppressor.create(active.audioSessionId))
                    if (AcousticEchoCanceler.isAvailable()) disable(AcousticEchoCanceler.create(active.audioSessionId))
                } catch (_: RuntimeException) { /* Manufacturer can reject optional effects. */ }
                val actualRate = active.sampleRate
                val pcm = ShortArray(1024)
                analysisThread = Thread({ analyze(actualRate, source) }, "Lumen-analise").also { it.start() }
                active.startRecording()
                while (running.get()) {
                    val count = active.read(pcm, 0, pcm.size, AudioRecord.READ_BLOCKING)
                    if (count < 0) throw CaptureException(context.getString(R.string.capture_error, count))
                    if (count == 0) continue
                    val block = free.poll()
                    if (block == null) { overruns++; continue }
                    // Reads are not guaranteed to fill a block. Accumulate without inserting silence.
                    for (i in 0 until count) block[i] = pcm[i]/32768.0
                    if (count == pcm.size) { if (!ready.offer(block)) { overruns++; free.offer(block) } }
                    else {
                        var filled = count
                        while (filled < pcm.size && running.get()) {
                            val more = active.read(pcm, 0, pcm.size-filled, AudioRecord.READ_BLOCKING)
                            if (more <= 0) throw CaptureException(context.getString(R.string.capture_error, more))
                            for (i in 0 until more) block[filled+i] = pcm[i]/32768.0
                            filled += more
                        }
                        if (filled == block.size && !ready.offer(block)) { overruns++; free.offer(block) }
                    }
                }
            } catch (e: Throwable) { if (running.get()) failure(e) }
            finally {
                running.set(false)
                analysisThread?.interrupt()
                callback?.let { manager.unregisterAudioRecordingCallback(it) }
                effects.forEach { it.release() }
                try { record?.stop() } catch (_: RuntimeException) { }
                record?.release()
                synchronized(this) { recorder = null }
            }
        }, "Lumen-captura").also { it.start() }
    }

    private fun analyze(rate: Int, source: Int) {
        val ring = DoubleArray(32768)
        val short = DoubleArray(8192)
        val long = DoubleArray(32768)
        val normal = PitchDetector(8192); val bass = PitchDetector(32768)
        val drums = DrumAnalyzer()
        val stability = PitchStability()
        var write = 0; var total = 0; var hops = 0; var lastDrumMode = false
        var route = routeGeneration.get()
        var losses = overruns
        try {
            while (running.get()) {
                val block = ready.take()
                if (route != routeGeneration.get() || losses != overruns) {
                    route = routeGeneration.get(); losses = overruns; write = 0; total = 0; drums.reset(); stability.reset()
                }
                val level = 20*kotlin.math.log10(kotlin.math.sqrt(block.sumOf { it*it }/block.size).coerceAtLeast(1e-6))
                if (drumMode != lastDrumMode) { drums.reset(); lastDrumMode = drumMode }
                drums.ignoreMs = drumIgnoreMs; drums.selectedHz = selectedDrumHz
                val drum = if (drumMode) drums.accept(block, rate, level) else null
                for (v in block) { ring[write] = v; write = (write+1)%ring.size }
                total = minOf(ring.size,total+block.size); free.offer(block); hops++
                if (total < 8192 || hops%4 != 0) continue
                for (i in short.indices) short[i] = ring[(write-short.size+i+ring.size)%ring.size]
                var result = normal.analyze(short, rate, gateDb)
                if (total >= ring.size && (result.pitch.hz == null || result.pitch.hz!! < 70 || drumMode)) {
                    for (i in long.indices) long[i] = ring[(write+i)%ring.size]
                    result = bass.analyze(long, rate, gateDb)
                }
                // Latest block gating clears a previous tone before the analysis window drains.
                if (level < gateDb) result = result.copy(pitch = Pitch(null,0.0,false), dbfs = level)
                result = result.copy(pitch = stability.validate(result.pitch))
                val routeName = recorder?.routedDevice?.productName?.toString().orEmpty()
                frame(CaptureFrame(result, drum, rate, source, routeName, overruns, route))
            }
        } catch (_: InterruptedException) { } catch (e: Throwable) { if (running.get()) { failure(e); stop() } }
    }

    @Synchronized fun stop() {
        running.set(false)
        try { recorder?.stop() } catch (_: RuntimeException) { }
        analysisThread?.interrupt()
    }
    fun awaitStopped() { captureThread?.join(1500); analysisThread?.join(1500) }
}
