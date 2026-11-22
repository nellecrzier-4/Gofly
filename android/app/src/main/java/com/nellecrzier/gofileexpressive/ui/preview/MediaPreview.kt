package com.nellecrzier.gofileexpressive.ui.preview

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.nellecrzier.gofileexpressive.GItem
import com.nellecrzier.gofileexpressive.GofileConfig
import com.nellecrzier.gofileexpressive.R
import com.nellecrzier.gofileexpressive.ui.Hyper
import com.nellecrzier.gofileexpressive.ui.HyperCard
import com.nellecrzier.gofileexpressive.ui.IconChip
import kotlinx.coroutines.delay

/**
 * Video lewat Media3 ExoPlayer di dalam PlayerView. Kontrol bawaan pemutar
 * dipakai supaya scrubbing, subtitle, dan aksesibilitasnya ikut gratis;
 * bilah aplikasi disembunyikan saat mode layar penuh.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun VideoPreview(
    item: GItem,
    token: String,
    fullscreen: Boolean,
    reduceMotion: Boolean,
    onOpenWith: (GItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var attempt by remember(item.id) { mutableIntStateOf(0) }
    var failed by remember(item.id) { mutableStateOf(false) }

    val player = remember(item.id, token, attempt) {
        previewPlayer(context, item.link, token, video = true)
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                failed = true
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) failed = false
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = true
                    controllerShowTimeoutMs = 3500
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { view ->
                view.player = player
                view.resizeMode = if (fullscreen) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (failed) {
            PreviewFailure(
                message = stringResource(R.string.preview_video_failed),
                reduceMotion = reduceMotion,
                onRetry = {
                    failed = false
                    attempt++
                },
                onOpenWith = { onOpenWith(item) },
            )
        }
    }
}

/**
 * Audio tanpa artwork: satu kartu HyperSpring dengan judul, seek bar,
 * play/pause, dan lompat 10 detik. Pemutar hidup selama pratinjau terbuka.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun AudioPreview(
    item: GItem,
    token: String,
    reduceMotion: Boolean,
    onOpenWith: (GItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var attempt by remember(item.id) { mutableIntStateOf(0) }
    var failed by remember(item.id) { mutableStateOf(false) }

    val player = remember(item.id, token, attempt) {
        previewPlayer(context, item.link, token, video = false)
    }

    var positionMs by remember(player) { mutableLongStateOf(0L) }
    var durationMs by remember(player) { mutableLongStateOf(0L) }
    var playing by remember(player) { mutableStateOf(false) }
    var scrubbing by remember(player) { mutableStateOf(false) }
    var scrubValue by remember(player) { mutableFloatStateOf(0f) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                failed = true
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) failed = false
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            if (!scrubbing) positionMs = player.currentPosition.coerceAtLeast(0L)
            val total = player.duration
            durationMs = if (total == C.TIME_UNSET || total < 0L) 0L else total
            delay(if (player.isPlaying) 200L else 500L)
        }
    }

    val maxValue = durationMs.coerceAtLeast(1L).toFloat()

    fun seekBy(deltaMs: Long) {
        val total = if (durationMs > 0L) durationMs else player.duration.coerceAtLeast(0L)
        val target = (player.currentPosition + deltaMs).coerceAtLeast(0L)
        player.seekTo(if (total > 0L) target.coerceAtMost(total) else target)
        positionMs = player.currentPosition.coerceAtLeast(0L)
    }

    fun togglePlay() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0L)
            player.play()
        }
    }

    Box(modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        HyperCard(Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconChip(
                        icon = Icons.Filled.MusicNote,
                        size = 54.dp,
                        iconSize = 26.dp,
                        tint = Hyper.Accent,
                        fill = Hyper.AccentSoft,
                        edge = Hyper.AccentEdge,
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Hyper.Text,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = previewSizeText(item.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = Hyper.Text2,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Slider(
                    value = if (scrubbing) {
                        scrubValue.coerceIn(0f, maxValue)
                    } else {
                        positionMs.toFloat().coerceIn(0f, maxValue)
                    },
                    onValueChange = { value ->
                        scrubbing = true
                        scrubValue = value
                    },
                    onValueChangeFinished = {
                        player.seekTo(scrubValue.toLong())
                        positionMs = scrubValue.toLong()
                        scrubbing = false
                    },
                    valueRange = 0f..maxValue,
                    enabled = durationMs > 0L,
                    colors = SliderDefaults.colors(
                        thumbColor = Hyper.Accent,
                        activeTrackColor = Hyper.Accent,
                        inactiveTrackColor = Hyper.Track,
                    ),
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = previewTimeText(if (scrubbing) scrubValue.toLong() else positionMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = Hyper.Text2,
                    )
                    Text(
                        text = previewTimeText(durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = Hyper.Text2,
                    )
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PreviewBarButton(
                        icon = Icons.Filled.FastRewind,
                        description = stringResource(R.string.preview_audio_rewind),
                        onClick = { seekBy(-10_000L) },
                    )
                    Surface(
                        onClick = { togglePlay() },
                        shape = CircleShape,
                        color = Hyper.Accent,
                        border = BorderStroke(0.8.dp, Hyper.AccentEdge),
                        modifier = Modifier.size(64.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = stringResource(
                                    if (playing) R.string.preview_audio_pause else R.string.preview_audio_play,
                                ),
                                tint = Hyper.AccentInk,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }
                    PreviewBarButton(
                        icon = Icons.Filled.FastForward,
                        description = stringResource(R.string.preview_audio_forward),
                        onClick = { seekBy(10_000L) },
                    )
                }
            }
        }

        if (failed) {
            PreviewFailure(
                message = stringResource(R.string.preview_audio_failed),
                reduceMotion = reduceMotion,
                onRetry = {
                    failed = false
                    attempt++
                },
                onOpenWith = { onOpenWith(item) },
            )
        }
    }
}

/**
 * Satu ExoPlayer dengan header CDN yang benar. Header dipasang di pabrik
 * sumber data, bukan di MediaItem, karena Media3 tidak punya header per item.
 */
@androidx.annotation.OptIn(UnstableApi::class)
private fun previewPlayer(
    context: Context,
    url: String,
    token: String,
    video: Boolean,
): ExoPlayer {
    val http = DefaultHttpDataSource.Factory()
        .setDefaultRequestProperties(previewRequestHeaders(token))
        .setConnectTimeoutMs(GofileConfig.CONNECT_TIMEOUT_MS.toInt())
        .setReadTimeoutMs(GofileConfig.READ_TIMEOUT_MS.toInt())
        .setAllowCrossProtocolRedirects(true)

    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(context, http)))
        .build()
        .apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(
                        if (video) C.AUDIO_CONTENT_TYPE_MOVIE else C.AUDIO_CONTENT_TYPE_MUSIC,
                    )
                    .build(),
                true,
            )
            setHandleAudioBecomingNoisy(true)
            setMediaItem(MediaItem.fromUri(url))
            prepare()
        }
}
