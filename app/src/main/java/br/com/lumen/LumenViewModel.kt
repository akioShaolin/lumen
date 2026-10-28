package br.com.lumen

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.lumen.dsp.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

data class Settings(val a4: Double = 440.0, val tolerance: Double = 3.0, val gate: Double = -65.0, val flats: Boolean = false, val manual: Boolean = false, val target: Double = 440.0, val preset: Int = 0, val drumContext: Int = 0, val ignoreMs: Int = 50, val persistence: Boolean = true, val enhancePeaks: Boolean = true, val fullSpectrum: Boolean = false)
data class UiState(val frame: CaptureFrame? = null, val error: String? = null, val target: Double? = null, val active: Boolean = false,
    val pitch: PresentedPitch = PresentedPitch(), val spectrum: DisplaySpectrum? = null,
    val status: ListeningStatus = ListeningStatus.LISTENING, val epoch: Int = 0, val inputFresh: Boolean = true)

class LumenViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("lumen", 0)
    private val _settings = MutableStateFlow(Settings(
        a4 = prefs.getFloat("a4",440f).toDouble(), tolerance = prefs.getFloat("tolerance",3f).toDouble(), gate = prefs.getFloat("gate",-65f).toDouble(),
        flats = prefs.getBoolean("flats",false), manual = prefs.getBoolean("manual",false), target = prefs.getFloat("target",440f).toDouble(), preset = prefs.getInt("preset",0), drumContext = prefs.getInt("drumContext",0), ignoreMs = prefs.getInt("ignoreMs",50),
        persistence = prefs.getBoolean("persistence",true), enhancePeaks = prefs.getBoolean("enhancePeaks",true), fullSpectrum = prefs.getBoolean("fullSpectrum",false)))
    val settings = _settings.asStateFlow()
    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()
    private val presentationLock = Any()
    private val pitchFilter = PitchPresentation()
    private val spectrumFilter = SpectrumDisplay()
    private val statusFilter = StatusPresentation()
    private var lastFrameAt = 0L
    private var accepting = false
    private var stream: Triple<Int, Int, String>? = null
    private val engine = CaptureEngine(app, { f -> synchronized(presentationLock) {
        if (accepting) {
            val identity = Triple(f.generation, f.rate, f.route)
            if (identity != stream) { resetPresentation(); stream = identity }
            val now = SystemClock.elapsedRealtime()
            lastFrameAt = now
            val s = _settings.value
            val pitch = pitchFilter.update(f.analysis.pitch, now, s.a4, f.analysis.clipped)
            val spectrum = spectrumFilter.update(f.analysis.spectrum, f.analysis.dbfs, s.gate, now, s.enhancePeaks)
            _state.value = UiState(f, target = pitch.target(s.a4, s.target.takeIf { s.manual }), active = true,
                pitch = pitch, spectrum = spectrum, status = statusFor(f, pitch, now), epoch = _state.value.epoch)
        }
    } }, { synchronized(presentationLock) {
        accepting = false; resetPresentation()
        _state.value = _state.value.copy(error = if (it is CaptureException) it.message else app.getString(R.string.capture_unavailable))
    } })
    init {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(50)
                synchronized(presentationLock) {
                    if (accepting) {
                        val now = SystemClock.elapsedRealtime()
                        val s = _settings.value
                        val pitch = pitchFilter.snapshot(now)
                        val stale = now - lastFrameAt > 180
                        val visual = if (stale) spectrumFilter.update(null, -120.0, s.gate, now) else _state.value.spectrum
                        _state.value = _state.value.copy(pitch = pitch, spectrum = visual,
                            inputFresh = !stale,
                            target = pitch.target(s.a4, s.target.takeIf { s.manual }),
                            status = statusFor(_state.value.frame.takeUnless { stale }, pitch, now))
                    }
                }
            }
        }
    }
    private fun statusFor(f: CaptureFrame?, pitch: PresentedPitch, now: Long) = statusFilter.update(when {
        f?.analysis?.clipped == true -> ListeningStatus.CLIPPING
        pitch.kind == ReadingKind.HELD -> ListeningStatus.HELD
        pitch.kind == ReadingKind.LIVE -> ListeningStatus.LISTENING
        f?.analysis?.pitch?.hz != null -> ListeningStatus.UNCERTAIN
        else -> ListeningStatus.WEAK
    }, now)
    private fun resetPresentation() {
        pitchFilter.reset(); spectrumFilter.reset(); statusFilter.reset(); stream = null
        _state.value = UiState(active = accepting, epoch = _state.value.epoch + 1)
    }
    fun update(s: Settings) = synchronized(presentationLock) {
        val old = _settings.value
        _settings.value = s
        prefs.edit().putFloat("a4",s.a4.toFloat()).putFloat("tolerance",s.tolerance.toFloat()).putFloat("gate",s.gate.toFloat()).putBoolean("flats",s.flats).putBoolean("manual",s.manual).putFloat("target",s.target.toFloat()).putInt("preset",s.preset).putInt("drumContext",s.drumContext).putInt("ignoreMs",s.ignoreMs).apply()
        prefs.edit().putBoolean("persistence",s.persistence).putBoolean("enhancePeaks",s.enhancePeaks).putBoolean("fullSpectrum",s.fullSpectrum).apply()
        if (old.a4 != s.a4) resetPresentation()
        _state.value = _state.value.copy(target = _state.value.pitch.target(s.a4, s.target.takeIf { s.manual }))
        engine.gateDb = s.gate; engine.drumIgnoreMs = s.ignoreMs
    }
    fun selectTarget(midi: Int) = update(_settings.value.copy(manual = true,target = Music.frequency(midi,_settings.value.a4)))
    fun mode(mode: Int) = synchronized(presentationLock) { engine.drumMode = mode == 2; resetPresentation() }
    fun clearPeaks() = synchronized(presentationLock) {
        spectrumFilter.clearHeld(SystemClock.elapsedRealtime())
        _state.value = _state.value.copy(spectrum = _state.value.spectrum?.let { it.copy(heldDb = DoubleArray(it.heldDb.size) { -60.0 }) })
    }
    fun drumPeak(hz: Double) { engine.selectedDrumHz = hz }
    fun start() {
        synchronized(presentationLock) { if (accepting) return; accepting = true; resetPresentation() }
        engine.gateDb = _settings.value.gate; engine.start()
    }
    fun stop() {
        synchronized(presentationLock) { accepting = false; resetPresentation() }
        engine.stop(); engine.awaitStopped()
    }
    override fun onCleared() { engine.stop() }
}
