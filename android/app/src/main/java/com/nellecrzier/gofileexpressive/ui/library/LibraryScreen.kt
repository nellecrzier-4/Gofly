@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.nellecrzier.gofileexpressive.ui.library

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nellecrzier.gofileexpressive.GItem
import com.nellecrzier.gofileexpressive.MainActivity
import com.nellecrzier.gofileexpressive.PrefsState
import com.nellecrzier.gofileexpressive.R
import com.nellecrzier.gofileexpressive.ui.EmptyState
import com.nellecrzier.gofileexpressive.ui.ExpressiveLoader
import com.nellecrzier.gofileexpressive.ui.Hyper
import com.nellecrzier.gofileexpressive.ui.HyperIconButton
import com.nellecrzier.gofileexpressive.ui.HyperTextField
import com.nellecrzier.gofileexpressive.ui.IconChip
import com.nellecrzier.gofileexpressive.ui.SectionLabel
import com.nellecrzier.gofileexpressive.ui.components.fileKind
import com.nellecrzier.gofileexpressive.ui.readableSize
import com.nellecrzier.gofileexpressive.vm.FolderViewModel
import kotlinx.coroutines.delay

/**
 * Pustaka lokal: favorit, file terbaru, dan pencarian lintas folder yang
 * sudah di-cache. GoFile tidak menyediakan pencarian global, jadi cakupannya
 * dijelaskan langsung di layar.
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

    LaunchedEffect(query) {
        delay(180)
        vm.searchGlobal(query)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column {
                Text(
                    stringResource(R.string.library_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Hyper.Text,
                )
                Text(
                    stringResource(R.string.library_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Hyper.Text2,
                )
            }
        }

        item {
            HyperTextField(
                value = query,
                onValueChange = { query = it },
                label = stringResource(R.string.library_search_hint),
                leadingIcon = Icons.Filled.Search,
                modifier = Modifier.fillMaxWidth(),
                trailing = {
                    if (state.globalSearching) {
                        ExpressiveLoader(size = 18.dp, strokeOnly = true)
                    }
                },
            )
        }

        item {
            Text(
                stringResource(R.string.library_search_note),
                style = MaterialTheme.typography.labelSmall,
                color = Hyper.Text3,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        if (query.isNotBlank()) {
            item { SectionLabel(stringResource(R.string.library_search_hint)) }
            if (state.globalResults.isEmpty() && !state.globalSearching) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = stringResource(R.string.library_search_empty, query),
                        body = stringResource(R.string.folder_search_empty_body),
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            } else {
                items(state.globalResults, key = { "g-${it.id}" }) { item ->
                    LibraryRow(
                        item = item,
                        favorite = prefs.favorites.any { it.id == item.id },
                        onOpen = {
                            if (item.isFolder) {
                                vm.openDirect(token, item)
                                query = ""
                                onOpenFolder()
                            } else if (item.link.startsWith("http")) {
                                activity.startDownload(item.link, item.name, token)
                                vm.recordRecent(item)
                            }
                        },
                        onFavorite = { vm.toggleFavorite(item) },
                    )
                }
            }
            return@LazyColumn
        }

        item { SectionLabel(stringResource(R.string.library_favorites)) }
        if (prefs.favorites.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.StarBorder,
                    title = stringResource(R.string.library_no_favorites),
                    body = stringResource(R.string.library_no_favorites_body),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            items(prefs.favorites, key = { "f-${it.id}" }) { item ->
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
                )
            }
        }

        item { SectionLabel(stringResource(R.string.library_recent)) }
        if (prefs.recent.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.Folder,
                    title = stringResource(R.string.library_no_recent),
                    body = stringResource(R.string.library_no_recent_body),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            items(prefs.recent, key = { "r-${it.id}" }) { item ->
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
                )
            }
            item {
                TextButton(
                    onClick = { vm.clearLibrary() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = Hyper.Text3,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.library_clear), color = Hyper.Text3)
                }
            }
        }
    }
}

@Composable
private fun LibraryRow(
    item: GItem,
    favorite: Boolean,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Hyper.Card,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Hyper.Hairline),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onOpen),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconChip(
                icon = if (item.isFolder) Icons.Filled.Folder else item.fileKind().icon,
                tint = if (item.isFolder) Hyper.Accent else Hyper.Text2,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Hyper.Text,
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
                    color = Hyper.Text2,
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
    }
}
