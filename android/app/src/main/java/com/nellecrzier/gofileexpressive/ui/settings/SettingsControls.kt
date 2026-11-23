package com.nellecrzier.gofileexpressive.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nellecrzier.gofileexpressive.AccentChoice
import com.nellecrzier.gofileexpressive.Density
import com.nellecrzier.gofileexpressive.R

/* ------------------------------ segmen ------------------------------ */

/**
 * Segmen setelan Material 3. `SingleChoiceSegmentedButtonRow` sudah mengumumkan
 * "radio button 1 dari 3" ke TalkBack dan menjaga sasaran sentuh 48dp, jadi
 * `reduceMotion` tidak lagi dipakai di sini.
 *
 * Perpindahan pilihan diberi getaran [HapticFeedbackType.ContextClick];
 * mengetuk segmen yang sudah terpilih tidak mengubah apa pun, jadi tidak
 * bergetar. Getaran ini sengaja terpisah dari `reduceMotion`.
 */
@Composable
internal fun SettingsSegmented(
    options: List<Pair<String, String>>,
    value: String,
    onChange: (String) -> Unit,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (key, label) ->
            SegmentedButton(
                selected = value == key,
                onClick = {
                    if (value != key) {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    }
                    onChange(key)
                },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                },
            )
        }
    }
}

/* ------------------------------- sakelar ----------------------------- */

/**
 * Baris dengan sakelar. Sakelar mendapat deskripsi + status baca layar karena
 * `Switch` sendiri tidak menempelkan `ToggleableState`. Setiap perubahan
 * memberi getaran [HapticFeedbackType.ContextClick].
 */
@Composable
internal fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val haptic = LocalHapticFeedback.current
    val stateText = stringResource(
        if (checked) R.string.settings_state_on else R.string.settings_state_off,
    )
    ListItem(
        headlineContent = {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
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
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = { next ->
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onChange(next)
                },
                enabled = enabled,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = title
                        stateDescription = stateText
                    },
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Baris aksi dengan panah: membuka layar/lembar lain. */
@Composable
internal fun SettingsActionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    ListItem(
        headlineContent = {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
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
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) {
                    Spacer(Modifier.width(8.dp))
                    ValueBadge(value)
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

/* -------------------------------- geser ------------------------------ */

/** Penggeser dengan label nilai; `contentDescription` menjaga fokus baca. */
@Composable
internal fun SettingsSlider(
    title: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            ValueBadge(valueLabel)
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .semantics { contentDescription = title },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                activeTickColor = MaterialTheme.colorScheme.onPrimary,
                inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            ),
        )
    }
}

/* ------------------------------- aksen ------------------------------- */

/**
 * Warna chip aksen. Disalin dari `Theme.kt` (pemetaannya privat di sana) agar
 * pratinjau tetap menunjukkan lima pilihan, bukan warna yang sedang aktif.
 */
private fun accentSwatch(choice: AccentChoice): Color = when (choice) {
    AccentChoice.CORAL -> Color(0xFFFF5F45)
    AccentChoice.AMBER -> Color(0xFFFFB547)
    AccentChoice.MINT -> Color(0xFF6FD99A)
    AccentChoice.VIOLET -> Color(0xFFA78BFA)
    AccentChoice.BLUE -> Color(0xFF5B9DFF)
}

@Composable
private fun accentName(choice: AccentChoice): String = stringResource(
    when (choice) {
        AccentChoice.CORAL -> R.string.settings_accent_coral
        AccentChoice.AMBER -> R.string.settings_accent_amber
        AccentChoice.MINT -> R.string.settings_accent_mint
        AccentChoice.VIOLET -> R.string.settings_accent_violet
        AccentChoice.BLUE -> R.string.settings_accent_blue
    },
)

/**
 * Lima bulatan aksen. Pilihan ditandai cincin + nama aksen yang terpilih, jadi
 * status tidak hanya dibedakan warna. Memilih aksen lain memberi getaran
 * [HapticFeedbackType.ContextClick], sama seperti segmen.
 */
@Composable
internal fun AccentSwatches(
    value: AccentChoice,
    onChange: (AccentChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val selectedName = accentName(value)
    Column(modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.settings_accent),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            ValueBadge(selectedName)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AccentChoice.entries.forEach { choice ->
                val selected = value == choice
                val name = accentName(choice)
                val description = stringResource(R.string.settings_accent_description, name)
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .semantics { contentDescription = description }
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = {
                                if (!selected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                }
                                onChange(choice)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentSwatch(choice)),
                    )
                    if (selected) {
                        Box(
                            Modifier
                                .size(46.dp)
                                .border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
                        )
                    }
                }
            }
        }
    }
}

/* ------------------------------ pratinjau ---------------------------- */

/** Pratinjau ukuran teks: memakai skala draf, bukan skala tema yang aktif. */
@Composable
internal fun TextSizePreview(
    scale: Float,
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = (16f * scale).sp,
                lineHeight = (24f * scale).sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

/** Pratinjau kerapatan: dua baris yang tingginya mengikuti pilihan. */
@Composable
internal fun DensityPreview(
    density: Density,
    modifier: Modifier = Modifier,
) {
    val padding = when (density) {
        Density.COMPACT -> 6.dp
        Density.COMFORT -> 11.dp
        Density.RELAXED -> 17.dp
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            PreviewFileRow(
                title = stringResource(R.string.settings_density_preview_folder),
                meta = stringResource(R.string.settings_density_preview_meta),
                vertical = padding,
                folder = true,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            PreviewFileRow(
                title = stringResource(R.string.settings_density_preview_file),
                meta = stringResource(R.string.settings_density_preview_meta),
                vertical = padding,
                folder = false,
            )
        }
    }
}

@Composable
private fun PreviewFileRow(
    title: String,
    meta: String,
    vertical: androidx.compose.ui.unit.Dp,
    folder: Boolean,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = vertical),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (folder) Icons.Filled.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            tint = if (folder) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = meta,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
        )
    }
}

/** Pil nilai kecil: latar aksen lembut, teks aksen, sudut 8dp. */
@Composable
private fun ValueBadge(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
        )
    }
}
