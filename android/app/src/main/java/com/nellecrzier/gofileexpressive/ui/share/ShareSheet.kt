package com.nellecrzier.gofileexpressive.ui.share

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nellecrzier.gofileexpressive.GItem
import com.nellecrzier.gofileexpressive.R
import com.nellecrzier.gofileexpressive.ui.Hyper
import com.nellecrzier.gofileexpressive.ui.HyperCard
import com.nellecrzier.gofileexpressive.ui.HyperExpoOut
import com.nellecrzier.gofileexpressive.ui.HyperIconButton
import com.nellecrzier.gofileexpressive.ui.IconChip
import com.nellecrzier.gofileexpressive.ui.components.fileKind
import kotlinx.coroutines.delay

/**
 * Lembar bagikan native: pratinjau tautan, lalu Salin, Bagikan, Kode QR,
 * Unduh, dan Detail. Tanpa WebView dan tanpa Toast — konfirmasi salin muncul
 * langsung di barisnya.
 *
 * @param token hanya dipakai untuk memberi tahu kalau sesi kosong; berbagi
 *   tautan tetap bekerja tanpa token.
 * @param onShareIntent pemanggil yang membangun dan meluncurkan ACTION_SEND.
 * @param onDownload pemanggil yang memulai unduhan.
 */
@Composable
fun ShareSheet(
    item: GItem,
    token: String,
    reduceMotion: Boolean,
    onDismiss: () -> Unit,
    onShareIntent: (GItem) -> Unit,
    onDownload: (GItem) -> Unit,
) {
    val context = LocalContext.current
    val link = remember(item.id, item.link, item.isFolder) { shareLinkOf(item) }
    val hasLink = link.isNotBlank()
    val canDownload = !item.isFolder && hasLink
    val linkLabel = stringResource(R.string.share_link_label)

    var copied by remember(link) { mutableStateOf(false) }
    var clipboardFailed by remember(link) { mutableStateOf(false) }
    var detailsOpen by remember { mutableStateOf(false) }
    var qrOpen by remember { mutableStateOf(false) }

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
                        text = stringResource(R.string.share_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = Hyper.Text,
                    )
                    Text(
                        text = stringResource(
                            if (item.isFolder) R.string.share_kind_folder else R.string.share_kind_file,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = Hyper.Text3,
                    )
                }
                HyperIconButton(
                    icon = Icons.Filled.Close,
                    description = stringResource(R.string.share_close),
                    onClick = onDismiss,
                )
            }

            Spacer(Modifier.height(14.dp))
            PreviewCard(item)
            Spacer(Modifier.height(10.dp))

            if (hasLink) {
                LinkCard(link = link, folder = item.isFolder)
            } else {
                HintLine(
                    text = stringResource(R.string.share_no_link),
                    extra = stringResource(R.string.share_no_link_hint),
                    color = Hyper.Warn,
                )
            }

            Spacer(Modifier.height(14.dp))
            HyperCard {
                ShareActionRow(
                    icon = Icons.Filled.ContentCopy,
                    title = stringResource(R.string.share_action_copy),
                    subtitle = stringResource(R.string.share_action_copy_sub),
                    enabled = hasLink,
                    onClick = {
                        val ok = copyToClipboard(context, link, linkLabel)
                        copied = ok
                        clipboardFailed = !ok
                    },
                    trailing = { CopiedPill(visible = copied, reduceMotion = reduceMotion) },
                )
                ShareActionRow(
                    icon = Icons.Filled.Share,
                    title = stringResource(R.string.share_action_share),
                    subtitle = stringResource(R.string.share_action_share_sub),
                    enabled = hasLink,
                    accent = true,
                    onClick = { onShareIntent(item) },
                )
                ShareActionRow(
                    icon = Icons.Filled.QrCode2,
                    title = stringResource(R.string.share_action_qr),
                    subtitle = stringResource(R.string.share_action_qr_sub),
                    enabled = hasLink,
                    onClick = { qrOpen = true },
                )
                ShareActionRow(
                    icon = Icons.Filled.Download,
                    title = stringResource(R.string.share_action_download),
                    subtitle = stringResource(
                        if (item.isFolder) {
                            R.string.share_action_download_folder_sub
                        } else {
                            R.string.share_action_download_sub
                        },
                    ),
                    enabled = canDownload,
                    onClick = { onDownload(item) },
                )
                ShareActionRow(
                    icon = if (detailsOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    title = stringResource(
                        if (detailsOpen) R.string.share_action_details_hide else R.string.share_action_details,
                    ),
                    subtitle = stringResource(R.string.share_action_details_sub),
                    onClick = { detailsOpen = !detailsOpen },
                )
            }

            if (reduceMotion) {
                if (detailsOpen) DetailsCard(item)
            } else {
                AnimatedVisibility(
                    visible = detailsOpen,
                    enter = expandVertically(animationSpec = tween(230, easing = HyperExpoOut)) +
                        fadeIn(animationSpec = tween(160)),
                    exit = shrinkVertically(animationSpec = tween(180)) +
                        fadeOut(animationSpec = tween(120)),
                ) {
                    DetailsCard(item)
                }
            }

            if (clipboardFailed) {
                HintLine(text = stringResource(R.string.share_clipboard_unavailable), color = Hyper.Accent)
            }
            if (token.isBlank()) {
                HintLine(text = stringResource(R.string.share_no_session), color = Hyper.Text3)
            }
        }
    }

    if (qrOpen) {
        ShareQrSheet(
            link = link,
            title = item.name,
            onDismiss = { qrOpen = false },
            reduceMotion = reduceMotion,
        )
    }
}

/* ------------------------------- potongan ------------------------------- */

@Composable
private fun PreviewCard(item: GItem) {
    val kind = item.fileKind()
    val kindLabel = shareKindLabel(kind)
    val meta = if (item.isFolder) {
        stringResource(
            R.string.share_meta_pattern,
            kindLabel,
            pluralStringResource(
                R.plurals.share_items_count,
                item.childrenCount,
                item.childrenCount,
            ),
        )
    } else {
        stringResource(
            R.string.share_meta_pattern,
            kindLabel,
            if (item.size > 0L) shareSize(item.size) else stringResource(R.string.share_size_unknown),
        )
    }

    HyperCard(shape = RoundedCornerShape(22.dp)) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconChip(
                icon = kind.icon,
                size = 46.dp,
                iconSize = 22.dp,
                tint = if (item.isFolder) Hyper.Accent else Hyper.Text,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = middleEllipsis(item.name, 46),
                    style = MaterialTheme.typography.titleMedium,
                    color = Hyper.Text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = Hyper.Text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LinkCard(link: String, folder: Boolean) {
    HyperCard(
        shape = RoundedCornerShape(18.dp),
        fill = Hyper.CardRaised,
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = link,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = Hyper.Text,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (folder) {
                Text(
                    text = stringResource(R.string.share_folder_link_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Hyper.Text3,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun HintLine(text: String, color: androidx.compose.ui.graphics.Color, extra: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(14.dp),
        )
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = color,
            )
            if (extra != null) {
                Text(
                    text = extra,
                    style = MaterialTheme.typography.bodySmall,
                    color = Hyper.Text3,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailsCard(item: GItem) {
    HyperCard(modifier = Modifier.padding(top = 10.dp)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            DetailLine(stringResource(R.string.share_detail_id), item.id, mono = true)
            DetailLine(
                stringResource(R.string.share_detail_size),
                if (item.size > 0L) shareSize(item.size) else stringResource(R.string.share_size_unknown),
            )
            DetailLine(
                stringResource(R.string.share_detail_type),
                item.mimetype.ifBlank { item.type },
                mono = true,
            )
            if (item.createTime > 0L) {
                DetailLine(stringResource(R.string.share_detail_created), shareDateTime(item.createTime))
            }
            if (item.modTime > 0L) {
                DetailLine(stringResource(R.string.share_detail_modified), shareDateTime(item.modTime))
            }
            DetailLine(stringResource(R.string.share_detail_downloads), item.downloadCount.toString())
            if (item.md5.isNotBlank()) {
                DetailLine(stringResource(R.string.share_detail_md5), item.md5, mono = true)
            }
            if (item.parentFolder.isNotBlank()) {
                DetailLine(stringResource(R.string.share_detail_parent), item.parentFolder, mono = true)
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String, mono: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Hyper.Text3,
            modifier = Modifier.width(96.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Hyper.Text2,
            fontFamily = if (mono) FontFamily.Monospace else null,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
