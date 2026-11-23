package com.nellecrzier.gofly.ui.share

import com.nellecrzier.gofly.ui.components.MorphIcon
import com.nellecrzier.gofly.ui.components.AppLoadingIndicator
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.nellecrzier.gofly.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Status pembuatan matriks QR; teks tautan tetap ditampilkan apa pun hasilnya. */
private sealed interface ShareQrState {
    data object Building : ShareQrState
    data object Failed : ShareQrState
    data class Ready(val bitmap: ImageBitmap) : ShareQrState
}

/**
 * Lembar kode QR: kotak putih berkontras tinggi dengan quiet zone, tautan
 * monospace yang bisa dipilih, tombol salin, dan petunjuk.
 *
 * Matriks dibuat di perangkat dengan ZXing (koreksi galat M), jadi tidak ada
 * data yang dikirim ke mana pun.
 */
@Suppress("ProduceStateDoesNotAssignValue")
@Composable
fun ShareQrSheet(
    link: String,
    title: String,
    onDismiss: () -> Unit,
    reduceMotion: Boolean,
) {
    val context = LocalContext.current
    val linkLabel = stringResource(R.string.share_link_label)
    var copied by remember(link) { mutableStateOf(false) }

    val qr by produceState<ShareQrState>(
        initialValue = ShareQrState.Building,
        key1 = link,
    ) {
        val result = withContext(Dispatchers.Default) {
            val bitmap = buildShareQr(link)
            if (bitmap == null) ShareQrState.Failed else ShareQrState.Ready(bitmap)
        }
        value = result
    }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    ShareSheetFrame(onDismiss = onDismiss) {
        ShareContentFade(reduceMotion) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.share_qr_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = middleEllipsis(title, 42),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                FilledTonalIconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.share_close),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val side = minOf(maxWidth * 0.94f, 292.dp)
                Box(
                    Modifier
                        .size(side)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when (val state = qr) {
                        is ShareQrState.Ready -> Image(
                            bitmap = state.bitmap,
                            contentDescription = stringResource(R.string.share_qr_scan_desc, title),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            filterQuality = FilterQuality.None,
                        )

                        ShareQrState.Failed -> Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFF9A1B12),
                            modifier = Modifier.size(36.dp),
                        )

                        ShareQrState.Building -> AppLoadingIndicator(
                            size = 30.dp,
                            color = Color(0xFF141414),
                            reduceMotion = reduceMotion,
                        )
                    }
                }
            }

            if (qr is ShareQrState.Failed) {
                Text(
                    text = stringResource(R.string.share_qr_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 10.dp, start = 4.dp),
                )
            }

            Spacer(Modifier.height(14.dp))
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                SelectionContainer {
                    Text(
                        text = link,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 132.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { copied = copyToClipboard(context, link, linkLabel) },
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                MorphIcon(
                    first = Icons.Filled.ContentCopy,
                    second = Icons.Filled.Check,
                    showSecond = copied,
                    contentDescription = null,
                    reduceMotion = reduceMotion,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(
                        if (copied) R.string.share_action_copied else R.string.share_action_copy,
                    ),
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.share_qr_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Text(
                text = stringResource(R.string.share_qr_local),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp),
            )
        }
    }
}

/* ------------------------------- matriks QR ------------------------------ */

/**
 * Bitmap hitam-putih dari [QRCodeWriter]: koreksi galat M, quiet zone 4 modul,
 * ukuran tetap 512px supaya tajam saat diperbesar dengan FilterQuality.None.
 * `null` berarti tautan terlalu panjang atau tidak bisa dikodekan.
 */
private fun buildShareQr(link: String): ImageBitmap? {
    if (link.isBlank()) return null
    return try {
        val hints = mapOf<EncodeHintType, Any>(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 4,
        )
        val target = 512
        val matrix = QRCodeWriter().encode(link, BarcodeFormat.QR_CODE, target, target, hints)
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                pixels[row + x] = if (matrix.get(x, y)) INK else PAPER
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.asImageBitmap()
    } catch (error: Exception) {
        null
    }
}

private val INK = 0xFF000000.toInt()
private val PAPER = 0xFFFFFFFF.toInt()
