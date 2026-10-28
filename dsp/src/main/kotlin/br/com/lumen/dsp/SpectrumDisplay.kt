package br.com.lumen.dsp

import kotlin.math.*

data class SpectrumTiming(
    val holdMs: Long = 1000, val decayDbPerSecond: Double = 16.0,
    val attackMs: Double = 35.0, val releaseMs: Double = 280.0,
    val referenceReleaseMs: Double = 1000.0, val floorDb: Double = -60.0
)
data class DisplaySpectrum(val liveDb: DoubleArray, val heldDb: DoubleArray, val binHz: Double)

/** Absolute-frequency storage: changing a musical reference never relabels bins. */
class SpectrumDisplay(private val timing: SpectrumTiming = SpectrumTiming()) {
    private var live = DoubleArray(0)
    private var held = DoubleArray(0)
    private var holdUntil = LongArray(0)
    private var binHz = 0.0
    private var referenceDb = -30.0
    private var lastTime: Long? = null
    private var previousSignal = false
    fun reset() { live = DoubleArray(0); held = DoubleArray(0); holdUntil = LongArray(0); binHz = 0.0; referenceDb = -30.0; lastTime = null; previousSignal = false }
    fun clearHeld(now: Long) { held.fill(timing.floorDb); holdUntil.fill(now) }
    fun update(spectrum: Spectrum?, rmsDb: Double, gateDb: Double, now: Long, enhance: Boolean = true): DisplaySpectrum? {
        if (spectrum != null && (live.isEmpty() || (rmsDb.isFinite() && rmsDb >= gateDb)) &&
            (spectrum.binHz != binHz || live.size != spectrum.amplitudes.size)) {
            reset(); binHz = spectrum.binHz
            live = DoubleArray(spectrum.amplitudes.size) { timing.floorDb }
            held = live.copyOf(); holdUntil = LongArray(live.size) { now }
        }
        if (live.isEmpty()) return null
        val previousTime = lastTime ?: now
        val dt = (now - previousTime).coerceAtLeast(0).toDouble()
        lastTime = now
        val signal = spectrum != null && rmsDb.isFinite() && rmsDb >= gateDb && binHz.isFinite() && binHz > 0
        val raw = spectrum?.amplitudes
        fun amplitudeDb(a: Double) = if (a.isFinite() && a > 0) 20 * log10(a.coerceAtLeast(1e-12)) else -240.0
        if (signal) {
            val maxDb = raw!!.maxOfOrNull { amplitudeDb(it) } ?: -240.0
            // A new louder sound attacks immediately; release cannot raise quiet frames during silence.
            referenceDb = if (maxDb > referenceDb) maxDb else
                (referenceDb + (1 - exp(-dt / timing.referenceReleaseMs)) * (maxDb - referenceDb)).coerceAtLeast(gateDb + 12)
        }
        // Robust median estimates the broad noise carpet, not whether individual peaks are musical.
        val noiseDb = if (signal && enhance) {
            val count = minOf(512,raw!!.size)
            val sorted = DoubleArray(count) { index -> raw[index * raw.size / count].takeIf { it.isFinite() && it >= 0 } ?: 0.0 }.sortedArray()
            amplitudeDb(sorted.getOrElse(sorted.size / 2) { 0.0 })
        } else -240.0
        for (i in live.indices) {
            val absolute = if (signal) amplitudeDb(raw!![i]) else -240.0
            val attenuation = if (enhance) ((noiseDb + 9 - absolute) * 0.75).coerceIn(0.0, 18.0) else 0.0
            val desired = if (signal) (absolute - referenceDb - attenuation).coerceIn(timing.floorDb, 0.0) else timing.floorDb
            val tau = if (desired > live[i]) timing.attackMs else timing.releaseMs
            live[i] = if (lastTime == previousTime && !previousSignal && signal) desired
                else live[i] + (1 - exp(-dt / tau)) * (desired - live[i])
            val decayStart = max(previousTime, holdUntil[i])
            held[i] = (held[i] - timing.decayDbPerSecond * (now - decayStart).coerceAtLeast(0) / 1000).coerceAtLeast(timing.floorDb)
            // Only an actual signal can acquire/refresh history. A decaying trace cannot hold itself.
            if (signal && desired >= held[i]) { held[i] = desired; holdUntil[i] = now + timing.holdMs }
        }
        previousSignal = signal
        return DisplaySpectrum(live.copyOf(), held.copyOf(), binHz)
    }
}

data class SpectrumAxis(val lowerHz: Double, val upperHz: Double) {
    fun position(hz: Double) = ln(hz / lowerHz) / ln(upperHz / lowerHz)
    companion object {
        fun musical(referenceHz: Double, nyquist: Double) = SpectrumAxis(
            max(1.0, referenceHz * 2.0.pow(-1.0 / 12)), min(nyquist, referenceHz * 2.0.pow(21.0 / 12)))
        fun full(nyquist: Double) = SpectrumAxis(20.0, nyquist)
    }
}

/** Max aggregation preserves narrow peaks when many FFT bins share a screen pixel. */
fun peakPixels(db: DoubleArray, binHz: Double, axis: SpectrumAxis, width: Int): DoubleArray {
    val pixels = DoubleArray(width.coerceAtLeast(1)) { Double.NaN }
    if (axis.upperHz <= axis.lowerHz || binHz <= 0) return pixels
    for (i in 1 until db.size) {
        val f = i * binHz
        if (f < axis.lowerHz || f > axis.upperHz || !db[i].isFinite()) continue
        val x = (axis.position(f) * (pixels.size - 1)).roundToInt().coerceIn(pixels.indices)
        pixels[x] = max(pixels[x].takeIf { it.isFinite() } ?: -60.0, db[i].coerceIn(-60.0, 0.0))
    }
    return pixels
}
