package com.nellecrzier.gofileexpressive.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nellecrzier.gofileexpressive.AnimPref
import com.nellecrzier.gofileexpressive.BuildConfig
import com.nellecrzier.gofileexpressive.DeleteConfirm
import com.nellecrzier.gofileexpressive.Density
import com.nellecrzier.gofileexpressive.LanguageChoice
import com.nellecrzier.gofileexpressive.PrefsState
import com.nellecrzier.gofileexpressive.R
import com.nellecrzier.gofileexpressive.ThemeMode
import com.nellecrzier.gofileexpressive.ViewMode
import com.nellecrzier.gofileexpressive.ui.components.HoldToConfirmButton
import com.nellecrzier.gofileexpressive.ui.readableSize
import com.nellecrzier.gofileexpressive.vm.SettingsViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Judul + penjelasan + kontrol, dipakai untuk segmen dan penggeser. */
@Composable
internal fun SettingsBlock(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

/** Judul bagian Material 3: kapital, tanpa garis aksen. */
@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
    )
}

/** Wadah grup setelan: kartu Material 3 datar tanpa efek kaca. */
@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(Modifier.padding(vertical = 2.dp), content = content)
    }
}

/** Nilai ringkas di kanan baris. */
@Composable
private fun SettingsValuePill(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
        )
    }
}

/** Baris setelan berbasis [ListItem]: ikon, judul, subjudul, nilai, aksi kanan. */
@Composable
private fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        leadingContent = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) SettingsValuePill(value)
                trailing?.invoke()
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
    )
}

@Composable
private fun themeOptions(): List<Pair<String, String>> = listOf(
    ThemeMode.SYSTEM.name to stringResource(R.string.settings_theme_system),
    ThemeMode.DARK.name to stringResource(R.string.settings_theme_dark),
    ThemeMode.LIGHT.name to stringResource(R.string.settings_theme_light),
)

@Composable
private fun densityOptions(): List<Pair<String, String>> = listOf(
    Density.COMPACT.name to stringResource(R.string.settings_density_compact),
    Density.COMFORT.name to stringResource(R.string.settings_density_comfort),
    Density.RELAXED.name to stringResource(R.string.settings_density_relaxed),
)

@Composable
private fun languageOptions(): List<Pair<String, String>> = listOf(
    LanguageChoice.SYSTEM.name to stringResource(R.string.settings_language_system),
    LanguageChoice.EN.name to stringResource(R.string.settings_language_en),
    LanguageChoice.ID.name to stringResource(R.string.settings_language_id),
)

@Composable
private fun animOptions(): List<Pair<String, String>> = listOf(
    AnimPref.SYSTEM.name to stringResource(R.string.settings_motion_system),
    AnimPref.REDUCED.name to stringResource(R.string.settings_motion_reduced),
    AnimPref.OFF.name to stringResource(R.string.settings_motion_off),
)

@Composable
private fun viewOptions(): List<Pair<String, String>> = listOf(
    ViewMode.LIST.name to stringResource(R.string.settings_view_list),
    ViewMode.GRID.name to stringResource(R.string.settings_view_grid),
)

/** `modified` adalah kunci yang benar-benar dipakai daftar berkas. */
@Composable
private fun sortOptions(): List<Pair<String, String>> = listOf(
    "name" to stringResource(R.string.settings_sort_name),
    "size" to stringResource(R.string.settings_sort_size),
    "modified" to stringResource(R.string.settings_sort_date),
    "type" to stringResource(R.string.settings_sort_type),
)

@Composable
private fun directionOptions(): List<Pair<String, String>> = listOf(
    "asc" to stringResource(R.string.settings_sort_ascending),
    "desc" to stringResource(R.string.settings_sort_descending),
)

@Composable
private fun deleteOptions(): List<Pair<String, String>> = listOf(
    DeleteConfirm.HOLD.name to stringResource(R.string.settings_delete_hold),
    DeleteConfirm.DIALOG.name to stringResource(R.string.settings_delete_dialog),
)

@Composable
private fun parallelOptions(): List<Pair<String, String>> = (1..4).map {
    it.toString() to stringResource(R.string.settings_parallel_value, it)
}

/* ------------------------------ tampilan ----------------------------- */

@Composable
internal fun SettingsAppearanceSection(
    prefs: PrefsState,
    vm: SettingsViewModel,
    reduceMotion: Boolean,
) {
    SettingsSectionHeader(stringResource(R.string.settings_section_appearance))
    SettingsGroup {
        SettingsBlock(
            title = stringResource(R.string.settings_theme),
            subtitle = stringResource(R.string.settings_theme_body),
        ) {
            SettingsSegmented(
                options = themeOptions(),
                value = prefs.themeMode.name,
                onChange = { vm.setThemeMode(ThemeMode.valueOf(it)) },
                reduceMotion = reduceMotion,
            )
        }
        SettingsDivider()
        AccentSwatches(
            value = prefs.accent,
            onChange = { vm.setAccent(it) },
        )
        SettingsDivider()
        DynamicColourRow(prefs = prefs, vm = vm)
        SettingsDivider()
        SettingsBlock(
            title = stringResource(R.string.settings_language),
            subtitle = stringResource(R.string.settings_language_body),
        ) {
            SettingsSegmented(
                options = languageOptions(),
                value = prefs.language.name,
                onChange = { vm.setLanguage(LanguageChoice.valueOf(it)) },
                reduceMotion = reduceMotion,
            )
        }
        SettingsDivider()
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            var draft by remember(prefs.fontScale) { mutableFloatStateOf(prefs.fontScale) }
            val percent = (draft * 100).roundToInt()
            SettingsSlider(
                title = stringResource(R.string.settings_text_size),
                valueLabel = stringResource(R.string.settings_text_size_value, percent),
                value = draft,
                onValueChange = { draft = it },
                onValueChangeFinished = { vm.setFontScale(draft) },
                valueRange = 0.85f..1.3f,
                steps = 44,
            )
            TextSizePreview(
                scale = draft,
                text = stringResource(R.string.settings_text_size_preview),
                modifier = Modifier.padding(horizontal = 14.dp),
            )
        }
        SettingsDivider()
        SettingsBlock(title = stringResource(R.string.settings_density)) {
            SettingsSegmented(
                options = densityOptions(),
                value = prefs.density.name,
                onChange = { vm.setDensity(Density.valueOf(it)) },
                reduceMotion = reduceMotion,
            )
            Spacer(Modifier.height(10.dp))
            DensityPreview(density = prefs.density)
        }
        SettingsDivider()
        SettingsBlock(
            title = stringResource(R.string.settings_motion),
            subtitle = stringResource(R.string.settings_motion_body),
        ) {
            SettingsSegmented(
                options = animOptions(),
                value = prefs.animations.name,
                onChange = { vm.setAnimations(AnimPref.valueOf(it)) },
                reduceMotion = reduceMotion,
            )
        }
    }
}

@Composable
private fun DynamicColourRow(prefs: PrefsState, vm: SettingsViewModel) {
    val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    SettingsSwitchRow(
        title = stringResource(R.string.settings_dynamic),
        subtitle = if (supported) {
            stringResource(R.string.settings_dynamic_body)
        } else {
            stringResource(R.string.settings_dynamic_disabled, Build.VERSION.RELEASE)
        },
        checked = supported && prefs.dynamicColor,
        enabled = supported,
        icon = Icons.Filled.Palette,
        onChange = { vm.setDynamicColor(it) },
        modifier = if (supported) Modifier else Modifier.alpha(0.62f),
    )
}

/* ------------------------------- berkas ------------------------------ */

@Composable
internal fun SettingsFilesSection(
    prefs: PrefsState,
    vm: SettingsViewModel,
    reduceMotion: Boolean,
) {
    SettingsSectionHeader(stringResource(R.string.settings_section_files))
    SettingsGroup {
        SettingsBlock(title = stringResource(R.string.settings_view_mode)) {
            SettingsSegmented(
                options = viewOptions(),
                value = prefs.viewMode.name,
                onChange = { vm.setViewMode(ViewMode.valueOf(it)) },
                reduceMotion = reduceMotion,
            )
        }
        SettingsDivider()
        SettingsBlock(title = stringResource(R.string.settings_sort)) {
            SettingsSegmented(
                options = sortOptions(),
                value = prefs.sortField,
                onChange = { vm.setSort(it, prefs.sortAscending) },
                reduceMotion = reduceMotion,
            )
        }
        SettingsDivider()
        SettingsBlock(title = stringResource(R.string.settings_sort_direction)) {
            SettingsSegmented(
                options = directionOptions(),
                value = if (prefs.sortAscending) "asc" else "desc",
                onChange = { vm.setSort(prefs.sortField, it == "asc") },
                reduceMotion = reduceMotion,
            )
        }
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_show_hidden),
            subtitle = stringResource(R.string.settings_show_hidden_body),
            checked = prefs.showHidden,
            icon = Icons.Filled.Visibility,
            onChange = { vm.setShowHidden(it) },
        )
        SettingsDivider()
        SettingsBlock(
            title = stringResource(R.string.settings_delete),
            subtitle = stringResource(R.string.settings_delete_body),
        ) {
            SettingsSegmented(
                options = deleteOptions(),
                value = prefs.deleteConfirm.name,
                onChange = { vm.setDeleteConfirm(DeleteConfirm.valueOf(it)) },
                reduceMotion = reduceMotion,
            )
        }
        SettingsDivider()
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            var undo by remember(prefs.undoSeconds) { mutableIntStateOf(prefs.undoSeconds) }
            SettingsSlider(
                title = stringResource(R.string.settings_undo),
                subtitle = stringResource(R.string.settings_undo_body),
                valueLabel = stringResource(R.string.settings_undo_value, undo),
                value = undo.toFloat(),
                onValueChange = { undo = it.roundToInt() },
                onValueChangeFinished = { vm.setUndoSeconds(undo) },
                valueRange = 3f..15f,
                steps = 11,
            )
        }
    }
}

/* ------------------------------ jaringan ----------------------------- */

/** "primary:Download/Gofile" → "Gofile"; kosong berarti folder bawaan. */
private fun downloadFolderName(tree: String): String? {
    if (tree.isBlank()) return null
    val segment = runCatching { android.net.Uri.parse(tree).lastPathSegment }.getOrNull() ?: return null
    val path = segment.substringAfter(':', segment)
    val name = path.trimEnd('/').substringAfterLast('/')
    return name.ifBlank { null }
}

@Composable
internal fun SettingsNetworkSection(
    prefs: PrefsState,
    vm: SettingsViewModel,
    reduceMotion: Boolean,
    actions: SettingsActions,
) {
    val folder = remember(prefs.downloadTree) { downloadFolderName(prefs.downloadTree) }
    val defaultLabel = stringResource(R.string.settings_download_default)
    val changeLabel = stringResource(R.string.settings_download_change)

    SettingsSectionHeader(stringResource(R.string.settings_section_network))
    SettingsGroup {
        SettingsSwitchRow(
            title = stringResource(R.string.settings_wifi_only),
            subtitle = stringResource(R.string.settings_wifi_only_body),
            checked = prefs.wifiOnly,
            icon = Icons.Filled.Wifi,
            onChange = { vm.setWifiOnly(it) },
        )
        SettingsDivider()
        SettingsBlock(
            title = stringResource(R.string.settings_parallel),
            subtitle = stringResource(R.string.settings_parallel_body),
        ) {
            SettingsSegmented(
                options = parallelOptions(),
                value = prefs.parallelTransfers.toString(),
                onChange = { count -> count.toIntOrNull()?.let { vm.setParallelTransfers(it) } },
                reduceMotion = reduceMotion,
            )
        }
        SettingsDivider()
        SettingsRow(
            title = stringResource(R.string.settings_download_location),
            subtitle = folder ?: defaultLabel,
            icon = Icons.Filled.Download,
            iconTint = MaterialTheme.colorScheme.primary,
            onClick = actions.onPickDownloadFolder,
            trailing = {
                Text(
                    text = changeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
        )
        Text(
            text = stringResource(R.string.settings_download_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 60.dp, end = 16.dp, bottom = 12.dp),
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_keep_awake),
            subtitle = stringResource(R.string.settings_keep_awake_body),
            checked = prefs.keepAwake,
            icon = Icons.Filled.WbSunny,
            onChange = { vm.setKeepAwake(it) },
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_notify_sound),
            subtitle = stringResource(R.string.settings_notify_sound_body),
            checked = prefs.notifySound,
            icon = Icons.Filled.NotificationsActive,
            onChange = { vm.setNotifySound(it) },
        )
    }
}

/* ------------------------------ keamanan ----------------------------- */

@Composable
internal fun SettingsSecuritySection(
    prefs: PrefsState,
    vm: SettingsViewModel,
    actions: SettingsActions,
) {
    var tipsRestored by remember { mutableStateOf(false) }
    LaunchedEffect(tipsRestored) {
        if (tipsRestored) {
            delay(2600)
            tipsRestored = false
        }
    }

    SettingsSectionHeader(stringResource(R.string.settings_section_security))
    SettingsGroup {
        SettingsSwitchRow(
            title = stringResource(R.string.settings_app_lock),
            subtitle = stringResource(R.string.settings_app_lock_body),
            checked = prefs.appLock,
            icon = Icons.Filled.Lock,
            onChange = { vm.setAppLock(it) },
        )
        if (prefs.appLock) {
            Text(
                text = stringResource(R.string.settings_app_lock_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 60.dp, end = 16.dp, bottom = 12.dp),
            )
        }
        SettingsDivider()
        SettingsActionRow(
            title = stringResource(R.string.settings_tips),
            subtitle = stringResource(R.string.settings_tips_body),
            icon = Icons.Filled.Refresh,
            onClick = {
                vm.setGesturesSeen(false)
                tipsRestored = true
            },
        )
        if (tipsRestored) {
            Text(
                text = stringResource(R.string.settings_tips_done),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 60.dp, end = 16.dp, bottom = 10.dp),
            )
        }
        SettingsDivider()
        SettingsActionRow(
            title = stringResource(R.string.settings_feedback),
            subtitle = stringResource(R.string.settings_feedback_body),
            icon = Icons.Filled.Description,
            onClick = actions.onSendFeedback,
        )
    }
}

/* -------------------------------- data ------------------------------- */

@Composable
internal fun SettingsDataSection(
    prefs: PrefsState,
    vm: SettingsViewModel,
    cacheBytes: Long,
    actions: SettingsActions,
) {
    var cacheCleared by remember { mutableStateOf(false) }
    var libraryCleared by remember { mutableStateOf(false) }
    LaunchedEffect(cacheCleared) {
        if (cacheCleared) {
            delay(2600)
            cacheCleared = false
        }
    }
    LaunchedEffect(libraryCleared) {
        if (libraryCleared) {
            delay(2600)
            libraryCleared = false
        }
    }

    SettingsSectionHeader(stringResource(R.string.settings_section_data))
    SettingsGroup {
        SettingsRow(
            title = stringResource(R.string.settings_cache),
            subtitle = stringResource(R.string.settings_cache_body),
            value = readableSize(cacheBytes),
            icon = Icons.Filled.Storage,
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 12.dp)) {
            HoldToConfirmButton(
                label = stringResource(R.string.settings_cache_clear),
                hint = if (cacheCleared) {
                    stringResource(R.string.settings_cache_cleared)
                } else {
                    stringResource(R.string.settings_cache_clear_hint)
                },
                onConfirm = {
                    vm.clearCache()
                    cacheCleared = true
                },
            )
        }
        SettingsDivider()
        SettingsRow(
            title = stringResource(R.string.settings_library),
            subtitle = stringResource(R.string.settings_library_body),
            icon = Icons.Filled.Star,
            iconTint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(
                R.string.settings_library_counts,
                prefs.favorites.size,
                prefs.recent.size,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 60.dp, end = 16.dp, bottom = 10.dp),
        )
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 12.dp)) {
            HoldToConfirmButton(
                label = stringResource(R.string.settings_library_clear),
                hint = if (libraryCleared) {
                    stringResource(R.string.settings_library_cleared)
                } else {
                    stringResource(R.string.settings_library_clear_hint)
                },
                onConfirm = {
                    vm.clearLibrary()
                    libraryCleared = true
                },
            )
        }
        SettingsDivider()
        SettingsActionRow(
            title = stringResource(R.string.settings_log),
            subtitle = stringResource(R.string.settings_log_body),
            icon = Icons.Filled.Code,
            onClick = actions.onExportLog,
        )
    }
}

/* -------------------------------- tentang ---------------------------- */

@Composable
internal fun SettingsAboutSection(actions: SettingsActions) {
    SettingsSectionHeader(stringResource(R.string.settings_section_about))
    SettingsGroup {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = stringResource(R.string.settings_app_name),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_built_with),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SettingsDivider()
        SettingsRow(
            title = stringResource(R.string.settings_version),
            value = stringResource(
                R.string.settings_version_value,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
            ),
            icon = Icons.Filled.Info,
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsDivider()
        SettingsActionRow(
            title = stringResource(R.string.settings_repo),
            subtitle = stringResource(R.string.settings_repo_body),
            icon = Icons.Filled.Code,
            onClick = { actions.onOpenLink(REPO_URL) },
        )
        SettingsDivider()
        SettingsActionRow(
            title = stringResource(R.string.settings_license),
            value = stringResource(R.string.settings_license_value),
            icon = Icons.Filled.Description,
            onClick = actions.onShowLicense,
        )
        SettingsDivider()
        SettingsActionRow(
            title = stringResource(R.string.settings_whats_new),
            subtitle = stringResource(R.string.settings_whats_new_body),
            icon = Icons.Filled.Star,
            iconTint = MaterialTheme.colorScheme.primary,
            onClick = actions.onShowWhatsNew,
        )
    }
}
