package com.nellecrzier.gofileexpressive

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.nellecrzier.gofileexpressive.ui.GofileApp
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

/**
 * Aktivitas native — TIDAK memakai WebView. Seluruh antarmuka dibangun dengan
 * Jetpack Compose dan seluruh komunikasi GoFile lewat GofileApi.
 */
class MainActivity : ComponentActivity() {

    val permissionRevision = MutableStateFlow(0)

    private var pendingFolderId = ""
    private var pendingToken = ""

    private lateinit var notificationLauncher: ActivityResultLauncher<String>
    private lateinit var batteryLauncher: ActivityResultLauncher<Intent>
    private lateinit var fileLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notificationLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                permissionRevision.value++
                toast(if (granted) "Izin notifikasi aktif." else "Izin notifikasi ditolak.")
            }
        batteryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                permissionRevision.value++
            }
        fileLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode != RESULT_OK) return@registerForActivityResult
                val uri = result.data?.data ?: return@registerForActivityResult
                handlePickedFile(uri)
            }

        NotificationHelper.createChannel(this)
        setContent { GofileApp(this) }
    }

    /* ------------------------------- izin ------------------------------- */

    fun hasNotificationPermission(): Boolean = TransferService.hasNotificationPermission(this)

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (isIgnoringBatteryOptimizations()) {
            toast("Aplikasi sudah diizinkan berjalan di latar belakang.")
            return
        }
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            batteryLauncher.launch(intent)
        } catch (_: Exception) {
            toast("Buka Pengaturan → Baterai → Gofile Expressive → Izinkan latar belakang.")
        }
    }

    /* ------------------------------ transfer ---------------------------- */

    fun pickFileForUpload(folderId: String, token: String) {
        if (!hasNotificationPermission()) {
            toast("Aktifkan izin notifikasi dulu supaya progres transfer tetap terlihat.")
            return
        }
        pendingFolderId = folderId
        pendingToken = token
        val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        }
        try {
            fileLauncher.launch(Intent.createChooser(picker, "Pilih berkas untuk diunggah"))
        } catch (_: Exception) {
            toast("Tidak ada aplikasi pemilih berkas di perangkat ini.")
        }
    }

    private fun handlePickedFile(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Sebagian provider tidak mendukung izin persistable.
        }
        val name = queryDisplayName(uri) ?: "berkas-${System.currentTimeMillis()}"
        val id = UUID.randomUUID().toString()
        TransferStore.add(TransferInfo(id, "upload", name, 0, "running"))
        val intent = Intent(this, TransferService::class.java).apply {
            action = TransferService.ACTION_UPLOAD
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(TransferService.EXTRA_TRANSFER_ID, id)
            putExtra(TransferService.EXTRA_URI, uri)
            putExtra(TransferService.EXTRA_TOKEN, pendingToken)
            putExtra(TransferService.EXTRA_FOLDER_ID, pendingFolderId)
            putExtra(TransferService.EXTRA_FILE_NAME, name)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    fun startDownload(url: String, fileName: String) {
        if (!url.startsWith("http")) {
            toast("Tautan unduh tidak valid.")
            return
        }
        if (!hasNotificationPermission()) {
            toast("Aktifkan izin notifikasi dulu supaya progres transfer tetap terlihat.")
            return
        }
        val id = UUID.randomUUID().toString()
        TransferStore.add(TransferInfo(id, "download", fileName, 0, "running"))
        val intent = Intent(this, TransferService::class.java).apply {
            action = TransferService.ACTION_DOWNLOAD
            putExtra(TransferService.EXTRA_TRANSFER_ID, id)
            putExtra(TransferService.EXTRA_URL, url)
            putExtra(TransferService.EXTRA_FILE_NAME, fileName)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun queryDisplayName(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return cursor.getString(index)
            }
        }
        return null
    }

    fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
