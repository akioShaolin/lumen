package br.com.lumen.dsp

import kotlin.math.*

data class PresentationTiming(
    val notePersistenceMs: Long = 300,
    val smoothingMs: Double = 140.0,
    val dropoutMs: Long = 500,
    val staleMs: Long = 180,
    val boundaryCents: Double = 68.0,
    val minConfidence: Double = 0.9,
    val maxEvidenceGapMs: Long = 180
)
enum class ReadingKind { LIVE, HELD, EMPTY }
data class PresentedPitch(
    val hz: Double? = null, val midi: Int? = null, val referenceMidi: Int? = null,
    val confidence: Double = 0.0, val kind: ReadingKind = ReadingKind.EMPTY
) {
    fun target(a4: Double, manual: Double?) = manual ?: referenceMidi?.let { Music.frequency(it, a4) }
    fun cents(a4: Double, manual: Double?) = target(a4, manual)?.let { t -> hz?.let { Music.cents(it, t) } }
}

/** Display-only filter. Never changes PCM, raw DSP estimates or octave candidates. */
class PitchPresentation(private val timing: PresentationTiming = PresentationTiming()) {
    private var accepted: Int? = null
    private var logHz: Double? = null
    private var candidate: Int? = null
    private var candidateSince = 0L
    private var lastFrame: Long? = null
    private var lastGood: Long? = null
    private var confidence = 0.0
    private var live = false
    fun reset() {
        accepted = null; logHz = null; candidate = null; lastFrame = null; lastGood = null; live = false
    }
    fun update(pitch: Pitch, now: Long, a4: Double = 440.0, clipped: Boolean = false): PresentedPitch {
        val previousFrame = lastFrame
        lastFrame = now
        val hz = pitch.hz
        val valid = hz != null && hz.isFinite() && hz > 0 && !pitch.ambiguous &&
            pitch.confidence.isFinite() && pitch.confidence >= timing.minConfidence && !clipped
        if (!valid) { candidate = null; live = false; return snapshot(now) }
        val measuredMidi = Music.midi(hz!!, a4)
        val note = measuredMidi.roundToInt()
        if (accepted == null || abs((measuredMidi - accepted!!) * 100) > timing.boundaryCents) {
            if (candidate != note || previousFrame == null || now - previousFrame > timing.maxEvidenceGapMs) {
                candidate = note; candidateSince = now
            }
            live = false
            if (now - candidateSince >= timing.notePersistenceMs) {
                accepted = note; logHz = ln(hz); candidate = null; lastGood = now
                confidence = pitch.confidence; live = true
            }
        } else {
            candidate = null
            val dt = (now - (lastGood ?: now)).coerceAtLeast(0).toDouble()
            // Reacquisition after a gap initializes directly, rather than sweeping from stale sound.
            logHz = if (logHz == null || dt > timing.dropoutMs) ln(hz)
                else logHz!! + (1 - exp(-dt / timing.smoothingMs)) * (ln(hz) - logHz!!)
            confidence = pitch.confidence; lastGood = now; live = true
        }
        return snapshot(now)
    }
    fun snapshot(now: Long): PresentedPitch {
        val age = lastGood?.let { now - it } ?: Long.MAX_VALUE
        if (age > timing.dropoutMs || logHz == null) return PresentedPitch(referenceMidi = accepted)
        return PresentedPitch(exp(logHz!!), accepted, accepted, confidence,
            if (live && age <= timing.staleMs) ReadingKind.LIVE else ReadingKind.HELD)
    }
}

enum class ListeningStatus { LISTENING, WEAK, UNCERTAIN, HELD, CLIPPING }
class StatusPresentation(private val debounceMs: Long = 180) {
    private var shown = ListeningStatus.LISTENING
    private var pending = shown
    private var since = 0L
    fun reset() { shown = ListeningStatus.LISTENING; pending = shown; since = 0 }
    fun update(status: ListeningStatus, now: Long): ListeningStatus {
        if (status == ListeningStatus.CLIPPING) { shown = status; pending = status; since = now }
        else if (status != pending) { pending = status; since = now }
        else if (now - since >= debounceMs) shown = status
        return shown
    }
}
