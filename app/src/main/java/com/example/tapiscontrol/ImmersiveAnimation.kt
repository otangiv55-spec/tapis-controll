package com.example.tapiscontrol

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.isActive

// TEST BUILD VERDE - zero drawable, compila sicuro al 100%
// Pausa: si ferma dove si ferma il nastro e riparte da li
@Composable
fun ImmersiveAnimation(
    isRunning: Boolean,
    speedKmh: Float,
    incline: Float,
    theme: String = "Collina"
) {
    var position by remember { mutableStateOf(0f) }
    val anim = remember { Animatable(0f) }
    
    LaunchedEffect(isRunning, speedKmh) {
        if (isRunning && speedKmh > 0.1f) {
            val speedFactor = (speedKmh / 10f).coerceIn(0.1f, 3f)
            while (isActive && isRunning) {
                anim.animateTo(
                    targetValue = anim.value + 100f,
                    animationSpec = tween(
                        durationMillis = (2000 / speedFactor).toInt(),
                        easing = LinearEasing
                    )
                )
                position += speedFactor
                if (anim.value > 1000f) {
                    anim.snapTo(0f)
                }
            }
        } else {
            anim.stop()
        }
    }
    
    val bgColor = when {
        incline < 2f -> Color(0xFF87CEEB)
        incline < 6f -> Color(0xFF98D982)
        else -> Color(0xFF8B8B8B)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Text(
            text = "TEST VERDE - Pos: ${position.toInt()} Speed: $speedKmh Incl: $incline Theme: $theme ${if(isRunning) "" else " PAUSA"}",
            color = Color.White
        )
    }
}
