
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
    val panX = remember { Animatable(0f) }
    val zoom = remember { Animatable(1.2f) }
    val tiltY = remember { Animatable(0f) }
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
    
    LaunchedEffect(isRunning, speedKmh) {
        if (isRunning && speedKmh > 0.1f) {
            val speedFactor = (speedKmh / 5f).coerceIn(0.2f, 3f)
            var direction = 1f
            while (isActive && isRunning) {
                totalDistance += speedFactor * 0.6f
                
                // Pan avanti e indietro come camminata reale nello stesso posto
                val targetPan = direction * 80f
                panX.animateTo(
                    targetPan,
                    tween((3000 / speedFactor).toInt(), easing = LinearEasing)
                )
                direction *= -1f
                
                val targetZoom = 1.25f + (incline / 200f)
                zoom.animateTo(
                    targetZoom,
                    tween((2500 / speedFactor).toInt(), easing = LinearEasing)
                )
                
                val targetTilt = -incline * 1.5f
                tiltY.animateTo(
                    targetTilt,
                    tween((2000 / speedFactor).toInt(), easing = LinearEasing)
                )
            }
        } else {
            panX.stop()
            zoom.stop()
            tiltY.stop()
        }
    }
    
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (resId != 0) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom.value
                        scaleY = zoom.value
                        translationX = panX.value
                        translationY = tiltY.value
                    },
                contentScale = ContentScale.Crop
            )
        }
        
        if (!isRunning) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text("PAUSA - $baseDrawableName", color = Color.White)
                Text("Distanza: ${totalDistance.toInt()}m - stesso panorama che continua", color = Color.White.copy(0.9f))
            }
        } else {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Text("$theme | ${speedKmh}km/h | ${incline}% | ${totalDistance.toInt()}m", color = Color.White)
                Text("Endless: $baseDrawableName", color = Color.White.copy(0.7f))
            }
        }
    }
}
