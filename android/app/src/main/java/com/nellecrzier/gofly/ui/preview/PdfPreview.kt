package com.nellecrzier.gofly.ui.preview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nellecrzier.gofly.GItem
import com.nellecrzier.gofly.GoFileException
import com.nellecrzier.gofly.GofileApi
import com.nellecrzier.gofly.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private sealed interface PdfState {
    data object Loading : PdfState
    data class Ready(val document: PdfDocumentHandle) : PdfState
    data class Failed(val error: Throwable) : PdfState
}

/** Keadaan satu halaman: sedang dirender, siap, atau gagal dirender. */
private sealed interface PageState {
    data object Loading : PageState
    data class Ready(val bitmap: Bitmap) : PageState
    data object Failed : PageState
}

/**
 * PDF dirender ke Bitmap lewat `PdfRenderer` — tidak ada pustaka PDF pihak
 * ketiga dan tidak ada WebView. Setiap halaman dirender sesuai lebar layar,
 * lalu digeser horizontal.
 */
@Suppress("ProduceStateDoesNotAssignValue")
@Composable
internal fun PdfPreview(
    item: GItem,
    token: String,
    reduceMotion: Boolean,
    onOpenWith: (GItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var attempt by remember(item.id) { mutableIntStateOf(0) }

    val state by produceState<PdfState>(
        initialValue = PdfState.Loading,
        item.id,
        token,
        attempt,
    ) {
        value = PdfState.Loading
        value = withContext(Dispatchers.IO) {
            previewRunCatching {
                val document = openPdf(context, item, token)
                if (document.pageCount <= 0) {
                    document.close()
                    PdfState.Failed(IllegalStateException("PDF has no pages"))
                } else {
                    PdfState.Ready(document)
                }
            }.getOrElse { error -> PdfState.Failed(error) }
        }
    }

    DisposableEffect(state) {
        onDispose {
            (state as? PdfState.Ready)?.document?.close()
        }
    }

    when (val current = state) {
        PdfState.Loading -> PreviewLoading(
            reduceMotion = reduceMotion,
            modifier = modifier,
            label = stringResource(R.string.preview_loading),
        )

        is PdfState.Failed -> PreviewFailure(
            message = if (current.error is GoFileException) {
                stringResource(previewErrorRes(current.error))
            } else {
                stringResource(R.string.preview_error_pdf)
            },
            reduceMotion = reduceMotion,
            modifier = modifier,
            onRetry = { attempt++ },
            onOpenWith = { onOpenWith(item) },
        )

        is PdfState.Ready -> PdfPages(
            document = current.document,
            reduceMotion = reduceMotion,
            modifier = modifier,
        )
    }
}

@Suppress("ProduceStateDoesNotAssignValue")
@Composable
private fun PdfPages(
    document: PdfDocumentHandle,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val pageCount = document.pageCount
    val pagerState = rememberPagerState(initialPage = 0) { pageCount }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.coerceAtLeast(1)

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { index ->
            PdfPage(
                document = document,
                index = index,
                widthPx = widthPx,
                pageCount = pageCount,
                reduceMotion = reduceMotion,
            )
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
        ) {
            PreviewPageCounter(
                text = stringResource(
                    R.string.preview_page_counter,
                    pagerState.currentPage + 1,
                    pageCount,
                ),
                description = stringResource(
                    R.string.preview_page_position,
                    pagerState.currentPage + 1,
                    pageCount,
                ),
            )
        }
    }
}

@Suppress("ProduceStateDoesNotAssignValue")
@Composable
private fun PdfPage(
    document: PdfDocumentHandle,
    index: Int,
    widthPx: Int,
    pageCount: Int,
    reduceMotion: Boolean,
) {
    val state by produceState<PageState>(
        initialValue = PageState.Loading,
        document,
        index,
        widthPx,
    ) {
        value = withContext(Dispatchers.IO) {
            previewRunCatching { renderPdfPage(document, index, widthPx) }
                .fold(
                    onSuccess = { bitmap -> PageState.Ready(bitmap) },
                    onFailure = { PageState.Failed },
                )
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val current = state) {
            PageState.Loading -> PreviewLoading(reduceMotion = reduceMotion)
            PageState.Failed -> PreviewFailure(
                message = stringResource(R.string.preview_error_pdf),
                reduceMotion = reduceMotion,
            )
            is PageState.Ready -> Image(
                bitmap = current.bitmap.asImageBitmap(),
                contentDescription = stringResource(
                    R.string.preview_page_position,
                    index + 1,
                    pageCount,
                ),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, end = 12.dp, top = 84.dp, bottom = 64.dp),
            )
        }
    }
}

/**
 * PDF berpindah thread dengan aman: `synchronized` memastikan satu halaman
 * dirender pada satu waktu dan penutupan menunggu render yang sedang jalan.
 * Bitmap tidak didaur ulang manual supaya tidak ada gambar yang dipakai
 * setelah di-recycle; GC yang mengambilnya saat halaman keluar komposisi.
 */
private class PdfDocumentHandle(
    private val renderer: PdfRenderer,
    private val descriptor: ParcelFileDescriptor,
    private val file: File,
) {
    val pageCount: Int = renderer.pageCount
    private val lock = Any()

    fun <T> withRenderer(block: (PdfRenderer) -> T): T = synchronized(lock) { block(renderer) }

    fun close() {
        synchronized(lock) {
            runCatching { renderer.close() }
            runCatching { descriptor.close() }
            runCatching { file.delete() }
        }
    }
}

private fun openPdf(context: Context, item: GItem, token: String): PdfDocumentHandle {
    val file: File = GofileApi.downloadToTemp(context, item.link, item.name, token)
    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    return try {
        PdfDocumentHandle(PdfRenderer(descriptor), descriptor, file)
    } catch (error: Throwable) {
        runCatching { descriptor.close() }
        runCatching { file.delete() }
        throw error
    }
}

private fun renderPdfPage(
    document: PdfDocumentHandle,
    index: Int,
    widthPx: Int,
): Bitmap = document.withRenderer { renderer ->
    renderer.openPage(index).use { page ->
        val targetWidth = widthPx.coerceIn(240, 2048)
        val ratio = page.height.toFloat() / page.width.coerceAtLeast(1).toFloat()
        val targetHeight = (targetWidth * ratio).toInt().coerceIn(1, 4096)
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bitmap
    }
}
