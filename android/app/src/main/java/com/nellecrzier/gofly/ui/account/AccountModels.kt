package com.nellecrzier.gofly.ui.account

import androidx.annotation.StringRes
import com.nellecrzier.gofly.GSession
import com.nellecrzier.gofly.GoFileException
import com.nellecrzier.gofly.R
import org.json.JSONArray
import org.json.JSONObject

/** Satu potong rincian penyimpanan per jenis berkas. */
internal data class BreakdownSlice(
    val label: String,
    val bytes: Long,
)

/**
 * Ringkasan akun yang dibaca dari payload GoFile. Semua angka opsional: kalau
 * kunci tidak ada, UI menampilkan "tidak tersedia" alih-alih mengarang nilai.
 */
internal data class AccountSnapshot(
    val email: String = "",
    val tier: String = "",
    val accountId: String = "",
    val rootFolder: String = "",
    val usedBytes: Long? = null,
    val totalBytes: Long? = null,
    val remainingBytes: Long? = null,
    val fileCount: Int? = null,
    val folderCount: Int? = null,
    val breakdown: List<BreakdownSlice> = emptyList(),
) {
    /** Porsi terpakai untuk bilah penyimpanan; null kalau total tidak diketahui. */
    val usedFraction: Float?
        get() {
            val used = usedBytes ?: return null
            val total = totalBytes ?: return null
            if (total <= 0L) return null
            return (used.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
        }

    val hasStorageNumbers: Boolean get() = usedBytes != null || totalBytes != null
}

/** Keadaan pemuatan detail akun. */
internal sealed interface AccountUiState {
    data object Loading : AccountUiState
    data object Expired : AccountUiState

    /** Sesi tamu / tanpa id akun: detail penyimpanan memang tidak bisa dibaca. */
    data object Unavailable : AccountUiState
    data class Ready(val snapshot: AccountSnapshot) : AccountUiState
    data class Failed(val error: AccountError) : AccountUiState
}

/** Jenis galat yang dipetakan ke pesan ramah; tidak ada kode mentah di UI. */
internal enum class AccountError {
    NETWORK,
    RATE,
    SERVER,
    NOT_FOUND,
    PERMISSION,
    PREMIUM,
    QUOTA,
    UNKNOWN,
}

internal fun accountErrorOf(error: Throwable): AccountError = when (error) {
    is GoFileException.Network -> AccountError.NETWORK
    is GoFileException.RateLimited -> AccountError.RATE
    is GoFileException.Server -> AccountError.SERVER
    is GoFileException.NotFound -> AccountError.NOT_FOUND
    is GoFileException.Permission -> AccountError.PERMISSION
    is GoFileException.NotPremium -> AccountError.PREMIUM
    is GoFileException.QuotaExceeded -> AccountError.QUOTA
    else -> AccountError.UNKNOWN
}

@StringRes
internal fun accountErrorRes(error: AccountError): Int = when (error) {
    AccountError.NETWORK -> R.string.settings_account_error_network
    AccountError.RATE -> R.string.settings_account_error_rate
    AccountError.SERVER -> R.string.settings_account_error_server
    AccountError.NOT_FOUND -> R.string.settings_account_error_not_found
    AccountError.PERMISSION -> R.string.settings_account_error_permission
    AccountError.PREMIUM -> R.string.settings_account_error_premium
    AccountError.QUOTA -> R.string.settings_account_error_quota
    AccountError.UNKNOWN -> R.string.settings_account_error_unknown
}

/* ------------------------------ parsing ------------------------------ */

/** Kunci yang mungkin dipakai GoFile untuk rincian per jenis berkas. */
private val BREAKDOWN_KEYS = listOf(
    "storageByType",
    "storageBreakdown",
    "fileTypes",
    "contentTypes",
    "byType",
    "types",
)

/**
 * Baca detail akun dengan `opt*` saja: payload GoFile berubah antar versi, dan
 * kunci yang hilang tidak boleh membuat layar kosong atau angka palsu.
 */
internal fun accountSnapshotOf(payload: JSONObject, session: GSession): AccountSnapshot {
    // `execute()` sudah membuka bungkus "data"; kalau pemanggil memberi bungkus
    // penuh, buka sekali lagi tanpa menebak-nebak.
    val wrapped = payload.optJSONObject("data")
    val root = if (
        wrapped != null &&
        payload.optString("email").isBlank() &&
        payload.optString("tier").isBlank()
    ) {
        wrapped
    } else {
        payload
    }
    val stats = root.optJSONObject("stats")
    val storage = root.optJSONObject("storage")

    val used = longOf(root, "storageUsed", "storage_used", "used")
        ?: longOf(storage, "used", "storageUsed", "storage_used")
    val total = longOf(root, "storageTotal", "storage_total", "total", "quota")
        ?: longOf(storage, "total", "storageTotal", "storage_total", "quota")
    val remaining = longOf(root, "storageRemaining", "storage_remaining", "remaining")
        ?: longOf(storage, "remaining", "storageRemaining", "storage_remaining")
        ?: if (used != null && total != null && total >= used) total - used else null

    return AccountSnapshot(
        email = root.optString("email").ifBlank { session.email },
        tier = root.optString("tier").ifBlank { session.tier },
        accountId = root.optString("id").ifBlank { session.accountId },
        rootFolder = root.optString("rootFolder").ifBlank { session.rootFolder },
        usedBytes = used,
        totalBytes = total,
        remainingBytes = remaining,
        fileCount = (longOf(root, "fileCount", "files") ?: longOf(stats, "fileCount"))?.toInt(),
        folderCount = (longOf(root, "folderCount", "folders")
            ?: longOf(stats, "folderCount"))?.toInt(),
        breakdown = breakdownOf(root, stats),
    )
}

private fun longOf(obj: JSONObject?, vararg keys: String): Long? {
    if (obj == null) return null
    for (key in keys) {
        if (!obj.has(key) || obj.isNull(key)) continue
        val value = when (val raw = obj.opt(key)) {
            is Number -> raw.toLong()
            is String -> raw.toDoubleOrNull()?.toLong()
            else -> null
        }
        if (value != null && value >= 0L) return value
    }
    return null
}

private fun breakdownOf(root: JSONObject, stats: JSONObject?): List<BreakdownSlice> {
    for (source in listOfNotNull(root, stats)) {
        for (key in BREAKDOWN_KEYS) {
            source.optJSONObject(key)?.let { obj ->
                val slices = slicesFromObject(obj)
                if (slices.isNotEmpty()) return slices
            }
            source.optJSONArray(key)?.let { array ->
                val slices = slicesFromArray(array)
                if (slices.isNotEmpty()) return slices
            }
        }
    }
    return emptyList()
}

private fun slicesFromObject(obj: JSONObject): List<BreakdownSlice> {
    val slices = mutableListOf<BreakdownSlice>()
    val keys = obj.keys()
    while (keys.hasNext()) {
        val label = keys.next()
        val value = obj.opt(label)
        val bytes = when (value) {
            is Number -> value.toLong()
            is JSONObject -> longOf(value, "size", "bytes", "total", "value", "used")
            else -> null
        }
        if (bytes != null && bytes > 0L) slices += BreakdownSlice(label, bytes)
    }
    return slices.sortedByDescending { it.bytes }.take(8)
}

private fun slicesFromArray(array: JSONArray): List<BreakdownSlice> {
    val slices = mutableListOf<BreakdownSlice>()
    for (index in 0 until array.length()) {
        val item = array.optJSONObject(index) ?: continue
        val label = listOf("type", "name", "label", "kind", "mime")
            .firstNotNullOfOrNull { key -> item.optString(key).takeIf { it.isNotBlank() } }
            ?: continue
        val bytes = longOf(item, "size", "bytes", "total", "value") ?: continue
        if (bytes > 0L) slices += BreakdownSlice(label, bytes)
    }
    return slices.sortedByDescending { it.bytes }.take(8)
}
