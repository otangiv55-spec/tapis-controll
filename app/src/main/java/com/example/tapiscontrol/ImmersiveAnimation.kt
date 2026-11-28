
package com.example.tapiscontrol

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.isActive
import kotlin.math.sin

// V5 ENDLESS SAME BIOMA - colline toscane che continuano all'infinito
// Stesso panorama che evolve come se camminassi davvero dentro
// Nessun salto ad altro video, solo la stessa zona con visuale che cambia

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
    
    // Scegli UN SOLO bioma in base al tema e resta li per sempre
    val baseDrawableName = remember(theme, incline) {
        when {
            theme.contains("Vigneti", true) || theme.contains("Toscana", true) -> "bg_vigneti_toscana"
            theme.contains("Collina", true) && incline < 3f -> "bg_collina_0_giorno"
            theme.contains("Collina", true) -> "bg_collina_tramonto_gold"
            theme.contains("Bosco", true) && incline < 5f -> "bg_bosco_3_giorno"
            theme.contains("Bosco", true) -> "bg_bosco_3_tramonto"
            theme.contains("Montagna", true) && incline < 10f -> "bg_montagna_9_drammatica"
            theme.contains("Montagna", true) -> "bg_montagna_12_estremo"
            theme.contains("Acqua", true) || theme.contains("Lago", true) && incline < 6f -> "bg_lago_6_giorno"
            theme.contains("Acqua", true) || theme.contains("Lago", true) -> "bg_lago_sera_stellato"
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
    
    // Movimento ENDLESS: pan orizzontale lento + zoom avanti/indietro + leggera oscillazione
    // Simula camminata reale dentro lo stesso paesaggio
    LaunchedEffect(isRunning, speedKmh) {
        if (isRunning && speedKmh > 0.1f) {
            val speedFactor = (speedKmh / 4f).coerceIn(0.3f, 3f)
            while (isActive && isRunning) {
                // Avanza distanza totale
                totalDistance += speedFactor * 0.6f
                
                // Pan orizzontale che va avanti e indietro lentamente come se guardassi intorno
                // Usa sin() per movimento naturale non lineare
                val targetPan = (sin(totalDistance / 200f) * 80f + sin(totalDistance / 80f) * 20f)
                panX.animateTo(
                    targetPan,
                    tween((2500 / speedFactor).toInt(), easing = LinearEasing)
                )
                
                // Zoom che simula avanzamento: quando cammini ti avvicini alle colline
                val targetZoom = 1.25f + sin(totalDistance / 300f) * 0.15f + (incline / 200f)
                zoom.animateTo(
                    targetZoom,
                    tween((3000 / speedFactor).toInt(), easing = FastOutSlowInEasing)
                )
                
                // Tilt verticale con pendenza
                val targetTilt = -incline * 1.5f + sin(totalDistance / 150f) * 5f
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
            // Immagine base che evolve all'infinito nello stesso bioma
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
            
            // Leggero overlay per profondita atmosferica che cambia con distanza
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(
                            alpha = (0.05f + (sin(totalDistance / 100f) * 0.05f)).coerceIn(0f, 0.15f)
                        )
                    )
            )
        }
        
        // HUD
        if (!isRunning) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text("PAUSA - Esplorazione $baseDrawableName", color = Color.White)
                Text("Distanza percorsa: ${totalDistance.toInt()}m nello stesso panorama", color = Color.White.copy(0.9f))
                Text("La visuale continua a cambiare come se camminassi davvero", color = Color.White.copy(0.6f))
            }
        } else {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Text("$theme | ${speedKmh}km/h | ${incline}% | ${totalDistance.toInt()}m", color = Color.White)
                Text("Esplorazione infinita: $baseDrawableName", color = Color.White.copy(0.7f))
            }
        }
    }
}
