package com.nellecrzier.gofly.transfer

import android.net.Uri

/** Jenis pekerjaan yang bisa dijalankan mesin transfer. */
internal enum class TransferKind {
    UPLOAD,
    DOWNLOAD,
    FOLDER_UPLOAD,
}

/**
 * Satu pekerjaan transfer. Semua data yang dibutuhkan mesin ada di sini supaya
 * pekerjaan bisa diantrekan, dijeda, dan dijalankan ulang tanpa menyentuh
 * Intent lagi.
 */
internal data class TransferTask(
    val id: String,
    val kind: TransferKind,
    val name: String,
    val uri: Uri? = null,
    val treeUri: Uri? = null,
    val url: String = "",
    val token: String = "",
    val folderId: String = "",
    val md5: String = "",
    val createdAt: Long = System.currentTimeMillis(),
) {
    /** "upload" atau "download" — sama dengan nilai `TransferInfo.type`. */
    val type: String
        get() = if (kind == TransferKind.DOWNLOAD) "download" else "upload"

    val isFolder: Boolean
        get() = kind == TransferKind.FOLDER_UPLOAD
}

/** Hasil akhir satu pekerjaan yang sukses. */
internal data class TransferResult(
    val message: String,
    val outputUri: String = "",
    val outputPath: String = "",
    val mimeType: String = "",
    val bytesTotal: Long = 0L,
    val bytesDone: Long = 0L,
)

/** Callback progres dari mesin ke antrean (yang lalu menulis ke store + notifikasi). */
internal fun interface ProgressCallback {
    fun onProgress(bytesDone: Long, bytesTotal: Long, bytesPerSecond: Long, etaSeconds: Int)
}
