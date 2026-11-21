package com.nellecrzier.gofileexpressive.ui

import android.os.Build
import android.util.Patterns
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nellecrzier.gofileexpressive.BuildConfig
import com.nellecrzier.gofileexpressive.GSession
import com.nellecrzier.gofileexpressive.GoFileException
import com.nellecrzier.gofileexpressive.GofileApi
import com.nellecrzier.gofileexpressive.GItem
import com.nellecrzier.gofileexpressive.MainActivity
import com.nellecrzier.gofileexpressive.SessionStore
import com.nellecrzier.gofileexpressive.TransferInfo
import com.nellecrzier.gofileexpressive.TransferStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

private data class Crumb(val id: String, val name: String)

private val APP_TABS = listOf(
    HyperTab("home", "Beranda", Icons.Filled.Home),
    HyperTab("folder", "Folder", Icons.Filled.Folder),
    HyperTab("settings", "Setelan", Icons.Filled.Settings),
)

private val TAB_ORDER = APP_TABS.map { it.key }

private fun readableSize(bytes: Long): String {
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
    val reduceMotion = rememberReduceMotion()

    var themeMode by rememberSaveable { mutableStateOf("system") }
    var dynamicColor by rememberSaveable { mutableStateOf(true) }
    // HyperSpring hanya punya satu palet gelap: tema sistem diabaikan.
    val darkTheme = when (themeMode) {
        "light" -> false
        else -> true
    }

    GofileTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
        val scope = rememberCoroutineScope()
        val permissionRevision by activity.permissionRevision.collectAsState()

        var session by remember { mutableStateOf(SessionStore.load(context)) }
        var stack by remember { mutableStateOf(listOf<Crumb>()) }
        var items by remember { mutableStateOf(listOf<GItem>()) }
        var busy by remember { mutableStateOf(false) }
        var notice by remember { mutableStateOf<String?>(null) }
        var noticeIsError by remember { mutableStateOf(true) }
        var showCreateFolder by remember { mutableStateOf(false) }
        var newFolderName by remember { mutableStateOf("") }
        var pendingDelete by remember { mutableStateOf<GItem?>(null) }
        var loginStep by rememberSaveable { mutableStateOf(if (session.isLoggedIn) "ready" else "email") }
        var checkingSession by remember { mutableStateOf(session.isLoggedIn) }
        var currentTab by rememberSaveable { mutableStateOf("home") }
        var email by rememberSaveable { mutableStateOf("") }
        var tokenInput by rememberSaveable { mutableStateOf("") }
        val handledErrors = remember { mutableSetOf<String>() }

        fun showNotice(text: String, error: Boolean = true) {
            notice = text
            noticeIsError = error
        }

        fun report(error: Exception) {
            showNotice((error as? GoFileException)?.message ?: error.message ?: "Terjadi kesalahan.")
        }

        fun loadFolder(id: String) {
            scope.launch {
                busy = true
                notice = null
                try {
                    items = withContext(Dispatchers.IO) { GofileApi.getFolder(session.token, id) }
                } catch (error: Exception) {
                    report(error)
                } finally {
                    busy = false
                }
            }
        }

        fun createFolder() {
            val parent = stack.lastOrNull()?.id ?: return
            val name = newFolderName.trim()
            if (name.isBlank()) return
            scope.launch {
                busy = true
                notice = null
                try {
                    withContext(Dispatchers.IO) {
                        GofileApi.createFolder(session.token, parent, name)
                    }
                    items = withContext(Dispatchers.IO) { GofileApi.getFolder(session.token, parent) }
                    showCreateFolder = false
                    newFolderName = ""
                    showNotice("Folder \"$name\" dibuat.", error = false)
                } catch (error: Exception) {
                    report(error)
                } finally {
                    busy = false
                }
            }
        }

        fun deleteItem(item: GItem) {
            val parent = stack.lastOrNull()?.id ?: return
            scope.launch {
                busy = true
                notice = null
                try {
                    withContext(Dispatchers.IO) { GofileApi.deleteContent(session.token, item.id) }
                    items = withContext(Dispatchers.IO) { GofileApi.getFolder(session.token, parent) }
                    pendingDelete = null
                    showNotice("\"${item.name}\" dihapus.", error = false)
                } catch (error: Exception) {
                    report(error)
                } finally {
                    busy = false
                }
            }
        }

        fun enter(sessionValue: GSession) {
            SessionStore.save(context, sessionValue)
            session = sessionValue
            val root = sessionValue.rootFolder.ifBlank { "root" }
            stack = listOf(Crumb(root, "Root"))
            currentTab = "home"
            loginStep = "ready"
            loadFolder(root)
        }

        fun logout() {
            SessionStore.clear(context)
            session = GSession()
            stack = emptyList()
            items = emptyList()
            tokenInput = ""
            notice = null
            noticeIsError = true
            showCreateFolder = false
            newFolderName = ""
            pendingDelete = null
            currentTab = "home"
            loginStep = "email"
        }

        fun openFolderTab() {
            currentTab = "folder"
            val root = stack.lastOrNull()?.id ?: session.rootFolder.ifBlank { "root" }
            if (items.isEmpty()) loadFolder(root)
        }

        LaunchedEffect(Unit) {
            if (session.isLoggedIn) {
                try {
                    val fresh = withContext(Dispatchers.IO) { GofileApi.getAccount(session.token) }
                    enter(fresh.copy(token = session.token))
                } catch (_: Exception) {
                    logout()
                }
            }
            checkingSession = false
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
                if (token.isNullOrBlank() || session.isLoggedIn || busy) return@collect
                busy = true
                notice = null
                try {
                    val account = withContext(Dispatchers.IO) { GofileApi.getAccount(token) }
                    enter(account.copy(token = token))
                    activity.loginLinkFlow.value = null
                    showNotice("Masuk lewat link email berhasil.", error = false)
                } catch (error: Exception) {
                    report(error)
                } finally {
                    busy = false
                }
            }
        }

        // Pemberitahuan hilang sendiri seperti pil toast HyperSpring.
        LaunchedEffect(notice) {
            if (notice != null) {
                delay(4600)
                notice = null
            }
        }

        BackHandler(enabled = loginStep == "ready" && (currentTab != "home" || stack.size > 1)) {
            if (currentTab == "folder" && stack.size > 1) {
                stack = stack.dropLast(1)
                loadFolder(stack.last().id)
            } else {
                currentTab = "home"
            }
        }

        Box(Modifier.fillMaxSize().background(Hyper.Bg)) {
            if (loginStep == "ready") {
                if (checkingSession) {
                    DotGridBackground(Modifier.fillMaxSize(), reduceMotion = reduceMotion, interactive = false)
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        ExpressiveLoader(
                            size = 52.dp,
                            reduceMotion = reduceMotion,
                            color = Hyper.Accent,
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Menyiapkan sesi",
                            style = MaterialTheme.typography.labelLarge,
                            color = Hyper.Text2,
                        )
                    }
                } else {
                    AnimatedContent(
                        targetState = currentTab,
                        label = "tabContent",
                        transitionSpec = {
                            if (reduceMotion) {
                                fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                            } else {
                                val forward = TAB_ORDER.indexOf(targetState) >= TAB_ORDER.indexOf(initialState)
                                (
                                    fadeIn(tween(200, easing = HyperEase)) +
                                        scaleIn(tween(260, easing = HyperExpoOut), initialScale = 0.97f) +
                                        slideInHorizontally(tween(260, easing = HyperExpoOut)) { full ->
                                            if (forward) full / 12 else -full / 12
                                        }
                                    ) togetherWith (
                                    fadeOut(tween(140, easing = HyperPower3Out)) +
                                        scaleOut(tween(180, easing = HyperPower3Out), targetScale = 0.99f) +
                                        slideOutHorizontally(tween(200, easing = HyperPower3Out)) { full ->
                                            if (forward) -full / 16 else full / 16
                                        }
                                    )
                            }
                        },
                    ) { tab ->
                        when (tab) {
                            "folder" -> FolderScreen(
                                items = items,
                                busy = busy,
                                stack = stack,
                                reduceMotion = reduceMotion,
                                onOpen = { item ->
                                    if (item.isFolder) {
                                        stack = stack + Crumb(item.id, item.name)
                                        loadFolder(item.id)
                                    } else if (item.link.startsWith("http")) {
                                        activity.startDownload(item.link, item.name, session.token)
                                    }
                                },
                                onDownload = { item ->
                                    activity.startDownload(item.link, item.name, session.token)
                                },
                                onDelete = { item -> pendingDelete = item },
                                onUpload = {
                                    stack.lastOrNull()?.let {
                                        activity.pickFileForUpload(it.id, session.token)
                                    }
                                },
                                onBack = {
                                    if (stack.size > 1) {
                                        stack = stack.dropLast(1)
                                        loadFolder(stack.last().id)
                                    }
                                },
                                onRefresh = { stack.lastOrNull()?.let { loadFolder(it.id) } },
                                onNewFolder = {
                                    newFolderName = ""
                                    showCreateFolder = true
                                },
                            )

                            "settings" -> SettingsScreen(
                                session = session,
                                themeMode = themeMode,
                                dynamicColor = dynamicColor,
                                reduceMotion = reduceMotion,
                                onThemeChange = { themeMode = it },
                                onDynamicChange = { dynamicColor = it },
                                onLogout = { logout() },
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
                                onRefresh = { stack.lastOrNull()?.let { loadFolder(it.id) } },
                            )
                        }
                    }

                    HyperNavBar(
                        tabs = APP_TABS,
                        current = currentTab,
                        reduceMotion = reduceMotion,
                        onSelect = { tab ->
                            currentTab = tab
                            if (tab == "folder") openFolderTab()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding(),
                    )
                }
            } else {
                LoginScreen(
                    step = loginStep,
                    email = email,
                    tokenInput = tokenInput,
                    busy = busy,
                    reduceMotion = reduceMotion,
                    onEmailChange = { email = it },
                    onTokenChange = { tokenInput = it },
                    onBackToEmail = {
                        loginStep = "email"
                        notice = null
                    },
                    onSend = {
                        scope.launch {
                            busy = true
                            notice = null
                            try {
                                withContext(Dispatchers.IO) { GofileApi.sendLoginLink(email.trim()) }
                                loginStep = "token"
                                showNotice(
                                    "Link login terkirim. Cek email lalu tempel di sini.",
                                    error = false,
                                )
                            } catch (error: Exception) {
                                report(error)
                            } finally {
                                busy = false
                            }
                        }
                    },
                    onLogin = {
                        val token = GofileApi.extractToken(tokenInput)
                        if (token == null) {
                            showNotice("Link atau token tidak dikenali. Tempel link lengkap dari email.")
                        } else {
                            scope.launch {
                                busy = true
                                notice = null
                                try {
                                    val account = withContext(Dispatchers.IO) {
                                        GofileApi.getAccount(token)
                                    }
                                    enter(account.copy(token = token))
                                } catch (error: Exception) {
                                    report(error)
                                } finally {
                                    busy = false
                                }
                            }
                        }
                    },
                    onGuest = {
                        scope.launch {
                            busy = true
                            notice = null
                            try {
                                val guest = withContext(Dispatchers.IO) { GofileApi.createGuest() }
                                enter(guest)
                            } catch (error: Exception) {
                                report(error)
                            } finally {
                                busy = false
                            }
                        }
                    },
                )
            }

            // Pil toast di atas bar navigasi.
            AnimatedVisibility(
                visible = notice != null,
                enter = if (reduceMotion) {
                    fadeIn(tween(120))
                } else {
                    slideInVertically(tween(240, easing = HyperExpoOut)) { it / 2 } + fadeIn(tween(180))
                },
                exit = if (reduceMotion) {
                    fadeOut(tween(100))
                } else {
                    slideOutVertically(tween(180, easing = HyperPower3Out)) { it / 3 } + fadeOut(tween(140))
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = if (loginStep == "ready" && !checkingSession) 92.dp else 16.dp,
                    ),
            ) {
                notice?.let { text ->
                    HyperToast(
                        text = text,
                        error = noticeIsError,
                        onDismiss = { notice = null },
                    )
                }
            }
        }

        if (showCreateFolder) {
            AlertDialog(
                onDismissRequest = { showCreateFolder = false },
                containerColor = Hyper.Card,
                shape = RoundedCornerShape(24.dp),
                title = { Text("Folder baru", color = Hyper.Text) },
                text = {
                    HyperTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = "Nama folder",
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { createFolder() },
                        enabled = newFolderName.isNotBlank() && !busy,
                    ) {
                        Text("Buat", color = Hyper.Accent)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateFolder = false }) {
                        Text("Batal", color = Hyper.Text2)
                    }
                },
            )
        }

        pendingDelete?.let { target ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                containerColor = Hyper.Card,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text(
                        "Hapus ${if (target.isFolder) "folder" else "file"}?",
                        color = Hyper.Text,
                    )
                },
                text = {
                    Column {
                        Text(
                            "\"${target.name}\" akan dihapus permanen dari GoFile. Tindakan ini tidak bisa dibatalkan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Hyper.Text2,
                        )
                        Spacer(Modifier.height(18.dp))
                        HoldToConfirm(
                            label = "Tahan untuk hapus",
                            hint = "Tahan 0,9 detik. Ketukan pendek ditolak.",
                            enabled = !busy,
                            onConfirm = { deleteItem(target) },
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text("Batal", color = Hyper.Text2)
                    }
                },
            )
        }
    }
}

/* ================================= toast ================================== */

@Composable
private fun HyperToast(text: String, error: Boolean, onDismiss: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = Hyper.Card,
        border = BorderStroke(1.dp, if (error) Hyper.AccentEdge else Hyper.Hairline),
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDismiss),
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (error) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Hyper.Accent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Hyper.Text,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/* ================================ beranda ================================= */

@Composable
private fun HomeScreen(
    session: GSession,
    items: List<GItem>,
    busy: Boolean,
    reduceMotion: Boolean,
    activity: MainActivity,
    permissionRevision: Int,
    onOpenFolder: () -> Unit,
    onUpload: () -> Unit,
    onNewFolder: () -> Unit,
    onRefresh: () -> Unit,
) {
    val scroll = rememberScrollState()
    val folderCount = remember(items) { items.count { it.isFolder } }
    val fileCount = remember(items) { items.size - folderCount }
    val totalSize = remember(items) { items.sumOf { if (it.isFolder) 0L else it.size } }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val entrance by animateFloatAsState(
        targetValue = if (entered || reduceMotion) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 320, easing = HyperExpoOut),
        label = "homeEntrance",
    )
    val density = LocalDensity.current
    val shift = with(density) { (1f - entrance) * 14.dp.toPx() }

    HyperPage(
        title = "Beranda",
        subtitle = "Isi root folder GoFile kamu, langsung dari perangkat.",
        scroll = scroll,
        reduceMotion = reduceMotion,
        actions = {
            HyperIconButton(
                icon = Icons.Filled.Refresh,
                description = "Muat ulang",
                enabled = !busy,
                onClick = onRefresh,
            )
        },
    ) {
        Column(
            Modifier.graphicsLayer {
                alpha = entrance
                translationY = shift
            },
        ) {
            PermissionSetup(activity = activity, revision = permissionRevision, reduceMotion = reduceMotion)

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(
                    text = "Sesi aktif",
                    dotColor = Hyper.Ok,
                    reduceMotion = reduceMotion,
                )
                StatusPill(
                    text = "Tier ${session.tier.ifBlank { "Tamu" }}",
                    dotColor = Hyper.Warn,
                    pulse = false,
                    reduceMotion = reduceMotion,
                )
            }

            Spacer(Modifier.height(14.dp))

            // Kartu akun: koral tipis dengan garis koral 40%.
            HyperCard(
                fill = Hyper.AccentSoft,
                border = Hyper.AccentEdge,
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CloudQueue,
                            contentDescription = null,
                            tint = Hyper.Accent,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = "GOFILE CLOUD",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.6.sp,
                            color = Hyper.Accent,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = session.email.ifBlank { "Akun tamu" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Hyper.Text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MicroPill(session.tier.ifBlank { "Tamu" })
                        MicroPill("Siap transfer")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StatTile(
                    label = "Folder",
                    value = folderCount.toString(),
                    icon = Icons.Filled.Folder,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "File",
                    value = fileCount.toString(),
                    icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "Ukuran",
                    value = readableSize(totalSize),
                    icon = Icons.Filled.SwapVert,
                    modifier = Modifier.weight(1f),
                )
            }

            SectionLabel("Aksi cepat")
            HyperButton(
                text = "Buka folder root",
                icon = Icons.Filled.FolderOpen,
                onClick = onOpenFolder,
                height = 56.dp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FeatureTile(
                    icon = Icons.Filled.Upload,
                    title = "Unggah",
                    state = "Pilih berkas",
                    onClick = onUpload,
                    modifier = Modifier.weight(1f),
                )
                FeatureTile(
                    icon = Icons.Filled.CreateNewFolder,
                    title = "Folder baru",
                    state = "Di folder ini",
                    onClick = onNewFolder,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))
            if (busy) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExpressiveLoader(size = 20.dp, reduceMotion = reduceMotion, color = Hyper.Accent)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Menyinkronkan isi folder…",
                        style = MaterialTheme.typography.labelLarge,
                        color = Hyper.Text2,
                    )
                }
            } else {
                HyperButton(
                    text = "Sinkronkan sekarang",
                    icon = Icons.Filled.Refresh,
                    primary = false,
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionLabel("Transfer terbaru")
            TransferPanel(
                limit = 2,
                reduceMotion = reduceMotion,
                showEmptyHint = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Pil kecil di dalam kartu akun. */
@Composable
private fun MicroPill(text: String) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.07f),
        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.10f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Hyper.Text,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** Kotak statistik: ikon kecil, angka tebal, label kapital. */
@Composable
private fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    HyperCard(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 11.dp)) {
            IconChip(icon, size = 30.dp, tint = Hyper.Accent, iconSize = 16.dp)
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Hyper.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.1.sp,
                color = Hyper.Text3,
                maxLines = 1,
            )
        }
    }
}

/** Ubin fitur: ikon, judul, dan status kapital di bawahnya. */
@Composable
private fun FeatureTile(
    icon: ImageVector,
    title: String,
    state: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = hyperSpring(0.3f, 0.6f),
        label = "tilePress",
    )
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(16.dp),
        color = if (active) Hyper.AccentSoft else Hyper.Card,
        border = BorderStroke(0.8.dp, if (active) Hyper.AccentEdge else Hyper.Hairline),
        modifier = modifier
            .heightIn(min = 92.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 11.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) Hyper.Accent else Hyper.Text,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(9.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Hyper.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.1.sp,
                color = if (active) Hyper.Accent else Hyper.Text3,
                maxLines = 1,
            )
        }
    }
}

/* ============================= izin transfer ============================== */

@Composable
private fun PermissionSetup(activity: MainActivity, revision: Int, reduceMotion: Boolean) {
    if (revision < 0) return
    val notificationsReady = activity.hasNotificationPermission()
    val batteryReady = activity.isIgnoringBatteryOptimizations()
    if (notificationsReady && batteryReady) return

    HyperCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconChip(
                    icon = Icons.Filled.NotificationsActive,
                    size = 34.dp,
                    tint = Hyper.Accent,
                    fill = Hyper.AccentSoft,
                    edge = Hyper.AccentEdge,
                    iconSize = 18.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Siapkan transfer latar belakang",
                        style = MaterialTheme.typography.titleSmall,
                        color = Hyper.Text,
                    )
                    Text(
                        "Dua izin ini menjaga progres tetap berjalan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Hyper.Text2,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            PermissionRow(
                ready = notificationsReady,
                title = "Notifikasi progres",
                supporting = "Progres unggah dan unduh tetap terlihat.",
                actionLabel = "Aktifkan",
                onAction = { activity.requestNotificationPermission() },
            )
            Spacer(Modifier.height(6.dp))
            PermissionRow(
                ready = batteryReady,
                title = "Izin latar belakang",
                supporting = "Transfer tidak terhenti saat aplikasi ditutup.",
                actionLabel = "Atur",
                onAction = { activity.requestIgnoreBatteryOptimizations() },
            )
        }
    }
}

@Composable
private fun PermissionRow(
    ready: Boolean,
    title: String,
    supporting: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val fill by animateColorAsState(
        targetValue = if (ready) Hyper.AccentSoft else Hyper.CardRaised,
        animationSpec = tween(240, easing = HyperEase),
        label = "permissionFill",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(fill)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (ready) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (ready) Hyper.Accent else Hyper.Text2,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = Hyper.Text,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = Hyper.Text2,
            )
        }
        if (ready) {
            Text(
                "SIAP",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.1.sp,
                color = Hyper.Accent,
            )
        } else {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = Hyper.Accent)
            }
        }
    }
}

/* ================================= folder ================================= */

@Composable
private fun FolderScreen(
    items: List<GItem>,
    busy: Boolean,
    stack: List<Crumb>,
    reduceMotion: Boolean,
    onOpen: (GItem) -> Unit,
    onDownload: (GItem) -> Unit,
    onDelete: (GItem) -> Unit,
    onUpload: () -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onNewFolder: () -> Unit,
) {
    val scroll = rememberScrollState()
    val folders = remember(items) { items.count { it.isFolder } }
    val totalSize = remember(items) { items.sumOf { if (it.isFolder) 0L else it.size } }
    val subtitle = when {
        busy && items.isEmpty() -> "Memuat isi folder…"
        items.isEmpty() -> "Belum ada isi di folder ini."
        else -> "$folders folder · ${items.size - folders} file · ${readableSize(totalSize)}"
    }

    Box(Modifier.fillMaxSize()) {
    HyperPage(
        title = stack.lastOrNull()?.name ?: "Folder",
        subtitle = subtitle,
        scroll = scroll,
        reduceMotion = reduceMotion,
        actions = {
            if (stack.size > 1) {
                HyperIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    description = "Kembali",
                    onClick = onBack,
                )
            }
            HyperIconButton(
                icon = Icons.Filled.CreateNewFolder,
                description = "Folder baru",
                enabled = !busy,
                onClick = onNewFolder,
            )
            HyperIconButton(
                icon = Icons.Filled.Refresh,
                description = "Muat ulang",
                enabled = !busy,
                onClick = onRefresh,
            )
        },
    ) {
        when {
            busy && items.isEmpty() -> {
                Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
                    ExpressiveLoader(size = 44.dp, reduceMotion = reduceMotion, color = Hyper.Accent)
                }
            }

            items.isEmpty() -> {
                Box(Modifier.fillMaxWidth().height(380.dp)) {
                    EmptyState(
                        icon = Icons.Filled.FolderOpen,
                        title = "Folder ini kosong",
                        body = "Unggah file pertama atau buat folder baru dari sini.",
                        actionLabel = "Unggah file",
                        onAction = onUpload,
                    )
                }
            }

            else -> {
                HyperGroup {
                    items.forEach { item ->
                        HyperRow(
                            title = item.name,
                            subtitle = if (item.isFolder) "Folder" else readableSize(item.size),
                            icon = if (item.isFolder) Icons.Filled.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                            iconTint = if (item.isFolder) Hyper.Accent else Hyper.Text2,
                            iconFill = if (item.isFolder) Hyper.AccentSoft else Hyper.Chip,
                            onClick = { onOpen(item) },
                            trailing = {
                                Row {
                                    if (!item.isFolder && item.link.startsWith("http")) {
                                        SmallIconAction(
                                            icon = Icons.Filled.Download,
                                            description = "Unduh ${item.name}",
                                            onClick = { onDownload(item) },
                                        )
                                    }
                                    SmallIconAction(
                                        icon = Icons.Filled.Delete,
                                        description = "Hapus ${item.name}",
                                        tint = Hyper.Accent,
                                        onClick = { onDelete(item) },
                                    )
                                }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                HyperButton(
                    text = "Unggah ke folder ini",
                    icon = Icons.Filled.Upload,
                    onClick = onUpload,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    // Panel transfer mengambang di atas bar navigasi.
    TransferPanel(
        reduceMotion = reduceMotion,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 88.dp),
    )
    }
}

/** Aksi ikon 36dp di ujung baris. */
@Composable
private fun SmallIconAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color = Hyper.Text2,
) {
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = tint,
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(7.dp),
    )
}

/* ================================ setelan ================================= */

@Composable
private fun SettingsScreen(
    session: GSession,
    themeMode: String,
    dynamicColor: Boolean,
    reduceMotion: Boolean,
    onThemeChange: (String) -> Unit,
    onDynamicChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
) {
    val scroll = rememberScrollState()

    HyperPage(
        title = "Setelan",
        subtitle = "Akun, tampilan, dan tentang aplikasi.",
        scroll = scroll,
        reduceMotion = reduceMotion,
    ) {
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
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.size(52.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.CloudQueue,
                                contentDescription = null,
                                tint = Hyper.Accent,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = session.email.ifBlank { "Akun tamu" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Hyper.Text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Tier ${session.tier.ifBlank { "Tamu" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Hyper.Text2,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                HyperButton(
                    text = "Keluar dari akun",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    primary = false,
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        SectionLabel("Tampilan")
        HyperGroup {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Tema",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Hyper.Text,
                )
                Spacer(Modifier.height(12.dp))
                HyperSegmented(
                    options = listOf(
                        "system" to "Sistem",
                        "light" to "Terang",
                        "dark" to "Gelap",
                    ),
                    value = themeMode,
                    onChange = onThemeChange,
                )
                if (themeMode == "light") {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Palet HyperSpring hanya gelap. Pilihan terang tetap memakai hitam.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Hyper.Text3,
                    )
                }
            }
            HyperRow(
                title = "Warna dinamis",
                subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    "Mengikuti warna wallpaper perangkat."
                } else {
                    "Membutuhkan Android 12 atau lebih baru."
                },
                icon = Icons.Filled.Settings,
                iconTint = Hyper.Text2,
                trailing = {
                    HyperToggle(
                        checked = dynamicColor,
                        onChange = onDynamicChange,
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    )
                },
            )
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Hyper.CardRaised,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (reduceMotion) {
                            "Animasi diperkecil sesuai skala animator sistem."
                        } else {
                            "Transisi memakai spring HyperSpring: 0,4s response, easing expo-out, dan tekan karet."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Hyper.Text2,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }

        SectionLabel("Tentang")
        HyperGroup {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Gofile Expressive",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Hyper.Text,
                        )
                        Text(
                            "Kotlin native dan Jetpack Compose",
                            style = MaterialTheme.typography.bodySmall,
                            color = Hyper.Text2,
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Hyper.AccentSoft,
                        border = BorderStroke(0.8.dp, Hyper.AccentEdge),
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Hyper.Accent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Tanpa WebView. Unggah, unduh, folder, dan progres latar belakang berjalan di aplikasi ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Hyper.Text2,
                )
            }
            HyperRow(
                title = "Bahasa visual",
                subtitle = "HyperSpring: hitam murni, satu aksen koral, garis rambut.",
                icon = Icons.Filled.Shield,
                iconTint = Hyper.Text2,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

/* ============================ masuk / setup =============================== */

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun LoginScreen(
    step: String,
    email: String,
    tokenInput: String,
    busy: Boolean,
    reduceMotion: Boolean,
    onEmailChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onBackToEmail: () -> Unit,
    onSend: () -> Unit,
    onLogin: () -> Unit,
    onGuest: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var page by rememberSaveable { mutableStateOf(if (step == "token") 2 else 0) }
    LaunchedEffect(step) {
        if (step == "token" && page != 2) page = 2
        if (step == "email" && page > 1) page = 1
    }

    BackHandler(enabled = page > 0 && !busy) {
        if (page == 2) {
            page = 1
            onBackToEmail()
        } else {
            page = 0
        }
    }

    val emailInvalid = email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val emailReady = email.isNotBlank() && !emailInvalid

    fun tap() {
        if (!reduceMotion) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Box(Modifier.fillMaxSize()) {
        DotGridBackground(
            Modifier.fillMaxSize(),
            hero = page == 0,
            reduceMotion = reduceMotion,
        )
        TopScrim(Modifier.align(Alignment.TopCenter), height = 132.dp)
        BottomVeil(Modifier.align(Alignment.BottomCenter), height = 236.dp)

        // Pil lewati di kanan atas, seperti di onboarding HyperSpring.
        Row(
            Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 10.dp, end = 14.dp),
        ) {
            Surface(
                onClick = {
                    if (busy) return@Surface
                    tap()
                    if (page == 0) page = 1
                },
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.07f),
                border = BorderStroke(0.8.dp, Hyper.Hairline),
            ) {
                Text(
                    text = if (page == 0) "Lewati" else "Masuk",
                    style = MaterialTheme.typography.bodySmall,
                    color = Hyper.Text2,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            AnimatedContent(
                targetState = page,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "setupPage",
                transitionSpec = {
                    if (reduceMotion) {
                        fadeIn(tween(140)) togetherWith fadeOut(tween(120))
                    } else {
                        val slide = spring<IntOffset>(dampingRatio = 0.84f, stiffness = 130f)
                        val fade = tween<Float>(420)
                        if (targetState > initialState) {
                            ContentTransform(
                                slideInHorizontally(slide) { full -> full } + fadeIn(fade),
                                slideOutHorizontally(tween(340, easing = HyperEase)) { full -> -full / 4 } +
                                    fadeOut(tween(340)),
                                0f,
                                SizeTransform(clip = false),
                            )
                        } else {
                            ContentTransform(
                                slideInHorizontally(slide) { full -> -full } + fadeIn(fade),
                                slideOutHorizontally(tween(340, easing = HyperEase)) { full -> full / 4 } +
                                    fadeOut(tween(340)),
                                0f,
                                SizeTransform(clip = false),
                            )
                        }
                    }
                },
            ) { current ->
                val entrance = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    if (reduceMotion) {
                        entrance.snapTo(1f)
                    } else {
                        entrance.animateTo(1f, hyperSpring(0.55f, 0.84f))
                    }
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .graphicsLayer {
                            alpha = entrance.value
                            translationY = 40f * (1f - entrance.value)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        Modifier
                            .widthIn(max = 460.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when (current) {
                            0 -> WelcomePage(reduceMotion)
                            1 -> EmailPage(
                                email = email,
                                emailInvalid = emailInvalid,
                                busy = busy,
                                onEmailChange = onEmailChange,
                                onGuest = {
                                    tap()
                                    onGuest()
                                },
                                onSend = {
                                    tap()
                                    onSend()
                                },
                                emailReady = emailReady,
                            )

                            else -> TokenPage(
                                tokenInput = tokenInput,
                                email = email,
                                busy = busy,
                                onTokenChange = onTokenChange,
                                onLogin = {
                                    tap()
                                    onLogin()
                                },
                                onResend = { onSend() },
                                onChangeEmail = {
                                    page = 1
                                    onBackToEmail()
                                },
                            )
                        }
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                DropletPager(count = 3, index = page)
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (page > 0) {
                    HyperButton(
                        text = "Kembali",
                        primary = false,
                        squash = true,
                        reduceMotion = reduceMotion,
                        enabled = !busy,
                        height = 56.dp,
                        onClick = {
                            tap()
                            if (page == 2) {
                                page = 1
                                onBackToEmail()
                            } else {
                                page = 0
                            }
                        },
                        modifier = Modifier.weight(0.8f),
                    )
                }
                val nextEnabled = when (page) {
                    0 -> !busy
                    1 -> emailReady && !busy
                    else -> tokenInput.isNotBlank() && !busy
                }
                HyperButton(
                    text = when {
                        busy && page == 1 -> "Mengirim…"
                        busy && page == 2 -> "Memeriksa…"
                        page == 2 -> "Masuk"
                        page == 1 -> "Kirim tautan"
                        else -> "Lanjut"
                    },
                    primary = true,
                    squash = true,
                    loading = busy,
                    reduceMotion = reduceMotion,
                    enabled = nextEnabled,
                    height = 56.dp,
                    onClick = {
                        tap()
                        when (page) {
                            0 -> page = 1
                            1 -> if (emailReady && !busy) onSend()
                            else -> if (tokenInput.isNotBlank() && !busy) onLogin()
                        }
                    },
                    modifier = Modifier.weight(1.2f),
                )
            }
        }
    }
}

@Composable
private fun WelcomePage(reduceMotion: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "SELAMAT DATANG",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            letterSpacing = 3.2.sp,
            color = Hyper.Accent,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        SplitTitle(
            text = "GoFile",
            style = MaterialTheme.typography.displayLarge,
            reduceMotion = reduceMotion,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Klien cloud native untuk mengunggah, mengunduh, dan merapikan folder tanpa WebView.",
            style = MaterialTheme.typography.bodyLarge,
            color = Hyper.Text2,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(30.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WelcomeFeature(
                icon = Icons.Filled.CloudQueue,
                title = "Native dan ringan",
                body = "Dibangun dengan Kotlin dan Jetpack Compose.",
                delayIndex = 0,
                reduceMotion = reduceMotion,
            )
            WelcomeFeature(
                icon = Icons.Filled.Upload,
                title = "Transfer latar belakang",
                body = "Progres tetap berjalan lewat foreground service.",
                delayIndex = 1,
                reduceMotion = reduceMotion,
            )
            WelcomeFeature(
                icon = Icons.Filled.Folder,
                title = "Folder rapi",
                body = "Buat, buka, dan hapus isi cloud langsung dari aplikasi.",
                delayIndex = 2,
                reduceMotion = reduceMotion,
            )
        }
    }
}

/** Baris fitur onboarding: kaca tipis, ikon 48dp, muncul berurutan. */
@Composable
private fun WelcomeFeature(
    icon: ImageVector,
    title: String,
    body: String,
    delayIndex: Int,
    reduceMotion: Boolean,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (reduceMotion) {
            progress.snapTo(1f)
        } else {
            delay(120L + delayIndex * 90L)
            progress.animateTo(1f, tween(760, easing = HyperPower4Out))
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = progress.value
                translationY = 22f * (1f - progress.value)
            }
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Hyper.AccentSoft,
            border = BorderStroke(0.8.dp, Hyper.AccentEdge),
            modifier = Modifier.size(48.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Hyper.Accent, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = Hyper.Text,
            )
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = Hyper.Text2,
            )
        }
    }
}

@Composable
private fun EmailPage(
    email: String,
    emailInvalid: Boolean,
    emailReady: Boolean,
    busy: Boolean,
    onEmailChange: (String) -> Unit,
    onGuest: () -> Unit,
    onSend: () -> Unit,
) {
    Text(
        text = "Masuk ke GoFile",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.ExtraBold,
        color = Hyper.Text,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = "Kami kirim tautan login sekali pakai ke email kamu. Ketuk linknya — langsung membuka aplikasi ini, tanpa browser.",
        style = MaterialTheme.typography.bodyMedium,
        color = Hyper.Text2,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(26.dp))
    HyperTextField(
        value = email,
        onValueChange = onEmailChange,
        label = "Alamat email",
        placeholder = "nama@contoh.com",
        leadingIcon = Icons.Filled.Email,
        supporting = if (emailInvalid) "Format email belum benar." else "Tautan login hanya dikirim ke alamat ini.",
        isError = emailInvalid,
        enabled = !busy,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Send,
        ),
        keyboardActions = KeyboardActions(onSend = { if (emailReady && !busy) onSend() }),
    )
    Spacer(Modifier.height(14.dp))
    HyperButton(
        text = "Lanjut sebagai tamu",
        icon = Icons.Filled.CloudQueue,
        primary = false,
        enabled = !busy,
        onClick = onGuest,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = "Token disimpan lokal di perangkat.",
        style = MaterialTheme.typography.bodySmall,
        color = Hyper.Text3,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun TokenPage(
    tokenInput: String,
    email: String,
    busy: Boolean,
    onTokenChange: (String) -> Unit,
    onLogin: () -> Unit,
    onResend: () -> Unit,
    onChangeEmail: () -> Unit,
) {
    Text(
        text = "Cek email kamu",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.ExtraBold,
        color = Hyper.Text,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = "Buka email dari GoFile, ketuk linknya untuk langsung masuk ke aplikasi — atau tempel di bawah.",
        style = MaterialTheme.typography.bodyMedium,
        color = Hyper.Text2,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(26.dp))
    HyperTextField(
        value = tokenInput,
        onValueChange = onTokenChange,
        label = "Tautan atau token dari email",
        placeholder = "https://gofile.io/login/...",
        leadingIcon = Icons.Filled.Key,
        trailing = {
            Icon(
                Icons.Filled.ContentPaste,
                contentDescription = null,
                tint = Hyper.Text2,
                modifier = Modifier.size(20.dp),
            )
        },
        supporting = "Link dari email otomatis membuka aplikasi ini.",
        enabled = !busy,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = { if (tokenInput.isNotBlank() && !busy) onLogin() },
        ),
    )
    Spacer(Modifier.height(10.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onResend, enabled = email.isNotBlank() && !busy) {
            Text("Kirim ulang", color = Hyper.Accent)
        }
        TextButton(onClick = onChangeEmail, enabled = !busy) {
            Text("Ganti email", color = Hyper.Text2)
        }
    }
}

/* ============================== transfer ================================== */

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun TransferPanel(
    modifier: Modifier = Modifier,
    limit: Int = 3,
    reduceMotion: Boolean = false,
    showEmptyHint: Boolean = false,
) {
    val transfers by TransferStore.transfers.collectAsState()
    val recent = transfers.takeLast(limit).asReversed()
    if (recent.isEmpty()) {
        if (showEmptyHint) {
            Text(
                "Belum ada transfer. Unggah atau unduh file untuk melihat progresnya di sini.",
                style = MaterialTheme.typography.bodySmall,
                color = Hyper.Text2,
            )
        }
        return
    }
    AnimatedVisibility(
        visible = recent.isNotEmpty(),
        enter = if (reduceMotion) {
            fadeIn(tween(120))
        } else {
            slideInVertically(tween(240, easing = HyperExpoOut)) { it / 4 } + fadeIn(tween(180))
        },
        exit = if (reduceMotion) {
            fadeOut(tween(100))
        } else {
            slideOutVertically(tween(160, easing = HyperPower3Out)) { it / 4 } + fadeOut(tween(120))
        },
    ) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(22.dp),
            color = Hyper.Card,
            border = BorderStroke(0.8.dp, Hyper.Hairline),
            shadowElevation = 6.dp,
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconChip(
                        icon = Icons.Filled.SwapVert,
                        size = 30.dp,
                        tint = Hyper.Accent,
                        fill = Hyper.AccentSoft,
                        edge = Hyper.AccentEdge,
                        iconSize = 16.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Transfer GoFile",
                        style = MaterialTheme.typography.titleSmall,
                        color = Hyper.Text,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${recent.size} item",
                        style = MaterialTheme.typography.labelMedium,
                        color = Hyper.Text2,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Tutup semua progres transfer",
                        tint = Hyper.Text2,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { TransferStore.dismissAll() }
                            .padding(7.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Geser baris ke kiri atau kanan untuk menghapusnya.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Hyper.Text3,
                )
                Spacer(Modifier.height(8.dp))
                recent.forEachIndexed { index, transfer ->
                    SwipeToDismiss(onDismiss = { TransferStore.dismiss(transfer.id) }) {
                        TransferRow(transfer = transfer, reduceMotion = reduceMotion)
                    }
                    if (index != recent.lastIndex) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeToDismiss(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dismissed by remember { mutableStateOf(false) }
    var widthPx by remember { mutableIntStateOf(0) }

    val offset by animateFloatAsState(
        targetValue = when {
            dismissed -> if (dragOffset >= 0f) widthPx * 1.4f else -widthPx * 1.4f
            else -> dragOffset
        },
        animationSpec = if (dismissed) {
            tween(180, easing = HyperPower3Out)
        } else {
            hyperSpring(0.35f, 0.72f)
        },
        finishedListener = { if (dismissed) onDismiss() },
        label = "swipeOffset",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { widthPx = it.width }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Hapus dari daftar transfer") {
                        onDismiss()
                        true
                    },
                )
            }
            .graphicsLayer {
                translationX = offset
                val limit = widthPx.coerceAtLeast(1).toFloat()
                alpha = (1f - abs(offset) / limit).coerceIn(0.2f, 1f)
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (widthPx > 0 && abs(dragOffset) > widthPx * 0.32f) {
                            dismissed = true
                        } else {
                            dragOffset = 0f
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset = (dragOffset + dragAmount)
                            .coerceIn(-widthPx * 1.4f, widthPx * 1.4f)
                    },
                )
            },
    ) {
        content()
    }
}

@Composable
private fun TransferRow(transfer: TransferInfo, reduceMotion: Boolean) {
    val progress = transfer.progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0, 100) / 100f,
        animationSpec = tween(if (reduceMotion) 0 else 320, easing = HyperExpoOut),
        label = "transferProgress",
    )
    val finished = transfer.state == "success"
    val failed = transfer.state == "error"
    val indicator = when {
        finished -> "selesai"
        failed -> transfer.message
        progress < 0 -> "menyiapkan…"
        else -> "$progress%"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Hyper.CardRaised)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when {
                    finished -> Icons.Filled.CheckCircle
                    failed -> Icons.Filled.ErrorOutline
                    else -> Icons.Filled.SwapVert
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = when {
                    finished -> Hyper.Ok
                    failed -> Hyper.Accent
                    else -> Hyper.Text2
                },
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${transfer.fileName} · $indicator",
                style = MaterialTheme.typography.bodySmall,
                color = Hyper.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(6.dp))
        if (progress < 0 && !finished && !failed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                ExpressiveLoader(
                    size = 16.dp,
                    color = Hyper.Accent,
                    reduceMotion = reduceMotion,
                )
            }
        } else {
            LinearProgressIndicator(
                progress = { if (finished) 1f else animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = if (failed) Hyper.Accent else Hyper.Text,
                trackColor = Hyper.Track,
            )
        }
    }
}
