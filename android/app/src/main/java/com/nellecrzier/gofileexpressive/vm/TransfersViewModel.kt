package com.nellecrzier.gofileexpressive.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import com.nellecrzier.gofileexpressive.TransferInfo
import com.nellecrzier.gofileexpressive.TransferStore
import com.nellecrzier.gofileexpressive.transfer.TransferControls
import com.nellecrzier.gofileexpressive.transfer.TransferHistory
import kotlinx.coroutines.flow.StateFlow

/**
 * Jembatan tipis antara mesin transfer (service) dan Compose.
 *
 * [transfers] berisi transfer aktif + yang baru selesai di proses ini;
 * [history] adalah riwayat JSON yang tahan restart. Aksi jeda/lanjut/batal
 * diteruskan ke service lewat [TransferControls].
 */
class TransfersViewModel : ViewModel() {
    val transfers: StateFlow<List<TransferInfo>> = TransferStore.transfers
    val history: StateFlow<List<TransferInfo>> = TransferHistory.entries

    fun dismiss(id: String) = TransferStore.dismiss(id)

    fun dismissAll() = TransferStore.dismissAll()

    fun pause(context: Context, id: String) = TransferControls.pause(context, id)

    fun resume(context: Context, id: String) = TransferControls.resume(context, id)

    fun cancel(context: Context, id: String) = TransferControls.cancel(context, id)

    fun clearHistory() = TransferHistory.clear()

    fun lastHistory(count: Int): List<TransferInfo> = TransferHistory.last(count)
}
