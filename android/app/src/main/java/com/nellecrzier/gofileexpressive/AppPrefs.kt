package com.nellecrzier.gofileexpressive

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.appDataStore by preferencesDataStore(name = "gofile-settings")

enum class ViewMode { LIST, GRID }

enum class ThemeMode { SYSTEM, DARK, LIGHT }

enum class AccentChoice { CORAL, AMBER, MINT, VIOLET, BLUE }

enum class Density { COMPACT, COMFORT, RELAXED }

enum class AnimPref { SYSTEM, REDUCED, OFF }

enum class DeleteConfirm { HOLD, DIALOG }

enum class LanguageChoice { SYSTEM, EN, ID }

data class PrefsState(
    val viewMode: ViewMode = ViewMode.LIST,
    val sortField: String = "name",
    val sortAscending: Boolean = true,
    val favorites: List<GItem> = emptyList(),
    val recent: List<GItem> = emptyList(),
    // Tampilan
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accent: AccentChoice = AccentChoice.CORAL,
    val dynamicColor: Boolean = false,
    val fontScale: Float = 1f,
    val density: Density = Density.COMFORT,
    val language: LanguageChoice = LanguageChoice.SYSTEM,
    val animations: AnimPref = AnimPref.SYSTEM,
    // Perilaku daftar
    val showHidden: Boolean = false,
    val deleteConfirm: DeleteConfirm = DeleteConfirm.HOLD,
    val undoSeconds: Int = 6,
    // Jaringan dan transfer
    val wifiOnly: Boolean = false,
    val parallelTransfers: Int = 2,
    val downloadTree: String = "",
    val keepAwake: Boolean = false,
    val notifySound: Boolean = false,
    // Keamanan
    val appLock: Boolean = false,
    // Onboarding
    val gesturesSeen: Boolean = false,
    val whatsNewSeen: Int = 0,
) {
    /** Animasi diredupkan: setelan aplikasi menang atas skala animator sistem. */
    val motionReduced: Boolean get() = animations != AnimPref.SYSTEM
}

/**
 * Preferensi tampilan, tema, transfer, dan pustaka lokal. Semua disimpan di
 * DataStore supaya aman ditulis dari coroutine dan tidak hilang saat app
 * ditutup. Favorit/terbaru adalah fitur lokal — GoFile tidak punya endpoint
 * favorit.
 */
class AppPrefs(context: Context) {
    private val store = context.applicationContext.appDataStore

    val state: Flow<PrefsState> = store.data.map { prefs ->
        PrefsState(
            viewMode = if (prefs[VIEW_MODE] == "grid") ViewMode.GRID else ViewMode.LIST,
            sortField = prefs[SORT_FIELD] ?: "name",
            sortAscending = prefs[SORT_ASCENDING] ?: true,
            favorites = GItemJson.decodeList(prefs[FAVORITES]),
            recent = GItemJson.decodeList(prefs[RECENT]),
            themeMode = enumOr(prefs[THEME_MODE], ThemeMode.DARK),
            accent = enumOr(prefs[ACCENT], AccentChoice.CORAL),
            dynamicColor = prefs[DYNAMIC_COLOR] ?: false,
            fontScale = (prefs[FONT_SCALE] ?: 1f).coerceIn(0.85f, 1.3f),
            density = enumOr(prefs[DENSITY], Density.COMFORT),
            language = enumOr(prefs[LANGUAGE], LanguageChoice.SYSTEM),
            animations = enumOr(prefs[ANIMATIONS], AnimPref.SYSTEM),
            showHidden = prefs[SHOW_HIDDEN] ?: false,
            deleteConfirm = enumOr(prefs[DELETE_CONFIRM], DeleteConfirm.HOLD),
            undoSeconds = (prefs[UNDO_SECONDS] ?: 6).coerceIn(3, 15),
            wifiOnly = prefs[WIFI_ONLY] ?: false,
            parallelTransfers = (prefs[PARALLEL] ?: 2).coerceIn(1, 4),
            downloadTree = prefs[DOWNLOAD_TREE] ?: "",
            keepAwake = prefs[KEEP_AWAKE] ?: false,
            notifySound = prefs[NOTIFY_SOUND] ?: false,
            appLock = prefs[APP_LOCK] ?: false,
            gesturesSeen = prefs[GESTURES_SEEN] ?: false,
            whatsNewSeen = prefs[WHATS_NEW] ?: 0,
        )
    }

    suspend fun setViewMode(mode: ViewMode) {
        store.edit { it[VIEW_MODE] = if (mode == ViewMode.GRID) "grid" else "list" }
    }

    suspend fun setSort(field: String, ascending: Boolean) {
        store.edit {
            it[SORT_FIELD] = field
            it[SORT_ASCENDING] = ascending
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[THEME_MODE] = mode.name }
    suspend fun setAccent(accent: AccentChoice) = store.edit { it[ACCENT] = accent.name }
    suspend fun setDynamicColor(enabled: Boolean) = store.edit { it[DYNAMIC_COLOR] = enabled }
    suspend fun setFontScale(scale: Float) =
        store.edit { it[FONT_SCALE] = scale.coerceIn(0.85f, 1.3f) }
    suspend fun setDensity(density: Density) = store.edit { it[DENSITY] = density.name }
    suspend fun setLanguage(language: LanguageChoice) = store.edit { it[LANGUAGE] = language.name }
    suspend fun setAnimations(pref: AnimPref) = store.edit { it[ANIMATIONS] = pref.name }
    suspend fun setShowHidden(show: Boolean) = store.edit { it[SHOW_HIDDEN] = show }
    suspend fun setDeleteConfirm(mode: DeleteConfirm) = store.edit { it[DELETE_CONFIRM] = mode.name }
    suspend fun setUndoSeconds(seconds: Int) =
        store.edit { it[UNDO_SECONDS] = seconds.coerceIn(3, 15) }
    suspend fun setWifiOnly(enabled: Boolean) = store.edit { it[WIFI_ONLY] = enabled }
    suspend fun setParallelTransfers(count: Int) =
        store.edit { it[PARALLEL] = count.coerceIn(1, 4) }
    suspend fun setDownloadTree(uri: String) = store.edit { it[DOWNLOAD_TREE] = uri }
    suspend fun setKeepAwake(enabled: Boolean) = store.edit { it[KEEP_AWAKE] = enabled }
    suspend fun setNotifySound(enabled: Boolean) = store.edit { it[NOTIFY_SOUND] = enabled }
    suspend fun setAppLock(enabled: Boolean) = store.edit { it[APP_LOCK] = enabled }
    suspend fun setGesturesSeen(seen: Boolean) = store.edit { it[GESTURES_SEEN] = seen }
    suspend fun setWhatsNewSeen(version: Int) = store.edit { it[WHATS_NEW] = version }

    suspend fun clearTransfersView() = store.edit { it[TRANSFERS_VIEW] = "" }

    suspend fun toggleFavorite(item: GItem) {
        store.edit { prefs ->
            val current = GItemJson.decodeList(prefs[FAVORITES])
            val next = if (current.any { it.id == item.id }) {
                current.filterNot { it.id == item.id }
            } else {
                listOf(item) + current
            }
            prefs[FAVORITES] = GItemJson.encodeList(next.take(200))
        }
    }

    suspend fun pushRecent(item: GItem) {
        if (item.isFolder) return
        store.edit { prefs ->
            val current = GItemJson.decodeList(prefs[RECENT])
            val next = (listOf(item) + current.filterNot { it.id == item.id }).take(60)
            prefs[RECENT] = GItemJson.encodeList(next)
        }
    }

    suspend fun clearLibrary() {
        store.edit {
            it[FAVORITES] = "[]"
            it[RECENT] = "[]"
        }
    }

    private companion object {
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val SORT_FIELD = stringPreferencesKey("sort_field")
        val SORT_ASCENDING = booleanPreferencesKey("sort_ascending")
        val FAVORITES = stringPreferencesKey("favorites")
        val RECENT = stringPreferencesKey("recent")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT = stringPreferencesKey("accent")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val DENSITY = stringPreferencesKey("density")
        val LANGUAGE = stringPreferencesKey("language")
        val ANIMATIONS = stringPreferencesKey("animations")
        val SHOW_HIDDEN = booleanPreferencesKey("show_hidden")
        val DELETE_CONFIRM = stringPreferencesKey("delete_confirm")
        val UNDO_SECONDS = intPreferencesKey("undo_seconds")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val PARALLEL = intPreferencesKey("parallel_transfers")
        val DOWNLOAD_TREE = stringPreferencesKey("download_tree")
        val KEEP_AWAKE = booleanPreferencesKey("keep_awake")
        val NOTIFY_SOUND = booleanPreferencesKey("notify_sound")
        val APP_LOCK = booleanPreferencesKey("app_lock")
        val GESTURES_SEEN = booleanPreferencesKey("gestures_seen")
        val WHATS_NEW = intPreferencesKey("whats_new_seen")
        val TRANSFERS_VIEW = stringPreferencesKey("transfers_view")
    }
}

private inline fun <reified T : Enum<T>> enumOr(raw: String?, fallback: T): T =
    raw?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: fallback
