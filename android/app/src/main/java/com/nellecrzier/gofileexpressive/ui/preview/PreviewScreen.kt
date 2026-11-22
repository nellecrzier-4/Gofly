package com.nellecrzier.gofileexpressive.ui.preview

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nellecrzier.gofileexpressive.GItem
import com.nellecrzier.gofileexpressive.R
import com.nellecrzier.gofileexpressive.ui.Hyper
import com.nellecrzier.gofileexpressive.ui.HyperEase
import kotlinx.coroutines.delay

/**
 * Satu item yang sedang dipratinjau, beserta saudara sekandungnya di folder
 * yang sama (`album`) untuk digeser pada pager gambar.
 */
data class PreviewTarget(
    val item: GItem,
    val album: List<GItem>,
)

/**
 * Pratinjau media layar penuh ala HyperSpring, digambar sepenuhnya dengan
 * Compose — tanpa WebView, tanpa HTML. Panggil di dalam Box di atas daftar
 * folder; `null` berarti tidak ada yang dirender.
 *
 * [onOpenWith], [onDownload], dan [onShare] adalah satu-satunya jalan keluar
 * ke Intent: pratinjau tidak pernah memulai Intent sendiri. Dua yang terakhir
 * opsional; entri menunya hanya muncul kalau pemanggil menyediakannya.
 */
@Composable
fun PreviewHost(
    target: PreviewTarget?,
    token: String,
    reduceMotion: Boolean,
    onClose: () -> Unit,
    onOpenWith: (GItem) -> Unit,
    onDownload: ((GItem) -> Unit)? = null,
    onShare: ((GItem) -> Unit)? = null,
) {
    if (target == null) return
    key(target.item.id) {
        PreviewOverlay(
            target = target,
            token = token,
            reduceMotion = reduceMotion,
            onClose = onClose,
            onOpenWith = onOpenWith,
            onDownload = onDownload,
            onShare = onShare,
        )
    }
}

@Composable
private fun PreviewOverlay(
    target: PreviewTarget,
    token: String,
    reduceMotion: Boolean,
    onClose: () -> Unit,
    onOpenWith: (GItem) -> Unit,
    onDownload: ((GItem) -> Unit)?,
    onShare: ((GItem) -> Unit)?,
) {
    val item = target.item
    val kind = remember(item) { item.previewKind() }
    val context = LocalContext.current

    var menuOpen by remember { mutableStateOf(false) }
    var detailsOpen by remember { mutableStateOf(false) }
    var slideshow by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(false) }
    var lineNumbers by remember { mutableStateOf(true) }
    var notice by remember { mutableStateOf<String?>(null) }
    var backProgress by remember { mutableFloatStateOf(0f) }

    val appear = remember { Animatable(if (reduceMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!reduceMotion && appear.value < 1f) {
            appear.animateTo(1f, tween(240, easing = HyperEase))
        }
    }

    LaunchedEffect(notice) {
        val shown = notice
        if (shown != null) {
            delay(2400)
            if (notice == shown) notice = null
        }
    }

    fun leavePreview() {
        if (fullscreen) fullscreen = false else onClose()
    }

    // Back prediktif di Android 14+, BackHandler biasa di bawahnya. Keduanya
    // tidak pernah aktif bersamaan supaya hanya satu callback yang terdaftar.
    val predictiveBack = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    PredictiveBackHandler(enabled = predictiveBack) { events ->
        try {
            events.collect { event -> backProgress = event.progress }
            leavePreview()
        } finally {
            backProgress = 0f
        }
    }
    BackHandler(enabled = !predictiveBack) { leavePreview() }

    val downloadAction: (() -> Unit)? = onDownload?.let { callback -> { callback(item) } }
    val shareAction: (() -> Unit)? = onShare?.let { callback -> { callback(item) } }

    Box(
        Modifier
            .fillMaxSize()
            .background(Hyper.Bg)
            // Menelan ketukan yang tidak ditangani konten, supaya daftar folder
            // di bawah overlay tidak ikut bereaksi.
            .pointerInput(Unit) { detectTapGestures(onTap = { }) }
            .graphicsLayer {
                alpha = appear.value * (1f - backProgress * 0.55f)
                val shrink = 0.02f * (1f - appear.value) + 0.03f * backProgress
                scaleX = 1f - shrink
                scaleY = 1f - shrink
            },
    ) {
        if (item.link.isBlank()) {
            PreviewFailure(
                message = stringResource(R.string.preview_error_no_link),
                reduceMotion = reduceMotion,
                onOpenWith = { onOpenWith(item) },
            )
        } else {
            when (kind) {
                PreviewKind.IMAGE -> ImagePreview(
                    item = item,
                    album = target.album,
                    token = token,
                    reduceMotion = reduceMotion,
                    slideshow = slideshow,
                    slideshowPaused = menuOpen || detailsOpen || fullscreen,
                    onClose = onClose,
                    onOpenWith = onOpenWith,
                    indicatorBottomPadding = if (reduceMotion) 62.dp else 78.dp,
                )

                PreviewKind.VIDEO -> VideoPreview(
                    item = item,
                    token = token,
                    fullscreen = fullscreen,
                    reduceMotion = reduceMotion,
                    onOpenWith = onOpenWith,
                )

                PreviewKind.AUDIO -> AudioPreview(
                    item = item,
                    token = token,
                    reduceMotion = reduceMotion,
                    onOpenWith = onOpenWith,
                )

                PreviewKind.PDF -> PdfPreview(
                    item = item,
                    token = token,
                    reduceMotion = reduceMotion,
                    onOpenWith = onOpenWith,
                )

                PreviewKind.MARKDOWN,
                PreviewKind.CODE,
                PreviewKind.TEXT -> DocumentPreview(
                    item = item,
                    token = token,
                    kind = kind,
                    reduceMotion = reduceMotion,
                    lineNumbers = lineNumbers,
                    onOpenWith = onOpenWith,
                )

                PreviewKind.UNSUPPORTED -> PreviewFailure(
                    title = stringResource(R.string.preview_unsupported_title),
                    message = stringResource(R.string.preview_unsupported_body),
                    reduceMotion = reduceMotion,
                    onOpenWith = { onOpenWith(item) },
                )
            }
        }

        if (!fullscreen) {
            PreviewScrim(
                top = true,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            PreviewTopBar(
                title = item.name,
                onBack = { leavePreview() },
                menuOpen = menuOpen,
                onMenuOpenChange = { menuOpen = it },
                onDownload = downloadAction,
                onShare = shareAction,
                onOpenWith = { onOpenWith(item) },
                onDetails = { detailsOpen = true },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                extra = {
                    when (kind) {
                        PreviewKind.CODE, PreviewKind.TEXT -> PreviewBarButton(
                            icon = Icons.Filled.FormatListNumbered,
                            description = stringResource(
                                if (lineNumbers) {
                                    R.string.preview_line_numbers_hide
                                } else {
                                    R.string.preview_line_numbers_show
                                },
                            ),
                            onClick = { lineNumbers = !lineNumbers },
                        )

                        PreviewKind.VIDEO -> PreviewBarButton(
                            icon = Icons.Filled.Fullscreen,
                            description = stringResource(R.string.preview_fullscreen_enter),
                            onClick = { fullscreen = true },
                        )

                        else -> Unit
                    }
                },
            )
        }

        if (fullscreen && kind == PreviewKind.VIDEO) {
            PreviewBarButton(
                icon = Icons.Filled.FullscreenExit,
                description = stringResource(R.string.preview_fullscreen_exit),
                onClick = { fullscreen = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp),
            )
        }

        if (kind == PreviewKind.IMAGE && !fullscreen) {
            PreviewScrim(
                top = false,
                modifier = Modifier.align(Alignment.BottomCenter),
                height = 156.dp,
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (!reduceMotion) {
                        PreviewActionPill(
                            icon = if (slideshow) Icons.Filled.StopCircle else Icons.Filled.PlayCircle,
                            label = stringResource(
                                if (slideshow) {
                                    R.string.preview_slideshow_stop
                                } else {
                                    R.string.preview_slideshow_start
                                },
                            ),
                            active = slideshow,
                            onClick = { slideshow = !slideshow },
                        )
                    }
                    PreviewActionPill(
                        icon = Icons.Filled.OpenInNew,
                        label = stringResource(R.string.preview_open_with),
                        onClick = { onOpenWith(item) },
                    )
                    downloadAction?.let { action ->
                        PreviewActionPill(
                            icon = Icons.Filled.Download,
                            label = stringResource(R.string.preview_download),
                            onClick = action,
                        )
                    }
                    shareAction?.let { action ->
                        PreviewActionPill(
                            icon = Icons.Filled.Share,
                            label = stringResource(R.string.preview_share),
                            onClick = action,
                        )
                    }
                }
            }
        }

        if (detailsOpen) {
            PreviewDetailsDialog(
                item = item,
                onCopyLink = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText(item.name, item.link))
                    detailsOpen = false
                    notice = context.getString(R.string.preview_details_link_copied)
                },
                onDismiss = { detailsOpen = false },
            )
        }

        val shownNotice = notice
        if (shownNotice != null) {
            PreviewNotice(
                text = shownNotice,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (kind == PreviewKind.IMAGE) 78.dp else 28.dp),
            )
        }
    }
}
