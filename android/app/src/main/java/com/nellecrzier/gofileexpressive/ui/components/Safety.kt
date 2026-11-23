@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.nellecrzier.gofileexpressive.ui.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nellecrzier.gofileexpressive.ui.motion.motionScheme
import kotlinx.coroutines.launch

/**
 * `HapticFeedbackType.Confirm` dan `Reject` dipetakan ke konstanta platform
 * `HapticFeedbackConstants.CONFIRM`/`REJECT` yang baru ada di Android 11
 * (API 30), jadi keduanya dijaga dengan [CONFIRM_REJECT_MIN_SDK].
 *
 * Di bawah API 30 konfirmasi tetap memakai LongPress seperti sebelumnya supaya
 * perangkat lama tidak kehilangan umpan balik; penolakan tidak punya padanan
 * lama, jadi dibiarkan senyap.
 */
private const val CONFIRM_REJECT_MIN_SDK = 30

/** Getaran "berhasil" setelah konfirmasi tahan selesai (atau lewat TalkBack). */
private fun HapticFeedback.confirmHaptic() {
    if (Build.VERSION.SDK_INT >= CONFIRM_REJECT_MIN_SDK) {
        performHapticFeedback(HapticFeedbackType.Confirm)
    } else {
        performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

/** Getaran "ditolak" saat ketukan pendek tidak mencapai ambang tahan. */
private fun HapticFeedback.rejectHaptic() {
    if (Build.VERSION.SDK_INT >= CONFIRM_REJECT_MIN_SDK) {
        performHapticFeedback(HapticFeedbackType.Reject)
    }
}

/**
 * Tombol konfirmasi tahan untuk aksi merusak. Ketukan pendek ditolak; isi
 * tombol terisi selama 900 ms sebelum aksi dijalankan. Warna dan bentuk
 * memakai token Material 3, bukan bahasa visual lama.
 */
@Composable
fun HoldToConfirmButton(
    label: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    val resetSpec = motionScheme.fastEffectsSpec<Float>()
    val accent = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.primaryContainer
    val edge = MaterialTheme.colorScheme.outlineVariant
    val labelColor = if (enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(CircleShape)
                .background(container)
                .border(1.dp, edge, CircleShape)
                .semantics {
                    contentDescription = label
                    onClick(label) {
                        haptic.confirmHaptic()
                        onConfirm()
                        true
                    }
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onPress = {
                            val job = scope.launch {
                                progress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(900, easing = LinearEasing),
                                )
                            }
                            val released = tryAwaitRelease()
                            job.cancel()
                            if (released && progress.value >= 0.98f) {
                                haptic.confirmHaptic()
                                onConfirm()
                            } else {
                                if (released) haptic.rejectHaptic()
                                progress.animateTo(0f, resetSpec)
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    color = accent.copy(alpha = 0.24f),
                    size = Size(size.width * progress.value, size.height),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = labelColor,
            )
        }
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            )
        }
    }
}
