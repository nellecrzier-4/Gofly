package com.nellecrzier.gofileexpressive.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nellecrzier.gofileexpressive.BuildConfig
import com.nellecrzier.gofileexpressive.GSession
import com.nellecrzier.gofileexpressive.GoFileException
import com.nellecrzier.gofileexpressive.GofileApi
import com.nellecrzier.gofileexpressive.R
import com.nellecrzier.gofileexpressive.ui.ExpressiveLoader
import com.nellecrzier.gofileexpressive.ui.Hyper
import com.nellecrzier.gofileexpressive.ui.HyperButton
import com.nellecrzier.gofileexpressive.ui.HyperCard
import com.nellecrzier.gofileexpressive.ui.HyperPage
import com.nellecrzier.gofileexpressive.ui.account.AccountScreen
import com.nellecrzier.gofileexpressive.ui.account.AccountTierBadge
import com.nellecrzier.gofileexpressive.ui.account.AccountUiState
import com.nellecrzier.gofileexpressive.ui.account.StorageBar
import com.nellecrzier.gofileexpressive.ui.account.accountErrorOf
import com.nellecrzier.gofileexpressive.ui.account.accountErrorRes
import com.nellecrzier.gofileexpressive.ui.account.accountSnapshotOf
import com.nellecrzier.gofileexpressive.ui.readableSize
import com.nellecrzier.gofileexpressive.vm.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Setelan lengkap: akun, tampilan, berkas, jaringan, keamanan, data, dan
 * tentang. Semua baris mengubah preferensi yang benar-benar dipakai layar lain.
 *
 * Parameter baru punya nilai bawaan supaya pemanggil lama (AppUi) tetap
 * kompilasi; induk aplikasi tinggal mengisi callback-nya.
 */
@Composable
internal fun SettingsScreen(
    session: GSession,
    reduceMotion: Boolean,
    onLogout: () -> Unit,
    onPickDownloadFolder: () -> Unit = {},
    onSendFeedback: () -> Unit = {},
    onExportLog: () -> Unit = {},
    onOpenLink: (String) -> Unit = {},
    onSignOut: (Boolean) -> Unit = { onLogout() },
) {
    val vm: SettingsViewModel = viewModel()
    val prefs by vm.state.collectAsStateWithLifecycle()
    val cacheBytes by vm.cacheBytes.collectAsStateWithLifecycle()
    val still = reduceMotion || prefs.motionReduced

    val scroll = rememberScrollState()
    var showAccount by rememberSaveable { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf("") }
    var reload by remember { mutableIntStateOf(0) }
    var accountState by remember(session.token, session.accountId) {
        mutableStateOf<AccountUiState>(AccountUiState.Loading)
    }
    var changelog by remember { mutableStateOf("") }

    // Detail akun dimuat sekali per kunjungan (dan saat "Coba lagi" ditekan).
    LaunchedEffect(session.token, session.accountId, reload) {
        vm.refreshCacheSize()
        changelog = withContext(Dispatchers.IO) { runCatching { vm.changelog() }.getOrDefault("") }
        if (session.accountId.isBlank()) {
            accountState = AccountUiState.Unavailable
            return@LaunchedEffect
        }
        accountState = AccountUiState.Loading
        accountState = withContext(Dispatchers.IO) {
            try {
                AccountUiState.Ready(
                    accountSnapshotOf(
                        GofileApi.getAccountDetails(session.token, session.accountId),
                        session,
                    ),
                )
            } catch (expired: GoFileException.AuthExpired) {
                AccountUiState.Expired
            } catch (error: Throwable) {
                AccountUiState.Failed(accountErrorOf(error))
            }
        }
    }

    // "Hapus akun" membersihkan data lokal sebelum sesi dibuang.
    val signOut: (Boolean) -> Unit = { wipeLocal ->
        if (wipeLocal) {
            vm.clearCache()
            vm.clearLibrary()
        }
        onSignOut(wipeLocal)
    }

    val actions = SettingsActions(
        onPickDownloadFolder = onPickDownloadFolder,
        onSendFeedback = onSendFeedback,
        onExportLog = onExportLog,
        onOpenLink = onOpenLink,
        onShowWhatsNew = {
            sheet = "whatsnew"
            vm.markWhatsNewSeen(BuildConfig.VERSION_CODE)
        },
        onShowLicense = { sheet = "license" },
    )

    if (showAccount) {
        AccountScreen(
            session = session,
            state = accountState,
            reduceMotion = still,
            onBack = { showAccount = false },
            onRetry = { reload += 1 },
            onSignOut = signOut,
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        HyperPage(
            title = stringResource(R.string.settings_title),
            subtitle = stringResource(R.string.settings_subtitle),
            scroll = scroll,
            reduceMotion = still,
        ) {
            SettingsAccountCard(
                session = session,
                state = accountState,
                reduceMotion = still,
                onOpen = { showAccount = true },
                onRetry = { reload += 1 },
                onSignIn = { signOut(false) },
            )

            SettingsAppearanceSection(prefs = prefs, vm = vm, reduceMotion = still)
            SettingsFilesSection(prefs = prefs, vm = vm, reduceMotion = still)
            SettingsNetworkSection(prefs = prefs, vm = vm, reduceMotion = still, actions = actions)
            SettingsSecuritySection(prefs = prefs, vm = vm, actions = actions)
            SettingsDataSection(
                prefs = prefs,
                vm = vm,
                cacheBytes = cacheBytes,
                actions = actions,
            )
            SettingsAboutSection(actions = actions)
            Spacer(Modifier.height(8.dp))
        }

        when (sheet) {
            "whatsnew" -> WhatsNewSheet(
                version = BuildConfig.VERSION_NAME,
                text = changelog,
                onDismiss = { sheet = "" },
            )

            "license" -> LicenseSheet(
                onDismiss = { sheet = "" },
                onOpenLink = onOpenLink,
            )
        }
    }
}

/* ------------------------------ kartu akun --------------------------- */

/**
 * Kartu akun di puncak Setelan: email, tier, bilah penyimpanan, dan pintu ke
 * [AccountScreen]. Saat sesi berakhir kartu berganti menjadi ajakan masuk lagi.
 */
@Composable
private fun SettingsAccountCard(
    session: GSession,
    state: AccountUiState,
    reduceMotion: Boolean,
    onOpen: () -> Unit,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
) {
    val snapshot = (state as? AccountUiState.Ready)?.snapshot
    val email = snapshot?.email?.takeIf { it.isNotBlank() }
        ?: session.email.takeIf { it.isNotBlank() }
    val tier = snapshot?.tier?.takeIf { it.isNotBlank() }
        ?: session.tier.takeIf { it.isNotBlank() }
    val openLabel = stringResource(R.string.settings_account_expand)

    HyperCard(
        fill = Hyper.AccentSoft,
        border = Hyper.AccentEdge,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                role = Role.Button,
                onClickLabel = openLabel,
                onClick = onOpen,
            ),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Hyper.Card,
                    modifier = Modifier.size(46.dp),
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
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = email ?: stringResource(R.string.settings_account_guest),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Hyper.Text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
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
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Hyper.Text3,
                    modifier = Modifier.size(20.dp),
                )
            }

            when (state) {
                AccountUiState.Loading -> {
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ExpressiveLoader(size = 22.dp, color = Hyper.Accent, reduceMotion = reduceMotion)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.settings_account_loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = Hyper.Text2,
                        )
                    }
                }

                AccountUiState.Expired -> {
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = Hyper.Accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_account_expired_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = Hyper.Text,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.settings_account_expired_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = Hyper.Text2,
                    )
                    Spacer(Modifier.height(12.dp))
                    HyperButton(
                        text = stringResource(R.string.settings_account_sign_in),
                        icon = Icons.AutoMirrored.Filled.Logout,
                        onClick = onSignIn,
                        height = 48.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is AccountUiState.Failed -> {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(accountErrorRes(state.error)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Hyper.Text2,
                    )
                    Spacer(Modifier.height(10.dp))
                    HyperButton(
                        text = stringResource(R.string.settings_account_retry),
                        onClick = onRetry,
                        primary = false,
                        height = 46.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                AccountUiState.Unavailable -> {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.settings_account_no_details),
                        style = MaterialTheme.typography.bodySmall,
                        color = Hyper.Text2,
                    )
                }

                is AccountUiState.Ready -> {
                    val used = state.snapshot.usedBytes
                    val remaining = state.snapshot.remainingBytes
                    Spacer(Modifier.height(14.dp))
                    if (used != null) {
                        val line = buildString {
                            append(stringResource(R.string.settings_account_storage_used, readableSize(used)))
                            if (remaining != null) {
                                append(' ')
                                append(stringResource(R.string.settings_account_storage_remaining, readableSize(remaining)))
                            }
                        }
                        StorageBar(
                            fraction = state.snapshot.usedFraction,
                            reduceMotion = reduceMotion,
                            description = line,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = Hyper.Text2,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.settings_account_storage_unknown),
                            style = MaterialTheme.typography.bodySmall,
                            color = Hyper.Text2,
                        )
                    }
                }
            }
        }
    }
}

/** Callback yang harus disambungkan induk; semuanya punya nilai bawaan kosong. */
internal class SettingsActions(
    val onPickDownloadFolder: () -> Unit = {},
    val onSendFeedback: () -> Unit = {},
    val onExportLog: () -> Unit = {},
    val onOpenLink: (String) -> Unit = {},
    val onShowWhatsNew: () -> Unit = {},
    val onShowLicense: () -> Unit = {},
)

/** Latar rambut tipis yang dipakai bagian Data/Akun sebagai pemisah. */
@Composable
internal fun SettingsDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(0.8.dp)
            .background(Hyper.Hairline),
    )
}
