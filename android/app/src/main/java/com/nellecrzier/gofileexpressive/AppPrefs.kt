package com.nellecrzier.gofileexpressive

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.appDataStore by preferencesDataStore(name = "gofile-settings")

enum class ViewMode { LIST, GRID }

data class PrefsState(
    val viewMode: ViewMode = ViewMode.LIST,
    val sortField: String = "name",
    val sortAscending: Boolean = true,
    val favorites: List<GItem> = emptyList(),
    val recent: List<GItem> = emptyList(),
)

/**
 * Preferensi tampilan, favorit, dan daftar terbaru. Semua disimpan di
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
    }
}
