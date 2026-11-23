package com.nellecrzier.gofly.transfer

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Berkas sementara untuk unduhan: `<id>.part` dan `<id>.json` (validator
 * ETag/Last-Modified) di `filesDir/transfer/partial/`.
 *
 * Pause membiarkan kedua berkas; cancel menghapusnya. Resume memakai ukuran
 * `.part` sebagai titik awal Range.
 */
internal object PartialStore {

    data class Meta(
        val url: String,
        val etag: String,
        val lastModified: String,
        val total: Long,
        val md5: String,
    )

    private const val DIR = "transfer/partial"

    fun dir(context: Context): File {
        val dir = File(context.applicationContext.filesDir, DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun partFile(context: Context, id: String): File =
        File(dir(context), "${safe(id)}.part")

    fun size(context: Context, id: String): Long {
        val file = partFile(context, id)
        return if (file.exists()) file.length() else 0L
    }

    fun readMeta(context: Context, id: String): Meta? {
        val file = File(dir(context), "${safe(id)}.json")
        if (!file.exists()) return null
        return try {
            val json = JSONObject(file.readText())
            Meta(
                url = json.optString("url"),
                etag = json.optString("etag"),
                lastModified = json.optString("lastModified"),
                total = json.optLong("total"),
                md5 = json.optString("md5"),
            )
        } catch (_: Exception) {
            null
        }
    }

    fun writeMeta(context: Context, id: String, meta: Meta) {
        try {
            val json = JSONObject()
                .put("url", meta.url)
                .put("etag", meta.etag)
                .put("lastModified", meta.lastModified)
                .put("total", meta.total)
                .put("md5", meta.md5)
            File(dir(context), "${safe(id)}.json").writeText(json.toString())
        } catch (_: Exception) {
            // Validator bersifat opsional: tanpa itu resume jatuh ke validasi
            // ukuran/Content-Range saja.
        }
    }

    fun clear(context: Context, id: String) {
        partFile(context, id).delete()
        File(dir(context), "${safe(id)}.json").delete()
    }

    private fun safe(id: String): String =
        id.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "transfer" }
}
