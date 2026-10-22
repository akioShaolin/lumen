package br.com.lumen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import br.com.lumen.dsp.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

data class Settings(val a4: Double = 440.0, val tolerance: Double = 3.0, val gate: Double = -65.0, val flats: Boolean = false, val manual: Boolean = false, val target: Double = 440.0, val preset: Int = 0, val drumContext: Int = 0, val ignoreMs: Int = 50)
data class UiState(val frame: CaptureFrame? = null, val error: String? = null, val target: Double? = null, val active: Boolean = false)

class LumenViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("lumen", 0)
    private val _settings = MutableStateFlow(Settings(
        a4 = prefs.getFloat("a4",440f).toDouble(), tolerance = prefs.getFloat("tolerance",3f).toDouble(), gate = prefs.getFloat("gate",-65f).toDouble(),
        flats = prefs.getBoolean("flats",false), manual = prefs.getBoolean("manual",false), target = prefs.getFloat("target",440f).toDouble(), preset = prefs.getInt("preset",0), drumContext = prefs.getInt("drumContext",0), ignoreMs = prefs.getInt("ignoreMs",50)))
    val settings = _settings.asStateFlow()
    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()
    private val tracker = TargetTracker()
    private val engine = CaptureEngine(app, { f ->
        val s = _settings.value
        val hz = f.analysis.pitch.hz
        val target = if (s.manual) s.target else hz?.let { Music.frequency(tracker.select(it,s.a4),s.a4) }
        if (hz == null) tracker.clear()
        _state.value = UiState(f, target = target, active = true)
    }, { _state.value = UiState(error = if (it is CaptureException) it.message else app.getString(R.string.capture_unavailable)) })
    fun update(s: Settings) {
        _settings.value = s
        prefs.edit().putFloat("a4",s.a4.toFloat()).putFloat("tolerance",s.tolerance.toFloat()).putFloat("gate",s.gate.toFloat()).putBoolean("flats",s.flats).putBoolean("manual",s.manual).putFloat("target",s.target.toFloat()).putInt("preset",s.preset).putInt("drumContext",s.drumContext).putInt("ignoreMs",s.ignoreMs).apply()
        tracker.clear(); engine.gateDb = s.gate; engine.drumIgnoreMs = s.ignoreMs
    }
    fun selectTarget(midi: Int) = update(_settings.value.copy(manual = true,target = Music.frequency(midi,_settings.value.a4)))
    fun mode(mode: Int) { engine.drumMode = mode == 2; _state.value = _state.value.copy(frame = null) }
    fun drumPeak(hz: Double) { engine.selectedDrumHz = hz }
    fun start() { engine.gateDb = _settings.value.gate; engine.start(); _state.value = UiState(active = true) }
    fun stop() { engine.stop(); engine.awaitStopped(); tracker.clear(); _state.value = UiState() }
    override fun onCleared() { engine.stop() }
}
