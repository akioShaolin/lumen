package br.com.lumen.dsp

import kotlin.math.*
import kotlin.random.Random

private var checks = 0
private fun verify(condition: Boolean, message: String) { checks++; check(condition) { message } }
private fun tone(n: Int, rate: Int, hz: Double, harmonics: List<Double> = listOf(0.6), phase: Double = 0.37): DoubleArray = DoubleArray(n) { i -> harmonics.mapIndexed { h,a -> a*sin(2*PI*hz*(h+1)*i/rate+phase) }.sum() }

fun main() {
    checks += verifyPresentation()
    var worst = 0.0; var octaveErrors = 0; var tones = 0
    val started = System.nanoTime()
    for(rate in listOf(44100,48000)) {
        val detector = PitchDetector(32768)
        for(f in listOf(20.0,30.0,40.0,41.2034,55.0,82.4069,110.0,261.6256,440.0,443.0,1000.0)) {
            val result=detector.analyze(tone(32768,rate,f),rate)
            val actual=result.pitch.hz ?: error("Senoide $f Hz não detectada em $rate")
            val error=abs(Music.cents(actual,f)); worst=max(worst,error); tones++
            if(error>600) octaveErrors++
            verify(error <= if(f<40) 5.0 else 2.0,"Erro $error cents em $f/$rate")
            verify(!result.pitch.ambiguous,"Senoide limpa ambígua $f/$rate")
        }
        for(f in listOf(41.2034,82.4069)) for(h in listOf(listOf(0.15,0.45,0.25,0.08), listOf(0.05,0.5,0.3,0.08),listOf(0.0,0.45,0.3,0.1))) {
            val p=detector.analyze(tone(32768,rate,f,h),rate).pitch
            val e=abs(Music.cents(p.hz ?: error("Sem pitch harmônico"),f))
            verify(e<2,"Otava/harmônicos $f/$rate: ${p.hz}, erro $e")
            if(h[0]==0.0) verify(p.ambiguous,"Fundamental ausente deve sinalizar ambiguidade")
        }
        val even=detector.analyze(tone(32768,rate,41.2034,listOf(0.0,0.5,0.0,0.1)),rate)
        verify(abs(Music.cents(even.pitch.hz!!,82.4068))<2,"Suporte apenas par não pode inventar E1")
        val silence=detector.analyze(DoubleArray(32768),rate)
        verify(silence.pitch.hz==null && silence.dbfs == -120.0,"Silêncio e piso finito")
        verify(detector.analyze(DoubleArray(32768){0.2},rate).pitch.hz==null,"DC não é nota")
        val random=Random(2048)
        val noise=detector.analyze(DoubleArray(32768){random.nextDouble(-0.1,0.1)},rate)
        verify(noise.pitch.hz==null || noise.pitch.ambiguous,"Ruído não pode orientar afinação")
        val clip=detector.analyze(DoubleArray(32768){if(it%100<50) 1.0 else -1.0},rate)
        verify(clip.clipped && clip.pitch.ambiguous,"Saturação deve bloquear instruções")
        val chord=DoubleArray(32768){i -> listOf(261.6256,329.6276,391.9954).sumOf {f->0.2*sin(2*PI*f*i/rate)} }
        val chordResult=detector.analyze(chord,rate)
        for(f in listOf(261.6256,329.6276,391.9954)) verify(chordResult.spectrum.peaks.any{abs(it.hz-f)<chordResult.spectrum.binHz},"Pico do acorde ausente: $f")
        verify(chordResult.pitch.hz==null || chordResult.pitch.ambiguous,"Acorde não pode orientar tarraxa")
        val bin=120; val f=bin*rate/32768.0
        val normalized=SpectrumAnalyzer(32768).analyze(tone(32768,rate,f,listOf(0.5)),rate)
        verify(abs(normalized.amplitudes[bin]-0.5)<1e-5,"Ganho coerente Hann")
        verify(abs(detector.analyze(tone(32768,rate,f,listOf(0.5)),rate).dbfs-20*log10(0.5/sqrt(2.0)))<0.02,"RMS não é pico")
        val vib=DoubleArray(32768){i -> val t=i.toDouble()/rate;0.5*sin(2*PI*110*t+0.02*sin(2*PI*5*t))}
        val v=detector.analyze(vib,rate).pitch
        verify(v.hz!=null && abs(Music.cents(v.hz,110.0))<5,"Vibrato pequeno")
        val pluck=tone(32768,rate,82.4069,listOf(0.3,0.2,0.12))
        for(i in pluck.indices) pluck[i]*=exp(-i.toDouble()/rate/0.7)
        verify(abs(Music.cents(detector.analyze(pluck,rate).pitch.hz!!,82.4069))<2,"Envelope pinçado")
        detector.analyze(tone(32768,rate,82.4069),rate)
        verify(abs(Music.cents(detector.analyze(tone(32768,rate,110.0),rate).pitch.hz!!,110.0))<2,"Troca de nota sem prender resultado")
        verify(detector.analyze(DoubleArray(32768),rate).pitch.hz==null,"Limpar resultado antigo")
        val drum=DrumAnalyzer()
        drum.accept(DoubleArray(1024),rate,-120.0)
        var result: DrumResult?=null
        for(block in 0..40) {
            val samples=DoubleArray(1024){i -> val t=(block*1024+i).toDouble()/rate; exp(-t/0.5)*(0.4*sin(2*PI*100*t)+0.3*sin(2*PI*159.3*t)+0.2*sin(2*PI*213.5*t))}
            result=drum.accept(samples,rate,20*log10(sqrt(samples.sumOf{it*it}/1024))) ?: result
        }
        verify(result!=null,"Golpe deve produzir resultado")
        for(resonance in listOf(100.0,159.3,213.5)) verify(result!!.peaks.any{abs(it.hz-resonance)<2},"Ressonância $resonance ausente")
    }
    verify(Music.name(69)=="A4" && Music.name(28)=="E1" && Music.name(40)=="E2","Oitavas científicas")
    verify(abs(Music.frequency(69,442.0)-442)<1e-10,"Lá4 configurável")
    verify(Music.cents(439.0,440.0)<0 && Music.cents(441.0,440.0)>0,"Sinais de cents")
    val tracker=TargetTracker()
    verify(tracker.select(440.0,440.0)==69 && tracker.select(Music.frequency(69)*2.0.pow(0.55/12),440.0)==69,"Histerese")
    val stability=PitchStability()
    stability.validate(Pitch(82.4069,1.0,false))
    verify(stability.validate(Pitch(41.2034,1.0,false)).ambiguous,"Mudança abrupta de oitava requer confirmação")
    verify(!stability.validate(Pitch(41.2034,1.0,false)).ambiguous,"E1 verdadeiro deve estabilizar sem dobrar medição")
    stability.validate(Pitch(null,0.0,false))
    verify(!stability.validate(Pitch(440.0,1.0,false)).ambiguous,"Silêncio limpa histórico temporal")
    val rng=Random(4096)
    val stress=PitchDetector(8192)
    for(index in 0 until 30) {
        val f=rng.nextDouble(70.0,900.0)
        val samples=tone(8192,48000,f,listOf(0.3,0.2,0.1),rng.nextDouble(0.0,2*PI))
        for(i in samples.indices) samples[i]+=rng.nextDouble(-0.01,0.01)
        val p=stress.analyze(samples,48000).pitch
        verify(p.hz!=null && abs(Music.cents(p.hz,f))<2,"Janela curta com ruído/phase em $f Hz")
    }
    // Low periodicity must return uncertainty, never throw on an empty threshold selection.
    repeat(20) {
        val f=rng.nextDouble(40.0,700.0)
        val samples=tone(8192,48000,f,listOf(0.1))
        for(i in samples.indices) samples[i]+=rng.nextDouble(-0.3,0.3)
        val p=stress.analyze(samples,48000).pitch
        verify(p.hz==null || p.ambiguous,"Baixa periodicidade não pode produzir instrução confiável")
    }
    val elapsed=(System.nanoTime()-started)/1e6
    println("PASSOU: $checks verificações; $tones senoides; pior erro limpo: %.5f cents; erros de oitava: $octaveErrors; duração host: %.1f ms".format(worst,elapsed))
    val bench=PitchDetector(32768); val samples=tone(32768,48000,41.2034)
    repeat(10){bench.analyze(samples,48000)}
    val t=System.nanoTime(); repeat(50){bench.analyze(samples,48000)}
    println("Benchmark host JVM: %.2f ms/quadro de 32768; não representa CPU Android.".format((System.nanoTime()-t)/1e6/50))
}
