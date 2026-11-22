package com.nellecrzier.gofileexpressive.transfer

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.nellecrzier.gofileexpressive.GoFileException
import com.nellecrzier.gofileexpressive.GofileApi
import com.nellecrzier.gofileexpressive.GofileConfig
import com.nellecrzier.gofileexpressive.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.PushbackInputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Mesin transfer: unggah (satu berkas atau satu pohon folder), unduh dengan
 * Range/If-Range, verifikasi MD5, dan penulisan ke tujuan akhir.
 *
 * Seluruh jaringan memakai OkHttp (tidak ada WebView, tidak ada stack HTTP
 * lain). Progres dilaporkan lewat [ProgressCallback] yang sudah dibatasi
 * ~400 ms supaya store dan notifikasi tidak dibanjiri.
 *
 * Pembatalan: setiap panggilan HTTP didaftarkan ke Job transfer, jadi pause
 * atau cancel langsung menutup soket, bukan menunggu timeout.
 */
internal class TransferEngine(private val context: Context) {

    private val resolver: ContentResolver get() = context.contentResolver

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun execute(
        task: TransferTask,
        treeUri: String,
        onProgress: ProgressCallback,
        onMessage: (String) -> Unit,
    ): TransferResult = withContext(Dispatchers.IO) {
        when (task.kind) {
            TransferKind.UPLOAD -> uploadSingle(task, onProgress, onMessage)
            TransferKind.DOWNLOAD -> download(task, treeUri, onProgress, onMessage)
            TransferKind.FOLDER_UPLOAD -> uploadTree(task, onProgress, onMessage)
        }
    }

    /* ------------------------------ unggah ------------------------------ */

    private data class UploadSource(val uri: Uri, val size: Long, val temporaryFile: File?)

    private suspend fun uploadSingle(
        task: TransferTask,
        onProgress: ProgressCallback,
        onMessage: (String) -> Unit,
    ): TransferResult {
        val uri = task.uri ?: throw TransferException(TransferErrorKind.URI)
        val source = prepareUploadSource(uri)
        try {
            onMessage(context.getString(R.string.transfer_message_uploading, task.name))
            val emitter = ProgressEmitter(onProgress)
            uploadOne(
                displayName = task.name,
                uri = source.uri,
                size = source.size,
                token = task.token,
                folderId = task.folderId,
                onBytes = { local -> emitter.update(local, source.size) },
            )
            emitter.update(source.size, source.size, force = true)
            return TransferResult(
                message = context.getString(
                    R.string.transfer_message_done_upload,
                    task.name,
                    TransferFormat.bytes(source.size),
                ),
                bytesTotal = source.size,
                bytesDone = source.size,
            )
        } finally {
            source.temporaryFile?.delete()
        }
    }

    /**
     * Unggah folder SAF: buat struktur folder di GoFile (createFolder), lalu
     * unggah setiap anak. Progres dihitung dari total byte seluruh pohon.
     */
    private suspend fun uploadTree(
        task: TransferTask,
        onProgress: ProgressCallback,
        onMessage: (String) -> Unit,
    ): TransferResult {
        val treeUri = task.treeUri ?: throw TransferException(TransferErrorKind.URI)
        val root = try {
            DocumentFile.fromTreeUri(context, treeUri)
        } catch (_: Exception) {
            null
        } ?: throw TransferException(TransferErrorKind.URI)
        if (!root.isDirectory) throw TransferException(TransferErrorKind.URI)

        val totalFiles = countFiles(root, 0)
        if (totalFiles == 0) throw TransferException(TransferErrorKind.EMPTY)
        val totalBytes = countBytes(root, 0)
        val emitter = ProgressEmitter(onProgress)
        val rootName = sanitize(task.name.ifBlank { root.name ?: "Folder" })

        onMessage(context.getString(R.string.transfer_message_creating_folder, rootName))
        val rootId = createFolderId(task.token, task.folderId, rootName)

        var uploaded = 0L
        var index = 0

        suspend fun walk(dir: DocumentFile, parentId: String, depth: Int) {
            if (depth > MAX_FOLDER_DEPTH) return
            val children = try {
                dir.listFiles()
            } catch (_: Exception) {
                emptyArray<DocumentFile>()
            }
            for (child in children) {
                currentCoroutineContext().ensureActive()
                val name = child.name ?: continue
                if (child.isDirectory) {
                    val childId = createFolderId(task.token, parentId, sanitize(name))
                    walk(child, childId, depth + 1)
                } else {
                    index++
                    onMessage(
                        context.getString(
                            R.string.transfer_message_uploading_item,
                            index,
                            totalFiles,
                            name,
                        ),
                    )
                    val source = prepareUploadSource(child.uri)
                    try {
                        uploadOne(
                            displayName = name,
                            uri = source.uri,
                            size = source.size,
                            token = task.token,
                            folderId = parentId,
                            onBytes = { local -> emitter.update(uploaded + local, totalBytes) },
                        )
                    } finally {
                        source.temporaryFile?.delete()
                    }
                    uploaded += source.size
                    emitter.update(uploaded, totalBytes, force = true)
                }
            }
        }

        walk(root, rootId, 0)

        return TransferResult(
            message = context.getString(
                R.string.transfer_message_done_folder,
                rootName,
                totalFiles,
                TransferFormat.bytes(uploaded),
            ),
            bytesTotal = totalBytes,
            bytesDone = uploaded,
        )
    }

    /** Multipart manual dengan Content-Length pasti — GoFile menolak chunked. */
    private suspend fun uploadOne(
        displayName: String,
        uri: Uri,
        size: Long,
        token: String,
        folderId: String,
        onBytes: (Long) -> Unit,
    ): JSONObject {
        val boundary = "----GofileBoundary${System.nanoTime()}"
        val body = MultipartUploadBody(
            boundary = boundary,
            token = token,
            folderId = folderId,
            fileName = sanitize(displayName),
            uri = uri,
            size = size,
            resolver = resolver,
            job = currentCoroutineContext()[Job],
            onBytes = onBytes,
        )
        val request = Request.Builder()
            .url(GofileConfig.UPLOAD_URL)
            .header("Accept", "*/*")
            .header("User-Agent", GofileConfig.USER_AGENT)
            .apply { if (token.isNotBlank()) header("Authorization", "Bearer $token") }
            .post(body)
            .build()

        val call = client.newCall(request)
        val cancelHandle = currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }
        try {
            call.execute().use { response ->
                val text = try {
                    response.body?.string().orEmpty()
                } catch (error: IOException) {
                    currentCoroutineContext().ensureActive()
                    throw error
                }
                if (!response.isSuccessful) {
                    throw TransferHttpException(response.code, text.take(240))
                }

                val json = try {
                    JSONObject(text)
                } catch (_: Exception) {
                    throw TransferException(TransferErrorKind.UPLOAD)
                }
                if (json.optString("status") != "ok") {
                    val data = json.optJSONObject("data")
                    val raw = data?.optString("message").orEmpty()
                        .ifBlank { json.optString("status") }
                        .lowercase(Locale.ROOT)
                    val kind = when {
                        raw.contains("token") -> TransferErrorKind.AUTH
                        raw.contains("premium") -> TransferErrorKind.NOT_PREMIUM
                        raw.contains("limit") || raw.contains("quota") || raw.contains("space") ->
                            TransferErrorKind.QUOTA
                        raw.contains("big") || raw.contains("size") || raw.contains("large") ->
                            TransferErrorKind.TOO_BIG
                        else -> TransferErrorKind.UPLOAD
                    }
                    throw TransferException(kind)
                }

                val data = json.optJSONObject("data")
                val accepted = data?.optLong("size") ?: -1L
                if (accepted > 0 && accepted != size) {
                    throw TransferException(TransferErrorKind.FILE_CHANGED)
                }
                val destination = data?.optString("parentFolder").orEmpty()
                if (folderId.isNotBlank() && destination.isNotBlank() &&
                    !destination.equals(folderId, ignoreCase = true)
                ) {
                    throw TransferException(TransferErrorKind.UPLOAD)
                }
                return json
            }
        } catch (error: IOException) {
            currentCoroutineContext().ensureActive()
            throw error
        } finally {
            cancelHandle?.dispose()
        }
    }

    /**
     * Body multipart yang menulis tepat `Content-Length` byte: token, folderId,
     * header berkas, isi berkas, penutup. GoFile menolak Transfer-Encoding chunked.
     */
    private class MultipartUploadBody(
        private val boundary: String,
        token: String,
        folderId: String,
        fileName: String,
        private val uri: Uri,
        private val size: Long,
        private val resolver: ContentResolver,
        private val job: Job?,
        private val onBytes: (Long) -> Unit,
    ) : RequestBody() {
        private val tokenPart = if (token.isNotBlank()) {
            part("token", token)
        } else {
            ByteArray(0)
        }
        private val folderPart = if (folderId.isNotBlank()) {
            part("folderId", folderId)
        } else {
            ByteArray(0)
        }
        private val fileHeader = (
            "--$boundary\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"${fileName.replace("\"", "")}\"\r\n" +
                "Content-Type: application/octet-stream\r\n\r\n"
            ).toByteArray(Charsets.UTF_8)
        private val closing = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)

        override fun contentType() = "multipart/form-data; boundary=$boundary".toMediaType()

        override fun contentLength(): Long =
            tokenPart.size.toLong() + folderPart.size.toLong() +
                fileHeader.size.toLong() + size + closing.size.toLong()

        override fun writeTo(sink: BufferedSink) {
            if (job != null && !job.isActive) throw IOException("canceled")
            sink.write(tokenPart)
            sink.write(folderPart)
            sink.write(fileHeader)

            val input = resolver.openInputStream(uri) ?: throw TransferException(TransferErrorKind.URI)
            var written = 0L
            input.use { source ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    if (job != null && !job.isActive) throw IOException("canceled")
                    val read = source.read(buffer)
                    if (read == -1) break
                    sink.write(buffer, 0, read)
                    written += read
                    onBytes(written)
                }
            }
            if (written != size) throw TransferException(TransferErrorKind.FILE_CHANGED)
            sink.write(closing)
        }

        private fun part(name: String, value: String): ByteArray =
            "--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n"
                .toByteArray(Charsets.UTF_8)
    }

    /** Pastikan ukuran diketahui; kalau tidak, salin ke cache dulu. */
    private fun prepareUploadSource(uri: Uri): UploadSource {
        val known = queryFileSize(uri)
        if (known > 0) return UploadSource(uri, known, null)
        val temporaryFile = File(context.cacheDir, "upload-${UUID.randomUUID()}.bin")
        val input: InputStream = resolver.openInputStream(uri)
            ?: throw TransferException(TransferErrorKind.URI)
        try {
            input.use { source ->
                FileOutputStream(temporaryFile).use { out -> source.copyTo(out, 64 * 1024) }
            }
        } catch (error: Exception) {
            temporaryFile.delete()
            throw error
        }
        val size = temporaryFile.length()
        if (size <= 0) {
            temporaryFile.delete()
            throw TransferException(TransferErrorKind.EMPTY)
        }
        return UploadSource(Uri.fromFile(temporaryFile), size, temporaryFile)
    }

    private fun queryFileSize(uri: Uri): Long {
        try {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (index >= 0) {
                        val value = cursor.getLong(index)
                        if (value > 0) return value
                    }
                }
            }
        } catch (_: Exception) {
            // Lanjut ke cara kedua.
        }
        return try {
            resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        } catch (_: Exception) {
            -1L
        }
    }

    /** Buat folder dan kembalikan id-nya (createFolder tidak mengembalikan id). */
    private fun createFolderId(token: String, parentId: String, name: String): String {
        try {
            GofileApi.createFolder(token, parentId, name)
        } catch (error: GoFileException) {
            throw error
        } catch (_: Exception) {
            throw TransferException(TransferErrorKind.FOLDER)
        }
        return try {
            GofileApi.getFolder(token, parentId)
                .filter { it.isFolder && it.name.equals(name, ignoreCase = true) }
                .maxByOrNull { it.createTime }
                ?.id
                ?.takeIf { it.isNotBlank() }
                ?: throw TransferException(TransferErrorKind.FOLDER)
        } catch (error: GoFileException) {
            throw error
        } catch (error: TransferException) {
            throw error
        } catch (_: Exception) {
            throw TransferException(TransferErrorKind.FOLDER)
        }
    }

    private fun countFiles(dir: DocumentFile, depth: Int): Int {
        if (depth > MAX_FOLDER_DEPTH) return 0
        val children = try {
            dir.listFiles()
        } catch (_: Exception) {
            return 0
        }
        var count = 0
        for (child in children) {
            count += if (child.isDirectory) countFiles(child, depth + 1) else 1
        }
        return count
    }

    private fun countBytes(dir: DocumentFile, depth: Int): Long {
        if (depth > MAX_FOLDER_DEPTH) return 0L
        val children = try {
            dir.listFiles()
        } catch (_: Exception) {
            return 0L
        }
        var bytes = 0L
        for (child in children) {
            bytes += if (child.isDirectory) {
                countBytes(child, depth + 1)
            } else {
                child.length().coerceAtLeast(0L)
            }
        }
        return bytes
    }

    /* ------------------------------- unduh ------------------------------ */

    private class RestartFromZero : Exception()

    private suspend fun download(
        task: TransferTask,
        treeUri: String,
        onProgress: ProgressCallback,
        onMessage: (String) -> Unit,
    ): TransferResult {
        if (!task.url.startsWith("http", ignoreCase = true)) {
            throw TransferException(TransferErrorKind.URL)
        }
        val part = PartialStore.partFile(context, task.id)
        var existing = if (part.exists()) part.length() else 0L
        var meta = PartialStore.readMeta(context, task.id)
        val hasValidator = meta != null &&
            (meta.etag.isNotBlank() || meta.lastModified.isNotBlank())
        if (existing > 0 && (meta == null || meta.url != task.url || !hasValidator)) {
            // Tanpa If-Range/ETag/Last-Modified, melanjutkan berisiko mencampur
            // dua versi berkas. Mulai dari nol lebih aman.
            part.delete()
            existing = 0L
            meta = null
        }
        if (existing > 0) {
            onMessage(
                context.getString(
                    R.string.transfer_message_resume_from,
                    TransferFormat.bytes(existing),
                ),
            )
        }

        var restarts = 0
        while (true) {
            try {
                return downloadPass(task, part, existing, meta, treeUri, onProgress, onMessage)
            } catch (_: RestartFromZero) {
                restarts++
                if (restarts > 1) throw TransferException(TransferErrorKind.UNKNOWN)
                part.delete()
                existing = 0L
                meta = null
                onMessage(context.getString(R.string.transfer_message_restart_zero))
            }
        }
    }

    private suspend fun downloadPass(
        task: TransferTask,
        part: File,
        existing: Long,
        meta: PartialStore.Meta?,
        treeUri: String,
        onProgress: ProgressCallback,
        onMessage: (String) -> Unit,
    ): TransferResult {
        val validator = meta?.etag?.takeIf { it.isNotBlank() } ?: meta?.lastModified.orEmpty()
        var withAuth = task.token.isNotBlank()

        while (true) {
            val request = downloadRequest(task, existing, validator, withAuth)
            val call = client.newCall(request)
            val cancelHandle = currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }
            try {
                call.execute().use { response ->
                    // Node CDN GoFile kadang menolak header akun: ulangi anonim.
                    if (response.code in 400..499 && response.code != 416 && withAuth) {
                        withAuth = false
                    } else {
                        return handleDownloadResponse(
                            response = response,
                            task = task,
                            part = part,
                            existing = existing,
                            treeUri = treeUri,
                            onProgress = onProgress,
                            onMessage = onMessage,
                        )
                    }
                }
            } catch (error: IOException) {
                currentCoroutineContext().ensureActive()
                throw error
            } finally {
                cancelHandle?.dispose()
            }
        }
    }

    private fun downloadRequest(
        task: TransferTask,
        existing: Long,
        validator: String,
        withAuth: Boolean,
    ): Request = Request.Builder()
        .url(task.url)
        .header("User-Agent", GofileConfig.USER_AGENT)
        .header("Accept", "*/*")
        .header("Accept-Encoding", "identity")
        .header("Referer", "https://gofile.io/")
        .apply {
            if (existing > 0L) {
                header("Range", "bytes=$existing-")
                if (validator.isNotBlank()) header("If-Range", validator)
            }
            if (withAuth && task.token.isNotBlank()) {
                header("Authorization", "Bearer ${task.token}")
                header("X-Website-Token", GofileApi.websiteToken(task.token))
                header("X-BL", Locale.getDefault().toLanguageTag().ifBlank { "en-US" })
            }
        }
        .build()

    private suspend fun handleDownloadResponse(
        response: Response,
        task: TransferTask,
        part: File,
        existing: Long,
        treeUri: String,
        onProgress: ProgressCallback,
        onMessage: (String) -> Unit,
    ): TransferResult {
        val code = response.code
        if (code == 416) throw RestartFromZero()
        if (code !in 200..299) throw TransferHttpException(code, readErrorBody(response))

        val contentType = response.header("Content-Type").orEmpty().lowercase(Locale.ROOT)
        val total: Long
        if (code == 206) {
            val range = parseContentRange(response.header("Content-Range"))
            if (existing <= 0L || range == null || range.first != existing) {
                throw RestartFromZero()
            }
            total = range.second
        } else {
            if (existing > 0L) throw RestartFromZero()
            total = response.body?.contentLength() ?: -1L
        }

        val etag = response.header("ETag").orEmpty()
        val modified = response.header("Last-Modified").orEmpty()
        if (total > 0) {
            PartialStore.writeMeta(
                context,
                task.id,
                PartialStore.Meta(task.url, etag, modified, total, task.md5),
            )
        }
        onMessage(context.getString(R.string.transfer_message_downloading, task.name))

        val emitter = ProgressEmitter(onProgress)
        val digest = if (task.md5.isNotBlank()) MessageDigest.getInstance("MD5") else null
        if (digest != null && existing > 0L && part.exists()) {
            part.inputStream().use { stream -> feed(stream, digest) }
        }

        val body = response.body ?: throw TransferException(TransferErrorKind.EMPTY)
        val input = PushbackInputStream(body.byteStream(), 512)
        if (contentType.contains("text/html")) {
            val probe = ByteArray(512)
            var length = 0
            try {
                length = input.read(probe)
            } catch (_: Exception) {
                length = -1
            }
            if (length > 0) {
                input.unread(probe, 0, length)
                if (String(probe, 0, length, Charsets.UTF_8).trimStart().startsWith("<")) {
                    throw TransferException(TransferErrorKind.UNKNOWN)
                }
            }
        }

        var written = existing
        input.use { source ->
            FileOutputStream(part, existing > 0L).use { out ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = try {
                        source.read(buffer)
                    } catch (error: IOException) {
                        currentCoroutineContext().ensureActive()
                        throw error
                    }
                    if (read == -1) break
                    out.write(buffer, 0, read)
                    digest?.update(buffer, 0, read)
                    written += read
                    emitter.update(written, if (total > 0) total else -1L)
                }
                out.flush()
            }
        }

        if (written <= 0L) throw TransferException(TransferErrorKind.EMPTY)
        if (total > 0 && written != total) throw TransferException(TransferErrorKind.SIZE)
        emitter.update(written, written, force = true)

        if (digest != null) {
            onMessage(context.getString(R.string.transfer_message_verifying))
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (!actual.equals(task.md5.trim(), ignoreCase = true)) {
                part.delete()
                PartialStore.clear(context, task.id)
                throw TransferException(TransferErrorKind.CHECKSUM)
            }
        }

        val sink = DownloadSinks.create(context, task.name, treeUri)
        try {
            part.inputStream().use { source ->
                sink.open().use { out -> source.copyTo(out, 64 * 1024) }
            }
            sink.commit()
        } catch (error: Exception) {
            sink.abort()
            throw error
        }
        part.delete()
        PartialStore.clear(context, task.id)

        return TransferResult(
            message = context.getString(
                R.string.transfer_message_done_download,
                task.name,
                sink.displayPath,
                TransferFormat.bytes(written),
            ),
            outputUri = sink.uri.toString(),
            outputPath = sink.displayPath,
            mimeType = sink.mimeType,
            bytesTotal = written,
            bytesDone = written,
        )
    }

    private fun readErrorBody(response: Response): String = try {
        response.body?.string()?.take(240).orEmpty()
    } catch (_: Exception) {
        ""
    }

    /** `Content-Range: bytes 1024-2047/4096` → (start, total). */
    private fun parseContentRange(value: String?): Pair<Long, Long>? {
        if (value.isNullOrBlank()) return null
        val match = Regex("bytes\\s+(\\d+)-(\\d+)/(\\d+)", RegexOption.IGNORE_CASE)
            .find(value) ?: return null
        val start = match.groupValues[1].toLongOrNull() ?: return null
        val end = match.groupValues[2].toLongOrNull() ?: return null
        val total = match.groupValues[3].toLongOrNull() ?: return null
        if (end < start || total <= 0L) return null
        return start to total
    }

    private fun feed(stream: InputStream, digest: MessageDigest) {
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = stream.read(buffer)
            if (read == -1) break
            digest.update(buffer, 0, read)
        }
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|\\r\\n]"), "_")
            .trim()
            .take(180)
            .ifBlank { "berkas" }

    /* ---------------------------- pembantu ----------------------------- */

    /** Membatasi update ke ~400 ms sekali; kecepatan memakai rata-rata bergerak. */
    private class ProgressEmitter(private val callback: ProgressCallback) {
        private var lastEmit = 0L
        private var lastTime = 0L
        private var lastBytes = 0L
        private var smoothed = 0L

        fun update(done: Long, total: Long, force: Boolean = false) {
            val now = System.currentTimeMillis()
            if (!force && now - lastEmit < PROGRESS_INTERVAL_MS) return
            lastEmit = now
            if (lastTime == 0L) {
                lastTime = now
                lastBytes = 0L
            }
            val elapsed = (now - lastTime).coerceAtLeast(1L)
            val delta = (done - lastBytes).coerceAtLeast(0L)
            val instant = delta * 1000L / elapsed
            smoothed = if (smoothed == 0L) instant else (smoothed * 2 + instant) / 3
            val safeTotal = total.coerceAtLeast(0L)
            val eta = if (safeTotal > 0 && smoothed > 0) {
                ((safeTotal - done).coerceAtLeast(0L) / smoothed).toInt()
            } else {
                -1
            }
            callback.onProgress(done, safeTotal, smoothed, eta)
            lastTime = now
            lastBytes = done
        }
    }

    private companion object {
        const val MAX_FOLDER_DEPTH = 12
        const val PROGRESS_INTERVAL_MS = 400L
    }
}
