package com.nellecrzier.gofileexpressive

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.PowerManager
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.PushbackInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Foreground service (dataSync) untuk unggah/unduh sungguhan.
 * Tetap hidup saat aplikasi ditutup, dengan notifikasi progres.
 */
class TransferService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_UPLOAD = "com.nellecrzier.gofileexpressive.UPLOAD"
        const val ACTION_DOWNLOAD = "com.nellecrzier.gofileexpressive.DOWNLOAD"

        const val EXTRA_TRANSFER_ID = "transferId"
        const val EXTRA_URI = "uri"
        const val EXTRA_TOKEN = "token"
        const val EXTRA_FOLDER_ID = "folderId"
        const val EXTRA_FILE_NAME = "fileName"
        const val EXTRA_URL = "url"

        private const val NOTIFICATION_ID = 1001
        private const val TAG = "TransferService"
        private val active = AtomicInteger(0)

        fun hasNotificationPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
    }

    private var wakeLock: PowerManager.WakeLock? = null
    @Volatile
    private var lastNotifiedProgress = -1

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action != ACTION_UPLOAD && action != ACTION_DOWNLOAD) {
            stopSelfResult(startId)
            return START_NOT_STICKY
        }

        NotificationHelper.createChannel(this)
        acquireWakeLock()
        val isFirst = active.incrementAndGet() == 1
        lastNotifiedProgress = -1
        showProgress("Transfer GoFile", -1, indeterminate = true)

        val transferId = intent.getStringExtra(EXTRA_TRANSFER_ID) ?: UUID.randomUUID().toString()
        val fileName = intent.getStringExtra(EXTRA_FILE_NAME).orEmpty().ifBlank { "berkas" }

        Thread {
            try {
                when (action) {
                    ACTION_UPLOAD -> {
                        val uri = getUriExtra(intent)
                            ?: throw IllegalArgumentException("Berkas tidak ditemukan")
                        doUpload(
                            id = transferId,
                            uri = uri,
                            token = intent.getStringExtra(EXTRA_TOKEN).orEmpty(),
                            folderId = intent.getStringExtra(EXTRA_FOLDER_ID).orEmpty(),
                            fileName = fileName,
                        )
                    }

                    ACTION_DOWNLOAD -> {
                        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
                        if (!url.startsWith("http")) throw IllegalArgumentException("Tautan unduh tidak valid")
                        doDownload(id = transferId, url = url, fileName = fileName)
                    }
                }
            } catch (error: Exception) {
                Log.e(TAG, "Transfer gagal", error)
                val reason = error.message?.take(240) ?: "Transfer gagal"
                TransferStore.update(transferId, state = "error", message = reason, progress = -1)
                showResult("Transfer gagal", fileName, reason, failed = true)
            } finally {
                releaseWakeLock()
                finishOne(startId, isFirst)
            }
        }.start()

        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun getUriExtra(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_URI, Uri::class.java)
        } else {
            intent.getParcelableExtra(EXTRA_URI)
        }

    /* ------------------------------ unggah ------------------------------ */

    private fun doUpload(id: String, uri: Uri, token: String, folderId: String, fileName: String) {
        val safeName = sanitize(fileName)
        val expectedSize = queryFileSize(uri)

        TransferStore.update(id, state = "running", message = "Membaca $safeName")
        showProgress(safeName, -1, indeterminate = expectedSize <= 0)

        val boundary = "----GofileBoundary${System.currentTimeMillis()}"
        val connection = URL("https://upload.gofile.io/uploadfile").openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 180_000
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "GofileExpressive/1.0")
            if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
        }

        val rawOut = connection.outputStream
        val preamble = StringBuilder()
            .append("--").append(boundary).append("\r\n")
            .append("Content-Disposition: form-data; name=\"file\"; filename=\"")
            .append(safeName.replace("\"", "")).append("\"\r\n")
            .append("Content-Type: application/octet-stream\r\n\r\n")
            .toString()
        rawOut.write(preamble.toByteArray(Charsets.UTF_8))

        var fileBytes = 0L
        val counting = CountingOutputStream(rawOut, expectedSize) { uploaded, size ->
            if (size > 0) reportProgress(id, safeName, (uploaded * 100 / size).toInt().coerceAtMost(99))
        }

        try {
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) {
                    throw IllegalArgumentException(
                        "Berkas tidak dapat dibaca. Coba pilih dari aplikasi Files/Downloads.",
                    )
                }
                val buffer = ByteArray(64 * 1024)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    counting.write(buffer, 0, read)
                    fileBytes += read
                }
            }
        } finally {
            counting.flush()
        }

        // Jangan kirim request kosong: ini penyebab "progress penuh tapi tidak
        // ada file di GoFile" ketika URI tidak bisa dibaca service.
        if (fileBytes <= 0) {
            connection.disconnect()
            throw IllegalArgumentException("Berkas kosong (0 byte) atau aksesnya habis. Pilih ulang file.")
        }

        if (folderId.isNotBlank()) rawOut.write(formPart(boundary, "folderId", folderId))
        if (token.isNotBlank()) rawOut.write(formPart(boundary, "token", token))
        rawOut.write("\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8))
        rawOut.flush()

        val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        connection.disconnect()

        val json = try {
            JSONObject(response)
        } catch (_: Exception) {
            throw IllegalArgumentException("Balasan GoFile tidak dikenali: ${response.take(160)}")
        }
        if (json.optString("status") != "ok") {
            val message = json.optJSONObject("data")?.optString("message").takeUnless { it.isNullOrBlank() }
                ?: json.optString("status")
                ?: "GoFile menolak unggahan"
            throw IllegalArgumentException(message)
        }

        val data = json.optJSONObject("data")
        val accepted = data?.optLong("size") ?: -1L
        if (accepted > 0 && accepted != fileBytes) {
            throw IllegalArgumentException(
                "GoFile menerima $accepted byte, padahal berkas berisi $fileBytes byte. Coba unggah ulang.",
            )
        }
        val destination = data?.optString("parentFolder").orEmpty()
        if (folderId.isNotBlank() && destination.isNotBlank() &&
            !destination.equals(folderId, ignoreCase = true)
        ) {
            throw IllegalArgumentException("File masuk ke folder lain, bukan folder saat ini. Coba ulang.")
        }

        reportProgress(id, safeName, 100)
        TransferStore.update(
            id,
            state = "success",
            progress = 100,
            message = "Unggah selesai: $safeName ($fileBytes byte)",
        )
        showResult("Unggah selesai", safeName, "$fileBytes byte tersimpan", failed = false)
    }

    private fun queryFileSize(uri: Uri): Long {
        contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0) {
                    val value = cursor.getLong(index)
                    if (value > 0) return value
                }
            }
        }
        return try {
            contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        } catch (_: Exception) {
            -1L
        }
    }

    private fun formPart(boundary: String, name: String, value: String): ByteArray =
        StringBuilder()
            .append("\r\n--").append(boundary).append("\r\n")
            .append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n")
            .append(value).append("\r\n")
            .toString()
            .toByteArray(Charsets.UTF_8)

    /* ------------------------------ unduh ------------------------------ */

    @Suppress("DEPRECATION")
    private fun doDownload(id: String, url: String, fileName: String) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.apply {
            connectTimeout = 20_000
            readTimeout = 180_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
            setRequestProperty("Accept", "*/*")
            setRequestProperty("Referer", "https://gofile.io/")
        }
        connection.connect()

        val code = connection.responseCode
        if (code !in 200..299) {
            val body = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }?.take(200).orEmpty()
            connection.disconnect()
            throw IllegalArgumentException("Server GoFile menjawab HTTP $code. ${body.ifBlank { "Coba lagi nanti." }}")
        }

        val name = sanitize(fileName.ifBlank { "gofile-download" })
        val expected = connection.contentLengthLong
        val mime = connection.contentType ?: "application/octet-stream"

        TransferStore.update(id, state = "running", message = "Mengunduh $name")
        showProgress(name, -1, indeterminate = expected <= 0)

        val input = PushbackInputStream(connection.inputStream, 1024)
        val probe = ByteArray(1024)
        val probeLength = try { input.read(probe) } catch (_: Exception) { -1 }
        if (probeLength > 0) input.unread(probe, 0, probeLength)
        val head = if (probeLength > 0) String(probe, 0, probeLength, Charsets.UTF_8) else ""
        if (head.startsWith("{\"status\":\"error", ignoreCase = true) ||
            head.contains("error-notPremium", ignoreCase = true) ||
            head.startsWith("{\"error", ignoreCase = true)
        ) {
            connection.disconnect()
            throw IllegalArgumentException("GoFile mengirim pesan error, bukan file: ${head.take(160)}")
        }

        val output: OutputStream
        var mediaUri: Uri? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            mediaUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Gagal membuat berkas di folder Downloads")
            output = contentResolver.openOutputStream(mediaUri)
                ?: throw IllegalStateException("Gagal membuka berkas Downloads")
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Gofile Expressive",
            )
            if (!dir.exists()) dir.mkdirs()
            output = FileOutputStream(File(dir, name))
        }

        var written = 0L
        var success = false
        try {
            input.use { source ->
                output.use { out ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (source.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                        written += read
                        if (expected > 0) {
                            reportProgress(id, name, (written * 100 / expected).toInt().coerceAtMost(99))
                        }
                    }
                }
            }
            if (written <= 0) {
                throw IllegalArgumentException("Server mengirim file kosong (0 byte). Tautan mungkin sudah kedaluwarsa.")
            }
            if (expected > 0 && kotlin.math.abs(expected - written) > 64) {
                throw IllegalArgumentException(
                    "Ukuran tidak sesuai: diharapkan $expected byte, diterima $written byte. Coba lagi.",
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaUri != null) {
                val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                contentResolver.update(mediaUri, done, null, null)
            }
            success = true
        } finally {
            if (!success) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaUri != null) {
                        contentResolver.delete(mediaUri, null, null)
                    }
                } catch (_: Exception) {
                    // abaikan; notify di bawah sudah cukup
                }
            }
            connection.disconnect()
        }

        reportProgress(id, name, 100)
        TransferStore.update(
            id,
            state = "success",
            progress = 100,
            message = "Tersimpan di Downloads: $name ($written byte)",
        )
        showResult("Unduh selesai", name, "$written byte tersimpan", failed = false)
    }

    /* --------------------------- notifikasi --------------------------- */

    private fun baseNotification(title: String, text: String): NotificationCompat.Builder {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_transfer)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }

    private fun showProgress(title: String, progress: Int, indeterminate: Boolean) {
        val notification = baseNotification(
            title,
            if (indeterminate) "Menyiapkan transfer…" else "$progress% selesai",
        )
            .setProgress(100, progress.coerceAtLeast(0), indeterminate)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun reportProgress(id: String, title: String, progress: Int) {
        TransferStore.update(id, progress = progress)
        if (progress - lastNotifiedProgress >= 2 || progress >= 100) {
            lastNotifiedProgress = progress
            showProgress(title, progress, indeterminate = false)
        }
    }

    private fun showResult(title: String, fileName: String, text: String, failed: Boolean) {
        val notification = baseNotification(title, "$fileName · $text")
            .setOngoing(false)
            .setAutoCancel(true)
            .setProgress(0, 0, false)
            .setPriority(if (failed) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun finishOne(startId: Int, wasFirst: Boolean) {
        val remaining = active.decrementAndGet()
        if (remaining <= 0) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
            stopSelfResult(startId)
        } else if (wasFirst) {
            Log.d(TAG, "Masih ada $remaining transfer aktif")
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "gofile:transfer")
            .apply {
                setReferenceCounted(false)
                acquire(10 * 60 * 1000L)
            }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|\\r\\n]"), "_")
            .trim()
            .take(180)
            .ifBlank { "berkas" }

    /** Menghitung byte saat menulis supaya progres unggah akurat. */
    private class CountingOutputStream(
        private val out: OutputStream,
        private val total: Long,
        private val onProgress: (Long, Long) -> Unit,
    ) : OutputStream() {
        private var written = 0L

        override fun write(byte: Int) {
            out.write(byte)
            written += 1
            onProgress(written, total)
        }

        override fun write(bytes: ByteArray, offset: Int, length: Int) {
            out.write(bytes, offset, length)
            written += length
            onProgress(written, total)
        }

        override fun flush() = out.flush()
        override fun close() = out.close()
    }
}
