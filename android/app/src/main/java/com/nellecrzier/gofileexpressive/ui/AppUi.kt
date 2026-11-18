package com.nellecrzier.gofileexpressive.ui

import android.os.Build
import android.provider.Settings
import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nellecrzier.gofileexpressive.GSession
import com.nellecrzier.gofileexpressive.GoFileException
import com.nellecrzier.gofileexpressive.GofileApi
import com.nellecrzier.gofileexpressive.GItem
import com.nellecrzier.gofileexpressive.MainActivity
import com.nellecrzier.gofileexpressive.SessionStore
import com.nellecrzier.gofileexpressive.TransferInfo
import com.nellecrzier.gofileexpressive.TransferStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class Crumb(val id: String, val name: String)

private data class AppTab(val key: String, val label: String, val icon: ImageVector)

/** Kurva emphasized Material 3 Expressive, dibangun dari bezier agar stabil di Compose. */
private val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

private val APP_TABS = listOf(
    AppTab("home", "Beranda", Icons.Filled.Home),
    AppTab("folder", "Folder", Icons.Filled.Folder),
    AppTab("settings", "Pengaturan", Icons.Filled.Settings),
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

/** Menghormati preferensi animasi sistem Android lewat skala animator. */
@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

/** Entrance berurutan untuk grup kecil. Tidak memblokir interaksi. */
@Composable
private fun StaggerIn(
    index: Int,
    reduceMotion: Boolean,
    content: @Composable () -> Unit,
) {
    val entered = remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (entered.value || reduceMotion) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 200, easing = FastOutSlowInEasing),
        label = "staggerAlpha",
    )
    val offsetY by animateDpAsState(
        targetValue = if (entered.value || reduceMotion) 0.dp else 10.dp,
        animationSpec = tween(
            durationMillis = if (reduceMotion) 0 else 240,
            delayMillis = if (reduceMotion) 0 else index.coerceAtMost(8) * 28,
            easing = FastOutSlowInEasing,
        ),
        label = "staggerOffset",
    )
    val offsetPx = with(LocalDensity.current) { offsetY.toPx() }
    LaunchedEffect(Unit) { entered.value = true }
    Box(Modifier.fillMaxWidth().graphicsLayer(alpha = alpha, translationY = offsetPx)) { content() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun GofileApp(activity: MainActivity) {
    val context = LocalContext.current
    val reduceMotion = rememberReduceMotion()
    val systemDark = isSystemInDarkTheme()

    var themeMode by rememberSaveable { mutableStateOf("system") }
    var dynamicColor by rememberSaveable { mutableStateOf(true) }
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
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
        var currentTab by rememberSaveable { mutableStateOf("home") }
        var email by rememberSaveable { mutableStateOf("") }
        var tokenInput by rememberSaveable { mutableStateOf("") }
        val transfers by TransferStore.transfers.collectAsState()
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
        }

        LaunchedEffect(transfers) {
            transfers.filter { it.state == "error" && it.id !in handledErrors }.forEach {
                handledErrors += it.id
                activity.toast(it.message.ifBlank { "Transfer gagal." })
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

        val topTitle = when (currentTab) {
            "folder" -> stack.lastOrNull()?.name ?: "Folder"
            "settings" -> "Pengaturan"
            else -> "Beranda"
        }

        Scaffold(
            topBar = {
                if (loginStep == "ready") {
                    TopAppBar(
                        title = {
                            Text(
                                text = topTitle,
                                style = MaterialTheme.typography.titleLarge,
                            )
                        },
                        navigationIcon = {
                            if (currentTab == "folder" && stack.size > 1) {
                                IconButton(onClick = {
                                    stack = stack.dropLast(1)
                                    loadFolder(stack.last().id)
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                                }
                            }
                        },
                        actions = {
                            when (currentTab) {
                                "folder" -> {
                                    IconButton(
                                        onClick = {
                                            newFolderName = ""
                                            showCreateFolder = true
                                        },
                                        enabled = !busy,
                                    ) {
                                        Icon(Icons.Filled.CreateNewFolder, contentDescription = "Folder baru")
                                    }
                                    RefreshAction(busy, reduceMotion) {
                                        stack.lastOrNull()?.let { loadFolder(it.id) }
                                    }
                                }
                                "home" -> RefreshAction(busy, reduceMotion) {
                                    stack.lastOrNull()?.let { loadFolder(it.id) }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }
            },
            bottomBar = {
                if (loginStep == "ready") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.Center,
                    ) {
                        FloatingNavBar(
                            currentTab = currentTab,
                            onSelect = { tab ->
                                currentTab = tab
                                if (tab == "folder") openFolderTab()
                            },
                        )
                    }
                }
            },
            floatingActionButton = {
                if (loginStep == "ready" && currentTab == "folder") {
                    ExtendedFloatingActionButton(
                        onClick = {
                            stack.lastOrNull()?.let { activity.pickFileForUpload(it.id, session.token) }
                        },
                        icon = { Icon(Icons.Filled.Upload, contentDescription = null) },
                        text = { Text("Unggah") },
                    )
                }
            },
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                if (loginStep == "ready") {
                    PermissionCards(activity = activity, revision = permissionRevision)
                }

                AnimatedVisibility(
                    visible = notice != null,
                    enter = if (reduceMotion) {
                        fadeIn(tween(120))
                    } else {
                        slideInVertically(animationSpec = tween(200, easing = FastOutSlowInEasing)) {
                            -it / 4
                        } + fadeIn(tween(160))
                    },
                    exit = if (reduceMotion) {
                        fadeOut(tween(100))
                    } else {
                        slideOutVertically(animationSpec = tween(160, easing = FastOutSlowInEasing)) {
                            -it / 4
                        } + fadeOut(tween(120))
                    },
                ) {
                    notice?.let { text ->
                        NoticeCard(
                            text = text,
                            error = noticeIsError,
                            onDismiss = {
                                notice = null
                                noticeIsError = true
                            },
                        )
                    }
                }

                if (loginStep == "ready") {
                    AnimatedContent(
                        targetState = currentTab,
                        modifier = Modifier.weight(1f),
                        label = "tabContent",
                        transitionSpec = {
                            if (reduceMotion) {
                                fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                            } else {
                                val forward = TAB_ORDER.indexOf(targetState) >= TAB_ORDER.indexOf(initialState)
                                (
                                    fadeIn(tween(200, easing = EmphasizedDecelerate)) +
                                        scaleIn(
                                            animationSpec = tween(260, easing = EmphasizedDecelerate),
                                            initialScale = 0.97f,
                                        ) +
                                        slideInHorizontally(
                                            animationSpec = tween(260, easing = EmphasizedDecelerate),
                                        ) { fullWidth -> if (forward) fullWidth / 12 else -fullWidth / 12 }
                                    ) togetherWith (
                                    fadeOut(tween(140, easing = FastOutSlowInEasing)) +
                                        scaleOut(
                                            animationSpec = tween(180, easing = FastOutSlowInEasing),
                                            targetScale = 0.99f,
                                        ) +
                                        slideOutHorizontally(
                                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                                        ) { fullWidth -> if (forward) -fullWidth / 16 else fullWidth / 16 }
                                    )
                            }
                        },
                    ) { tab ->
                        when (tab) {
                            "folder" -> Box(Modifier.fillMaxSize()) {
                                FolderList(
                                    items = items,
                                    busy = busy,
                                    reduceMotion = reduceMotion,
                                    onOpen = { item ->
                                        if (item.isFolder) {
                                            stack = stack + Crumb(item.id, item.name)
                                            loadFolder(item.id)
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
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .fillMaxSize()
                                        .widthIn(max = 720.dp),
                                )
                                TransferPanel(
                                    transfers = transfers,
                                    reduceMotion = reduceMotion,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                            "settings" -> SettingsScreen(
                                session = session,
                                themeMode = themeMode,
                                dynamicColor = dynamicColor,
                                reduceMotion = reduceMotion,
                                onThemeChange = { themeMode = it },
                                onDynamicChange = { dynamicColor = it },
                                onLogout = { logout() },
                                modifier = Modifier.fillMaxSize(),
                            )
                            else -> HomeScreen(
                                email = session.email,
                                tier = session.tier,
                                items = items,
                                transfers = transfers,
                                busy = busy,
                                reduceMotion = reduceMotion,
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
                                onRefresh = {
                                    stack.lastOrNull()?.let { loadFolder(it.id) }
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
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
                                        "Link login telah dikirim. Cek email lalu tempel di sini.",
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
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }
            }
        }

        if (showCreateFolder) {
            AlertDialog(
                onDismissRequest = { showCreateFolder = false },
                title = { Text("Folder baru") },
                text = {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Nama folder") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { createFolder() },
                        enabled = newFolderName.isNotBlank() && !busy,
                    ) {
                        Text("Buat")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateFolder = false }) { Text("Batal") }
                },
            )
        }

        pendingDelete?.let { target ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("Hapus ${if (target.isFolder) "folder" else "file"}?") },
                text = {
                    Text(
                        "\"${target.name}\" akan dihapus permanen dari GoFile. Tindakan ini tidak bisa dibatalkan.",
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { deleteItem(target) },
                        enabled = !busy,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("Hapus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) { Text("Batal") }
                },
            )
        }
    }
}

/* ------------------------------ komponen ------------------------------ */

@Composable
private fun FloatingNavBar(
    currentTab: String,
    onSelect: (String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            APP_TABS.forEach { tab ->
                val selected = currentTab == tab.key
                val container by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(220, easing = EmphasizedEasing),
                    label = "navContainer",
                )
                val contentColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(180, easing = EmphasizedDecelerate),
                    label = "navContent",
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.08f else 1f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
                    label = "navIconScale",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 54.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(container)
                        .selectable(
                            selected = selected,
                            onClick = { onSelect(tab.key) },
                            role = Role.Tab,
                        )
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer { scaleX = iconScale; scaleY = iconScale },
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = contentColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginStepRail(isEmailStep: Boolean) {
    val connector by animateFloatAsState(
        targetValue = if (isEmailStep) 0f else 1f,
        animationSpec = tween(280, easing = EmphasizedEasing),
        label = "stepConnector",
    )
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepNode(
                number = 1,
                label = "Email",
                active = isEmailStep,
                done = !isEmailStep,
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .weight(1f)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(connector)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            StepNode(
                number = 2,
                label = "Tautan",
                active = !isEmailStep,
                done = false,
            )
        }
    }
}

@Composable
private fun StepNode(
    number: Int,
    label: String,
    active: Boolean,
    done: Boolean,
) {
    val container by animateColorAsState(
        targetValue = when {
            active -> MaterialTheme.colorScheme.primary
            done -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(220, easing = EmphasizedEasing),
        label = "stepContainer",
    )
    val content by animateColorAsState(
        targetValue = when {
            active -> MaterialTheme.colorScheme.onPrimary
            done -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(220, easing = EmphasizedEasing),
        label = "stepContent",
    )
    val scale by animateFloatAsState(
        targetValue = if (active) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "stepScale",
    )
    val status = when {
        done -> "selesai"
        active -> "aktif"
        else -> "belum aktif"
    }
    Row(
        modifier = Modifier
            .widthIn(min = 92.dp)
            .clearAndSetSemantics {
                contentDescription = "Langkah $number dari 2, $label, $status"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(container),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                Text(
                    text = "$number",
                    style = MaterialTheme.typography.labelMedium,
                    color = content,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            color = if (active || done) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun RefreshAction(
    busy: Boolean,
    reduceMotion: Boolean,
    onClick: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (busy && !reduceMotion) 360f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "refreshRotation",
    )
    IconButton(onClick = onClick, enabled = !busy) {
        Icon(
            Icons.Filled.Refresh,
            contentDescription = "Muat ulang",
            modifier = Modifier.graphicsLayer { rotationZ = rotation },
        )
    }
}

@Composable
private fun PermissionCards(activity: MainActivity, revision: Int) {
    // `revision` sengaja dibaca supaya status izin dihitung ulang setelah berubah.
    if (revision < 0) return
    val notificationsReady = activity.hasNotificationPermission()
    val batteryReady = activity.isIgnoringBatteryOptimizations()
    if (notificationsReady && batteryReady) return

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Siapkan transfer latar belakang",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "Dua izin ini menjaga progres tetap berjalan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            SetupRow(
                ready = notificationsReady,
                title = "Notifikasi progres",
                supporting = "Progres upload dan download tetap terlihat.",
                actionLabel = "Aktifkan",
                onAction = { activity.requestNotificationPermission() },
            )
            Spacer(Modifier.height(8.dp))
            SetupRow(
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
private fun SetupRow(
    ready: Boolean,
    title: String,
    supporting: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val container by animateColorAsState(
        targetValue = if (ready) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(240, easing = EmphasizedEasing),
        label = "setupContainer",
    )
    val contentColor = if (ready) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (ready) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (ready) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
            )
        }
        if (ready) {
            Text(
                text = "Siap",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun NoticeCard(
    text: String,
    error: Boolean = false,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val colors = if (error) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    }
    Card(
        colors = colors,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon ?: if (error) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (error) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (error) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = "Tutup")
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    email: String,
    tier: String,
    items: List<GItem>,
    transfers: List<TransferInfo>,
    busy: Boolean,
    reduceMotion: Boolean,
    onOpenFolder: () -> Unit,
    onUpload: () -> Unit,
    onNewFolder: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val folderCount = items.count { it.isFolder }
    val fileCount = items.size - folderCount
    val totalSize = items.filter { !it.isFolder }.sumOf { it.size }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val entrance by animateFloatAsState(
        targetValue = if (entered || reduceMotion) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 320, easing = EmphasizedDecelerate),
        label = "homeEntrance",
    )
    val entranceOffset = with(LocalDensity.current) { (1f - entrance) * 14.dp.toPx() }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .graphicsLayer {
                    alpha = entrance
                    translationY = entranceOffset
                    scaleX = 0.98f + entrance * 0.02f
                    scaleY = 0.98f + entrance * 0.02f
                }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.CloudQueue,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Halo, GoFile", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = email.ifBlank { "Akun tamu" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            text = tier.ifBlank { "Tamu" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatChip(
                    label = "Folder",
                    value = folderCount.toString(),
                    icon = Icons.Filled.Folder,
                    modifier = Modifier.weight(1f),
                )
                StatChip(
                    label = "File",
                    value = fileCount.toString(),
                    icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                    modifier = Modifier.weight(1f),
                )
                StatChip(
                    label = "Ukuran",
                    value = readableSize(totalSize),
                    icon = Icons.Filled.SwapVert,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = "Aksi cepat",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onOpenFolder,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(Icons.Filled.Folder, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Buka Folder Root")
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AnimatedActionCard(
                    label = "Unggah",
                    supporting = "Pilih file dari perangkat",
                    icon = Icons.Filled.Upload,
                    onClick = onUpload,
                    reduceMotion = reduceMotion,
                    modifier = Modifier.weight(1f),
                )
                AnimatedActionCard(
                    label = "Folder baru",
                    supporting = "Buat di folder saat ini",
                    icon = Icons.Filled.CreateNewFolder,
                    onClick = onNewFolder,
                    reduceMotion = reduceMotion,
                    modifier = Modifier.weight(1f),
                )
            }

            if (busy) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text(
                    "Menyinkronkan isi folder...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sinkronkan sekarang")
                }
            }

            Spacer(Modifier.height(28.dp))
            Text("Transfer terbaru", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (transfers.isEmpty()) {
                Text(
                    "Belum ada transfer. Unggah atau unduh file untuk melihat progresnya di sini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                TransferPanel(
                    transfers = transfers,
                    limit = 2,
                    reduceMotion = reduceMotion,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimatedActionCard(
    label: String,
    supporting: String,
    icon: ImageVector,
    onClick: () -> Unit,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) 0.97f else 1f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "actionPress",
    )
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelLarge)
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
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
    modifier: Modifier = Modifier,
) {
    val isEmailStep = step == "email"
    val emailInvalid = email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val emailReady = email.isNotBlank() && !emailInvalid

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val entrance by animateFloatAsState(
        targetValue = if (entered || reduceMotion) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 380, easing = EmphasizedDecelerate),
        label = "loginEntrance",
    )
    val entranceOffset = with(LocalDensity.current) { (1f - entrance) * 22.dp.toPx() }

    // Blur hanya untuk latar dekoratif. Kartu form tetap solid supaya teks dan
    // kontras tidak bergantung pada warna di belakangnya.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            Modifier
                .offset(x = (-96).dp, y = (-160).dp)
                .size(300.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))
                .blur(90.dp),
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 120.dp)
                .size(280.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f))
                .blur(110.dp),
        )

        Column(
            Modifier
                .align(Alignment.Center)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 20.dp)
                .imePadding()
                .graphicsLayer {
                    alpha = entrance
                    translationY = entranceOffset
                }
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(54.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.CloudQueue,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Gofile Expressive",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Upload dan download native tanpa WebView.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = if (isEmailStep) "Masuk ke GoFile" else "Cek email kamu",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isEmailStep) {
                    "Kami kirim tautan login sekali pakai. Tidak perlu kata sandi."
                } else {
                    "Buka email dari GoFile, salin tautan login, lalu tempel di bawah."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            LoginStepRail(isEmailStep = isEmailStep)
            Spacer(Modifier.height(18.dp))

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp)) {
                    AnimatedContent(
                        targetState = isEmailStep,
                        label = "loginStep",
                        transitionSpec = {
                            if (reduceMotion) {
                                fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                            } else {
                                (
                                    fadeIn(tween(180, easing = EmphasizedDecelerate)) +
                                        scaleIn(
                                            animationSpec = tween(240, easing = EmphasizedDecelerate),
                                            initialScale = 0.96f,
                                        ) +
                                        slideInHorizontally(
                                            animationSpec = tween(240, easing = EmphasizedDecelerate),
                                        ) { width -> if (targetState) width / 14 else -width / 14 }
                                    ) togetherWith (
                                    fadeOut(tween(120)) +
                                        scaleOut(
                                            animationSpec = tween(160, easing = FastOutSlowInEasing),
                                            targetScale = 0.98f,
                                        )
                                    )
                            }
                        },
                    ) { emailStepValue ->
                        if (emailStepValue) {
                            OutlinedTextField(
                                value = email,
                                onValueChange = onEmailChange,
                                label = { Text("Alamat email") },
                                placeholder = { Text("nama@contoh.com") },
                                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                                supportingText = {
                                    Text(
                                        if (emailInvalid) {
                                            "Format email belum benar."
                                        } else {
                                            "Tautan login hanya dikirim ke alamat ini."
                                        },
                                    )
                                },
                                isError = emailInvalid,
                                enabled = !busy,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Send,
                                ),
                                keyboardActions = KeyboardActions(
                                    onSend = { if (emailReady && !busy) onSend() },
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(16.dp))
                            PrimaryBusyButton(
                                busy = busy,
                                enabled = emailReady && !busy,
                                label = "Kirim tautan login",
                                busyLabel = "Mengirim tautan...",
                                onClick = onSend,
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onGuest,
                                enabled = !busy,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                            ) {
                                Icon(Icons.Filled.CloudQueue, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text("Lanjut sebagai tamu")
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Sesi tamu bersifat sementara dan tidak menyimpan akun.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            OutlinedTextField(
                                value = tokenInput,
                                onValueChange = onTokenChange,
                                label = { Text("Tautan atau token dari email") },
                                placeholder = { Text("https://gofile.io/login/...") },
                                leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
                                supportingText = { Text("Tempel tautan lengkap, bukan hanya kode.") },
                                enabled = !busy,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = { if (tokenInput.isNotBlank() && !busy) onLogin() },
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(16.dp))
                            PrimaryBusyButton(
                                busy = busy,
                                enabled = tokenInput.isNotBlank() && !busy,
                                label = "Masuk",
                                busyLabel = "Memeriksa tautan...",
                                onClick = onLogin,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                TextButton(
                                    onClick = onSend,
                                    enabled = email.isNotBlank() && !busy,
                                ) { Text("Kirim ulang") }
                                TextButton(onClick = onBackToEmail, enabled = !busy) {
                                    Text("Ganti email")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "Token disimpan lokal di perangkat ini. Tidak ada kata sandi yang dikirim.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PrimaryBusyButton(
    busy: Boolean,
    enabled: Boolean,
    label: String,
    busyLabel: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 900f),
        label = "primaryPress",
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(if (busy) busyLabel else label)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderList(
    items: List<GItem>,
    busy: Boolean,
    reduceMotion: Boolean,
    onOpen: (GItem) -> Unit,
    onDownload: (GItem) -> Unit,
    onDelete: (GItem) -> Unit,
    onUpload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (busy && items.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (items.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(76.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Folder ini kosong", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Unggah file pertama atau buat folder dari tab Beranda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onUpload,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Filled.Upload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Unggah file")
                }
            }
        }
        return
    }

    Column(modifier = modifier) {
        if (busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 4.dp,
                bottom = 104.dp,
            ),
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                StaggerIn(index = index, reduceMotion = reduceMotion) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = item.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = if (item.isFolder) "Folder" else readableSize(item.size),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = if (item.isFolder) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (item.isFolder) {
                                            Icons.Filled.Folder
                                        } else {
                                            Icons.AutoMirrored.Filled.InsertDriveFile
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (item.isFolder) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            Row {
                                if (!item.isFolder && item.link.startsWith("http")) {
                                    IconButton(onClick = { onDownload(item) }) {
                                        Icon(
                                            Icons.Filled.Download,
                                            contentDescription = "Unduh ${item.name}",
                                        )
                                    }
                                }
                                IconButton(onClick = { onDelete(item) }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Hapus ${item.name}",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 3.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onOpen(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    session: GSession,
    themeMode: String,
    dynamicColor: Boolean,
    reduceMotion: Boolean,
    onThemeChange: (String) -> Unit,
    onDynamicChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val entrance by animateFloatAsState(
        targetValue = if (entered || reduceMotion) 1f else 0f,
        animationSpec = tween(if (reduceMotion) 0 else 300, easing = EmphasizedDecelerate),
        label = "settingsEntrance",
    )
    val offset = with(LocalDensity.current) { (1f - entrance) * 12.dp.toPx() }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .graphicsLayer {
                    alpha = entrance
                    translationY = offset
                }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(52.dp),
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.CloudQueue,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = session.email.ifBlank { "Akun tamu" },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                            )
                            Text(
                                text = "Tier: ${session.tier.ifBlank { "Tamu" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onLogout,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("Keluar dari akun")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = "Tampilan",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Tema", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(10.dp))
                    ThemeSegmented(value = themeMode, onChange = onThemeChange)
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Warna dinamis", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    "Mengikuti warna wallpaper perangkat."
                                } else {
                                    "Membutuhkan Android 12 atau lebih baru."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = dynamicColor,
                            onCheckedChange = onDynamicChange,
                            enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (reduceMotion) {
                            "Animasi diperkecil sesuai skala animator sistem."
                        } else {
                            "Transisi pendek dan mikro-interaksi mengikuti Material 3 Expressive."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            Text(
                text = "Gofile Expressive · native Kotlin",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ThemeSegmented(
    value: String,
    onChange: (String) -> Unit,
) {
    val options = listOf(
        "system" to "Sistem",
        "light" to "Terang",
        "dark" to "Gelap",
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { (key, label) ->
                val selected = value == key
                val container by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(220, easing = EmphasizedEasing),
                    label = "themeContainer",
                )
                val contentColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(180, easing = EmphasizedDecelerate),
                    label = "themeContent",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(container)
                        .selectable(
                            selected = selected,
                            onClick = { onChange(key) },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = contentColor,
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = contentColor,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun TransferPanel(
    transfers: List<TransferInfo>,
    modifier: Modifier = Modifier,
    limit: Int = 3,
    reduceMotion: Boolean = false,
) {
    val recent = transfers.takeLast(limit).asReversed()
    AnimatedVisibility(
        visible = recent.isNotEmpty(),
        enter = if (reduceMotion) {
            fadeIn(tween(120))
        } else {
            slideInVertically(
                animationSpec = tween(240, easing = EmphasizedDecelerate),
            ) { it / 4 } + fadeIn(tween(180))
        },
        exit = if (reduceMotion) {
            fadeOut(tween(100))
        } else {
            slideOutVertically(
                animationSpec = tween(160, easing = FastOutSlowInEasing),
            ) { it / 4 } + fadeOut(tween(120))
        },
    ) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
            shadowElevation = 6.dp,
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.SwapVert,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Transfer GoFile", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${recent.size} item",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(10.dp))
                recent.forEachIndexed { index, transfer ->
                    TransferRow(transfer = transfer, reduceMotion = reduceMotion)
                    if (index != recent.lastIndex) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferRow(transfer: TransferInfo, reduceMotion: Boolean) {
    val progress = transfer.progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0, 100) / 100f,
        animationSpec = tween(
            durationMillis = if (reduceMotion) 0 else 320,
            easing = EmphasizedDecelerate,
        ),
        label = "transferProgress",
    )
    val finished = transfer.state == "success"
    val failed = transfer.state == "error"
    val indicator = when {
        finished -> "selesai"
        failed -> transfer.message
        progress < 0 -> "menyiapkan..."
        else -> "$progress%"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
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
                    finished -> MaterialTheme.colorScheme.primary
                    failed -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${transfer.fileName} · $indicator",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(6.dp))
        if (progress < 0 && !finished && !failed) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(
                progress = { if (finished) 1f else animatedProgress },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
