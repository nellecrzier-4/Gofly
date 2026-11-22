package com.nellecrzier.gofileexpressive.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.nellecrzier.gofileexpressive.ui.Hyper
import com.nellecrzier.gofileexpressive.ui.HyperRow
import com.nellecrzier.gofileexpressive.ui.HyperToggle
import com.nellecrzier.gofileexpressive.ui.ValuePill

/* ------------------------------ segmen ------------------------------ */

/**
 * Segmen setelan dengan tinggi sentuh 48dp (HyperSegmented bawaan 44dp) dan
 * `selectableGroup` supaya TalkBack mengumumkan "radio button 1 dari 3".
 */
@Composable
internal fun SettingsSegmented(
    options: List<Pair<String, String>>,
    value: String,
    onChange: (String) -> Unit,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Hyper.Track,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { (key, label) ->
                val selected = value == key
                val container by animateColorAsState(
                    targetValue = if (selected) Hyper.Accent else Color.Transparent,
                    animationSpec = tween(if (reduceMotion) 0 else 220),
                    label = "settingsSegmentFill",
                )
                val content by animateColorAsState(
                    targetValue = if (selected) Hyper.AccentInk else Hyper.Text2,
                    animationSpec = tween(if (reduceMotion) 0 else 180),
                    label = "settingsSegmentText",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(container)
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onChange(key) },
                        )
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = content,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

/* ------------------------------- sakelar ----------------------------- */

/**
 * Baris dengan sakelar. Sakelar mendapat deskripsi + status baca layar karena
 * `HyperToggle` sendiri tidak menempelkan `ToggleableState`.
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
    iconTint: Color = Hyper.Text2,
) {
    val stateText = stringResource(
        if (checked) R.string.settings_state_on else R.string.settings_state_off,
    )
    HyperRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        iconTint = iconTint,
        modifier = modifier,
        trailing = {
            HyperToggle(
                checked = checked,
                onChange = onChange,
                enabled = enabled,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = title
                        stateDescription = stateText
                    },
            )
        },
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
    iconTint: Color = Hyper.Text2,
) {
    HyperRow(
        title = title,
        subtitle = subtitle,
        value = value,
        icon = icon,
        iconTint = iconTint,
        onClick = onClick,
        modifier = modifier,
        trailing = {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Hyper.Text3,
                modifier = Modifier.size(20.dp),
            )
        },
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
                color = Hyper.Text,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            ValuePill(valueLabel)
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Hyper.Text2,
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
                thumbColor = Hyper.Accent,
                activeTrackColor = Hyper.Accent,
                inactiveTrackColor = Hyper.Track,
                activeTickColor = Hyper.AccentInk,
                inactiveTickColor = Hyper.Text3,
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
 * status tidak hanya dibedakan warna.
 */
@Composable
internal fun AccentSwatches(
    value: AccentChoice,
    onChange: (AccentChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedName = accentName(value)
    Column(modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.settings_accent),
                style = MaterialTheme.typography.titleSmall,
                color = Hyper.Text,
                modifier = Modifier.weight(1f),
            )
            ValuePill(selectedName)
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
                            onClick = { onChange(choice) },
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
                                .border(2.dp, Hyper.Text, CircleShape),
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
        color = Hyper.Track,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = (16f * scale).sp,
                lineHeight = (24f * scale).sp,
            ),
            color = Hyper.Text,
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
        color = Hyper.Track,
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
                    .height(0.8.dp)
                    .background(Hyper.Hairline),
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
            tint = if (folder) Hyper.Accent else Hyper.Text2,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Hyper.Text,
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
            color = Hyper.Text3,
            maxLines = 1,
        )
    }
}
