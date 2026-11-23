package com.nellecrzier.gofly

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

/**
 * Dua kanal:
 * - [CHANNEL_ID] senyap dan berprioritas rendah untuk progres yang terus berubah,
 * - [CHANNEL_ALERTS_ID] dengan bunyi/getar untuk notifikasi hasil saat
 *   preferensi `notifySound` aktif.
 */
object NotificationHelper {
    const val CHANNEL_ID = "gofile_transfers"
    const val CHANNEL_ALERTS_ID = "gofile_transfer_done"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val progress = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.channel_description)
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }
            manager.createNotificationChannel(progress)

            val alerts = NotificationChannel(
                CHANNEL_ALERTS_ID,
                context.getString(R.string.transfer_channel_alerts_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.transfer_channel_alerts_description)
                enableVibration(true)
                enableLights(false)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            }
            manager.createNotificationChannel(alerts)
        }
    }

    /** Kanal untuk notifikasi hasil; jatuh ke kanal senyap saat bunyi dimatikan. */
    fun channelFor(context: Context, sound: Boolean): String {
        if (!sound) return CHANNEL_ID
        createChannel(context)
        return CHANNEL_ALERTS_ID
    }
}
