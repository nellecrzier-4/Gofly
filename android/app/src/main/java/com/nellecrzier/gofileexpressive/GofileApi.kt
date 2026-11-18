package com.nellecrzier.gofileexpressive

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

class GoFileException(message: String) : Exception(message)

data class GItem(
    val id: String,
    val name: String,
    val type: String,
    val size: Long,
    val link: String,
    val parentFolder: String,
    val mimetype: String,
) {
    val isFolder: Boolean get() = type.equals("folder", ignoreCase = true)
}

/**
 * Klien GoFile native — tidak memakai WebView maupun skrip JS.
 * Token website dihitung langsung dengan SHA-256 (rumus resmi gofile.io).
 */
object GofileApi {
    private const val BASE = "https://api.gofile.io"
    private const val SECRET = "12af056dacea0b"
    private const val PERIOD_MS = 4 * 60 * 60 * 1000L

    private val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    private val language: String
        get() = Locale.getDefault().toLanguageTag().ifBlank { "en-US" }

    /** Kirim magic link login ke email. */
    fun sendLoginLink(email: String) {
        request("/accounts", "POST", JSONObject().put("email", email).toString())
    }

    /** Buat akun tamu (tanpa email) — berguna untuk mencoba fitur. */
    fun createGuest(): GSession = accountFrom(request("/accounts", "POST", "{}"))

    /** Tukar token magic link menjadi data akun. */
    fun getAccount(token: String): GSession = accountFrom(request("/accounts/website", token = token))

    private fun accountFrom(data: JSONObject): GSession = GSession(
        token = data.optString("token"),
        accountId = data.optString("id"),
        rootFolder = data.optString("rootFolder"),
        email = data.optString("email"),
        tier = data.optString("tier"),
    )

    /** Ambil isi folder. */
    fun getFolder(token: String, folderId: String): List<GItem> {
        val query = "page=1&pageSize=100&sortField=name&sortDirection=1"
        val data = request(
            "/contents/$folderId?$query",
            token = token,
            extra = contentHeaders(token),
        )
        val children = data.optJSONObject("children") ?: return emptyList()
        val items = mutableListOf<GItem>()
        val keys = children.keys()
        while (keys.hasNext()) {
            val id = keys.next()
            val child = children.optJSONObject(id) ?: continue
            val type = child.optString("type")
            if (!type.equals("folder", true) && !type.equals("file", true)) continue
            items += GItem(
                id = id,
                name = child.optString("name").ifBlank { id },
                type = type,
                size = child.optLong("size"),
                link = child.optString("link"),
                parentFolder = child.optString("parentFolder"),
                mimetype = child.optString("mimetype"),
            )
        }
        return items.sortedWith(compareBy({ !it.isFolder }, { it.name.lowercase(Locale.ROOT) }))
    }

    /** Buat folder baru di dalam folder induk tertentu. */
    fun createFolder(token: String, parentFolderId: String, folderName: String) {
        val body = JSONObject()
            .put("token", token)
            .put("parentFolderId", parentFolderId)
            .put("folderName", folderName)
        request(
            "/contents/createFolder",
            "POST",
            body.toString(),
            token = token,
            extra = contentHeaders(token),
        )
    }

    /** Hapus file atau folder beserta isinya. */
    fun deleteContent(token: String, contentId: String) {
        val body = JSONObject()
            .put("token", token)
            .put("contentsId", contentId)
        request(
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

    /** SHA-256(userAgent :: language :: token :: periode :: secret). */
    fun websiteToken(token: String): String {
        val period = System.currentTimeMillis() / PERIOD_MS
        val message = "$USER_AGENT::$language::$token::$period::$SECRET"
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

    private fun request(
        path: String,
        method: String = "GET",
        body: String? = null,
        token: String? = null,
        extra: Map<String, String> = emptyMap(),
    ): JSONObject {
        val connection = URL(BASE + path).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 20_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
            extra.forEach { (key, value) -> setRequestProperty(key, value) }
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        body?.let { payload ->
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        connection.disconnect()

        val json = try {
            JSONObject(text)
        } catch (_: Exception) {
            throw GoFileException("GoFile mengirim balasan yang tidak dikenali: ${text.take(120)}")
        }
        if (code !in 200..299 || json.optString("status") != "ok") {
            val status = json.optString("status")
            val message = json.optJSONObject("data")?.optString("message").takeUnless { it.isNullOrBlank() }
                ?: json.optString("message").takeUnless { it.isBlank() }
                ?: when (status) {
                    "error-rateLimit" -> "Terlalu banyak percobaan. Tunggu sebentar lalu coba lagi."
                    "error-wrongToken", "error-token" -> "Token tidak valid atau sudah kedaluwarsa."
                    "error-email" -> "Alamat email tidak valid."
                    "error-notPremium" -> "Akun ini tidak punya akses API premium."
                    else -> "GoFile menjawab $status${if (code != 0) " (HTTP $code)" else ""}."
                }
            throw GoFileException(message)
        }
        return json.optJSONObject("data") ?: JSONObject()
    }
}
