package com.nellecrzier.gofileexpressive

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Satu transfer yang terlihat oleh UI.
 *
 * Lima parameter pertama (id, type, fileName, progress, state) sengaja
 * dipertahankan persis seperti versi lama supaya MainActivity tetap bisa
 * memanggil `TransferInfo(id, "upload", name, 0, "queued")`. Semua data baru
 * punya nilai default dan ditambahkan di belakangnya.
 *
 * `state` yang dipakai mesin transfer:
 * `queued`, `running`, `paused`, `retrying`, `success`, `error`, `cancelled`.
 */
data class TransferInfo(
    val id: String,
    val type: String,
    val fileName: String,
    val progress: Int = 0,
    val state: String = "running",
    val message: String = "",
    val bytesTotal: Long = 0L,
    val bytesDone: Long = 0L,
    val bytesPerSecond: Long = 0L,
    val etaSeconds: Int = -1,
    val attempt: Int = 0,
    val pausable: Boolean = true,
    val outputUri: String = "",
    val outputPath: String = "",
    val mimeType: String = "",
    val md5: String = "",
    val errorKind: String = "",
    val folder: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val isFinished: Boolean
        get() = state == STATE_SUCCESS || state == STATE_ERROR || state == STATE_CANCELLED

    val isActive: Boolean
        get() = !isFinished

    /** "upload" untuk unggahan folder juga, supaya filter tetap sederhana. */
    val direction: String
        get() = if (type.equals("download", ignoreCase = true)) "download" else "upload"

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type)
        put("fileName", fileName)
        put("progress", progress)
        put("state", state)
        put("message", message)
        put("bytesTotal", bytesTotal)
        put("bytesDone", bytesDone)
        put("bytesPerSecond", bytesPerSecond)
        put("etaSeconds", etaSeconds)
        put("attempt", attempt)
        put("pausable", pausable)
        put("outputUri", outputUri)
        put("outputPath", outputPath)
        put("mimeType", mimeType)
        put("md5", md5)
        put("errorKind", errorKind)
        put("folder", folder)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    companion object {
        const val STATE_QUEUED = "queued"
        const val STATE_RUNNING = "running"
        const val STATE_PAUSED = "paused"
        const val STATE_RETRYING = "retrying"
        const val STATE_SUCCESS = "success"
        const val STATE_ERROR = "error"
        const val STATE_CANCELLED = "cancelled"

        fun fromJson(json: JSONObject): TransferInfo = TransferInfo(
            id = json.optString("id"),
            type = json.optString("type", "download"),
            fileName = json.optString("fileName"),
            progress = json.optInt("progress"),
            state = json.optString("state", STATE_QUEUED),
            message = json.optString("message"),
            bytesTotal = json.optLong("bytesTotal"),
            bytesDone = json.optLong("bytesDone"),
            bytesPerSecond = json.optLong("bytesPerSecond"),
            etaSeconds = json.optInt("etaSeconds", -1),
            attempt = json.optInt("attempt"),
            pausable = json.optBoolean("pausable", true),
            outputUri = json.optString("outputUri"),
            outputPath = json.optString("outputPath"),
            mimeType = json.optString("mimeType"),
            md5 = json.optString("md5"),
            errorKind = json.optString("errorKind"),
            folder = json.optBoolean("folder"),
            createdAt = json.optLong("createdAt"),
            updatedAt = json.optLong("updatedAt"),
        )
    }
}

/** Memegang progres transfer di memori proses agar UI Compose bisa mengamatinya. */
object TransferStore {
    private val lock = Any()
    private val items = LinkedHashMap<String, TransferInfo>()
    private val _transfers = MutableStateFlow<List<TransferInfo>>(emptyList())

    val transfers: StateFlow<List<TransferInfo>> = _transfers.asStateFlow()

    fun add(info: TransferInfo) {
        val now = System.currentTimeMillis()
        val stamped = info.copy(
            createdAt = if (info.createdAt > 0L) info.createdAt else now,
            updatedAt = if (info.updatedAt > 0L) info.updatedAt else now,
        )
        synchronized(lock) { items[stamped.id] = stamped }
        publish()
    }

    fun update(
        id: String,
        progress: Int? = null,
        state: String? = null,
        message: String? = null,
        bytesTotal: Long? = null,
        bytesDone: Long? = null,
        bytesPerSecond: Long? = null,
        etaSeconds: Int? = null,
        attempt: Int? = null,
        outputUri: String? = null,
        outputPath: String? = null,
        mimeType: String? = null,
        md5: String? = null,
        errorKind: String? = null,
        folder: Boolean? = null,
    ) {
        var changed = false
        synchronized(lock) {
            val current = items[id]
            if (current != null) {
                // TransferInfo harus immutable: mutation in-place membuat
                // StateFlow mengira daftar lama dan baru sama, sehingga UI
                // tidak pernah menerima update progres.
                val next = current.copy(
                    progress = progress ?: current.progress,
                    state = state ?: current.state,
                    message = message ?: current.message,
                    bytesTotal = bytesTotal ?: current.bytesTotal,
                    bytesDone = bytesDone ?: current.bytesDone,
                    bytesPerSecond = bytesPerSecond ?: current.bytesPerSecond,
                    etaSeconds = etaSeconds ?: current.etaSeconds,
                    attempt = attempt ?: current.attempt,
                    outputUri = outputUri ?: current.outputUri,
                    outputPath = outputPath ?: current.outputPath,
                    mimeType = mimeType ?: current.mimeType,
                    md5 = md5 ?: current.md5,
                    errorKind = errorKind ?: current.errorKind,
                    folder = folder ?: current.folder,
                    updatedAt = System.currentTimeMillis(),
                )
                // Jangan pancarkan ulang kalau isinya benar-benar sama; notifikasi
                // dan database kecil tidak perlu churn tiap 400 ms.
                if (next.copy(updatedAt = 0L) != current.copy(updatedAt = 0L)) {
                    items[id] = next
                    changed = true
                }
            }
        }
        if (changed) publish()
    }

    private fun publish() {
        _transfers.value = synchronized(lock) { items.values.toList() }
    }

    fun get(id: String): TransferInfo? = synchronized(lock) { items[id] }

    /** Menghapus satu transfer dari daftar, misal setelah baris digeser pengguna. */
    fun dismiss(id: String) {
        val removed = synchronized(lock) { items.remove(id) != null }
        if (removed) publish()
    }

    fun dismissAll() {
        val had = synchronized(lock) {
            val any = items.isNotEmpty()
            items.clear()
            any
        }
        if (had) publish()
    }

    fun all(): List<TransferInfo> = synchronized(lock) { items.values.toList() }

    fun toJson(): String = JSONArray(all().map { it.toJson() }).toString()
}
