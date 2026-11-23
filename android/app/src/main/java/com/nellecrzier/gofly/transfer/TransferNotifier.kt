package com.nellecrzier.gofly.transfer

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.nellecrzier.gofly.MainActivity
import com.nellecrzier.gofly.NotificationHelper
import com.nellecrzier.gofly.R
import com.nellecrzier.gofly.TransferInfo
import com.nellecrzier.gofly.TransferService
import com.nellecrzier.gofly.TransferStore

/**
 * Semua notifikasi transfer:
 * - satu notifikasi ringkasan yang menjadi notifikasi foreground service
 *   (id tetap [SUMMARY_ID], selalu diperbarui dalam 5 detik pertama),
 * - satu notifikasi progres per transfer (id stabil dari id transfer, grup
 *   stabil per transfer, aksi Jeda/Lanjutkan + Batal),
 * - notifikasi hasil dengan aksi Buka/Bagikan yang auto-cancel.
 *
 * Teks selalu dari resource `transfer_*`, tidak pernah kode mentah.
 */
internal class TransferNotifier(
    private val service: Service,
) {
    private val context: Context get() = service
    private val manager: NotificationManager by lazy {
        service.getSystemService(NotificationManager::class.java)
    }

    /* ----------------------------- foreground ----------------------------- */

    /** Wajib dipanggil di awal setiap `onStartCommand`. */
    fun startForeground() {
        val notification = summary()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                service,
                SUMMARY_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            @Suppress("DEPRECATION")
            service.startForeground(SUMMARY_ID, notification)
        }
    }

    /** Perbarui ringkasan tanpa mengubah status foreground. */
    fun refreshSummary() {
        if (!TransferService.hasNotificationPermission(context)) return
        try {
            manager.notify(SUMMARY_ID, summary())
        } catch (_: Exception) {
            // Notifikasi ringkasan bukan hal kritis.
        }
    }

    private fun summary(): Notification {
        val items = TransferStore.all().filter { it.isActive }
        val running = items.count {
            it.state == TransferInfo.STATE_RUNNING || it.state == TransferInfo.STATE_RETRYING
        }
        val queued = items.count { it.state == TransferInfo.STATE_QUEUED }
        val paused = items.count { it.state == TransferInfo.STATE_PAUSED }

        val text = when {
            running > 0 -> context.getString(R.string.transfer_notification_summary, running, queued)
            paused > 0 -> context.getString(R.string.transfer_notification_summary_paused, paused)
            else -> context.getString(R.string.transfer_notification_summary_idle)
        }
        var doneBytes = 0L
        var totalBytes = 0L
        items.forEach {
            if (it.bytesTotal > 0) {
                doneBytes += it.bytesDone
                totalBytes += it.bytesTotal
            }
        }
        val determinate = totalBytes > 0L
        val percent = if (determinate) {
            ((doneBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_transfer)
            .setContentTitle(context.getString(R.string.transfer_notification_title))
            .setContentText(text)
            .setContentIntent(openApp())
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setGroup(GROUP_SUMMARY)
            .setGroupSummary(true)
            .setProgress(100, percent, !determinate)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        return builder.build()
    }

    /* --------------------------- progres per item -------------------------- */

    fun progress(info: TransferInfo) {
        if (!TransferService.hasNotificationPermission(context)) return
        val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_transfer)
            .setContentTitle(info.fileName.ifBlank { context.getString(R.string.transfer_notification_title) })
            .setContentText(statusText(info))
            .setSubText(typeLabel(info))
            .setContentIntent(openApp())
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setGroup(groupFor(info.id))
            .setProgress(
                100,
                info.progress.coerceAtLeast(0),
                info.progress < 0 || info.state == TransferInfo.STATE_QUEUED,
            )

        if (info.state == TransferInfo.STATE_PAUSED) {
            builder.addAction(
                0,
                context.getString(R.string.transfer_action_resume),
                serviceAction(TransferService.ACTION_RESUME, info.id, 0),
            )
        } else {
            builder.addAction(
                0,
                context.getString(R.string.transfer_action_pause),
                serviceAction(TransferService.ACTION_PAUSE, info.id, 1),
            )
        }
        builder.addAction(
            0,
            context.getString(R.string.transfer_action_cancel),
            serviceAction(TransferService.ACTION_CANCEL, info.id, 2),
        )

        try {
            manager.notify(notificationId(info.id), builder.build())
        } catch (_: Exception) {
            // Izin notifikasi bisa dicabut saat transfer berjalan.
        }
    }

    fun cancel(id: String) {
        try {
            manager.cancel(notificationId(id))
        } catch (_: Exception) {
            // abaikan
        }
    }

    /* -------------------------------- hasil -------------------------------- */

    /** Pembatalan oleh pengguna: notifikasi singkat tanpa aksi Buka/Bagikan. */
    fun cancelled(info: TransferInfo) {
        if (!TransferService.hasNotificationPermission(context)) return
        val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_transfer)
            .setContentTitle(
                "${context.getString(R.string.transfer_state_cancelled)} · ${info.fileName}",
            )
            .setContentText(info.message.ifBlank {
                context.getString(R.string.transfer_state_cancelled)
            })
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setGroup(groupFor(info.id))
        try {
            manager.notify(notificationId(info.id), builder.build())
        } catch (_: Exception) {
            // abaikan
        }
    }

    fun complete(info: TransferInfo, failed: Boolean, sound: Boolean) {
        if (!TransferService.hasNotificationPermission(context)) return
        val title = context.getString(
            if (failed) R.string.transfer_notification_failed else R.string.transfer_notification_done,
        )
        val body = info.message.ifBlank { title }
        val builder = NotificationCompat.Builder(
            context,
            NotificationHelper.channelFor(context, sound),
        )
            .setSmallIcon(R.drawable.ic_stat_transfer)
            .setContentTitle("$title · ${info.fileName}")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .setOngoing(false)
            .setOnlyAlertOnce(false)
            .setPriority(
                if (failed) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT,
            )
            .setCategory(
                if (failed) NotificationCompat.CATEGORY_ERROR else NotificationCompat.CATEGORY_STATUS,
            )
            .setGroup(groupFor(info.id))

        if (!failed && info.outputUri.isNotBlank()) {
            builder.addAction(
                0,
                context.getString(R.string.transfer_action_open),
                openOutput(info),
            )
            builder.addAction(
                0,
                context.getString(R.string.transfer_action_share),
                shareOutput(info),
            )
        }
        try {
            manager.notify(notificationId(info.id), builder.build())
        } catch (_: Exception) {
            // abaikan
        }
    }

    /* ------------------------------- teks ------------------------------- */

    private fun statusText(info: TransferInfo): String {
        return when (info.state) {
            TransferInfo.STATE_QUEUED -> context.getString(R.string.transfer_notification_queued)
            TransferInfo.STATE_PAUSED -> context.getString(R.string.transfer_notification_paused)
            TransferInfo.STATE_RETRYING -> context.getString(
                R.string.transfer_notification_retrying,
                info.attempt.coerceAtLeast(1),
                TransferQueue.MAX_ATTEMPTS,
            )
            TransferInfo.STATE_SUCCESS -> context.getString(R.string.transfer_notification_done)
            TransferInfo.STATE_ERROR -> info.message.ifBlank {
                context.getString(R.string.transfer_notification_failed)
            }
            TransferInfo.STATE_CANCELLED -> context.getString(R.string.transfer_state_cancelled)
            else -> runningText(info)
        }
    }

    private fun runningText(info: TransferInfo): String {
        if (info.progress < 0) {
            return context.getString(R.string.transfer_notification_preparing)
        }
        val parts = mutableListOf<String>()
        parts += context.getString(R.string.transfer_notification_percent, info.progress)
        if (info.bytesPerSecond > 0) {
            parts += context.getString(
                R.string.transfer_notification_speed,
                TransferFormat.bytes(info.bytesPerSecond),
            )
        }
        if (info.etaSeconds >= 0) {
            parts += TransferFormat.eta(context, info.etaSeconds)
        }
        return parts.joinToString(" · ")
    }

    private fun typeLabel(info: TransferInfo): String = context.getString(
        when {
            info.folder -> R.string.transfer_type_folder
            info.type.equals("download", ignoreCase = true) -> R.string.transfer_type_download
            else -> R.string.transfer_type_upload
        },
    )

    /* ------------------------------ intents ------------------------------ */

    private fun openApp(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun serviceAction(action: String, id: String, index: Int): PendingIntent {
        val intent = Intent(context, TransferService::class.java).apply {
            this.action = action
            putExtra(TransferService.EXTRA_TRANSFER_ID, id)
        }
        return PendingIntent.getForegroundService(
            context,
            notificationId(id) * 4 + index,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun openOutput(info: TransferInfo): PendingIntent {
        val uri = Uri.parse(info.outputUri)
        val mime = info.mimeType.ifBlank { "*/*" }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            clipData = ClipData.newRawUri(info.fileName, uri)
        }
        return PendingIntent.getActivity(
            context,
            notificationId(info.id) * 4 + 3,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun shareOutput(info: TransferInfo): PendingIntent {
        val uri = Uri.parse(info.outputUri)
        val mime = info.mimeType.ifBlank { "*/*" }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, info.fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(info.fileName, uri)
        }
        val chooser = Intent.createChooser(send, context.getString(R.string.transfer_action_share)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return PendingIntent.getActivity(
            context,
            notificationId(info.id) * 4 + 4,
            chooser,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun notificationId(id: String): Int =
        10_000 + (id.hashCode() and 0x3FFFFFF) % 900_000

    private fun groupFor(id: String): String = "$GROUP_PREFIX$id"

    private companion object {
        const val SUMMARY_ID = 1001
        const val GROUP_PREFIX = "gofile.transfers.item."
        const val GROUP_SUMMARY = "gofile.transfers.summary"
    }
}
