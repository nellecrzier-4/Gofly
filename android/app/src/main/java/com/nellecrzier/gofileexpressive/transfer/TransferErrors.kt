package com.nellecrzier.gofileexpressive.transfer

import android.content.Context
import com.nellecrzier.gofileexpressive.GoFileException
import com.nellecrzier.gofileexpressive.R
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Kategori kegagalan. Dipakai untuk memilih pesan dan menentukan boleh-tidaknya retry. */
internal object TransferErrorKind {
    const val AUTH = "auth"
    const val QUOTA = "quota"
    const val TOO_BIG = "too_big"
    const val STORAGE = "storage"
    const val NETWORK = "network"
    const val NOT_FOUND = "not_found"
    const val PERMISSION = "permission"
    const val NOT_PREMIUM = "not_premium"
    const val RATE_LIMITED = "rate_limited"
    const val EMPTY = "empty"
    const val URI = "uri"
    const val URL = "url"
    const val FOLDER = "folder"
    const val SIZE = "size"
    const val SERVER = "server"
    const val CHECKSUM = "checksum"
    const val UPLOAD = "upload"
    const val DESTINATION = "destination"
    const val FILE_CHANGED = "file_changed"
    const val UNKNOWN = "unknown"
}

internal data class TransferFailure(
    val kind: String,
    val message: String,
    val retryable: Boolean,
)

/** Kesalahan yang sudah diklasifikasikan oleh mesin transfer. */
internal class TransferException(
    val kind: String,
    message: String? = null,
) : Exception(message ?: kind)

/** Balasan HTTP yang tidak sukses, dengan kode dan badan singkat. */
internal class TransferHttpException(
    val code: Int,
    val body: String = "",
) : Exception("HTTP $code")

/**
 * Menerjemahkan semua kegagalan menjadi pesan Indonesia/Inggris yang ramah.
 * Pengguna tidak pernah melihat kode HTTP, nama exception, atau balasan mentah.
 */
internal object TransferErrors {

    fun classify(context: Context, error: Throwable): TransferFailure = when (error) {
        is TransferException -> failure(context, error.kind)
        is GoFileException.AuthExpired -> failure(context, TransferErrorKind.AUTH)
        is GoFileException.NotPremium -> failure(context, TransferErrorKind.NOT_PREMIUM)
        is GoFileException.QuotaExceeded -> {
            val raw = error.message.orEmpty().lowercase()
            if (raw.contains("big") || raw.contains("size") || raw.contains("large")) {
                failure(context, TransferErrorKind.TOO_BIG)
            } else {
                failure(context, TransferErrorKind.QUOTA)
            }
        }
        is GoFileException.RateLimited -> failure(context, TransferErrorKind.RATE_LIMITED)
        is GoFileException.NotFound -> failure(context, TransferErrorKind.NOT_FOUND)
        is GoFileException.Permission -> failure(context, TransferErrorKind.PERMISSION)
        is GoFileException.Network -> failure(context, TransferErrorKind.NETWORK)
        is GoFileException.Server -> failure(context, TransferErrorKind.SERVER)
        is GoFileException.Unknown -> failure(context, TransferErrorKind.UNKNOWN)
        is TransferHttpException -> classifyHttp(context, error)
        is UnknownHostException -> failure(context, TransferErrorKind.NETWORK)
        is SocketTimeoutException -> failure(context, TransferErrorKind.NETWORK)
        is OutOfMemoryError -> failure(context, TransferErrorKind.STORAGE)
        is IOException -> classifyIo(context, error)
        is SecurityException -> failure(context, TransferErrorKind.URI)
        is IllegalStateException -> {
            val raw = error.message.orEmpty().lowercase()
            when {
                raw.contains("enospc") || raw.contains("no space") || raw.contains("space left") ->
                    failure(context, TransferErrorKind.STORAGE)
                raw.contains("folder") -> failure(context, TransferErrorKind.DESTINATION)
                else -> failure(context, TransferErrorKind.UNKNOWN)
            }
        }
        else -> failure(context, TransferErrorKind.UNKNOWN)
    }

    private fun classifyHttp(context: Context, error: TransferHttpException): TransferFailure {
        val raw = error.body.lowercase()
        val kind = when {
            error.code == 401 || error.code == 403 -> TransferErrorKind.AUTH
            error.code == 404 || error.code == 410 -> TransferErrorKind.NOT_FOUND
            error.code == 413 -> TransferErrorKind.TOO_BIG
            error.code == 429 -> TransferErrorKind.NETWORK
            error.code == 507 -> TransferErrorKind.QUOTA
            error.code == 400 && (
                raw.contains("limit") || raw.contains("quota") || raw.contains("space")
                ) -> TransferErrorKind.QUOTA
            error.code == 400 && (raw.contains("big") || raw.contains("size")) ->
                TransferErrorKind.TOO_BIG
            error.code >= 500 -> TransferErrorKind.SERVER
            else -> TransferErrorKind.UNKNOWN
        }
        // 429 dan 5xx pantas dicoba ulang; 4xx lain tidak.
        val retryable = kind == TransferErrorKind.SERVER || error.code == 429
        return failure(context, kind).copy(retryable = retryable)
    }

    private fun classifyIo(context: Context, error: IOException): TransferFailure {
        val raw = error.message.orEmpty().lowercase()
        return when {
            raw.contains("enospc") || raw.contains("no space") || raw.contains("space left") ->
                failure(context, TransferErrorKind.STORAGE)
            raw.contains("permission") || raw.contains("eacces") ->
                failure(context, TransferErrorKind.DESTINATION)
            else -> failure(context, TransferErrorKind.NETWORK)
        }
    }

    fun failure(context: Context, kind: String): TransferFailure {
        val res = when (kind) {
            TransferErrorKind.AUTH -> R.string.transfer_error_expired
            TransferErrorKind.QUOTA -> R.string.transfer_error_quota
            TransferErrorKind.TOO_BIG -> R.string.transfer_error_too_big
            TransferErrorKind.STORAGE -> R.string.transfer_error_storage_full
            TransferErrorKind.NETWORK -> R.string.transfer_error_network
            TransferErrorKind.NOT_FOUND -> R.string.transfer_error_not_found
            TransferErrorKind.PERMISSION -> R.string.transfer_error_permission
            TransferErrorKind.NOT_PREMIUM -> R.string.transfer_error_not_premium
            TransferErrorKind.RATE_LIMITED -> R.string.transfer_error_rate_limited
            TransferErrorKind.EMPTY -> R.string.transfer_error_empty_file
            TransferErrorKind.URI -> R.string.transfer_error_uri
            TransferErrorKind.URL -> R.string.transfer_error_invalid_url
            TransferErrorKind.FOLDER -> R.string.transfer_error_folder_create
            TransferErrorKind.SIZE -> R.string.transfer_error_size_mismatch
            TransferErrorKind.SERVER -> R.string.transfer_error_server
            TransferErrorKind.CHECKSUM -> R.string.transfer_error_checksum
            TransferErrorKind.UPLOAD -> R.string.transfer_error_upload_rejected
            TransferErrorKind.DESTINATION -> R.string.transfer_error_no_destination
            TransferErrorKind.FILE_CHANGED -> R.string.transfer_error_file_changed
            else -> R.string.transfer_error_unknown
        }
        val retryable = kind == TransferErrorKind.NETWORK ||
            kind == TransferErrorKind.SERVER ||
            kind == TransferErrorKind.RATE_LIMITED
        return TransferFailure(kind = kind, message = context.getString(res), retryable = retryable)
    }
}
