package com.nellecrzier.gofly

import org.json.JSONObject

/**
 * Penguraian balasan GoFile dan pemetaan galat — dipisah dari klien HTTP supaya
 * bisa diuji tanpa jaringan.
 *
 * Semua fungsi di sini murni: menerima JSON, mengembalikan model. Tidak ada
 * keputusan UI di dalamnya.
 */
object GofileParsing {

    /** Satu anak folder; null kalau tipenya bukan file/folder. */
    fun itemFrom(id: String, child: JSONObject): GItem? {
        val type = child.optString("type")
        if (!type.equals("folder", true) && !type.equals("file", true)) return null
        return GItem(
            id = id.ifBlank { child.optString("id") },
            name = child.optString("name").ifBlank { id },
            type = type,
            size = child.optLong("size"),
            link = child.optString("link"),
            parentFolder = child.optString("parentFolder"),
            mimetype = child.optString("mimetype"),
            createTime = child.optLong("createTime"),
            modTime = child.optLong("modTime"),
            md5 = child.optString("md5"),
            downloadCount = child.optLong("downloads"),
            childrenCount = child.optInt("childrenCount"),
        )
    }

    /**
     * Isi satu halaman folder. Folder selalu di depan, sisanya mengikuti urutan
     * server; urutan dipertahankan stabil supaya penggabungan halaman tidak
     * menduplikasi baris.
     */
    fun parseFolderPage(
        envelope: JSONObject,
        fallbackPage: Int = 1,
        fallbackPageSize: Int = GofileConfig.PAGE_SIZE,
    ): FolderPage {
        val data = envelope.optJSONObject("data") ?: JSONObject()
        val metadata = envelope.optJSONObject("metadata")
        val children = data.optJSONObject("children")
        val items = mutableListOf<GItem>()
        if (children != null) {
            val keys = children.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val child = children.optJSONObject(id) ?: continue
                itemFrom(id, child)?.let { items += it }
            }
        }
        return FolderPage(
            items = items.sortedBy { if (it.isFolder) 0 else 1 },
            page = metadata?.optInt("page") ?: fallbackPage,
            pageSize = metadata?.optInt("pageSize") ?: fallbackPageSize,
            totalPages = (metadata?.optInt("totalPages") ?: 1).coerceAtLeast(1),
            totalCount = metadata?.optInt("totalCount") ?: items.size,
            hasNextPage = metadata?.optBoolean("hasNextPage") ?: false,
        )
    }

    /** Hasil `/contents/search` — objek berkunci id, bukan array. */
    fun parseSearch(envelope: JSONObject): List<GItem> {
        val data = envelope.optJSONObject("data") ?: return emptyList()
        val items = mutableListOf<GItem>()
        val keys = data.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val child = data.optJSONObject(key) ?: continue
            itemFrom(key, child)?.let { items += it }
        }
        return items
    }

    /** Akun dari `POST /accounts` atau `GET /accounts/website`. */
    fun parseSession(data: JSONObject): GSession = GSession(
        token = data.optString("token"),
        accountId = data.optString("id"),
        rootFolder = data.optString("rootFolder"),
        email = data.optString("email"),
        tier = data.optString("tier"),
    )

    /** Pesan galat yang ramah pengguna, dari status GoFile dan kode HTTP. */
    fun friendlyMessage(status: String, code: Int, serverMessage: String?): String {
        serverMessage?.takeUnless { it.isBlank() }?.let { return it }
        return when (status) {
            "error-token", "error-wrongToken" -> AppStrings.get(R.string.err_session_expired)
            "error-notPremium" -> AppStrings.get(R.string.err_premium_required)
            "error-rateLimit" -> AppStrings.get(R.string.err_rate_limited)
            "error-limits" -> AppStrings.get(R.string.err_quota_full)
            "error-notFound" -> AppStrings.get(R.string.err_not_found)
            "error-email" -> AppStrings.get(R.string.err_invalid_email)
            "error-field", "error-contentsId", "error-tags" -> AppStrings.get(R.string.err_invalid_field)
            "error-owner", "error-notOwner", "error-accountId" ->
                AppStrings.get(R.string.err_not_owner)
            "error-protectedFolder" -> AppStrings.get(R.string.err_protected_folder)
            "error-rootFolder" -> AppStrings.get(R.string.err_root_folder)
            else -> AppStrings.get(if (code != 0) R.string.err_unknown_status_http else R.string.err_unknown_status, status, code)
        }
    }

    /** Petakan status GoFile ke hierarki [GoFileException]. */
    fun errorFor(status: String, code: Int, serverMessage: String?): GoFileException {
        val message = friendlyMessage(status, code, serverMessage)
        return when (status) {
            "error-token", "error-wrongToken" -> GoFileException.AuthExpired(message)
            "error-notPremium" -> GoFileException.NotPremium(message)
            "error-rateLimit" -> GoFileException.RateLimited(message)
            "error-limits", "error-quota" -> GoFileException.QuotaExceeded(message)
            "error-notFound" -> GoFileException.NotFound(message)
            "error-owner", "error-notOwner", "error-accountId",
            "error-protectedFolder", "error-rootFolder" -> GoFileException.Permission(message)
            else -> if (code >= 500) GoFileException.Server(message) else GoFileException.Unknown(message)
        }
    }
}
