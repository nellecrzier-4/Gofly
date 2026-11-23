package com.nellecrzier.gofileexpressive.ui.share

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nellecrzier.gofileexpressive.GItem
import com.nellecrzier.gofileexpressive.GoFileException
import com.nellecrzier.gofileexpressive.GofileApi
import com.nellecrzier.gofileexpressive.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

private const val RETRY_NONE = ""
private const val RETRY_SAVE = "save"
private const val RETRY_RENAME = "rename"

private const val NAME_MAX = 255
private const val PASSWORD_MAX = 64
private const val DESCRIPTION_MAX = 1000
private const val TAG_MAX = 32
private const val TAG_COUNT_MAX = 20

/**
 * Setelan konten native untuk atribut yang benar-benar didukung
 * [GofileApi.updateAttribute]: publik, kata sandi, kedaluwarsa, deskripsi,
 * tag, dan ganti nama lewat [GofileApi.renameContent].
 *
 * Semua panggilan jaringan berjalan di [Dispatchers.IO] di dalam scope
 * komposisi. GoFile tidak menyediakan daftar tautan yang dibagikan maupun
 * endpoint pencabutan, jadi keduanya tidak ditawarkan di sini.
 *
 * @param onSaved dipanggil dengan pesan konfirmasi yang sudah diterjemahkan
 *   setelah penyimpanan berhasil; lembar juga langsung ditutup.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettingsSheet(
    item: GItem,
    token: String,
    reduceMotion: Boolean,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val hasSession = token.isNotBlank()

    var publicValue by remember(item.id) { mutableStateOf<Boolean?>(null) }
    var password by remember(item.id) { mutableStateOf("") }
    var passwordEdited by remember(item.id) { mutableStateOf(false) }
    var expireAt by remember(item.id) { mutableStateOf<Long?>(null) }
    var expireEdited by remember(item.id) { mutableStateOf(false) }
    var description by remember(item.id) { mutableStateOf("") }
    var descriptionEdited by remember(item.id) { mutableStateOf(false) }
    var tagsText by remember(item.id) { mutableStateOf("") }
    var tagsEdited by remember(item.id) { mutableStateOf(false) }

    var pickDate by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var errorRes by remember { mutableStateOf<Int?>(null) }
    var retryKind by remember { mutableStateOf(RETRY_NONE) }

    var renameOpen by remember(item.id) { mutableStateOf(false) }
    var newName by remember(item.id) { mutableStateOf(item.name) }
    var renaming by remember { mutableStateOf(false) }

    val savedText = stringResource(R.string.share_settings_saved)
    val renamedText = stringResource(R.string.share_settings_renamed)

    /* ----------------------------- validasi ----------------------------- */

    val expireValue = expireAt
    val passwordError = if (password.length > PASSWORD_MAX) R.string.share_error_password_long else null
    val descriptionError = if (description.length > DESCRIPTION_MAX) R.string.share_error_description_long else null
    val parsedTags = remember(tagsText) { parseShareTags(tagsText) }
    val tagsError = if (tagsEdited) parsedTags.error else null
    val expireError = if (expireValue != null && expireValue <= System.currentTimeMillis() / 1000L) {
        R.string.share_error_expire_past
    } else {
        null
    }

    val pending = publicValue != null || passwordEdited || expireEdited ||
        descriptionEdited || tagsEdited
    val blocked = passwordError != null || descriptionError != null ||
        tagsError != null || expireError != null
    val ready = hasSession && pending && !blocked

    val trimmedName = newName.trim()
    val nameError = validateShareName(trimmedName)
    val nameChanged = trimmedName.isNotEmpty() && trimmedName != item.name

    /* ------------------------------ aksi ------------------------------- */

    fun persist() {
        if (!ready || saving) return
        val publicAttribute = publicValue
        val passwordValue = password
        val expireValueToSend = expireAt
        val descriptionValue = description
        val tagList = parsedTags.tags
        val sendPassword = passwordEdited
        val sendExpire = expireEdited
        val sendDescription = descriptionEdited
        val sendTags = tagsEdited

        errorRes = null
        retryKind = RETRY_NONE
        saving = true
        scope.launch {
            val failure = withContext(Dispatchers.IO) {
                try {
                    if (publicAttribute != null) {
                        GofileApi.updateAttribute(token, item.id, "public", publicAttribute)
                    }
                    if (sendPassword) {
                        GofileApi.updateAttribute(token, item.id, "password", passwordValue)
                    }
                    if (sendExpire) {
                        GofileApi.updateAttribute(token, item.id, "expire", expireValueToSend ?: "")
                    }
                    if (sendDescription) {
                        GofileApi.updateAttribute(token, item.id, "description", descriptionValue)
                    }
                    if (sendTags) {
                        GofileApi.updateAttribute(token, item.id, "tags", tagList.joinToString(","))
                    }
                    null
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    error
                }
            }
            saving = false
            if (failure == null) {
                onSaved(savedText)
                onDismiss()
            } else {
                errorRes = shareErrorRes(failure)
                retryKind = if (failure is GoFileException.Network ||
                    failure is GoFileException.Server
                ) {
                    RETRY_SAVE
                } else {
                    RETRY_NONE
                }
            }
        }
    }

    fun applyRename() {
        if (!hasSession || renaming || nameError != null || !nameChanged) return
        val target = trimmedName
        errorRes = null
        retryKind = RETRY_NONE
        renaming = true
        scope.launch {
            val failure = withContext(Dispatchers.IO) {
                try {
                    GofileApi.renameContent(token, item.id, target)
                    null
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    error
                }
            }
            renaming = false
            if (failure == null) {
                onSaved(renamedText)
                onDismiss()
            } else {
                errorRes = shareErrorRes(failure)
                retryKind = if (failure is GoFileException.Network ||
                    failure is GoFileException.Server
                ) {
                    RETRY_RENAME
                } else {
                    RETRY_NONE
                }
            }
        }
    }

    /* -------------------------------- UI ------------------------------- */

    ShareSheetFrame(onDismiss = onDismiss, modifier = Modifier.imePadding()) {
        ShareContentFade(reduceMotion) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.share_settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = middleEllipsis(item.name, 42),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FilledTonalIconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.share_close),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            ShareNotice(
                text = stringResource(R.string.share_settings_no_links_notice),
                error = false,
                icon = Icons.Filled.Info,
            )

            if (!hasSession) {
                Spacer(Modifier.height(10.dp))
                ShareNotice(
                    text = stringResource(R.string.share_settings_no_session),
                    error = true,
                )
            }

            val failure = errorRes
            if (failure != null) {
                Spacer(Modifier.height(10.dp))
                ShareNotice(
                    text = stringResource(failure),
                    error = true,
                    actionLabel = if (retryKind != RETRY_NONE) {
                        stringResource(R.string.share_action_retry)
                    } else {
                        null
                    },
                    onAction = if (retryKind == RETRY_RENAME) {
                        { applyRename() }
                    } else if (retryKind == RETRY_SAVE) {
                        { persist() }
                    } else {
                        null
                    },
                    onDismiss = { errorRes = null },
                )
            }

            SheetSectionHeader(stringResource(R.string.share_settings_section_access))
            ShareSettingsCard {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.share_settings_visibility),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = when (publicValue) {
                                true -> stringResource(R.string.share_settings_visibility_public)
                                false -> stringResource(R.string.share_settings_visibility_private)
                                null -> stringResource(R.string.share_settings_visibility_keep)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (publicValue == null) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = publicValue ?: false,
                        onCheckedChange = { publicValue = it },
                        enabled = hasSession,
                    )
                }
                Text(
                    text = stringResource(R.string.share_settings_visibility_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
                )
                SharePasswordField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordEdited = true
                    },
                    enabled = hasSession,
                    error = if (passwordError != null) stringResource(passwordError) else null,
                    hint = stringResource(R.string.share_settings_password_hint),
                )
            }

            SheetSectionHeader(stringResource(R.string.share_settings_section_expiry))
            ShareSettingsCard {
                ShareActionRow(
                    icon = Icons.Filled.DateRange,
                    title = stringResource(R.string.share_settings_expire_row),
                    subtitle = if (expireValue != null) {
                        shareDate(expireValue)
                    } else {
                        stringResource(R.string.share_settings_expire_none)
                    },
                    enabled = hasSession,
                    onClick = { pickDate = true },
                )
                if (expireValue != null) {
                    ShareActionRow(
                        icon = Icons.Filled.Close,
                        title = stringResource(R.string.share_settings_expire_clear),
                        enabled = hasSession,
                        onClick = {
                            expireAt = null
                            expireEdited = true
                        },
                    )
                }
                Text(
                    text = if (expireError != null) {
                        stringResource(expireError)
                    } else {
                        stringResource(R.string.share_settings_expire_note)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (expireError != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 10.dp),
                )
            }

            SheetSectionHeader(stringResource(R.string.share_settings_section_meta))
            ShareSettingsCard {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            descriptionEdited = true
                        },
                        label = { Text(stringResource(R.string.share_settings_description)) },
                        placeholder = {
                            Text(stringResource(R.string.share_settings_description_hint))
                        },
                        supportingText = {
                            Text(
                                if (descriptionError != null) {
                                    stringResource(descriptionError)
                                } else {
                                    stringResource(R.string.share_settings_description_support, DESCRIPTION_MAX)
                                },
                            )
                        },
                        isError = descriptionError != null,
                        enabled = hasSession,
                        singleLine = false,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tagsText,
                        onValueChange = {
                            tagsText = it
                            tagsEdited = true
                        },
                        label = { Text(stringResource(R.string.share_settings_tags)) },
                        placeholder = { Text(stringResource(R.string.share_settings_tags_hint)) },
                        supportingText = {
                            Text(
                                if (tagsError != null) {
                                    stringResource(tagsError)
                                } else {
                                    stringResource(R.string.share_settings_tags_support)
                                },
                            )
                        },
                        isError = tagsError != null,
                        enabled = hasSession,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Done,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            SheetSectionHeader(stringResource(R.string.share_settings_section_name))
            ShareSettingsCard {
                ShareActionRow(
                    icon = Icons.Filled.Edit,
                    title = stringResource(R.string.share_settings_rename_row),
                    subtitle = middleEllipsis(item.name, 40),
                    enabled = hasSession,
                    onClick = { renameOpen = !renameOpen },
                )
                if (renameOpen) {
                    Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 12.dp)) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it.take(NAME_MAX + 1) },
                            label = { Text(stringResource(R.string.share_settings_rename_hint)) },
                            supportingText = {
                                Text(
                                    if (nameError != null) {
                                        stringResource(nameError)
                                    } else {
                                        stringResource(R.string.share_settings_rename_note)
                                    },
                                )
                            },
                            isError = nameError != null,
                            enabled = hasSession,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        FilledTonalButton(
                            onClick = { applyRename() },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            enabled = hasSession && nameChanged && nameError == null && !renaming,
                            shape = RoundedCornerShape(percent = 50),
                        ) {
                            if (renaming) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            Text(stringResource(R.string.share_settings_rename_row))
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { persist() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = ready && !saving,
                shape = RoundedCornerShape(percent = 50),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    stringResource(
                        if (saving) R.string.share_settings_saving else R.string.share_settings_save,
                    ),
                )
            }
            if (!pending) {
                Text(
                    text = stringResource(R.string.share_settings_no_changes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                )
            }
        }
    }

    if (pickDate) {
        val initialMillis = expireValue?.let { it * 1000L }
        val dateState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = dateState.selectedDateMillis
                        if (picked != null) {
                            expireAt = endOfDayEpochSeconds(picked)
                            expireEdited = true
                        }
                        pickDate = false
                    },
                ) {
                    Text(
                        text = stringResource(R.string.share_action_save_date),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pickDate = false }) {
                    Text(
                        text = stringResource(R.string.share_action_cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        ) {
            DatePicker(state = dateState, showModeToggle = false)
        }
    }
}

/* ------------------------------- komponen ------------------------------- */

/** Judul bagian Material 3: kapital, tanpa garis aksen. */
@Composable
private fun SheetSectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
    )
}

/** Kartu bagian lembar: permukaan Material 3 datar, tanpa efek kaca. */
@Composable
private fun ShareSettingsCard(content: @Composable ColumnScope.() -> Unit) {
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

/** Pemberitahuan Material 3: ikon, teks, aksi teks, dan tombol tutup. */
@Composable
private fun ShareNotice(
    text: String,
    modifier: Modifier = Modifier,
    error: Boolean = true,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon ?: if (error) {
                    Icons.Filled.ErrorOutline
                } else {
                    Icons.Filled.CheckCircle
                },
                contentDescription = null,
                tint = if (error) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onAction) {
                    Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
            if (onDismiss != null) {
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.share_close),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/** Bidang kata sandi bertitik dengan tombol lihat/sembunyikan bergaya Material 3. */
@Composable
private fun SharePasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    error: String?,
    hint: String,
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.share_settings_password)) },
        placeholder = {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
            )
        },
        supportingText = error?.let { { Text(it) } },
        isError = error != null,
        enabled = enabled,
        singleLine = true,
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = stringResource(R.string.share_settings_password),
                    modifier = Modifier.size(19.dp),
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
    )
}

/* ------------------------------- validasi ------------------------------- */

/** Nama konten: tidak kosong, maksimal 255, tanpa pemisah path. */
internal fun validateShareName(name: String): Int? = when {
    name.isBlank() -> R.string.share_error_name_empty
    name.length > NAME_MAX -> R.string.share_error_name_long
    name == "." || name == ".." -> R.string.share_error_name_chars
    name.contains('/') || name.contains('\\') -> R.string.share_error_name_chars
    name.any { it.code < 32 } -> R.string.share_error_name_chars
    else -> null
}

internal data class ShareTags(val tags: List<String>, val error: Int?)

private val TAG_PATTERN = Regex("^[a-z0-9-]+$")

/**
 * Tag GoFile: huruf kecil, angka, tanda hubung; dipisah koma. Duplikat
 * dibuang diam-diam, urutan dipertahankan.
 */
internal fun parseShareTags(raw: String): ShareTags {
    val unique = LinkedHashSet<String>()
    raw.split(',', '\n').forEach { part ->
        val tag = part.trim()
        if (tag.isNotEmpty()) unique.add(tag)
    }
    val tags = unique.toList()
    val error = when {
        tags.size > TAG_COUNT_MAX -> R.string.share_error_tag_count
        tags.any { it.length > TAG_MAX } -> R.string.share_error_tag_long
        tags.any { !TAG_PATTERN.matches(it) } -> R.string.share_error_tag_chars
        else -> null
    }
    return ShareTags(tags, error)
}

/**
 * Tanggal dari DatePicker (tengah malam UTC) menjadi akhir hari waktu lokal,
 * karena tautan GoFile berhenti berlaku pada akhir tanggal yang dipilih.
 */
internal fun endOfDayEpochSeconds(utcDateMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = utcDateMillis
    }
    val local = Calendar.getInstance().apply {
        clear()
        set(
            utc.get(Calendar.YEAR),
            utc.get(Calendar.MONTH),
            utc.get(Calendar.DAY_OF_MONTH),
            23,
            59,
            59,
        )
    }
    return local.timeInMillis / 1000L
}
