@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.nellecrzier.gofileexpressive.ui.motion

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Helper gerak di atas `MaterialTheme.motionScheme` Expressive.
 *
 * Spesifikasi spatial dipakai untuk posisi/bentuk/skala; spesifikasi effects
 * untuk alfa dan warna. Tidak ada angka spring yang ditulis di layar.
 */

/** Redupkan gerak bila Setelan meminta, atau skala animator sistem = 0. */
@Composable
fun rememberReducedMotion(settingReduced: Boolean = false): Boolean {
    val context = LocalContext.current
    val systemScale = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f)
    }
    return settingReduced || systemScale == 0f
}

/** Skala tekan halus memakai spesifikasi spatial tercepat. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    reduceMotion: Boolean,
    pressedScale: Float = 0.96f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val spec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) pressedScale else 1f,
        animationSpec = spec,
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Bentuk yang memipih saat ditekan (pil → sudut lebih kecil) memakai
 * spesifikasi spatial tercepat. Kembali ke bentuk semula saat dilepas.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun pressMorphShape(
    interactionSource: MutableInteractionSource,
    reduceMotion: Boolean,
    restRadius: Dp = 28.dp,
    pressedRadius: Dp = 12.dp,
): Shape {
    val isPressed by interactionSource.collectIsPressedAsState()
    val spec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val density = LocalDensity.current
    val restPx = with(density) { restRadius.toPx() }
    val pressedPx = with(density) { pressedRadius.toPx() }
    val radius by animateFloatAsState(
        targetValue = if (isPressed && !reduceMotion) pressedPx else restPx,
        animationSpec = spec,
        label = "pressMorphShape",
    )
    return RoundedCornerShape(radius)
}

/**
 * Masuk bertahap untuk item pertama sebuah daftar: alfa + geser 16dp + skala
 * 0.96 → 1. Stagger 28 ms per item, dibatasi 10 item pertama.
 * Saat gerak diredupkan, item langsung tampil tanpa stagger.
 */
@Composable
fun Modifier.staggeredEnter(
    index: Int,
    reduceMotion: Boolean,
    visible: Boolean = true,
): Modifier {
    val delay = if (reduceMotion) 0 else (index.coerceAtMost(10) * 28)
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 100 else 220, delayMillis = delay),
        label = "staggerAlpha",
    )
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 100 else 260, delayMillis = delay),
        label = "staggerProgress",
    )
    return graphicsLayer {
        this.alpha = alpha
        translationY = 16.dp.toPx() * (1f - progress)
        val scale = 0.96f + 0.04f * progress
        scaleX = scale
        scaleY = scale
    }
}

/** Akses singkat ke skema gerak Material 3 Expressive. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val motionScheme: MotionScheme
    @Composable @ReadOnlyComposable get() = MaterialTheme.motionScheme
