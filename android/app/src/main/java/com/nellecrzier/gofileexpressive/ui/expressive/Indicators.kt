package com.nellecrzier.gofileexpressive.ui.expressive

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Indikator muat M3 Expressive tiruan: bentuk yang berputar sambil bermorfosis
 * (persegi membulat → belah ketupat → lingkaran), lalu kembali. Warna memakai
 * `primary` / `primaryContainer`. Saat gerak diredupkan, indikator berhenti
 * menjadi bentuk statis.
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    reduceMotion: Boolean = false,
) {
    val phase = rotationPhase()
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val side = this.size.minDimension
            val stroke = side * 0.11f
            if (reduceMotion) {
                drawCircle(
                    color = color,
                    radius = side * 0.22f,
                    style = Stroke(width = stroke),
                )
                return@Canvas
            }
            val t = phase
            val corners = 6f - 2f * (0.5f + 0.5f * sin(t * PI.toFloat()).toFloat())
            val rect = Size(side * 0.62f, side * 0.62f)
            rotate(degrees = t * 360f) {
                drawRoundRect(
                    color = containerColor,
                    topLeft = Offset((side - rect.width) / 2f, (side - rect.height) / 2f),
                    size = rect,
                    cornerRadius = CornerRadius(rect.width / 2f * (1f - corners / 6f)),
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset((side - rect.width) / 2f, (side - rect.height) / 2f),
                    size = rect,
                    cornerRadius = CornerRadius(rect.width / 2f * (1f - corners / 6f)),
                    style = Stroke(width = stroke),
                )
            }
        }
    }
}

/** Indikator di dalam kontainer tonal, sepadan `ContainedLoadingIndicator`. */
@Composable
fun ContainedLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    reduceMotion: Boolean = false,
) {
    androidx.compose.material3.Surface(
        modifier = modifier.size(size),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
            LoadingIndicator(size = size * 0.6f, reduceMotion = reduceMotion)
        }
    }
}

@Composable
private fun rotationPhase(): Float {
    val transition = rememberInfiniteTransition(label = "loadingIndicator")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    return phase
}

/**
 * Progres bergelombang linear: amplitudo naik-turun saat berjalan, datar saat
 * dijeda/selesai, dengan track, celah, dan titik akhir.
 */
@Composable
fun LinearWavyProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    reduceMotion: Boolean = false,
    amplitude: Float = 0.5f,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    stopIndicatorColor: Color = MaterialTheme.colorScheme.primary,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Canvas(modifier) {
        val h = size.height
        val w = size.width
        val stroke = h * 0.28f
        val mid = h / 2f
        // Track.
        drawLine(
            color = trackColor,
            start = Offset(stroke / 2f, mid),
            end = Offset(w - stroke / 2f, mid),
            strokeWidth = stroke,
        )
        if (clamped <= 0f) return@Canvas
        val wave = if (active && !reduceMotion) amplitude else 0f
        val path = Path()
        val steps = 48
        for (i in 0..steps) {
            val x = (w - stroke) * (i.toFloat() / steps) * clamped + stroke / 2f
            val y = mid + sin((i.toFloat() / steps) * 3f * PI.toFloat()) * (h * 0.28f * wave)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        // Titik akhir.
        val endX = (w - stroke) * clamped + stroke / 2f
        drawCircle(stopIndicatorColor, radius = stroke * 0.8f, center = Offset(endX, mid))
    }
}

/** Progres bergelombang melingkar. */
@Composable
fun CircularWavyProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    reduceMotion: Boolean = false,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Canvas(modifier) {
        val stroke = size.minDimension * 0.11f
        val radius = size.minDimension / 2f - stroke
        drawCircle(color = trackColor, radius = radius, style = Stroke(stroke))
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * clamped,
            useCenter = false,
            style = Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )
    }
}

