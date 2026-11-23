package com.nellecrzier.gofly

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cache folder/favorit memakai [GItemJson]; round-trip harus menjaga semua
 * field dan tidak boleh melempar untuk data rusak.
 */
class GItemJsonTest {

    @Test
    fun roundTripPreservesEveryField() {
        val folder = Fixtures.item(
            id = "folder-9",
            name = "Dokumen",
            type = "folder",
            size = 0L,
            link = "",
            parentFolder = "root",
            mimetype = "",
            md5 = "",
            downloadCount = 0L,
            childrenCount = 12,
        )

        val decoded = GItemJson.decode(GItemJson.encode(folder))

        assertEquals(folder, decoded)
        assertTrue(decoded.isFolder)
    }

    @Test
    fun roundTripKeepsLargeNumbersAndEmptyStrings() {
        val file = Fixtures.item(
            id = "file-1",
            name = "besar.iso",
            size = 9_000_000_000L,
            downloadCount = 1_234_567_890L,
            link = "",
        )

        val decoded = GItemJson.decode(GItemJson.encode(file))

        assertEquals(9_000_000_000L, decoded.size)
        assertEquals(1_234_567_890L, decoded.downloadCount)
        assertEquals("", decoded.link)
        assertEquals(file, decoded)
    }

    @Test
    fun decodeAppliesDefaultsToSparseJson() {
        val decoded = GItemJson.decode(
            JSONObject().put("id", "x").put("name", "y"),
        )

        assertEquals("x", decoded.id)
        assertEquals("y", decoded.name)
        // Tipe yang hilang dianggap berkas, bukan folder.
        assertEquals("file", decoded.type)
        assertFalse(decoded.isFolder)
        assertEquals(0L, decoded.size)
        assertEquals("", decoded.md5)
        assertEquals(0, decoded.childrenCount)
    }

    @Test
    fun encodeWritesEveryDocumentedKey() {
        val json = GItemJson.encode(Fixtures.item())
        val expected = listOf(
            "id", "name", "type", "size", "link", "parentFolder", "mimetype",
            "createTime", "modTime", "md5", "downloadCount", "childrenCount",
        )

        expected.forEach { key -> assertTrue("kunci hilang: $key", json.has(key)) }
        assertFalse(json.has("downloads"))
    }

    @Test
    fun encodeListDecodeListRoundTripsItems() {
        val items = listOf(
            Fixtures.item(id = "a", name = "a.txt"),
            Fixtures.item(id = "b", name = "Musik", type = "folder", childrenCount = 3),
            Fixtures.item(id = "c", name = "c.pdf", size = 77L),
        )

        val decoded = GItemJson.decodeList(GItemJson.encodeList(items))

        assertEquals(items, decoded)
        assertEquals("[]", GItemJson.encodeList(emptyList()))
        assertTrue(GItemJson.decodeList("[]").isEmpty())
    }

    @Test
    fun decodeListReturnsEmptyForBlankOrBrokenInput() {
        assertTrue(GItemJson.decodeList(null).isEmpty())
        assertTrue(GItemJson.decodeList("").isEmpty())
        assertTrue(GItemJson.decodeList("   \n").isEmpty())
        assertTrue(GItemJson.decodeList("bukan json").isEmpty())
        assertTrue(GItemJson.decodeList("{}").isEmpty())
    }

    @Test
    fun decodeListSkipsEntriesThatAreNotObjects() {
        val array = JSONArray()
        array.put(JSONObject().put("id", "a").put("name", "a.txt"))
        array.put(42)
        array.put(JSONObject.NULL)
        array.put(JSONObject().put("id", "b").put("name", "b.txt"))

        val decoded = GItemJson.decodeList(array.toString())

        assertEquals(2, decoded.size)
        assertEquals(listOf("a", "b"), decoded.map { it.id })
        assertNotNull(GItemJson.decode(JSONObject()))
    }
}
