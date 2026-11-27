package com.example.tapiscontrol

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val Z_NEAR = 1.2f
private const val Z_FAR = 140f
private const val SPAN = Z_FAR - Z_NEAR
private const val ROAD_HALF = 2.0f
private const val BAND = 1.5f          // metri di mezza fascia del terreno (fascia completa = 3 m)
private const val STRIPE_END = 45f     // oltre questa distanza le fasce alternate sfumano

/** Palette di un ambiente a una certa ora del giorno. */
private class Pal(
    val skyTop: Color, val skyHor: Color, val fog: Color,
    val grassA: Color, val grassB: Color, val roadA: Color, val roadB: Color, val shoulder: Color,
    val r1: Color, val r2: Color, val r3: Color,
    val waterA: Color, val waterB: Color,
    val cloud: Color, val tint: Color,
)

private fun buildPal(theme: SceneTheme, light: TimeOfDay): Pal {
    val d = when (theme) {
        SceneTheme.COLLINA -> Pal(
            Color(0xFF4F9FE8), Color(0xFFD9EDFA), Color(0xFFC9E0D2),
            Color(0xFF63A64C), Color(0xFF559443), Color(0xFFBDA37C), Color(0xFFAB9069), Color(0xFF88985A),
            Color(0xFFA9C0DB), Color(0xFF8DB0A0), Color(0xFF6E9F7C),
            Color(0xFF3E8FC0), Color(0xFF4AA0CF), Color(0xCCFFFFFF), Color(0x00000000),
        )
        SceneTheme.MONTAGNA -> Pal(
            Color(0xFF2F7FD6), Color(0xFFCFE6F7), Color(0xFFC4D6E6),
            Color(0xFF7A9A55), Color(0xFF6C8B4B), Color(0xFFA39C93), Color(0xFF938C84), Color(0xFF7D8760),
            Color(0xFF9FB0C6), Color(0xFF7F92AA), Color(0xFF55766A),
            Color(0xFF3E8FC0), Color(0xFF4AA0CF), Color(0xCCFFFFFF), Color(0x00000000),
        )
        SceneTheme.BOSCO -> Pal(
            Color(0xFF7FB6A8), Color(0xFFE6F0D0), Color(0xFFB7CDB0),
            Color(0xFF3F7A38), Color(0xFF356B31), Color(0xFF8C6E4B), Color(0xFF7C603F), Color(0xFF4F6B35),
            Color(0xFF5E8B6A), Color(0xFF4C7A5A), Color(0xFF3F6B4C),
            Color(0xFF3E8FC0), Color(0xFF4AA0CF), Color(0x99FFFFFF), Color(0x00000000),
        )
        SceneTheme.ACQUA -> Pal(
            Color(0xFF4A9BE0), Color(0xFFE0F1FA), Color(0xFFCFE3EA),
            Color(0xFF8DB35A), Color(0xFF7FA650), Color(0xFFD8C9A5), Color(0xFFC9B88F), Color(0xFFB2B08A),
            Color(0xFFB6CBDD), Color(0xFF9DBBCB), Color(0xFF7EA8A0),
            Color(0xFF3E8FC0), Color(0xFF4AA0CF), Color(0xCCFFFFFF), Color(0x00000000),
        )
    }
    return when (light) {
        TimeOfDay.GIORNO -> d
        TimeOfDay.TRAMONTO -> {
            val pur = Color(0xFF8E6FA0)
            val sun = Color(0xFFE59A6A)
            Pal(
                Color(0xFF4B5FA8), Color(0xFFFFB27A), lerp(d.fog, Color(0xFFF0B48C), 0.6f),
                d.grassA, d.grassB, d.roadA, d.roadB, d.shoulder,
                lerp(d.r1, pur, 0.45f), lerp(d.r2, pur, 0.45f), lerp(d.r3, pur, 0.40f),
                lerp(d.waterA, sun, 0.40f), lerp(d.waterB, sun, 0.40f), Color(0xCCFFC9A8), Color(0x2EFF8A3D),
            )
        }
        TimeOfDay.SERA -> {
            val nv = Color(0xFF111A38)
            val wn = Color(0xFF0F1C3A)
            Pal(
                Color(0xFF070B24), Color(0xFF2B3A70), Color(0xFF27304F),
                d.grassA, d.grassB, d.roadA, d.roadB, d.shoulder,
                lerp(d.r1, nv, 0.70f), lerp(d.r2, nv, 0.72f), lerp(d.r3, nv, 0.74f),
                lerp(d.waterA, wn, 0.60f), lerp(d.waterB, wn, 0.60f), Color(0x40506090), Color(0x70101A44),
            )
        }
    }
}

/** Elemento del paesaggio. kind: 0-1 alberi tondi, 2 pino, 3 cespuglio, 4 sasso, 5 fiori, 6 barca, 7 lampione, 8 albero alto. */
private class Scenery(val side: Int, val lateral: Float, val phase: Float, val kind: Int, val size: Float, val hue: Int)

private fun makeScenery(theme: SceneTheme): List<Scenery> {
    val r = Random(11 + theme.ordinal)
    val out = ArrayList<Scenery>()
    when (theme) {
        SceneTheme.COLLINA -> {
            for (i in 0 until 64) {
                out += Scenery(
                    side = if (i % 2 == 0) -1 else 1,
                    lateral = 3.4f + r.nextFloat() * 9f,
                    phase = (i / 64f) * SPAN + r.nextFloat() * 1.5f,
                    kind = r.nextInt(3),
                    size = 0.8f + r.nextFloat() * 0.7f,
                    hue = r.nextInt(3),
                )
            }
            for (i in 0 until 54) {
                out += Scenery(
                    side = if (r.nextBoolean()) -1 else 1,
                    lateral = 2.5f + r.nextFloat() * 7f,
                    phase = r.nextFloat() * SPAN,
                    kind = 3 + r.nextInt(3),
                    size = 0.6f + r.nextFloat() * 0.8f,
                    hue = r.nextInt(3),
                )
            }
        }
        SceneTheme.MONTAGNA -> {
            for (i in 0 until 70) {
                out += Scenery(if (i % 2 == 0) -1 else 1, 3.2f + r.nextFloat() * 12f,
                    (i / 70f) * SPAN + r.nextFloat() * 1.5f, 2, 1.0f + r.nextFloat() * 0.9f, r.nextInt(3))
            }
            for (i in 0 until 40) {
                out += Scenery(if (r.nextBoolean()) -1 else 1, 2.8f + r.nextFloat() * 8f,
                    r.nextFloat() * SPAN, 4, 1.0f + r.nextFloat() * 1.6f, r.nextInt(3))
            }
            for (i in 0 until 24) {
                out += Scenery(if (r.nextBoolean()) -1 else 1, 2.5f + r.nextFloat() * 6f,
                    r.nextFloat() * SPAN, 3, 0.6f + r.nextFloat() * 0.6f, r.nextInt(3))
            }
            for (i in 0 until 20) {
                out += Scenery(if (r.nextBoolean()) -1 else 1, 2.5f + r.nextFloat() * 6f,
                    r.nextFloat() * SPAN, 5, 0.8f + r.nextFloat() * 0.6f, r.nextInt(3))
            }
        }
        SceneTheme.BOSCO -> {
            for (i in 0 until 120) {
                out += Scenery(if (i % 2 == 0) -1 else 1, 2.7f + r.nextFloat() * 8f,
                    (i / 120f) * SPAN + r.nextFloat() * 1.0f, 8, 0.9f + r.nextFloat() * 0.6f, r.nextInt(3))
            }
            for (i in 0 until 60) {
                out += Scenery(if (r.nextBoolean()) -1 else 1, 2.4f + r.nextFloat() * 5f,
                    r.nextFloat() * SPAN, 3, 0.7f + r.nextFloat() * 0.8f, r.nextInt(3))
            }
            for (i in 0 until 20) {
                out += Scenery(if (r.nextBoolean()) -1 else 1, 2.4f + r.nextFloat() * 5f,
                    r.nextFloat() * SPAN, 4, 0.5f + r.nextFloat() * 0.6f, r.nextInt(3))
            }
        }
        SceneTheme.ACQUA -> {
            for (i in 0 until 26) out += Scenery(-1, 2.9f, i * (SPAN / 26f), 7, 1f, 0)
            for (i in 0 until 40) {
                out += Scenery(-1, 4f + r.nextFloat() * 10f, r.nextFloat() * SPAN, r.nextInt(3),
                    0.8f + r.nextFloat() * 0.7f, r.nextInt(3))
            }
            for (i in 0 until 30) {
                out += Scenery(-1, 2.6f + r.nextFloat() * 7f, r.nextFloat() * SPAN, 3,
                    0.6f + r.nextFloat() * 0.8f, r.nextInt(3))
            }
            for (i in 0 until 20) {
                out += Scenery(1, 3.5f + r.nextFloat() * 2.5f, r.nextFloat() * SPAN, 4,
                    0.6f + r.nextFloat() * 0.9f, r.nextInt(3))
            }
            for (i in 0 until 14) {
                out += Scenery(1, 14f + r.nextFloat() * 40f, r.nextFloat() * SPAN, 6,
                    1.0f + r.nextFloat() * 0.8f, r.nextInt(3))
            }
        }
    }
    return out
}

private val sceneryCache: Map<SceneTheme, List<Scenery>> = SceneTheme.values().associateWith { makeScenery(it) }

/** Stelle per la sera: posizione relativa (x in 0..1, y in 0..1 della parte di cielo) e dimensione. */
private val STARS: List<Triple<Float, Float, Float>> = run {
    val r = Random(5)
    List(46) { Triple(r.nextFloat(), r.nextFloat(), 0.8f + r.nextFloat() * 1.4f) }
}

// Percorso sinuoso e colline dolci, funzioni della distanza percorsa (metri).
// Curve ben visibili: due onde sovrapposte (S, tornantini dolci) piu' una lenta per variare la direzione
private fun curveAt(zw: Double) =
    18.0 * sin(zw * 0.0314) + 5.0 * sin(zw * 0.062 + 1.3) + 12.0 * sin(zw * 0.0107 + 2.0)
private fun curveSlope(zw: Double) =
    18.0 * 0.0314 * cos(zw * 0.0314) + 5.0 * 0.062 * cos(zw * 0.062 + 1.3) + 12.0 * 0.0107 * cos(zw * 0.0107 + 2.0)
private fun elevAt(zw: Double) = 0.7 * sin(zw * 0.0698 + 0.4) + 0.35 * sin(zw * 0.17 + 0.7)
private fun elevSlope(zw: Double) = 0.7 * 0.0698 * cos(zw * 0.0698 + 0.4) + 0.35 * 0.17 * cos(zw * 0.17 + 0.7)

/** Proiezione prospettica: la camera segue la tangente del sentiero e l'inclinazione del terreno. */
private class Proj(val d: Double, val f: Float, val cx: Float, val yHor: Float, val cam: Float) {
    private val c0 = curveAt(d)
    private val cs = curveSlope(d)
    private val e0 = elevAt(d)
    private val es = elevSlope(d)
    fun lateral(zr: Float): Float = (curveAt(d + zr) - c0 - cs * zr).toFloat()
    fun elev(zr: Float): Float = (elevAt(d + zr) - e0 - es * zr).toFloat()
    fun y(zr: Float): Float = yHor + (cam - elev(zr)) * f / zr
    fun x(zr: Float, off: Float): Float = cx + (lateral(zr) + off) * f / zr
}

private fun fogT(z: Float): Float = ((z - 12f) / (Z_FAR - 12f)).coerceIn(0f, 1f).pow(1.3f) * 0.85f

/**
 * Percorso animato (nessun file video): sentiero con curve, colline, alberi e nuvole che scorre alla velocita'
 * REALE del tapis roulant e si inclina con la sua pendenza. Segue quindi in automatico qualsiasi allenamento (e le
 * correzioni manuali). Non finisce mai. `onPosition` riporta l'equivalente in secondi di video alla velocita'
 * di ripresa, cosi' il profilo del percorso funziona come con un video vero.
 * La scena si adatta a qualsiasi proporzione dello schermo (anche a tutto schermo in orizzontale).
 * `overlay = false` non disegna nessun dato (li mostra il chiamante).
 */
@Composable
fun AnimatedRoute(
    speedKmh: Double,
    inclinePct: Double,
    playing: Boolean,
    refSpeedKmh: Double,
    startSec: Double,
    caption: String,
    modifier: Modifier = Modifier,
    hud: HudData? = null,
    hudScale: Float = 1f,
    overlay: Boolean = true,
    theme: SceneTheme = SceneTheme.COLLINA,
    light: TimeOfDay = TimeOfDay.GIORNO,
    onPosition: (Double) -> Unit = {},
) {
    val pal = remember(theme, light) { buildPal(theme, light) }
    val speedNow by rememberUpdatedState(speedKmh)
    val inclineNow by rememberUpdatedState(inclinePct)
    val playingNow by rememberUpdatedState(playing)
    val refNow by rememberUpdatedState(refSpeedKmh)
    val posCb by rememberUpdatedState(onPosition)

    var distance by remember { mutableDoubleStateOf(startSec * refSpeedKmh / 3.6) }   // metri percorsi
    var tilt by remember { mutableFloatStateOf(inclinePct.toFloat()) }                // pendenza "ammorbidita"
    var speedSmooth by remember { mutableFloatStateOf(0f) }                           // km/h ammorbiditi
    var animT by remember { mutableFloatStateOf(0f) }                                 // secondi (uccelli, ecc.)

    LaunchedEffect(Unit) {
        var last = 0L
        var acc = 0.0
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1_000_000_000.0).coerceIn(0.0, 0.1)
                    val target = if (playingNow) speedNow.toFloat() else 0f
                    speedSmooth += (target - speedSmooth) * min(1.0, dt * 3.0).toFloat()
                    tilt += (inclineNow.toFloat() - tilt) * min(1.0, dt * 2.0).toFloat()
                    distance += speedSmooth / 3.6 * dt
                    animT += dt.toFloat()
                    acc += dt
                    if (acc >= 0.5) {
                        acc = 0.0
                        posCb(distance / (refNow / 3.6))
                    }
                }
                last = now
            }
        }
    }

    Box(modifier) {
        Canvas(Modifier.matchParentSize()) { drawScene(distance, tilt, speedSmooth, animT, theme, light, pal) }
        if (overlay) {
            if (hud != null) {
                RunHud(hud, Modifier.matchParentSize(), hudScale)
            } else {
                Column(
                    Modifier.align(Alignment.TopStart).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    HudChip("${fmt(if (playing) speedKmh else 0.0)} km/h")
                    HudChip("pendenza ${inclinePct.roundToInt()} %")
                    if (caption.isNotEmpty()) HudChip(caption)
                }
            }
        }
    }
}

private fun DrawScope.drawScene(
    distance: Double, tiltPct: Float, speed: Float, t: Float,
    theme: SceneTheme, light: TimeOfDay, pal: Pal,
) {
    // leggero dondolio a ogni passo (circa 0,75 m)
    val bobAmp = if (speed > 0.5f) size.height * 0.004f else 0f
    val bob = (sin(distance / 0.75 * 2.0 * PI) * bobAmp).toFloat()
    translate(0f, bob) { paintScene(distance, tiltPct, t, theme, light, pal) }
}

private fun DrawScope.paintScene(distance: Double, tiltPct: Float, t: Float, theme: SceneTheme, light: TimeOfDay, pal: Pal) {
    val w = size.width
    val h = size.height
    val f = h * 1.6f                 // distanza focale basata sull'altezza: stessa scena a ogni proporzione
    val cx = w / 2f
    val cam = 1.6f                   // altezza degli occhi (m)
    val dF = distance.toFloat()
    val night = light == TimeOfDay.SERA

    // in salita il terreno "sale" davanti a te: la linea dell'orizzonte si alza (effetto esagerato di 1,5 volte)
    val slope = (tiltPct / 100f) * 1.5f
    val yHor = max(h * 0.10f, h * 0.45f - slope * f)
    val proj = Proj(distance, f, cx, yHor, cam)

    // ---------------------------------------------------------------- cielo
    drawRect(
        brush = Brush.verticalGradient(listOf(pal.skyTop, pal.skyHor), startY = 0f, endY = yHor),
        topLeft = Offset(0f, -h * 0.05f),
        size = Size(w, yHor + h * 0.05f + 2f),
    )
    if (night) {
        for ((idx, st) in STARS.withIndex()) {
            val a = (0.45f + 0.45f * sin(t * 1.7f + idx)).coerceIn(0f, 1f)
            drawCircle(Color.White.copy(alpha = a), st.third, Offset(st.first * w, st.second * yHor * 0.9f))
        }
    }
    when (light) {
        TimeOfDay.GIORNO -> {
            drawCircle(Color(0x33FFF4B0), h * 0.17f, Offset(w * 0.76f, h * 0.17f))
            drawCircle(Color(0x55FFF4B0), h * 0.10f, Offset(w * 0.76f, h * 0.17f))
            drawCircle(Color(0xFFFFF6C4), h * 0.055f, Offset(w * 0.76f, h * 0.17f))
        }
        TimeOfDay.TRAMONTO -> {
            val c = Offset(w * 0.30f, yHor - h * 0.05f)
            drawCircle(
                brush = Brush.radialGradient(listOf(Color(0xCCFFC070), Color(0x00FFC070)), center = c, radius = h * 0.55f),
                radius = h * 0.55f,
                center = c,
            )
            drawCircle(Color(0xFFFFB347), h * 0.075f, c)
            drawCircle(Color(0xFFFFE2A8), h * 0.05f, c)
        }
        TimeOfDay.SERA -> {
            val c = Offset(w * 0.76f, h * 0.16f)
            drawCircle(
                brush = Brush.radialGradient(listOf(Color(0x44DDE6FF), Color(0x00DDE6FF)), center = c, radius = h * 0.22f),
                radius = h * 0.22f,
                center = c,
            )
            drawCircle(Color(0xFFF4F1DE), h * 0.04f, c)
            drawCircle(Color(0x22909090), h * 0.012f, Offset(c.x - h * 0.012f, c.y - h * 0.008f))
            drawCircle(Color(0x22909090), h * 0.008f, Offset(c.x + h * 0.014f, c.y + h * 0.012f))
        }
    }

    // nuvole (due strati a velocita' diverse)
    val cl = w + h * 0.8f
    for (i in 0..4) {
        val layer = if (i < 3) 0.6f else 0.3f
        val x = (((i * w * 0.38f - dF * layer - t * 4f) % cl) + cl) % cl - h * 0.4f
        val y = h * (0.07f + 0.055f * (i % 3)) + (if (i >= 3) h * 0.03f else 0f)
        val cw = h * (0.30f - 0.04f * (i % 3))
        drawOval(pal.cloud, Offset(x, y), Size(cw, cw * 0.30f))
        drawOval(pal.cloud, Offset(x + cw * 0.22f, y - cw * 0.10f), Size(cw * 0.55f, cw * 0.30f))
        drawOval(pal.cloud, Offset(x + cw * 0.45f, y + cw * 0.02f), Size(cw * 0.5f, cw * 0.22f))
    }

    // uccelli lontani (non di sera)
    if (!night) {
        for (i in 0..2) {
            val bx = (((i * w * 0.31f + t * (18f + i * 7f)) % (w + 120f)) + (w + 120f)) % (w + 120f) - 60f
            val by = h * (0.13f + 0.07f * i) + sin(t * 1.3f + i) * h * 0.012f
            val flap = sin(t * 7f + i * 2f) * h * 0.012f
            val sw = h * 0.016f
            val bc = Color(0xCC2B3A4A)
            drawLine(bc, Offset(bx - sw, by - flap), Offset(bx, by), strokeWidth = 2.2f)
            drawLine(bc, Offset(bx, by), Offset(bx + sw, by - flap), strokeWidth = 2.2f)
        }
    }

    // montagne e colline lontane (scorrono pochissimo: effetto profondita')
    val rf = when (theme) {
        SceneTheme.MONTAGNA -> 1.6f
        SceneTheme.BOSCO -> 0.7f
        SceneTheme.ACQUA -> 0.6f
        SceneTheme.COLLINA -> 1f
    }
    if (theme == SceneTheme.MONTAGNA) {
        // neve sulle cime: prima la sagoma bianca, poi quella di roccia piu' bassa sulle cime
        val snow = lerp(Color(0xFFF4F7FB), pal.r1, when (light) {
            TimeOfDay.GIORNO -> 0f
            TimeOfDay.TRAMONTO -> 0.35f
            TimeOfDay.SERA -> 0.75f
        })
        ridge(snow, h * 0.17f * rf, 0.0055f, dF * 0.003f, h * 0.075f * rf, yHor)
        ridge(pal.r1, h * 0.17f * rf * 0.72f, 0.0055f, dF * 0.003f, h * 0.075f * rf, yHor)
        ridge(snow, h * 0.10f * rf, 0.010f, dF * 0.007f + 2f, h * 0.03f * rf, yHor)
        ridge(pal.r2, h * 0.10f * rf * 0.72f, 0.010f, dF * 0.007f + 2f, h * 0.03f * rf, yHor)
    } else {
        ridge(pal.r1, h * 0.17f * rf, 0.0055f, dF * 0.003f, h * 0.075f * rf, yHor)
        ridge(pal.r2, h * 0.10f * rf, 0.010f, dF * 0.007f + 2f, h * 0.03f * rf, yHor)
    }
    ridge(pal.r3, h * 0.05f, 0.020f, dF * 0.015f + 5f, h * 0.008f, yHor)

    // ---------------------------------------------------------------- terreno
    drawRect(pal.fog, Offset(0f, yHor), Size(w, h - yHor + 2f))

    // confini delle strisce, dalla piu' vicina alla piu' lontana: fasce fisse nel mondo (3 m) per il movimento
    val bounds = ArrayList<Float>(96)
    bounds += Z_NEAR
    var k = ceil((distance + Z_NEAR) / BAND).toLong()
    var zr = (k * BAND - distance).toFloat()
    while (zr < STRIPE_END - 1f) {
        if (zr > bounds[bounds.size - 1] + 0.05f) bounds += zr
        k++
        zr = (k * BAND - distance).toFloat()
    }
    bounds += STRIPE_END
    val ratio = (Z_FAR / STRIPE_END).pow(1f / 22f)
    var zg = STRIPE_END
    repeat(22) {
        zg *= ratio
        bounds += min(zg, Z_FAR)
    }
    val n = bounds.size
    val ys = FloatArray(n) { proj.y(bounds[it]) }
    val xs = FloatArray(n) { proj.x(bounds[it], 0f) }

    // elementi del paesaggio, dal piu' lontano al piu' vicino
    val items = (sceneryCache[theme] ?: emptyList())
        .map { it to (Z_NEAR + (((it.phase - dF) % SPAN) + SPAN) % SPAN) }
        .sortedByDescending { it.second }
    var ip = 0

    val grassAvg = lerp(pal.grassA, pal.grassB, 0.5f)
    val roadAvg = lerp(pal.roadA, pal.roadB, 0.5f)
    val waterAvg = lerp(pal.waterA, pal.waterB, 0.5f)

    for (i in n - 2 downTo 0) {
        val za = bounds[i]
        val zb = bounds[i + 1]
        val yN = ys[i]
        val yF = ys[i + 1]
        val zMid = (za + zb) / 2f
        val band = floor((distance + zMid) / BAND).toLong()
        val parity = (band and 1L) == 0L
        val contrast = ((STRIPE_END - zMid) / 15f).coerceIn(0f, 1f)
        val fog = fogT(zMid)
        val gCol = lerp(lerp(grassAvg, if (parity) pal.grassA else pal.grassB, contrast), pal.fog, fog)
        val rCol = lerp(lerp(roadAvg, if (parity) pal.roadA else pal.roadB, contrast), pal.fog, fog)
        val sCol = lerp(pal.shoulder, pal.fog, fog)

        val bottom = max(yN, yF) + 1f
        if (bottom > yF) {
            drawRect(gCol, Offset(0f, yF), Size(w, bottom - yF))

            if (theme == SceneTheme.ACQUA) {
                // acqua sul lato destro, oltre la riva (6,5 m dal centro del sentiero)
                val wN = proj.x(za, 6.5f)
                val wF = proj.x(zb, 6.5f)
                val wCol = lerp(lerp(waterAvg, if (parity) pal.waterA else pal.waterB, contrast), pal.fog, fog)
                val wp = Path().apply {
                    moveTo(wF, yF)
                    lineTo(w + 40f, yF)
                    lineTo(w + 40f, bottom)
                    lineTo(wN, bottom)
                    close()
                }
                drawPath(wp, wCol)
                if (zMid < STRIPE_END) {
                    for (q in 0..1) {
                        val hsh = ((band * 31 + q * 17) % 97).toInt()
                        val lat = 8f + (hsh % 13).toFloat()
                        val spx = proj.x(zMid, lat)
                        val spy = proj.y(zMid)
                        val len = 0.9f * f / zMid
                        val a = (0.25f + 0.25f * sin(t * 2.2f + hsh)).coerceIn(0f, 1f)
                        drawLine(
                            Color.White.copy(alpha = a),
                            Offset(spx - len, spy),
                            Offset(spx + len, spy),
                            strokeWidth = max(1f, len * 0.08f),
                        )
                    }
                }
            }

            val hwN = ROAD_HALF * f / za
            val hwF = ROAD_HALF * f / zb
            val sh = Path().apply {
                moveTo(xs[i + 1] - hwF * 1.22f, yF)
                lineTo(xs[i + 1] + hwF * 1.22f, yF)
                lineTo(xs[i] + hwN * 1.22f, bottom)
                lineTo(xs[i] - hwN * 1.22f, bottom)
                close()
            }
            drawPath(sh, sCol)
            val rd = Path().apply {
                moveTo(xs[i + 1] - hwF, yF)
                lineTo(xs[i + 1] + hwF, yF)
                lineTo(xs[i] + hwN, bottom)
                lineTo(xs[i] - hwN, bottom)
                close()
            }
            drawPath(rd, rCol)
        }

        // elementi che stanno su questa striscia
        while (ip < items.size && items[ip].second >= za) {
            drawScenery(items[ip].first, items[ip].second, proj, w, pal, light)
            ip++
        }
    }
    // eventuali elementi rimasti (molto vicini)
    while (ip < items.size) {
        drawScenery(items[ip].first, items[ip].second, proj, w, pal, light)
        ip++
    }

    // ---------------------------------------------------------------- bosco: chiome in alto e raggi di luce
    if (theme == SceneTheme.BOSCO) {
        val canopy = Color(0xFF1E4F27).copy(alpha = 0.92f)
        for (i in 0..5) {
            val px = if (i < 3) w * (0.02f + 0.11f * i) else w * (0.98f - 0.11f * (i - 3))
            drawCircle(canopy, h * (0.17f - 0.02f * (i % 3)), Offset(px, -h * 0.02f))
        }
        if (light != TimeOfDay.SERA) {
            val rayCol = if (light == TimeOfDay.TRAMONTO) Color(0x22FFB36B) else Color(0x1FFFF4C0)
            for (j in 0..3) {
                val x0 = w * (0.22f + 0.17f * j) + sin(t * 0.35f + j) * w * 0.012f
                val ray = Path().apply {
                    moveTo(x0, 0f)
                    lineTo(x0 + w * 0.05f, 0f)
                    lineTo(x0 + w * 0.05f - h * 0.30f, h * 0.85f)
                    lineTo(x0 - h * 0.30f, h * 0.85f)
                    close()
                }
                drawPath(ray, rayCol)
            }
        }
    }

    // ---------------------------------------------------------------- luce dell'ora e bordi scuri
    if (light != TimeOfDay.GIORNO) drawRect(pal.tint, Offset.Zero, size)
    drawRect(
        brush = Brush.radialGradient(
            listOf(Color.Transparent, Color(0x44000000)),
            center = Offset(cx, h * 0.5f),
            radius = max(w, h) * 0.75f,
        ),
        topLeft = Offset.Zero,
        size = size,
    )
}

private fun DrawScope.drawScenery(o: Scenery, z: Float, proj: Proj, w: Float, pal: Pal, light: TimeOfDay) {
    val sc = proj.f / z
    val sx = proj.x(z, o.side * o.lateral)
    if (sx < -sc * 4f || sx > w + sc * 4f) return
    val by = proj.y(z)
    val fog = fogT(z)
    val alpha = min(1f, (Z_FAR - z) / 30f)
    fun col(c: Color) = lerp(c, pal.fog, fog).copy(alpha = alpha)

    when (o.kind) {
        0, 1, 2 -> {
            val trunkH = 1.9f * sc * o.size
            val trunkW = 0.26f * sc * o.size
            val r = 1.5f * sc * o.size
            // ombra
            drawOval(Color(0x33000000).copy(alpha = 0.2f * alpha), Offset(sx - r * 0.9f, by - r * 0.12f), Size(r * 1.8f, r * 0.30f))
            drawRect(col(Color(0xFF6B4A2B)), Offset(sx - trunkW / 2f, by - trunkH), Size(trunkW, trunkH))
            if (o.kind == 2) {
                val g0 = col(Color(0xFF184D28))
                val g1 = col(Color(0xFF1F5E30))
                val g2 = col(Color(0xFF2B7A3D))
                val h1 = 2.4f * sc * o.size
                val tri0 = Path().apply {
                    moveTo(sx, by - trunkH - h1 * 0.45f)
                    lineTo(sx - r * 1.25f, by - trunkH * 0.75f)
                    lineTo(sx + r * 1.25f, by - trunkH * 0.75f)
                    close()
                }
                val tri1 = Path().apply {
                    moveTo(sx, by - trunkH - h1 * 1.25f)
                    lineTo(sx - r * 0.75f, by - trunkH - h1 * 0.15f)
                    lineTo(sx + r * 0.75f, by - trunkH - h1 * 0.15f)
                    close()
                }
                val tri2 = Path().apply {
                    moveTo(sx, by - trunkH - h1 * 0.8f)
                    lineTo(sx - r * 1.0f, by - trunkH * 0.85f)
                    lineTo(sx + r * 1.0f, by - trunkH * 0.85f)
                    close()
                }
                drawPath(tri0, g0)
                drawPath(tri2, g1)
                drawPath(tri1, g2)
            } else {
                val dark = when (o.hue) {
                    0 -> Color(0xFF2F7A34)
                    1 -> Color(0xFF3E8E3A)
                    else -> Color(0xFF4C8B2E)
                }
                val cy = by - trunkH - r * 0.5f
                drawCircle(col(lerp(dark, Color.Black, 0.18f)), r, Offset(sx, cy))
                drawCircle(col(lerp(dark, Color.Black, 0.18f)), r * 0.8f, Offset(sx - r * 0.65f, cy + r * 0.35f))
                drawCircle(col(lerp(dark, Color.Black, 0.18f)), r * 0.8f, Offset(sx + r * 0.65f, cy + r * 0.35f))
                drawCircle(col(dark), r * 0.85f, Offset(sx - r * 0.12f, cy - r * 0.12f))
                drawCircle(col(lerp(dark, Color.White, 0.25f)), r * 0.5f, Offset(sx - r * 0.35f, cy - r * 0.35f))
            }
        }
        3 -> { // cespuglio
            val r = 0.55f * sc * o.size
            val g = col(if (o.hue == 0) Color(0xFF3A7D36) else Color(0xFF4A8F3C))
            drawCircle(g, r, Offset(sx, by - r * 0.7f))
            drawCircle(g, r * 0.8f, Offset(sx - r * 0.9f, by - r * 0.45f))
            drawCircle(g, r * 0.8f, Offset(sx + r * 0.9f, by - r * 0.45f))
        }
        4 -> { // sasso
            val r = 0.45f * sc * o.size
            drawOval(col(Color(0xFF8A8A84)), Offset(sx - r, by - r * 1.1f), Size(r * 2f, r * 1.3f))
            drawOval(col(Color(0xFFA9A9A2)), Offset(sx - r * 0.7f, by - r * 1.0f), Size(r * 1.2f, r * 0.7f))
        }
        5 -> { // fiori
            val r = 0.12f * sc * o.size
            val c = when (o.hue) {
                0 -> Color(0xFFF2D14B)
                1 -> Color(0xFFF2F2F2)
                else -> Color(0xFFD9548B)
            }
            for (j in 0..2) {
                drawCircle(col(c), max(r, 1.2f), Offset(sx + (j - 1) * r * 3.2f, by - r * (1.2f + j % 2)))
            }
        }
        6 -> { // barca a vela
            val bw = 2.4f * sc * o.size
            val hull = Path().apply {
                moveTo(sx - bw / 2f, by - bw * 0.12f)
                lineTo(sx + bw / 2f, by - bw * 0.12f)
                lineTo(sx + bw * 0.35f, by + bw * 0.08f)
                lineTo(sx - bw * 0.35f, by + bw * 0.08f)
                close()
            }
            drawPath(hull, col(if (o.hue == 0) Color(0xFFB03A2E) else Color(0xFFF2F2F2)))
            drawLine(
                col(Color(0xFF555555)),
                Offset(sx, by - bw * 0.12f),
                Offset(sx, by - bw * 1.0f),
                strokeWidth = max(1f, bw * 0.03f),
            )
            val sail = Path().apply {
                moveTo(sx + bw * 0.04f, by - bw * 0.98f)
                lineTo(sx + bw * 0.04f, by - bw * 0.18f)
                lineTo(sx + bw * 0.42f, by - bw * 0.18f)
                close()
            }
            drawPath(sail, col(Color(0xFFFAFAFA)))
        }
        7 -> { // lampione del lungolago
            val ph = 3.3f * sc
            val pw = max(1.5f, 0.09f * sc)
            val pole = col(Color(0xFF3A3F45))
            drawRect(pole, Offset(sx - pw / 2f, by - ph), Size(pw, ph))
            val lamp = Offset(sx + pw * 2f, by - ph)
            drawLine(pole, Offset(sx, by - ph), lamp, strokeWidth = pw)
            drawCircle(col(Color(0xFFFFF2B0)), max(2f, 0.16f * sc), lamp)
            if (light == TimeOfDay.SERA) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0x99FFE9A0), Color(0x00FFE9A0)),
                        center = lamp,
                        radius = 1.8f * sc,
                    ),
                    radius = 1.8f * sc,
                    center = lamp,
                )
            }
        }
        else -> { // 8: albero alto di bosco (tronco lungo e chioma in alto)
            val trunkH = 5.2f * sc * o.size
            val trunkW = 0.34f * sc * o.size
            val r = 1.9f * sc * o.size
            drawOval(Color(0x33000000).copy(alpha = 0.2f * alpha), Offset(sx - r * 0.9f, by - r * 0.12f), Size(r * 1.8f, r * 0.30f))
            drawRect(col(Color(0xFF5A4028)), Offset(sx - trunkW / 2f, by - trunkH), Size(trunkW, trunkH))
            val dark = when (o.hue) {
                0 -> Color(0xFF285F2E)
                1 -> Color(0xFF2F6B35)
                else -> Color(0xFF33702B)
            }
            val cy = by - trunkH
            drawCircle(col(dark), r, Offset(sx, cy))
            drawCircle(col(dark), r * 0.8f, Offset(sx - r * 0.7f, cy + r * 0.4f))
            drawCircle(col(dark), r * 0.8f, Offset(sx + r * 0.7f, cy + r * 0.4f))
            drawCircle(col(lerp(dark, Color.White, 0.18f)), r * 0.5f, Offset(sx - r * 0.3f, cy - r * 0.25f))
        }
    }
}

private fun DrawScope.ridge(color: Color, amp: Float, k: Float, shift: Float, base: Float, yHor: Float) {
    val w = size.width
    val p = Path()
    p.moveTo(0f, yHor + 4f)
    var x = 0f
    while (x <= w + 8f) {
        val y = yHor - base - amp * (
            0.55f * sin(x * k + shift) +
                0.30f * sin(x * k * 2.3f + shift * 1.7f) +
                0.15f * sin(x * k * 5.1f)
            )
        p.lineTo(x, y)
        x += 8f
    }
    p.lineTo(w + 8f, yHor + 4f)
    p.close()
    drawPath(p, color)
}
