package com.nellecrzier.gofileexpressive.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.nellecrzier.gofileexpressive.R
import com.nellecrzier.gofileexpressive.ui.Hyper
import com.nellecrzier.gofileexpressive.ui.HyperButton
import com.nellecrzier.gofileexpressive.ui.SheetShape

internal const val APACHE_LICENSE_URL = "https://www.apache.org/licenses/LICENSE-2.0"
internal const val REPO_URL = "https://github.com/immorgue/gofile-expressive"

/** Kerangka lembar bawah setelan: sudut 30dp, garis rambut, tanpa bayangan. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheetFrame(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = SheetShape,
        containerColor = Hyper.Card,
        contentColor = Hyper.Text,
        scrimColor = Hyper.SheetScrim,
        dragHandle = { SettingsDragHandle() },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, bottom = 22.dp),
            content = content,
        )
    }
}

@Composable
private fun SettingsDragHandle() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .width(38.dp)
                .height(4.dp)
                .background(Hyper.HairlineStrong, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * "Yang baru": teks changelog apa adanya (CHANGELOG.md dibundel sebagai aset).
 * Tidak ada WebView maupun HTML — hanya teks biasa di kolom yang bisa digulir.
 */
@Composable
internal fun WhatsNewSheet(
    version: String,
    text: String,
    onDismiss: () -> Unit,
) {
    SettingsSheetFrame(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.settings_whats_new_title, version),
            style = MaterialTheme.typography.titleLarge,
            color = Hyper.Text,
        )
        Spacer(Modifier.height(10.dp))
        if (text.isBlank()) {
            Text(
                text = stringResource(R.string.settings_whats_new_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = Hyper.Text2,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = text.trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = Hyper.Text2,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        HyperButton(
            text = stringResource(R.string.settings_close),
            onClick = onDismiss,
            primary = false,
            height = 50.dp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Lisensi: teks singkat Apache-2.0 plus tautan ke teks penuh. */
@Composable
internal fun LicenseSheet(
    onDismiss: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    SettingsSheetFrame(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.settings_license_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            color = Hyper.Text,
        )
        Spacer(Modifier.height(10.dp))
        RowLicense()
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.settings_license_body),
            style = MaterialTheme.typography.bodyMedium,
            color = Hyper.Text2,
        )
        Spacer(Modifier.height(14.dp))
        // Teks lisensi lengkap, dibaca sebagai teks biasa di kolom bergulir.
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.settings_license_full),
                style = MaterialTheme.typography.bodySmall,
                color = Hyper.Text3,
            )
        }
        Spacer(Modifier.height(16.dp))
        HyperButton(
            text = stringResource(R.string.settings_license_link),
            onClick = {
                onOpenLink(APACHE_LICENSE_URL)
                onDismiss()
            },
            primary = false,
            height = 50.dp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        HyperButton(
            text = stringResource(R.string.settings_close),
            onClick = onDismiss,
            primary = false,
            height = 50.dp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RowLicense() {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.Description,
            contentDescription = null,
            tint = Hyper.Accent,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.settings_license_value),
            style = MaterialTheme.typography.labelLarge,
            color = Hyper.Text,
        )
    }
}
