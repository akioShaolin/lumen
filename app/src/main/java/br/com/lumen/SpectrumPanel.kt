package br.com.lumen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import br.com.lumen.dsp.*
import kotlin.math.*

internal data class SpectrumScene(val visual: DisplaySpectrum?, val measured: Spectrum?, val reference: Double?,
    val hz: Double?, val rate: Int, val full: Boolean, val a4: Double, val flats: Boolean)

@Composable internal fun SpectrumPanel(state: UiState, settings: Settings, mode: Int, model: LumenViewModel) {
    var frozen by remember { mutableStateOf<SpectrumScene?>(null) }
    var options by remember { mutableStateOf(false) }
    val current = SpectrumScene(state.spectrum, state.frame?.analysis?.spectrum.takeIf { state.inputFresh }, state.target,
        state.pitch.hz.takeIf { state.pitch.kind == ReadingKind.LIVE }, state.frame?.rate ?: 48000,
        settings.fullSpectrum || mode == 2, settings.a4, settings.flats)
    val scene = frozen ?: current
    Text(stringResource(R.string.spectrum_label))
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        TextButton(onClick = { frozen = if (frozen == null) current else null }) { Text(stringResource(if(frozen == null) R.string.freeze else R.string.unfreeze)) }
        TextButton(onClick = { options = !options }) { Text(stringResource(R.string.spectrum_options)) }
    }
    if (options || mode == 1) {
        SpectrumToggle(settings.persistence, R.string.persistence) { model.update(settings.copy(persistence = it)) }
        SpectrumToggle(settings.enhancePeaks, R.string.enhance_peaks) { model.update(settings.copy(enhancePeaks = it)) }
        if (mode != 2) SpectrumToggle(settings.fullSpectrum, R.string.full_spectrum) { frozen = null; model.update(settings.copy(fullSpectrum = it)) }
        TextButton(onClick = { frozen = null; model.clearPeaks() }) { Text(stringResource(R.string.clear_peaks)) }
        if (mode != 2) Text(stringResource(R.string.lock_reference_help))
    }
    val ref = scene.reference
    StableText(if(scene.full) stringResource(R.string.full_axis) else ref?.let {
        stringResource(R.string.musical_axis, number(it,2), number(it * 2.0.pow(21.0/12),2))
    } ?: stringResource(R.string.reference_wait), listOf(stringResource(R.string.musical_axis,"2000,00","6727,17"), stringResource(R.string.reference_wait)))
    SpectrumPlot(scene, settings.persistence, mode == 1)
    StableText(stringResource(if(frozen != null) R.string.trace_frozen else R.string.trace_legend),
        listOf(stringResource(R.string.trace_frozen),stringResource(R.string.trace_legend)))
    StableText(scene.hz?.let { stringResource(R.string.pitch_marker,number(it,2)) } ?: stringResource(R.string.pitch_marker_unavailable),
        listOf(stringResource(R.string.pitch_marker,"2000,00")))
    Text(stringResource(R.string.measured_peaks))
    // Constant row count: incoming FFT frames cannot move the tuning bar.
    repeat(if(mode == 1) 12 else 3) { index ->
        val peak = scene.measured?.peaks?.getOrNull(index)
        val text = peak?.let { "${number(it.hz,1)} Hz · ${Music.name(Music.midi(it.hz,scene.a4).roundToInt(),scene.flats)} · ${number(it.relativeDb,0)} dB" } ?: "— Hz · — · — dB"
        StableText(text,listOf("20000,0 Hz · C♯10 · −120 dB", "— Hz · — · — dB"))
    }
}

@Composable private fun SpectrumToggle(value: Boolean, label: Int, change: (Boolean) -> Unit) {
    val text = stringResource(label)
    Row { Checkbox(value,change,Modifier.semantics { contentDescription = text }); Text(text,Modifier.padding(top = 12.dp).weight(1f)) }
}

@Composable internal fun SpectrumPlot(scene: SpectrumScene, persistence: Boolean, expanded: Boolean) {
    val description = stringResource(R.string.spectrum_accessibility,scene.measured?.peaks?.size ?: 0)
    val axis = if(scene.full) SpectrumAxis.full(scene.rate/2.0) else scene.reference?.let { SpectrumAxis.musical(it,scene.rate/2.0) }
    val measurer = rememberTextMeasurer()
    val labelStyle = LocalTextStyle.current
    val labelWidth = with(LocalDensity.current) { measurer.measure(AnnotatedString("−60"),labelStyle).size.width.toDp() + 8.dp }
    Row(Modifier.fillMaxWidth().testTag("spectrum-plot").height(if(expanded) 280.dp else 160.dp).background(Color(0xFF141925),RoundedCornerShape(12.dp))) {
        Column(Modifier.width(labelWidth).fillMaxHeight().padding(4.dp),verticalArrangement = Arrangement.SpaceBetween) { Text("0"); Text("−30"); Text("−60") }
        Canvas(Modifier.weight(1f).fillMaxHeight().semantics { contentDescription = description }) {
            for(db in listOf(0,-30,-60)) { val y = -db/60f*size.height; drawLine(Color(0xFFA7B0C0).copy(alpha=0.2f),Offset(0f,y),Offset(size.width,y)) }
            val visual = scene.visual
            if(visual != null && axis != null) {
                fun trace(values: DoubleArray, color: Color, stroke: Float) {
                    val pixels = peakPixels(values,visual.binHz,axis,size.width.roundToInt())
                    val path = Path(); var started = false
                    for(i in pixels.indices) {
                        if(!pixels[i].isFinite()) continue
                        val y = (-pixels[i]/60*size.height).toFloat()
                        if(!started) { path.moveTo(i.toFloat(),y); started = true } else path.lineTo(i.toFloat(),y)
                    }
                    drawPath(path,color,style = Stroke(stroke))
                }
                if(persistence) trace(visual.heldDb,Color(0xFFA78BFA).copy(alpha=0.6f),1.dp.toPx())
                trace(visual.liveDb,Color(0xFF35E8FF),1.5.dp.toPx())
                scene.hz?.let { hz -> if(hz in axis.lowerHz..axis.upperHz) {
                    val x = (axis.position(hz)*size.width).toFloat()
                    drawLine(Color(0xFFFF4FD8),Offset(x,0f),Offset(x,size.height),1.dp.toPx())
                } }
            }
        }
    }
    val labels = if(scene.full) listOf(20.0 to "20 Hz",100.0 to "100",1000.0 to "1k",scene.rate/2.0 to "${scene.rate/2} Hz")
        else listOf(0 to "1",4 to "3M",7 to "5J",12 to "8J",16 to "10M",21 to "13M").map { (step,label) -> (scene.reference ?: 440.0)*2.0.pow(step/12.0) to label }
    AxisLabels(labels,axis ?: SpectrumAxis.musical(440.0,scene.rate/2.0),Modifier.padding(start=labelWidth))
}

@Composable private fun AxisLabels(labels: List<Pair<Double,String>>, axis: SpectrumAxis, modifier: Modifier) {
    Layout(content = { labels.forEach { Text(it.second,softWrap=false) } },modifier=modifier.fillMaxWidth()) { measurables,constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth=0,minHeight=0)) }
        layout(constraints.maxWidth,placeables.maxOfOrNull { it.height } ?: 0) {
            var rightEdge = -8
            val lastWidth = placeables.lastOrNull()?.width ?: 0
            placeables.forEachIndexed { index,p ->
                val x = (axis.position(labels[index].first)*constraints.maxWidth-p.width/2).roundToInt().coerceIn(0,(constraints.maxWidth-p.width).coerceAtLeast(0))
                val last = index == placeables.lastIndex
                if(last || (x >= rightEdge + 8 && x+p.width+8 <= constraints.maxWidth-lastWidth)) {
                    p.placeRelative(x,0); rightEdge = x+p.width
                }
            }
        }
    }
}
