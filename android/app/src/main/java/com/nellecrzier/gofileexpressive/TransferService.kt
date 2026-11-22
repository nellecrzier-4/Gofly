package com.nellecrzier.gofileexpressive

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.nellecrzier.gofileexpressive.transfer.TransferHistory
import com.nellecrzier.gofileexpressive.transfer.TransferKind
import com.nellecrzier.gofileexpressive.transfer.TransferNotifier
import com.nellecrzier.gofileexpressive.transfer.TransferQueue
import com.nellecrzier.gofileexpressive.transfer.TransferTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Foreground service (dataSync) untuk unggah/unduh sungguhan.
 *
 * Service ini hanya bertugas menerjemahkan Intent menjadi [TransferTask],
 * menyerahkannya ke [TransferQueue], dan memastikan `startForeground()` dipanggil
 * dalam 5 detik pertama setiap kali dimulai. Semua pekerjaan berat ada di
 * `transfer/` dan berjalan sebagai coroutine per transfer.
 *
 * Nama aksi, nama extra, `hasNotificationPermission`, dan lima parameter
 * pertama [TransferInfo] dipertahankan supaya MainActivity tidak perlu diubah.
 */
class TransferService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_UPLOAD = "com.nellecrzier.gofileexpressive.UPLOAD"
        const val ACTION_DOWNLOAD = "com.nellecrzier.gofileexpressive.DOWNLOAD"
        const val ACTION_PAUSE = "com.nellecrzier.gofileexpressive.PAUSE"
        const val ACTION_RESUME = "com.nellecrzier.gofileexpressive.RESUME"
        const val ACTION_CANCEL = "com.nellecrzier.gofileexpressive.CANCEL"

        const val EXTRA_TRANSFER_ID = "transferId"
        const val EXTRA_URI = "uri"
        const val EXTRA_TOKEN = "token"
        const val EXTRA_FOLDER_ID = "folderId"
        const val EXTRA_FILE_NAME = "fileName"
        const val EXTRA_URL = "url"

        /** Daftar URI untuk unggahan banyak berkas dalam satu Intent. */
        const val EXTRA_URIS = "uris"

        /** URI pohon SAF (ACTION_OPEN_DOCUMENT_TREE) untuk unggah folder. */
        const val EXTRA_TREE_URI = "treeUri"

        /** MD5 dari GItem — diverifikasi setelah unduhan selesai. */
        const val EXTRA_MD5 = "md5"

        private const val TAG = "TransferService"

        fun hasNotificationPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED

        /**
         * Antrekan beberapa unggahan sekaligus lewat satu Intent. `name` hanya
         * dipakai kalau [uris] berisi satu berkas; selain itu nama diambil dari
         * ContentResolver.
         */
        fun enqueueUploads(
            context: Context,
            uris: List<Uri>,
            folderId: String,
            token: String,
            name: String,
        ) {
            if (uris.isEmpty()) return
            val intent = Intent(context, TransferService::class.java).apply {
                action = ACTION_UPLOAD
                putParcelableArrayListExtra(EXTRA_URIS, ArrayList(uris))
                putExtra(EXTRA_FOLDER_ID, folderId)
                putExtra(EXTRA_TOKEN, token)
                putExtra(EXTRA_FILE_NAME, name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /** Unggah satu pohon folder SAF; struktur folder dibuat di GoFile. */
        fun enqueueFolder(
            context: Context,
            treeUri: Uri,
            folderId: String,
            token: String,
            name: String,
        ) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
                // Sebagian penyedia tidak mendukung izin persistable.
            }
            val intent = Intent(context, TransferService::class.java).apply {
                action = ACTION_UPLOAD
                putExtra(EXTRA_TREE_URI, treeUri.toString())
                putExtra(EXTRA_FOLDER_ID, folderId)
                putExtra(EXTRA_TOKEN, token)
                putExtra(EXTRA_FILE_NAME, name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var notifier: TransferNotifier
    private lateinit var queue: TransferQueue

    private var prefsJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var wakeLock: PowerManager.WakeLock? = null

    @Volatile
    private var prefsState = PrefsState()

    @Volatile
    private var transfersRunning = false

    @Volatile
    private var lastStartAt = 0L

    /**
     * Teks notifikasi dan pesan status memakai bahasa pilihan pengguna, bukan
     * hanya locale sistem (penting di Android 12 ke bawah, tempat locale
     * per-aplikasi belum otomatis sampai ke service).
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleSupport.wrap(newBase))
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        TransferHistory.ensureLoaded(this)
        notifier = TransferNotifier(this)
        queue = TransferQueue(
            context = this,
            scope = scope,
            notifier = notifier,
            onIdle = { stopWhenIdle() },
            onWakeLockNeeded = { running ->
                transfersRunning = running
                refreshWakeLock()
            },
        )
        observePrefs()
        registerNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Wajib: foreground dalam 5 detik sejak startForegroundService().
        notifier.startForeground()
        lastStartAt = System.currentTimeMillis()

        val command = intent
        if (command != null) {
            when (command.action) {
                ACTION_UPLOAD -> enqueueUpload(command)
                ACTION_DOWNLOAD -> enqueueDownload(command)
                ACTION_PAUSE -> withTransferId(command) { queue.pause(it) }
                ACTION_RESUME -> withTransferId(command) { queue.resume(it) }
                ACTION_CANCEL -> withTransferId(command) { queue.cancel(it) }
                else -> Log.d(TAG, "Aksi tidak dikenal: ${command.action}")
            }
        }

        queue.wake()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        prefsJob?.cancel()
        networkCallback?.let { callback ->
            try {
                getSystemService(ConnectivityManager::class.java)
                    ?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
                // Callback sudah dilepas.
            }
        }
        networkCallback = null
        queue.stopAll()
        scope.cancel()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    /* ------------------------------- masuk ------------------------------- */

    private fun withTransferId(intent: Intent, block: (String) -> Unit) {
        val id = intent.getStringExtra(EXTRA_TRANSFER_ID).orEmpty()
        if (id.isNotBlank()) block(id)
    }

    private fun enqueueUpload(intent: Intent) {
        val token = intent.getStringExtra(EXTRA_TOKEN).orEmpty()
        val folderId = intent.getStringExtra(EXTRA_FOLDER_ID).orEmpty()
        val baseName = intent.getStringExtra(EXTRA_FILE_NAME).orEmpty()
        val baseId = intent.getStringExtra(EXTRA_TRANSFER_ID)

        val tree = intent.getStringExtra(EXTRA_TREE_URI).orEmpty()
        if (tree.isNotBlank()) {
            val uri = Uri.parse(tree)
            val name = baseName.ifBlank { folderName(uri) }
            queue.enqueue(
                TransferTask(
                    id = baseId ?: UUID.randomUUID().toString(),
                    kind = TransferKind.FOLDER_UPLOAD,
                    name = name,
                    treeUri = uri,
                    token = token,
                    folderId = folderId,
                ),
            )
            return
        }

        val uris = parcelableUris(intent)
        if (uris.isEmpty()) {
            Log.w(TAG, "Unggahan tanpa URI")
            return
        }
        uris.forEachIndexed { index, uri ->
            val id = if (index == 0 && !baseId.isNullOrBlank()) baseId else UUID.randomUUID().toString()
            val name = when {
                uris.size == 1 && baseName.isNotBlank() -> baseName
                else -> displayName(uri) ?: "berkas-${System.currentTimeMillis()}-$index"
            }.ifBlank { "berkas" }
            if (TransferStore.get(id) == null) {
                TransferStore.add(TransferInfo(id, "upload", name, 0, "queued"))
            }
            queue.enqueue(
                TransferTask(
                    id = id,
                    kind = TransferKind.UPLOAD,
                    name = name,
                    uri = uri,
                    token = token,
                    folderId = folderId,
                ),
            )
        }
    }

    private fun enqueueDownload(intent: Intent) {
        val id = intent.getStringExtra(EXTRA_TRANSFER_ID) ?: UUID.randomUUID().toString()
        val name = intent.getStringExtra(EXTRA_FILE_NAME).orEmpty().ifBlank { "gofile-download" }
        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        val token = intent.getStringExtra(EXTRA_TOKEN).orEmpty()
        val md5 = intent.getStringExtra(EXTRA_MD5).orEmpty()
        if (TransferStore.get(id) == null) {
            TransferStore.add(TransferInfo(id, "download", name, 0, "queued"))
        }
        queue.enqueue(
            TransferTask(
                id = id,
                kind = TransferKind.DOWNLOAD,
                name = name,
                url = url,
                token = token,
                md5 = md5,
            ),
        )
    }

    private fun parcelableUris(intent: Intent): List<Uri> {
        val result = mutableListOf<Uri>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(EXTRA_URIS, Uri::class.java)?.let { result += it }
            intent.getParcelableExtra(EXTRA_URI, Uri::class.java)?.let { result += it }
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Uri>(EXTRA_URIS)?.let { result += it }
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Uri>(EXTRA_URI)?.let { result += it }
        }
        return result.distinct()
    }

    private fun displayName(uri: Uri): String? = try {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else {
                    null
                }
            }
    } catch (_: Exception) {
        null
    }

    private fun folderName(uri: Uri): String = try {
        DocumentFile.fromTreeUri(this, uri)?.name ?: "Folder"
    } catch (_: Exception) {
        "Folder"
    }

    /* ---------------------------- preferensi ---------------------------- */

    private fun observePrefs() {
        prefsJob = scope.launch {
            AppPrefs(this@TransferService).state.collect { state ->
                prefsState = state
                queue.updatePrefs(state)
                refreshWakeLock()
            }
        }
    }

    /* ------------------------------ jaringan ----------------------------- */

    private fun registerNetworkCallback() {
        val manager = getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                queue.onNetworkChanged()
            }

            override fun onLost(network: Network) {
                queue.onNetworkChanged()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                queue.onNetworkChanged()
            }
        }
        try {
            manager.registerDefaultNetworkCallback(callback)
            networkCallback = callback
        } catch (error: Exception) {
            Log.w(TAG, "Tidak bisa memantau jaringan", error)
        }
    }

    /* ------------------------------ wake lock ---------------------------- */

    private fun refreshWakeLock() {
        val needed = prefsState.keepAwake || transfersRunning
        if (needed && wakeLock == null) {
            acquireWakeLock()
        } else if (!needed && wakeLock != null) {
            releaseWakeLock()
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "gofile:transfer")
            .apply {
                setReferenceCounted(false)
                acquire(2 * 60 * 60 * 1000L)
            }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    /* -------------------------------- akhir ------------------------------ */

    private fun stopWhenIdle() {
        // Beri jeda singkat supaya Intent yang datang bersamaan (mis. beberapa
        // startForegroundService berurutan) sempat masuk ke antrean.
        scope.launch {
            delay(250)
            if (queue.hasWork() || System.currentTimeMillis() - lastStartAt < 250) return@launch
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
}
