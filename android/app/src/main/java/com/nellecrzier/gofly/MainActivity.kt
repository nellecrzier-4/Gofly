package com.nellecrzier.gofly

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nellecrzier.gofly.ui.GofileApp
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale
import java.util.UUID

private data class PendingDownload(
    val url: String,
    val fileName: String,
    val token: String,
    val md5: String,
)

/**
 * Aktivitas native — TIDAK memakai WebView. Seluruh antarmuka dibangun dengan
 * Jetpack Compose dan seluruh komunikasi GoFile lewat GofileApi.
 */
class MainActivity : FragmentActivity() {

    val permissionRevision = MutableStateFlow(0)

    /** Token dari link login `https://gofile.io/login/...` yang dibuka di aplikasi. */
    val loginLinkFlow = MutableStateFlow<String?>(null)

    /** Folder dari tautan publik `https://gofile.io/d/<id>` yang perlu dibuka native. */
    val deepLinkFolder = MutableStateFlow<String?>(null)

    /** Berkas yang dibagikan aplikasi lain ke Gofly. */
    val incomingShare = MutableStateFlow<List<Uri>>(emptyList())

    /** Pintasan ("upload" / "transfers") yang harus dibuka saat app.start. */
    val shortcutRequest = MutableStateFlow<String?>(null)

    private var pendingFolderId = ""
    private var pendingToken = ""

    private lateinit var notificationLauncher: ActivityResultLauncher<String>
    private lateinit var batteryLauncher: ActivityResultLauncher<Intent>
    private lateinit var fileLauncher: ActivityResultLauncher<Intent>
    private lateinit var storageLauncher: ActivityResultLauncher<String>
    private lateinit var treeLauncher: ActivityResultLauncher<Uri?>
    private var pendingDownload: PendingDownload? = null

    override fun attachBaseContext(newBase: Context) {
        // Bahasa per-aplikasi untuk Android 12 ke bawah: bungkus konteks dengan
        // locale tersimpan sebelum aktivitas dibuat.
        super.attachBaseContext(LocaleSupport.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
        storageLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                val request = pendingDownload
                pendingDownload = null
                if (granted && request != null) {
                    launchDownload(request)
                } else if (!granted) {
                    toast("Izin penyimpanan ditolak. File tidak bisa disimpan ke Downloads.")
                }
            }

        treeLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                if (uri != null) {
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        )
                    } catch (_: SecurityException) {
                        // Sebagian penyedia tidak mendukung izin persistable.
                    }
                    downloadTreeResult.value = uri.toString()
                }
            }

        NotificationHelper.createChannel(this)
        captureIntent(intent)
        setContent { GofileApp(this) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        captureIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Kunci aplikasi diminta lagi setiap kali kembali dari latar belakang;
        // AppUi mengamati sinyal ini.
        lockRevision.value = lockRevision.value + 1
    }

    /** Sinyal untuk meminta kunci aplikasi (biometrik/PIN). */
    val lockRevision = MutableStateFlow(0)

    /** Folder unduhan SAF yang baru dipilih pengguna. */
    val downloadTreeResult = MutableStateFlow<String?>(null)

    /** Tangkap link login, tautan folder, pintasan, dan berkas yang dibagikan. */
    private fun captureIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                val uri = intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                if (uri != null) incomingShare.value = listOf(uri)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                @Suppress("DEPRECATION")
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                if (!uris.isNullOrEmpty()) incomingShare.value = uris
            }
            else -> {
                intent.getStringExtra(EXTRA_SHORTCUT)?.let { shortcutRequest.value = it }
                val data = intent.data ?: return
                if (data.host != "gofile.io") return
                val path = data.path.orEmpty()
                when {
                    path.startsWith("/login") -> {
                        GofileApi.extractToken(data.toString())?.let { loginLinkFlow.value = it }
                    }
                    path.startsWith("/d/") -> {
                        deepLinkFolder.value = path.removePrefix("/d/").trim('/').ifBlank { null }
                    }
                }
            }
        }
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
            toast("Buka Pengaturan → Baterai → Gofly → Izinkan latar belakang.")
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
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        try {
            fileLauncher.launch(Intent.createChooser(picker, "Pilih berkas untuk diunggah"))
        } catch (_: Exception) {
            toast("Tidak ada aplikasi pemilih berkas di perangkat ini.")
        }
    }

    /** Unggah beberapa URI (hasil ACTION_SEND/SEND_MULTIPLE atau pemilih ganda). */
    fun uploadUris(uris: List<Uri>, folderId: String, token: String) {
        if (uris.isEmpty()) return
        if (!hasNotificationPermission()) {
            toast("Aktifkan izin notifikasi dulu supaya progres transfer tetap terlihat.")
            return
        }
        uris.forEach { uri ->
            takePermission(uri)
            val name = queryDisplayName(uri) ?: "berkas-${System.currentTimeMillis()}"
            val id = UUID.randomUUID().toString()
            TransferStore.add(TransferInfo(id, "upload", name, 0, "queued"))
            val intent = Intent(this, TransferService::class.java).apply {
                action = TransferService.ACTION_UPLOAD
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(TransferService.EXTRA_TRANSFER_ID, id)
                putExtra(TransferService.EXTRA_URI, uri)
                putExtra(TransferService.EXTRA_TOKEN, token)
                putExtra(TransferService.EXTRA_FOLDER_ID, folderId)
                putExtra(TransferService.EXTRA_FILE_NAME, name)
            }
            ContextCompat.startForegroundService(this, intent)
        }
        incomingShare.value = emptyList()
    }

    private fun handlePickedFile(uri: Uri) {
        takePermission(uri)
        val name = queryDisplayName(uri) ?: "berkas-${System.currentTimeMillis()}"
        val id = UUID.randomUUID().toString()
        TransferStore.add(TransferInfo(id, "upload", name, 0, "queued"))
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

    private fun takePermission(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Sebagian provider tidak mendukung izin persistable.
        }
    }

    fun startDownload(url: String, fileName: String, token: String, md5: String = "") {
        if (!url.startsWith("http")) {
            toast("Tautan unduh tidak valid.")
            return
        }
        if (!hasNotificationPermission()) {
            toast("Aktifkan izin notifikasi dulu supaya progres transfer tetap terlihat.")
            return
        }
        val request = PendingDownload(url, fileName, token, md5)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingDownload = request
            storageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        launchDownload(request)
    }

    private fun launchDownload(request: PendingDownload) {
        val id = UUID.randomUUID().toString()
        TransferStore.add(TransferInfo(id, "download", request.fileName, 0, "queued"))
        val intent = Intent(this, TransferService::class.java).apply {
            action = TransferService.ACTION_DOWNLOAD
            putExtra(TransferService.EXTRA_TRANSFER_ID, id)
            putExtra(TransferService.EXTRA_URL, request.url)
            putExtra(TransferService.EXTRA_FILE_NAME, request.fileName)
            putExtra(TransferService.EXTRA_TOKEN, request.token)
            putExtra(TransferService.EXTRA_MD5, request.md5)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    /**
     * Buka berkas lokal (pratinjau yang sudah diunduh ke cache) dengan aplikasi
     * lain lewat chooser sistem. Aplikasi ini sendiri tidak pernah merender
     * berkas di dalam WebView.
     */
    fun openWith(file: java.io.File, mimeType: String) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "$packageName.files",
                file,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType.ifBlank { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Buka dengan"))
        } catch (error: Exception) {
            toast("Tidak ada aplikasi yang bisa membuka berkas ini.")
        }
    }

    /** Pilih folder unduhan lewat SAF. */
    fun pickDownloadFolder() {
        try {
            treeLauncher.launch(null)
        } catch (_: Exception) {
            toast("Tidak ada aplikasi pemilih folder di perangkat ini.")
        }
    }

    /** Ekspor log lokal ke aplikasi lain (berkas, Drive, email). */
    fun exportLog() {
        try {
            val file = AppLog.file(this)
            if (!file.exists()) AppLog.write(this, "app", "log diminta dari Setelan")
            val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.files", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Gofly — log")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Ekspor log"))
        } catch (error: Exception) {
            toast("Log belum bisa diekspor.")
        }
    }

    /** Kirim masukan: log terlampir, tanpa layanan pihak ketiga. */
    fun sendFeedback() {
        try {
            val file = AppLog.file(this)
            val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.files", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Masukan Gofly")
                putExtra(Intent.EXTRA_TEXT, "Tulis masalah atau saran di sini.\n\nLog terlampir.")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Kirim masukan"))
        } catch (error: Exception) {
            toast("Tidak ada aplikasi untuk mengirim masukan.")
        }
    }

    /** Bagikan tautan lewat sheet sistem. */
    fun shareLink(link: String, name: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, name)
            putExtra(Intent.EXTRA_TEXT, link)
        }
        startActivity(Intent.createChooser(intent, "Bagikan tautan"))
    }

    fun openUrlExternally(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            toast("Tidak ada aplikasi untuk membuka tautan ini.")
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

    fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_SHORTCUT = "com.nellecrzier.gofly.extra.SHORTCUT"
    }
}

/**
 * Bahasa per-aplikasi. Android 13 punya `LocaleManager`; versi lama memakai
 * konfigurasi locale yang disimpan di preferensi biasa (sinkron, bisa dibaca
 * sebelum aktivitas dibuat).
 */
object LocaleSupport {
    private const val PREFS = "gofile-locale"
    private const val KEY = "language"

    fun saved(base: Context): String =
        base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "system").orEmpty()

    fun store(base: Context, language: String) {
        base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, language).apply()
    }

    /** Konteks dengan locale pilihan pengguna (atau konteks aslinya). */
    fun wrap(base: Context): Context {
        val tag = saved(base)
        if (tag.isBlank() || tag == "system") return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    /**
     * Terapkan bahasa tersimpan ke seluruh aplikasi: `LocaleManager` di
     * Android 13+, `AppCompatDelegate` di versi lama.
     */
    fun applySystemLocales(context: Context) {
        val tag = saved(context)
        val system = tag.isBlank() || tag == "system"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = context.getSystemService(android.app.LocaleManager::class.java) ?: return
            manager.applicationLocales = if (system) {
                android.os.LocaleList.getDefault()
            } else {
                android.os.LocaleList.forLanguageTags(tag)
            }
        } else {
            val locales = if (system) {
                androidx.core.os.LocaleListCompat.getEmptyLocaleList()
            } else {
                androidx.core.os.LocaleListCompat.forLanguageTags(tag)
            }
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(locales)
        }
    }
}
