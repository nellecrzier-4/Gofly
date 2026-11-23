package com.nellecrzier.gofileexpressive.ui.preview

import com.nellecrzier.gofileexpressive.ui.components.AppLoadingIndicator
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil.compose.SubcomposeAsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.nellecrzier.gofileexpressive.GItem
import com.nellecrzier.gofileexpressive.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.6f
private const val SLIDESHOW_INTERVAL_MS = 3000L

/**
 * Pager gambar layar penuh: geser antar gambar, cubit untuk zoom, geser saat
 * zoom, ketuk dua kali untuk zoom/pulihkan, dan geser ke bawah untuk menutup.
 * Slideshow berjalan hanya jika [reduceMotion] mati.
 */
@Composable
internal fun ImagePreview(
    item: GItem,
    album: List<GItem>,
    token: String,
    reduceMotion: Boolean,
    slideshow: Boolean,
    slideshowPaused: Boolean,
    onClose: () -> Unit,
    onOpenWith: (GItem) -> Unit,
    modifier: Modifier = Modifier,
    indicatorBottomPadding: Dp = 0.dp,
) {
    val images = remember(item, album) { previewAlbumFor(item, album, PreviewKind.IMAGE) }
    val startIndex = remember(images, item) {
        images.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = startIndex) { images.size }
    var zoomed by remember(item.id) { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        zoomed = false
    }

    // Slideshow: satu gambar tiap 3 detik, berhenti menggeser sendiri saat
    // pengguna menyentuh, sedang zoom, atau saat menu/lembar detail terbuka.
    LaunchedEffect(
        slideshow,
        reduceMotion,
        slideshowPaused,
        zoomed,
        pagerState.currentPage,
        pagerState.isScrollInProgress,
        images.size,
    ) {
        if (!slideshow || reduceMotion || slideshowPaused) return@LaunchedEffect
        if (zoomed || pagerState.isScrollInProgress) return@LaunchedEffect
        if (images.size < 2) return@LaunchedEffect
        delay(SLIDESHOW_INTERVAL_MS)
        pagerState.animateScrollToPage((pagerState.currentPage + 1) % images.size)
    }

    Box(modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            userScrollEnabled = !zoomed,
            key = { index -> images[index].id },
        ) { page ->
            ImagePage(
                item = images[page],
                token = token,
                reduceMotion = reduceMotion,
                isCurrent = page == pagerState.currentPage,
                onZoomChanged = { zoom -> if (page == pagerState.currentPage) zoomed = zoom },
                onDismiss = onClose,
                onOpenWith = onOpenWith,
            )
        }

        if (images.size > 1) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = indicatorBottomPadding)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                if (images.size <= 8) {
                    PagerDots(count = images.size, index = pagerState.currentPage)
                } else {
                    PreviewPageCounter(
                        text = stringResource(
                            R.string.preview_page_counter,
                            pagerState.currentPage + 1,
                            images.size,
                        ),
                        description = stringResource(
                            R.string.preview_page_position,
                            pagerState.currentPage + 1,
                            images.size,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ImagePage(
    item: GItem,
    token: String,
    reduceMotion: Boolean,
    isCurrent: Boolean,
    onZoomChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onOpenWith: (GItem) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var attempt by remember(item.id) { mutableIntStateOf(0) }
    var scale by remember(item.id) { mutableFloatStateOf(1f) }
    var offset by remember(item.id) { mutableStateOf(Offset.Zero) }
    var dragY by remember(item.id) { mutableFloatStateOf(0f) }
    var pageSize by remember { mutableStateOf(IntSize.Zero) }

    val request = remember(item.id, token, attempt, reduceMotion) {
        previewImageRequest(context, item.link, token, crossfade = !reduceMotion)
    }

    // Halaman yang ditinggalkan selalu kembali ke ukuran asli.
    LaunchedEffect(isCurrent) {
        if (!isCurrent) {
            scale = 1f
            offset = Offset.Zero
            dragY = 0f
        }
    }

    val dismissThreshold = with(density) { 132.dp.toPx() }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { pageSize = it }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y + dragY
                val height = pageSize.height.coerceAtLeast(1)
                alpha = (1f - (dragY / height) * 0.9f).coerceIn(0.2f, 1f)
            }
            // Cubit dan geser-saat-zoom. Sentuhan satu jari pada skala 1 tidak
            // dikonsumsi supaya pager tetap bisa digeser.
            .pointerInput(item.id, pageSize) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.count { it.pressed }
                        if (pressed > 1) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val next = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                            val boundX = pageSize.width * (next - 1f) / 2f
                            val boundY = pageSize.height * (next - 1f) / 2f
                            scale = next
                            offset = if (next <= 1.001f) {
                                Offset.Zero
                            } else {
                                Offset(
                                    (offset.x + pan.x).coerceIn(-boundX, boundX),
                                    (offset.y + pan.y).coerceIn(-boundY, boundY),
                                )
                            }
                            onZoomChanged(next > 1.001f)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } else if (scale > 1.001f) {
                            val pan = event.calculatePan()
                            if (pan != Offset.Zero) {
                                val boundX = pageSize.width * (scale - 1f) / 2f
                                val boundY = pageSize.height * (scale - 1f) / 2f
                                offset = Offset(
                                    (offset.x + pan.x).coerceIn(-boundX, boundX),
                                    (offset.y + pan.y).coerceIn(-boundY, boundY),
                                )
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        }
                        if (event.changes.none { it.pressed }) break
                    }
                }
            }
            .pointerInput(item.id, pageSize) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        val startScale = scale
                        val startOffset = offset
                        val zoomIn = startScale <= 1.001f
                        val targetScale = if (zoomIn) DOUBLE_TAP_ZOOM else 1f
                        val boundX = pageSize.width * (targetScale - 1f) / 2f
                        val boundY = pageSize.height * (targetScale - 1f) / 2f
                        val targetOffset = if (zoomIn) {
                            Offset(
                                ((pageSize.width / 2f - tap.x) * (targetScale - 1f))
                                    .coerceIn(-boundX, boundX),
                                ((pageSize.height / 2f - tap.y) * (targetScale - 1f))
                                    .coerceIn(-boundY, boundY),
                            )
                        } else {
                            Offset.Zero
                        }
                        onZoomChanged(zoomIn)
                        if (reduceMotion) {
                            scale = targetScale
                            offset = targetOffset
                        } else {
                            scope.launch {
                                animate(0f, 1f, animationSpec = tween(220)) { fraction, _ ->
                                    scale = lerp(startScale, targetScale, fraction)
                                    offset = Offset(
                                        lerp(startOffset.x, targetOffset.x, fraction),
                                        lerp(startOffset.y, targetOffset.y, fraction),
                                    )
                                }
                            }
                        }
                    },
                )
            }
            // Geser ke bawah untuk menutup, hanya saat gambar belum di-zoom.
            .pointerInput(item.id, scale > 1.001f) {
                if (scale > 1.001f) return@pointerInput
                detectVerticalDragGestures(
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        dragY = (dragY + amount).coerceAtLeast(0f)
                    },
                    onDragCancel = { dragY = 0f },
                    onDragEnd = {
                        if (dragY > dismissThreshold) {
                            onDismiss()
                        } else if (dragY > 0f && !reduceMotion) {
                            val from = dragY
                            scope.launch {
                                animate(from, 0f, animationSpec = tween(200)) { value, _ ->
                                    dragY = value
                                }
                            }
                        } else {
                            dragY = 0f
                        }
                    },
                )
            },
    ) {
        SubcomposeAsyncImage(
            model = request,
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            loading = {
                BlurredBackdrop(
                    url = item.link,
                    token = token,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppLoadingIndicator(
                        size = 42.dp,
                        color = MaterialTheme.colorScheme.primary,
                        reduceMotion = reduceMotion,
                    )
                }
            },
            error = {
                PreviewFailure(
                    message = stringResource(R.string.preview_image_failed),
                    reduceMotion = reduceMotion,
                    onRetry = { attempt++ },
                    onOpenWith = { onOpenWith(item) },
                )
            },
        )
    }
}

/**
 * Indikator halaman statis: satu titik per gambar, titik aktif memakai warna
 * aksen dan sisanya garis luar.
 */
@Composable
private fun PagerDots(count: Int, index: Int, modifier: Modifier = Modifier) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { dot ->
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        if (dot == index) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            )
        }
    }
}

/**
 * Placeholder sementara: salinan gambar yang sangat kecil lalu diburamkan,
 * jadi area tidak pernah berkedip putih. Di perangkat di bawah API 31 blur
 * diabaikan dan hasilnya tetap berupa versi gambar yang lembut.
 */
@Suppress("ProduceStateDoesNotAssignValue")
@Composable
private fun BlurredBackdrop(
    url: String,
    token: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val backdrop by produceState<ImageBitmap?>(initialValue = null, url, token) {
        value = withContext(Dispatchers.IO) {
            previewRunCatching {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(48)
                    .allowHardware(false)
                    .apply {
                        previewRequestHeaders(token).forEach { (name, headerValue) ->
                            addHeader(name, headerValue)
                        }
                    }
                    .build()
                val result = context.imageLoader.execute(request)
                (result.drawable as? BitmapDrawable)?.bitmap?.asImageBitmap()
            }.getOrNull()
        }
    }
    val frame = backdrop
    if (frame != null) {
        Image(
            bitmap = frame,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.blur(30.dp).scale(1.2f),
        )
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh))
    }
}

private fun previewImageRequest(
    context: Context,
    url: String,
    token: String,
    crossfade: Boolean,
): ImageRequest {
    val builder = ImageRequest.Builder(context)
        .data(url)
        .crossfade(crossfade)
    previewRequestHeaders(token).forEach { (name, value) -> builder.addHeader(name, value) }
    return builder.build()
}
