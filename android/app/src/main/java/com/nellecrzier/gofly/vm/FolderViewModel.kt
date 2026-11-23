package com.nellecrzier.gofly.vm

import com.nellecrzier.gofly.R
import com.nellecrzier.gofly.AppStrings
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nellecrzier.gofly.AppPrefs
import com.nellecrzier.gofly.FolderCache
import com.nellecrzier.gofly.GItem
import com.nellecrzier.gofly.GoFileException
import com.nellecrzier.gofly.GofileConfig
import com.nellecrzier.gofly.GofileRepository
import com.nellecrzier.gofly.PrefsState
import com.nellecrzier.gofly.ViewMode
import com.nellecrzier.gofly.ui.Crumb
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/** Mode pemilih folder untuk aksi pindah/salin. */
enum class PickerMode { MOVE, COPY }

data class PickerState(
    val mode: PickerMode = PickerMode.MOVE,
    val crumbs: List<Crumb> = emptyList(),
    val folders: List<GItem> = emptyList(),
    val busy: Boolean = false,
)

data class PendingUndo(
    val items: List<GItem>,
    val folderId: String,
    val snapshot: List<GItem>,
)

data class FolderUiState(
    val crumbs: List<Crumb> = emptyList(),
    val items: List<GItem> = emptyList(),
    val totalCount: Int = 0,
    val busy: Boolean = false,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val page: Int = 1,
    val hasNextPage: Boolean = false,
    val query: String = "",
    val filter: String = FILTER_ALL,
    val selectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val offline: Boolean = false,
    val undo: PendingUndo? = null,
    val picker: PickerState? = null,
    val globalResults: List<GItem> = emptyList(),
    val globalSearching: Boolean = false,
    val error: String? = null,
) {
    val currentFolderId: String? get() = crumbs.lastOrNull()?.id
    val selectedItems: List<GItem> get() = items.filter { it.id in selectedIds }
    val undoCount: Int get() = undo?.items?.size ?: 0
}

sealed interface FolderEvent {
    data class Message(val text: String, val error: Boolean = true) : FolderEvent
    data object SessionExpired : FolderEvent
}

const val FILTER_ALL = "all"
const val FILTER_IMAGE = "image"
const val FILTER_VIDEO = "video"
const val FILTER_AUDIO = "audio"
const val FILTER_DOCUMENT = "document"
const val FILTER_ARCHIVE = "archive"

/**
 * Isi folder, paginasi, pencarian lokal, seleksi, dan operasi tulis.
 *
 * Catatan API: GoFile tidak punya recycle bin. Hapus ditunda selama
 * [GofileConfig.DELETE_UNDO_MS] dan baru benar-benar dikirim setelah jendela
 * Undo berakhir.
 */
class FolderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GofileRepository()
    private val prefs = AppPrefs(application)
    private val cache = FolderCache(application)

    val preferences: StateFlow<PrefsState> =
        prefs.state.stateIn(viewModelScope, SharingStarted.Eagerly, PrefsState())

    private val _state = MutableStateFlow(FolderUiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<FolderEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    private var deleteJob: Job? = null

    /** Jendela Undo dalam milidetik; mengikuti Setelan (3–15 detik). */
    var undoWindowMs: Long = GofileConfig.DELETE_UNDO_MS
    private var globalJob: Job? = null
    private var loadJob: Job? = null
    private var lastToken: String? = null

    /* ------------------------------ navigasi ----------------------------- */

    fun openRoot(token: String, rootFolder: String) {
        val id = rootFolder.ifBlank { "root" }
        if (_state.value.crumbs.firstOrNull()?.id == id && _state.value.items.isNotEmpty()) return
        _state.value = FolderUiState(crumbs = listOf(Crumb(id, AppStrings.get(R.string.folder_root_name))))
        load(token, id, reset = true)
    }

    fun open(token: String, item: GItem) {
        _state.update {
            it.copy(
                crumbs = it.crumbs + Crumb(item.id, item.name),
                items = emptyList(),
                query = "",
                filter = FILTER_ALL,
                selectionMode = false,
                selectedIds = emptySet(),
            )
        }
        load(token, item.id, reset = true)
    }

    /** Buka folder langsung dari Pustaka/pencarian, tanpa breadcrumb lama. */
    fun openDirect(token: String, item: GItem) {
        _state.update {
            it.copy(
                crumbs = listOf(Crumb(item.id, item.name)),
                items = emptyList(),
                query = "",
                filter = FILTER_ALL,
                selectionMode = false,
                selectedIds = emptySet(),
            )
        }
        load(token, item.id, reset = true)
    }

    /** Naik satu folder. Mengembalikan false kalau sudah di akar. */
    fun back(token: String): Boolean {
        val crumbs = _state.value.crumbs
        if (crumbs.size <= 1) return false
        val next = crumbs.dropLast(1)
        _state.update {
            it.copy(
                crumbs = next,
                items = emptyList(),
                query = "",
                selectionMode = false,
                selectedIds = emptySet(),
            )
        }
        load(token, next.last().id, reset = true)
        return true
    }

    fun jumpTo(token: String, index: Int) {
        val crumbs = _state.value.crumbs
        if (index !in crumbs.indices || index == crumbs.lastIndex) return
        val next = crumbs.take(index + 1)
        _state.update { it.copy(crumbs = next, items = emptyList(), query = "") }
        load(token, next.last().id, reset = true)
    }

    fun refresh(token: String) {
        val id = _state.value.currentFolderId ?: return
        load(token, id, reset = true, refreshing = true)
    }

    fun loadMore(token: String) {
        val current = _state.value
        if (current.loadingMore || !current.hasNextPage || current.busy) return
        val id = current.currentFolderId ?: return
        load(token, id, reset = false)
    }

    private fun load(
        token: String,
        folderId: String,
        reset: Boolean,
        refreshing: Boolean = false,
    ) {
        lastToken = token
        loadJob?.cancel()
        if (reset) {
            val cached = cache.load(folderId)
            if (cached != null && _state.value.items.isEmpty()) {
                _state.update {
                    it.copy(
                        items = cached.items,
                        totalCount = cached.items.size,
                        offline = true,
                    )
                }
            }
        }
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    busy = reset && it.items.isEmpty(),
                    refreshing = refreshing,
                    loadingMore = !reset,
                    error = null,
                )
            }
            try {
                val page = repository.getFolderPage(
                    token = token,
                    folderId = folderId,
                    page = if (reset) 1 else _state.value.page + 1,
                    sortField = preferences.value.sortField,
                    sortDirection = if (preferences.value.sortAscending) 1 else -1,
                )
                val merged = if (reset) page.items else merge(_state.value.items, page.items)
                if (reset) cache.save(folderId, page.items)
                _state.update {
                    it.copy(
                        items = merged,
                        totalCount = page.totalCount.coerceAtLeast(merged.size),
                        page = page.page,
                        hasNextPage = page.hasNextPage,
                        busy = false,
                        refreshing = false,
                        loadingMore = false,
                        offline = false,
                    )
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(busy = false, refreshing = false, loadingMore = false)
                }
                if (error is GoFileException.AuthExpired) {
                    _events.tryEmit(FolderEvent.SessionExpired)
                } else {
                    val hadCache = _state.value.items.isNotEmpty()
                    _state.update { it.copy(offline = hadCache, error = error.message) }
                    if (!hadCache) {
                        _events.tryEmit(
                            FolderEvent.Message(error.message ?: AppStrings.get(R.string.err_generic)),
                        )
                    }
                }
            }
        }
    }

    private fun merge(old: List<GItem>, fresh: List<GItem>): List<GItem> {
        val seen = old.mapTo(HashSet()) { it.id }
        return old + fresh.filter { seen.add(it.id) }
    }

    /* ------------------------------ pencarian ---------------------------- */

    fun setQuery(value: String) = _state.update { it.copy(query = value) }

    fun setFilter(value: String) = _state.update { it.copy(filter = value) }

    fun clearFilters() = _state.update { it.copy(query = "", filter = FILTER_ALL) }

    /** Cari di seluruh folder yang pernah di-cache (GoFile tidak punya global search). */
    fun searchGlobal(query: String) {
        globalJob?.cancel()
        val clean = query.trim()
        if (clean.isBlank()) {
            _state.update { it.copy(globalResults = emptyList(), globalSearching = false) }
            return
        }
        _state.update { it.copy(globalSearching = true) }
        globalJob = viewModelScope.launch {
            delay(220)
            val lowered = clean.lowercase(Locale.ROOT)
            val results = cache.all()
                .flatMap { it.items }
                .filter { it.name.lowercase(Locale.ROOT).contains(lowered) }
                .distinctBy { it.id }
                .take(80)
            _state.update { it.copy(globalResults = results, globalSearching = false) }
        }
    }

    /* ------------------------------- seleksi ----------------------------- */

    fun toggleSelection(item: GItem) {
        _state.update { current ->
            val next = if (item.id in current.selectedIds) {
                current.selectedIds - item.id
            } else {
                current.selectedIds + item.id
            }
            current.copy(selectionMode = next.isNotEmpty(), selectedIds = next)
        }
    }

    fun startSelection(item: GItem) = _state.update {
        it.copy(selectionMode = true, selectedIds = it.selectedIds + item.id)
    }

    fun selectAll(ids: List<String>) = _state.update {
        it.copy(selectionMode = ids.isNotEmpty(), selectedIds = ids.toSet())
    }

    fun invertSelection(ids: List<String>) = _state.update {
        val next = ids.toSet() - it.selectedIds
        it.copy(selectionMode = next.isNotEmpty(), selectedIds = next)
    }

    fun clearSelection() = _state.update { it.copy(selectionMode = false, selectedIds = emptySet()) }

    /* -------------------------------- tulis ------------------------------ */

    fun createFolder(token: String, name: String) {
        val parent = _state.value.currentFolderId ?: return
        val clean = name.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                repository.createFolder(token, parent, clean)
                _state.update { it.copy(busy = false) }
                _events.tryEmit(FolderEvent.Message(AppStrings.get(R.string.folder_created, clean), error = false))
                load(token, parent, reset = true)
            } catch (error: Exception) {
                fail(error)
            }
        }
    }

    fun rename(token: String, item: GItem, newName: String) {
        val clean = newName.trim()
        if (clean.isBlank() || clean == item.name) return
        val parent = _state.value.currentFolderId ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                repository.rename(token, item.id, clean)
                _state.update {
                    it.copy(
                        busy = false,
                        items = it.items.map { old ->
                            if (old.id == item.id) old.copy(name = clean) else old
                        },
                    )
                }
                _events.tryEmit(FolderEvent.Message(AppStrings.get(R.string.folder_renamed, clean), error = false))
                cache.save(parent, _state.value.items)
            } catch (error: Exception) {
                fail(error)
            }
        }
    }

    /**
     * Hapus dengan jendela Undo. Item langsung hilang dari daftar, tetapi
     * panggilan DELETE baru dikirim setelah [GofileConfig.DELETE_UNDO_MS].
     */
    fun requestDelete(token: String, items: List<GItem>) {
        if (items.isEmpty()) return
        val folderId = _state.value.currentFolderId ?: return
        deleteJob?.cancel()
        val snapshot = _state.value.items
        val ids = items.mapTo(HashSet()) { it.id }
        _state.update {
            it.copy(
                items = it.items.filterNot { item -> item.id in ids },
                selectedIds = it.selectedIds - ids,
                selectionMode = (it.selectedIds - ids).isNotEmpty(),
                undo = PendingUndo(items, folderId, snapshot),
            )
        }
        deleteJob = viewModelScope.launch {
            delay(undoWindowMs)
            try {
                repository.delete(token, items.map { it.id })
                _state.update { it.copy(undo = null) }
                _events.tryEmit(
                    FolderEvent.Message(
                        if (items.size == 1) {
                            AppStrings.get(R.string.folder_deleted_one, items.first().name)
                        } else {
                            AppStrings.get(R.string.folder_deleted_many, items.size)
                        },
                        error = false,
                    ),
                )
                cache.save(folderId, _state.value.items)
            } catch (error: Exception) {
                _state.update { it.copy(undo = null) }
                fail(error)
                load(token, folderId, reset = true)
            }
        }
    }

    /** Batalkan penghapusan sebelum jendela Undo berakhir. */
    fun undoDelete() {
        deleteJob?.cancel()
        deleteJob = null
        _state.update { current ->
            val undo = current.undo ?: return@update current
            current.copy(
                items = undo.snapshot,
                undo = null,
                totalCount = maxOf(current.totalCount, undo.snapshot.size),
            )
        }
    }

    fun toggleFavorite(item: GItem) {
        viewModelScope.launch { prefs.toggleFavorite(item) }
    }

    fun recordRecent(item: GItem) {
        viewModelScope.launch { prefs.pushRecent(item) }
    }

    fun clearLibrary() {
        viewModelScope.launch { prefs.clearLibrary() }
    }

    fun setViewMode(mode: ViewMode) {
        viewModelScope.launch { prefs.setViewMode(mode) }
    }

    fun setSort(field: String, ascending: Boolean) {
        viewModelScope.launch { prefs.setSort(field, ascending) }
        val id = _state.value.currentFolderId ?: return
        lastToken?.let { load(it, id, reset = true, refreshing = true) }
    }

    fun rememberToken(token: String) {
        lastToken = token
    }

    /* ---------------------------- pemilih folder ------------------------- */

    fun openPicker(token: String, mode: PickerMode) {
        lastToken = token
        val start = _state.value.currentFolderId ?: return
        val name = _state.value.crumbs.lastOrNull()?.name ?: AppStrings.get(R.string.folder_root_name)
        _state.update {
            it.copy(picker = PickerState(mode = mode, crumbs = listOf(Crumb(start, name))))
        }
        loadPicker(token, start)
    }

    fun pickerEnter(token: String, folder: GItem) {
        val picker = _state.value.picker ?: return
        _state.update {
            it.copy(
                picker = picker.copy(
                    crumbs = picker.crumbs + Crumb(folder.id, folder.name),
                    folders = emptyList(),
                ),
            )
        }
        loadPicker(token, folder.id)
    }

    fun pickerBack(token: String) {
        val picker = _state.value.picker ?: return
        if (picker.crumbs.size <= 1) return
        val next = picker.crumbs.dropLast(1)
        _state.update { it.copy(picker = picker.copy(crumbs = next, folders = emptyList())) }
        loadPicker(token, next.last().id)
    }

    fun pickerNewFolder(token: String, name: String) {
        val picker = _state.value.picker ?: return
        val parent = picker.crumbs.lastOrNull()?.id ?: return
        val clean = name.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(picker = picker.copy(busy = true)) }
            try {
                repository.createFolder(token, parent, clean)
                loadPicker(token, parent)
                _events.tryEmit(FolderEvent.Message(AppStrings.get(R.string.folder_created, clean), error = false))
            } catch (error: Exception) {
                _state.update { it.copy(picker = picker.copy(busy = false)) }
                fail(error)
            }
        }
    }

    fun closePicker() = _state.update { it.copy(picker = null) }

    fun confirmPicker(token: String, items: List<GItem>) {
        val picker = _state.value.picker ?: return
        val destination = picker.crumbs.lastOrNull()?.id ?: return
        val ids = items.map { it.id }
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(picker = picker.copy(busy = true)) }
            try {
                when (picker.mode) {
                    PickerMode.MOVE -> repository.move(token, ids, destination)
                    PickerMode.COPY -> repository.copy(token, ids, destination)
                }
                _state.update {
                    it.copy(
                        picker = null,
                        selectionMode = false,
                        selectedIds = emptySet(),
                        items = if (picker.mode == PickerMode.MOVE) {
                            it.items.filterNot { item -> item.id in ids }
                        } else {
                            it.items
                        },
                    )
                }
                _events.tryEmit(
                    FolderEvent.Message(
                        when (picker.mode) {
                            PickerMode.MOVE -> AppStrings.get(R.string.folder_moved_to, picker.crumbs.last().name)
                            PickerMode.COPY -> AppStrings.get(R.string.folder_copied_to, picker.crumbs.last().name)
                        },
                        error = false,
                    ),
                )
                _state.value.currentFolderId?.let { cache.save(it, _state.value.items) }
            } catch (error: Exception) {
                _state.update { it.copy(picker = picker.copy(busy = false)) }
                fail(error)
            }
        }
    }

    private fun loadPicker(token: String, folderId: String) {
        viewModelScope.launch {
            val picker = _state.value.picker ?: return@launch
            _state.update { it.copy(picker = picker.copy(busy = true)) }
            try {
                val page = repository.getFolderPage(token, folderId, page = 1)
                _state.update { current ->
                    val currentPicker = current.picker ?: return@update current
                    current.copy(
                        picker = currentPicker.copy(
                            folders = page.items.filter { it.isFolder },
                            busy = false,
                        ),
                    )
                }
            } catch (error: Exception) {
                _state.update { current ->
                    val currentPicker = current.picker ?: return@update current
                    current.copy(picker = currentPicker.copy(busy = false))
                }
                if (error is GoFileException.AuthExpired) {
                    _events.tryEmit(FolderEvent.SessionExpired)
                } else {
                    _events.tryEmit(FolderEvent.Message(error.message ?: AppStrings.get(R.string.err_generic)))
                }
            }
        }
    }

    private fun fail(error: Exception) {
        _state.update { it.copy(busy = false) }
        if (error is GoFileException.AuthExpired) {
            _events.tryEmit(FolderEvent.SessionExpired)
        } else {
            _events.tryEmit(FolderEvent.Message(error.message ?: AppStrings.get(R.string.err_generic)))
        }
    }
}
