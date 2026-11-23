@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.nellecrzier.gofileexpressive.ui.account

import com.nellecrzier.gofileexpressive.ui.components.AppLoadingIndicator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nellecrzier.gofileexpressive.GSession
import com.nellecrzier.gofileexpressive.R
import com.nellecrzier.gofileexpressive.ui.components.HoldToConfirmButton
import com.nellecrzier.gofileexpressive.ui.components.animatedConnectedShape
import com.nellecrzier.gofileexpressive.ui.components.connectedShape
import com.nellecrzier.gofileexpressive.ui.motion.motionScheme
import com.nellecrzier.gofileexpressive.ui.motion.rememberReducedMotion
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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    // Posisi gulir dibaca sebagai satu nilai turunan; hanya dua keadaan yang
    // dipakai, jadi warna kontainer tidak perlu disimpan di state mana pun.
    val collapsed by remember(scrollBehavior) {
        derivedStateOf { scrollBehavior.state.collapsedFraction >= 0.5f }
    }
    val barColorSpec: AnimationSpec<Color> =
        if (reduceMotion) tween(0) else motionScheme.defaultEffectsSpec<Color>()
    val barContainer by animateColorAsState(
        targetValue = if (collapsed) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            Color.Transparent
        },
        animationSpec = barColorSpec,
        label = "accountTopBarContainer",
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_account_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                subtitle = {
                    Text(
                        text = stringResource(R.string.settings_account_subtitle),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_account_back),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = onRetry,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.settings_account_retry),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = barContainer,
                    scrolledContainerColor = barContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        // Isi dibatasi 720.dp dan di tengah pada layar lebar; gulir tetap
        // dimiliki kolom ini supaya bilah atas ikut mengecil.
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.height(8.dp))
                Column(
                        Modifier.padding(
                            top = 4.dp,
                            bottom = 24.dp + com.nellecrzier.gofileexpressive.ui.components.LocalBottomChromeHeight.current,
                        ),
                    ) {
                    AccountHeader(session, state)

                    when (state) {
                        AccountUiState.Loading -> AccountLoadingCard(reduceMotion)

                        is AccountUiState.Failed -> {
                            Spacer(Modifier.height(12.dp))
                            AccountNotice(stringResource(accountErrorRes(state.error)))
                            Spacer(Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = onRetry,
                                shape = RoundedCornerShape(percent = 50),
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                            ) {
                                Text(stringResource(R.string.settings_account_retry))
                            }
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

                    IdentitySection(session, snapshot, reduceMotion)
                    SignOutSection(onSignOut)
                }
            }
        }
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

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.CloudQueue,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
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
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2,
                    )
                    Spacer(Modifier.height(3.dp))
                    if (tier != null) {
                        AccountTierBadge(tier)
                    } else {
                        Text(
                            text = stringResource(R.string.settings_account_guest),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
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
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppLoadingIndicator(
                size = 28.dp,
                color = MaterialTheme.colorScheme.primary,
                reduceMotion = reduceMotion,
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = stringResource(R.string.settings_account_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExpiredSessionCard(onSignIn: () -> Unit) {
    Spacer(Modifier.height(14.dp))
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.settings_account_expired_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.settings_account_expired_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSignIn,
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_account_sign_in))
            }
        }
    }
}

/* ---------------------------- penyimpanan ---------------------------- */

@Composable
private fun StorageSection(snapshot: AccountSnapshot, reduceMotion: Boolean) {
    SectionHeader(stringResource(R.string.settings_storage_title))
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
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
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = total?.let { stringResource(R.string.settings_storage_total, readableSize(it)) }
                        ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (total == null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.settings_storage_missing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun BreakdownSection(snapshot: AccountSnapshot, reduceMotion: Boolean) {
    SectionHeader(stringResource(R.string.settings_account_breakdown_title))
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            if (snapshot.breakdown.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_account_breakdown_missing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
private fun IdentitySection(
    session: GSession,
    snapshot: AccountSnapshot?,
    reduceMotion: Boolean = rememberReducedMotion(),
) {
    SectionHeader(stringResource(R.string.settings_account_details))
    val email = snapshot?.email?.takeIf { it.isNotBlank() } ?: session.email
    val tier = snapshot?.tier?.takeIf { it.isNotBlank() } ?: session.tier
    val root = snapshot?.rootFolder?.takeIf { it.isNotBlank() } ?: session.rootFolder
    val id = snapshot?.accountId?.takeIf { it.isNotBlank() } ?: session.accountId

    // Grup baris identitas: tiap baris adalah Surface tonal sendiri dengan jarak
    // 2.dp, sudut luar hanya di baris pertama/terakhir (daftar bersambung).
    val rows = listOf(
        stringResource(R.string.settings_account_email) to email,
        stringResource(R.string.settings_account_tier_label) to tier,
        stringResource(R.string.settings_account_root) to root,
        stringResource(R.string.settings_account_id) to id,
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        rows.forEachIndexed { index, (label, value) ->
            Surface(
                shape = if (reduceMotion) {
                    connectedShape(index, rows.size)
                } else {
                    animatedConnectedShape(index, rows.size, reduceMotion)
                },
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IdentityRow(label = label, value = value)
            }
        }
    }
}

@Composable
private fun IdentityRow(label: String, value: String) {
    val supporting = value.ifBlank { null }
    ListItem(
        headlineContent = {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = supporting?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        trailingContent = {
            if (value.isBlank()) {
                ValueBadge(stringResource(R.string.settings_account_none))
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    )
}

/* -------------------------------- keluar ----------------------------- */

@Composable
private fun SignOutSection(onSignOut: (Boolean) -> Unit) {
    val signOut = stringResource(R.string.settings_sign_out)
    val signOutHint = stringResource(R.string.settings_sign_out_hint)
    val remove = stringResource(R.string.settings_remove_account)
    val removeHint = stringResource(R.string.settings_remove_account_hint)

    SectionHeader(stringResource(R.string.settings_sign_out_title))
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            HoldToConfirmButton(
                label = signOut,
                hint = signOutHint,
                onConfirm = { onSignOut(false) },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_remove_account_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            HoldToConfirmButton(
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
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}

/** Pemberitahuan galat: ikon aksen + teks di permukaan netral. */
@Composable
private fun AccountNotice(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Judul bagian tanpa batang aksen, memakai token tipografi Material 3. */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
    )
}

/** Pil nilai netral untuk isian yang kosong. */
@Composable
private fun ValueBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
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
