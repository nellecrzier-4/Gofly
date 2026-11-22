@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.nellecrzier.gofileexpressive.ui.folder

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.nellecrzier.gofileexpressive.GItem
import com.nellecrzier.gofileexpressive.MainActivity
import com.nellecrzier.gofileexpressive.DeleteConfirm
import com.nellecrzier.gofileexpressive.Density
import com.nellecrzier.gofileexpressive.GofileApi
import com.nellecrzier.gofileexpressive.ui.HyperButton
import com.nellecrzier.gofileexpressive.ui.preview.PreviewHost
import com.nellecrzier.gofileexpressive.ui.preview.PreviewTarget
import com.nellecrzier.gofileexpressive.ui.share.ContentSettingsSheet
import com.nellecrzier.gofileexpressive.ui.share.ShareIntents
import com.nellecrzier.gofileexpressive.ui.share.ShareSheet
import com.nellecrzier.gofileexpressive.ui.share.shareLinkOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.nellecrzier.gofileexpressive.PrefsState
import com.nellecrzier.gofileexpressive.R
import com.nellecrzier.gofileexpressive.ViewMode
import com.nellecrzier.gofileexpressive.ui.Crumb
import com.nellecrzier.gofileexpressive.ui.EmptyState
import com.nellecrzier.gofileexpressive.ui.ExpressiveLoader
import com.nellecrzier.gofileexpressive.ui.HoldToConfirm
import com.nellecrzier.gofileexpressive.ui.Hyper
import com.nellecrzier.gofileexpressive.ui.hyperSpring
import com.nellecrzier.gofileexpressive.ui.HyperPower3Out
import com.nellecrzier.gofileexpressive.ui.HyperIconButton
import com.nellecrzier.gofileexpressive.ui.HyperRow
import com.nellecrzier.gofileexpressive.ui.HyperTextField
import com.nellecrzier.gofileexpressive.ui.IconChip
import com.nellecrzier.gofileexpressive.ui.readableSize
import com.nellecrzier.gofileexpressive.ui.components.FileKind
import com.nellecrzier.gofileexpressive.ui.components.fileKind
import com.nellecrzier.gofileexpressive.ui.components.matchesFilter
import com.nellecrzier.gofileexpressive.ui.components.sortItems
import com.nellecrzier.gofileexpressive.vm.FILTER_ALL
import com.nellecrzier.gofileexpressive.vm.FILTER_ARCHIVE
import com.nellecrzier.gofileexpressive.vm.FILTER_AUDIO
import com.nellecrzier.gofileexpressive.vm.FILTER_DOCUMENT
import com.nellecrzier.gofileexpressive.vm.FILTER_IMAGE
import com.nellecrzier.gofileexpressive.vm.FILTER_VIDEO
import com.nellecrzier.gofileexpressive.vm.FolderUiState
import com.nellecrzier.gofileexpressive.vm.FolderViewModel
import com.nellecrzier.gofileexpressive.vm.PickerMode
import com.nellecrzier.gofileexpressive.vm.PickerState
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private const val SWIPE_THRESHOLD = 96f

/**
 * File manager native: paginasi, pencarian, urutan, filter, kisi/daftar,
 * breadcrumb, tarik-untuk-segarkan, skeleton, cache offline, seleksi ganda,
 * urungkan hapus, detail, favorit, dan pemilih folder untuk pindah/salin.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    vm: FolderViewModel,
    state: FolderUiState,
    prefs: PrefsState,
    token: String,
    reduceMotion: Boolean,
    activity: MainActivity,
    onNewFolder: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current

    var searchOpen by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var pendingRename by remember { mutableStateOf<GItem?>(null) }
    var pendingDelete by remember { mutableStateOf<List<GItem>>(emptyList()) }
    var details by remember { mutableStateOf<GItem?>(null) }
    var fabOpen by remember { mutableStateOf(false) }
    var pickerTargets by remember { mutableStateOf<List<GItem>>(emptyList()) }
    var previewTarget by remember { mutableStateOf<PreviewTarget?>(null) }
    var shareTarget by remember { mutableStateOf<GItem?>(null) }
    var contentTarget by remember { mutableStateOf<GItem?>(null) }
    val scope = rememberCoroutineScope()

    val query = state.query
    val filter = state.filter
    val visible = remember(state.items, query, filter, prefs.sortField, prefs.sortAscending) {
        val lowered = query.trim().lowercase(Locale.ROOT)
        val filtered = state.items.filter { item ->
            item.matchesFilter(filter) &&
                (lowered.isBlank() || item.name.lowercase(Locale.ROOT).contains(lowered))
        }
        sortItems(filtered, prefs.sortField, prefs.sortAscending)
    }
    val selected = state.selectedItems

    fun share(items: List<GItem>) {
        val links = items.filter { it.link.startsWith("http") }
        if (links.isEmpty()) {
            activity.toast(context.getString(R.string.details_no_link))
            return
        }
        val text = links.joinToString("\n") { "${it.name}\n${it.link}" }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_share)))
    }

    fun copyLink(item: GItem) {
        if (item.link.isBlank()) {
            activity.toast(context.getString(R.string.details_no_link))
            return
        }
        clipboard.setText(AnnotatedString(item.link))
        activity.toast(context.getString(R.string.details_link_copied))
    }

    fun openPreviewOrFolder(item: GItem) {
        if (item.isFolder) {
            vm.open(token, item)
            return
        }
        vm.recordRecent(item)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        previewTarget = PreviewTarget(
            item = item,
            album = visible.filterNot { it.isFolder },
        )
    }

    /** Unduh ke cache lalu serahkan ke aplikasi lain — tanpa WebView. */
    fun openWithExternal(item: GItem) {
        if (item.link.isBlank()) {
            activity.toast(context.getString(R.string.details_no_link))
            return
        }
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    GofileApi.downloadToTemp(context, item.link, item.name, token)
                }
                activity.openWith(file, item.mimetype)
            } catch (error: Exception) {
                activity.toast(error.message ?: context.getString(R.string.preview_open_with))
            }
        }
    }

    fun toggleSelection(item: GItem) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        vm.toggleSelection(item)
    }

    LaunchedEffect(token) { vm.rememberToken(token) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.statusBarsPadding())
            FolderTopBar(
                title = state.crumbs.lastOrNull()?.name ?: stringResource(R.string.tab_folder),
                canGoBack = state.crumbs.size > 1,
                searchOpen = searchOpen,
                viewMode = prefs.viewMode,
                onBack = { vm.back(token) },
                onToggleSearch = {
                    searchOpen = !searchOpen
                    if (!searchOpen) vm.setQuery("")
                },
                onToggleView = {
                    vm.setViewMode(
                        if (prefs.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST,
                    )
                },
                onOpenSort = { sortMenu = true },
                onRefresh = { vm.refresh(token) },
            )

            AnimatedVisibility(
                visible = searchOpen,
                enter = fadeIn() + slideInVertically { -it / 3 },
                exit = fadeOut() + slideOutVertically { -it / 3 },
            ) {
                HyperTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    label = stringResource(R.string.folder_search_hint),
                    leadingIcon = Icons.Filled.Search,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    trailing = {
                        if (query.isNotBlank()) {
                            HyperIconButton(
                                icon = Icons.Filled.Close,
                                description = stringResource(R.string.folder_search_close),
                                onClick = { vm.setQuery("") },
                            )
                        }
                    },
                )
            }

            Breadcrumbs(
                crumbs = state.crumbs,
                onJump = { index -> vm.jumpTo(token, index) },
            )

            FilterChips(
                selected = filter,
                onSelect = vm::setFilter,
            )

            if (state.offline) {
                OfflineBanner(
                    text = stringResource(R.string.folder_offline_banner),
                    hint = stringResource(R.string.folder_offline_hint),
                )
            }

            Box(Modifier.weight(1f)) {
                PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = { vm.refresh(token) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when {
                        state.busy && state.items.isEmpty() -> SkeletonList(reduceMotion)

                        visible.isEmpty() -> FolderEmpty(
                            searching = query.isNotBlank() || filter != FILTER_ALL,
                            query = query,
                            error = state.error,
                            onRetry = { vm.refresh(token) },
                            onUpload = {
                                state.crumbs.lastOrNull()?.let {
                                    activity.pickFileForUpload(it.id, token)
                                }
                            },
                        )

                        prefs.viewMode == ViewMode.GRID -> FileGrid(
                            items = visible,
                            selectedIds = state.selectedIds,
                            selectionMode = state.selectionMode,
                            favorites = prefs.favorites.map { it.id }.toSet(),
                            hasNextPage = state.hasNextPage,
                            loadingMore = state.loadingMore,
                            onOpen = { item ->
                                if (state.selectionMode) {
                                    toggleSelection(item)
                                } else {
                                    openPreviewOrFolder(item)
                                }
                            },
                            onLongPress = { item ->
                                if (state.selectionMode) toggleSelection(item) else vm.startSelection(item)
                            },
                            onMenu = { item -> details = item },
                            onLoadMore = { vm.loadMore(token) },
                        )

                        else -> FileList(
                            items = visible,
                            selectedIds = state.selectedIds,
                            selectionMode = state.selectionMode,
                            favorites = prefs.favorites.map { it.id }.toSet(),
                            hasNextPage = state.hasNextPage,
                            loadingMore = state.loadingMore,
                            query = query,
                            onOpen = { item ->
                                if (state.selectionMode) {
                                    toggleSelection(item)
                                } else {
                                    openPreviewOrFolder(item)
                                }
                            },
                            onLongPress = { item ->
                                if (state.selectionMode) toggleSelection(item) else vm.startSelection(item)
                            },
                            onMenu = { item -> details = item },
                            onSwipeShare = { share(listOf(it)) },
                            onSwipeDelete = { pendingDelete = listOf(it) },
                            onLoadMore = { vm.loadMore(token) },
                            density = prefs.density,
                        )
                    }
                }
            }
        }

        // Bar aksi seleksi: kapsul kaca di atas bar navigasi.
        AnimatedVisibility(
            visible = state.selectionMode,
            enter = fadeIn(tween(160)) +
                slideInVertically(hyperSpring(0.46f, 0.74f)) { it },
            exit = fadeOut(tween(120)) +
                slideOutVertically(tween(180, easing = HyperPower3Out)) { it / 3 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, bottom = 86.dp),
        ) {
            SelectionBar(
                count = state.selectedIds.size,
                onClear = vm::clearSelection,
                onSelectAll = { vm.selectAll(visible.map { it.id }) },
                onInvert = { vm.invertSelection(visible.map { it.id }) },
                onDownload = {
                    selected.filter { it.link.isNotBlank() }.forEach {
                        activity.startDownload(it.link, it.name, token)
                        vm.recordRecent(it)
                    }
                },
                onShare = { share(selected) },
                onMove = {
                    pickerTargets = selected
                    vm.openPicker(token, PickerMode.MOVE)
                },
                onCopy = {
                    pickerTargets = selected
                    vm.openPicker(token, PickerMode.COPY)
                },
                onDelete = { pendingDelete = selected },
            )
        }

        // Pil Undo untuk penghapusan yang ditunda.
        AnimatedVisibility(
            visible = state.undo != null,
            enter = fadeIn(tween(160)) +
                slideInVertically(hyperSpring(0.46f, 0.74f)) { it },
            exit = fadeOut(tween(120)) +
                slideOutVertically(tween(180, easing = HyperPower3Out)) { it / 3 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = if (state.selectionMode) 148.dp else 86.dp,
                ),
        ) {
            UndoBar(
                count = state.undoCount,
                onUndo = vm::undoDelete,
            )
        }

        // FAB unggah / folder baru.
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 18.dp, bottom = if (state.selectionMode) 158.dp else 96.dp),
        ) {
            AnimatedVisibility(
                visible = fabOpen,
                enter = fadeIn(tween(140)) + scaleIn(
                    animationSpec = hyperSpring(0.4f, 0.62f),
                    initialScale = 0.86f,
                ),
                exit = fadeOut(tween(120)) + scaleOut(tween(150), targetScale = 0.9f),
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    MiniAction(
                        icon = Icons.Filled.CreateNewFolder,
                        label = stringResource(R.string.folder_new_folder),
                        onClick = {
                            fabOpen = false
                            onNewFolder()
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    MiniAction(
                        icon = Icons.Filled.Upload,
                        label = stringResource(R.string.action_upload),
                        onClick = {
                            fabOpen = false
                            state.crumbs.lastOrNull()?.let { activity.pickFileForUpload(it.id, token) }
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
            Surface(
                shape = CircleShape,
                color = Hyper.Accent,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = {
                            if (state.selectionMode) vm.clearSelection() else fabOpen = !fabOpen
                        },
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            fabOpen = !fabOpen
                        },
                    ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (fabOpen) Icons.Filled.Close else Icons.Filled.Add,
                        contentDescription = stringResource(R.string.action_upload),
                        tint = Hyper.AccentInk,
                    )
                }
            }
        }

        DropdownMenu(
            expanded = sortMenu,
            onDismissRequest = { sortMenu = false },
            containerColor = Hyper.Card,
        ) {
            SortMenuItem(stringResource(R.string.sort_name), "name", prefs, vm) { sortMenu = false }
            SortMenuItem(stringResource(R.string.sort_size), "size", prefs, vm) { sortMenu = false }
            SortMenuItem(stringResource(R.string.sort_created), "created", prefs, vm) { sortMenu = false }
            SortMenuItem(stringResource(R.string.sort_modified), "modified", prefs, vm) { sortMenu = false }
            SortMenuItem(stringResource(R.string.sort_type), "type", prefs, vm) { sortMenu = false }
            DropdownMenuItem(
                text = {
                    Text(
                        if (prefs.sortAscending) stringResource(R.string.sort_descending)
                        else stringResource(R.string.sort_ascending),
                        color = Hyper.Text,
                    )
                },
                onClick = {
                    vm.setSort(prefs.sortField, !prefs.sortAscending)
                    sortMenu = false
                },
            )
        }
    }

    // Dialog ganti nama.
    pendingRename?.let { item ->
        var name by remember(item.id) { mutableStateOf(item.name) }
        AlertDialog(
            onDismissRequest = { pendingRename = null },
            containerColor = Hyper.Card,
            shape = RoundedCornerShape(24.dp),
            title = { Text(stringResource(R.string.folder_rename_title), color = Hyper.Text) },
            text = {
                HyperTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.folder_rename_label),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.rename(token, item, name)
                        pendingRename = null
                    },
                    enabled = name.isNotBlank() && name != item.name,
                ) { Text(stringResource(R.string.action_save), color = Hyper.Accent) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRename = null }) {
                    Text(stringResource(R.string.action_cancel), color = Hyper.Text2)
                }
            },
        )
    }

    // Dialog hapus dengan tahan-untuk-konfirmasi.
    if (pendingDelete.isNotEmpty()) {
        val targets = pendingDelete
        AlertDialog(
            onDismissRequest = { pendingDelete = emptyList() },
            containerColor = Hyper.Card,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    stringResource(
                        R.string.folder_delete_title,
                        if (targets.size == 1) {
                            if (targets.first().isFolder) stringResource(R.string.details_folder)
                            else stringResource(R.string.details_file)
                        } else {
                            stringResource(R.string.folder_item_count, targets.size)
                        },
                    ),
                    color = Hyper.Text,
                )
            },
            text = {
                Column {
                    Text(
                        if (targets.size == 1) {
                            stringResource(R.string.folder_delete_body, targets.first().name)
                        } else {
                            stringResource(R.string.folder_deleted_many, targets.size)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Hyper.Text2,
                    )
                    Spacer(Modifier.height(18.dp))
                    if (prefs.deleteConfirm == DeleteConfirm.DIALOG) {
                        HyperButton(
                            text = stringResource(R.string.action_delete),
                            onClick = {
                                vm.requestDelete(token, targets)
                                pendingDelete = emptyList()
                            },
                            enabled = !state.busy,
                            primary = false,
                        )
                    } else {
                        HoldToConfirm(
                            label = stringResource(R.string.folder_delete_hold),
                            hint = stringResource(R.string.folder_delete_hint),
                            enabled = !state.busy,
                            onConfirm = {
                                vm.requestDelete(token, targets)
                                pendingDelete = emptyList()
                            },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pendingDelete = emptyList() }) {
                    Text(stringResource(R.string.action_cancel), color = Hyper.Text2)
                }
            },
        )
    }

    details?.let { item ->
        ItemDetailsSheet(
            item = item,
            favorite = prefs.favorites.any { it.id == item.id },
            onDismiss = { details = null },
            onFavorite = { vm.toggleFavorite(item) },
            onDownload = {
                activity.startDownload(item.link, item.name, token, item.md5)
                vm.recordRecent(item)
                details = null
            },
            onShare = {
                details = null
                shareTarget = item
            },
            onPreview = {
                details = null
                openPreviewOrFolder(item)
            },
            onContentSettings = {
                details = null
                contentTarget = item
            },
            onCopyLink = { copyLink(item) },
            onRename = {
                details = null
                pendingRename = item
            },
            onMove = {
                details = null
                pickerTargets = listOf(item)
                vm.openPicker(token, PickerMode.MOVE)
            },
            onCopy = {
                details = null
                pickerTargets = listOf(item)
                vm.openPicker(token, PickerMode.COPY)
            },
            onDelete = {
                details = null
                pendingDelete = listOf(item)
            },
        )
    }

    state.picker?.let { picker ->
        PickerSheet(
            picker = picker,
            mode = picker.mode,
            onDismiss = vm::closePicker,
            onEnter = { vm.pickerEnter(token, it) },
            onUp = { vm.pickerBack(token) },
            onNewFolder = { name -> vm.pickerNewFolder(token, name) },
            onConfirm = { vm.confirmPicker(token, pickerTargets) },
        )
    }

    // Pratinjau native menutupi seluruh layar; ia menelan ketukan di latar.
    PreviewHost(
        target = previewTarget,
        token = token,
        reduceMotion = reduceMotion,
        onClose = { previewTarget = null },
        onOpenWith = { item -> openWithExternal(item) },
        onDownload = { item ->
            activity.startDownload(item.link, item.name, token, item.md5)
            vm.recordRecent(item)
        },
        onShare = { item -> shareTarget = item },
    )

    shareTarget?.let { item ->
        ShareSheet(
            item = item,
            token = token,
            reduceMotion = reduceMotion,
            onDismiss = { shareTarget = null },
            onShareIntent = { target ->
                val link = shareLinkOf(target)
                if (link.isBlank()) {
                    activity.toast(context.getString(R.string.details_no_link))
                } else {
                    context.startActivity(ShareIntents.sendText(context, link, target.name))
                }
                shareTarget = null
            },
            onDownload = { target ->
                activity.startDownload(target.link, target.name, token, target.md5)
                shareTarget = null
            },
        )
    }

    contentTarget?.let { item ->
        ContentSettingsSheet(
            item = item,
            token = token,
            reduceMotion = reduceMotion,
            onDismiss = { contentTarget = null },
            onSaved = { message ->
                activity.toast(message)
                contentTarget = null
                vm.refresh(token)
            },
        )
    }
}

/* --------------------------------- header --------------------------------- */

@Composable
private fun FolderTopBar(
    title: String,
    canGoBack: Boolean,
    searchOpen: Boolean,
    viewMode: ViewMode,
    onBack: () -> Unit,
    onToggleSearch: () -> Unit,
    onToggleView: () -> Unit,
    onOpenSort: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Hyper.GlassFill)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (canGoBack) {
            HyperIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                description = stringResource(R.string.root),
                onClick = onBack,
            )
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Hyper.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        HyperIconButton(
            icon = if (searchOpen) Icons.Filled.Close else Icons.Filled.Search,
            description = stringResource(R.string.folder_search_hint),
            onClick = onToggleSearch,
        )
        HyperIconButton(
            icon = Icons.AutoMirrored.Filled.Sort,
            description = stringResource(R.string.folder_sort),
            onClick = onOpenSort,
        )
        HyperIconButton(
            icon = if (viewMode == ViewMode.GRID) Icons.Filled.List else Icons.Filled.GridView,
            description = stringResource(
                if (viewMode == ViewMode.GRID) R.string.view_list else R.string.view_grid,
            ),
            onClick = onToggleView,
        )
        HyperIconButton(
            icon = Icons.Filled.FolderOpen,
            description = stringResource(R.string.action_refresh),
            onClick = onRefresh,
        )
    }
}

@Composable
private fun SortMenuItem(
    label: String,
    field: String,
    prefs: PrefsState,
    vm: FolderViewModel,
    onDone: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    color = if (prefs.sortField == field) Hyper.Accent else Hyper.Text,
                    modifier = Modifier.weight(1f),
                )
                if (prefs.sortField == field) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Hyper.Accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
        onClick = {
            vm.setSort(field, prefs.sortAscending)
            onDone()
        },
    )
}

@Composable
private fun Breadcrumbs(crumbs: List<Crumb>, onJump: (Int) -> Unit) {
    if (crumbs.size <= 1) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(bottom = 4.dp),
    ) {
        items(crumbs.size) { index ->
            val crumb = crumbs[index]
            val last = index == crumbs.lastIndex
            Surface(
                shape = CircleShape,
                color = if (last) Hyper.AccentSoft else Hyper.Chip,
                modifier = Modifier.clip(CircleShape).combinedClickable(
                    onClick = { onJump(index) },
                ),
            ) {
                Text(
                    text = crumb.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (last) Hyper.Accent else Hyper.Text2,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            if (!last) {
                Text(
                    "›",
                    color = Hyper.Text3,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun FilterChips(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        FILTER_ALL to R.string.filter_all,
        FILTER_IMAGE to R.string.filter_images,
        FILTER_VIDEO to R.string.filter_video,
        FILTER_AUDIO to R.string.filter_audio,
        FILTER_DOCUMENT to R.string.filter_documents,
        FILTER_ARCHIVE to R.string.filter_archives,
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options) { (value, label) ->
            val active = value == selected
            Surface(
                shape = CircleShape,
                color = if (active) Hyper.AccentSoft else Hyper.Chip,
                border = BorderStroke(
                    0.8.dp,
                    if (active) Hyper.AccentEdge else Hyper.Hairline,
                ),
                modifier = Modifier.clip(CircleShape).combinedClickable(
                    onClick = { onSelect(value) },
                ),
            ) {
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) Hyper.Accent else Hyper.Text2,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun OfflineBanner(text: String, hint: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Hyper.Card,
        border = BorderStroke(0.8.dp, Hyper.Hairline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.WifiOff,
                contentDescription = null,
                tint = Hyper.Warn,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(text, style = MaterialTheme.typography.labelLarge, color = Hyper.Text)
                Text(hint, style = MaterialTheme.typography.bodySmall, color = Hyper.Text3)
            }
        }
    }
}

/* --------------------------------- lists ---------------------------------- */

@Composable
private fun FileList(
    items: List<GItem>,
    selectedIds: Set<String>,
    selectionMode: Boolean,
    favorites: Set<String>,
    hasNextPage: Boolean,
    loadingMore: Boolean,
    query: String,
    onOpen: (GItem) -> Unit,
    onLongPress: (GItem) -> Unit,
    onMenu: (GItem) -> Unit,
    onSwipeShare: (GItem) -> Unit,
    onSwipeDelete: (GItem) -> Unit,
    onLoadMore: () -> Unit,
    density: Density,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember(hasNextPage, items.size) {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            hasNextPage && last >= items.size - 4
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = { it.id }) { item ->
            Box(Modifier.animateItem()) {
                SwipeableRow(
                    key = item.id,
                    enabled = !selectionMode,
                    onShare = { onSwipeShare(item) },
                    onDelete = { onSwipeDelete(item) },
                ) {
                FileRow(
                    density = density,
                    item = item,
                    selected = item.id in selectedIds,
                    selectionMode = selectionMode,
                    favorite = item.id in favorites,
                    query = query,
                    onOpen = { onOpen(item) },
                    onLongPress = { onLongPress(item) },
                    onMenu = { onMenu(item) },
                )
                }
            }
        }
        if (loadingMore) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExpressiveLoader(size = 22.dp, strokeOnly = true)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.folder_loading_more),
                        style = MaterialTheme.typography.labelMedium,
                        color = Hyper.Text3,
                    )
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    density: Density,
    item: GItem,
    selected: Boolean,
    selectionMode: Boolean,
    favorite: Boolean,
    query: String,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onMenu: () -> Unit,
) {
    val kind = item.fileKind()
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (selected) Hyper.AccentSoft else Hyper.Card,
        border = BorderStroke(
            if (selected) 1.dp else 0.8.dp,
            if (selected) Hyper.AccentEdge else Hyper.Hairline,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(
                    min = when (density) {
                        Density.COMPACT -> 48.dp
                        Density.COMFORT -> 56.dp
                        Density.RELAXED -> 64.dp
                    },
                )
                .padding(
                    horizontal = 14.dp,
                    vertical = when (density) {
                        Density.COMPACT -> 6.dp
                        Density.COMFORT -> 10.dp
                        Density.RELAXED -> 14.dp
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconChip(
                icon = kind.icon,
                tint = if (item.isFolder) Hyper.Accent else Hyper.Text2,
            )
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                HighlightedText(text = item.name, query = query, selected = selected)
                Spacer(Modifier.height(1.dp))
                Text(
                    text = if (item.isFolder) {
                        stringResource(R.string.details_folder)
                    } else {
                        "${kind.name.lowercase(Locale.ROOT)} · ${readableSize(item.size)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Hyper.Text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (favorite) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = stringResource(R.string.library_favorites),
                    tint = Hyper.Accent,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            if (selectionMode) {
                Icon(
                    imageVector = if (selected) Icons.Filled.Check else Icons.Filled.Add,
                    contentDescription = stringResource(R.string.folder_selected_count, 1),
                    tint = if (selected) Hyper.Accent else Hyper.Text3,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                HyperIconButton(
                    icon = Icons.Filled.Info,
                    description = stringResource(R.string.action_details),
                    onClick = onMenu,
                )
            }
        }
    }
}

@Composable
private fun HighlightedText(text: String, query: String, selected: Boolean) {
    val lowered = query.trim().lowercase(Locale.ROOT)
    val annotated = if (lowered.isBlank()) {
        AnnotatedString(text)
    } else {
        buildAnnotatedString {
            var start = 0
            val source = text.lowercase(Locale.ROOT)
            while (true) {
                val index = source.indexOf(lowered, start)
                if (index < 0) {
                    append(text.substring(start))
                    break
                }
                append(text.substring(start, index))
                withStyle(SpanStyle(color = Hyper.Accent, fontWeight = FontWeight.SemiBold)) {
                    append(text.substring(index, index + lowered.length))
                }
                start = index + lowered.length
            }
        }
    }
    Text(
        annotated,
        style = MaterialTheme.typography.bodyLarge,
        color = if (selected) Hyper.Accent else Hyper.Text,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun FileGrid(
    items: List<GItem>,
    selectedIds: Set<String>,
    selectionMode: Boolean,
    favorites: Set<String>,
    hasNextPage: Boolean,
    loadingMore: Boolean,
    onOpen: (GItem) -> Unit,
    onLongPress: (GItem) -> Unit,
    onMenu: (GItem) -> Unit,
    onLoadMore: () -> Unit,
) {
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val shouldLoadMore by remember(hasNextPage, items.size) {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            hasNextPage && last >= items.size - 6
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(112.dp),
        state = gridState,
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 130.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        gridItems(items, key = { it.id }) { item ->
            GridCard(
                modifier = Modifier.animateItem(),
                item = item,
                selected = item.id in selectedIds,
                selectionMode = selectionMode,
                favorite = item.id in favorites,
                onOpen = { onOpen(item) },
                onLongPress = { onLongPress(item) },
                onMenu = { onMenu(item) },
            )
        }
        if (loadingMore) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ExpressiveLoader(size = 22.dp, strokeOnly = true)
                }
            }
        }
    }
}

@Composable
private fun GridCard(
    modifier: Modifier = Modifier,
    item: GItem,
    selected: Boolean,
    selectionMode: Boolean,
    favorite: Boolean,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onMenu: () -> Unit,
) {
    val context = LocalContext.current
    val kind = item.fileKind()
    val imageRequest = remember(item.id, item.link) {
        ImageRequest.Builder(context)
            .data(item.link)
            .apply { if (kind == FileKind.VIDEO) videoFrameMillis(1000) }
            .crossfade(true)
            .build()
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Hyper.Card,
        border = BorderStroke(
            if (selected) 1.dp else 0.8.dp,
            if (selected) Hyper.Accent else Hyper.Hairline,
        ),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.35f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Hyper.CardRaised),
                contentAlignment = Alignment.Center,
            ) {
                if (item.isFolder) {
                    Icon(
                        Icons.Filled.Folder,
                        contentDescription = null,
                        tint = Hyper.Accent,
                        modifier = Modifier.size(34.dp),
                    )
                } else if (kind == FileKind.IMAGE || kind == FileKind.VIDEO) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        kind.icon,
                        contentDescription = null,
                        tint = Hyper.Text2,
                        modifier = Modifier.size(30.dp),
                    )
                }
                if (favorite) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = stringResource(R.string.library_favorites),
                        tint = Hyper.Accent,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(15.dp),
                    )
                }
                if (selectionMode) {
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (selected) Hyper.Accent else Hyper.Card)
                            .border(0.8.dp, Hyper.Hairline, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = Hyper.AccentInk,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Hyper.Text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (item.isFolder) stringResource(R.string.details_folder) else readableSize(item.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = Hyper.Text3,
                )
            }
        }
    }
}

/* ------------------------------- komponen --------------------------------- */

@Composable
private fun SwipeableRow(
    key: String,
    enabled: Boolean,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    var offset by remember(key) { mutableFloatStateOf(0f) }
    val animated by animateFloatAsState(offset, label = "swipe")
    val density = LocalDensity.current
    val threshold = with(density) { SWIPE_THRESHOLD.dp.toPx() }
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .matchParentSize()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Share,
                contentDescription = stringResource(R.string.action_share),
                tint = Hyper.Text3.copy(alpha = if (animated > 8f) 1f else 0f),
                modifier = Modifier.size(18.dp),
            )
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.action_delete),
                tint = Hyper.Accent.copy(alpha = if (animated < -8f) 1f else 0f),
                modifier = Modifier.size(18.dp),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = animated }
                .then(
                    if (enabled) {
                        Modifier.pointerInput(key) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, drag ->
                                    offset = (offset + drag)
                                        .coerceIn(-threshold * 1.6f, threshold * 1.6f)
                                },
                                onDragEnd = {
                                    when {
                                        offset > threshold -> onShare()
                                        offset < -threshold -> onDelete()
                                    }
                                    offset = 0f
                                },
                                onDragCancel = { offset = 0f },
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            content()
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onClear: () -> Unit,
    onSelectAll: () -> Unit,
    onInvert: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Hyper.GlassFill,
        border = BorderStroke(0.8.dp, Hyper.GlassEdge),
        shadowElevation = 10.dp,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HyperIconButton(
                    icon = Icons.Filled.Close,
                    description = stringResource(R.string.folder_selection_clear),
                    onClick = onClear,
                )
                Text(
                    stringResource(R.string.folder_selected_count, count),
                    style = MaterialTheme.typography.labelLarge,
                    color = Hyper.Text,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onSelectAll) {
                    Text(
                        stringResource(R.string.folder_select_all),
                        color = Hyper.Text2,
                        fontSize = 12.sp,
                    )
                }
                TextButton(onClick = onInvert) {
                    Text(
                        stringResource(R.string.folder_invert_selection),
                        color = Hyper.Text2,
                        fontSize = 12.sp,
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BarAction(Icons.Filled.Download, stringResource(R.string.action_download), onDownload)
                BarAction(Icons.Filled.Share, stringResource(R.string.action_share), onShare)
                BarAction(Icons.Filled.FolderOpen, stringResource(R.string.action_move), onMove)
                BarAction(Icons.Filled.ContentCopy, stringResource(R.string.action_copy), onCopy)
                BarAction(Icons.Filled.Delete, stringResource(R.string.action_delete), onDelete)
            }
        }
    }
}

@Composable
private fun BarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(icon, contentDescription = label, tint = Hyper.Text, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Hyper.Text2, fontSize = 10.sp)
    }
}

@Composable
private fun MiniAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = Hyper.Card,
        border = BorderStroke(0.8.dp, Hyper.Hairline),
        modifier = Modifier.clip(CircleShape).combinedClickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = Hyper.Accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = Hyper.Text)
        }
    }
}

@Composable
private fun UndoBar(count: Int, onUndo: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = Hyper.Card,
        border = BorderStroke(0.8.dp, Hyper.AccentEdge),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.folder_undo_available, count),
                style = MaterialTheme.typography.bodySmall,
                color = Hyper.Text,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onUndo) {
                Text(
                    stringResource(R.string.action_undo),
                    color = Hyper.Accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun SkeletonList(reduceMotion: Boolean) {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(900), repeatMode = RepeatMode.Reverse),
        label = "pulse",
    )
    val alpha = if (reduceMotion) 0.6f else pulse
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(8) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Hyper.Card,
                border = BorderStroke(0.8.dp, Hyper.Hairline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .alpha(alpha),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Hyper.CardRaised),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.55f)
                                .height(11.dp)
                                .clip(CircleShape)
                                .background(Hyper.CardRaised),
                        )
                        Spacer(Modifier.height(7.dp))
                        Box(
                            Modifier
                                .fillMaxWidth(0.3f)
                                .height(9.dp)
                                .clip(CircleShape)
                                .background(Hyper.CardRaised),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderEmpty(
    searching: Boolean,
    query: String,
    error: String?,
    onRetry: () -> Unit,
    onUpload: () -> Unit,
) {
    when {
        error != null -> EmptyState(
            icon = Icons.Filled.WifiOff,
            title = stringResource(R.string.folder_error_title),
            body = error,
            actionLabel = stringResource(R.string.action_retry),
            onAction = onRetry,
            modifier = Modifier.padding(top = 48.dp),
        )

        searching -> EmptyState(
            icon = Icons.Filled.Search,
            title = stringResource(R.string.folder_search_empty_title, query.ifBlank { "…" }),
            body = stringResource(R.string.folder_search_empty_body),
            modifier = Modifier.padding(top = 48.dp),
        )

        else -> EmptyState(
            icon = Icons.Filled.Folder,
            title = stringResource(R.string.folder_empty_title),
            body = stringResource(R.string.folder_empty_body),
            actionLabel = stringResource(R.string.action_upload),
            onAction = onUpload,
            modifier = Modifier.padding(top = 48.dp),
        )
    }
}

/* --------------------------------- detail --------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDetailsSheet(
    item: GItem,
    favorite: Boolean,
    onDismiss: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    onPreview: () -> Unit,
    onContentSettings: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Hyper.Card,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    item.fileKind().icon,
                    contentDescription = null,
                    tint = if (item.isFolder) Hyper.Accent else Hyper.Text2,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Hyper.Text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (item.isFolder) stringResource(R.string.details_folder)
                        else stringResource(R.string.details_file),
                        style = MaterialTheme.typography.labelSmall,
                        color = Hyper.Text3,
                    )
                }
                HyperIconButton(
                    icon = if (favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    description = stringResource(
                        if (favorite) R.string.folder_favorite_remove else R.string.folder_favorite_add,
                    ),
                    onClick = onFavorite,
                )
            }

            Spacer(Modifier.height(16.dp))
            DetailRow(stringResource(R.string.details_size), readableSize(item.size))
            DetailRow(
                stringResource(R.string.details_type),
                item.mimetype.ifBlank { item.fileKind().name.lowercase(Locale.ROOT) },
            )
            DetailRow(stringResource(R.string.details_id), item.id)
            if (item.createTime > 0) {
                DetailRow(stringResource(R.string.details_created), formatTime(item.createTime))
            }
            if (item.modTime > 0) {
                DetailRow(stringResource(R.string.details_modified), formatTime(item.modTime))
            }
            DetailRow(stringResource(R.string.details_downloads), item.downloadCount.toString())
            if (item.md5.isNotBlank()) {
                DetailRow(stringResource(R.string.details_md5), item.md5)
            }
            if (item.isFolder) {
                DetailRow(stringResource(R.string.details_items), item.childrenCount.toString())
            }
            if (item.link.isNotBlank()) {
                DetailRow(stringResource(R.string.details_link), item.link, mono = true)
            }

            Spacer(Modifier.height(18.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                if (!item.isFolder) {
                    HyperIconButton(Icons.Filled.Visibility, stringResource(R.string.preview_title_short), onPreview)
                }
                HyperIconButton(Icons.Filled.Download, stringResource(R.string.action_download), onDownload)
                HyperIconButton(Icons.Filled.Share, stringResource(R.string.action_share), onShare)
                HyperIconButton(Icons.Filled.Tune, stringResource(R.string.share_settings_title), onContentSettings)
                HyperIconButton(Icons.Filled.ContentCopy, stringResource(R.string.details_copy_link), onCopyLink)
                HyperIconButton(Icons.Filled.CreateNewFolder, stringResource(R.string.action_rename), onRename)
                HyperIconButton(Icons.Filled.FolderOpen, stringResource(R.string.action_move), onMove)
                HyperIconButton(Icons.Filled.ContentPaste, stringResource(R.string.action_copy), onCopy)
                HyperIconButton(Icons.Filled.Delete, stringResource(R.string.action_delete), onDelete)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, mono: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = Hyper.Text3,
            modifier = Modifier.width(92.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = Hyper.Text2,
            fontFamily = if (mono) FontFamily.Monospace else null,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun formatTime(epochSeconds: Long): String {
    val millis = if (epochSeconds < 1_000_000_000_000L) epochSeconds * 1000 else epochSeconds
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(millis))
}

/* --------------------------------- picker --------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerSheet(
    picker: PickerState,
    mode: PickerMode,
    onDismiss: () -> Unit,
    onEnter: (GItem) -> Unit,
    onUp: () -> Unit,
    onNewFolder: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newFolder by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Hyper.Card,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 6.dp),
        ) {
            Text(
                stringResource(
                    if (mode == PickerMode.MOVE) R.string.picker_move_title else R.string.picker_copy_title,
                ),
                style = MaterialTheme.typography.titleLarge,
                color = Hyper.Text,
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(picker.crumbs) { crumb ->
                    Text(
                        crumb.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = Hyper.Text2,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Hyper.Chip)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (picker.crumbs.size > 1) {
                    HyperIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        description = stringResource(R.string.root),
                        onClick = onUp,
                    )
                }
                TextButton(onClick = { newFolder = true }) {
                    Icon(
                        Icons.Filled.CreateNewFolder,
                        contentDescription = null,
                        tint = Hyper.Accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.picker_new_folder), color = Hyper.Accent)
                }
                Spacer(Modifier.weight(1f))
                if (picker.busy) ExpressiveLoader(size = 20.dp, strokeOnly = true)
            }
            if (picker.folders.isEmpty()) {
                Text(
                    stringResource(R.string.picker_no_subfolders),
                    style = MaterialTheme.typography.bodySmall,
                    color = Hyper.Text3,
                    modifier = Modifier.padding(vertical = 18.dp),
                )
            } else {
                Column(
                    Modifier
                        .height(280.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    picker.folders.forEach { folder ->
                        HyperRow(
                            title = folder.name,
                            subtitle = stringResource(R.string.details_folder),
                            icon = Icons.Filled.Folder,
                            iconTint = Hyper.Accent,
                            onClick = { onEnter(folder) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = CircleShape,
                color = Hyper.Accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .combinedClickable(onClick = onConfirm),
            ) {
                Text(
                    stringResource(
                        if (mode == PickerMode.MOVE) R.string.picker_here else R.string.picker_here_copy,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = Hyper.AccentInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 14.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (newFolder) {
        AlertDialog(
            onDismissRequest = { newFolder = false },
            containerColor = Hyper.Card,
            shape = RoundedCornerShape(24.dp),
            title = { Text(stringResource(R.string.folder_new_folder), color = Hyper.Text) },
            text = {
                HyperTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.folder_name_label),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onNewFolder(name)
                        newFolder = false
                        name = ""
                    },
                    enabled = name.isNotBlank(),
                ) { Text(stringResource(R.string.action_create), color = Hyper.Accent) }
            },
            dismissButton = {
                TextButton(onClick = { newFolder = false }) {
                    Text(stringResource(R.string.action_cancel), color = Hyper.Text2)
                }
            },
        )
    }
}
