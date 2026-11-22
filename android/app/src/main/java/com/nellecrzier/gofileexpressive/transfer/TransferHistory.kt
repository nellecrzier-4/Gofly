package com.nellecrzier.gofileexpressive.transfer

import android.content.Context
import com.nellecrzier.gofileexpressive.TransferInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.io.File

/**
 * Riwayat transfer yang tahan restart: satu berkas JSON di `filesDir/transfer/`,
 * maksimal 200 entri (yang terlama dibuang lebih dulu). Entri terbaru ada di
 * indeks 0.
 *
 * Ditulis dari thread IO mesin transfer; UI hanya membaca [entries].
 */
object TransferHistory {
    private const val MAX_ENTRIES = 200
    private const val DIR = "transfer"
    private const val FILE_NAME = "history.json"

    private val lock = Any()
    private var loaded = false
    private var file: File? = null
    private val _entries = MutableStateFlow<List<TransferInfo>>(emptyList())

    val entries: StateFlow<List<TransferInfo>> = _entries.asStateFlow()

    /** Panggil sekali sebelum membaca/menulis (service dan panel melakukannya). */
    fun ensureLoaded(context: Context) {
        synchronized(lock) {
            if (loaded) return
            loaded = true
            val dir = File(context.applicationContext.filesDir, DIR)
            if (!dir.exists()) dir.mkdirs()
            val target = File(dir, FILE_NAME)
            file = target
            _entries.value = read(target)
        }
    }

    fun add(info: TransferInfo) {
        if (!loaded) return
        synchronized(lock) {
            val next = (listOf(info) + _entries.value.filterNot { it.id == info.id })
                .take(MAX_ENTRIES)
            _entries.value = next
            write(next)
        }
    }

    fun clear() {
        synchronized(lock) {
            _entries.value = emptyList()
            write(emptyList())
        }
    }

    /** N entri terbaru (indeks 0 = paling baru). */
    fun last(count: Int): List<TransferInfo> =
        _entries.value.take(count.coerceAtLeast(0))

    fun remove(id: String) {
        synchronized(lock) {
            val next = _entries.value.filterNot { it.id == id }
            if (next.size == _entries.value.size) return
            _entries.value = next
            write(next)
        }
    }

    private fun read(target: File): List<TransferInfo> {
        if (!target.exists()) return emptyList()
        return try {
            val text = target.readText()
            if (text.isBlank()) return emptyList()
            val array = JSONArray(text)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(TransferInfo.fromJson(item))
                }
            }
        } catch (_: Exception) {
            // Berkas rusak (mis. proses mati saat menulis): mulai dari kosong
            // daripada membuat aplikasi gagal.
            emptyList()
        }
    }

    private fun write(entries: List<TransferInfo>) {
        val target = file ?: return
        try {
            val array = JSONArray()
            entries.forEach { array.put(it.toJson()) }
            val temp = File(target.parentFile, "$FILE_NAME.tmp")
            temp.writeText(array.toString())
            if (!temp.renameTo(target)) {
                // Rename bisa gagal di beberapa perangkat; tulis langsung.
                target.writeText(array.toString())
                temp.delete()
            }
        } catch (_: Exception) {
            // Riwayat bersifat pelengkap; kegagalan menulis tidak boleh
            // menggagalkan transfer.
        }
    }

    /** Untuk pengujian/alat: jumlah entri yang tersimpan. */
    fun size(): Int = _entries.value.size
}
