package br.com.lumen.dsp

import kotlin.math.abs

/** Frequency is never fabricated or averaged. Sudden octave jumps need a second frame for guidance. */
class PitchStability {
    private var previous: Double? = null
    fun reset() { previous = null }
    fun validate(pitch: Pitch): Pitch {
        val current = pitch.hz
        if (current == null) { reset(); return pitch }
        val old = previous
        previous = current
        return if (old != null && abs(Music.cents(current,old)) > 600) pitch.copy(ambiguous=true,confidence=minOf(pitch.confidence,0.65)) else pitch
    }
}
