package com.nellecrzier.gofileexpressive.ui.preview

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nellecrzier.gofileexpressive.GItem
import com.nellecrzier.gofileexpressive.R
import com.nellecrzier.gofileexpressive.ui.ExpressiveLoader
import com.nellecrzier.gofileexpressive.ui.Hyper
import com.nellecrzier.gofileexpressive.ui.HyperButton
import com.nellecrzier.gofileexpressive.ui.IconChip

/**
 * Tombol ikon 48dp di bilah atas. HyperSpring memakai 44dp di layar biasa,
 * tetapi pratinjau ini menargetkan sasaran sentuh 48dp agar tetap nyaman.
 */
@Composable
internal fun PreviewBarButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Hyper.Card,
        border = BorderStroke(0.8.dp, Hyper.Hairline),
        modifier = modifier.size(48.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (enabled) Hyper.Text else Hyper.Text3,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

/** Pil aksi di baris bawah gambar: ikon + label, tinggi minimum 48dp. */
@Composable
internal fun PreviewActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (active) Hyper.AccentSoft else Hyper.Card.copy(alpha = 0.94f),
        border = BorderStroke(0.8.dp, if (active) Hyper.AccentEdge else Hyper.Hairline),
        modifier = modifier,
    ) {
        Row(
            Modifier
                .heightIn(min = 48.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) Hyper.Accent else Hyper.Text,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (active) Hyper.Accent else Hyper.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Bilah atas pratinjau: kaca gelap dengan garis rambut, nama file di tengah,
 * aksi tambahan (nomor baris / layar penuh), lalu menu "⋮".
 */
@Composable
internal fun PreviewTopBar(
    title: String,
    onBack: () -> Unit,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onDownload: (() -> Unit)?,
    onShare: (() -> Unit)?,
    onOpenWith: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier,
    extra: (@Composable () -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Hyper.Card.copy(alpha = 0.94f),
        border = BorderStroke(0.8.dp, Hyper.Hairline),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .heightIn(min = 60.dp)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PreviewBarButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                description = stringResource(R.string.preview_back),
                onClick = onBack,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = previewMiddleEllipsis(title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Hyper.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            extra?.let {
                Spacer(Modifier.width(6.dp))
                it()
            }
            Spacer(Modifier.width(6.dp))
            Box {
                PreviewBarButton(
                    icon = Icons.Filled.MoreVert,
                    description = stringResource(R.string.preview_more),
                    onClick = { onMenuOpenChange(true) },
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { onMenuOpenChange(false) },
                    modifier = Modifier.widthIn(min = 220.dp),
                ) {
                    if (onDownload != null) {
                        PreviewMenuItem(
                            icon = Icons.Filled.Download,
                            label = stringResource(R.string.preview_download),
                            onClick = {
                                onMenuOpenChange(false)
                                onDownload()
                            },
                        )
                    }
                    if (onShare != null) {
                        PreviewMenuItem(
                            icon = Icons.Filled.Share,
                            label = stringResource(R.string.preview_share),
                            onClick = {
                                onMenuOpenChange(false)
                                onShare()
                            },
                        )
                    }
                    PreviewMenuItem(
                        icon = Icons.Filled.OpenInNew,
                        label = stringResource(R.string.preview_open_with),
                        onClick = {
                            onMenuOpenChange(false)
                            onOpenWith()
                        },
                    )
                    PreviewMenuItem(
                        icon = Icons.Filled.Info,
                        label = stringResource(R.string.preview_details),
                        onClick = {
                            onMenuOpenChange(false)
                            onDetails()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = Hyper.Text,
            )
        },
        onClick = onClick,
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = Hyper.Text2, modifier = Modifier.size(19.dp))
        },
    )
}

/** Lembar detail: nama, ukuran, jenis, id, dan tautan. */
@Composable
internal fun PreviewDetailsDialog(
    item: GItem,
    onCopyLink: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Hyper.Card,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = stringResource(R.string.preview_details_title),
                style = MaterialTheme.typography.titleLarge,
                color = Hyper.Text,
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                PreviewDetailRow(
                    label = stringResource(R.string.preview_details_name),
                    value = item.name,
                )
                PreviewDetailRow(
                    label = stringResource(R.string.preview_details_size),
                    value = previewSizeText(item.size),
                )
                PreviewDetailRow(
                    label = stringResource(R.string.preview_details_type),
                    value = item.mimetype.ifBlank { stringResource(R.string.preview_unknown) },
                    mono = true,
                )
                PreviewDetailRow(
                    label = stringResource(R.string.preview_details_id),
                    value = item.id.ifBlank { stringResource(R.string.preview_unknown) },
                    mono = true,
                )
                PreviewDetailRow(
                    label = stringResource(R.string.preview_details_link),
                    value = item.link.ifBlank { stringResource(R.string.preview_details_no_link) },
                    mono = true,
                    maxLines = 3,
                )
                Spacer(Modifier.height(10.dp))
                HyperButton(
                    text = stringResource(R.string.preview_details_copy_link),
                    onClick = onCopyLink,
                    modifier = Modifier.fillMaxWidth(),
                    primary = false,
                    height = 48.dp,
                )
            }
        },
        confirmButton = {
            HyperButton(
                text = stringResource(R.string.preview_details_close),
                onClick = onDismiss,
                height = 46.dp,
            )
        },
    )
}

@Composable
private fun PreviewDetailRow(
    label: String,
    value: String,
    mono: Boolean = false,
    maxLines: Int = 2,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Hyper.Text3,
            modifier = Modifier.width(84.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Hyper.Text2,
            fontFamily = if (mono) FontFamily.Monospace else null,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Loader HyperSpring di tengah area konten. */
@Composable
internal fun PreviewLoading(
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ExpressiveLoader(size = 46.dp, color = Hyper.Accent, reduceMotion = reduceMotion)
        if (label != null) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = Hyper.Text2,
            )
        }
    }
}

/**
 * Kartu kegagalan dengan pesan ramah, tombol coba lagi, dan jalan keluar
 * "Buka dengan". Aksi ditumpuk vertikal supaya tetap rapi di skala font 200%.
 */
@Composable
internal fun PreviewFailure(
    message: String,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.preview_error_title),
    onRetry: (() -> Unit)? = null,
    onOpenWith: (() -> Unit)? = null,
) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Hyper.Card,
            border = BorderStroke(0.8.dp, Hyper.Hairline),
            modifier = Modifier.widthIn(max = 420.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconChip(
                        icon = Icons.Filled.ErrorOutline,
                        tint = Hyper.Accent,
                        fill = Hyper.AccentSoft,
                        edge = Hyper.AccentEdge,
                        size = 38.dp,
                        iconSize = 21.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Hyper.Text,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Hyper.Text2,
                )
                if (onRetry != null) {
                    Spacer(Modifier.height(18.dp))
                    HyperButton(
                        text = stringResource(R.string.preview_retry),
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                        reduceMotion = reduceMotion,
                        height = 48.dp,
                    )
                }
                if (onOpenWith != null) {
                    Spacer(Modifier.height(10.dp))
                    HyperButton(
                        text = stringResource(R.string.preview_open_with),
                        onClick = onOpenWith,
                        modifier = Modifier.fillMaxWidth(),
                        primary = false,
                        reduceMotion = reduceMotion,
                        height = 48.dp,
                    )
                }
            }
        }
    }
}

/** Pil pemberitahuan singkat di atas baris aksi bawah. */
@Composable
internal fun PreviewNotice(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = Hyper.CardRaised,
        border = BorderStroke(0.8.dp, Hyper.Hairline),
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Hyper.Text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

/** Bayangan gradasi hitam di bawah bilah atas agar judul terbaca di atas foto. */
@Composable
internal fun PreviewScrim(
    top: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 132.dp,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(
                if (top) {
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent),
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)),
                    )
                },
            ),
    )
}

/** Penghitung halaman ringkas: `3 / 12`, dipakai gambar dan PDF. */
@Composable
internal fun PreviewPageCounter(
    text: String,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.58f),
        border = BorderStroke(0.8.dp, Hyper.Hairline),
        modifier = if (description != null) {
            modifier.semantics { contentDescription = description }
        } else {
            modifier
        },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            color = Hyper.Text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
