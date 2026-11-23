@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.nellecrzier.gofileexpressive.ui.settings

import com.nellecrzier.gofileexpressive.ui.motion.staggeredEnter
import com.nellecrzier.gofileexpressive.ui.components.rememberTabEntered
import com.nellecrzier.gofileexpressive.ui.components.AppLoadingIndicator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import com.nellecrzier.gofileexpressive.ui.account.AccountScreen
import com.nellecrzier.gofileexpressive.ui.account.AccountTierBadge
import com.nellecrzier.gofileexpressive.ui.account.AccountUiState
import com.nellecrzier.gofileexpressive.ui.account.StorageBar
import com.nellecrzier.gofileexpressive.ui.account.accountErrorOf
import com.nellecrzier.gofileexpressive.ui.account.accountErrorRes
import com.nellecrzier.gofileexpressive.ui.account.accountSnapshotOf
import com.nellecrzier.gofileexpressive.ui.debug.MotionPlayground
import com.nellecrzier.gofileexpressive.ui.motion.motionScheme
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
    val shown = rememberTabEntered()
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

    // Pintu debug ke laboratorium gerak. Signature layar ini sengaja tidak
    // berubah: panorama dibuka sebagai hamparan penuh di dalam Setelan, bukan
    // lewat callback baru dari AppUi.
    var showPlayground by remember { mutableStateOf(false) }
    val onOpenMotionPlayground: () -> Unit = { showPlayground = true }
    BackHandler(enabled = showPlayground) { showPlayground = false }

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
        // Bilah atas ekspresif: mengecil saat konten digulir, lalu berhenti
        // tergulir penuh di tinggi baris tunggal. Warna kontainernya mengikuti
        // posisi gulir, dibaca lewat derivedStateOf, bukan state tambahan.
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val collapsed by remember(scrollBehavior) {
            derivedStateOf { scrollBehavior.state.collapsedFraction >= 0.5f }
        }
        val barColorSpec: AnimationSpec<Color> =
            if (still) tween(0) else motionScheme.defaultEffectsSpec<Color>()
        val barContainer by animateColorAsState(
            targetValue = if (collapsed) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                Color.Transparent
            },
            animationSpec = barColorSpec,
            label = "settingsTopBarContainer",
        )

        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                LargeFlexibleTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.settings_title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    subtitle = {
                        Text(
                            text = stringResource(R.string.settings_subtitle),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = barContainer,
                        scrolledContainerColor = barContainer,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { innerPadding ->
            // Di layar lebar isi dibatasi 720.dp dan diletakkan di tengah; gulir
            // vertikal tetap milik kolom ini supaya bilah atas ikut mengecil.
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
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
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
                        Box(Modifier.staggeredEnter(0, still, shown)) { SettingsAccountCard(
                            session = session,
                            state = accountState,
                            reduceMotion = still,
                            onOpen = { showAccount = true },
                            onRetry = { reload += 1 },
                            onSignIn = { signOut(false) },
                        ) }

                        Box(Modifier.staggeredEnter(1, still, shown)) { SettingsAppearanceSection(prefs = prefs, vm = vm, reduceMotion = still) }
                        Box(Modifier.staggeredEnter(2, still, shown)) { SettingsFilesSection(prefs = prefs, vm = vm, reduceMotion = still) }
                        Box(Modifier.staggeredEnter(3, still, shown)) { SettingsNetworkSection(prefs = prefs, vm = vm, reduceMotion = still, actions = actions) }
                        Box(Modifier.staggeredEnter(4, still, shown)) { SettingsSecuritySection(prefs = prefs, vm = vm, actions = actions) }
                        Box(Modifier.staggeredEnter(5, still, shown)) {
                            SettingsDataSection(
                                prefs = prefs,
                                vm = vm,
                                cacheBytes = cacheBytes,
                                actions = actions,
                            )
                        }
                        Box(Modifier.staggeredEnter(6, still, shown)) { SettingsAboutSection(actions = actions) }

                        // Hanya di build debug, dan hanya baris ini yang ditambah.
                        if (BuildConfig.DEBUG) {
                            Spacer(Modifier.height(16.dp))
                            Surface(
                                shape = RoundedCornerShape(26.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                SettingsActionRow(
                                    title = "Motion playground",
                                    subtitle = "Demonstrasi gerak Material 3 Expressive (build debug)",
                                    icon = Icons.Filled.PlayArrow,
                                    iconTint = MaterialTheme.colorScheme.tertiary,
                                    onClick = onOpenMotionPlayground,
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
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

        // Hamparan penuh di atas Setelan, bukan layar baru di AppUi.
        if (showPlayground) {
            MotionPlayground(onClose = { showPlayground = false })
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

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
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
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.size(46.dp),
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
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = email ?: stringResource(R.string.settings_account_guest),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }

            when (state) {
                AccountUiState.Loading -> {
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppLoadingIndicator(
                            size = 22.dp,
                            color = MaterialTheme.colorScheme.primary,
                            reduceMotion = reduceMotion,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.settings_account_loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                AccountUiState.Expired -> {
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_account_expired_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.settings_account_expired_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onSignIn,
                        shape = RoundedCornerShape(percent = 50),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
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

                is AccountUiState.Failed -> {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(accountErrorRes(state.error)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = onRetry,
                        shape = RoundedCornerShape(percent = 50),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                    ) {
                        Text(stringResource(R.string.settings_account_retry))
                    }
                }

                AccountUiState.Unavailable -> {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.settings_account_no_details),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.settings_account_storage_unknown),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

/** Garis pemisah tipis yang dipakai bagian Data/Akun sebagai pemisah. */
@Composable
internal fun SettingsDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(0.8.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}
