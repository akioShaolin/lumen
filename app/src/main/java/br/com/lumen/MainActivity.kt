package br.com.lumen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.lumen.dsp.*
import java.util.Locale
import kotlin.math.*

private val Background = Color(0xFF090B12)
private val Surface = Color(0xFF141925)
private val Cyan = Color(0xFF35E8FF)
private val Green = Color(0xFF46F5AE)
private val Pink = Color(0xFFFF4FD8)
private val NoteColors = listOf(0xFF35E8FF,0xFF54C7FF,0xFF789BFF,0xFFA78BFA,0xFFD279FF,0xFFFF4FD8,0xFFFF6B9D,0xFFFF9671,0xFFFFD166,0xFFDBF56A,0xFF8FF59B,0xFF46F5CE).map { Color(it) }
private fun number(x: Double?, digits: Int = 1, signed: Boolean = false) = if (x == null || !x.isFinite()) "—" else String.format(Locale.forLanguageTag("pt-BR"), "%${if(signed) "+" else ""}.${digits}f",x)

class MainActivity : ComponentActivity() {
    private val model: LumenViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent {
        MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, secondary = Color(0xFFA78BFA), background = Background, surface = Surface, onSurface = Color(0xFFF4F7FF))) { LumenApp(model) }
    } }
}

@Composable private fun LumenApp(model: LumenViewModel) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val state by model.state.collectAsState()
    val settings by model.settings.collectAsState()
    var permission by remember { mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    var denied by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var mode by remember { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> permission = granted; denied = !granted; if (granted && !paused) model.start() }
    DisposableEffect(owner, paused) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permission = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (permission && !paused) model.start()
            }
            if (event == Lifecycle.Event.ON_PAUSE) model.stop()
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && permission && !paused) model.start()
        onDispose { owner.lifecycle.removeObserver(observer); model.stop() }
    }
    val analysis = state.frame?.analysis
    val hz = analysis?.pitch?.hz
    val midi = hz?.let { Music.midi(it, settings.a4).roundToInt() }
    val cents = if (hz != null && state.target != null) Music.cents(hz,state.target!!) else null
    val reliable = analysis?.pitch?.let { it.hz != null && !it.ambiguous && it.confidence >= 0.9 } == true
    val manualLabel = stringResource(R.string.manual)
    val status = when {
        paused -> R.string.paused
        analysis?.clipped == true -> R.string.clipping
        analysis == null -> R.string.listening
        hz == null -> R.string.weak
        !reliable -> R.string.uncertain
        else -> R.string.listening
    }
    Scaffold(containerColor = Background) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("L Ú M E N", fontSize = 28.sp); Text(stringResource(R.string.subtitle), color = Color(0xFFA7B0C0)) }
                TextButton(onClick = { showSettings = true }) { Text(stringResource(R.string.settings)) }
            }
            Text(stringResource(R.string.reference,number(settings.a4)))
            Row(Modifier.fillMaxWidth()) { listOf(R.string.tuner,R.string.spectrum,R.string.drums).forEachIndexed { i, label ->
                TextButton(onClick = { mode = i; model.mode(i) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { selected = mode == i }, colors = ButtonDefaults.textButtonColors(contentColor = if (mode == i) Cyan else Color(0xFFA7B0C0))) { Text(stringResource(label)) }
            } }
            if (!permission) {
                Text(stringResource(if (denied) R.string.denied else R.string.permission))
                Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) { Text(stringResource(R.string.allow)) }
                if (denied) TextButton(onClick = { context.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}"))) }) { Text(stringResource(R.string.open_settings)) }
            } else {
                TextButton(onClick = { paused = !paused; if (paused) model.stop() else model.start() }) { Text(stringResource(if (paused) R.string.resume else R.string.pause)) }
            }
            state.error?.let { Text(it, color = Pink) }
            Text(stringResource(status), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            Row(Modifier.horizontalScroll(rememberScrollState())) { repeat(12) { pc ->
                OutlinedButton(onClick = { model.selectTarget((midi?.let { Math.floorDiv(it,12)*12 } ?: 60)+pc) }, modifier = Modifier.padding(end=4.dp).heightIn(min=48.dp), contentPadding = PaddingValues(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = NoteColors[pc])) { Text(Music.name(60+pc,settings.flats).dropLast(1)) }
            } }
            if (mode != 2) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Surface)) { Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(midi?.let { Music.name(it,settings.flats) } ?: "—", fontSize = 76.sp, color = midi?.let { NoteColors[Math.floorMod(it,12)] } ?: Cyan, fontFamily = FontFamily.Monospace)
                    Text(midi?.let { Music.portuguese(it) } ?: "—", fontSize = 22.sp)
                    Text(stringResource(R.string.confidence, ((analysis?.pitch?.confidence ?: 0.0)*100).roundToInt()))
                } }
                FlowMetrics(hz,cents,analysis?.dbfs,reliable)
                Row(verticalAlignment = Alignment.CenterVertically) { Switch(settings.manual,{ model.update(settings.copy(manual=it)) },Modifier.semantics { contentDescription=manualLabel }); Text(stringResource(if (settings.manual) R.string.manual else R.string.automatic)) }
                state.target?.let { target -> Text(stringResource(R.string.target,Music.name(Music.midi(target,settings.a4).roundToInt(),settings.flats),number(target,2))) }
                if(settings.manual) Row {
                    TextButton(onClick={model.selectTarget((Music.midi(settings.target,settings.a4).roundToInt()-12).coerceAtLeast(16))}) { Text(stringResource(R.string.octave_down)) }
                    TextButton(onClick={model.selectTarget((Music.midi(settings.target,settings.a4).roundToInt()+12).coerceAtMost(95))}) { Text(stringResource(R.string.octave_up)) }
                }
                Row { listOf(R.string.chromatic,R.string.guitar,R.string.bass).forEachIndexed { i, name -> TextButton(onClick = { model.update(settings.copy(preset=i)) }) { Text(stringResource(name)) } } }
                val strings = when(settings.preset) { 1 -> listOf(40,45,50,55,59,64); 2 -> listOf(28,33,38,43); else -> emptyList() }
                if (strings.isNotEmpty()) { Text(stringResource(R.string.preset_help)); Row(Modifier.horizontalScroll(rememberScrollState())) { strings.forEach { note -> TextButton(onClick = { model.selectTarget(note) }) { Text(Music.name(note,settings.flats)) } } } }
            } else DrumPanel(state,settings,model)
            SpectrumPanel(analysis?.spectrum, hz, state.frame?.rate ?: 48000, mode == 1, settings.a4, settings.flats)
            if (mode == 0) TuningBar(cents, reliable, settings.tolerance)
            state.frame?.let { f ->
                val source = stringResource(when(f.source) { 9 -> R.string.raw; 6 -> R.string.voice; else -> R.string.mic })
                Text(stringResource(R.string.diagnostics,source,f.rate,f.route.ifEmpty { context.getString(R.string.route_unknown) },f.overruns), fontSize = 12.sp)
            }
            Text(stringResource(R.string.audio_caveat), fontSize = 12.sp)
            Text(stringResource(R.string.low_frequency), fontSize = 12.sp)
        }
    }
    if (showSettings) SettingsDialog(settings,model) { showSettings = false }
}

@Composable private fun FlowMetrics(hz: Double?, cents: Double?, db: Double?, reliable: Boolean) {
    Column(Modifier.fillMaxWidth(),verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(R.string.frequency to "${number(hz,if(reliable) 2 else 0)} Hz", R.string.deviation to "${number(cents,if(reliable) 1 else 0,true)} cents", R.string.level to "${number(db,0)} dBFS").forEach { (label,value) ->
            Row(Modifier.fillMaxWidth().background(Surface,RoundedCornerShape(12.dp)).padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween) { Text(stringResource(label)); Text(value,fontFamily=FontFamily.Monospace) }
        }
    }
}

@Composable private fun TuningBar(cents: Double?, reliable: Boolean, tolerance: Double) {
    val instruction = when { !reliable || cents == null -> R.string.no_guidance; abs(cents) <= tolerance -> R.string.tuned; cents < 0 -> R.string.tighten; else -> R.string.loosen }
    Column(Modifier.fillMaxWidth().background(Surface,RoundedCornerShape(16.dp)).padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Text(stringResource(instruction),color=if (instruction==R.string.tuned) Green else Cyan)
        Canvas(Modifier.fillMaxWidth().height(70.dp).semantics { contentDescription = "−50 … 0 … +50 cents" }) {
            val y = size.height*0.5f
            for (i in 0..20) { val x = i*size.width/20; drawLine(if(i==10) Green else Color.Gray,androidx.compose.ui.geometry.Offset(x,y-12),androidx.compose.ui.geometry.Offset(x,y+12),if(i==10) 4f else 1f) }
            if (reliable && cents != null) { val x = ((cents.coerceIn(-50.0,50.0)+50)/100*size.width).toFloat(); drawCircle(Cyan,7.dp.toPx(),androidx.compose.ui.geometry.Offset(x,y)) }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("−50"); Text("0"); Text("+50") }
        if (cents != null && abs(cents)>50) Text(stringResource(R.string.off_scale))
    }
}

@Composable private fun SpectrumPanel(incoming: Spectrum?, hz: Double?, rate: Int, expanded: Boolean, a4: Double, flats: Boolean) {
    var frozen by remember { mutableStateOf(false) }
    var held by remember { mutableStateOf(false) }
    var broad by remember { mutableStateOf(false) }
    var display by remember { mutableStateOf<Spectrum?>(null) }
    var hold by remember { mutableStateOf<DoubleArray?>(null) }
    val freezeLabel=stringResource(R.string.freeze)
    val holdLabel=stringResource(R.string.peak_hold)
    val nyquistLabel=stringResource(R.string.nyquist)
    LaunchedEffect(incoming,frozen,held) { if (!frozen) {
        display = incoming
        hold = incoming?.amplitudes?.let { a -> DoubleArray(a.size) { i -> if(held) max(a[i],(hold?.getOrNull(i) ?: 0.0)*0.93) else a[i] } }
    } }
    Text(stringResource(R.string.spectrum_label))
    Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(frozen,{ frozen=it },Modifier.semantics {contentDescription=freezeLabel}); Text(stringResource(if(frozen) R.string.unfreeze else R.string.freeze)) }
    if (expanded) {
        Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(held,{held=it},Modifier.semantics {contentDescription=holdLabel}); Text(stringResource(R.string.peak_hold)) }
        Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(broad,{broad=it},Modifier.semantics {contentDescription=nyquistLabel}); Text(stringResource(R.string.nyquist)) }
    }
    val description = stringResource(R.string.spectrum_accessibility,display?.peaks?.size ?: 0)
    Canvas(Modifier.fillMaxWidth().height(if(expanded) 280.dp else 140.dp).background(Surface,RoundedCornerShape(12.dp)).semantics { contentDescription=description }) {
        val spectrum=display ?: return@Canvas
        val amplitudes=hold ?: spectrum.amplitudes
        val max=amplitudes.maxOrNull()?.coerceAtLeast(1e-12) ?: 1e-12
        val upper=if(broad) rate/2.0 else min(5000.0,rate/2.0)
        fun x(f: Double) = (ln(f/20)/ln(upper/20)*size.width).toFloat()
        val path=Path()
        var started=false
        for(i in 1 until amplitudes.size) {
            val f=i*spectrum.binHz
            if(f<20 || f>upper) continue
            val y=(size.height*(-20*log10((amplitudes[i]/max).coerceAtLeast(1e-6))).coerceIn(0.0,80.0)/80).toFloat()
            if(!started) {path.moveTo(x(f),y);started=true} else path.lineTo(x(f),y)
        }
        drawPath(path,Cyan,style=androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        if(hz!=null && hz in 20.0..upper && !frozen) drawLine(Pink,androidx.compose.ui.geometry.Offset(x(hz),0f),androidx.compose.ui.geometry.Offset(x(hz),size.height),2f)
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("20 Hz"); Text(if(broad) "${rate/2} Hz" else "5 kHz") }
    if(hz!=null && !frozen) Text(stringResource(R.string.pitch_marker,number(hz,1)),color=Pink)
    display?.peaks?.take(if(expanded) 12 else 3)?.forEach { p -> Text("${number(p.hz,1)} Hz · ${Music.name(Music.midi(p.hz,a4).roundToInt(),flats)} · ${number(p.relativeDb,0)} dB") }
}

@Composable private fun DrumPanel(state: UiState, s: Settings, model: LumenViewModel) {
    Text(stringResource(R.string.drum_experimental),color=Color(0xFFFFBC66))
    Text(stringResource(R.string.drum_help))
    Row { listOf(R.string.skin,R.string.global).forEachIndexed { i,r -> TextButton(onClick={model.update(s.copy(drumContext=i))}) { Text((if(s.drumContext==i) "● " else "")+stringResource(r)) } } }
    Text(stringResource(R.string.onset,s.ignoreMs))
    Slider(s.ignoreMs.toFloat(),{model.update(s.copy(ignoreMs=it.roundToInt()))},valueRange=20f..150f)
    val result=state.frame?.drum
    if(result==null) Text(stringResource(R.string.drum_wait)) else {
        if(result.lowConfidence) Text(stringResource(R.string.drum_low))
        result.previousHz?.let { Text(stringResource(R.string.drum_previous,number(it,1))) }
        result.peaks.forEach { peak -> TextButton(onClick={model.drumPeak(peak.hz);model.update(s.copy(manual=true,target=peak.hz))}) { Text(stringResource(R.string.drum_peak,number(peak.hz,1))) } }
        if(s.manual) Text(stringResource(R.string.target,Music.name(Music.midi(s.target,s.a4).roundToInt()),number(s.target,1)))
    }
}

@Composable private fun SettingsDialog(s: Settings, model: LumenViewModel, close: () -> Unit) {
    var a4 by remember { mutableStateOf(number(s.a4)) }
    var target by remember { mutableStateOf(number(s.target,2)) }
    var error by remember { mutableStateOf(false) }
    val flatsLabel=stringResource(R.string.flats)
    val toleranceLabel=stringResource(R.string.tolerance,number(s.tolerance))
    val gateLabel=stringResource(R.string.sensitivity,number(s.gate,0))
    AlertDialog(onDismissRequest=close,title={Text(stringResource(R.string.settings))},text={ Column(Modifier.verticalScroll(rememberScrollState())) {
        OutlinedTextField(a4,{a4=it},label={Text(stringResource(R.string.a4))})
        Row { TextButton(onClick={a4=number(((a4.replace(',','.').toDoubleOrNull() ?: s.a4)-0.1).coerceIn(400.0,480.0))}) {Text("−0,1")}; TextButton(onClick={a4=number(((a4.replace(',','.').toDoubleOrNull() ?: s.a4)+0.1).coerceIn(400.0,480.0))}) {Text("+0,1")} }
        OutlinedTextField(target,{target=it},label={Text(stringResource(R.string.frequency_target))})
        Text(stringResource(R.string.tolerance,number(s.tolerance)))
        Slider(s.tolerance.toFloat(),{model.update(s.copy(tolerance=it.toDouble()))},modifier=Modifier.semantics {contentDescription=toleranceLabel},valueRange=1f..10f)
        Text(stringResource(R.string.sensitivity,number(s.gate,0)))
        Slider(s.gate.toFloat(),{model.update(s.copy(gate=it.toDouble()))},modifier=Modifier.semantics {contentDescription=gateLabel},valueRange=-85f..-35f)
        Row(verticalAlignment=Alignment.CenterVertically) {Switch(s.flats,{model.update(s.copy(flats=it))},Modifier.semantics {contentDescription=flatsLabel});Text(stringResource(R.string.flats))}
        if(error) Text(stringResource(R.string.invalid_number),color=Pink)
    } },confirmButton={ TextButton(onClick={
        val a=a4.replace(',','.').toDoubleOrNull();val t=target.replace(',','.').toDoubleOrNull()
        if(a!=null && a in 400.0..480.0 && t!=null && t in 20.0..2000.0) {model.update(s.copy(a4=(a*10).roundToInt()/10.0,target=t,manual=s.manual || abs(t-s.target)>0.001));close()} else error=true
    }) {Text(stringResource(R.string.apply))} },dismissButton={TextButton(onClick=close){Text(stringResource(R.string.close))}})
}
