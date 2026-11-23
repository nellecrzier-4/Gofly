package com.nellecrzier.gofly.ui.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nellecrzier.gofly.GItem
import com.nellecrzier.gofly.GoFileException
import com.nellecrzier.gofly.R
import com.nellecrzier.gofly.ui.components.FileKind
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/* ------------------------------ tautan ------------------------------ */

/** Basis tautan folder GoFile: id konten cukup untuk halaman publiknya. */
internal const val SHARE_LINK_BASE = "https://gofile.io/d/"

/**
 * Tautan yang benar-benar bisa dibagikan untuk [item]: folder memakai halaman
 * `/d/<id>` (folder tidak pernah punya `link` langsung), file memakai `link`
 * dari server. String kosong berarti tidak ada tautan yang bisa dipakai.
 */
internal fun shareLinkOf(item: GItem): String = when {
    item.isFolder -> if (item.id.isBlank()) "" else SHARE_LINK_BASE + item.id
    item.link.startsWith("http", ignoreCase = true) -> item.link
    else -> ""
}

/**
 * Elipsis tengah: nama panjang tetap memperlihatkan awal dan ekstensi,
 * mis. `report_fi…_final.pdf`.
 */
internal fun middleEllipsis(text: String, max: Int = 36): String {
    if (text.length <= max) return text
    if (max <= 8) return text.take(max)
    val head = (max * 0.56f).toInt().coerceAtLeast(4)
    val tail = (max - head - 1).coerceAtLeast(3)
    return text.take(head) + "…" + text.takeLast(tail)
}

/** Ukuran yang ramah dibaca; pemisah desimal mengikuti locale perangkat. */
internal fun shareSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0) "$bytes B" else String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
}

/** Tanggal ringkas untuk baris kedaluwarsa dan blok detail. */
internal fun shareDate(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return ""
    val millis = if (epochSeconds < 1_000_000_000_000L) epochSeconds * 1000L else epochSeconds
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
}

/** Tanggal + jam, dipakai blok detail. */
internal fun shareDateTime(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return ""
    val millis = if (epochSeconds < 1_000_000_000_000L) epochSeconds * 1000L else epochSeconds
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
}

/**
 * Salin tanpa Toast: pemanggil menampilkan status "Tersalin" sendiri.
 * Mengembalikan `false` kalau papan klip tidak tersedia.
 */
internal fun copyToClipboard(context: Context, text: String, label: String): Boolean {
    if (text.isBlank()) return false
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
    return try {
        manager.setPrimaryClip(ClipData.newPlainText(label, text))
        true
    } catch (error: Exception) {
        false
    }
}

/* ----------------------------- builder intent ----------------------------- */

/**
 * Pembangun Intent murni: tidak memegang Activity, tidak meluncurkan apa pun.
 * Pemanggil yang menjalankan `context.startActivity(result)`.
 */
object ShareIntents {

    /** ACTION_SEND teks biasa, sudah dibungkus chooser. */
    fun sendText(context: Context, link: String, name: String): Intent {
        val body = if (name.isBlank()) link else "$name\n$link"
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, body)
            if (name.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, name)
        }
        return Intent.createChooser(send, context.getString(R.string.share_chooser_title))
    }

    /** ACTION_SEND_MULTIPLE untuk beberapa tautan sekaligus (nama, tautan). */
    fun sendMultiple(context: Context, links: List<Pair<String, String>>): Intent {
        val usable = links.filter { it.first.isNotBlank() }
        if (usable.isEmpty()) return sendText(context, "", "")
        if (usable.size == 1) return sendText(context, usable.first().first, usable.first().second)

        val bodies = ArrayList(usable.map { (link, name) -> if (name.isBlank()) link else "$name\n$link" })
        val names = ArrayList(usable.map { it.second })
        val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/plain"
            putStringArrayListExtra(Intent.EXTRA_TEXT, bodies)
            putStringArrayListExtra(Intent.EXTRA_TITLE, names)
        }
        return Intent.createChooser(send, context.getString(R.string.share_chooser_title))
    }
}

/* ------------------------------- komponen ------------------------------- */

/** Baris aksi lembar bagikan: ikon chip, judul, subjudul, aksi tambahan. */
@Composable
fun ShareActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    accent: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val chipTint = when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            accent -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        }
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(34.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = chipTint,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        trailing?.let {
            Spacer(Modifier.width(10.dp))
            it()
        }
    }
}

/** Pil "Tersalin": muncul dengan pudar dan skala halus, hilang tanpa animasi. */
@Composable
fun CopiedPill(
    modifier: Modifier = Modifier,
    text: String? = null,
    visible: Boolean = true,
    reduceMotion: Boolean = false,
) {
    if (!visible) return
    val label = text ?: stringResource(R.string.share_action_copied)
    val appear = remember { Animatable(if (reduceMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            appear.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 200))
        }
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.graphicsLayer {
            alpha = appear.value
            scaleX = 0.86f + 0.14f * appear.value
            scaleY = 0.86f + 0.14f * appear.value
        },
    ) {
        Row(
            Modifier.padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
            )
        }
    }
}

/* -------------------------------- kerangka ------------------------------- */

/**
 * Kerangka lembar bagikan Material 3: sudut atas membulat, gagang geser, dan
 * isi yang bisa digulir. Scrim memakai bawaan M3. Dipakai ketiga lembar supaya
 * chromenya identik.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareSheetFrame(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { ShareDragHandle() },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .then(modifier)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 22.dp),
            content = content,
        )
    }
}

@Composable
private fun ShareDragHandle() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .width(38.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
        )
        Spacer(Modifier.height(6.dp))
    }
}

/**
 * Isi lembar masuk dengan naik halus + pudar. Saat [reduceMotion] aktif,
 * mulai langsung di posisi akhir tanpa animasi.
 */
@Composable
internal fun ShareContentFade(
    reduceMotion: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val appear = remember { Animatable(if (reduceMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            appear.animateTo(1f, tween(durationMillis = 260))
        }
    }

    Column(
        Modifier.graphicsLayer {
            alpha = appear.value
            translationY = (1f - appear.value) * 14.dp.toPx()
        },
        content = content,
    )
}

/* -------------------------------- label jenis ------------------------------- */

/** Nama jenis konten yang bisa diterjemahkan, dipakai kartu pratinjau. */
@Composable
internal fun shareKindLabel(kind: FileKind): String = stringResource(
    when (kind) {
        FileKind.FOLDER -> R.string.share_kind_folder
        FileKind.IMAGE -> R.string.share_kind_image
        FileKind.VIDEO -> R.string.share_kind_video
        FileKind.AUDIO -> R.string.share_kind_audio
        FileKind.PDF -> R.string.share_kind_pdf
        FileKind.DOCUMENT -> R.string.share_kind_document
        FileKind.ARCHIVE -> R.string.share_kind_archive
        FileKind.CODE -> R.string.share_kind_code
        FileKind.APK -> R.string.share_kind_apk
        FileKind.OTHER -> R.string.share_kind_other
    },
)

/* ------------------------------- kesalahan ------------------------------ */

/**
 * Peta [GoFileException] ke pesan ramah. Kode mentah server tidak pernah
 * ditampilkan ke pengguna.
 */
internal fun shareErrorRes(error: Throwable): Int = when (error) {
    is GoFileException.AuthExpired -> R.string.share_error_auth
    is GoFileException.NotPremium -> R.string.share_error_premium
    is GoFileException.RateLimited -> R.string.share_error_rate
    is GoFileException.QuotaExceeded -> R.string.share_error_quota
    is GoFileException.NotFound -> R.string.share_error_notfound
    is GoFileException.Permission -> R.string.share_error_permission
    is GoFileException.Network -> R.string.share_error_network
    is GoFileException.Server -> R.string.share_error_server
    else -> R.string.share_error_generic
}

