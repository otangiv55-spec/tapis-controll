package com.example.tapiscontrol

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.isActive

@Composable
fun ImmersiveAnimation(
    isRunning: Boolean,
    speedKmh: Float,
    incline: Float,
    theme: String = "Collina"
) {
    var position by remember { mutableStateOf(0f) }
    val offsetAnim = remember { Animatable(0f) }
    val context = LocalContext.current
    LaunchedEffect(isRunning, speedKmh) {
        if (isRunning && speedKmh > 0.05f) {
            val factor = (speedKmh / 8f).coerceIn(0.2f, 4f)
            while (isActive && isRunning) {
                offsetAnim.animateTo(targetValue = offsetAnim.value + 1f, animationSpec = tween((1200 / factor).toInt(), easing = LinearEasing))
                position += factor * 0.1f
                if (offsetAnim.value > 5f) offsetAnim.snapTo(0f)
            }
        } else {
            offsetAnim.stop()
        }
    }
    val drawableName = remember(theme, incline) {
        when {
            theme.contains("Collina", true) && incline < 3f -> "bg_collina_0_giorno"
            theme.contains("Collina", true) -> "bg_collina_tramonto_gold"
            theme.contains("Bosco", true) && incline < 5f -> "bg_bosco_3_giorno"
            theme.contains("Bosco", true) -> "bg_bosco_3_tramonto"
            theme.contains("Montagna", true) && incline < 10f -> "bg_montagna_9_drammatica"
            theme.contains("Montagna", true) -> "bg_montagna_12_estremo"
            theme.contains("Acqua", true) || theme.contains("Lago", true) -> if (incline > 6f) "bg_lago_6_giorno" else "bg_lago_sera_stellato"
            theme.contains("Vigneti", true) || theme.contains("Toscana", true) -> "bg_vigneti_toscana"
            theme.contains("Autunno", true) -> "bg_autunno_rosse"
            theme.contains("Deserto", true) -> "bg_deserto_tramonto"
            theme.contains("Cascate", true) || theme.contains("Giungla", true) -> "bg_cascate_giungla"
            theme.contains("Spiaggia", true) || theme.contains("Tropicale", true) -> "bg_spiaggia_tropicale"
            theme.contains("Lavanda", true) -> "bg_lavanda_provenza"
            else -> "bg_collina_0_giorno"
        }
    }
    val resId = remember(drawableName) { context.resources.getIdentifier(drawableName, "drawable", context.packageName) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (resId != 0) {
            Image(painter = painterResource(id = resId), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        if (!isRunning) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
            Text("PAUSA - Pos ${position.toInt()}", color = Color.White, modifier = Modifier.fillMaxSize())
        } else {
            Text("$theme | $speedKmh km/h | $incline% | Pos ${position.toInt()}", color = Color.White.copy(alpha = 0.8f))
        }
    }
}
