package br.com.lumen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.lumen.dsp.*
import kotlin.math.abs

/** Reserves the tallest supported message at the actual width, font scale and text style. */
@Composable internal fun StableText(text: String, variants: List<String>, modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current, color: Color = Color.Unspecified,
    align: TextAlign = TextAlign.Start) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val height = remember(variants, text, style, width, density.fontScale) {
            (variants + text).maxOf { measurer.measure(AnnotatedString(it.ifEmpty { " " }), style,
                constraints = Constraints(maxWidth = width)).size.height }
        }
        Text(text.ifEmpty { " " }, style = style, color = color, textAlign = align,
            modifier = Modifier.fillMaxWidth().heightIn(min = with(density) { height.toDp() }))
    }
}

@Composable internal fun StatusRegion(state: UiState, paused: Boolean) {
    var details by remember { mutableStateOf(false) }
    val statuses = listOf(R.string.listening, R.string.weak, R.string.uncertain, R.string.clipping,
        R.string.held, R.string.paused, R.string.capture_short)
    val status = when {
        state.error != null -> R.string.capture_short
        paused -> R.string.paused
        state.pitch.kind == ReadingKind.HELD && state.frame?.analysis?.clipped != true -> R.string.held
        else -> when (state.status) {
            ListeningStatus.LISTENING -> R.string.listening
            ListeningStatus.WEAK -> R.string.weak
            ListeningStatus.UNCERTAIN -> R.string.uncertain
            ListeningStatus.HELD -> R.string.held
            ListeningStatus.CLIPPING -> R.string.clipping
        }
    }
    Column(Modifier.testTag("status-region")) {
        StableText(stringResource(status), statuses.map { stringResource(it) },
            Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        TextButton(onClick = { details = true }) { Text(stringResource(R.string.signal_help)) }
    }
    if (details) AlertDialog(onDismissRequest = { details = false }, title = { Text(stringResource(R.string.signal_help)) },
        text = { Text(state.error ?: stringResource(R.string.signal_explanation)) },
        confirmButton = { TextButton(onClick = { details = false }) { Text(stringResource(R.string.close)) } })
}

@Composable internal fun NoteReadout(pitch: PresentedPitch, flats: Boolean) {
    val midi = pitch.midi
    val color by animateColorAsState(midi?.let { NotePalette[Math.floorMod(it,12)] } ?: Color(0xFF35E8FF), tween(120), label = "note-color")
    val opacity by animateFloatAsState(when(pitch.kind) { ReadingKind.LIVE -> 1f; ReadingKind.HELD -> 0.62f; ReadingKind.EMPTY -> 0.35f }, tween(120), label = "reading-opacity")
    Card(Modifier.fillMaxWidth().testTag("note-card"), colors = CardDefaults.cardColors(containerColor = Color(0xFF141925))) {
        Column(Modifier.fillMaxWidth().padding(20.dp).alpha(opacity), horizontalAlignment = Alignment.CenterHorizontally) {
            StableText(midi?.let { Music.name(it,flats) } ?: "—", listOf("C♯10", "D♭10", "—"),
                style = LocalTextStyle.current.copy(fontSize = 76.sp, lineHeight = 88.sp, fontFamily = FontFamily.Monospace), color = color, align = TextAlign.Center)
            StableText(midi?.let { Music.portuguese(it) } ?: "—", listOf("Sol♯", "—"),
                style = LocalTextStyle.current.copy(fontSize = 22.sp, lineHeight = 28.sp), align = TextAlign.Center)
            StableText(stringResource(R.string.confidence, (pitch.confidence * 100).toInt()),
                listOf(stringResource(R.string.confidence,100)), align = TextAlign.Center)
            StableText(stringResource(when(pitch.kind) { ReadingKind.LIVE -> R.string.reading_live; ReadingKind.HELD -> R.string.held; ReadingKind.EMPTY -> R.string.reading_empty }),
                listOf(stringResource(R.string.reading_live), stringResource(R.string.held), stringResource(R.string.reading_empty)), align = TextAlign.Center)
        }
    }
}

private val NotePalette = listOf(0xFF35E8FF,0xFF54C7FF,0xFF789BFF,0xFFA78BFA,0xFFD279FF,0xFFFF4FD8,0xFFFF6B9D,0xFFFF9671,0xFFFFD166,0xFFDBF56A,0xFF8FF59B,0xFF46F5CE).map { Color(it) }

@Composable internal fun FlowMetrics(hz: Double?, cents: Double?, db: Double?, kind: ReadingKind) {
    Column(Modifier.fillMaxWidth().testTag("metrics"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Precision is stable; the retained/live label conveys confidence, not changing digit counts.
        val values = listOf(number(hz,2) + " Hz", number(cents,1,true) + " cents", number(db,0) + " dBFS")
        val labels = listOf(R.string.frequency, R.string.deviation, R.string.level)
        val samples = listOf("2000,00 Hz", "−99999,9 cents", "−120 dBFS")
        values.forEachIndexed { index, value ->
            Row(Modifier.fillMaxWidth().background(Color(0xFF141925),RoundedCornerShape(12.dp)).padding(12.dp)) {
                StableText(stringResource(labels[index]), listOf(stringResource(labels[index])),Modifier.weight(0.4f))
                StableText(value, listOf(samples[index],"—"), Modifier.weight(0.6f),style = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontFeatureSettings = "tnum"), align = TextAlign.End,
                    color = if (index < 2 && kind == ReadingKind.HELD) Color(0xFFA7B0C0) else Color.Unspecified)
            }
        }
    }
}

@Composable internal fun TuningBar(cents: Double?, reliable: Boolean, tolerance: Double) {
    val instruction = when { !reliable || cents == null -> R.string.no_guidance; abs(cents) <= tolerance -> R.string.tuned; cents < 0 -> R.string.tighten; else -> R.string.loosen }
    Column(Modifier.fillMaxWidth().testTag("tuning-bar").background(Color(0xFF141925),RoundedCornerShape(16.dp)).padding(16.dp)) {
        StableText(stringResource(instruction), listOf(R.string.no_guidance,R.string.tuned,R.string.tighten,R.string.loosen).map { stringResource(it) },
            color = if(instruction == R.string.tuned) Color(0xFF46F5AE) else Color(0xFF35E8FF), align = TextAlign.Center)
        Canvas(Modifier.fillMaxWidth().height(70.dp).semantics { contentDescription = "−50 … 0 … +50 cents" }) {
            val y = size.height * 0.5f
            for(i in 0..20) { val x = i * size.width / 20; drawLine(if(i==10) Color(0xFF46F5AE) else Color.Gray, androidx.compose.ui.geometry.Offset(x,y-12), androidx.compose.ui.geometry.Offset(x,y+12), if(i==10) 4f else 1f) }
            if(reliable && cents != null) { val x = ((cents.coerceIn(-50.0,50.0)+50)/100*size.width).toFloat(); drawCircle(Color(0xFF35E8FF),7.dp.toPx(),androidx.compose.ui.geometry.Offset(x,y)) }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("−50"); Text("0"); Text("+50") }
        StableText(if(cents != null && abs(cents)>50) stringResource(R.string.off_scale) else " ", listOf(stringResource(R.string.off_scale)), align = TextAlign.Center)
    }
}
