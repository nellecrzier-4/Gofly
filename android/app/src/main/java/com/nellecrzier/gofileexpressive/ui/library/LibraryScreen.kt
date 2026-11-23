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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nellecrzier.gofileexpressive.GItem
import com.nellecrzier.gofileexpressive.MainActivity
import com.nellecrzier.gofileexpressive.PrefsState
import com.nellecrzier.gofileexpressive.R
import com.nellecrzier.gofileexpressive.ui.ExpressiveLoader
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
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.library_search_hint)) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (state.globalSearching) {
                        ExpressiveLoader(size = 18.dp, strokeOnly = true)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Text(
                stringResource(R.string.library_search_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 4.dp),
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
                items(state.globalResults, key = { "g-${it.id}" }) { item ->
                Box(Modifier.animateItem()) {
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
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onOpen),
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
