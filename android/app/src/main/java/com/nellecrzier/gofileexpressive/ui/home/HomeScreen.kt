package com.nellecrzier.gofileexpressive.ui.home

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
import com.nellecrzier.gofileexpressive.ui.transfer.TransferPanel

/* ================================ beranda ================================= */

@Composable
internal fun HomeScreen(
    session: GSession,
    items: List<GItem>,
    busy: Boolean,
    reduceMotion: Boolean,
    activity: MainActivity,
    permissionRevision: Int,
    onOpenFolder: () -> Unit,
    onUpload: () -> Unit,
    onNewFolder: () -> Unit,
    onRefresh: () -> Unit,
) {
    val scroll = rememberScrollState()
    val folderCount = remember(items) { items.count { it.isFolder } }
    val fileCount = remember(items) { items.size - folderCount }
    val totalSize = remember(items) { items.sumOf { if (it.isFolder) 0L else it.size } }
    // Angka bergulir dengan spring saat isi folder berubah.
    val shownFolders = animatedCount(folderCount, reduceMotion)
    val shownFiles = animatedCount(fileCount, reduceMotion)
    val shownSize = animatedSize(totalSize.toFloat(), reduceMotion).toLong()
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val entrance by animateFloatAsState(
        targetValue = if (entered || reduceMotion) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 320, easing = HyperExpoOut),
        label = "homeEntrance",
    )
    val density = LocalDensity.current
    val shift = with(density) { (1f - entrance) * 14.dp.toPx() }

    HyperPage(
        title = "Beranda",
        subtitle = "Isi root folder GoFile kamu, langsung dari perangkat.",
        scroll = scroll,
        reduceMotion = reduceMotion,
        actions = {
            HyperIconButton(
                icon = Icons.Filled.Refresh,
                description = "Muat ulang",
                enabled = !busy,
                onClick = onRefresh,
            )
        },
    ) {
        Column(
            Modifier.graphicsLayer {
                alpha = entrance
                translationY = shift
            },
        ) {
            PermissionSetup(activity = activity, revision = permissionRevision, reduceMotion = reduceMotion)

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(
                    text = "Sesi aktif",
                    dotColor = Hyper.Ok,
                    reduceMotion = reduceMotion,
                )
                StatusPill(
                    text = "Tier ${session.tier.ifBlank { "Tamu" }}",
                    dotColor = Hyper.Warn,
                    pulse = false,
                    reduceMotion = reduceMotion,
                )
            }

            Spacer(Modifier.height(14.dp))

            // Kartu akun: koral tipis dengan garis koral 40%.
            HyperCard(
                fill = Hyper.AccentSoft,
                border = Hyper.AccentEdge,
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CloudQueue,
                            contentDescription = null,
                            tint = Hyper.Accent,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = "GOFILE CLOUD",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.6.sp,
                            color = Hyper.Accent,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = session.email.ifBlank { "Akun tamu" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Hyper.Text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MicroPill(session.tier.ifBlank { "Tamu" })
                        MicroPill("Siap transfer")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StatTile(
                    label = "Folder",
                    value = shownFolders.toString(),
                    icon = Icons.Filled.Folder,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "File",
                    value = shownFiles.toString(),
                    icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "Ukuran",
                    value = readableSize(shownSize),
                    icon = Icons.Filled.SwapVert,
                    modifier = Modifier.weight(1f),
                )
            }

            SectionLabel("Aksi cepat")
            HyperButton(
                text = "Buka folder root",
                icon = Icons.Filled.FolderOpen,
                onClick = onOpenFolder,
                height = 56.dp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FeatureTile(
                    icon = Icons.Filled.Upload,
                    title = "Unggah",
                    state = "Pilih berkas",
                    onClick = onUpload,
                    modifier = Modifier.weight(1f),
                )
                FeatureTile(
                    icon = Icons.Filled.CreateNewFolder,
                    title = "Folder baru",
                    state = "Di folder ini",
                    onClick = onNewFolder,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))
            if (busy) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExpressiveLoader(size = 20.dp, reduceMotion = reduceMotion, color = Hyper.Accent)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Menyinkronkan isi folder…",
                        style = MaterialTheme.typography.labelLarge,
                        color = Hyper.Text2,
                    )
                }
            } else {
                HyperButton(
                    text = "Sinkronkan sekarang",
                    icon = Icons.Filled.Refresh,
                    primary = false,
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionLabel("Transfer terbaru")
            TransferPanel(
                limit = 2,
                reduceMotion = reduceMotion,
                showEmptyHint = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Pil kecil di dalam kartu akun. */
@Composable
private fun MicroPill(text: String) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.07f),
        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.10f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Hyper.Text,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** Kotak statistik: ikon kecil, angka tebal, label kapital. */
@Composable
private fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    HyperCard(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 11.dp)) {
            IconChip(icon, size = 30.dp, tint = Hyper.Accent, iconSize = 16.dp)
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Hyper.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.1.sp,
                color = Hyper.Text3,
                maxLines = 1,
            )
        }
    }
}

/** Ubin fitur: ikon, judul, dan status kapital di bawahnya. */
@Composable
private fun FeatureTile(
    icon: ImageVector,
    title: String,
    state: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = hyperSpring(0.3f, 0.6f),
        label = "tilePress",
    )
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(16.dp),
        color = if (active) Hyper.AccentSoft else Hyper.Card,
        border = BorderStroke(0.8.dp, if (active) Hyper.AccentEdge else Hyper.Hairline),
        modifier = modifier
            .heightIn(min = 92.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 11.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) Hyper.Accent else Hyper.Text,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(9.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Hyper.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.1.sp,
                color = if (active) Hyper.Accent else Hyper.Text3,
                maxLines = 1,
            )
        }
    }
}

/* ============================= izin transfer ============================== */

@Composable
private fun PermissionSetup(activity: MainActivity, revision: Int, reduceMotion: Boolean) {
    if (revision < 0) return
    val notificationsReady = activity.hasNotificationPermission()
    val batteryReady = activity.isIgnoringBatteryOptimizations()
    if (notificationsReady && batteryReady) return

    HyperCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(
                    icon = Icons.Filled.NotificationsActive,
                    size = 34.dp,
                    tint = Hyper.Accent,
                    fill = Hyper.AccentSoft,
                    edge = Hyper.AccentEdge,
                    iconSize = 18.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Siapkan transfer latar belakang",
                        style = MaterialTheme.typography.titleSmall,
                        color = Hyper.Text,
                    )
                    Text(
                        "Dua izin ini menjaga progres tetap berjalan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Hyper.Text2,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            PermissionRow(
                ready = notificationsReady,
                title = "Notifikasi progres",
                supporting = "Progres unggah dan unduh tetap terlihat.",
                actionLabel = "Aktifkan",
                onAction = { activity.requestNotificationPermission() },
            )
            Spacer(Modifier.height(6.dp))
            PermissionRow(
                ready = batteryReady,
                title = "Izin latar belakang",
                supporting = "Transfer tidak terhenti saat aplikasi ditutup.",
                actionLabel = "Atur",
                onAction = { activity.requestIgnoreBatteryOptimizations() },
            )
        }
    }
}

@Composable
private fun PermissionRow(
    ready: Boolean,
    title: String,
    supporting: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val fill by animateColorAsState(
        targetValue = if (ready) Hyper.AccentSoft else Hyper.CardRaised,
        animationSpec = tween(240, easing = HyperEase),
        label = "permissionFill",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(fill)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (ready) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (ready) Hyper.Accent else Hyper.Text2,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = Hyper.Text,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = Hyper.Text2,
            )
        }
        if (ready) {
            Text(
                "SIAP",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.1.sp,
                color = Hyper.Accent,
            )
        } else {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = Hyper.Accent)
            }
        }
    }
}
