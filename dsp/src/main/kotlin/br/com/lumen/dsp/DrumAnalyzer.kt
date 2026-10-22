package br.com.lumen.dsp

import kotlin.math.*

data class DrumResult(val peaks: List<Peak>, val previousHz: Double?, val lowConfidence: Boolean)

/** Strike state machine: onset → ignore → collect decay → hold. No harmonic f0 assumption. */
class DrumAnalyzer {
    private var elapsed = -1
    private var previousLevel = -120.0
    private var lastHz: Double? = null
    private val decay = DoubleArray(32768)
    private var written = 0
    private val spectrum = SpectrumAnalyzer(decay.size)
    var result: DrumResult? = null
        private set
    var ignoreMs = 50
    var selectedHz: Double? = null
    fun reset() { elapsed = -1; written = 0; previousLevel = -120.0; result = null }
    fun accept(block: DoubleArray, rate: Int, level: Double): DrumResult? {
        if (elapsed < 0 && level > -48 && level - previousLevel > 9) { elapsed = 0; written = 0 }
        previousLevel = level
        if (elapsed >= 0) {
            val skip = rate*ignoreMs/1000
            for (sample in block) {
                if (elapsed++ >= skip && written < decay.size) decay[written++] = sample
            }
            if (written == decay.size) {
                val measured = spectrum.analyze(decay, rate).peaks.filter { it.hz in 20.0..2500.0 }
                val first = decay.copyOfRange(0,16384)
                val second = decay.copyOfRange(16384,32768)
                val a = SpectrumAnalyzer(16384).analyze(first, rate).peaks
                val b = SpectrumAnalyzer(16384).analyze(second, rate).peaks
                val stable = measured.filter { p -> a.any { abs(it.hz-p.hz) < 2*rate/16384.0 } && b.any { abs(it.hz-p.hz) < 2*rate/16384.0 } }
                val chosen = selectedHz?.let { target -> stable.minByOrNull { abs(it.hz-target) }?.takeIf { abs(it.hz-target)/target < 0.12 } }
                result = DrumResult(stable.take(8), lastHz, stable.isEmpty() || (selectedHz != null && chosen == null) || (chosen?.hz ?: stable.firstOrNull()?.hz ?: 0.0) < 40)
                // Preserve intended resonance across hits; never chase the loudest mode once selected.
                if (chosen != null) lastHz = chosen.hz
                elapsed = -1
            }
        }
        return result
    }
}
