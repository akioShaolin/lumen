package br.com.lumen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import br.com.lumen.dsp.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StableLayoutTest {
    @get:Rule val compose = createComposeRule()
    @Test fun audioStatesKeepBoundsAtNormalFont() = verifyBounds(1f)
    @Test fun audioStatesKeepBoundsAtLargeFontAndRemainScrollable() = verifyBounds(2f)

    private fun verifyBounds(fontScale: Float) {
        val pitch = mutableStateOf(PresentedPitch(440.0,69,69,0.99,ReadingKind.LIVE))
        val status = mutableStateOf(ListeningStatus.LISTENING)
        val message = mutableStateOf("Ouvindo…")
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density,fontScale)) {
                MaterialTheme {
                    Column(Modifier.width(320.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        LumenHeader { }
                        StableText(message.value,listOf("Ouvindo…","Afinação incerta — use uma nota isolada ou um alvo manual"),Modifier.testTag("variable-status"))
                        StatusRegion(UiState(pitch=pitch.value,status=status.value),false)
                        NoteReadout(pitch.value,false)
                        FlowMetrics(pitch.value.hz,pitch.value.cents(440.0,null),-20.0,pitch.value.kind)
                        SpectrumPlot(SpectrumScene(null,null,440.0,null,48000,false,440.0,false),true,false)
                        TuningBar(pitch.value.cents(440.0,null),pitch.value.kind==ReadingKind.LIVE,3.0)
                    }
                }
            }
        }
        compose.waitForIdle()
        val tags = listOf("variable-status","status-region","note-card","metrics","spectrum-plot","tuning-bar")
        val before = tags.associateWith { compose.onNodeWithTag(it).getUnclippedBoundsInRoot() }
        for(kind in listOf(ReadingKind.HELD,ReadingKind.EMPTY,ReadingKind.LIVE)) {
            compose.runOnIdle {
                pitch.value = if(kind == ReadingKind.EMPTY) PresentedPitch() else PresentedPitch(82.41,40,40,0.95,kind)
                status.value = if(kind == ReadingKind.LIVE) ListeningStatus.LISTENING else ListeningStatus.UNCERTAIN
                message.value = if(kind == ReadingKind.LIVE) "Ouvindo…" else "Afinação incerta — use uma nota isolada ou um alvo manual"
            }
            compose.waitForIdle()
            tags.forEach { assertEquals("Bounds changed for $it / $kind / font=$fontScale",before[it],compose.onNodeWithTag(it).getUnclippedBoundsInRoot()) }
        }
        compose.onNodeWithTag("tuning-bar").performScrollTo().assertIsDisplayed()
    }
}
