@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.nellecrzier.gofileexpressive.ui

import android.net.Uri
import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nellecrzier.gofileexpressive.BuildConfig
import com.nellecrzier.gofileexpressive.GItem
import com.nellecrzier.gofileexpressive.R
import com.nellecrzier.gofileexpressive.GSession
import com.nellecrzier.gofileexpressive.GoFileException
import com.nellecrzier.gofileexpressive.GofileApi
import com.nellecrzier.gofileexpressive.MainActivity
import com.nellecrzier.gofileexpressive.SessionStore
import com.nellecrzier.gofileexpressive.TransferInfo
import com.nellecrzier.gofileexpressive.TransferStore
import com.nellecrzier.gofileexpressive.ui.components.AppLoadingIndicator
import com.nellecrzier.gofileexpressive.ui.components.MorphIcon
import com.nellecrzier.gofileexpressive.ui.folder.FolderScreen
import com.nellecrzier.gofileexpressive.ui.motion.motionScheme
import com.nellecrzier.gofileexpressive.ui.motion.rememberReducedMotion
import com.nellecrzier.gofileexpressive.ui.security.AppLockGate
import com.nellecrzier.gofileexpressive.ui.home.HomeScreen
import com.nellecrzier.gofileexpressive.ui.login.LoginScreen
import com.nellecrzier.gofileexpressive.ui.settings.SettingsScreen
import com.nellecrzier.gofileexpressive.ui.transfer.TransferPanel
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.nellecrzier.gofileexpressive.vm.SettingsViewModel
import com.nellecrzier.gofileexpressive.vm.SessionViewModel
import com.nellecrzier.gofileexpressive.vm.SessionEvent
import com.nellecrzier.gofileexpressive.vm.FolderViewModel
import com.nellecrzier.gofileexpressive.vm.FolderEvent
import com.nellecrzier.gofileexpressive.ui.library.LibraryScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class Crumb(val id: String, val name: String)

private data class AppTab(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val outlined: ImageVector,
)

private val APP_TABS = listOf(
    AppTab("home", "Beranda", Icons.Filled.Home, Icons.Outlined.Home),
    AppTab("folder", "File", Icons.Filled.Folder, Icons.Outlined.Folder),
    AppTab("library", "Pustaka", Icons.Filled.Star, Icons.Outlined.Star),
    AppTab("settings", "Setelan", Icons.Filled.Settings, Icons.Outlined.Settings),
)

private val TAB_ORDER = APP_TABS.map { it.key }

internal fun readableSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0) "$bytes B" else String.format("%.1f %s", value, units[unit])
}

/* ================================ aplikasi ================================ */

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GofileApp(activity: MainActivity) {
    val context = LocalContext.current
    val systemReduceMotion = rememberReducedMotion()

    // ViewModel dibuat di luar tema supaya preferensi (tema, aksen, skala teks)
    // sudah tersedia saat palet dipilih.
    val sessionVm: SessionViewModel = viewModel()
    val folderVm: FolderViewModel = viewModel()
    val settingsVm: SettingsViewModel = viewModel()
    val prefs by settingsVm.state.collectAsStateWithLifecycle()
    val reduceMotion = systemReduceMotion || prefs.motionReduced
    val darkTheme = when (prefs.themeMode) {
        com.nellecrzier.gofileexpressive.ThemeMode.DARK -> true
        com.nellecrzier.gofileexpressive.ThemeMode.LIGHT -> false
        com.nellecrzier.gofileexpressive.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    // Ikon status/navigation bar mengikuti tema aplikasi, bukan hanya tema sistem.
    LaunchedEffect(darkTheme) {
        androidx.core.view.WindowCompat.getInsetsController(
            activity.window,
            activity.window.decorView,
        ).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
    val lockRevision by activity.lockRevision.collectAsState()

    GofileTheme(
        mode = prefs.themeMode,
        accent = prefs.accent,
        dynamic = prefs.dynamicColor,
        amoled = prefs.amoled,
        fontScale = prefs.fontScale,
        reduceMotion = reduceMotion,
    ) {
        AppLockGate(enabled = prefs.appLock, lockRevision = lockRevision) {
        val permissionRevision by activity.permissionRevision.collectAsState()

        val sessionState by sessionVm.state.collectAsStateWithLifecycle()
        val folderState by folderVm.state.collectAsStateWithLifecycle()

        val session = sessionState.session
        val loginStep = sessionState.step
        val checkingSession = sessionState.checking
        val busy = sessionState.busy || folderState.busy
        val stack = folderState.crumbs
        val items = folderState.items

        var notice by remember { mutableStateOf<String?>(null) }
        var noticeIsError by remember { mutableStateOf(true) }
        var showCreateFolder by remember { mutableStateOf(false) }
        var newFolderName by remember { mutableStateOf("") }
        var currentTab by rememberSaveable { mutableStateOf("home") }
        val handledErrors = remember { mutableSetOf<String>() }

        fun showNotice(text: String, error: Boolean = true) {
            notice = text
            noticeIsError = error
        }

        fun createFolder() {
            val name = newFolderName.trim()
            if (name.isBlank()) return
            folderVm.createFolder(session.token, name)
            showCreateFolder = false
            newFolderName = ""
        }

        fun openFolderTab() {
            currentTab = "folder"
            if (items.isEmpty()) folderVm.openRoot(session.token, session.rootFolder)
        }

        // Satu jalur pemilihan tab untuk bilah bawah maupun rel samping.
        fun selectTab(key: String) {
            currentTab = key
            if (key == "folder") openFolderTab()
        }

        LaunchedEffect(Unit) {
            sessionVm.events.collect { event ->
                when (event) {
                    is SessionEvent.Message -> showNotice(event.text, event.error)
                    SessionEvent.LoggedIn -> {
                        currentTab = "home"
                        val current = sessionVm.state.value.session
                        folderVm.openRoot(current.token, current.rootFolder)
                    }
                    SessionEvent.LoggedOut -> {
                        currentTab = "home"
                        showCreateFolder = false
                        newFolderName = ""
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            folderVm.events.collect { event ->
                when (event) {
                    is FolderEvent.Message -> showNotice(event.text, event.error)
                    FolderEvent.SessionExpired -> sessionVm.logout(expired = true)
                }
            }
        }

        LaunchedEffect(Unit) {
            TransferStore.transfers.collect { list ->
                list.filter { it.state == "error" && it.id !in handledErrors }.forEach {
                    handledErrors += it.id
                    activity.toast(it.message.ifBlank { "Transfer gagal." })
                }
            }
        }

        LaunchedEffect(Unit) {
            activity.loginLinkFlow.collect { token ->
                if (token.isNullOrBlank()) return@collect
                sessionVm.adoptLoginLink(token)
                activity.loginLinkFlow.value = null
            }
        }

        val folderLabel = stringResource(R.string.tab_folder)

        // Tautan folder publik (gofile.io/d/<id>) dibuka native, bukan di browser.
        LaunchedEffect(Unit) {
            activity.deepLinkFolder.collect { id ->
                if (id.isNullOrBlank()) return@collect
                val current = sessionVm.state.value.session
                if (current.token.isNotBlank()) {
                    folderVm.openDirect(
                        current.token,
                        GItem(
                            id = id,
                            name = folderLabel,
                            type = "folder",
                            size = 0,
                            link = "",
                            parentFolder = "",
                            mimetype = "",
                        ),
                    )
                    currentTab = "folder"
                } else {
                    showNotice("Masuk dulu untuk membuka tautan folder.", error = true)
                }
                activity.deepLinkFolder.value = null
            }
        }

        // Berkas yang dibagikan aplikasi lain diunggah ke folder yang sedang dibuka.
        LaunchedEffect(Unit) {
            activity.incomingShare.collect { uris ->
                if (uris.isEmpty()) return@collect
                val current = sessionVm.state.value.session
                val folderId = folderVm.state.value.currentFolderId ?: current.rootFolder
                if (current.token.isBlank()) {
                    showNotice("Masuk dulu untuk mengunggah berkas.", error = true)
                } else {
                    currentTab = "folder"
                    activity.uploadUris(uris, folderId, current.token)
                    showNotice("Mengunggah ${uris.size} berkas…", error = false)
                }
                activity.incomingShare.value = emptyList()
            }
        }

        // Pintasan aplikasi (tekan lama ikon): unggah atau transfer.
        LaunchedEffect(Unit) {
            activity.shortcutRequest.collect { request ->
                if (request.isNullOrBlank()) return@collect
                val current = sessionVm.state.value.session
                when (request) {
                    "upload" -> {
                        val folderId = folderVm.state.value.currentFolderId ?: current.rootFolder
                        if (current.token.isNotBlank()) {
                            activity.pickFileForUpload(folderId, current.token)
                        }
                    }
                    "transfers" -> currentTab = "home"
                }
                activity.shortcutRequest.value = null
            }
        }

        // Bahasa per-aplikasi: aktivitas dibuat ulang sekali setelah pilihan berubah.
        // Jendela Undo mengikuti Setelan (3–15 detik).
        LaunchedEffect(prefs.undoSeconds) {
            folderVm.undoWindowMs = prefs.undoSeconds * 1000L
        }

        // Folder unduhan SAF yang dipilih dari Setelan.
        LaunchedEffect(Unit) {
            activity.downloadTreeResult.collect { uri ->
                if (!uri.isNullOrBlank()) {
                    settingsVm.setDownloadTree(uri)
                    activity.downloadTreeResult.value = null
                }
            }
        }

        val initialLanguage = remember { prefs.language }
        LaunchedEffect(prefs.language) {
            if (prefs.language != initialLanguage) activity.recreate()
        }

        // Pemberitahuan hilang sendiri setelah beberapa detik.
        LaunchedEffect(notice) {
            if (notice != null) {
                delay(4600)
                notice = null
            }
        }

        val canGoBack = loginStep == "ready" && (currentTab != "home" || stack.size > 1)
        var backProgress by remember { mutableFloatStateOf(0f) }

        fun goBack() {
            if (currentTab == "folder" && folderVm.back(session.token)) return
            currentTab = "home"
        }

        // Gestur back prediktif: halaman mengikuti jari sebelum benar-benar keluar.
        PredictiveBackHandler(enabled = canGoBack) { events ->
            try {
                events.collect { event -> backProgress = event.progress }
                goBack()
            } finally {
                backProgress = 0f
            }
        }

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            // Layar lebar (tablet, lipat terbuka) memakai rel di sisi awal; layar
            // ringkas tetap memakai bilah navigasi pendek di tepi bawah.
            val wideLayout = maxWidth >= 600.dp
            val showNavigation = loginStep == "ready" && !checkingSession

            val rootFade = motionScheme.fastEffectsSpec<Float>()
            val rootScale = motionScheme.defaultSpatialSpec<Float>()
            Row(Modifier.fillMaxSize()) {
                if (showNavigation && wideLayout) {
                    AppNavigationRail(
                        currentTab = currentTab,
                        reduceMotion = reduceMotion,
                        onSelect = { key -> selectTab(key) },
                    )
                }

                // Rel duduk sebagai saudara konten, jadi konten bergeser sejauh lebar
                // rel (padding awal) dan toast tetap rata pada area konten.
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    AnimatedContent(
                        targetState = loginStep == "ready",
                        transitionSpec = {
                            if (reduceMotion) {
                                fadeIn(tween(100)) togetherWith fadeOut(tween(100))
                            } else {
                                (fadeIn(rootFade) + scaleIn(rootScale, initialScale = 0.98f)) togetherWith
                                    (fadeOut(rootFade) + scaleOut(rootScale, targetScale = 1.02f))
                            }
                        },
                        label = "appRoot",
                    ) { ready ->
                    if (ready) {
                        if (checkingSession) {
                            Column(
                                Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                AppLoadingIndicator(
                                    size = 52.dp,
                                    reduceMotion = reduceMotion,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    stringResource(R.string.session_preparing),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            Box(Modifier.fillMaxSize()) {
                                // Halaman bergeser mengikuti gestur back prediktif.
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            val p = backProgress
                                            if (p > 0f) {
                                                translationX = 36.dp.toPx() * p
                                                scaleX = 1f - 0.05f * p
                                                scaleY = 1f - 0.05f * p
                                                transformOrigin = TransformOrigin(1f, 0.5f)
                                            }
                                        },
                                ) {
                                    val tabFade = motionScheme.fastEffectsSpec<Float>()
                                    val tabScale = motionScheme.defaultSpatialSpec<Float>()
                                    val tabSlide = motionScheme.defaultSpatialSpec<IntOffset>()
                                    AnimatedContent(
                                        targetState = currentTab,
                                        label = "tabContent",
                                        transitionSpec = {
                                            if (reduceMotion) {
                                                fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                                            } else {
                                                val forward = TAB_ORDER.indexOf(targetState) >= TAB_ORDER.indexOf(initialState)
                                                (
                                                    fadeIn(tabFade) +
                                                        scaleIn(tabScale, initialScale = 0.97f) +
                                                        slideInHorizontally(tabSlide) { full ->
                                                            if (forward) full / 12 else -full / 12
                                                        }
                                                    ) togetherWith (
                                                    fadeOut(tabFade) +
                                                        scaleOut(tabScale, targetScale = 0.99f) +
                                                        slideOutHorizontally(tabSlide) { full ->
                                                            if (forward) -full / 16 else full / 16
                                                        }
                                                    )
                                            }
                                        },
                                    ) { tab ->
                                        when (tab) {
                                            "folder" -> FolderScreen(
                                                vm = folderVm,
                                                state = folderState,
                                                prefs = prefs,
                                                token = session.token,
                                                reduceMotion = reduceMotion,
                                                activity = activity,
                                                onNewFolder = {
                                                    newFolderName = ""
                                                    showCreateFolder = true
                                                },
                                            )

                                            "library" -> LibraryScreen(
                                                vm = folderVm,
                                                prefs = prefs,
                                                token = session.token,
                                                reduceMotion = reduceMotion,
                                                activity = activity,
                                                onOpenFolder = { currentTab = "folder" },
                                            )

                                            "settings" -> SettingsScreen(
                                                session = session,
                                                reduceMotion = reduceMotion,
                                                onLogout = { sessionVm.logout() },
                                                onPickDownloadFolder = { activity.pickDownloadFolder() },
                                                onSendFeedback = { activity.sendFeedback() },
                                                onExportLog = { activity.exportLog() },
                                                onOpenLink = { url -> activity.openUrlExternally(url) },
                                                onSignOut = { wipe ->
                                                    if (wipe) {
                                                        settingsVm.clearCache()
                                                        settingsVm.clearLibrary()
                                                        folderVm.clearLibrary()
                                                    }
                                                    sessionVm.logout()
                                                },
                                            )

                                            else -> HomeScreen(
                                                session = session,
                                                items = items,
                                                busy = busy,
                                                reduceMotion = reduceMotion,
                                                activity = activity,
                                                permissionRevision = permissionRevision,
                                                onOpenFolder = { openFolderTab() },
                                                onUpload = {
                                                    stack.lastOrNull()?.let {
                                                        activity.pickFileForUpload(it.id, session.token)
                                                    }
                                                },
                                                onNewFolder = {
                                                    newFolderName = ""
                                                    showCreateFolder = true
                                                },
                                                onRefresh = { folderVm.refresh(session.token) },
                                            )
                                        }
                                    }
                                }
                            }

                            // Lebar ringkas: bilah navigasi pendek Expressive di tepi bawah.
                            if (!wideLayout) {
                                AppShortNavigationBar(
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                    currentTab = currentTab,
                                    reduceMotion = reduceMotion,
                                    onSelect = { key -> selectTab(key) },
                                )
                            }
                        }
                    } else {
                        LoginScreen(
                            step = loginStep,
                            email = sessionState.email,
                            tokenInput = sessionState.tokenInput,
                            busy = busy,
                            reduceMotion = reduceMotion,
                            onEmailChange = sessionVm::setEmail,
                            onTokenChange = sessionVm::setTokenInput,
                            onBackToEmail = {
                                sessionVm.backToEmail()
                                notice = null
                            },
                            onSend = { sessionVm.sendLoginLink(sessionState.email) },
                            onLogin = { raw -> sessionVm.loginWithToken(raw) },
                            onGuest = { sessionVm.loginAsGuest() },
                        )
                    }}

                    // Pil toast di atas bar navigasi.
                    AppToastOverlay(
                        visible = notice != null,
                        reduceMotion = reduceMotion,
                        bottomPadding = if (loginStep == "ready" && !checkingSession && !wideLayout) 92.dp else 16.dp,
                    ) {
                        notice?.let { text ->
                            AppToast(
                                text = text,
                                error = noticeIsError,
                                reduceMotion = reduceMotion,
                                onDismiss = { notice = null },
                            )
                        }
                    }
                }
            }
        }

        if (showCreateFolder) {
            AlertDialog(
                onDismissRequest = { showCreateFolder = false },
                shape = RoundedCornerShape(28.dp),
                title = { Text(stringResource(R.string.folder_new_folder)) },
                text = {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text(stringResource(R.string.folder_name_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { createFolder() },
                        enabled = newFolderName.isNotBlank() && !busy,
                    ) { Text(stringResource(R.string.action_create)) }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateFolder = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }

        }
    }
}

/* ============================= navigasi shell ============================= */

/**
 * Ikon tab dengan gerak seleksi Expressive: ikon terisi masuk dengan skala +
 * pudar, ikon garis keluar sambil mengecil. Label tab tetap di tempatnya.
 * Saat gerak diredupkan hanya silang-pudar singkat tanpa skala.
 */
@Composable
private fun AnimatedTabIcon(
    tab: AppTab,
    selected: Boolean,
    reduceMotion: Boolean,
) {
    val spatial = motionScheme.fastSpatialSpec<Float>()
    val effects = motionScheme.fastEffectsSpec<Float>()
    AnimatedContent(
        targetState = selected,
        transitionSpec = {
            if (reduceMotion) {
                fadeIn(tween(100)) togetherWith fadeOut(tween(100))
            } else {
                (fadeIn(effects) + scaleIn(spatial, initialScale = 0.72f)) togetherWith
                    (fadeOut(effects) + scaleOut(spatial, targetScale = 0.86f))
            }
        },
        contentAlignment = Alignment.Center,
        label = "tabIcon",
    ) { active ->
        Icon(
            imageVector = if (active) tab.icon else tab.outlined,
            contentDescription = tab.label,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Bilah navigasi pendek Expressive (lebar ringkas, < 600.dp). */
@Composable
private fun AppShortNavigationBar(
    currentTab: String,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    ShortNavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        arrangement = ShortNavigationBarArrangement.EqualWeight,
    ) {
        APP_TABS.forEach { tab ->
            ShortNavigationBarItem(
                selected = currentTab == tab.key,
                onClick = { onSelect(tab.key) },
                icon = { AnimatedTabIcon(tab, currentTab == tab.key, reduceMotion) },
                label = { Text(tab.label, maxLines = 1) },
            )
        }
    }
}

/** Rel navigasi di sisi awal untuk lebar lebar (>= 600.dp). */
@Composable
private fun AppNavigationRail(
    currentTab: String,
    reduceMotion: Boolean,
    onSelect: (String) -> Unit,
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        APP_TABS.forEach { tab ->
            NavigationRailItem(
                selected = currentTab == tab.key,
                onClick = { onSelect(tab.key) },
                icon = { AnimatedTabIcon(tab, currentTab == tab.key, reduceMotion) },
                label = { Text(tab.label, maxLines = 1) },
                alwaysShowLabel = true,
            )
        }
    }
}

/* ================================= toast ================================== */

/**
 * Lapisan toast di atas area konten. Dipisah dari layar induk supaya gestur dan
 * tata letak tidak memakai receiver RowScope; saat kosong tidak ada yang
 * menghalangi sentuhan karena hanya isi [AnimatedVisibility] yang digambar.
 */
@Composable
private fun AppToastOverlay(
    visible: Boolean,
    reduceMotion: Boolean,
    bottomPadding: Dp,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = if (reduceMotion) {
                fadeIn(tween(120))
            } else {
                val toastSlide = motionScheme.fastSpatialSpec<androidx.compose.ui.unit.IntOffset>()
                val toastFade = motionScheme.fastEffectsSpec<Float>()
                slideInVertically(toastSlide) { it / 2 } + fadeIn(toastFade)
            },
            exit = if (reduceMotion) {
                fadeOut(tween(100))
            } else {
                val toastSlide = motionScheme.fastSpatialSpec<androidx.compose.ui.unit.IntOffset>()
                val toastFade = motionScheme.fastEffectsSpec<Float>()
                slideOutVertically(toastSlide) { it / 3 } + fadeOut(toastFade)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = bottomPadding),
        ) {
            content()
        }
    }
}

@Composable
private fun AppToast(text: String, error: Boolean, reduceMotion: Boolean, onDismiss: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDismiss),
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MorphIcon(
                first = Icons.Filled.ErrorOutline,
                second = Icons.Filled.CheckCircle,
                showSecond = !error,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                reduceMotion = reduceMotion,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
