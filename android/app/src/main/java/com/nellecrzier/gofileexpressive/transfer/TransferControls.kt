package com.nellecrzier.gofileexpressive.transfer

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.nellecrzier.gofileexpressive.R
import com.nellecrzier.gofileexpressive.TransferInfo
import com.nellecrzier.gofileexpressive.TransferService

/**
 * Jembatan tipis dari UI ke service: jeda, lanjutkan, batal, lalu buka/bagikan
 * berkas hasil unduhan. Semua aksi masuk lewat Intent yang sama dengan yang
 * dipakai tombol notifikasi.
 */
object TransferControls {

    fun pause(context: Context, id: String) = send(context, id, TransferService.ACTION_PAUSE)

    fun resume(context: Context, id: String) = send(context, id, TransferService.ACTION_RESUME)

    fun cancel(context: Context, id: String) = send(context, id, TransferService.ACTION_CANCEL)

    fun open(context: Context, info: TransferInfo): Boolean {
        val uri = outputUri(info) ?: return false
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, info.mimeType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            clipData = ClipData.newRawUri(info.fileName, uri)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun share(context: Context, info: TransferInfo): Boolean {
        val uri = outputUri(info) ?: return false
        val send = Intent(Intent.ACTION_SEND).apply {
            type = info.mimeType.ifBlank { "*/*" }
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, info.fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(info.fileName, uri)
        }
        return try {
            context.startActivity(
                Intent.createChooser(send, context.getString(R.string.transfer_action_share))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun outputUri(info: TransferInfo): Uri? {
        if (info.outputUri.isBlank()) return null
        return try {
            Uri.parse(info.outputUri)
        } catch (_: Exception) {
            null
        }
    }

    private fun send(context: Context, id: String, action: String) {
        if (id.isBlank()) return
        val intent = Intent(context, TransferService::class.java).apply {
            this.action = action
            putExtra(TransferService.EXTRA_TRANSFER_ID, id)
        }
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (_: Exception) {
            // Service sedang berhenti; aksi berikutnya akan mengulanginya.
        }
    }
}
