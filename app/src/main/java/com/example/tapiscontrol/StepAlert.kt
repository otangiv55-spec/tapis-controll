package com.example.tapiscontrol

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.max
import kotlin.math.min

/** Preavviso del cambio di step: testo da mostrare e se il prossimo step appartiene a un altro blocco. */
data class StepAlertText(val text: String, val blockChange: Boolean)

/** Durate (s) del preavviso tra cui scegliere. */
val ALERT_LEADS = listOf(3, 5)

/**
 * Se mancano al massimo `leadSec` secondi (al massimo meta' della durata dello step) alla fine dello step in
 * corso, restituisce il testo del preavviso; altrimenti null.
 */
fun stepAlertFor(w: Workout, steps: List<Step>, p: WorkoutRunner.Progress, leadSec: Int): StepAlertText? {
    if (!p.running || p.paused || p.stepIndex !in steps.indices) return null
    val cur = steps[p.stepIndex]
    val eff = min(leadSec, max(1, cur.durationSec / 2))
    if (p.stepRemainingSec !in 1..eff) return null
    val rem = p.stepRemainingSec
    val next = steps.getOrNull(p.stepIndex + 1)
        ?: return StepAlertText("Tra $rem s: fine allenamento", true)
    val here = w.blockNameAt(p.stepIndex)
    val there = w.blockNameAt(p.stepIndex + 1)
    val change = here != there
    val what = "${fmt(next.speedKmh)} km/h · ${fmt(next.inclinePct)} %"
    return if (change && there.isNotEmpty()) {
        StepAlertText("Tra $rem s: nuovo blocco «$there» · $what", true)
    } else {
        StepAlertText("Tra $rem s: step successivo · $what", change)
    }
}

/** Fascia colorata lampeggiante col preavviso (arancione: nuovo step, viola: nuovo blocco o fine). */
@Composable
fun StepAlertBanner(a: StepAlertText, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "alert").animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "alertAlpha",
    )
    Text(
        a.text,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = modifier
            .alpha(pulse)
            .background(if (a.blockChange) Color(0xFF7B1FA2) else Color(0xFFE65100), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** Interruttori e durata del preavviso (scheda In corso). */
@Composable
fun AlertSettings(vm: TapisViewModel) {
    val sound by vm.alertSound.collectAsStateWithLifecycle()
    val visual by vm.alertVisual.collectAsStateWithLifecycle()
    val lead by vm.alertLeadSec.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Avviso prima del cambio di step o blocco", style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(checked = sound, onCheckedChange = { vm.setAlertSound(it) })
            Text("Suono")
            Switch(checked = visual, onCheckedChange = { vm.setAlertVisual(it) })
            Text("Visivo")
        }
        if (sound || visual) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Anticipo:")
                ALERT_LEADS.forEach { s ->
                    if (s == lead) {
                        Button(onClick = { vm.setAlertLead(s) }) { Text("$s s") }
                    } else {
                        OutlinedButton(onClick = { vm.setAlertLead(s) }) { Text("$s s") }
                    }
                }
            }
        }
    }
}

/** Durate (s) della transizione graduale tra uno step e il successivo. */
val RAMP_OPTIONS = listOf(6, 10, 15, 20, 30)

/** Scelta della durata della transizione graduale di velocita' e pendenza (scheda In corso). */
@Composable
fun RampSettings(vm: TapisViewModel) {
    val ramp by vm.rampSec.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Transizione graduale tra gli step", style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RAMP_OPTIONS.forEach { s ->
                if (s == ramp) {
                    Button(onClick = { vm.setRamp(s) }) { Text("$s s") }
                } else {
                    OutlinedButton(onClick = { vm.setRamp(s) }) { Text("$s s") }
                }
            }
        }
        Text(
            "Velocita' e pendenza cambiano un po' alla volta nei primi secondi di ogni step " +
                "(al massimo meta' dello step). La pendenza del tapis sale di 1 % per volta.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
