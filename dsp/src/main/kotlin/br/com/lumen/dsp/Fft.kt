package br.com.lumen.dsp

import kotlin.math.*

/** Radix-2 complex FFT; inverse divides by N. Workspace is caller-owned. */
object Fft {
    fun transform(re: DoubleArray, im: DoubleArray, inverse: Boolean = false) {
        val n = re.size
        require(n > 0 && n and (n - 1) == 0 && im.size == n)
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) { val r = re[i]; re[i] = re[j]; re[j] = r; val v = im[i]; im[i] = im[j]; im[j] = v }
        }
        var len = 2
        while (len <= n) {
            val angle = (if (inverse) 2 else -2) * PI / len
            val wr = cos(angle); val wi = sin(angle)
            for (base in 0 until n step len) {
                var ur = 1.0; var ui = 0.0
                for (k in 0 until len / 2) {
                    val a = base + k; val b = a + len / 2
                    val vr = re[b] * ur - im[b] * ui; val vi = re[b] * ui + im[b] * ur
                    re[b] = re[a] - vr; im[b] = im[a] - vi; re[a] += vr; im[a] += vi
                    val next = ur * wr - ui * wi; ui = ur * wi + ui * wr; ur = next
                }
            }
            len = len shl 1
        }
        if (inverse) for (i in re.indices) { re[i] /= n; im[i] /= n }
    }
}

data class Peak(val hz: Double, val amplitude: Double, val relativeDb: Double)
data class Spectrum(val amplitudes: DoubleArray, val peaks: List<Peak>, val binHz: Double)

class SpectrumAnalyzer(private val size: Int) {
    private val re = DoubleArray(size)
    private val im = DoubleArray(size)
    private val window = DoubleArray(size) { 0.5 - 0.5 * cos(2 * PI * it / size) }
    private val gain = window.sum()
    fun analyze(samples: DoubleArray, rate: Int): Spectrum {
        require(samples.size == size)
        val mean = samples.average()
        for (i in re.indices) { re[i] = (samples[i] - mean) * window[i]; im[i] = 0.0 }
        Fft.transform(re, im)
        val a = DoubleArray(size / 2 + 1) { hypot(re[it], im[it]) * (if (it == 0 || it == size / 2) 1 else 2) / gain }
        val maxA = a.maxOrNull()!!.coerceAtLeast(1e-12)
        val peaks = ArrayList<Peak>()
        for (k in 1 until a.lastIndex) if (a[k] > a[k-1] && a[k] > a[k+1] && a[k] > maxA * 0.025) {
            val l = ln(a[k-1].coerceAtLeast(1e-15)); val c = ln(a[k].coerceAtLeast(1e-15)); val r = ln(a[k+1].coerceAtLeast(1e-15))
            val d = (0.5 * (l-r) / (l-2*c+r)).coerceIn(-0.5, 0.5)
            val hz = (k+d)*rate/size
            if (hz >= 20) peaks.add(Peak(hz, a[k], 20*log10(a[k]/maxA)))
        }
        return Spectrum(a, peaks.sortedByDescending { it.amplitude }.take(32), rate.toDouble()/size)
    }
}
