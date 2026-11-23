@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.nellecrzier.gofileexpressive.ui.account

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nellecrzier.gofileexpressive.R
import com.nellecrzier.gofileexpressive.ui.motion.motionScheme
import com.nellecrzier.gofileexpressive.ui.readableSize
import kotlin.math.roundToInt

/**
 * Bilah penyimpanan Material 3: rel track membulat, isi aksen membulat. Tidak
 * memakai pustaka grafik — satu Canvas, dua bentuk.
 */
@Composable
internal fun StorageBar(
    fraction: Float?,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
    description: String? = null,
) {
    val target = fraction?.coerceIn(0f, 1f) ?: 0f
    val reveal = remember { Animatable(if (reduceMotion) target else 0f) }
    val spec = motionScheme.defaultSpatialSpec<Float>()

    LaunchedEffect(target, reduceMotion) {
        if (reduceMotion) {
            reveal.snapTo(target)
        } else {
            reveal.animateTo(target, spec)
        }
    }

    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val outline = MaterialTheme.colorScheme.outline
    val accent = MaterialTheme.colorScheme.primary

    Canvas(
        modifier
            .fillMaxWidth()
            .height(12.dp)
            .semantics {
                if (description != null) contentDescription = description
            },
    ) {
        val radius = CornerRadius(size.height / 2f)
        // Rel: satu bidang track, tanpa lapisan tambahan.
        drawRoundRect(
            color = track,
            cornerRadius = radius,
            size = Size(size.width, size.height),
        )
        drawRoundRect(
            color = outline,
            cornerRadius = radius,
            size = Size(size.width, size.height),
            style = Stroke(width = 0.8.dp.toPx()),
        )
        val filled = size.width * reveal.value
        if (filled > 0.5f) {
            drawRoundRect(
                color = accent,
                cornerRadius = radius,
                size = Size(filled.coerceAtLeast(size.height), size.height),
            )
        }
    }
}

/** Warna potongan: satu aksen dengan alfa bertingkat, bukan pelangi. */
internal fun sliceColor(index: Int, accent: Color): Color = accent.copy(
    alpha = when (index) {
        0 -> 1f
        1 -> 0.74f
        2 -> 0.52f
        3 -> 0.36f
        4 -> 0.24f
        else -> 0.15f
    },
)

/**
 * Grafik batang bertumpuk per jenis berkas, digambar di Canvas. Warna selalu
 * disertai label di legenda, jadi jenis tidak dibedakan warna saja.
 */
@Composable
internal fun TypeBreakdownChart(
    slices: List<BreakdownSlice>,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
    description: String? = null,
) {
    val total = slices.sumOf { it.bytes }.coerceAtLeast(1L)
    val reveal = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val spec = motionScheme.defaultSpatialSpec<Float>()

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) reveal.snapTo(1f) else reveal.animateTo(1f, spec)
    }

    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val accent = MaterialTheme.colorScheme.primary
    val sliceColors = remember(slices, accent) {
        List(slices.size) { index -> sliceColor(index, accent) }
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(16.dp)
            .semantics {
                if (description != null) contentDescription = description
            },
    ) {
        val radius = CornerRadius(size.height / 2f)
        val clip = Path().apply {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    cornerRadius = radius,
                ),
            )
        }
        clipPath(clip) {
            drawRect(color = track, size = size)
            var x = 0f
            slices.forEachIndexed { index, slice ->
                val width = size.width * (slice.bytes.toFloat() / total.toFloat()) * reveal.value
                if (width <= 0f) return@forEachIndexed
                drawRect(
                    color = sliceColors.getOrElse(index) { accent },
                    topLeft = androidx.compose.ui.geometry.Offset(x, 0f),
                    size = Size(width, size.height),
                )
                x += width
            }
        }
    }
}

/** Legenda grafik: titik warna, label, ukuran, dan persentase. */
@Composable
internal fun BreakdownLegend(
    slices: List<BreakdownSlice>,
    modifier: Modifier = Modifier,
) {
    val total = slices.sumOf { it.bytes }.coerceAtLeast(1L)
    val accent = MaterialTheme.colorScheme.primary
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        slices.forEachIndexed { index, slice ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(sliceColor(index, accent)),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = slice.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(
                        R.string.settings_account_breakdown_share,
                        ((slice.bytes.toDouble() / total.toDouble()) * 100).roundToInt(),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = readableSize(slice.bytes),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
