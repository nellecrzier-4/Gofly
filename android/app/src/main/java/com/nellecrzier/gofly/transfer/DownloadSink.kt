package com.nellecrzier.gofly.transfer

import com.nellecrzier.gofly.R
import com.nellecrzier.gofly.AppStrings
import android.content.ContentValues
import android.content.Context
import androidx.annotation.RequiresApi
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Tujuan akhir unduhan. Mesin mengunduh ke berkas `.part` dulu (supaya bisa
 * dijeda/dilanjutkan), lalu menyalin hasilnya ke sink ini.
 *
 * Tiga kemungkinan, sesuai urutan prioritas:
 * 1. folder SAF pilihan pengguna (`downloadTree`) lewat DocumentFile,
 * 2. MediaStore Downloads di Android 10+,
 * 3. `filesDir/downloads/` (>= API 26, dan FileProvider sudah memetakannya).
 */
internal abstract class DownloadSink {
    abstract val uri: Uri
    abstract val displayPath: String
    abstract val mimeType: String

    abstract fun open(): OutputStream

    open fun commit() {}

    open fun abort() {}
}

internal object DownloadSinks {

    fun create(context: Context, requestedName: String, treeUri: String): DownloadSink {
        val name = sanitize(requestedName)
        val mime = mimeFor(name)
        if (treeUri.isNotBlank()) {
            safSink(context, treeUri, name, mime)?.let { return it }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaStoreSink(context, name, mime)?.let { return it }
        }
        return fileSink(context, name, mime)
    }

    /**
     * Peta ekstensi kecil dan eksplisit. Sengaja tidak memakai kelas di paket
     * `android.webkit`: aplikasi ini tidak boleh menyentuh WebView sama sekali.
     */
    fun mimeFor(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return MIME_TYPES[ext] ?: "application/octet-stream"
    }

    private val MIME_TYPES = mapOf(
        "jpg" to "image/jpeg", "jpeg" to "image/jpeg", "png" to "image/png",
        "gif" to "image/gif", "webp" to "image/webp", "heic" to "image/heic",
        "bmp" to "image/bmp", "svg" to "image/svg+xml",
        "pdf" to "application/pdf", "txt" to "text/plain", "md" to "text/markdown",
        "csv" to "text/csv", "json" to "application/json", "xml" to "application/xml",
        "html" to "text/html",
        "doc" to "application/msword",
        "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "xls" to "application/vnd.ms-excel",
        "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "ppt" to "application/vnd.ms-powerpoint",
        "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "mp3" to "audio/mpeg", "m4a" to "audio/mp4", "wav" to "audio/wav",
        "flac" to "audio/flac", "ogg" to "audio/ogg", "opus" to "audio/opus",
        "mp4" to "video/mp4", "mkv" to "video/x-matroska", "webm" to "video/webm",
        "avi" to "video/x-msvideo", "mov" to "video/quicktime", "3gp" to "video/3gpp",
        "zip" to "application/zip", "rar" to "application/vnd.rar", "7z" to "application/x-7z-compressed",
        "tar" to "application/x-tar", "gz" to "application/gzip",
        "apk" to "application/vnd.android.package-archive",
    )

    fun sanitize(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|\\r\\n]"), "_")
            .trim()
            .take(180)
            .ifBlank { "gofile-download" }

    /* ------------------------------- SAF -------------------------------- */

    private fun safSink(context: Context, treeUri: String, name: String, mime: String): DownloadSink? {
        return try {
            val uri = Uri.parse(treeUri)
            val tree = DocumentFile.fromTreeUri(context, uri) ?: return null
            if (!tree.isDirectory || !tree.canWrite()) return null
            val finalName = uniqueName(name) { candidate -> tree.findFile(candidate) != null }
            val document = tree.createFile(mime, finalName) ?: return null
            // Sebagian penyedia menambahkan ekstensi sendiri; rapikan namanya.
            if (document.name != finalName && !document.renameTo(finalName)) {
                // Nama dari penyedia tetap dipakai; pengguna melihat nama aslinya.
            }
            SafSink(
                context = context,
                document = document,
                displayPath = "${tree.name ?: AppStrings.get(R.string.folder_default_name)}/${document.name ?: finalName}",
                mimeType = mime,
            )
        } catch (_: Exception) {
            null
        }
    }

    private class SafSink(
        private val context: Context,
        private val document: DocumentFile,
        override val displayPath: String,
        override val mimeType: String,
    ) : DownloadSink() {
        override val uri: Uri = document.uri

        override fun open(): OutputStream =
            context.contentResolver.openOutputStream(document.uri, "wt")
                ?: throw TransferException(TransferErrorKind.DESTINATION)

        override fun abort() {
            try {
                document.delete()
            } catch (_: Exception) {
                // Biarkan berkas kosong; pengguna bisa menghapusnya sendiri.
            }
        }
    }

    /* ---------------------------- MediaStore ---------------------------- */

    private fun mediaStoreSink(context: Context, name: String, mime: String): DownloadSink? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val finalName = uniqueName(name) { candidate -> mediaStoreNameTaken(context, candidate) }
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, finalName)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver
                .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            MediaStoreSink(context, uri, finalName, mime)
        } catch (_: Exception) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun mediaStoreNameTaken(context: Context, name: String): Boolean = try {
        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.DISPLAY_NAME} = ?",
            arrayOf(name),
            null,
        )?.use { cursor -> cursor.moveToFirst() } ?: false
    } catch (_: Exception) {
        false
    }

    private class MediaStoreSink(
        private val context: Context,
        override val uri: Uri,
        private val name: String,
        override val mimeType: String,
    ) : DownloadSink() {
        override val displayPath: String = "${Environment.DIRECTORY_DOWNLOADS}/$name"

        override fun open(): OutputStream =
            context.contentResolver.openOutputStream(uri, "wt")
                ?: throw TransferException(TransferErrorKind.DESTINATION)

        override fun commit() {
            try {
                val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                context.contentResolver.update(uri, done, null, null)
            } catch (_: Exception) {
                // Berkas tetap ada; hanya penanda pending yang gagal dilepas.
            }
        }

        override fun abort() {
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (_: Exception) {
                // abaikan
            }
        }
    }

    /* --------------------------- internal file -------------------------- */

    private fun fileSink(context: Context, name: String, mime: String): DownloadSink {
        val dir = File(context.filesDir, "downloads")
        if (!dir.exists()) dir.mkdirs()
        val finalName = uniqueName(name) { candidate -> File(dir, candidate).exists() }
        val file = File(dir, finalName)
        if (!dir.canWrite()) throw TransferException(TransferErrorKind.DESTINATION)
        return FileSink(context, file, mime)
    }

    private class FileSink(
        private val context: Context,
        private val file: File,
        override val mimeType: String,
    ) : DownloadSink() {
        override val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.files",
            file,
        )
        override val displayPath: String = "Downloads/${file.name}"

        override fun open(): OutputStream = FileOutputStream(file)

        override fun abort() {
            file.delete()
        }
    }

    /** `nama (1).ext`, `nama (2).ext`, … sampai tidak bentrok. */
    private fun uniqueName(base: String, taken: (String) -> Boolean): String {
        if (!taken(base)) return base
        val dot = base.lastIndexOf('.')
        val stem = if (dot > 0) base.substring(0, dot) else base
        val ext = if (dot > 0) base.substring(dot) else ""
        var index = 1
        while (index < 1000) {
            val candidate = "$stem ($index)$ext"
            if (!taken(candidate)) return candidate
            index++
        }
        return "$stem-${System.currentTimeMillis()}$ext"
    }
}
