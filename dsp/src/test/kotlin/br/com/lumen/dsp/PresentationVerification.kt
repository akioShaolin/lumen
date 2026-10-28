package br.com.lumen.dsp

import kotlin.math.*

fun verifyPresentation(): Int {
    var checks = 0
    fun test(ok: Boolean, message: String) { checks++; check(ok) { message } }
    val live = Pitch(440.0,0.99,false)
    val filter = PitchPresentation()
    test(filter.update(live,0).kind == ReadingKind.EMPTY,"Initial note must accumulate evidence")
    for(t in 50L..300L step 50) filter.update(live,t)
    test(filter.snapshot(300).midi == 69,"300 ms accepts first note")
    for(t in 350L..1000L step 50) {
        val hz = 440 * 2.0.pow((if(t%100L == 0L) 49 else 54)/1200.0)
        test(filter.update(Pitch(hz,0.99,false),t).midi == 69,"Semitone boundary flicker")
    }
    val next = Pitch(Music.frequency(71),0.99,false)
    filter.update(next,1050)
    for(t in 1100L..1300L step 50) test(filter.update(next,t).midi == 69,"Premature note replacement")
    val changed = filter.update(next,1350)
    test(changed.midi == 71 && abs(changed.hz!!-next.hz!!)<1e-9,"Confirmed change must initialize without intermediate pitch")
    test(abs(changed.cents(440.0,null)!!)<1e-8,"Automatic target and frequency coherent")
    test(abs(changed.cents(440.0,440.0)!!-200)<1e-8,"Manual target stays independent")
    test(filter.update(Pitch(null,0.0,false),1400).kind == ReadingKind.HELD,"Dropout must be visibly held")
    test(filter.snapshot(1850).kind == ReadingKind.HELD,"500 ms retention")
    test(filter.snapshot(1851).hz == null,"Prolonged silence must clear")
    test(filter.snapshot(1851).referenceMidi == 71,"Silence must not recenter musical axis")
    val stale = PitchPresentation()
    for(t in 0L..300L step 50) stale.update(live,t)
    test(stale.snapshot(481).kind == ReadingKind.HELD,"Stalled input cannot remain live")
    test(stale.update(live.copy(ambiguous=true),500).kind == ReadingKind.HELD,"Low confidence disables live guidance immediately")
    test(stale.update(live,550,clipped=true).kind == ReadingKind.HELD,"Clipping disables live guidance")
    val scattered = PitchPresentation()
    for(t in 0L..1500L step 50) {
        val p = when((t/50)%3) { 0L -> live; 1L -> next; else -> Pitch(null,0.0,false) }
        test(scattered.update(p,t).kind == ReadingKind.EMPTY,"Scattered candidates cannot accumulate")
    }
    val gaps = PitchPresentation()
    gaps.update(live,0)
    test(gaps.update(live,1000).kind == ReadingKind.EMPTY,"A gap is not continuous evidence")
    for(f in listOf(41.2034,82.4069)) {
        val octave = PitchPresentation()
        for(t in 0L..300L step 50) octave.update(Pitch(f,0.99,false),t)
        test(abs(Music.cents(octave.snapshot(300).hz!!,f))<1e-8,"Presentation must not double or halve E1/E2")
    }
    val octaveChange = PitchPresentation()
    for(t in 0L..300L step 50) octaveChange.update(Pitch(82.4069,0.99,false),t)
    for(t in 350L..600L step 50) octaveChange.update(Pitch(41.2034,0.99,false),t)
    test(octaveChange.snapshot(600).kind == ReadingKind.HELD,"Octave candidate must not get live guidance prematurely")
    val realBass = octaveChange.update(Pitch(41.2034,0.99,false),650)
    test(realBass.midi==28 && abs(Music.cents(realBass.hz!!,41.2034))<1e-8,"Confirmed E2 to E1 transition preserves real E1")
    test(octaveChange.update(Pitch(Double.NaN,0.99,false),700).kind==ReadingKind.HELD,"Nonfinite pitch invalidates live reading")
    val smoothing = PitchPresentation()
    for(t in 0L..300L step 50) smoothing.update(live,t)
    val detuned = 440 * 2.0.pow(20.0/1200)
    val result = smoothing.update(Pitch(detuned,0.99,false),440)
    test(abs(Music.cents(result.hz!!,440.0)-20*(1-exp(-1.0)))<1e-8,"140 ms log-frequency filter")
    test(abs(result.cents(440.0,440.0)!!-Music.cents(result.hz,440.0))<1e-8,"Cents must use displayed frequency")
    val status = StatusPresentation()
    status.update(ListeningStatus.UNCERTAIN,0)
    test(status.update(ListeningStatus.LISTENING,100)==ListeningStatus.LISTENING,"Debounce ordinary statuses")
    test(status.update(ListeningStatus.CLIPPING,101)==ListeningStatus.CLIPPING,"Critical clipping warning immediate")
    status.update(ListeningStatus.WEAK,150)
    test(status.update(ListeningStatus.WEAK,330)==ListeningStatus.WEAK,"Persistent status accepted")

    val bins = DoubleArray(4097) { 1e-8 }.also { it[75]=0.5; it[150]=0.015 }
    val measured = Spectrum(bins,emptyList(),48000.0/8192)
    val display = SpectrumDisplay()
    val visual = display.update(measured,-9.0,-65.0,0)!!
    test(visual.liveDb[150] in -33.0..-29.0,"Weak stable peak preserved beside strong peak")
    test(visual.liveDb[74] <= -59,"Broad noise carpet suppressed")
    val sinePair = DoubleArray(8192) { i -> 0.5*sin(2*PI*75*i/8192) + 0.015*sin(2*PI*150*i/8192) }
    val measuredPair = SpectrumAnalyzer(8192).analyze(sinePair,48000)
    val visiblePair = SpectrumDisplay().update(measuredPair,-9.0,-65.0,0)!!
    test(visiblePair.liveDb[150] in -33.0..-29.0,"Measured weak sinusoid remains visible alongside strong sinusoid")
    val quiet = SpectrumDisplay().update(Spectrum(DoubleArray(4097){1e-5},emptyList(),5.0),-80.0,-65.0,0)!!
    test(quiet.liveDb.all { it == -60.0 },"Quiet noise must not normalize to full scale")
    val ambient = SpectrumDisplay().update(Spectrum(DoubleArray(4097){1e-4},emptyList(),5.0),-60.0,-65.0,0)!!
    test(ambient.liveDb.maxOrNull()!! < -30,"Audible broad noise must not become 0 dB peak")
    fun decay(step: Long): DisplaySpectrum {
        val f = SpectrumDisplay(); f.update(measured,-9.0,-65.0,0)
        var v: DisplaySpectrum? = null
        for(t in step..1500L step step) v = f.update(null,-120.0,-65.0,t)
        return v!!
    }
    val fast = decay(20); val slow = decay(100)
    test(abs(fast.heldDb[75]+8)<1e-8,"1000 ms hold then 16 dB/s")
    test(abs(fast.heldDb[75]-slow.heldDb[75])<1e-8,"Hold decay independent of frame rate")
    test(abs(fast.liveDb[75]-slow.liveDb[75])<1e-8,"Live release independent of frame rate")
    val axis = SpectrumAxis.musical(100.0,24000.0)
    test(abs(axis.upperHz/100-2.0.pow(21.0/12))<1e-10,"13th means 21 semitones, not harmonic 13")
    val before = visual.heldDb.copyOf()
    peakPixels(visual.heldDb,visual.binHz,axis,200)
    peakPixels(visual.heldDb,visual.binHz,SpectrumAxis.musical(200.0,24000.0),200)
    test(visual.heldDb.contentEquals(before),"Reference changes must not reinterpret absolute-frequency history")
    for(reference in listOf(200.0,300.0)) {
        val projectedAxis = SpectrumAxis.musical(reference,24000.0)
        val projected = peakPixels(visual.heldDb,visual.binHz,projectedAxis,200)
        val maximumPixel = projected.indices.filter { projected[it].isFinite() }.maxBy { projected[it] }
        val expectedPixel = (projectedAxis.position(75*visual.binHz)*199).roundToInt()
        test(abs(maximumPixel-expectedPixel)<=1,"Historical peak must project at its original Hz after reference change")
    }
    val narrow = peakPixels(visual.liveDb,visual.binHz,SpectrumAxis.full(24000.0),30)
    test(narrow.any { it > -1.0 },"Pixel aggregation must preserve narrow maximum")
    display.clearHeld(10)
    test(display.update(null,-120.0,-65.0,10)!!.heldDb.all { it == -60.0 },"Clear held peaks")
    val silentWindow = display.update(Spectrum(DoubleArray(1025),emptyList(),10.0),-120.0,-65.0,15)!!
    test(silentWindow.liveDb.size == bins.size,"Silent FFT window change must allow old trace to decay")
    val resized = display.update(Spectrum(DoubleArray(1025),emptyList(),10.0),-20.0,-65.0,20)!!
    test(resized.heldDb.size==1025 && resized.heldDb.all { it == -60.0 },"FFT/sample rate changes reset history")
    val bad = SpectrumDisplay().update(Spectrum(doubleArrayOf(Double.NaN,Double.POSITIVE_INFINITY,-1.0,0.0),emptyList(),5.0),-20.0,-65.0,0)!!
    test(bad.liveDb.all { it.isFinite() },"Nonfinite amplitudes safely floored")
    println("Apresentação/espectro: $checks verificações passaram")
    return checks
}
