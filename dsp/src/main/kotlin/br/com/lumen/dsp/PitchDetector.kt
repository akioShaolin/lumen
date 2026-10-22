package br.com.lumen.dsp

import kotlin.math.*

data class Pitch(val hz: Double?, val confidence: Double, val ambiguous: Boolean)
data class Analysis(val pitch: Pitch, val spectrum: Spectrum, val dbfs: Double, val clipped: Boolean)

/** MPM NSDF using zero-padded FFT autocorrelation, O(N log N). */
class PitchDetector(private val size: Int) {
    private val re = DoubleArray(size*2)
    private val im = DoubleArray(size*2)
    private val energy = DoubleArray(size+1)
    private val nsdf = DoubleArray(size/2+1)
    private val spectrum = SpectrumAnalyzer(size)
    private var noiseFloor = -85.0
    fun analyze(x: DoubleArray, rate: Int, gateDb: Double = -65.0): Analysis {
        require(x.size == size)
        val mean = x.average()
        var sum = 0.0; var clipping = false
        re.fill(0.0); im.fill(0.0); energy[0] = 0.0
        for (i in x.indices) {
            val v = x[i]-mean; re[i] = v; sum += v*v; energy[i+1] = sum
            if (abs(x[i]) >= 0.999) clipping = true
        }
        val db = 20*log10(sqrt(x.sumOf { it*it }/size).coerceAtLeast(1e-6))
        val spec = spectrum.analyze(x, rate)
        if (db < noiseFloor + 6) noiseFloor = 0.98*noiseFloor + 0.02*db
        val threshold = max(gateDb, noiseFloor+10)
        if (db < threshold || sum/size < 1e-10) return Analysis(Pitch(null, 0.0, false), spec, db, clipping)
        Fft.transform(re, im)
        for (i in re.indices) { re[i] = re[i]*re[i]+im[i]*im[i]; im[i] = 0.0 }
        Fft.transform(re, im, true)
        val minLag = max(2, rate/2000)
        val maxLag = min(size/2-1, ceil(rate/20.0).toInt())
        for (lag in 0..maxLag+1) {
            val denom = energy[size-lag] + energy[size]-energy[lag]
            nsdf[lag] = if (denom > 1e-12) 2*re[lag]/denom else 0.0
        }
        val candidates = (minLag..maxLag).filter { nsdf[it] > nsdf[it-1] && nsdf[it] >= nsdf[it+1] && nsdf[it] > 0.65 }
        val best = candidates.maxOfOrNull { nsdf[it] } ?: return Analysis(Pitch(null, 0.0, true), spec, db, clipping)
        // Earliest near-best periodic peak avoids doubled-period E2→E1 errors.
        var lag = candidates.firstOrNull { nsdf[it] >= max(0.80, best*0.97) }
            ?: return Analysis(Pitch(null, best, true), spec, db, clipping)
        fun refine(t: Int): Double {
            val a = nsdf[t-1]; val b = nsdf[t]; val c = nsdf[t+1]
            return rate / (t + (0.5*(a-c)/(a-2*b+c)).coerceIn(-0.5,0.5))
        }
        var f = refine(lag)
        fun support(hz: Double): Double = spec.peaks.filter { abs(Music.cents(it.hz, hz)) < max(25.0, 1800*spec.binHz/hz) }.maxOfOrNull { it.amplitude } ?: 0.0
        // A half-frequency candidate needs measured odd harmonics, never even-only support.
        val doubled = candidates.minByOrNull { abs(it-2*lag) }
        if (doubled != null && abs(doubled-2*lag) < 4 && nsdf[doubled] > nsdf[lag]+0.02) {
            val lower = refine(doubled)
            val odd = max(support(lower), support(3*lower))
            val even = support(2*lower)
            if (odd > even*0.08) { lag = doubled; f = lower }
        }
        val significant = spec.peaks.filter { it.relativeDb > -24 && it.hz < min(rate/2.0, 8000.0) }
        val unexplained = significant.sumOf { p ->
            val harmonic = (p.hz/f).roundToInt().coerceAtLeast(1)
            if (abs(p.hz-harmonic*f) > max(spec.binHz*1.6, f*0.025)) p.amplitude*p.amplitude else 0.0
        }
        val total = significant.sumOf { it.amplitude*it.amplitude }.coerceAtLeast(1e-12)
        val missing = support(f) < (significant.maxOfOrNull { it.amplitude } ?: 0.0)*0.04
        val ambiguous = unexplained/total > 0.13 || missing || nsdf[lag] < 0.9 || clipping
        return Analysis(Pitch(f.takeIf { it in 19.8..2000.0 }, nsdf[lag].coerceIn(0.0,1.0)*(if (ambiguous) 0.65 else 1.0), ambiguous), spec, db, clipping)
    }
}
