package com.nellecrzier.gofly

/**
 * Satu tempat untuk semua nilai yang bisa berubah kalau GoFile mengganti
 * rumus token, batas halaman, atau alamat API.
 *
 * `SECRET` dan `USER_AGENT` dipakai untuk menghitung `X-Website-Token`
 * (SHA-256). Kalau GoFile merotasi salah satunya, cukup ubah di sini dan
 * dokumentasikan di README.
 */
object GofileConfig {
    const val API_BASE = "https://api.gofile.io"
    const val UPLOAD_URL = "https://upload.gofile.io/uploadfile"

    /** Kunci publik rumus token website GoFile. */
    const val SECRET = "12af056dacea0b"

    /** Token website berganti tiap periode ini. */
    const val PERIOD_MS = 4 * 60 * 60 * 1000L

    /** Jumlah item per halaman untuk daftar folder. */
    const val PAGE_SIZE = 100

    /** Batas aman jumlah item yang dimuat otomatis (anti-loop). */
    const val MAX_PAGES = 100

    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    const val CONNECT_TIMEOUT_MS = 20_000L
    const val READ_TIMEOUT_MS = 20_000L
    const val UPLOAD_READ_TIMEOUT_MS = 180_000L

    /** Jeda sebelum penghapusan benar-benar dikirim (jendela Undo). */
    const val DELETE_UNDO_MS = 5_000L
}
