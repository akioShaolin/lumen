package br.com.lumen.dsp

import kotlin.math.*

object Music {
    fun frequency(midi: Int, a4: Double = 440.0) = a4 * 2.0.pow((midi - 69) / 12.0)
    fun midi(f: Double, a4: Double = 440.0) = 69 + 12 * log2(f / a4)
    fun cents(f: Double, target: Double) = 1200 * log2(f / target)
    fun name(midi: Int, flats: Boolean = false): String {
        val names = if (flats) arrayOf("C", "D♭", "D", "E♭", "E", "F", "G♭", "G", "A♭", "A", "B♭", "B") else arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
        return names[Math.floorMod(midi, 12)] + (Math.floorDiv(midi, 12) - 1)
    }
    fun portuguese(midi: Int) = arrayOf("Dó", "Dó♯", "Ré", "Ré♯", "Mi", "Fá", "Fá♯", "Sol", "Sol♯", "Lá", "Lá♯", "Si")[Math.floorMod(midi, 12)]
}

class TargetTracker {
    private var target: Int? = null
    fun clear() { target = null }
    fun select(frequency: Double, a4: Double): Int {
        val m = Music.midi(frequency, a4)
        val old = target
        if (old == null || abs(m - old) > 0.58) target = m.roundToInt()
        return target!!
    }
}
