package com.nellecrzier.gofileexpressive.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Indikator loading bergaya Material 3 Expressive: bentuk yang berdenyut dan
 * berputar, bukan cincin berputar biasa. Digambar di satu Canvas supaya tidak
 * menambah node layout maupun animasi per bagian.
 */
@Composable
fun ExpressiveLoader(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    reduceMotion: Boolean = false,
    strokeOnly: Boolean = false,
) {
    if (reduceMotion) {
        Canvas(modifier.size(size)) {
            drawBlob(
                progress = 0.35f,
                color = color,
                strokeOnly = strokeOnly,
            )
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "expressiveLoader")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "loaderSpin",
    )
    val morph by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(760, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "loaderMorph",
    )

    Canvas(
        modifier
            .size(size)
            .graphicsLayer { rotationZ = spin },
    ) {
        drawBlob(progress = morph, color = color, strokeOnly = strokeOnly)
    }
}

/** Enam titik dengan radius termodulasi, disambung kurva halus jadi satu blob. */
private fun DrawScope.drawBlob(
    progress: Float,
    color: Color,
    strokeOnly: Boolean,
) {
    val side = size.minDimension
    val radius = side / 2f * 0.82f
    val center = Offset(size.width / 2f, size.height / 2f)
    val vertices = 6
    val points = List(vertices) { index ->
        val angle = (index.toFloat() / vertices) * 2f * PI.toFloat() - PI.toFloat() / 2f
        val wobble = 1f + 0.16f * progress * cos(3f * angle)
        Offset(
            x = center.x + cos(angle) * radius * wobble,
            y = center.y + sin(angle) * radius * wobble,
        )
    }

    val path = Path()
    val firstMid = midpoint(points[0], points[1])
    path.moveTo(firstMid.x, firstMid.y)
    for (index in points.indices) {
        val control = points[(index + 1) % vertices]
        val next = points[(index + 2) % vertices]
        val mid = midpoint(control, next)
        path.quadraticTo(control.x, control.y, mid.x, mid.y)
    }
    path.close()

    if (strokeOnly) {
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = side * 0.10f),
        )
    } else {
        drawPath(path = path, color = color)
    }
}

private fun midpoint(a: Offset, b: Offset): Offset =
    Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
