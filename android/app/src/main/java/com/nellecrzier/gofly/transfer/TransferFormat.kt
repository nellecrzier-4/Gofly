package com.nellecrzier.gofly.transfer

import android.content.Context
import com.nellecrzier.gofly.R
import java.util.Locale

/** Format angka yang dipakai notifikasi, panel, dan pesan status. */
internal object TransferFormat {

    fun bytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unit = 0
        while (value >= 1024.0 && unit < units.lastIndex) {
            value /= 1024.0
            unit++
        }
        return if (unit == 0) {
            "$bytes B"
        } else {
            String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
        }
    }

    fun speed(context: Context, bytesPerSecond: Long): String =
        context.getString(R.string.transfer_notification_speed, bytes(bytesPerSecond))

    fun speedShort(context: Context, bytesPerSecond: Long): String =
        context.getString(R.string.transfer_panel_speed_value, bytes(bytesPerSecond))

    /** `45s`, `2m 10s`, `1h 05m`; `—` saat belum bisa dihitung. */
    fun eta(context: Context, seconds: Int): String {
        if (seconds < 0) return context.getString(R.string.transfer_panel_unknown)
        if (seconds < 60) return context.getString(R.string.transfer_eta_seconds, seconds)
        if (seconds < 3600) {
            return context.getString(R.string.transfer_eta_minutes, seconds / 60, seconds % 60)
        }
        return context.getString(R.string.transfer_eta_hours, seconds / 3600, (seconds % 3600) / 60)
    }
}
