package com.nellecrzier.gofileexpressive.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nellecrzier.gofileexpressive.GItem
import com.nellecrzier.gofileexpressive.GSession
import com.nellecrzier.gofileexpressive.MainActivity
import com.nellecrzier.gofileexpressive.R
import com.nellecrzier.gofileexpressive.ui.animatedCount
import com.nellecrzier.gofileexpressive.ui.animatedSize
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
    val folderCount = remember(items) { items.count { it.isFolder } }
    val fileCount = remember(items) { items.size - folderCount }
    val totalSize = remember(items) { items.sumOf { if (it.isFolder) 0L else it.size } }
    val shownFolders = animatedCount(folderCount, reduceMotion)
    val shownFiles = animatedCount(fileCount, reduceMotion)
    val shownSize = animatedSize(totalSize.toFloat(), reduceMotion).toLong()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tab_home),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Isi root folder GoFile kamu, langsung dari perangkat.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRefresh, enabled = !busy) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh))
            }
        }

        Spacer(Modifier.height(12.dp))
        PermissionSetup(activity = activity, revision = permissionRevision)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = {},
                label = { Text("Sesi aktif") },
                leadingIcon = {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                },
            )
            AssistChip(
                onClick = {},
                label = { Text("Tier ${session.tier.ifBlank { "Tamu" }}") },
            )
        }

        Spacer(Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CloudQueue,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "GOFILE CLOUD",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = session.email.ifBlank { "Akun tamu" },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Siap menerima transfer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Folder", shownFolders.toString(), Icons.Filled.FolderOpen, Modifier.weight(1f))
            StatTile("Berkas", shownFiles.toString(), Icons.AutoMirrored.Filled.InsertDriveFile, Modifier.weight(1f))
            StatTile("Ukuran", readableSize(shownSize), Icons.Filled.SwapVert, Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "AKSI CEPAT",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onOpenFolder,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Buka folder root")
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionTile("Unggah", "Pilih berkas", Icons.Filled.Upload, onUpload, Modifier.weight(1f))
            ActionTile("Folder baru", "Di folder ini", Icons.Filled.CreateNewFolder, onNewFolder, Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))
        if (busy) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Menyinkronkan isi folder…", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            FilledTonalButton(
                onClick = onRefresh,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sinkronkan sekarang")
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = "TRANSFER TERBARU",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TransferPanel(
            limit = 2,
            reduceMotion = reduceMotion,
            showEmptyHint = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun StatTile(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(10.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    state: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(state, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PermissionSetup(activity: MainActivity, revision: Int) {
    if (revision < 0) return
    val notificationsReady = activity.hasNotificationPermission()
    val batteryReady = activity.isIgnoringBatteryOptimizations()
    if (notificationsReady && batteryReady) return

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Siapkan transfer latar belakang", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Dua izin ini menjaga progres tetap berjalan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            PermissionRow(
                ready = notificationsReady,
                title = "Notifikasi progres",
                supporting = "Progres unggah dan unduh tetap terlihat.",
                actionLabel = "Aktifkan",
                onAction = { activity.requestNotificationPermission() },
            )
            Spacer(Modifier.height(4.dp))
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
        targetValue = if (ready) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(240),
        label = "permissionFill",
    )
    Surface(shape = RoundedCornerShape(14.dp), color = fill, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(title, style = MaterialTheme.typography.labelLarge) },
            supportingContent = { Text(supporting, style = MaterialTheme.typography.bodySmall) },
            leadingContent = {
                Icon(
                    imageVector = if (ready) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
            trailingContent = {
                if (ready) {
                    Text(
                        "SIAP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                    )
                } else {
                    TextButton(onClick = onAction) { Text(actionLabel) }
                }
            },
            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = fill),
        )
    }
}

/** Ukuran berkas yang mudah dibaca. */
internal fun readableSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0) "$bytes B" else String.format("%.1f %s", value, units[unit])
}
