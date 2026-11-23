@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.nellecrzier.gofileexpressive.ui.transfer

import com.nellecrzier.gofileexpressive.ui.components.AppLoadingIndicator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nellecrzier.gofileexpressive.R
import com.nellecrzier.gofileexpressive.TransferInfo
import com.nellecrzier.gofileexpressive.TransferStore
import com.nellecrzier.gofileexpressive.transfer.TransferControls
import com.nellecrzier.gofileexpressive.transfer.TransferFormat
import com.nellecrzier.gofileexpressive.transfer.TransferHistory
import com.nellecrzier.gofileexpressive.ui.components.HoldToConfirmButton
import com.nellecrzier.gofileexpressive.ui.motion.motionScheme
import kotlin.math.abs
import java.text.DateFormat
import java.util.Date

private const val SEGMENT_ACTIVE = "active"
private const val SEGMENT_HISTORY = "history"

private const val FILTER_ALL = "all"
private const val FILTER_UPLOAD = "upload"
private const val FILTER_DOWNLOAD = "download"
private const val FILTER_DONE = "done"
private const val FILTER_FAILED = "failed"
private const val FILTER_CANCELLED = "cancelled"

private const val MAX_ROWS = 40

/* ============================== panel ================================== */

/**
 * Panel transfer Material 3.
 *
 * Dua segmen (Aktif / Riwayat), filter arah dan status, baris dengan progres,
 * kecepatan, ETA, serta tombol jeda/lanjutkan/batal. Baris selesai bisa digeser
 * atau diketuk untuk detail. Riwayat dibersihkan dengan tahan-lama.
 *
 * Parameter lama (`modifier`, `limit`, `reduceMotion`, `showEmptyHint`)
 * dipertahankan urut supaya pemanggil di HomeScreen tidak berubah.
 */
@Composable
internal fun TransferPanel(
    modifier: Modifier = Modifier,
    limit: Int = 3,
    reduceMotion: Boolean = false,
    showEmptyHint: Boolean = false,
    initialSegment: String = SEGMENT_ACTIVE,
) {
    val context = LocalContext.current
    val transfers by TransferStore.transfers.collectAsState()
    val history by TransferHistory.entries.collectAsState()

    LaunchedEffect(Unit) { TransferHistory.ensureLoaded(context) }

    var segment by rememberSaveable { mutableStateOf(initialSegment) }
    var typeFilter by rememberSaveable { mutableStateOf(FILTER_ALL) }
    var stateFilter by rememberSaveable { mutableStateOf(FILTER_ALL) }
    var detailsId by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    val active = transfers.filter { it.isActive && matches(it, typeFilter, stateFilter) }
    val historyRows = history.filter { matches(it, typeFilter, stateFilter) }
    val rows = if (segment == SEGMENT_HISTORY) {
        historyRows.take(MAX_ROWS)
    } else {
        if (limit > 0) active.take(limit) else active.take(MAX_ROWS)
    }

    if (transfers.isEmpty() && history.isEmpty()) {
        if (showEmptyHint) {
            Text(
                text = stringResource(R.string.transfer_panel_empty_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier,
            )
        }
        return
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            PanelHeader(
                segment = segment,
                activeCount = active.size,
                historyCount = historyRows.size,
                onClear = { confirmClear = true },
            )
            Spacer(Modifier.height(10.dp))
            val segments = listOf(
                SEGMENT_ACTIVE to stringResource(R.string.transfer_panel_active),
                SEGMENT_HISTORY to stringResource(R.string.transfer_panel_history),
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                segments.forEachIndexed { index, (key, label) ->
                    SegmentedButton(
                        selected = segment == key,
                        onClick = { segment = key },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = segments.size,
                        ),
                        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterPill(stringResource(R.string.transfer_panel_filter_all), typeFilter == FILTER_ALL) {
                    typeFilter = FILTER_ALL
                }
                FilterPill(stringResource(R.string.transfer_panel_filter_upload), typeFilter == FILTER_UPLOAD) {
                    typeFilter = FILTER_UPLOAD
                }
                FilterPill(stringResource(R.string.transfer_panel_filter_download), typeFilter == FILTER_DOWNLOAD) {
                    typeFilter = FILTER_DOWNLOAD
                }
            }
            if (segment == SEGMENT_HISTORY) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterPill(stringResource(R.string.transfer_panel_filter_all), stateFilter == FILTER_ALL) {
                        stateFilter = FILTER_ALL
                    }
                    FilterPill(stringResource(R.string.transfer_panel_filter_done), stateFilter == FILTER_DONE) {
                        stateFilter = FILTER_DONE
                    }
                    FilterPill(stringResource(R.string.transfer_panel_filter_failed), stateFilter == FILTER_FAILED) {
                        stateFilter = FILTER_FAILED
                    }
                    FilterPill(
                        stringResource(R.string.transfer_panel_filter_cancelled),
                        stateFilter == FILTER_CANCELLED,
                    ) {
                        stateFilter = FILTER_CANCELLED
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            if (rows.isEmpty()) {
                Text(
                    text = stringResource(
                        if (segment == SEGMENT_HISTORY) {
                            R.string.transfer_panel_empty_history
                        } else {
                            R.string.transfer_panel_empty_active
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 10.dp),
                )
            } else {
                if (segment == SEGMENT_HISTORY && rows.any { it.isFinished }) {
                    Text(
                        text = stringResource(R.string.transfer_panel_swipe_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                Column(
                    Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    rows.forEachIndexed { index, info ->
                        val dismissible = info.isFinished
                        SwipeToDismiss(
                            enabled = dismissible,
                            onDismiss = {
                                TransferStore.dismiss(info.id)
                                if (segment == SEGMENT_HISTORY) TransferHistory.remove(info.id)
                            },
                        ) {
                            TransferRow(
                                info = info,
                                reduceMotion = reduceMotion,
                                onDetails = { detailsId = info.id },
                                onPause = { TransferControls.pause(context, info.id) },
                                onResume = { TransferControls.resume(context, info.id) },
                                onCancel = { TransferControls.cancel(context, info.id) },
                            )
                        }
                        if (index != rows.lastIndex) Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    detailsId?.let { id ->
        val info = TransferStore.get(id) ?: history.firstOrNull { it.id == id }
        if (info != null) {
            TransferDetailsDialog(
                info = info,
                reduceMotion = reduceMotion,
                onClose = { detailsId = null },
            )
        }
    }

    if (confirmClear) {
        ClearHistoryDialog(
            count = history.size,
            onDismiss = { confirmClear = false },
            onCleared = {
                TransferHistory.clear()
                confirmClear = false
            },
        )
    }
}

@Composable
private fun PanelHeader(
    segment: String,
    activeCount: Int,
    historyCount: Int,
    onClear: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(30.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.SwapVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.transfer_panel_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(
                if (segment == SEGMENT_HISTORY) {
                    R.string.transfer_panel_history_count
                } else {
                    R.string.transfer_panel_active_count
                },
                if (segment == SEGMENT_HISTORY) historyCount else activeCount,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (segment == SEGMENT_HISTORY) {
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = stringResource(R.string.transfer_panel_clear),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable { onClear() }
                    .padding(7.dp),
            )
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

private fun matches(info: TransferInfo, type: String, state: String): Boolean {
    val typeOk = when (type) {
        FILTER_UPLOAD -> info.direction == "upload"
        FILTER_DOWNLOAD -> info.direction == "download"
        else -> true
    }
    if (!typeOk) return false
    return when (state) {
        FILTER_DONE -> info.state == TransferInfo.STATE_SUCCESS
        FILTER_FAILED -> info.state == TransferInfo.STATE_ERROR
        FILTER_CANCELLED -> info.state == TransferInfo.STATE_CANCELLED
        else -> true
    }
}

/* =============================== baris ================================= */

@Composable
private fun TransferRow(
    info: TransferInfo,
    reduceMotion: Boolean,
    onDetails: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = info.progress.coerceIn(0, 100) / 100f,
        animationSpec = if (reduceMotion) tween(0) else motionScheme.fastEffectsSpec<Float>(),
        label = "transferProgress",
    )
    val indeterminate = info.progress < 0 && info.state == TransferInfo.STATE_RUNNING
    val finished = info.state == TransferInfo.STATE_SUCCESS
    val failed = info.state == TransferInfo.STATE_ERROR
    val paused = info.state == TransferInfo.STATE_PAUSED

    Surface(
        onClick = onDetails,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        finished -> Icons.Filled.CheckCircle
                        failed -> Icons.Filled.ErrorOutline
                        info.direction == "download" -> Icons.Filled.Download
                        else -> Icons.Filled.Upload
                    },
                    contentDescription = stringResource(
                        if (info.direction == "download") {
                            R.string.transfer_type_download
                        } else {
                            R.string.transfer_type_upload
                        },
                    ),
                    modifier = Modifier.size(16.dp),
                    tint = when {
                        finished -> MaterialTheme.colorScheme.tertiary
                        failed -> MaterialTheme.colorScheme.primary
                        paused -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = info.fileName.ifBlank { stringResource(R.string.transfer_panel_unknown) },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = statusLine(info),
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            failed -> MaterialTheme.colorScheme.primary
                            paused -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (info.isActive) {
                    Spacer(Modifier.width(4.dp))
                    if (paused) {
                        RowAction(Icons.Filled.PlayArrow, stringResource(R.string.transfer_action_resume), onResume)
                    } else {
                        RowAction(Icons.Filled.Pause, stringResource(R.string.transfer_action_pause), onPause)
                    }
                    RowAction(Icons.Filled.Close, stringResource(R.string.transfer_action_cancel), onCancel)
                } else if (info.outputUri.isNotBlank()) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = stringResource(R.string.transfer_action_open),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (indeterminate || (info.progress < 0 && info.state == TransferInfo.STATE_RETRYING)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    AppLoadingIndicator(
                        size = 14.dp,
                        color = MaterialTheme.colorScheme.primary,
                        reduceMotion = reduceMotion,
                    )
                }
            } else {
                LinearProgressIndicator(
                    progress = { if (finished) 1f else animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = when {
                        failed -> MaterialTheme.colorScheme.primary
                        paused -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        }
    }
}

@Composable
private fun RowAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(30.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** Baris status satu kalimat: persen, kecepatan, dan ETA saat berjalan. */
@Composable
private fun statusLine(info: TransferInfo): String {
    val context = LocalContext.current
    return when (info.state) {
        TransferInfo.STATE_QUEUED -> context.getString(R.string.transfer_state_queued)
        TransferInfo.STATE_PAUSED -> context.getString(R.string.transfer_state_paused_message)
        TransferInfo.STATE_RETRYING -> context.getString(
            R.string.transfer_state_retrying,
            info.attempt.coerceAtLeast(1),
            3,
        )
        TransferInfo.STATE_SUCCESS -> context.getString(R.string.transfer_state_success)
        TransferInfo.STATE_ERROR -> info.message.ifBlank {
            context.getString(R.string.transfer_state_error)
        }
        TransferInfo.STATE_CANCELLED -> context.getString(R.string.transfer_state_cancelled)
        else -> {
            if (info.progress < 0) {
                return context.getString(R.string.transfer_notification_preparing)
            }
            val parts = mutableListOf<String>()
            parts += context.getString(R.string.transfer_panel_percent, info.progress)
            if (info.bytesPerSecond > 0) {
                parts += TransferFormat.speedShort(context, info.bytesPerSecond)
            }
            if (info.etaSeconds >= 0) {
                parts += TransferFormat.eta(context, info.etaSeconds)
            }
            parts.joinToString(" · ")
        }
    }
}

/* ============================== geser ================================== */

@Composable
private fun SwipeToDismiss(
    enabled: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dismissed by remember { mutableStateOf(false) }
    var widthPx by remember { mutableIntStateOf(0) }
    val dismissLabel = stringResource(R.string.transfer_panel_dismiss)

    val offset by animateFloatAsState(
        targetValue = when {
            dismissed -> if (dragOffset >= 0f) widthPx * 1.4f else -widthPx * 1.4f
            else -> dragOffset
        },
        animationSpec = if (dismissed) {
            tween(180)
        } else {
            motionScheme.defaultSpatialSpec<Float>()
        },
        finishedListener = { if (dismissed) onDismiss() },
        label = "swipeOffset",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { widthPx = it.width }
            .semantics {
                if (!enabled) return@semantics
                customActions = listOf(
                    CustomAccessibilityAction(dismissLabel) {
                        onDismiss()
                        true
                    },
                )
            }
            .graphicsLayer {
                translationX = if (enabled) offset else 0f
                val limitPx = widthPx.coerceAtLeast(1).toFloat()
                alpha = if (enabled) (1f - abs(offset) / limitPx).coerceIn(0.2f, 1f) else 1f
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (widthPx > 0 && abs(dragOffset) > widthPx * 0.32f) {
                            dismissed = true
                        } else {
                            dragOffset = 0f
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset = (dragOffset + dragAmount)
                            .coerceIn(-widthPx * 1.4f, widthPx * 1.4f)
                    },
                )
            },
    ) {
        content()
    }
}

/* ============================== detail ================================= */

@Composable
private fun TransferDetailsDialog(
    info: TransferInfo,
    reduceMotion: Boolean,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier
                    .padding(18.dp)
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.transfer_panel_details_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.transfer_panel_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Text(
                    text = info.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(14.dp))
                DetailRow(stringResource(R.string.transfer_panel_field_direction), directionLabel(info))
                DetailRow(stringResource(R.string.transfer_panel_field_state), stateLabel(info))
                DetailRow(
                    stringResource(R.string.transfer_panel_field_size),
                    if (info.bytesTotal > 0) {
                        context.getString(
                            R.string.transfer_panel_of,
                            TransferFormat.bytes(info.bytesDone),
                            TransferFormat.bytes(info.bytesTotal),
                        )
                    } else {
                        stringResource(R.string.transfer_panel_unknown)
                    },
                )
                if (info.bytesPerSecond > 0) {
                    DetailRow(
                        stringResource(R.string.transfer_panel_field_speed),
                        TransferFormat.speedShort(context, info.bytesPerSecond),
                    )
                }
                if (info.etaSeconds >= 0 && info.isActive) {
                    DetailRow(
                        stringResource(R.string.transfer_panel_field_eta),
                        TransferFormat.eta(context, info.etaSeconds),
                    )
                }
                if (info.outputPath.isNotBlank()) {
                    DetailRow(stringResource(R.string.transfer_panel_field_saved), info.outputPath)
                }
                if (info.md5.isNotBlank()) {
                    DetailRow(stringResource(R.string.transfer_panel_field_md5), info.md5)
                } else if (info.direction == "download") {
                    DetailRow(
                        stringResource(R.string.transfer_panel_field_md5),
                        stringResource(R.string.transfer_panel_unknown),
                    )
                }
                if (info.message.isNotBlank()) {
                    DetailRow(stringResource(R.string.transfer_panel_field_message), info.message)
                }
                if (info.createdAt > 0) {
                    DetailRow(
                        stringResource(R.string.transfer_panel_field_started),
                        formatTime(info.createdAt),
                    )
                }
                if (info.isFinished && info.updatedAt > 0) {
                    DetailRow(
                        stringResource(R.string.transfer_panel_field_finished),
                        formatTime(info.updatedAt),
                    )
                }
                Spacer(Modifier.height(14.dp))
                if (info.isActive) {
                    FilledTonalButton(
                        onClick = {
                            TransferControls.cancel(context, info.id)
                            onClose()
                        },
                        shape = RoundedCornerShape(percent = 50),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.transfer_action_cancel))
                    }
                } else if (info.outputUri.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (TransferControls.open(context, info)) onClose()
                            },
                            shape = RoundedCornerShape(percent = 50),
                            modifier = Modifier.weight(1f).height(56.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.transfer_action_open))
                        }
                        FilledTonalButton(
                            onClick = {
                                if (TransferControls.share(context, info)) onClose()
                            },
                            shape = RoundedCornerShape(percent = 50),
                            modifier = Modifier.weight(1f).height(56.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.transfer_action_share))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(112.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ClearHistoryDialog(
    count: Int,
    onDismiss: () -> Unit,
    onCleared: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.transfer_panel_clear_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.transfer_panel_clear_body, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                HoldToConfirmButton(
                    label = stringResource(R.string.transfer_panel_clear_hold),
                    hint = stringResource(R.string.transfer_panel_clear_hint),
                    onConfirm = onCleared,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.transfer_panel_close),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(CircleShape)
                        .clickable { onDismiss() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/* ============================== pembantu =============================== */

@Composable
private fun directionLabel(info: TransferInfo): String = stringResource(
    when {
        info.folder -> R.string.transfer_type_folder
        info.direction == "download" -> R.string.transfer_type_download
        else -> R.string.transfer_type_upload
    },
)

@Composable
private fun stateLabel(info: TransferInfo): String = when (info.state) {
    TransferInfo.STATE_QUEUED -> stringResource(R.string.transfer_state_queued)
    TransferInfo.STATE_PAUSED -> stringResource(R.string.transfer_state_paused)
    TransferInfo.STATE_RETRYING -> stringResource(
        R.string.transfer_state_retrying,
        info.attempt.coerceAtLeast(1),
        3,
    )
    TransferInfo.STATE_SUCCESS -> stringResource(R.string.transfer_state_success)
    TransferInfo.STATE_ERROR -> stringResource(R.string.transfer_state_error)
    TransferInfo.STATE_CANCELLED -> stringResource(R.string.transfer_state_cancelled)
    else -> stringResource(R.string.transfer_state_running)
}

private fun formatTime(millis: Long): String = try {
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(millis))
} catch (_: Exception) {
    "—"
}
