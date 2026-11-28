
package com.example.tapiscontrol

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan

fun SceneTheme.toStableRes(seed: Long = 0): Int {
    val list = when(this) {
        SceneTheme.COLLINA -> listOf(R.drawable.bg_collina_0_giorno, R.drawable.bg_vigneti_toscana, R.drawable.bg_lavanda_provenza, R.drawable.bg_collina_tramonto_gold)
        SceneTheme.BOSCO -> listOf(R.drawable.bg_bosco_3_giorno, R.drawable.bg_autunno_rosse, R.drawable.bg_bosco_3_tramonto)
        SceneTheme.ACQUA -> listOf(R.drawable.bg_lago_6_giorno, R.drawable.bg_lago_sera_stellato, R.drawable.bg_spiaggia_tropicale, R.drawable.bg_cascate_giungla)
        SceneTheme.MONTAGNA -> listOf(R.drawable.bg_montagna_9_giorno, R.drawable.bg_montagna_9_drammatica, R.drawable.bg_montagna_12_estremo, R.drawable.bg_deserto_tramonto)
    }
    val idx = (seed % list.size).toInt().let { if(it<0) 0 else it }
    return list[idx]
}

@Composable
fun ImmersiveAnimationView(
    video: VideoRef,
    currentSpeedKmh: Float,
    currentInclinePct: Float,
    currentSegmentName: String = "",
    isPlaying: Boolean = true, // <--- questo arriva dal tuo paused: !paused
    workoutId: Long = 0,
    modifier: Modifier = Modifier
) {
    val resId = remember(video.scene, workoutId) { video.scene.toStableRes(workoutId) }
    
    // PAUSA/ RIPRESA ESATTA come animazione precedente
    // Usa Animatable che si ferma quando isPlaying=false e riparte dallo stesso valore
    val scrollAnim = remember { Animatable(0f) }
    val referenceSpeed = 10f
    val scrollFactor = (currentSpeedKmh / referenceSpeed).coerceIn(0f, 2.5f)
    val duration = (8000 / scrollFactor.coerceAtLeast(0.15f)).toInt().coerceIn(1200,20000)

    LaunchedEffect(isPlaying, duration) {
        if(isPlaying) {
            // Riparte da dove si era fermata
            while(true) {
                val remaining = 1000f - (scrollAnim.value % 1000f)
                val timeForRemaining = (duration * (remaining / 1000f)).toInt()
                scrollAnim.animateTo(
                    scrollAnim.value + remaining,
                    animationSpec = tween(timeForRemaining, easing = LinearEasing)
                )
                // Resetta a 0 mantenendo continuità visiva (loop infinito)
                scrollAnim.snapTo(0f)
            }
        } else {
            // Pausa: ferma animazione mantenendo valore attuale
            scrollAnim.stop()
        }
    }

    val scrollY = scrollAnim.value
    val tiltX = (atan(currentInclinePct/100f)*180/Math.PI*1.8f).toFloat()
    val density = LocalDensity.current

    Box(modifier.fillMaxSize().clipToBounds().graphicsLayer{rotationX = if(isPlaying) tiltX else 0f; cameraDistance = 16*density.density}) {
        for(i in 0..2){
            val factor = when(i){0->0.3f;1->0.6f;else->1f}
            val scale = when(i){0->1.15f;1->1.05f;else->1f}
            val alpha = when(i){0->0.8f;1->0.9f;else->1f}
            Column(Modifier.offset(y = (-(scrollY*factor)%1000).dp).graphicsLayer{scaleX=scale;scaleY=scale; alpha=alpha}) {
                Image(painterResource(resId), null, Modifier.fillMaxWidth().height(650.dp), contentScale=ContentScale.Crop)
                Image(painterResource(resId), null, Modifier.fillMaxWidth().height(650.dp), contentScale=ContentScale.Crop)
            }
        }
        // Overlay pausa
        if(!isPlaying) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("PAUSA", style=MaterialTheme.typography.headlineLarge, color=MaterialTheme.colorScheme.onPrimary.copy(0.9f))
            }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)){
            Text("${String.format("%.1f", currentSpeedKmh)} km/h · ${currentInclinePct.toInt()}% · ${video.scene.label}", style=MaterialTheme.typography.titleLarge.copy(fontSize=20.sp), color=MaterialTheme.colorScheme.onPrimary)
            if(currentSegmentName.isNotEmpty()) Text(currentSegmentName, style=MaterialTheme.typography.bodyMedium, color=MaterialTheme.colorScheme.onPrimary.copy(0.85f))
        }
        if(currentInclinePct>=10f){
            Box(Modifier.align(Alignment.TopCenter).padding(top=16.dp)){ Text("SALITA ESTREMA ${currentInclinePct.toInt()}%", color=androidx.compose.ui.graphics.Color.Red, fontSize=18.sp) }
        }
    }
}

@Composable
fun AnimationView(video: VideoRef, speed: Float, incline: Float, isPlaying: Boolean = true, modifier: Modifier = Modifier) {
    ImmersiveAnimationView(video, speed, incline, isPlaying=isPlaying, modifier=modifier)
}

@Composable
fun AnimatedBackground(scene: SceneTheme, light: TimeOfDay, speed: Float, incline: Float, isPlaying: Boolean = true, modifier: Modifier = Modifier) {
    val video = VideoRef(scene=scene, light=light, type=VideoType.ANIMATION)
    ImmersiveAnimationView(video, speed, incline, isPlaying=isPlaying, modifier=modifier)
}
