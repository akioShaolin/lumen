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
import androidx.compose.ui.platform.LocalDensity
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
internal fun number(x: Double?, digits: Int = 1, signed: Boolean = false) = if (x == null || !x.isFinite()) "—" else String.format(Locale.forLanguageTag("pt-BR"), "%${if(signed) "+" else ""}.${digits}f",x)

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
    val hz = state.pitch.hz
    val midi = state.pitch.midi
    val cents = state.pitch.cents(settings.a4, settings.target.takeIf { settings.manual })
    val reliable = state.pitch.kind == ReadingKind.LIVE && state.error == null
    val manualLabel = stringResource(R.string.manual)
    Scaffold(containerColor = Background) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LumenHeader { showSettings = true }
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
            StatusRegion(state, paused)
            Row(Modifier.horizontalScroll(rememberScrollState())) { repeat(12) { pc ->
                OutlinedButton(onClick = { model.selectTarget((midi?.let { Math.floorDiv(it,12)*12 } ?: 60)+pc) }, modifier = Modifier.padding(end=4.dp).heightIn(min=48.dp), contentPadding = PaddingValues(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = NoteColors[pc])) { Text(Music.name(60+pc,settings.flats).dropLast(1)) }
            } }
            if (mode != 2) {
                NoteReadout(state.pitch, settings.flats)
                FlowMetrics(hz,cents,analysis?.dbfs.takeIf { state.inputFresh },state.pitch.kind)
                Row(verticalAlignment = Alignment.CenterVertically) { Switch(settings.manual,{ model.update(settings.copy(manual=it)) },Modifier.semantics { contentDescription=manualLabel }); Text(stringResource(if (settings.manual) R.string.manual else R.string.automatic)) }
                StableText(state.target?.let { target -> stringResource(R.string.target,Music.name(Music.midi(target,settings.a4).roundToInt(),settings.flats),number(target,2)) } ?: stringResource(R.string.target_unavailable),
                    listOf(stringResource(R.string.target,"C♯10","2000,00")))
                if(settings.manual) Row {
                    TextButton(onClick={model.selectTarget((Music.midi(settings.target,settings.a4).roundToInt()-12).coerceAtLeast(16))}) { Text(stringResource(R.string.octave_down)) }
                    TextButton(onClick={model.selectTarget((Music.midi(settings.target,settings.a4).roundToInt()+12).coerceAtMost(95))}) { Text(stringResource(R.string.octave_up)) }
                }
                Row(Modifier.horizontalScroll(rememberScrollState())) { listOf(R.string.chromatic,R.string.guitar,R.string.bass).forEachIndexed { i, name -> TextButton(onClick = { model.update(settings.copy(preset=i)) }) { Text(stringResource(name)) } } }
                val strings = when(settings.preset) { 1 -> listOf(40,45,50,55,59,64); 2 -> listOf(28,33,38,43); else -> emptyList() }
                if (strings.isNotEmpty()) { Text(stringResource(R.string.preset_help)); Row(Modifier.horizontalScroll(rememberScrollState())) { strings.forEach { note -> TextButton(onClick = { model.selectTarget(note) }) { Text(Music.name(note,settings.flats)) } } } }
            } else DrumPanel(state,settings,model)
            key(state.epoch, mode) { SpectrumPanel(state,settings,mode,model) }
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

@Composable internal fun LumenHeader(openSettings: () -> Unit) {
    val largeFont = LocalDensity.current.fontScale > 1.3f
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (largeFont || maxWidth < 300.dp) {
            Column {
                Text("L Ú M E N", fontSize = 28.sp)
                Text(stringResource(R.string.subtitle),color = Color(0xFFA7B0C0))
                TextButton(onClick = openSettings) { Text(stringResource(R.string.settings)) }
            }
        } else Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("L Ú M E N",fontSize = 28.sp); Text(stringResource(R.string.subtitle),color = Color(0xFFA7B0C0)) }
            TextButton(onClick = openSettings) { Text(stringResource(R.string.settings)) }
        }
    }
}

@Composable private fun DrumPanel(state: UiState, s: Settings, model: LumenViewModel) {
    Text(stringResource(R.string.drum_experimental),color=Color(0xFFFFBC66))
    Text(stringResource(R.string.drum_help))
    Row(Modifier.horizontalScroll(rememberScrollState())) { listOf(R.string.skin,R.string.global).forEachIndexed { i,r -> TextButton(onClick={model.update(s.copy(drumContext=i))}) { Text((if(s.drumContext==i) "● " else "")+stringResource(r)) } } }
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
