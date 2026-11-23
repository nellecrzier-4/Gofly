package com.nellecrzier.gofly

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [TransferStore] adalah objek tunggal berstatus di memori proses. Setiap uji
 * membersihkannya lebih dulu supaya urutan eksekusi tidak berpengaruh.
 *
 * Perhatikan: [TransferStore.add] memberi cap waktu `createdAt`/`updatedAt`,
 * dan [TransferStore.update] memberi `updatedAt` baru — jadi perbandingan
 * memakai field, bukan kesamaan objek.
 */
class TransferStoreTest {

    private fun transfer(
        id: String = "tr-1",
        name: String = "berkas.zip",
        type: String = "upload",
    ): TransferInfo = TransferInfo(id = id, type = type, fileName = name)

    @Before
    fun resetBefore() {
        TransferStore.dismissAll()
    }

    @After
    fun resetAfter() {
        TransferStore.dismissAll()
    }

    @Test
    fun addPublishesTransferToObservers() {
        TransferStore.add(transfer())

        val published = TransferStore.transfers.value
        assertEquals(1, published.size)
        val item = published.single()
        assertEquals("tr-1", item.id)
        assertEquals("upload", item.type)
        assertEquals("berkas.zip", item.fileName)
        assertEquals(0, item.progress)
        assertEquals("running", item.state)
        // Cap waktu diisi otomatis saat masuk antrean.
        assertTrue(item.createdAt > 0L)
        assertEquals(item.createdAt, item.updatedAt)
        assertEquals(item, TransferStore.get("tr-1"))
        assertEquals(listOf(item), TransferStore.all())
    }

    @Test
    fun addWithSameIdReplacesEntryAndKeepsPosition() {
        TransferStore.add(transfer(id = "tr-1", name = "lama.zip"))
        TransferStore.add(transfer(id = "tr-2", name = "lain.zip"))

        TransferStore.add(transfer(id = "tr-1", name = "baru.zip"))

        assertEquals(2, TransferStore.all().size)
        assertEquals(listOf("tr-1", "tr-2"), TransferStore.all().map { it.id })
        assertEquals("baru.zip", TransferStore.get("tr-1")?.fileName)
    }

    @Test
    fun updatePublishesANewListInstance() {
        TransferStore.add(transfer())
        val before = TransferStore.transfers.value
        val beforeItem = before.single()

        TransferStore.update("tr-1", progress = 40)

        val after = TransferStore.transfers.value
        // Invarian yang didokumentasikan: daftar baru, bukan mutasi di tempat.
        assertNotSame(before, after)
        assertNotEquals(beforeItem, after.single())
        assertEquals(1, after.size)
        assertEquals(40, after.single().progress)
        assertTrue(after.single().updatedAt >= beforeItem.updatedAt)
    }

    @Test
    fun updateMergesOnlyGivenFields() {
        TransferStore.add(
            transfer().copy(progress = 10, state = "running", message = "mulai", bytesTotal = 500L),
        )

        TransferStore.update("tr-1", progress = 55, bytesDone = 220L)

        val updated = TransferStore.get("tr-1")!!
        assertEquals(55, updated.progress)
        assertEquals(220L, updated.bytesDone)
        assertEquals(500L, updated.bytesTotal)
        assertEquals("running", updated.state)
        assertEquals("mulai", updated.message)
        assertEquals("berkas.zip", updated.fileName)

        TransferStore.update("tr-1", state = "done", message = "selesai", errorKind = "")
        assertEquals(55, TransferStore.get("tr-1")!!.progress)
        assertEquals("done", TransferStore.get("tr-1")!!.state)
        assertEquals("selesai", TransferStore.get("tr-1")!!.message)
    }

    @Test
    fun updateWithSameValuesDoesNotPublishAgain() {
        TransferStore.add(transfer())
        TransferStore.update("tr-1", progress = 40)
        val settled = TransferStore.transfers.value

        // Nilai identik: tidak ada churn ke observer (hemat notifikasi).
        TransferStore.update("tr-1", progress = 40)

        assertSame(settled, TransferStore.transfers.value)
        assertEquals(40, TransferStore.get("tr-1")!!.progress)
    }

    @Test
    fun updateUnknownIdLeavesListUntouched() {
        TransferStore.add(transfer())
        val before = TransferStore.transfers.value

        TransferStore.update("tidak-ada", progress = 90)

        assertSame(before, TransferStore.transfers.value)
        assertNull(TransferStore.get("tidak-ada"))
        assertEquals(1, TransferStore.all().size)
    }

    @Test
    fun dismissRemovesEntryAndIgnoresMissingIds() {
        TransferStore.add(transfer(id = "tr-1"))
        TransferStore.add(transfer(id = "tr-2"))

        TransferStore.dismiss("tr-1")

        assertEquals(listOf("tr-2"), TransferStore.all().map { it.id })
        assertNull(TransferStore.get("tr-1"))

        // Id yang tidak ada tidak mengubah apa pun dan tidak melempar.
        val before = TransferStore.transfers.value
        TransferStore.dismiss("tr-1")
        assertSame(before, TransferStore.transfers.value)
        assertNotNull(TransferStore.get("tr-2"))
    }

    @Test
    fun dismissAllClearsEverythingAndStaysStableWhenEmpty() {
        TransferStore.add(transfer(id = "tr-1"))
        TransferStore.add(transfer(id = "tr-2", name = "video.mp4", type = "download"))

        TransferStore.dismissAll()

        assertTrue(TransferStore.transfers.value.isEmpty())
        assertTrue(TransferStore.all().isEmpty())
        assertNull(TransferStore.get("tr-1"))

        // Daftar sudah kosong: tidak perlu memancarkan nilai baru.
        val empty = TransferStore.transfers.value
        TransferStore.dismissAll()
        assertSame(empty, TransferStore.transfers.value)
    }
}
