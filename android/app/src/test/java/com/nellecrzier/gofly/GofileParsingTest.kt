package com.nellecrzier.gofly

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Penguraian balasan GoFile: folder, pencarian, sesi, dan pemetaan galat.
 * Semua fungsi murni, jadi tidak butuh jaringan maupun Robolectric.
 */
class GofileParsingTest {

    /* ------------------------- parseFolderPage ------------------------- */

    @Test
    fun parseFolderPageReadsEnvelopeAndMetadata() {
        val envelope = Fixtures.folderEnvelope(
            children = Fixtures.children(
                "folder-1" to Fixtures.child(type = "folder", name = "Musik", childrenCount = 4),
                "file-1" to Fixtures.child(type = "file", name = "lagu.mp3", size = 1024),
                "file-2" to Fixtures.child(type = "file", name = "catatan.txt", size = 12),
            ),
            metadata = Fixtures.metadata(
                page = 3,
                pageSize = 25,
                totalPages = 9,
                totalCount = 210,
                hasNextPage = true,
            ),
        )

        val page = GofileParsing.parseFolderPage(envelope)

        assertEquals(3, page.items.size)
        assertEquals(3, page.page)
        assertEquals(25, page.pageSize)
        assertEquals(9, page.totalPages)
        assertEquals(210, page.totalCount)
        assertTrue(page.hasNextPage)
    }

    @Test
    fun parseFolderPagePutsFoldersFirst() {
        val envelope = Fixtures.folderEnvelope(
            children = Fixtures.children(
                "file-1" to Fixtures.child(type = "file", name = "a.txt"),
                "folder-1" to Fixtures.child(type = "folder", name = "Folder A"),
                "file-2" to Fixtures.child(type = "file", name = "b.txt"),
            ),
        )

        val page = GofileParsing.parseFolderPage(envelope)

        assertEquals(3, page.items.size)
        assertTrue(page.items.first().isFolder)
        assertEquals("Folder A", page.items.first().name)
        // Urutan sesama berkas dipertahankan stabil (tidak ada yang hilang).
        assertEquals(
            setOf("Folder A", "a.txt", "b.txt"),
            page.items.map { it.name }.toSet(),
        )
    }

    @Test
    fun parseFolderPageMapsEveryItemField() {
        val envelope = Fixtures.folderEnvelope(
            children = Fixtures.children(
                "content-id" to Fixtures.child(
                    type = "file",
                    name = "film.mp4",
                    size = 987_654_321L,
                    link = "https://store1.gofile.io/download/film",
                    parentFolder = "root-id",
                    mimetype = "video/mp4",
                    createTime = 1_700_000_000L,
                    modTime = 1_700_000_600L,
                    md5 = "0cc175b9c0f1b6a831c399e269772661",
                    downloads = 42L,
                ),
            ),
            metadata = Fixtures.metadata(totalCount = 1),
        )

        val item = GofileParsing.parseFolderPage(envelope).items.single()

        assertEquals("content-id", item.id)
        assertEquals("film.mp4", item.name)
        assertEquals("file", item.type)
        assertEquals(987_654_321L, item.size)
        assertEquals("https://store1.gofile.io/download/film", item.link)
        assertEquals("root-id", item.parentFolder)
        assertEquals("video/mp4", item.mimetype)
        assertEquals(1_700_000_000L, item.createTime)
        assertEquals(1_700_000_600L, item.modTime)
        assertEquals("0cc175b9c0f1b6a831c399e269772661", item.md5)
        assertEquals(42L, item.downloadCount)
        assertFalse(item.isFolder)
    }

    @Test
    fun parseFolderPageSkipsUnsupportedChildTypes() {
        val envelope = Fixtures.folderEnvelope(
            children = Fixtures.children(
                "file-1" to Fixtures.child(type = "file", name = "ok.txt"),
                "odd-1" to Fixtures.child(type = "unknown", name = "aneh"),
                "odd-2" to Fixtures.child(type = "", name = "kosong"),
            ),
        )

        val page = GofileParsing.parseFolderPage(envelope)

        assertEquals(1, page.items.size)
        assertEquals("ok.txt", page.items.single().name)

        // Anak yang bukan objek JSON juga dilewati, bukan membuat crash.
        val broken = Fixtures.children("file-1" to Fixtures.child(type = "file", name = "ok.txt"))
        broken.put("rusak", "bukan objek")
        assertEquals(1, GofileParsing.parseFolderPage(Fixtures.folderEnvelope(broken)).items.size)
    }

    @Test
    fun parseFolderPageUsesFallbacksWithoutMetadata() {
        val envelope = Fixtures.folderEnvelope(
            children = Fixtures.children(
                "file-1" to Fixtures.child(type = "file", name = "a.txt"),
                "file-2" to Fixtures.child(type = "file", name = "b.txt"),
            ),
        )

        val page = GofileParsing.parseFolderPage(envelope, fallbackPage = 2, fallbackPageSize = 50)

        assertEquals(2, page.page)
        assertEquals(50, page.pageSize)
        assertEquals(1, page.totalPages)
        assertEquals(2, page.totalCount)
        assertFalse(page.hasNextPage)

        // Metadata rusak (totalPages 0) tetap menghasilkan minimal satu halaman.
        val clamped = GofileParsing.parseFolderPage(
            Fixtures.folderEnvelope(
                children = Fixtures.children("file-1" to Fixtures.child(type = "file")),
                metadata = Fixtures.metadata(page = 1, totalPages = 0),
            ),
        )
        assertEquals(1, clamped.totalPages)
    }

    @Test
    fun parseFolderPageHandlesMissingData() {
        val page = GofileParsing.parseFolderPage(JSONObject().put("status", "ok"))

        assertTrue(page.items.isEmpty())
        assertEquals(1, page.page)
        assertEquals(GofileConfig.PAGE_SIZE, page.pageSize)
        assertEquals(1, page.totalPages)
        assertEquals(0, page.totalCount)
        assertFalse(page.hasNextPage)
    }

    @Test
    fun parseFolderPageFallsBackToKeyForBlankNameAndChildId() {
        // Nama kosong memakai kunci anak; id kosong memakai id di dalam anak.
        val named = GofileParsing.itemFrom("kunci-id", Fixtures.child(type = "file", name = ""))!!
        assertEquals("kunci-id", named.name)

        val identified = GofileParsing.itemFrom(
            "",
            Fixtures.child(type = "file", name = "punya-id", id = "anak-id"),
        )!!
        assertEquals("anak-id", identified.id)
    }

    /* ---------------------------- parseSearch --------------------------- */

    @Test
    fun parseSearchReturnsItemsKeyedById() {
        val envelope = Fixtures.searchEnvelope(
            Fixtures.children(
                "id-satu" to Fixtures.child(type = "file", name = "satu.txt", size = 1),
                "id-dua" to Fixtures.child(type = "folder", name = "Dua", childrenCount = 3),
            ),
        )

        val items = GofileParsing.parseSearch(envelope)

        assertEquals(2, items.size)
        assertEquals(setOf("id-satu", "id-dua"), items.map { it.id }.toSet())
        assertEquals("Dua", items.single { it.isFolder }.name)
        assertEquals(3, items.single { it.isFolder }.childrenCount)
    }

    @Test
    fun parseSearchSkipsInvalidEntries() {
        val data = Fixtures.children("id-satu" to Fixtures.child(type = "file", name = "satu.txt"))
        data.put("id-dua", "bukan objek")
        data.put("id-tiga", Fixtures.child(type = "lain", name = "abaikan"))

        val items = GofileParsing.parseSearch(Fixtures.searchEnvelope(data))

        assertEquals(1, items.size)
        assertEquals("id-satu", items.single().id)
    }

    @Test
    fun parseSearchReturnsEmptyWhenDataMissing() {
        assertTrue(GofileParsing.parseSearch(JSONObject().put("status", "ok")).isEmpty())
        assertTrue(GofileParsing.parseSearch(Fixtures.searchEnvelope(null)).isEmpty())
        assertTrue(GofileParsing.parseSearch(Fixtures.searchEnvelope(JSONObject())).isEmpty())
    }

    /* --------------------------- parseSession --------------------------- */

    @Test
    fun parseSessionMapsAllFields() {
        val session = GofileParsing.parseSession(
            JSONObject()
                .put("token", "tok-123")
                .put("id", "acc-456")
                .put("rootFolder", "root-789")
                .put("email", "orang@contoh.com")
                .put("tier", "premium"),
        )

        assertEquals("tok-123", session.token)
        assertEquals("acc-456", session.accountId)
        assertEquals("root-789", session.rootFolder)
        assertEquals("orang@contoh.com", session.email)
        assertEquals("premium", session.tier)
        assertTrue(session.isLoggedIn)
    }

    @Test
    fun parseSessionDefaultsToEmpty() {
        val session = GofileParsing.parseSession(JSONObject())

        assertEquals(GSession(), session)
        assertFalse(session.isLoggedIn)
    }

    /* ---------------------------- errorFor ------------------------------ */

    @Test
    fun errorForMapsAuthPremiumAndQuota() {
        assertTrue(GofileParsing.errorFor("error-token", 401, null) is GoFileException.AuthExpired)
        assertTrue(GofileParsing.errorFor("error-wrongToken", 401, null) is GoFileException.AuthExpired)
        assertTrue(GofileParsing.errorFor("error-notPremium", 403, null) is GoFileException.NotPremium)
        assertTrue(GofileParsing.errorFor("error-rateLimit", 429, null) is GoFileException.RateLimited)
        assertTrue(GofileParsing.errorFor("error-limits", 400, null) is GoFileException.QuotaExceeded)
        assertTrue(GofileParsing.errorFor("error-quota", 400, null) is GoFileException.QuotaExceeded)
    }

    @Test
    fun errorForMapsNotFoundAndPermission() {
        assertTrue(GofileParsing.errorFor("error-notFound", 404, null) is GoFileException.NotFound)
        assertTrue(GofileParsing.errorFor("error-owner", 403, null) is GoFileException.Permission)
        assertTrue(GofileParsing.errorFor("error-notOwner", 403, null) is GoFileException.Permission)
        assertTrue(GofileParsing.errorFor("error-accountId", 403, null) is GoFileException.Permission)
        assertTrue(GofileParsing.errorFor("error-protectedFolder", 403, null) is GoFileException.Permission)
        assertTrue(GofileParsing.errorFor("error-rootFolder", 403, null) is GoFileException.Permission)
    }

    @Test
    fun errorForFallsBackToServerThenUnknown() {
        assertTrue(GofileParsing.errorFor("error-aneh", 500, null) is GoFileException.Server)
        assertTrue(GofileParsing.errorFor("error-aneh", 503, null) is GoFileException.Server)
        assertTrue(GofileParsing.errorFor("error-aneh", 0, null) is GoFileException.Unknown)
        assertTrue(GofileParsing.errorFor("error-aneh", 400, null) is GoFileException.Unknown)
    }

    @Test
    fun errorForKeepsServerMessage() {
        val error = GofileParsing.errorFor("error-rateLimit", 429, "Coba lagi 30 detik lagi.")

        assertTrue(error is GoFileException.RateLimited)
        assertEquals("Coba lagi 30 detik lagi.", error.message)
    }

    /* ------------------------- friendlyMessage -------------------------- */

    @Test
    fun friendlyMessagePrefersServerMessageOverStatus() {
        val message = GofileParsing.friendlyMessage("error-token", 401, "Token khusus dari server.")

        assertEquals("Token khusus dari server.", message)
        // Pesan server tidak boleh diganti teks status mentah.
        assertFalse(message.contains("error-token"))
    }

    @Test
    fun friendlyMessageMapsKnownStatusesWhenServerSilent() {
        assertTrue(GofileParsing.friendlyMessage("error-token", 401, null).isNotBlank())
        assertTrue(GofileParsing.friendlyMessage("error-notPremium", 403, "   ").contains("Premium"))
        assertTrue(GofileParsing.friendlyMessage("error-notFound", 404, null).contains("tidak ditemukan"))
        assertFalse(
            GofileParsing.friendlyMessage("error-limits", 400, "").contains("error-limits"),
        )
    }

    @Test
    fun friendlyMessageReportsUnknownStatusWithHttpCode() {
        assertEquals(
            "GoFile menjawab error-xyz (HTTP 418).",
            GofileParsing.friendlyMessage("error-xyz", 418, null),
        )
        assertEquals("GoFile menjawab error-xyz.", GofileParsing.friendlyMessage("error-xyz", 0, null))
    }
}
