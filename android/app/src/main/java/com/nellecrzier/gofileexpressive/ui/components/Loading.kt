package com.nellecrzier.gofileexpressive.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
