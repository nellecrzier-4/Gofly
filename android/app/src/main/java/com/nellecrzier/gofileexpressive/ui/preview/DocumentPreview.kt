package com.nellecrzier.gofileexpressive.ui.preview

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nellecrzier.gofileexpressive.GItem
import com.nellecrzier.gofileexpressive.GofileApi
import com.nellecrzier.gofileexpressive.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Hasil unduh + urai isi teks. */
private sealed interface DocumentState {
    data object Loading : DocumentState
    data object TooLarge : DocumentState
    data object NotText : DocumentState
    data class Failed(val error: Throwable) : DocumentState
    data class Ready(val text: String) : DocumentState
}

@Composable
private fun codeTextStyle(): TextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 13.sp,
    lineHeight = 20.sp,
    color = MaterialTheme.colorScheme.onSurface,
)

@Composable
private fun gutterTextStyle(): TextStyle = codeTextStyle().copy(
    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
)

/**
 * Teks, kode, dan Markdown dirender langsung di Compose. Di atas 1 MB file
 * tidak diunduh: pengguna mendapat penjelasan dan jalan "Buka dengan".
 */
@Suppress("ProduceStateDoesNotAssignValue")
@Composable
internal fun DocumentPreview(
    item: GItem,
    token: String,
    kind: PreviewKind,
    reduceMotion: Boolean,
    lineNumbers: Boolean,
    onOpenWith: (GItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var attempt by remember(item.id) { mutableIntStateOf(0) }

    val state by produceState<DocumentState>(
        initialValue = DocumentState.Loading,
        item.id,
        token,
        attempt,
    ) {
        value = DocumentState.Loading
        value = withContext(Dispatchers.IO) {
            if (item.size > PREVIEW_TEXT_LIMIT_BYTES) return@withContext DocumentState.TooLarge
            previewRunCatching {
                val bytes = GofileApi.fetchBytes(item.link, token)
                if (bytes.size > PREVIEW_TEXT_LIMIT_BYTES) {
                    DocumentState.TooLarge
                } else {
                    val text = previewDecodeText(bytes)
                    if (text == null) DocumentState.NotText else DocumentState.Ready(text)
                }
            }.getOrElse { error -> DocumentState.Failed(error) }
        }
    }

    when (val current = state) {
        DocumentState.Loading -> PreviewLoading(
            reduceMotion = reduceMotion,
            modifier = modifier,
            label = stringResource(R.string.preview_loading),
        )

        DocumentState.TooLarge -> PreviewFailure(
            title = stringResource(R.string.preview_too_large_title),
            message = stringResource(
                R.string.preview_too_large_body,
                stringResource(R.string.preview_text_limit),
            ),
            reduceMotion = reduceMotion,
            modifier = modifier,
            onOpenWith = { onOpenWith(item) },
        )

        DocumentState.NotText -> PreviewFailure(
            message = stringResource(R.string.preview_not_text),
            reduceMotion = reduceMotion,
            modifier = modifier,
            onRetry = { attempt++ },
            onOpenWith = { onOpenWith(item) },
        )

        is DocumentState.Failed -> PreviewFailure(
            message = stringResource(previewErrorRes(current.error)),
            reduceMotion = reduceMotion,
            modifier = modifier,
            onRetry = { attempt++ },
            onOpenWith = { onOpenWith(item) },
        )

        is DocumentState.Ready -> {
            if (current.text.isBlank()) {
                PreviewFailure(
                    title = stringResource(R.string.preview_text_empty_title),
                    message = stringResource(R.string.preview_text_empty),
                    reduceMotion = reduceMotion,
                    modifier = modifier,
                    onOpenWith = { onOpenWith(item) },
                )
            } else {
                // Teks dan kode sama-sama monospace: log, CSV, dan berkas
                // konfigurasi tetap sejajar kolomnya. Markdown memakai
                // tipografi proporsional karena isinya prosa.
                if (kind == PreviewKind.MARKDOWN) {
                    MarkdownBody(current.text, modifier)
                } else {
                    CodeBody(current.text, lineNumbers, modifier)
                }
            }
        }
    }
}

@Composable
private fun MarkdownBody(text: String, modifier: Modifier = Modifier) {
    val colors = MarkdownColors(
        text = MaterialTheme.colorScheme.onSurface,
        muted = MaterialTheme.colorScheme.onSurfaceVariant,
        faint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        accent = MaterialTheme.colorScheme.primary,
        codeFill = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
    val annotated = remember(text, colors) { buildMarkdownAnnotatedString(text, colors) }
    val scroll = rememberScrollState()
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        SelectionContainer {
            Text(
                text = annotated,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 27.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            )
        }
    }
}

/**
 * Kode: satu kolom nomor baris (opsional) yang ikut menggulir vertikal, dan
 * teks monospace yang menggulir horizontal. Nomor tetap segaris karena
 * keduanya memakai gaya huruf yang sama dan tidak membungkus baris.
 */
@Composable
private fun CodeBody(
    text: String,
    lineNumbers: Boolean,
    modifier: Modifier = Modifier,
) {
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()
    val lineCount = remember(text) { text.count { it == '\n' } + 1 }
    val numbers = remember(lineCount) { (1..lineCount).joinToString("\n") }
    val codeStyle = codeTextStyle()
    val gutterStyle = gutterTextStyle()

    SelectionContainer {
        Row(
            modifier
                .fillMaxSize()
                .verticalScroll(vertical),
        ) {
            if (lineNumbers) {
                Text(
                    text = numbers,
                    style = gutterStyle,
                    softWrap = false,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(start = 14.dp, end = 12.dp, top = 16.dp, bottom = 28.dp),
                )
            }
            Box(
                Modifier
                    .weight(1f)
                    .horizontalScroll(horizontal),
            ) {
                Text(
                    text = text,
                    style = codeStyle,
                    softWrap = false,
                    modifier = Modifier.padding(
                        start = if (lineNumbers) 0.dp else 16.dp,
                        end = 24.dp,
                        top = 16.dp,
                        bottom = 28.dp,
                    ),
                )
            }
        }
    }
}
