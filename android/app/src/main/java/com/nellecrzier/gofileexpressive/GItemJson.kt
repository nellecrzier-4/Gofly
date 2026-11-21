package com.nellecrzier.gofileexpressive

import org.json.JSONArray
import org.json.JSONObject

/**
 * Serialisasi [GItem] untuk cache folder, favorit, dan daftar terbaru.
 * Memakai `org.json` bawaan Android supaya tidak perlu dependency tambahan.
 */
object GItemJson {
    fun encode(item: GItem): JSONObject = JSONObject().apply {
        put("id", item.id)
        put("name", item.name)
        put("type", item.type)
        put("size", item.size)
        put("link", item.link)
        put("parentFolder", item.parentFolder)
        put("mimetype", item.mimetype)
        put("createTime", item.createTime)
        put("modTime", item.modTime)
        put("md5", item.md5)
        put("downloadCount", item.downloadCount)
        put("childrenCount", item.childrenCount)
    }

    fun decode(json: JSONObject): GItem = GItem(
        id = json.optString("id"),
        name = json.optString("name"),
        type = json.optString("type", "file"),
        size = json.optLong("size"),
        link = json.optString("link"),
        parentFolder = json.optString("parentFolder"),
        mimetype = json.optString("mimetype"),
        createTime = json.optLong("createTime"),
        modTime = json.optLong("modTime"),
        md5 = json.optString("md5"),
        downloadCount = json.optLong("downloadCount"),
        childrenCount = json.optInt("childrenCount"),
    )

    fun encodeList(items: List<GItem>): String {
        val array = JSONArray()
        items.forEach { array.put(encode(it)) }
        return array.toString()
    }

    fun decodeList(raw: String?): List<GItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let(::decode)
            }
        }.getOrDefault(emptyList())
    }
}
