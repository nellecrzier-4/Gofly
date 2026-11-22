package com.nellecrzier.gofileexpressive.ui.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nellecrzier.gofileexpressive.GSession
import com.nellecrzier.gofileexpressive.R
import com.nellecrzier.gofileexpressive.ui.ExpressiveLoader
import com.nellecrzier.gofileexpressive.ui.Hyper
import com.nellecrzier.gofileexpressive.ui.HyperButton
import com.nellecrzier.gofileexpressive.ui.HyperCard
import com.nellecrzier.gofileexpressive.ui.HyperGroup
import com.nellecrzier.gofileexpressive.ui.HyperIconButton
import com.nellecrzier.gofileexpressive.ui.HyperPage
import com.nellecrzier.gofileexpressive.ui.HyperRow
import com.nellecrzier.gofileexpressive.ui.HoldToConfirm
import com.nellecrzier.gofileexpressive.ui.NoticeBanner
import com.nellecrzier.gofileexpressive.ui.SectionLabel
import com.nellecrzier.gofileexpressive.ui.readableSize

/**
 * Layar akun: identitas, penyimpanan, rincian per jenis, dan keluar akun.
 * Semua angka berasal dari payload GoFile; kunci yang tidak ada ditampilkan
 * sebagai "tidak tersedia", bukan angka karangan.
 */
@Composable
internal fun AccountScreen(
    session: GSession,
    state: AccountUiState,
    reduceMotion: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSignOut: (Boolean) -> Unit,
) {
    val scroll = rememberScrollState()
    val snapshot = (state as? AccountUiState.Ready)?.snapshot

    BackHandler(enabled = true, onBack = onBack)

    HyperPage(
        title = stringResource(R.string.settings_account_title),
        subtitle = stringResource(R.string.settings_account_subtitle),
        scroll = scroll,
        reduceMotion = reduceMotion,
        actions = {
            HyperIconButton(
                icon = Icons.Filled.Refresh,
                description = stringResource(R.string.settings_account_retry),
                onClick = onRetry,
            )
            HyperIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                description = stringResource(R.string.settings_account_back),
                onClick = onBack,
            )
        },
    ) {
        AccountHeader(session, state)

        when (state) {
            AccountUiState.Loading -> AccountLoadingCard(reduceMotion)

            is AccountUiState.Failed -> {
                Spacer(Modifier.height(12.dp))
                NoticeBanner(text = stringResource(accountErrorRes(state.error)))
                Spacer(Modifier.height(10.dp))
                HyperButton(
                    text = stringResource(R.string.settings_account_retry),
                    onClick = onRetry,
                    primary = false,
                    height = 50.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            AccountUiState.Expired -> ExpiredSessionCard(
                onSignIn = { onSignOut(false) },
            )

            AccountUiState.Unavailable -> {
                Spacer(Modifier.height(12.dp))
                NoteCard(stringResource(R.string.settings_account_no_details))
            }

            is AccountUiState.Ready -> {
                StorageSection(state.snapshot, reduceMotion)
                BreakdownSection(state.snapshot, reduceMotion)
            }
        }

        IdentitySection(session, snapshot)
        SignOutSection(onSignOut)
    }
}

/* ------------------------------ kartu atas --------------------------- */

@Composable
private fun AccountHeader(session: GSession, state: AccountUiState) {
    val email = (state as? AccountUiState.Ready)?.snapshot?.email
        ?.takeIf { it.isNotBlank() }
        ?: session.email.takeIf { it.isNotBlank() }
    val tier = (state as? AccountUiState.Ready)?.snapshot?.tier
        ?.takeIf { it.isNotBlank() }
        ?: session.tier.takeIf { it.isNotBlank() }

    HyperCard(
        fill = Hyper.AccentSoft,
        border = Hyper.AccentEdge,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Hyper.Card,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.CloudQueue,
                            contentDescription = null,
                            tint = Hyper.Accent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = email ?: stringResource(R.string.settings_account_guest),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Hyper.Text,
                        maxLines = 2,
                    )
                    Spacer(Modifier.height(3.dp))
                    if (tier != null) {
                        AccountTierBadge(tier)
                    } else {
                        Text(
                            text = stringResource(R.string.settings_account_guest),
                            style = MaterialTheme.typography.bodySmall,
                            color = Hyper.Text2,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountLoadingCard(reduceMotion: Boolean) {
    Spacer(Modifier.height(14.dp))
    HyperGroup {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExpressiveLoader(
                size = 28.dp,
                color = Hyper.Accent,
                reduceMotion = reduceMotion,
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = stringResource(R.string.settings_account_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = Hyper.Text2,
            )
        }
    }
}

@Composable
private fun ExpiredSessionCard(onSignIn: () -> Unit) {
    Spacer(Modifier.height(14.dp))
    HyperCard(
        border = Hyper.AccentEdge,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = Hyper.Accent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.settings_account_expired_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Hyper.Text,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.settings_account_expired_body),
                style = MaterialTheme.typography.bodySmall,
                color = Hyper.Text2,
            )
            Spacer(Modifier.height(16.dp))
            HyperButton(
                text = stringResource(R.string.settings_account_sign_in),
                icon = Icons.AutoMirrored.Filled.Logout,
                onClick = onSignIn,
                height = 50.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/* ---------------------------- penyimpanan ---------------------------- */

@Composable
private fun StorageSection(snapshot: AccountSnapshot, reduceMotion: Boolean) {
    SectionLabel(stringResource(R.string.settings_storage_title))
    HyperGroup {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            val used = snapshot.usedBytes
            val total = snapshot.totalBytes
            val remaining = snapshot.remainingBytes

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = used?.let { readableSize(it) }
                        ?: stringResource(R.string.settings_account_none),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Hyper.Text,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = total?.let { stringResource(R.string.settings_storage_total, readableSize(it)) }
                        ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Hyper.Text2,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            if (snapshot.hasStorageNumbers) {
                Spacer(Modifier.height(14.dp))
                val description = used?.let { usedValue ->
                    val line = stringResource(
                        R.string.settings_storage_used,
                        readableSize(usedValue),
                    )
                    if (remaining != null) {
                        "$line · " + stringResource(
                            R.string.settings_storage_left,
                            readableSize(remaining),
                        )
                    } else {
                        line
                    }
                } ?: stringResource(R.string.settings_account_storage_unknown)
                StorageBar(
                    fraction = snapshot.usedFraction,
                    reduceMotion = reduceMotion,
                    description = description,
                )
                if (remaining != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(
                            R.string.settings_storage_left,
                            readableSize(remaining),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Hyper.Text2,
                    )
                }
            }

            if (total == null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.settings_storage_missing),
                    style = MaterialTheme.typography.bodySmall,
                    color = Hyper.Text3,
                )
            }

            Spacer(Modifier.height(10.dp))
            val files = snapshot.fileCount
            val folders = snapshot.folderCount
            Text(
                text = if (files != null && folders != null) {
                    stringResource(R.string.settings_account_counts, files, folders)
                } else {
                    stringResource(R.string.settings_account_counts_missing)
                },
                style = MaterialTheme.typography.bodySmall,
                color = Hyper.Text3,
            )
        }
    }
}

@Composable
private fun BreakdownSection(snapshot: AccountSnapshot, reduceMotion: Boolean) {
    SectionLabel(stringResource(R.string.settings_account_breakdown_title))
    HyperGroup {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            if (snapshot.breakdown.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_account_breakdown_missing),
                    style = MaterialTheme.typography.bodySmall,
                    color = Hyper.Text3,
                )
            } else {
                val description = snapshot.breakdown.joinToString(
                    separator = ", ",
                    prefix = stringResource(R.string.settings_account_breakdown_title) + ": ",
                ) { slice -> "${slice.label} ${readableSize(slice.bytes)}" }
                TypeBreakdownChart(
                    slices = snapshot.breakdown,
                    reduceMotion = reduceMotion,
                    description = description,
                )
                Spacer(Modifier.height(16.dp))
                BreakdownLegend(snapshot.breakdown)
            }
        }
    }
}

/* ------------------------------- identitas --------------------------- */

@Composable
private fun IdentitySection(session: GSession, snapshot: AccountSnapshot?) {
    SectionLabel(stringResource(R.string.settings_account_details))
    val email = snapshot?.email?.takeIf { it.isNotBlank() } ?: session.email
    val tier = snapshot?.tier?.takeIf { it.isNotBlank() } ?: session.tier
    val root = snapshot?.rootFolder?.takeIf { it.isNotBlank() } ?: session.rootFolder
    val id = snapshot?.accountId?.takeIf { it.isNotBlank() } ?: session.accountId

    HyperGroup {
        IdentityRow(
            label = stringResource(R.string.settings_account_email),
            value = email,
        )
        IdentityRow(
            label = stringResource(R.string.settings_account_tier_label),
            value = tier,
        )
        IdentityRow(
            label = stringResource(R.string.settings_account_root),
            value = root,
        )
        IdentityRow(
            label = stringResource(R.string.settings_account_id),
            value = id,
        )
    }
}

@Composable
private fun IdentityRow(label: String, value: String) {
    HyperRow(
        title = label,
        subtitle = value.ifBlank { null },
        value = if (value.isBlank()) stringResource(R.string.settings_account_none) else null,
    )
}

/* -------------------------------- keluar ----------------------------- */

@Composable
private fun SignOutSection(onSignOut: (Boolean) -> Unit) {
    val signOut = stringResource(R.string.settings_sign_out)
    val signOutHint = stringResource(R.string.settings_sign_out_hint)
    val remove = stringResource(R.string.settings_remove_account)
    val removeHint = stringResource(R.string.settings_remove_account_hint)

    SectionLabel(stringResource(R.string.settings_sign_out_title))
    HyperGroup {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            HoldToConfirm(
                label = signOut,
                hint = signOutHint,
                onConfirm = { onSignOut(false) },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_remove_account_body),
                style = MaterialTheme.typography.bodySmall,
                color = Hyper.Text2,
            )
            Spacer(Modifier.height(12.dp))
            HoldToConfirm(
                label = remove,
                hint = removeHint,
                onConfirm = { onSignOut(true) },
            )
        }
    }
}

/* --------------------------------- catatan --------------------------- */

@Composable
private fun NoteCard(text: String) {
    HyperCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Hyper.Text2,
            modifier = Modifier.padding(16.dp),
        )
    }
}
