package com.nellecrzier.gofly

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

data class GItem(
    val id: String,
    val name: String,
    val type: String,
    val size: Long,
    val link: String,
    val parentFolder: String,
    val mimetype: String,
    val createTime: Long = 0L,
    val modTime: Long = 0L,
    val md5: String = "",
    val downloadCount: Long = 0L,
    val childrenCount: Int = 0,
) {
    val isFolder: Boolean get() = type.equals("folder", ignoreCase = true)
}

/** Satu halaman isi folder beserta metadata paginasi dari GoFile. */
data class FolderPage(
    val items: List<GItem>,
    val page: Int = 1,
    val pageSize: Int = GofileConfig.PAGE_SIZE,
    val totalPages: Int = 1,
    val totalCount: Int = 0,
    val hasNextPage: Boolean = false,
)

/**
 * Klien GoFile native — tidak memakai WebView maupun skrip JS.
 * Token website dihitung langsung dengan SHA-256 (rumus resmi gofile.io).
 *
 * Semua panggilan jaringan memakai OkHttp dengan timeout dan header yang
 * konsisten. Hasilnya dipetakan ke [GoFileException] yang terstruktur.
 */
object GofileApi {
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(GofileConfig.CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(GofileConfig.READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", GofileConfig.USER_AGENT)
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    private val language: String
        get() = Locale.getDefault().toLanguageTag().ifBlank { "en-US" }

    /* ------------------------------- akun ------------------------------- */

    /** Kirim magic link login ke email. */
    fun sendLoginLink(email: String) {
        execute("/accounts", "POST", JSONObject().put("email", email).toString())
    }

    /** Buat akun tamu (tanpa email) — berguna untuk mencoba fitur. */
    fun createGuest(): GSession = accountFrom(execute("/accounts", "POST", "{}"))

    /** Tukar token magic link menjadi data akun. */
    fun getAccount(token: String): GSession =
        accountFrom(execute("/accounts/website", token = token))

    /** Detail akun lengkap (tier, root folder, statistik penyimpanan). */
    fun getAccountDetails(token: String, accountId: String): JSONObject =
        execute("/accounts/$accountId", token = token)

    private fun accountFrom(data: JSONObject): GSession = GofileParsing.parseSession(data)

    /* ------------------------------ folder ------------------------------ */

    /** Ambil isi folder (halaman pertama, kompatibel dengan pemanggil lama). */
    fun getFolder(token: String, folderId: String): List<GItem> =
        getFolderPage(token, folderId, page = 1).items

    /** Ambil satu halaman isi folder beserta metadata paginasi. */
    fun getFolderPage(
        token: String,
        folderId: String,
        page: Int = 1,
        pageSize: Int = GofileConfig.PAGE_SIZE,
        sortField: String = "name",
        sortDirection: Int = 1,
        filter: String? = null,
    ): FolderPage {
        val query = buildString {
            append("page=").append(page.coerceAtLeast(1))
            append("&pageSize=").append(pageSize.coerceIn(1, 1000))
            append("&sortField=").append(sortField)
            append("&sortDirection=").append(if (sortDirection >= 0) 1 else -1)
            if (!filter.isNullOrBlank()) append("&contentFilter=").append(filter)
        }
        val envelope = executeEnvelope(
            "/contents/$folderId?$query",
            token = token,
            extra = contentHeaders(token),
        )
        return GofileParsing.parseFolderPage(envelope, page, pageSize)
    }

    /** Cari isi folder secara rekursif (endpoint premium). */
    fun searchContents(token: String, folderId: String, query: String): List<GItem> {
        val envelope = executeEnvelope(
            "/contents/search?contentId=$folderId&searchedString=${java.net.URLEncoder.encode(query, "UTF-8")}",
            token = token,
            extra = contentHeaders(token),
        )
        return GofileParsing.parseSearch(envelope)
    }

    /** Buat folder baru di dalam folder induk tertentu. */
    fun createFolder(token: String, parentFolderId: String, folderName: String) {
        val body = JSONObject()
            .put("token", token)
            .put("parentFolderId", parentFolderId)
            .put("folderName", folderName)
        execute(
            "/contents/createFolder",
            "POST",
            body.toString(),
            token = token,
            extra = contentHeaders(token),
        )
    }

    /** Ubah satu atribut konten (nama, tags, deskripsi, publik, password, kedaluwarsa). */
    fun updateAttribute(token: String, contentId: String, attribute: String, value: Any?) {
        val body = JSONObject()
            .put("attribute", attribute)
            .put("attributeValue", value ?: "")
        execute(
            "/contents/$contentId/update",
            "PUT",
            body.toString(),
            token = token,
            extra = contentHeaders(token),
        )
    }

    fun renameContent(token: String, contentId: String, newName: String) =
        updateAttribute(token, contentId, "name", newName)

    /** Pindahkan konten (endpoint premium). */
    fun moveContents(token: String, contentIds: List<String>, destinationFolderId: String) {
        val body = JSONObject()
            .put("contentsId", contentIds.joinToString(","))
            .put("folderId", destinationFolderId)
        execute(
            "/contents/move",
            "PUT",
            body.toString(),
            token = token,
            extra = contentHeaders(token),
        )
    }

    /** Salin konten (endpoint premium). */
    fun copyContents(token: String, contentIds: List<String>, destinationFolderId: String) {
        val body = JSONObject()
            .put("contentsId", contentIds.joinToString(","))
            .put("folderId", destinationFolderId)
        execute(
            "/contents/copy",
            "POST",
            body.toString(),
            token = token,
            extra = contentHeaders(token),
        )
    }

    /** Hapus file atau folder beserta isinya. */
    fun deleteContent(token: String, contentId: String) = deleteContents(token, listOf(contentId))

    /** Hapus beberapa konten sekaligus (contentsId dipisah koma). */
    fun deleteContents(token: String, contentIds: List<String>) {
        if (contentIds.isEmpty()) return
        val body = JSONObject()
            .put("token", token)
            .put("contentsId", contentIds.joinToString(","))
        execute(
            "/contents",
            "DELETE",
            body.toString(),
            token = token,
            extra = contentHeaders(token),
        )
    }

    private fun contentHeaders(token: String): Map<String, String> = mapOf(
        "X-Website-Token" to websiteToken(token),
        "X-BL" to language,
    )

    /* ------------------------------ berkas ------------------------------ */

    /**
     * Ambil isi berkas dari CDN ke memori — dipakai pratinjau teks, PDF, dan
     * gambar kecil. Batas 8 MB supaya tidak menghabiskan heap.
     *
     * Node CDN kadang menolak header akun; percobaan pertama memakai header
     * akun, percobaan kedua tanpa header (pola yang sama dengan unduhan).
     */
    fun fetchBytes(url: String, token: String): ByteArray {
        val response = openStream(url, token, withAccountHeaders = true)
            ?: openStream(url, token, withAccountHeaders = false)
            ?: throw GoFileException.Network("Berkas tidak bisa diambil dari server.")
        response.use { res ->
            val body = res.body ?: throw GoFileException.Unknown("Balasan berkas kosong.")
            val declared = body.contentLength()
            if (declared > MAX_PREVIEW_BYTES) {
                throw GoFileException.Unknown("Berkas terlalu besar untuk pratinjau.")
            }
            val bytes = body.bytes()
            if (bytes.size > MAX_PREVIEW_BYTES) {
                throw GoFileException.Unknown("Berkas terlalu besar untuk pratinjau.")
            }
            return bytes
        }
    }

    /**
     * Unduh berkas ke cache aplikasi (bukan Downloads) untuk dibuka pemutar
     * native atau aplikasi lain lewat FileProvider.
     */
    fun downloadToTemp(
        context: android.content.Context,
        url: String,
        fileName: String,
        token: String,
    ): java.io.File {
        val dir = java.io.File(context.cacheDir, "preview").apply { mkdirs() }
        val safe = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "berkas" }
        val target = java.io.File(dir, safe)
        val response = openStream(url, token, withAccountHeaders = true)
            ?: openStream(url, token, withAccountHeaders = false)
            ?: throw GoFileException.Network("Berkas tidak bisa diunduh dari server.")
        response.use { res ->
            val body = res.body ?: throw GoFileException.Unknown("Balasan berkas kosong.")
            target.outputStream().use { out -> body.byteStream().copyTo(out) }
        }
        return target
    }

    /**
     * Buka aliran HTTP dengan opsi header akun. Mengembalikan null jika server
     * menolak (bukan melempar) supaya pemanggil bisa mencoba tanpa header.
     */
    private fun openStream(
        url: String,
        token: String,
        withAccountHeaders: Boolean,
    ): okhttp3.Response? {
        val builder = Request.Builder().url(url)
        if (withAccountHeaders && token.isNotBlank()) {
            builder.header("Authorization", "Bearer $token")
            contentHeaders(token).forEach { (key, value) -> builder.header(key, value) }
        }
        return try {
            val response = client.newCall(builder.build()).execute()
            if (response.isSuccessful) response else {
                response.close()
                null
            }
        } catch (error: IOException) {
            null
        }
    }

    /** Ukuran maksimum pratinjau yang dimuat ke memori. */
    const val MAX_PREVIEW_BYTES: Long = 8L * 1024 * 1024

    /* ------------------------------- token ------------------------------ */

    /** SHA-256(userAgent :: language :: token :: periode :: secret). */
    fun websiteToken(token: String): String {
        val period = System.currentTimeMillis() / GofileConfig.PERIOD_MS
        val message = "${GofileConfig.USER_AGENT}::$language::$token::$period::${GofileConfig.SECRET}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(message.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Terima URL penuh (`https://gofile.io/login/xxxx`) atau token mentah. */
    fun extractToken(input: String): String? {
        val clean = input.trim().replace(Regex("[\\s\"'`]"), "")
        if (clean.isBlank()) return null
        val pattern = "[a-zA-Z0-9_-]{16,64}"
        Regex("login/($pattern)", RegexOption.IGNORE_CASE).find(clean)?.let { return it.groupValues[1] }
        Regex("[?&](?:token|apiToken)=($pattern)", RegexOption.IGNORE_CASE).find(clean)?.let {
            return it.groupValues[1]
        }
        return if (Regex("^$pattern$").matches(clean)) clean else null
    }

    /* ----------------------------- jaringan ----------------------------- */

    private fun execute(
        path: String,
        method: String = "GET",
        body: String? = null,
        token: String? = null,
        extra: Map<String, String> = emptyMap(),
    ): JSONObject = executeEnvelope(path, method, body, token, extra)
        .optJSONObject("data") ?: JSONObject()

    private fun executeEnvelope(
        path: String,
        method: String = "GET",
        body: String? = null,
        token: String? = null,
        extra: Map<String, String> = emptyMap(),
    ): JSONObject {
        val builder = Request.Builder().url(GofileConfig.API_BASE + path)
        extra.forEach { (key, value) -> builder.header(key, value) }
        token?.takeIf { it.isNotBlank() }?.let { builder.header("Authorization", "Bearer $it") }

        val requestBody = if (method == "GET") null else (body ?: "{}").toRequestBody(JSON)
        builder.method(method, requestBody)

        val response = try {
            client.newCall(builder.build()).execute()
        } catch (error: IOException) {
            throw GoFileException.Network(
                "Tidak bisa menghubungi GoFile. Periksa koneksi internet lalu coba lagi.",
                error,
            )
        }

        response.use { res ->
            val text = res.body?.string().orEmpty()
            val json = try {
                JSONObject(text)
            } catch (error: Exception) {
                throw GoFileException.Unknown(
                    "GoFile mengirim balasan yang tidak dikenali: ${text.take(120)}",
                    error,
                )
            }
            val status = json.optString("status")
            if (!res.isSuccessful || status != "ok") {
                val serverMessage = json.optJSONObject("data")?.optString("message")
                    .takeUnless { it.isNullOrBlank() }
                    ?: json.optString("message").takeUnless { it.isBlank() }
                throw mapError(status, res.code, serverMessage)
            }
            return json
        }
    }

    private fun mapError(status: String, code: Int, serverMessage: String?): GoFileException =
        GofileParsing.errorFor(status, code, serverMessage)
}
