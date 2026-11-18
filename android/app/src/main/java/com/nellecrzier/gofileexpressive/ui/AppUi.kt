package com.nellecrzier.gofileexpressive.ui

import android.os.Build
import android.provider.Settings
import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
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
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
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

/**
 * Latar radial yang bergerak mengikuti halaman setup. Dua sumber cahaya
 * berpindah dengan spring lambat supaya perpindahan halaman terasa seperti
 * satu ruang yang sama, bukan tiga layar terpisah.
 */
@Composable
private fun SetupBackdrop(currentPage: Int, modifier: Modifier = Modifier) {
    val targetA = when (currentPage) {
        0 -> Offset(0.2f, 0.2f)
        1 -> Offset(0.8f, 0.1f)
        else -> Offset(0.1f, 0.8f)
    }
    val targetB = when (currentPage) {
        0 -> Offset(0.8f, 0.8f)
        1 -> Offset(0.2f, 0.7f)
        else -> Offset(0.9f, 0.2f)
    }
    val spec = spring<Offset>(dampingRatio = 0.8f, stiffness = 50f)
    val posA by animateOffsetAsState(targetA, spec, label = "setupPosA")
    val posB by animateOffsetAsState(targetB, spec, label = "setupPosB")
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Canvas(modifier.background(MaterialTheme.colorScheme.surface)) {
        val radius = size.width * 0.62f
        val centerA = Offset(size.width * posA.x, size.height * posA.y)
        val centerB = Offset(size.width * posB.x, size.height * posB.y)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.18f), Color.Transparent),
                center = centerA,
                radius = radius,
            ),
            radius = radius,
            center = centerA,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiary.copy(alpha = 0.14f), Color.Transparent),
                center = centerB,
                radius = radius,
            ),
            radius = radius,
            center = centerB,
        )
    }
}

/** Judul bagian ringkas dengan huruf kapital dan jarak huruf lebar. */
@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.6.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 10.dp, bottom = 8.dp),
    )
}

@Composable
private fun SetupFeature(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(46.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
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
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    val labelWidths = APP_TABS.map { tab ->
        with(density) {
            measurer.measure(AnnotatedString(tab.label), labelStyle).size.width.toDp() + 8.dp
        }
    }

    // Bar mengikuti isi: tab tidak terpilih hanya ikon, tab terpilih membuka
    // labelnya di samping ikon. Lebar label dianimasikan, bukan diganti
    // mendadak, jadi pil terasa membuka seperti satu benda.
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 8.dp,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            APP_TABS.forEachIndexed { index, tab ->
                val selected = currentTab == tab.key
                val interactionSource = remember { MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()

                val openWidth by animateDpAsState(
                    targetValue = if (selected) labelWidths[index] else 0.dp,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
                    label = "navLabelWidth",
                )
                val labelAlpha by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = tween(if (selected) 180 else 90, easing = EmphasizedDecelerate),
                    label = "navLabelAlpha",
                )
                val pressScale by animateFloatAsState(
                    targetValue = if (pressed) 0.95f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "navPressScale",
                )
                val selectionScale by animateFloatAsState(
                    targetValue = if (selected) 1.10f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "navSelectionScale",
                )
                val container by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(220, easing = EmphasizedEasing),
                    label = "navContainer",
                )
                val contentColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(200, easing = EmphasizedDecelerate),
                    label = "navContent",
                )

                Row(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = pressScale * selectionScale
                            scaleY = pressScale * selectionScale
                        }
                        .height(48.dp)
                        .defaultMinSize(minWidth = 48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(container)
                        .selectable(
                            selected = selected,
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(tab.key) },
                        )
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp),
                    )
                    Box(
                        modifier = Modifier
                            .width(openWidth)
                            .clipToBounds(),
                    ) {
                        Text(
                            text = tab.label,
                            style = labelStyle,
                            color = contentColor,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .alpha(labelAlpha),
                        )
                    }
                }
            }
        }
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
    val heroContentColor = MaterialTheme.colorScheme.onPrimaryContainer

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
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer,
                                ),
                            ),
                        ),
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        drawCircle(
                            color = heroContentColor.copy(alpha = 0.06f),
                            radius = size.width * 0.42f,
                            center = Offset(size.width * 0.94f, size.height * 0.08f),
                        )
                        drawCircle(
                            color = heroContentColor.copy(alpha = 0.05f),
                            radius = size.width * 0.26f,
                            center = Offset(size.width * 0.72f, size.height * 0.96f),
                        )
                    }
                    Column(Modifier.padding(22.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CloudQueue,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "GOFILE CLOUD",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.6.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Halo,",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                        Text(
                            text = email.ifBlank { "Akun tamu" },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                            ) {
                                Text(
                                    text = tier.ifBlank { "Tamu" },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                            ) {
                                Text(
                                    text = "Siap transfer",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
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

            Spacer(Modifier.height(24.dp))
            SectionLabel("Aksi cepat")
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
            SectionLabel("Transfer terbaru")
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
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Column(Modifier.padding(14.dp)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(42.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
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
    val haptic = LocalHapticFeedback.current
    var page by rememberSaveable {
        mutableStateOf(if (step == "token") 2 else 0)
    }
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
    val fieldShape = RoundedCornerShape(18.dp)

    fun tap() {
        if (!reduceMotion) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Box(modifier.fillMaxSize()) {
        SetupBackdrop(currentPage = page, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
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
                        val slideSpec = spring<IntOffset>(dampingRatio = 0.8f, stiffness = 300f)
                        val fadeSpec = tween<Float>(500)
                        if (targetState > initialState) {
                            ContentTransform(
                                (slideInHorizontally(slideSpec) { fullWidth -> fullWidth } +
                                    fadeIn(fadeSpec)),
                                (slideOutHorizontally(slideSpec) { fullWidth -> -fullWidth } +
                                    fadeOut(fadeSpec)),
                                0f,
                                SizeTransform(clip = false),
                            )
                        } else {
                            ContentTransform(
                                (slideInHorizontally(slideSpec) { fullWidth -> -fullWidth } +
                                    fadeIn(fadeSpec)),
                                (slideOutHorizontally(slideSpec) { fullWidth -> fullWidth } +
                                    fadeOut(fadeSpec)),
                                0f,
                                SizeTransform(clip = false),
                            )
                        }
                    }
                },
            ) { currentPage ->
                val entrance = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    entrance.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 150f),
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp)
                        .graphicsLayer {
                            val progress = entrance.value
                            alpha = progress
                            translationY = 40f * (1f - progress)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 460.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when (currentPage) {
                            0 -> {
                                Text(
                                    text = "SELAMAT DATANG",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 4.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "GoFile",
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(14.dp))
                                Box(
                                    Modifier
                                        .width(72.dp)
                                        .height(3.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                                Spacer(Modifier.height(20.dp))
                                Text(
                                    text = "Klien cloud native untuk mengunggah, mengunduh, dan merapikan folder tanpa WebView.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(28.dp))
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    SetupFeature(
                                        icon = Icons.Filled.CloudQueue,
                                        title = "Native dan ringan",
                                        body = "Dibangun dengan Kotlin dan Jetpack Compose.",
                                    )
                                    SetupFeature(
                                        icon = Icons.Filled.Upload,
                                        title = "Transfer latar belakang",
                                        body = "Progres tetap berjalan lewat foreground service.",
                                    )
                                    SetupFeature(
                                        icon = Icons.Filled.Folder,
                                        title = "Folder rapi",
                                        body = "Buat, buka, dan hapus isi cloud langsung dari aplikasi.",
                                    )
                                }
                            }

                            1 -> {
                                Text(
                                    text = "Masuk ke GoFile",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "Kami kirim tautan login sekali pakai ke email kamu. Tidak perlu kata sandi.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(24.dp))
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = onEmailChange,
                                    label = { Text("Alamat email") },
                                    placeholder = { Text("nama@contoh.com") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Email, contentDescription = null)
                                    },
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
                                    shape = fieldShape,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Send,
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onSend = { if (emailReady && !busy) onSend() },
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(14.dp))
                                OutlinedButton(
                                    onClick = {
                                        tap()
                                        onGuest()
                                    },
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
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Token disimpan lokal di perangkat.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }

                            else -> {
                                Text(
                                    text = "Cek email kamu",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "Buka email dari GoFile, salin tautan login, lalu tempel di bawah.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(24.dp))
                                OutlinedTextField(
                                    value = tokenInput,
                                    onValueChange = onTokenChange,
                                    label = { Text("Tautan atau token dari email") },
                                    placeholder = { Text("https://gofile.io/login/...") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Key, contentDescription = null)
                                    },
                                    supportingText = {
                                        Text("Tempel tautan lengkap, bukan hanya kode.")
                                    },
                                    enabled = !busy,
                                    singleLine = true,
                                    shape = fieldShape,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (tokenInput.isNotBlank() && !busy) onLogin()
                                        },
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    TextButton(
                                        onClick = { onSend() },
                                        enabled = email.isNotBlank() && !busy,
                                    ) { Text("Kirim ulang") }
                                    TextButton(
                                        onClick = {
                                            page = 1
                                            onBackToEmail()
                                        },
                                        enabled = !busy,
                                    ) { Text("Ganti email") }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(3) { index ->
                    val selected = index == page
                    val width by animateDpAsState(
                        targetValue = if (selected) 24.dp else 8.dp,
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
                        label = "setupIndicatorWidth",
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            ),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (page > 0) {
                    Button(
                        onClick = {
                            tap()
                            if (page == 2) {
                                page = 1
                                onBackToEmail()
                            } else {
                                page = 0
                            }
                        },
                        enabled = !busy,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                    ) {
                        Text(
                            text = "Kembali",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                val nextEnabled = when (page) {
                    0 -> !busy
                    1 -> emailReady && !busy
                    else -> tokenInput.isNotBlank() && !busy
                }
                Button(
                    onClick = {
                        tap()
                        when (page) {
                            0 -> page = 1
                            1 -> if (emailReady && !busy) onSend()
                            else -> if (tokenInput.isNotBlank() && !busy) onLogin()
                        }
                    },
                    enabled = nextEnabled,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) {
                    Text(
                        text = when {
                            busy && page == 1 -> "Mengirim..."
                            busy && page == 2 -> "Memeriksa..."
                            page == 2 -> "Masuk"
                            page == 1 -> "Kirim tautan"
                            else -> "Lanjut"
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
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
                bottom = 28.dp,
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
                                shape = RoundedCornerShape(14.dp),
                                color = if (item.isFolder) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                modifier = Modifier.size(44.dp),
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
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(22.dp))
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
    val headerContentColor = MaterialTheme.colorScheme.onPrimaryContainer

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
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier.background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        ),
                    ),
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        drawCircle(
                            color = headerContentColor.copy(alpha = 0.07f),
                            radius = size.width * 0.5f,
                            center = Offset(size.width, 0f),
                        )
                    }
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                                modifier = Modifier.size(56.dp),
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.CloudQueue,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = session.email.ifBlank { "Akun tamu" },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "Tier ${session.tier.ifBlank { "Tamu" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                                )
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = onLogout,
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("Keluar dari akun", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("Tampilan")
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        text = "Tema",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    ThemeSegmented(value = themeMode, onChange = onThemeChange)
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Warna dinamis",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
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
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (reduceMotion) {
                                "Animasi diperkecil sesuai skala animator sistem."
                            } else {
                                "Transisi pendek dan mikro-interaksi mengikuti Material 3 Expressive."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("Tentang")
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Gofile Expressive",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Native Kotlin dan Jetpack Compose",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Tanpa WebView. Upload, download, folder, dan progres latar belakang berjalan di aplikasi ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
