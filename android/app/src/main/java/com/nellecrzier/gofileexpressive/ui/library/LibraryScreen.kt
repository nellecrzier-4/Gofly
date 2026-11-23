@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.nellecrzier.gofileexpressive.ui.library

import com.nellecrzier.gofileexpressive.ui.components.AppLoadingIndicator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nellecrzier.gofileexpressive.GItem
import com.nellecrzier.gofileexpressive.MainActivity
import com.nellecrzier.gofileexpressive.PrefsState
import com.nellecrzier.gofileexpressive.R
import com.nellecrzier.gofileexpressive.ui.components.animatedConnectedShape
import com.nellecrzier.gofileexpressive.ui.components.fileKind
import com.nellecrzier.gofileexpressive.ui.motion.motionScheme
import com.nellecrzier.gofileexpressive.ui.motion.pressScale
import com.nellecrzier.gofileexpressive.ui.motion.staggeredEnter
import com.nellecrzier.gofileexpressive.ui.readableSize
import com.nellecrzier.gofileexpressive.vm.FolderViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pustaka lokal: favorit, file terbaru, dan pencarian lintas folder yang
 * sudah di-cache. GoFile tidak menyediakan pencarian global, jadi cakupannya
 * dijelaskan langsung di layar.
 *
 * Kolom pencarian memakai `SearchBar` Material 3: saat disentuh ia mengembang
 * menjadi permukaan pencarian penuh layar (`ExpandedFullScreenSearchBar`)
 * dengan hasil yang sama seperti daftar di bawahnya. `query` tetap menjadi
 * satu-satunya sumber untuk `vm.searchGlobal`, sehingga perilaku pencarian
 * tidak berubah.
 */
@Composable
fun LibraryScreen(
    vm: FolderViewModel,
    prefs: PrefsState,
    token: String,
    reduceMotion: Boolean,
    activity: MainActivity,
    onOpenFolder: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    // Masuk bertahap hanya pada komposisi pertama; bergulir tidak mengulanginya.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    LaunchedEffect(query) {
        delay(180)
        vm.searchGlobal(query)
    }

    // Isi kolom pencarian dipegang TextFieldState (komponen M3), sementara
    // `query` tetap sumber tunggal untuk pencarian global. Arah field → query.
    val textFieldState = rememberTextFieldState(query)
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }.collect { text ->
            if (text != query) query = text
        }
    }

    // Saat gerak diredupkan, transformasi wadah dilewati sepenuhnya (snap):
    // spesifikasi diingat agar SearchBarState tidak dibuat ulang tiap recomposisi.
    val scheme = motionScheme
    val expandSpec = remember(reduceMotion, scheme) {
        if (reduceMotion) tween<Float>(0) else scheme.slowSpatialSpec()
    }
    val collapseSpec = remember(reduceMotion, scheme) {
        if (reduceMotion) tween<Float>(0) else scheme.defaultSpatialSpec()
    }
    val searchBarState = rememberSearchBarState(
        animationSpecForExpand = expandSpec,
        animationSpecForCollapse = collapseSpec,
    )
    val searchExpanded = searchBarState.currentValue == SearchBarValue.Expanded
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Buka item hasil pencarian; `query` dikosongkan seperti sebelumnya.
    val openSearchResult: (GItem) -> Unit = { item ->
        if (item.isFolder) {
            vm.openDirect(token, item)
            query = ""
            textFieldState.clearText()
            scope.launch { searchBarState.animateToCollapsed() }
            onOpenFolder()
        } else if (item.link.startsWith("http")) {
            activity.startDownload(item.link, item.name, token)
            vm.recordRecent(item)
        }
    }

    // Satu input field dipakai versi ringkas maupun versi penuh layar agar
    // fokus dan teks tidak berpindah tangan saat mengembang.
    val searchInputField: @Composable () -> Unit = {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = { focusManager.clearFocus() },
            placeholder = { Text(stringResource(R.string.library_search_hint)) },
            leadingIcon = {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.globalSearching) {
                        AppLoadingIndicator(size = 18.dp, reduceMotion = reduceMotion)
                    }
                    if (searchExpanded) {
                        FilledTonalIconButton(
                            onClick = { scope.launch { searchBarState.animateToCollapsed() } },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.folder_search_close),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item {
                Column(Modifier.padding(bottom = 6.dp)) {
                    Text(
                        stringResource(R.string.library_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stringResource(R.string.library_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SearchBar(
                    state = searchBarState,
                    inputField = searchInputField,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                )
            }

            item {
                Text(
                    stringResource(R.string.library_search_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
                )
            }

            if (query.isNotBlank()) {
                item { LibrarySectionHeader(stringResource(R.string.library_search_hint)) }
                if (state.globalResults.isEmpty() && !state.globalSearching) {
                    item {
                        LibraryEmptyMessage(
                            icon = Icons.Filled.Search,
                            title = stringResource(R.string.library_search_empty, query),
                            body = stringResource(R.string.folder_search_empty_body),
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                } else {
                    itemsIndexed(state.globalResults, key = { _, item -> "g-${item.id}" }) { index, item ->
                    val rowShape = animatedConnectedShape(index, state.globalResults.size, reduceMotion)
                    Box(
                        Modifier
                            .animateItem()
                            .staggeredEnter(index, reduceMotion, entered || index >= 10),
                    ) {
                        LibraryRow(
                            item = item,
                            favorite = prefs.favorites.any { it.id == item.id },
                            onOpen = { openSearchResult(item) },
                            onFavorite = { vm.toggleFavorite(item) },
                            shape = rowShape,
                            reduceMotion = reduceMotion,
                        )
                        }
                    }
                }
                return@LazyColumn
            }

            item { LibrarySectionHeader(stringResource(R.string.library_favorites)) }
            if (prefs.favorites.isEmpty()) {
                item {
                    LibraryEmptyMessage(
                        icon = Icons.Filled.StarBorder,
                        title = stringResource(R.string.library_no_favorites),
                        body = stringResource(R.string.library_no_favorites_body),
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                itemsIndexed(prefs.favorites, key = { _, item -> "f-${item.id}" }) { index, item ->
                    val rowShape = animatedConnectedShape(index, prefs.favorites.size, reduceMotion)
                    Box(
                        Modifier
                            .animateItem()
                            .staggeredEnter(index, reduceMotion, entered || index >= 10),
                    ) {
                        LibraryRow(
                            item = item,
                            favorite = true,
                            onOpen = {
                                if (item.isFolder) {
                                    vm.openDirect(token, item)
                                    onOpenFolder()
                                } else if (item.link.startsWith("http")) {
                                    activity.startDownload(item.link, item.name, token)
                                    vm.recordRecent(item)
                                }
                            },
                            onFavorite = { vm.toggleFavorite(item) },
                            shape = rowShape,
                            reduceMotion = reduceMotion,
                        )
                    }
                }
            }

            item { LibrarySectionHeader(stringResource(R.string.library_recent)) }
            if (prefs.recent.isEmpty()) {
                item {
                    LibraryEmptyMessage(
                        icon = Icons.Filled.Folder,
                        title = stringResource(R.string.library_no_recent),
                        body = stringResource(R.string.library_no_recent_body),
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                itemsIndexed(prefs.recent, key = { _, item -> "r-${item.id}" }) { index, item ->
                    val rowShape = animatedConnectedShape(index, prefs.recent.size, reduceMotion)
                    Box(
                        Modifier
                            .animateItem()
                            .staggeredEnter(index, reduceMotion, entered || index >= 10),
                    ) {
                        LibraryRow(
                            item = item,
                            favorite = prefs.favorites.any { it.id == item.id },
                            onOpen = {
                                if (item.link.startsWith("http")) {
                                    activity.startDownload(item.link, item.name, token)
                                    vm.recordRecent(item)
                                }
                            },
                            onFavorite = { vm.toggleFavorite(item) },
                            shape = rowShape,
                            reduceMotion = reduceMotion,
                        )
                    }
                }
                item {
                    TextButton(
                        onClick = { vm.clearLibrary() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.library_clear),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }

        // Permukaan pencarian penuh layar: hanya hadir saat kolom mengembang.
        // Kembali (sistem) dan tombol tutup sama-sama melipat lewat state.
        ExpandedFullScreenSearchBar(
            state = searchBarState,
            inputField = searchInputField,
            modifier = Modifier.fillMaxSize(),
        ) {
            // Isi permukaan hanya memudar (tanpa geser/skala), dan saat gerak
            // diredupkan cukup fade ≤100 ms.
            var contentVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { contentVisible = true }
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(
                    if (reduceMotion) tween<Float>(100) else motionScheme.fastEffectsSpec(),
                ),
                exit = fadeOut(
                    if (reduceMotion) tween<Float>(80) else motionScheme.fastEffectsSpec(),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (state.globalSearching) {
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                AppLoadingIndicator(size = 28.dp, reduceMotion = reduceMotion)
                            }
                        }
                    }
                    if (query.isBlank()) {
                        item {
                            Text(
                                stringResource(R.string.library_search_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
                            )
                        }
                    } else {
                        item { LibrarySectionHeader(stringResource(R.string.library_search_hint)) }
                        if (state.globalResults.isEmpty() && !state.globalSearching) {
                            item {
                                LibraryEmptyMessage(
                                    icon = Icons.Filled.Search,
                                    title = stringResource(R.string.library_search_empty, query),
                                    body = stringResource(R.string.folder_search_empty_body),
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                            }
                        } else {
                            itemsIndexed(
                                state.globalResults,
                                key = { _, item -> "s-${item.id}" },
                            ) { index, item ->
                                Box(Modifier.animateItem()) {
                                    LibraryRow(
                                        item = item,
                                        favorite = prefs.favorites.any { it.id == item.id },
                                        onOpen = { openSearchResult(item) },
                                        onFavorite = { vm.toggleFavorite(item) },
                                        shape = animatedConnectedShape(
                                            index,
                                            state.globalResults.size,
                                            reduceMotion,
                                        ),
                                        reduceMotion = reduceMotion,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Judul bagian Material 3: huruf kapital, tanpa garis aksen. */
@Composable
private fun LibrarySectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
    )
}

/** Keadaan kosong Material 3: ikon dalam lingkaran tonal, judul, lalu keterangan. */
@Composable
private fun LibraryEmptyMessage(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(76.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LibraryRow(
    item: GItem,
    favorite: Boolean,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    shape: Shape = RoundedCornerShape(18.dp),
    reduceMotion: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .pressScale(interactionSource, reduceMotion)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onOpen,
            ),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(32.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (item.isFolder) Icons.Filled.Folder else item.fileKind().icon,
                        contentDescription = null,
                        tint = if (item.isFolder) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (item.isFolder) {
                        stringResource(R.string.details_folder)
                    } else {
                        readableSize(item.size)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalIconButton(
                onClick = onFavorite,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = if (favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = stringResource(
                        if (favorite) R.string.folder_favorite_remove else R.string.folder_favorite_add,
                    ),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
