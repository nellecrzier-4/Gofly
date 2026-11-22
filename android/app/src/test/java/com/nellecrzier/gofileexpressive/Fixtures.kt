package com.nellecrzier.gofileexpressive

import org.json.JSONObject

/**
 * Fixture JSON kecil yang meniru balasan GoFile asli.
 *
 * Semua helper murni membangun `org.json` — tidak menyentuh jaringan — supaya
 * kelas uji tetap cepat dan deterministik.
 */
object Fixtures {

    /** Satu anak folder seperti yang dikirim `/contents/{id}`. */
    fun child(
        type: String = "file",
        name: String = "",
        size: Long = 0L,
        link: String = "",
        parentFolder: String = "",
        mimetype: String = "",
        createTime: Long = 0L,
        modTime: Long = 0L,
        md5: String = "",
        downloads: Long = 0L,
        childrenCount: Int = 0,
        id: String? = null,
    ): JSONObject = JSONObject().apply {
        put("type", type)
        put("name", name)
        put("size", size)
        put("link", link)
        put("parentFolder", parentFolder)
        put("mimetype", mimetype)
        put("createTime", createTime)
        put("modTime", modTime)
        put("md5", md5)
        put("downloads", downloads)
        put("childrenCount", childrenCount)
        if (id != null) put("id", id)
    }

    /** Peta `children` berkunci id, persis seperti balasan GoFile. */
    fun children(vararg entries: Pair<String, JSONObject>): JSONObject =
        JSONObject().apply { entries.forEach { (id, child) -> put(id, child) } }

    /** Amplop `{"status":"ok","data":{"children":{...}},"metadata":{...}}`. */
    fun folderEnvelope(children: JSONObject?, metadata: JSONObject? = null): JSONObject =
        JSONObject().apply {
            put("status", "ok")
            put("data", JSONObject().apply { if (children != null) put("children", children) })
            if (metadata != null) put("metadata", metadata)
        }

    /** Amplop balasan `/contents/search`: `data` berkunci id. */
    fun searchEnvelope(entries: JSONObject?): JSONObject = JSONObject().apply {
        put("status", "ok")
        if (entries != null) put("data", entries)
    }

    fun metadata(
        page: Int? = null,
        pageSize: Int? = null,
        totalPages: Int? = null,
        totalCount: Int? = null,
        hasNextPage: Boolean? = null,
    ): JSONObject = JSONObject().apply {
        page?.let { put("page", it) }
        pageSize?.let { put("pageSize", it) }
        totalPages?.let { put("totalPages", it) }
        totalCount?.let { put("totalCount", it) }
        hasNextPage?.let { put("hasNextPage", it) }
    }

    /** [GItem] lengkap untuk uji serialisasi. */
    fun item(
        id: String = "item-1",
        name: String = "berkas.bin",
        type: String = "file",
        size: Long = 4096L,
        link: String = "https://store1.gofile.io/download/abc",
        parentFolder: String = "root-folder",
        mimetype: String = "application/octet-stream",
        createTime: Long = 1_700_000_000L,
        modTime: Long = 1_700_000_100L,
        md5: String = "d41d8cd98f00b204e9800998ecf8427e",
        downloadCount: Long = 7L,
        childrenCount: Int = 0,
    ): GItem = GItem(
        id = id,
        name = name,
        type = type,
        size = size,
        link = link,
        parentFolder = parentFolder,
        mimetype = mimetype,
        createTime = createTime,
        modTime = modTime,
        md5 = md5,
        downloadCount = downloadCount,
        childrenCount = childrenCount,
    )

    /** Amplop galat dengan pesan dari server. */
    fun errorEnvelope(status: String, message: String? = null): JSONObject = JSONObject().apply {
        put("status", status)
        put("data", JSONObject().apply { message?.let { put("message", it) } })
    }
}
