package com.nellecrzier.gofileexpressive

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.OpenableColumns
import android.provider.Settings
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.util.UUID

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var pendingFolderId = ""
    private var pendingToken = ""

    companion object {
        private const val REQ_PICK_FILE = 1001
        private const val REQ_NOTIFICATIONS = 1002
        private const val REQ_BATTERY = 1003
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.mediaPlaybackRequiresUserGesture = false
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            setBackgroundColor(Color.parseColor("#0E0B17"))
            addJavascriptInterface(GofileBridge(this@MainActivity), "AndroidBridge")
        }
        setContentView(webView)
        webView.loadUrl(BuildConfig.APP_URL)

        TransferStore.listener = { info -> postTransferToWeb(info) }
    }

    override fun onDestroy() {
        TransferStore.listener = null
        super.onDestroy()
    }

    /* ---------------------------- izin ---------------------------- */

    fun hasNotificationPermission(): Boolean = TransferService.hasNotificationPermission(this)

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATIONS)
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
            startActivityForResult(intent, REQ_BATTERY)
        } catch (_: Exception) {
            toast("Buka Pengaturan → Baterai → Gofile Expressive → Izinkan latar belakang.")
        }
    }

    /* --------------------------- transfer --------------------------- */

    fun pickFileForUpload(folderId: String, token: String) {
        if (!hasNotificationPermission()) {
            guardPermission("notifications")
            return
        }
        pendingFolderId = folderId
        pendingToken = token
        val picker = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        }
        try {
            startActivityForResult(Intent.createChooser(picker, "Pilih berkas untuk diunggah"), REQ_PICK_FILE)
        } catch (_: Exception) {
            toast("Tidak ada aplikasi pemilih berkas di perangkat ini.")
        }
    }

    fun startDownload(url: String, fileName: String) {
        if (!hasNotificationPermission()) {
            guardPermission("notifications")
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

    private fun guardPermission(kind: String) {
        toast("Aktifkan izin notifikasi dulu supaya progres transfer tetap terlihat.")
        callJs("window.onPermissionRequired && window.onPermissionRequired('$kind');")
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_PICK_FILE || resultCode != RESULT_OK) return
        val uri = data?.data ?: return

        val name = queryDisplayName(uri) ?: "berkas-${System.currentTimeMillis()}"
        val id = UUID.randomUUID().toString()
        TransferStore.add(TransferInfo(id, "upload", name, 0, "running"))

        val intent = Intent(this, TransferService::class.java).apply {
            action = TransferService.ACTION_UPLOAD
            putExtra(TransferService.EXTRA_TRANSFER_ID, id)
            putExtra(TransferService.EXTRA_URI, uri)
            putExtra(TransferService.EXTRA_TOKEN, pendingToken)
            putExtra(TransferService.EXTRA_FOLDER_ID, pendingFolderId)
            putExtra(TransferService.EXTRA_FILE_NAME, name)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_NOTIFICATIONS) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            callJs("window.onPermissionResult && window.onPermissionResult('notifications', $granted);")
            if (granted) toast("Izin notifikasi aktif. Progres transfer akan tampil di notifikasi.")
        }
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

    /* --------------------------- WebView --------------------------- */

    fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun callJs(script: String) {
        webView.post { webView.evaluateJavascript(script, null) }
    }

    private fun postTransferToWeb(info: TransferInfo) {
        val json = JSONObject().apply {
            put("id", info.id)
            put("type", info.type)
            put("fileName", info.fileName)
            put("progress", info.progress)
            put("state", info.state)
            put("message", info.message)
        }.toString()
        callJs("window.onTransferUpdate && window.onTransferUpdate($json);")
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
