package com.nellecrzier.gofileexpressive.ui.components

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Indikator muat Material 3 Expressive: poligon yang berputar sambil
 * bermorfosis. Saat gerak diredupkan, indikator berhenti sebagai pil statis
 * agar tidak ada animasi yang tersisa.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    reduceMotion: Boolean = false,
) {
    if (reduceMotion) {
        Canvas(modifier.size(size)) {
            val stroke = this.size.minDimension * 0.18f
            drawRoundRect(
                color = color.copy(alpha = 0.7f),
                topLeft = Offset(stroke, this.size.height * 0.3f),
                size = Size(this.size.width - stroke * 2f, this.size.height * 0.4f),
                cornerRadius = CornerRadius(this.size.height * 0.2f),
            )
        }
    } else {
        LoadingIndicator(
            modifier = modifier.size(size),
            color = color,
        )
    }
}

/**
 * Peredam status "sedang memuat" agar indikator tidak berkedip.
 *
 * Indikator baru ditampilkan bila pemuatan berlangsung lebih dari [delayMs],
 * lalu dipertahankan sedikitnya [minVisibleMs] sejak pertama terlihat. Jadi
 * pekerjaan yang selesai cepat tidak memunculkan kilatan, dan pekerjaan yang
 * sempat muncul tidak langsung hilang.
 *
 * [content] selalu dikomposisikan (tidak pernah dilewati) dan menerima status
 * yang sudah diredam; parameter Boolean-nya yang dipakai untuk menggambar
 * indikator, sehingga pemanggil menjaga tinggi/tata letaknya sendiri dan tidak
 * ada lompatan saat indikator muncul. Peredaman ini hanya soal waktu, bukan
 * gerak: `reduceMotion` tetap ditangani oleh indikator di dalam [content].
 */
@Composable
fun DebouncedLoading(
    loading: Boolean,
    minVisibleMs: Long = 500,
    delayMs: Long = 300,
    content: @Composable (Boolean) -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    var visibleSince by remember { mutableLongStateOf(0L) }

    LaunchedEffect(loading, delayMs, minVisibleMs) {
        if (loading) {
            // Sudah terlihat (mis. percobaan ulang): jangan sembunyikan dulu,
            // supaya tidak ada kedipan di antara dua pemuatan.
            if (!visible) {
                delay(delayMs)
                visibleSince = SystemClock.uptimeMillis()
                visible = true
            }
        } else if (visible) {
            val shown = SystemClock.uptimeMillis() - visibleSince
            if (shown < minVisibleMs) delay(minVisibleMs - shown)
            visible = false
        }
    }

    content(visible)
}
