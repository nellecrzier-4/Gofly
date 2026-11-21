package com.nellecrzier.gofileexpressive.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nellecrzier.gofileexpressive.AppPrefs
import com.nellecrzier.gofileexpressive.GItem
import com.nellecrzier.gofileexpressive.PrefsState
import com.nellecrzier.gofileexpressive.ViewMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Preferensi tampilan yang bertahan antar sesi (DataStore) plus favorit dan
 * daftar terbaru yang seluruhnya lokal.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPrefs(application)

    val state: StateFlow<PrefsState> =
        prefs.state.stateIn(viewModelScope, SharingStarted.Eagerly, PrefsState())

    fun setViewMode(mode: ViewMode) {
        viewModelScope.launch { prefs.setViewMode(mode) }
    }

    fun setSort(field: String, ascending: Boolean) {
        viewModelScope.launch { prefs.setSort(field, ascending) }
    }

    fun toggleFavorite(item: GItem) {
        viewModelScope.launch { prefs.toggleFavorite(item) }
    }

    fun pushRecent(item: GItem) {
        viewModelScope.launch { prefs.pushRecent(item) }
    }

    fun clearLibrary() {
        viewModelScope.launch { prefs.clearLibrary() }
    }
}
