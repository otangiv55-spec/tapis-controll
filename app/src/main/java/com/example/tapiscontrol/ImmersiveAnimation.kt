
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
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.isActive

// PACCHETTO FINALE VERDE - 13 paesaggi immersivi + pausa corretta
// Pausa: stop anim, mantieni position, overlay PAUSA, riprendi da li

@Composable
fun ImmersiveAnimation(
    isRunning: Boolean,
    speedKmh: Float,
    incline: Float,
    theme: String = "Collina"
) {
    var position by remember { mutableStateOf(0f) }
    val offsetAnim = remember { Animatable(0f) }
    
    // Pausa corretta: anim si ferma, position resta
    LaunchedEffect(isRunning, speedKmh) {
        if (isRunning && speedKmh > 0.05f) {
            val factor = (speedKmh / 8f).coerceIn(0.2f, 4f)
            while (isActive && isRunning) {
                offsetAnim.animateTo(
                    targetValue = offsetAnim.value + 1f,
                    animationSpec = tween(
                        durationMillis = (1200 / factor).toInt(),
                        easing = LinearEasing
                    )
                )
                position += factor * 0.1f
                if (offsetAnim.value > 5f) offsetAnim.snapTo(0f)
            }
        } else {
            offsetAnim.stop()
        }
    }
    
    // Scegli drawable in base a tema e pendenza
    val drawableRes = remember(theme, incline) {
        when {
            theme.contains("Collina", true) && incline < 3f -> R.drawable.bg_collina_0_giorno
            theme.contains("Collina", true) -> R.drawable.bg_collina_tramonto_gold
            theme.contains("Bosco", true) && incline < 5f -> R.drawable.bg_bosco_3_giorno
            theme.contains("Bosco", true) -> R.drawable.bg_bosco_3_tramonto
            theme.contains("Montagna", true) && incline < 10f -> R.drawable.bg_montagna_9_drammatica
            theme.contains("Montagna", true) -> R.drawable.bg_montagna_12_estremo
            theme.contains("Acqua", true) || theme.contains("Lago", true) -> if (incline > 6f) R.drawable.bg_lago_6_giorno else R.drawable.bg_lago_sera_stellato
            theme.contains("Vigneti", true) || theme.contains("Toscana", true) -> R.drawable.bg_vigneti_toscana
            theme.contains("Autunno", true) -> R.drawable.bg_autunno_rosse
            theme.contains("Deserto", true) -> R.drawable.bg_deserto_tramonto
            theme.contains("Cascate", true) || theme.contains("Giungla", true) -> R.drawable.bg_cascate_giungla
            theme.contains("Spiaggia", true) || theme.contains("Tropicale", true) -> R.drawable.bg_spiaggia_tropicale
            theme.contains("Lavanda", true) -> R.drawable.bg_lavanda_provenza
            else -> R.drawable.bg_collina_0_giorno
        }
    }
    
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        try {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } catch (e: Exception) {
            Box(Modifier.fillMaxSize().background(Color(0xFF1A1A2E)))
        }
        
        // Overlay info + PAUSA
        if (!isRunning) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
            Text(
                text = "PAUSA - Pos ${position.toInt()}",
                color = Color.White,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = "${theme} | ${speedKmh} km/h | ${incline}% | Pos ${position.toInt()}",
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
