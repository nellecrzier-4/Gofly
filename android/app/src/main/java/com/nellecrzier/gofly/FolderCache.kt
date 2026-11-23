package com.nellecrzier.gofly

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Isi folder yang tersimpan di disk agar bisa tampil instan dan offline. */
data class CachedFolder(
    val items: List<GItem>,
    val savedAt: Long,
)

/**
 * Cache JSON per folder di `filesDir/folders`. GoFile tidak menyediakan
 * endpoint "daftar folder" global, jadi cache ini juga menjadi sumber untuk
 * pencarian lokal lintas folder yang pernah dibuka.
 */
class FolderCache(context: Context) {
    private val root = File(context.applicationContext.filesDir, "folders")
    private val memory = ConcurrentHashMap<String, CachedFolder>()

    fun load(folderId: String): CachedFolder? {
        memory[folderId]?.let { return it }
        val file = fileFor(folderId)
        if (!file.exists()) return null
        return runCatching {
            val json = JSONObject(file.readText())
            val cached = CachedFolder(
                items = GItemJson.decodeList(json.optJSONArray("items")?.toString()),
                savedAt = json.optLong("savedAt"),
            )
            memory[folderId] = cached
            cached
        }.getOrNull()
    }

    fun save(folderId: String, items: List<GItem>) {
        if (folderId.isBlank()) return
        val cached = CachedFolder(items, System.currentTimeMillis())
        memory[folderId] = cached
        runCatching {
            root.mkdirs()
            val json = JSONObject().apply {
                put("folderId", folderId)
                put("savedAt", cached.savedAt)
                put("items", JSONArray(GItemJson.encodeList(items)))
            }
            fileFor(folderId).writeText(json.toString())
            prune()
        }
    }

    /** Semua folder yang pernah dibuka — dipakai pencarian global lokal. */
    fun all(): List<CachedFolder> {
        val files = root.listFiles { file -> file.extension == "json" && file.name != "index.json" }
            ?: return emptyList()
        return files.mapNotNull { file ->
            runCatching {
                val json = JSONObject(file.readText())
                CachedFolder(
                    items = GItemJson.decodeList(json.optJSONArray("items")?.toString()),
                    savedAt = json.optLong("savedAt"),
                )
            }.getOrNull()
        }
    }

    fun clear() {
        memory.clear()
        root.listFiles()?.forEach { it.delete() }
    }

    fun sizeBytes(): Long = root.listFiles()?.sumOf { it.length() } ?: 0L

    private fun fileFor(folderId: String): File =
        File(root, folderId.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".json")

    /** Simpan maksimal 60 folder terakhir supaya tidak tumbuh tanpa batas. */
    private fun prune() {
        val files = root.listFiles { file -> file.extension == "json" && file.name != "index.json" }
            ?: return
        if (files.size <= 60) return
        files.sortedBy { it.lastModified() }
            .take(files.size - 60)
            .forEach { it.delete() }
    }
}
