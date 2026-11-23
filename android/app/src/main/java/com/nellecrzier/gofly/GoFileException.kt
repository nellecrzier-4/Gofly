package com.nellecrzier.gofly

/**
 * Hierarki error GoFile. Setiap jenis dipetakan ke pesan yang ramah di UI
 * (lihat `GoFileErrors.message`) supaya pengguna tidak melihat kode mentah.
 */
sealed class GoFileException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /** `error-token` / `error-wrongToken` — token hilang atau kedaluwarsa. */
    class AuthExpired(message: String) : GoFileException(message)

    /** `error-notPremium` — endpoint hanya untuk akun premium. */
    class NotPremium(message: String) : GoFileException(message)

    /** `error-rateLimit` — terlalu banyak permintaan. */
    class RateLimited(message: String) : GoFileException(message)

    /** `error-limits` — kuota penyimpanan/jumlah konten penuh. */
    class QuotaExceeded(message: String) : GoFileException(message)

    /** `error-notFound` — konten tidak ada atau sudah dihapus. */
    class NotFound(message: String) : GoFileException(message)

    /** `error-owner` / `error-protectedFolder` / sejenisnya. */
    class Permission(message: String) : GoFileException(message)

    /** Gagal menghubungi server (timeout, DNS, koneksi putus). */
    class Network(message: String, cause: Throwable? = null) : GoFileException(message, cause)

    /** HTTP 5xx atau balasan rusak. */
    class Server(message: String, cause: Throwable? = null) : GoFileException(message, cause)

    /** Sisanya. */
    class Unknown(message: String, cause: Throwable? = null) : GoFileException(message, cause)
}
