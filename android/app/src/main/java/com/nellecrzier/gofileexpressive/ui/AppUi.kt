package com.nellecrzier.gofileexpressive.ui

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GofileApp(activity: MainActivity) {
    GofileTheme {
        val context = LocalContext.current
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
            loginStep = "email"
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

        BackHandler(enabled = stack.size > 1) {
            stack = stack.dropLast(1)
            loadFolder(stack.last().id)
        }

        Scaffold(
            topBar = {
                if (loginStep == "ready") {
                    TopAppBar(
                        title = { Text(stack.lastOrNull()?.name ?: "GoFile") },
                        navigationIcon = {
                            if (stack.size > 1) {
                                IconButton(onClick = {
                                    stack = stack.dropLast(1)
                                    loadFolder(stack.last().id)
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    newFolderName = ""
                                    showCreateFolder = true
                                },
                                enabled = !busy,
                            ) {
                                Icon(Icons.Filled.CreateNewFolder, contentDescription = "Folder baru")
                            }
                            IconButton(onClick = { stack.lastOrNull()?.let { loadFolder(it.id) } }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Muat ulang")
                            }
                            IconButton(onClick = { logout() }) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Keluar")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }
            },
            floatingActionButton = {
                if (loginStep == "ready") {
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

                if (loginStep == "ready") {
                    Box(Modifier.weight(1f)) {
                        FolderList(
                            items = items,
                            busy = busy,
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
                                stack.lastOrNull()?.let { activity.pickFileForUpload(it.id, session.token) }
                            },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxSize()
                                .widthIn(max = 720.dp),
                        )
                        TransferPanel(
                            transfers = transfers,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                } else {
                    LoginScreen(
                        step = loginStep,
                        email = email,
                        tokenInput = tokenInput,
                        busy = busy,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreen(
    step: String,
    email: String,
    tokenInput: String,
    busy: Boolean,
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
                        modifier = Modifier.size(38.dp),
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

            if (isEmailStep) {
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
        items(items, key = { it.id }) { item ->
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
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
}

@Composable
private fun TransferPanel(transfers: List<TransferInfo>, modifier: Modifier = Modifier) {
    val recent = transfers.takeLast(3).asReversed()
    if (recent.isEmpty()) return
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
