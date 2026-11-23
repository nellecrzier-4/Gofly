package com.nellecrzier.gofly.ui.preview

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.nellecrzier.gofly.GItem
import com.nellecrzier.gofly.GoFileException
import com.nellecrzier.gofly.GofileConfig
import com.nellecrzier.gofly.R
import java.io.IOException
import java.text.NumberFormat
import java.util.Locale

/**
 * Jenis pratinjau yang dikenali. Ditentukan dari `mimetype` lebih dulu, lalu
 * ekstensi, supaya file dengan MIME kosong tetap terbuka dengan benar.
 */
enum class PreviewKind {
    IMAGE,
    VIDEO,
    AUDIO,
    PDF,
    MARKDOWN,
    CODE,
    TEXT,
    UNSUPPORTED,
}

/** Batas teks yang dirender langsung di Compose. Di atas ini: buka di aplikasi lain. */
const val PREVIEW_TEXT_LIMIT_BYTES: Long = 1024L * 1024L

private val IMAGE_EXT = setOf(
    "jpg", "jpeg", "jfif", "png", "gif", "webp", "bmp", "heic", "heif", "avif",
)
private val VIDEO_EXT = setOf("mp4", "m4v", "mkv", "webm", "mov", "3gp", "ts", "avi", "flv", "wmv")
private val AUDIO_EXT = setOf("mp3", "m4a", "aac", "flac", "ogg", "oga", "opus", "wav", "amr", "mka")
private val MARKDOWN_EXT = setOf("md", "markdown", "mdown", "mkd")
private val TEXT_EXT = setOf("txt", "log", "csv", "tsv", "srt", "vtt", "nfo", "properties")
private val CODE_EXT = setOf(
    "kt", "kts", "java", "js", "mjs", "ts", "tsx", "jsx", "py", "rb", "go", "rs", "c", "h",
    "cpp", "cxx", "hpp", "cs", "swift", "php", "sh", "bash", "zsh", "fish", "json", "xml",
    "yml", "yaml", "toml", "ini", "conf", "sql", "html", "htm", "css", "scss", "sass", "less",
    "gradle", "kts", "lua", "pl", "r", "dart", "scala", "clj", "ex", "exs", "erl", "hs", "vue",
    "svelte", "dockerfile", "make", "cmake", "patch", "diff", "graphql", "proto",
)

/** File ini folder (tidak pernah punya pratinjau). */
fun GItem.previewKind(): PreviewKind {
    if (isFolder) return PreviewKind.UNSUPPORTED
    val mime = mimetype.lowercase(Locale.ROOT)
    val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return when {
        mime.startsWith("image/") || ext in IMAGE_EXT -> PreviewKind.IMAGE
        mime.startsWith("video/") || ext in VIDEO_EXT -> PreviewKind.VIDEO
        mime.startsWith("audio/") || ext in AUDIO_EXT -> PreviewKind.AUDIO
        mime == "application/pdf" || ext == "pdf" -> PreviewKind.PDF
        mime == "text/markdown" || mime == "text/x-markdown" || ext in MARKDOWN_EXT -> PreviewKind.MARKDOWN
        ext in CODE_EXT -> PreviewKind.CODE
        mime.startsWith("text/") || ext in TEXT_EXT -> PreviewKind.TEXT
        else -> PreviewKind.UNSUPPORTED
    }
}

/**
 * Daftar saudara sekandung untuk digeser: hanya jenis yang sama (gambar untuk
 * pager gambar), tanpa folder, dengan item yang dibuka selalu ada di dalamnya.
 */
fun previewAlbumFor(item: GItem, album: List<GItem>, kind: PreviewKind): List<GItem> {
    val siblings = album
        .filter { !it.isFolder && it.previewKind() == kind }
        .distinctBy { it.id }
    return if (siblings.any { it.id == item.id }) siblings else listOf(item) + siblings
}

/**
 * Header yang dibutuhkan CDN GoFile. Token hanya dikirim kalau memang ada;
 * `X-BL` adalah bahasa situs yang dipakai server untuk balasan.
 */
fun previewRequestHeaders(token: String): Map<String, String> {
    val language = Locale.getDefault().toLanguageTag().ifBlank { "en-US" }
    return buildMap {
        if (token.isNotBlank()) put("Authorization", "Bearer $token")
        put("User-Agent", GofileConfig.USER_AGENT)
        put("X-BL", language)
        put("X-Website-Language", language)
    }
}

/** Nama file panjang dipotong di tengah supaya ekstensi tetap terbaca. */
fun previewMiddleEllipsis(text: String, max: Int = 40): String {
    if (text.length <= max || max < 5) return text
    val keep = max - 1
    val head = keep / 2 + keep % 2
    val tail = keep / 2
    return text.take(head) + "\u2026" + text.takeLast(tail)
}

private val SIZE_UNITS = intArrayOf(
    R.string.preview_unit_b,
    R.string.preview_unit_kb,
    R.string.preview_unit_mb,
    R.string.preview_unit_gb,
    R.string.preview_unit_tb,
)

/** Ukuran file dengan satuan dari resource, mengikuti locale angka pengguna. */
@Composable
fun previewSizeText(bytes: Long): String {
    val context = LocalContext.current
    return remember(bytes, context) {
        var value = bytes.coerceAtLeast(0L).toDouble()
        var unit = 0
        while (value >= 1024 && unit < SIZE_UNITS.lastIndex) {
            value /= 1024
            unit++
        }
        val number = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = if (unit == 0) 0 else 1
            minimumFractionDigits = 0
        }
        context.getString(
            R.string.preview_size_format,
            number.format(value),
            context.getString(SIZE_UNITS[unit]),
        )
    }
}

/** `m:ss` untuk pemutar audio. Angka saja, tanpa kata yang perlu diterjemahkan. */
fun previewTimeText(millis: Long): String {
    val total = (millis.coerceAtLeast(0L) / 1000L)
    val minutes = total / 60L
    val seconds = total % 60L
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

/**
 * Error GoFile dipetakan ke pesan ramah. Kode mentah tidak pernah tampil.
 */
@StringRes
fun previewErrorRes(error: Throwable?): Int = when (error) {
    is GoFileException.AuthExpired -> R.string.preview_error_auth
    is GoFileException.NotPremium -> R.string.preview_error_premium
    is GoFileException.NotFound -> R.string.preview_error_not_found
    is GoFileException.RateLimited -> R.string.preview_error_rate_limited
    is GoFileException.QuotaExceeded -> R.string.preview_error_quota
    is GoFileException.Permission -> R.string.preview_error_permission
    is GoFileException.Network -> R.string.preview_error_network
    is GoFileException.Server -> R.string.preview_error_server
    is IOException -> R.string.preview_error_network
    is SecurityException -> R.string.preview_error_permission
    else -> R.string.preview_error_unknown
}

/**
 * `runCatching` yang tidak menelan pembatalan coroutine: pembatalan diteruskan
 * supaya `produceState` berhenti dengan benar saat pratinjau ditutup.
 */
internal inline fun <T> previewRunCatching(block: () -> T): Result<T> =
    runCatching(block).onFailure { error ->
        if (error is kotlinx.coroutines.CancellationException) throw error
    }

/**
 * Byte teks UTF-8, atau null kalau isinya biner. Deteksi sederhana: byte NUL
 * di 4 KB pertama hampir selalu berarti bukan teks.
 */
fun previewDecodeText(bytes: ByteArray): String? {
    if (bytes.isEmpty()) return ""
    val probe = if (bytes.size > 4096) bytes.copyOfRange(0, 4096) else bytes
    if (probe.any { it == 0.toByte() }) return null
    return bytes.toString(Charsets.UTF_8)
}
