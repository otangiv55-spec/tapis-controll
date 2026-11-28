
package com.example.tapiscontrol

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive

@Composable
fun ImmersiveAnimation(
    isRunning: Boolean,
    speedKmh: Float,
    incline: Float,
    theme: String = "Collina"
) {
    var totalDistance by remember { mutableStateOf(0f) }
    val scrollX = remember { Animatable(0f) }
    val zoom = remember { Animatable(1.25f) }
    val context = LocalContext.current
    
    val baseDrawableName = remember(theme, incline) {
        when {
            theme.contains("Vigneti", true) || theme.contains("Toscana", true) -> "bg_vigneti_toscana"
            theme.contains("Collina", true) && incline < 3f -> "bg_collina_0_giorno"
            theme.contains("Collina", true) -> "bg_collina_tramonto_gold"
            theme.contains("Bosco", true) && incline < 5f -> "bg_bosco_3_giorno"
            theme.contains("Bosco", true) -> "bg_bosco_3_tramonto"
            theme.contains("Montagna", true) && incline < 10f -> "bg_montagna_9_drammatica"
            theme.contains("Montagna", true) -> "bg_montagna_12_estremo"
            theme.contains("Lago", true) && incline < 6f -> "bg_lago_6_giorno"
            theme.contains("Lago", true) -> "bg_lago_sera_stellato"
            theme.contains("Autunno", true) -> "bg_autunno_rosse"
            theme.contains("Deserto", true) -> "bg_deserto_tramonto"
            theme.contains("Cascate", true) -> "bg_cascate_giungla"
            theme.contains("Spiaggia", true) -> "bg_spiaggia_tropicale"
            theme.contains("Lavanda", true) -> "bg_lavanda_provenza"
            else -> "bg_vigneti_toscana"
        }
    }
    
    val resId = remember(baseDrawableName) {
        context.resources.getIdentifier(baseDrawableName, "drawable", context.packageName)
    }
    
    // VELOCITA MOLTO EVIDENTE: scroll continuo la cui velocita e' direttamente speedKmh
    LaunchedEffect(isRunning, speedKmh) {
        if (isRunning && speedKmh > 0.1f) {
            while (isActive && isRunning) {
                // Velocita scroll proporzionale a speedKmh - molto evidente
                // A 3 km/h scorre piano, a 12 km/h scorre 4 volte piu veloce
                val speedFactor = speedKmh * 0.8f
                val duration = (1200 / speedFactor.coerceAtLeast(0.5f)).toInt().coerceIn(200, 3000)
                
                scrollX.animateTo(
                    targetValue = scrollX.value + 100f,
                    animationSpec = tween(duration, easing = LinearEasing)
                )
                totalDistance += speedFactor
                if (scrollX.value > 500f) scrollX.snapTo(0f)
                
                // Zoom cambia con pendenza
                zoom.animateTo(
                    1.25f + (incline / 150f),
                    tween(800, easing = LinearEasing)
                )
            }
        } else {
            scrollX.stop()
            zoom.stop()
        }
    }
    
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (resId != 0) {
            // Effetto scroll infinito - sembra video POV che avanza
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom.value
                        scaleY = zoom.value
                    }
            ) {
                // Due copie per loop continuo
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = -scrollX.value },
                    contentScale = ContentScale.Crop
                )
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = -scrollX.value + 600f },
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        if (!isRunning) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text("PAUSA", color = Color.White)
                Text("Velocita tapis: ${speedKmh}km/h - Distanza: ${totalDistance.toInt()}m", color = Color.White)
                Text("Scroll fermo - mantiene posizione", color = Color.White.copy(0.7f))
            }
        } else {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Text("$theme | ${speedKmh}km/h | ${incline}% | ${totalDistance.toInt()}m", color = Color.White)
                Text("Velocita scroll: ${"%.1f".format(speedKmh * 0.8f)} - stesso panorama endless", color = Color.White.copy(0.7f))
            }
        }
    }
}
