package com.nellecrzier.gofly.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nellecrzier.gofly.AccentChoice
import com.nellecrzier.gofly.AnimPref
import com.nellecrzier.gofly.AppLog
import com.nellecrzier.gofly.AppPrefs
import com.nellecrzier.gofly.DeleteConfirm
import com.nellecrzier.gofly.Density
import com.nellecrzier.gofly.GItem
import com.nellecrzier.gofly.LanguageChoice
import com.nellecrzier.gofly.LocaleSupport
import com.nellecrzier.gofly.PrefsState
import com.nellecrzier.gofly.ThemeMode
import com.nellecrzier.gofly.ViewMode
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Preferensi tampilan, tema, transfer, dan keamanan yang bertahan antar sesi
 * (DataStore) plus favorit dan daftar terbaru yang seluruhnya lokal.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = AppPrefs(application)

    /** Ukuran cache dalam byte; dihitung ulang lewat [refreshCacheSize]. */
    private val _cacheBytes = MutableStateFlow(0L)
    val cacheBytes: StateFlow<Long> = _cacheBytes

    val state: StateFlow<PrefsState> =
        prefs.state.stateIn(viewModelScope, SharingStarted.Eagerly, PrefsState())

    init {
        refreshCacheSize()
    }

    /* ------------------------------ tampilan ----------------------------- */

    fun setViewMode(mode: ViewMode) = launch { prefs.setViewMode(mode) }
    fun setSort(field: String, ascending: Boolean) = launch { prefs.setSort(field, ascending) }
    fun setThemeMode(mode: ThemeMode) = launch { prefs.setThemeMode(mode) }
    fun setAccent(accent: AccentChoice) = launch { prefs.setAccent(accent) }
    fun setDynamicColor(enabled: Boolean) = launch { prefs.setDynamicColor(enabled) }
    fun setAmoled(enabled: Boolean) = launch { prefs.setAmoled(enabled) }
    fun setFontScale(scale: Float) = launch { prefs.setFontScale(scale) }
    fun setDensity(density: Density) = launch { prefs.setDensity(density) }
    fun setAnimations(pref: AnimPref) = launch { prefs.setAnimations(pref) }
    fun setShowHidden(show: Boolean) = launch { prefs.setShowHidden(show) }

    fun setLanguage(language: LanguageChoice) {
        launch {
            prefs.setLanguage(language)
            // Disimpan juga di preferensi biasa supaya bisa dibaca sebelum
            // aktivitas dibuat (attachBaseContext).
            val tag = when (language) {
                LanguageChoice.SYSTEM -> "system"
                LanguageChoice.EN -> "en"
                LanguageChoice.ID -> "id"
            }
            LocaleSupport.store(getApplication(), tag)
        }
    }

    /* ------------------------------- daftar ------------------------------ */

    fun setDeleteConfirm(mode: DeleteConfirm) = launch { prefs.setDeleteConfirm(mode) }
    fun setUndoSeconds(seconds: Int) = launch { prefs.setUndoSeconds(seconds) }

    /* ------------------------------ transfer ----------------------------- */

    fun setWifiOnly(enabled: Boolean) = launch { prefs.setWifiOnly(enabled) }
    fun setParallelTransfers(count: Int) = launch { prefs.setParallelTransfers(count) }
    fun setDownloadTree(uri: String) = launch { prefs.setDownloadTree(uri) }
    fun setKeepAwake(enabled: Boolean) = launch { prefs.setKeepAwake(enabled) }
    fun setNotifySound(enabled: Boolean) = launch { prefs.setNotifySound(enabled) }

    /* ------------------------------ keamanan ----------------------------- */

    fun setAppLock(enabled: Boolean) = launch { prefs.setAppLock(enabled) }

    /* ------------------------------- pustaka ----------------------------- */

    fun toggleFavorite(item: GItem) = launch { prefs.toggleFavorite(item) }

    fun pushRecent(item: GItem) = launch { prefs.pushRecent(item) }

    fun clearLibrary() = launch { prefs.clearLibrary() }

    /* -------------------------------- cache ------------------------------ */

    fun refreshCacheSize() {
        viewModelScope.launch {
            _cacheBytes.value = withContext(Dispatchers.IO) {
                val root = getApplication<Application>().cacheDir
                root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                getApplication<Application>().cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            }
            refreshCacheSize()
        }
    }

    /* --------------------------- log / laporan --------------------------- */

    /** Berkas log untuk dilampirkan saat melaporkan masalah. */
    fun logFile(): File = AppLog.file(getApplication())

    /** Isi changelog bawaan aplikasi (aset CHANGELOG.md dibundel). */
    fun changelog(): String = runCatching {
        getApplication<Application>().assets.open("CHANGELOG.md")
            .bufferedReader().use { it.readText() }
    }.getOrDefault("")

    fun markWhatsNewSeen(version: Int) = launch { prefs.setWhatsNewSeen(version) }
    fun setGesturesSeen(seen: Boolean) = launch { prefs.setGesturesSeen(seen) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
