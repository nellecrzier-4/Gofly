package com.nellecrzier.gofileexpressive

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class TransferInfo(
    val id: String,
    val type: String,
    val fileName: String,
    val progress: Int = 0,
    val state: String = "running",
    val message: String = "",
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

/** Memegang progres transfer di memori proses agar UI Compose bisa mengamatinya. */
object TransferStore {
    private val lock = Any()
    private val items = LinkedHashMap<String, TransferInfo>()
    private val _transfers = MutableStateFlow<List<TransferInfo>>(emptyList())

    val transfers: StateFlow<List<TransferInfo>> = _transfers.asStateFlow()

    fun add(info: TransferInfo) {
        synchronized(lock) { items[info.id] = info }
        publish()
    }

    fun update(
        id: String,
        progress: Int? = null,
        state: String? = null,
        message: String? = null,
    ) {
        var changed = false
        synchronized(lock) {
            val current = items[id]
            if (current != null) {
                // TransferInfo harus immutable: mutation in-place membuat
                // StateFlow mengira daftar lama dan baru sama, sehingga UI
                // tidak pernah menerima update progres.
                items[id] = current.copy(
                    progress = progress ?: current.progress,
                    state = state ?: current.state,
                    message = message ?: current.message,
                )
                changed = true
            }
        }
        if (changed) publish()
    }

    private fun publish() {
        _transfers.value = synchronized(lock) { items.values.toList() }
    }

    fun get(id: String): TransferInfo? = synchronized(lock) { items[id] }

    fun all(): List<TransferInfo> = synchronized(lock) { items.values.toList() }

    fun toJson(): String = JSONArray(all().map { it.toJson() }).toString()
}
