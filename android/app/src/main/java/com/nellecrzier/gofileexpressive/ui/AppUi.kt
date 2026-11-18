package com.nellecrzier.gofileexpressive.ui

import android.os.Build
import android.provider.Settings
import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.BatteryChargingFull
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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

private val TAB_ORDER = listOf("home", "folder", "settings")

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
    Box(Modifier.graphicsLayer(alpha = alpha, translationY = offsetPx)) { content() }
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
        var loginStep by remember { mutableStateOf(if (session.isLoggedIn) "ready" else "email") }
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
                        title = { Text(topTitle) },
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
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }
            },
            bottomBar = {
                if (loginStep == "ready") {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        NavigationBarItem(
                            selected = currentTab == "home",
                            onClick = { currentTab = "home" },
                            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                            label = { Text("Beranda") },
                        )
                        NavigationBarItem(
                            selected = currentTab == "folder",
                            onClick = { openFolderTab() },
                            icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                            label = { Text("Folder") },
                        )
                        NavigationBarItem(
                            selected = currentTab == "settings",
                            onClick = { currentTab = "settings" },
                            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            label = { Text("Pengaturan") },
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
                PermissionCards(activity = activity, revision = permissionRevision)

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
                                    slideInHorizontally(
                                        animationSpec = tween(240, easing = FastOutSlowInEasing),
                                    ) { fullWidth -> if (forward) fullWidth / 10 else -fullWidth / 10 } +
                                        fadeIn(tween(180, easing = FastOutSlowInEasing))
                                    ) togetherWith (
                                    slideOutHorizontally(
                                        animationSpec = tween(240, easing = FastOutSlowInEasing),
                                    ) { fullWidth -> if (forward) -fullWidth / 10 else fullWidth / 10 } +
                                        fadeOut(tween(140, easing = FastOutSlowInEasing))
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
    // `revision` sengaja dibaca supaya kartu dihitung ulang setelah izin berubah.
    if (revision < 0) return
    if (!activity.hasNotificationPermission()) {
        NoticeCard(
            text = "Aktifkan notifikasi agar progres upload/download tetap terlihat.",
            actionLabel = "Aktifkan",
            icon = Icons.Filled.NotificationsActive,
            onAction = { activity.requestNotificationPermission() },
        )
    }
    if (!activity.isIgnoringBatteryOptimizations()) {
        NoticeCard(
            text = "Izinkan latar belakang agar transfer tidak terhenti saat aplikasi ditutup.",
            actionLabel = "Atur",
            icon = Icons.Filled.BatteryChargingFull,
            onAction = { activity.requestIgnoreBatteryOptimizations() },
        )
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
    val lift by animateDpAsState(
        targetValue = if (reduceMotion) 0.dp else 4.dp,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "homeIconLift",
    )
    val liftPx = with(LocalDensity.current) { lift.toPx() }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.CloudQueue,
                            contentDescription = null,
                            modifier = Modifier
                                .size(30.dp)
                                .graphicsLayer { translationY = liftPx },
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Halo, GoFile", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        listOf(email.ifBlank { "Akun tamu" }, tier.ifBlank { "Tamu" })
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
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

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onOpenFolder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(Icons.Filled.Folder, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Buka Folder Root")
            }

            Spacer(Modifier.height(12.dp))
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
                    "Menyinkronkan isi folder…",
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
    val pulse by animateFloatAsState(
        targetValue = if (reduceMotion) 1f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "loginIconPulse",
    )

    // Blur hanya untuk latar dekoratif di layar setup. Permukaan form tetap solid
    // supaya teks, kontras, dan dynamic color tidak bergantung pada warna latar.
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            Modifier
                .offset(x = (-90).dp, y = (-150).dp)
                .size(300.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                .blur(90.dp),
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 70.dp, y = 120.dp)
                .size(280.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f))
                .blur(110.dp),
        )

        Column(
            Modifier
                .align(Alignment.Center)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(72.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.CloudQueue,
                        contentDescription = null,
                        modifier = Modifier
                            .size(38.dp)
                            .graphicsLayer { scaleX = pulse; scaleY = pulse },
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                if (isEmailStep) "Masuk ke GoFile" else "Cek email kamu",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (isEmailStep) {
                    "Kami kirim tautan login. Tempel tautannya di langkah berikutnya."
                } else {
                    "Buka email dari GoFile, salin tautan login, lalu tempel di bawah."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Langkah ${if (isEmailStep) "1" else "2"} dari 2",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (isEmailStep) "Email" else "Tautan login",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (isEmailStep) 0.34f else 0.67f },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(28.dp))

            AnimatedContent(
                targetState = isEmailStep,
                label = "loginStep",
                transitionSpec = {
                    if (reduceMotion) {
                        fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                    } else {
                        (
                            slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) {
                                fullWidth -> if (targetState) fullWidth / 12 else -fullWidth / 12
                            } + fadeIn(tween(160, easing = FastOutSlowInEasing))
                            ) togetherWith (
                            slideOutHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) {
                                fullWidth -> if (targetState) -fullWidth / 12 else fullWidth / 12
                            } + fadeOut(tween(130, easing = FastOutSlowInEasing))
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
                        busyLabel = "Mengirim tautan…",
                        onClick = onSend,
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onGuest, enabled = !busy) {
                        Text("Lanjut sebagai tamu")
                    }
                } else {
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = onTokenChange,
                        label = { Text("Tautan atau token dari email") },
                        placeholder = { Text("https://gofile.io/login/…") },
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
                        busyLabel = "Memeriksa tautan…",
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
}

@Composable
private fun PrimaryBusyButton(
    busy: Boolean,
    enabled: Boolean,
    label: String,
    busyLabel: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Folder ini kosong.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onUpload) { Text("Unggah file pertama") }
            }
        }
        return
    }
    LazyColumn(modifier = modifier.padding(bottom = 96.dp)) {
        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            StaggerIn(index = index, reduceMotion = reduceMotion) {
                ListItem(
                    headlineContent = { Text(item.name, maxLines = 1) },
                    supportingContent = {
                        Text(if (item.isFolder) "Folder" else readableSize(item.size))
                    },
                    leadingContent = {
                        Icon(
                            if (item.isFolder) Icons.Filled.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                            contentDescription = null,
                            tint = if (item.isFolder) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    },
                    trailingContent = {
                        Row {
                            if (!item.isFolder && item.link.startsWith("http")) {
                                IconButton(onClick = { onDownload(item) }) {
                                    Icon(Icons.Filled.Download, contentDescription = "Unduh ${item.name}")
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
                    modifier = Modifier.clickable { onOpen(item) },
                )
            }
        }
    }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
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
    val themeOptions = listOf(
        "system" to "Sistem",
        "light" to "Terang",
        "dark" to "Gelap",
    )
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp),
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
                    Column {
                        Text(
                            session.email.ifBlank { "Akun tamu" },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "Tier: ${session.tier.ifBlank { "Tamu" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Text("Tampilan", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                themeOptions.forEach { (key, label) ->
                    FilterChip(
                        selected = themeMode == key,
                        onClick = { onThemeChange(key) },
                        label = { Text(label) },
                        leadingIcon = if (themeMode == key) {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else {
                            null
                        },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Warna dinamis", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                if (reduceMotion) {
                    "Animasi diperkecil sesuai preferensi sistem."
                } else {
                    "Transisi dan mikro-interaksi mengikuti Materi 3."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))
            Text("Akun", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Keluar dari akun")
            }

            Spacer(Modifier.height(32.dp))
            Text(
                "Gofile Expressive · native Kotlin",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
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
            slideInVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) {
                it / 4
            } + fadeIn(tween(160))
        },
        exit = if (reduceMotion) {
            fadeOut(tween(100))
        } else {
            slideOutVertically(animationSpec = tween(160, easing = FastOutSlowInEasing)) {
                it / 4
            } + fadeOut(tween(120))
        },
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SwapVert, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Transfer GoFile", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.weight(1f))
                    Text("${recent.size}", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(8.dp))
                recent.forEach { transfer ->
                    val progress = transfer.progress
                    val status = when (transfer.state) {
                        "success" -> "${transfer.fileName} · selesai"
                        "error" -> "${transfer.fileName} · ${transfer.message}"
                        else -> transfer.message.ifBlank {
                            if (progress < 0) "${transfer.fileName} · menyiapkan…"
                            else "${transfer.fileName} · $progress%"
                        }
                    }
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                    )
                    Spacer(Modifier.height(5.dp))
                    if (progress < 0) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0, 100) / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
