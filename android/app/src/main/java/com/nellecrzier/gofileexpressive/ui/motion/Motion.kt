package com.nellecrzier.gofileexpressive.ui.motion

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.nellecrzier.gofileexpressive.ui.expressive.defaultSpatial
import com.nellecrzier.gofileexpressive.ui.expressive.fastSpatial

/**
 * Helper gerak di atas `LocalMotionScheme` (tiruan MotionScheme Expressive).
 * Tidak ada angka spring di layar: semua lewat spesifikasi spatial/effects.
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
@Composable
fun Modifier.m3PressScale(
    interactionSource: MutableInteractionSource,
    reduceMotion: Boolean,
    pressedScale: Float = 0.96f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val spec: FiniteAnimationSpec<Float> = fastSpatial()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) pressedScale else 1f,
        animationSpec = spec,
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Nilai yang mengikuti target dengan spesifikasi spatial default. */
@Composable
fun rememberSpatialValue(target: Float, reduceMotion: Boolean): Float {
    val spec = defaultSpatial()
    val anim = remember { Animatable(target) }
    LaunchedEffect(target, reduceMotion) {
        if (reduceMotion) anim.snapTo(target) else anim.animateTo(target, spec)
    }
    return if (reduceMotion) target else anim.value
}
