package com.nellecrzier.gofileexpressive

import org.json.JSONArray
import org.json.JSONObject

data class TransferInfo(
    val id: String,
    val type: String,
    val fileName: String,
    var progress: Int = 0,
    var state: String = "running",
    var message: String = "",
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type)
        put("fileName", fileName)
        put("progress", progress)
        put("state", state)
        put("message", message)
    }
}

/** Memegang progres transfer di memori proses agar WebView bisa mengambilnya kapan saja. */
object TransferStore {
    private val lock = Any()
    private val items = LinkedHashMap<String, TransferInfo>()

    /** Dipasang MainActivity untuk meneruskan update ke JavaScript. */
    var listener: ((TransferInfo) -> Unit)? = null

    fun add(info: TransferInfo) {
        synchronized(lock) { items[info.id] = info }
        emit(info)
    }

    fun update(
        id: String,
        progress: Int? = null,
        state: String? = null,
        message: String? = null,
    ) {
        val info = synchronized(lock) { items[id] } ?: return
        if (progress != null) info.progress = progress
        if (state != null) info.state = state
        if (message != null) info.message = message
        emit(info)
    }

    fun get(id: String): TransferInfo? = synchronized(lock) { items[id] }

    fun all(): List<TransferInfo> = synchronized(lock) { items.values.toList() }

    fun toJson(): String = JSONArray(all().map { it.toJson() }).toString()

    private fun emit(info: TransferInfo) {
        val snapshot = info.copy()
        listener?.invoke(snapshot)
    }
}
