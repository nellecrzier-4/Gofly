package com.nellecrzier.gofileexpressive.transfer

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.nellecrzier.gofileexpressive.PrefsState
import com.nellecrzier.gofileexpressive.R
import com.nellecrzier.gofileexpressive.TransferInfo
import com.nellecrzier.gofileexpressive.TransferStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Antrean transfer dengan maksimal `prefs.parallelTransfers` pekerjaan aktif
 * (1–4). Setiap pekerjaan punya coroutine [Job] sendiri, sehingga bisa dijeda,
 * dilanjutkan, atau dibatalkan tanpa mengganggu yang lain.
 *
 * Aturan penting:
 * - `pause` membatalkan job tetapi membiarkan berkas `.part` (unduhan bisa
 *   dilanjutkan dengan Range; unggahan mulai dari awal),
 * - `cancel` membatalkan job dan menghapus berkas sementara,
 * - kegagalan yang bisa dipulihkan dicoba ulang 3 kali (2 s / 4 s / 8 s),
 * - layanan berhenti sendiri saat antrean benar-benar kosong (paused tetap
 *   dihitung sebagai pekerjaan supaya bisa dilanjutkan dari notifikasi).
 */
internal class TransferQueue(
    private val context: Context,
    private val scope: CoroutineScope,
    private val notifier: TransferNotifier,
    private val onIdle: () -> Unit,
    private val onWakeLockNeeded: (Boolean) -> Unit,
) {
    private val lock = Any()
    private val tasks = LinkedHashMap<String, TransferTask>()
    private val jobs = HashMap<String, Job>()
    private val paused = HashSet<String>()
    private val cancelled = HashSet<String>()
    private val finished = HashSet<String>()
    private val pumpMutex = Mutex()
    private val engine = TransferEngine(context)

    @Volatile
    private var prefs = PrefsState()

    /* ------------------------------ publik ------------------------------ */

    fun enqueue(task: TransferTask) {
        TransferHistory.ensureLoaded(context)
        val existing = TransferStore.get(task.id)
        if (existing == null) {
            TransferStore.add(
                TransferInfo(
                    id = task.id,
                    type = task.type,
                    fileName = task.name,
                    progress = 0,
                    state = TransferInfo.STATE_QUEUED,
                    folder = task.isFolder,
                    md5 = task.md5,
                ),
            )
        } else {
            TransferStore.update(
                task.id,
                state = TransferInfo.STATE_QUEUED,
                message = "",
                folder = task.isFolder,
            )
        }
        synchronized(lock) {
            tasks[task.id] = task
            finished.remove(task.id)
            cancelled.remove(task.id)
            paused.remove(task.id)
        }
        notifyInfo(task.id)
        requestPump()
    }

    fun updatePrefs(next: PrefsState) {
        val previous = prefs
        prefs = next
        if (previous.parallelTransfers != next.parallelTransfers || previous.wifiOnly != next.wifiOnly) {
            requestPump()
        }
    }

    fun pause(id: String) {
        if (TransferStore.get(id)?.isFinished == true) return
        val job = synchronized(lock) {
            if (id in finished) return
            paused.add(id)
            jobs[id]
        }
        job?.cancel()
        if (TransferStore.get(id)?.isFinished == true) return
        TransferStore.update(
            id,
            state = TransferInfo.STATE_PAUSED,
            message = context.getString(R.string.transfer_state_paused_message),
            bytesPerSecond = 0,
            etaSeconds = -1,
        )
        notifyInfo(id)
        requestPump()
    }

    fun resume(id: String) {
        if (TransferStore.get(id)?.isFinished == true) return
        val task = synchronized(lock) {
            paused.remove(id)
            tasks[id]
        } ?: return
        val info = TransferStore.get(id)
        val restartUpload = task.kind != TransferKind.DOWNLOAD && (info?.progress ?: 0) > 0
        TransferStore.update(
            id,
            state = TransferInfo.STATE_QUEUED,
            message = if (restartUpload) {
                context.getString(R.string.transfer_message_upload_restart)
            } else {
                ""
            },
            bytesPerSecond = 0,
            etaSeconds = -1,
        )
        notifyInfo(id)
        requestPump()
    }

    fun cancel(id: String) {
        val task = synchronized(lock) {
            if (id in finished) return
            cancelled.add(id)
            paused.remove(id)
            tasks[id]?.also { jobs[id] }
        } ?: run {
            notifier.cancel(id)
            return
        }
        val job = synchronized(lock) { jobs[id] }
        job?.cancel()
        scope.launch {
            try {
                job?.join()
            } catch (_: Exception) {
                // Job sudah selesai; tidak ada yang perlu ditunggu.
            }
            PartialStore.clear(context, id)
            finishCancelled(task)
        }
    }

    fun onNetworkChanged() {
        requestPump()
        notifier.refreshSummary()
    }

    /** Bangunkan antrean (mis. setelah Intent kontrol) tanpa pekerjaan baru. */
    fun wake() {
        requestPump()
    }

    fun activeCount(): Int = synchronized(lock) { jobs.size }

    fun hasWork(): Boolean = synchronized(lock) { tasks.isNotEmpty() }

    fun stopAll() {
        synchronized(lock) {
            jobs.values.forEach { it.cancel() }
            tasks.clear()
            jobs.clear()
            paused.clear()
        }
    }

    /* ------------------------------ antrean ----------------------------- */

    private fun requestPump() {
        scope.launch { pump() }
    }

    private suspend fun pump() {
        pumpMutex.withLock {
            val limit = prefs.parallelTransfers.coerceIn(1, 4)
            while (true) {
                val next = synchronized(lock) {
                    if (jobs.size >= limit) {
                        null
                    } else {
                        tasks.values.firstOrNull { task ->
                            task.id !in jobs &&
                                task.id !in paused &&
                                task.id !in cancelled &&
                                task.id !in finished
                        }
                    }
                } ?: break
                start(next)
            }
            notifier.refreshSummary()
            val (idle, running) = synchronized(lock) {
                (tasks.isEmpty() && jobs.isEmpty()) to jobs.isNotEmpty()
            }
            onWakeLockNeeded(running)
            if (idle) onIdle()
        }
    }

    private fun start(task: TransferTask) {
        val job = scope.launch(start = CoroutineStart.LAZY) { runTask(task) }
        synchronized(lock) { jobs[task.id] = job }
        job.invokeOnCompletion {
            val remaining = synchronized(lock) {
                jobs.remove(task.id)
                jobs.size
            }
            if (remaining == 0) notifier.refreshSummary()
            requestPump()
        }
        notifyInfo(task.id)
        job.start()
    }

    private suspend fun runTask(task: TransferTask) {
        TransferStore.update(
            task.id,
            state = TransferInfo.STATE_RUNNING,
            attempt = 0,
            message = context.getString(R.string.transfer_message_preparing, task.name),
            bytesPerSecond = 0,
            etaSeconds = -1,
        )
        notifyInfo(task.id)

        var attempt = 0
        while (true) {
            attempt++
            if (!networkAllowed()) {
                TransferStore.update(
                    task.id,
                    state = TransferInfo.STATE_PAUSED,
                    message = context.getString(R.string.transfer_state_waiting_wifi),
                    bytesPerSecond = 0,
                    etaSeconds = -1,
                )
                notifyInfo(task.id)
                waitUntilNetworkAllowed()
                TransferStore.update(
                    task.id,
                    state = TransferInfo.STATE_QUEUED,
                    message = "",
                )
            }
            try {
                val result = engine.execute(
                    task = task,
                    treeUri = prefs.downloadTree,
                    onProgress = { done, total, speed, eta ->
                        reportProgress(task, done, total, speed, eta)
                    },
                    onMessage = { message ->
                        TransferStore.update(task.id, message = message)
                        notifyInfo(task.id)
                    },
                )
                finishSuccess(task, result)
                return
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                // Pause/cancel bisa muncul sebagai IOException dari socket yang
                // ditutup; jangan pernah menganggapnya kegagalan yang di-retry.
                currentCoroutineContext().ensureActive()
                val failure = TransferErrors.classify(context, error)
                if (!failure.retryable || attempt >= MAX_ATTEMPTS) {
                    finishFailure(task, failure)
                    return
                }
                val seconds = backoffSeconds(attempt)
                TransferStore.update(
                    task.id,
                    state = TransferInfo.STATE_RETRYING,
                    attempt = attempt,
                    message = context.getString(
                        R.string.transfer_message_retrying,
                        attempt,
                        MAX_ATTEMPTS,
                        seconds,
                    ),
                    bytesPerSecond = 0,
                    etaSeconds = -1,
                )
                notifyInfo(task.id)
                delay(seconds * 1000L)
            }
        }
    }

    private fun reportProgress(
        task: TransferTask,
        done: Long,
        total: Long,
        speed: Long,
        eta: Int,
    ) {
        val current = TransferStore.get(task.id) ?: return
        if (current.state == TransferInfo.STATE_PAUSED ||
            current.state == TransferInfo.STATE_CANCELLED
        ) {
            return
        }
        val progress = if (total > 0) {
            ((done * 100) / total).toInt().coerceIn(0, 99)
        } else {
            -1
        }
        TransferStore.update(
            task.id,
            state = TransferInfo.STATE_RUNNING,
            progress = progress,
            bytesDone = done,
            bytesTotal = total,
            bytesPerSecond = speed,
            etaSeconds = eta,
        )
        notifyInfo(task.id)
    }

    /* ------------------------------- akhir ------------------------------ */

    private fun finishSuccess(task: TransferTask, result: TransferResult) {
        synchronized(lock) {
            if (!finished.add(task.id)) return
            tasks.remove(task.id)
            paused.remove(task.id)
        }
        TransferStore.update(
            task.id,
            state = TransferInfo.STATE_SUCCESS,
            progress = 100,
            message = result.message,
            bytesTotal = if (result.bytesTotal > 0) result.bytesTotal else null,
            bytesDone = if (result.bytesDone > 0) result.bytesDone else null,
            bytesPerSecond = 0,
            etaSeconds = 0,
            outputUri = result.outputUri,
            outputPath = result.outputPath,
            mimeType = result.mimeType,
            errorKind = "",
        )
        val info = TransferStore.get(task.id) ?: return
        TransferHistory.add(info)
        notifier.complete(info, failed = false, sound = prefs.notifySound)
        notifier.refreshSummary()
        requestPump()
    }

    private fun finishFailure(task: TransferTask, failure: TransferFailure) {
        synchronized(lock) {
            if (!finished.add(task.id)) return
            tasks.remove(task.id)
            paused.remove(task.id)
        }
        PartialStore.clear(context, task.id)
        TransferStore.update(
            task.id,
            state = TransferInfo.STATE_ERROR,
            message = failure.message,
            bytesPerSecond = 0,
            etaSeconds = -1,
            errorKind = failure.kind,
        )
        val info = TransferStore.get(task.id) ?: return
        TransferHistory.add(info)
        notifier.complete(info, failed = true, sound = prefs.notifySound)
        notifier.refreshSummary()
        requestPump()
    }

    private fun finishCancelled(task: TransferTask) {
        synchronized(lock) {
            if (!finished.add(task.id)) return
            tasks.remove(task.id)
            paused.remove(task.id)
            cancelled.remove(task.id)
        }
        TransferStore.update(
            task.id,
            state = TransferInfo.STATE_CANCELLED,
            message = context.getString(R.string.transfer_message_cancelled),
            bytesPerSecond = 0,
            etaSeconds = -1,
        )
        val info = TransferStore.get(task.id) ?: return
        TransferHistory.add(info)
        notifier.cancelled(info)
        notifier.refreshSummary()
        requestPump()
    }

    private fun notifyInfo(id: String) {
        TransferStore.get(id)?.let { notifier.progress(it) }
    }

    /* ------------------------------ jaringan ---------------------------- */

    private fun networkAllowed(): Boolean {
        if (!prefs.wifiOnly) return true
        return isOnWifi()
    }

    private fun isOnWifi(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return true
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private suspend fun waitUntilNetworkAllowed() {
        var waitedMs = 0L
        while (!networkAllowed()) {
            currentCoroutineContext().ensureActive()
            delay(1500L)
            waitedMs += 1500L
            if (waitedMs >= WIFI_WAIT_LIMIT_MS) {
                throw TransferException(TransferErrorKind.NETWORK)
            }
        }
    }

    private fun backoffSeconds(attempt: Int): Int = when (attempt) {
        1 -> 2
        2 -> 4
        else -> 8
    }

    internal companion object {
        const val MAX_ATTEMPTS = 3
        private const val WIFI_WAIT_LIMIT_MS = 30L * 60L * 1000L
    }
}
