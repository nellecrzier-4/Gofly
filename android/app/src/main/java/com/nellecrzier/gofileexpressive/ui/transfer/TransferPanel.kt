package com.nellecrzier.gofileexpressive.ui.transfer

import android.os.Build
import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nellecrzier.gofileexpressive.BuildConfig
import com.nellecrzier.gofileexpressive.GItem
import com.nellecrzier.gofileexpressive.GSession
import com.nellecrzier.gofileexpressive.GoFileException
import com.nellecrzier.gofileexpressive.GofileApi
import com.nellecrzier.gofileexpressive.MainActivity
import com.nellecrzier.gofileexpressive.SessionStore
import com.nellecrzier.gofileexpressive.TransferInfo
import com.nellecrzier.gofileexpressive.TransferStore
import com.nellecrzier.gofileexpressive.ui.Crumb
import com.nellecrzier.gofileexpressive.ui.readableSize
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.nellecrzier.gofileexpressive.ui.*

/* ============================== transfer ================================== */

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun TransferPanel(
    modifier: Modifier = Modifier,
    limit: Int = 3,
    reduceMotion: Boolean = false,
    showEmptyHint: Boolean = false,
) {
    val transfers by TransferStore.transfers.collectAsState()
    val recent = transfers.takeLast(limit).asReversed()
    if (recent.isEmpty()) {
        if (showEmptyHint) {
            Text(
                "Belum ada transfer. Unggah atau unduh file untuk melihat progresnya di sini.",
                style = MaterialTheme.typography.bodySmall,
                color = Hyper.Text2,
            )
        }
        return
    }
    AnimatedVisibility(
        visible = recent.isNotEmpty(),
        enter = if (reduceMotion) {
            fadeIn(tween(120))
        } else {
            slideInVertically(tween(240, easing = HyperExpoOut)) { it / 4 } + fadeIn(tween(180))
        },
        exit = if (reduceMotion) {
            fadeOut(tween(100))
        } else {
            slideOutVertically(tween(160, easing = HyperPower3Out)) { it / 4 } + fadeOut(tween(120))
        },
    ) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(22.dp),
            color = Hyper.Card,
            border = BorderStroke(0.8.dp, Hyper.Hairline),
            shadowElevation = 6.dp,
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconChip(
                        icon = Icons.Filled.SwapVert,
                        size = 30.dp,
                        tint = Hyper.Accent,
                        fill = Hyper.AccentSoft,
                        edge = Hyper.AccentEdge,
                        iconSize = 16.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Transfer GoFile",
                        style = MaterialTheme.typography.titleSmall,
                        color = Hyper.Text,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${recent.size} item",
                        style = MaterialTheme.typography.labelMedium,
                        color = Hyper.Text2,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Tutup semua progres transfer",
                        tint = Hyper.Text2,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { TransferStore.dismissAll() }
                            .padding(7.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Geser baris ke kiri atau kanan untuk menghapusnya.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Hyper.Text3,
                )
                Spacer(Modifier.height(8.dp))
                recent.forEachIndexed { index, transfer ->
                    SwipeToDismiss(onDismiss = { TransferStore.dismiss(transfer.id) }) {
                        TransferRow(transfer = transfer, reduceMotion = reduceMotion)
                    }
                    if (index != recent.lastIndex) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeToDismiss(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dismissed by remember { mutableStateOf(false) }
    var widthPx by remember { mutableIntStateOf(0) }

    val offset by animateFloatAsState(
        targetValue = when {
            dismissed -> if (dragOffset >= 0f) widthPx * 1.4f else -widthPx * 1.4f
            else -> dragOffset
        },
        animationSpec = if (dismissed) {
            tween(180, easing = HyperPower3Out)
        } else {
            hyperSpring(0.35f, 0.72f)
        },
        finishedListener = { if (dismissed) onDismiss() },
        label = "swipeOffset",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { widthPx = it.width }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Hapus dari daftar transfer") {
                        onDismiss()
                        true
                    },
                )
            }
            .graphicsLayer {
                translationX = offset
                val limit = widthPx.coerceAtLeast(1).toFloat()
                alpha = (1f - abs(offset) / limit).coerceIn(0.2f, 1f)
            }
            .pointerInput(Unit) {
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

@Composable
private fun TransferRow(transfer: TransferInfo, reduceMotion: Boolean) {
    val progress = transfer.progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0, 100) / 100f,
        animationSpec = tween(if (reduceMotion) 0 else 320, easing = HyperExpoOut),
        label = "transferProgress",
    )
    val finished = transfer.state == "success"
    val failed = transfer.state == "error"
    val indicator = when {
        finished -> "selesai"
        failed -> transfer.message
        progress < 0 -> "menyiapkan…"
        else -> "$progress%"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Hyper.CardRaised)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when {
                    finished -> Icons.Filled.CheckCircle
                    failed -> Icons.Filled.ErrorOutline
                    else -> Icons.Filled.SwapVert
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = when {
                    finished -> Hyper.Ok
                    failed -> Hyper.Accent
                    else -> Hyper.Text2
                },
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${transfer.fileName} · $indicator",
                style = MaterialTheme.typography.bodySmall,
                color = Hyper.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(6.dp))
        if (progress < 0 && !finished && !failed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                ExpressiveLoader(
                    size = 16.dp,
                    color = Hyper.Accent,
                    reduceMotion = reduceMotion,
                )
            }
        } else {
            LinearProgressIndicator(
                progress = { if (finished) 1f else animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = if (failed) Hyper.Accent else Hyper.Text,
                trackColor = Hyper.Track,
            )
        }
    }
}
