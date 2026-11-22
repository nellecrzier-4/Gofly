package com.nellecrzier.gofileexpressive

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Log lokal untuk laporan bug: crash terakhir dan catatan kejadian penting.
 *
 * Tidak ada data yang dikirim ke server mana pun — berkas ini hanya ditulis ke
 * penyimpanan aplikasi dan diekspor saat pengguna menekan "Kirim masukan".
 */
object AppLog {
    private const val MAX_BYTES = 256 * 1024
    private val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val lock = Any()

    fun file(context: Context): File {
        val dir = File(context.filesDir, "logs").apply { mkdirs() }
        return File(dir, "gofile-expressive.log")
    }

    /** Catat satu baris; aman dipanggil dari thread mana pun. */
    fun write(context: Context, tag: String, message: String) {
        synchronized(lock) {
            try {
                val target = file(context)
                if (target.length() > MAX_BYTES) target.delete()
                target.appendText("${stamp.format(Date())} [$tag] $message\n")
            } catch (_: Exception) {
                // Logging tidak boleh menjatuhkan aplikasi.
            }
        }
    }

    /** Simpan crash yang tidak tertangani, lalu teruskan ke handler sistem. */
    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            synchronized(lock) {
                try {
                    val header = buildString {
                        appendLine("=== crash ${stamp.format(Date())} ===")
                        appendLine("thread: ${thread.name}")
                        appendLine("app: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                        appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                        val buffer = StringWriter()
                        error.printStackTrace(PrintWriter(buffer))
                        appendLine(buffer.toString())
                    }
                    file(context).appendText(header)
                } catch (_: Exception) {
                    // abaikan
                }
            }
            previous?.uncaughtException(thread, error)
        }
        write(context, "app", "started ${BuildConfig.VERSION_NAME}")
    }

    fun read(context: Context): String = runCatching { file(context).readText() }.getOrDefault("")
}
