@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.nellecrzier.gofly.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.nellecrzier.gofly.ui.motion.motionScheme

/**
 * Ikon yang berganti di tempat dengan gerak Expressive.
 *
 * Ikon lama berputar 90° sambil mengecil dan memudar, ikon baru masuk dari
 * -90° dengan skala 0.6 → 1. Putaran dan skala memakai spesifikasi spatial
 * tercepat; alfa memakai spesifikasi effects tercepat supaya pudarnya tidak
 * ikut memantul. Saat [reduceMotion] aktif hanya ada silang-pudar 90 ms tanpa
 * putaran maupun skala.
 *
 * [first] tampil saat [showSecond] `false`, [second] saat `true`. Ukuran dan
 * warna datang dari pemanggil lewat [modifier] dan [tint], jadi kedua ikon
 * selalu memakai kotak yang sama dengan `Icon` biasa.
 */
@Composable
fun MorphIcon(
    first: ImageVector,
    second: ImageVector,
    showSecond: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
    tint: Color = LocalContentColor.current,
) {
    val target = if (showSecond) 1f else 0f
    // Posisi, putaran, dan skala: spatial. Alfa: effects.
    val spatial = motionScheme.fastSpatialSpec<Float>()
    val effects = motionScheme.fastEffectsSpec<Float>()
    val snap = tween<Float>(0)
    val quickFade = tween<Float>(90)
    val move by animateFloatAsState(
        targetValue = target,
        animationSpec = if (reduceMotion) snap else spatial,
        label = "morphMove",
    )
    val fade by animateFloatAsState(
        targetValue = target,
        animationSpec = if (reduceMotion) quickFade else effects,
        label = "morphFade",
    )

    val label = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    Box(modifier.then(label)) {
        Icon(
            imageVector = first,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = 1f - fade
                    if (!reduceMotion) {
                        rotationZ = 90f * move
                        val scale = 1f - 0.4f * move
                        scaleX = scale
                        scaleY = scale
                    }
                },
        )
        Icon(
            imageVector = second,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = fade
                    if (!reduceMotion) {
                        rotationZ = -90f * move
                        val scale = 0.6f + 0.4f * move
                        scaleX = scale
                        scaleY = scale
                    }
                },
        )
    }
}
