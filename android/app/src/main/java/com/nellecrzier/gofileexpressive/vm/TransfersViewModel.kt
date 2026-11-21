package com.nellecrzier.gofileexpressive.vm

import androidx.lifecycle.ViewModel
import com.nellecrzier.gofileexpressive.TransferInfo
import com.nellecrzier.gofileexpressive.TransferStore
import kotlinx.coroutines.flow.StateFlow

/** Jembatan tipis antara TransferStore (service) dan Compose. */
class TransfersViewModel : ViewModel() {
    val transfers: StateFlow<List<TransferInfo>> = TransferStore.transfers

    fun dismiss(id: String) = TransferStore.dismiss(id)

    fun dismissAll() = TransferStore.dismissAll()
}
