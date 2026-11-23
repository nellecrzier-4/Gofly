@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.nellecrzier.gofileexpressive.ui.login

import com.nellecrzier.gofileexpressive.ui.components.AppLoadingIndicator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.nellecrzier.gofileexpressive.GofileApi
import com.nellecrzier.gofileexpressive.ui.motion.motionScheme
import com.nellecrzier.gofileexpressive.ui.motion.staggeredEnter

private const val PAGE_WELCOME = 0
private const val PAGE_EMAIL = 1
private const val PAGE_TOKEN = 2

/**
 * Masuk tiga langkah dengan komponen Material 3.
 *
 * Alur: sambutan → email → tautan. Halaman tautan bisa dibuka langsung dari
 * halaman email, tombol tempel membaca clipboard, dan tombol utama meneruskan
 * token yang sudah di-trim ke ViewModel (tidak membaca state lama).
 *
 * Gerak: pergantian halaman memudar dengan spesifikasi effects dari
 * `motionScheme`, isi halaman masuk bertahap (`staggeredEnter`), pesan
 * galat/notis membuka ruang dengan `expandVertically` + `fadeIn`, dan label
 * tombol utama berpindah lebar/alfa dengan spesifikasi spatial + effects.
 */
@Composable
internal fun LoginScreen(
    step: String,
    email: String,
    tokenInput: String,
    busy: Boolean,
    reduceMotion: Boolean,
    onEmailChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onBackToEmail: () -> Unit,
    onSend: () -> Unit,
    onLogin: (String) -> Unit,
    onGuest: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var page by rememberSaveable { mutableStateOf(if (step == "token") PAGE_TOKEN else PAGE_WELCOME) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(step) {
        when (step) {
            "token" -> if (page != PAGE_TOKEN) page = PAGE_TOKEN
            "email" -> if (page == PAGE_TOKEN && tokenInput.isBlank()) page = PAGE_EMAIL
        }
    }

    BackHandler(enabled = page > PAGE_WELCOME && !busy) {
        notice = null
        if (page == PAGE_TOKEN) {
            page = PAGE_EMAIL
            onBackToEmail()
        } else {
            page = PAGE_WELCOME
        }
    }

    val trimmedEmail = email.trim()
    val emailInvalid = trimmedEmail.isNotBlank() &&
        !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()
    val emailReady = trimmedEmail.isNotBlank() && !emailInvalid
    val trimmedToken = tokenInput.trim()
    val tokenValid = GofileApi.extractToken(trimmedToken) != null

    fun paste(): Boolean {
        val text = clipboard.getText()?.text?.trim().orEmpty()
        if (text.isBlank()) {
            notice = "Clipboard kosong. Salin tautan dari email GoFile dulu."
            return false
        }
        val token = GofileApi.extractToken(text)
        if (token == null) {
            notice = "Teks di clipboard bukan tautan login GoFile."
            return false
        }
        onTokenChange(token)
        page = PAGE_TOKEN
        notice = null
        return true
    }

    fun submit(): Boolean = when (page) {
        PAGE_WELCOME -> { page = PAGE_EMAIL; true }
        PAGE_EMAIL -> if (emailReady && !busy) { onSend(); true } else false
        else -> {
            val token = GofileApi.extractToken(trimmedToken)
            if (token == null) {
                notice = "Tempel tautan lengkap dari email GoFile, atau token 32 karakternya."
                false
            } else if (busy) false else {
                onTokenChange(token)
                onLogin(token)
                true
            }
        }
    }

    // Spesifikasi gerak dari skema tema; saat gerak diredupkan semua menjadi snap.
    val fadeSpec = if (reduceMotion) tween<Float>(0) else motionScheme.fastEffectsSpec<Float>()
    val expandSpec = if (reduceMotion) tween<IntSize>(0) else motionScheme.fastSpatialSpec<IntSize>()
    val pageEnterSpec = if (reduceMotion) tween<Float>(0) else motionScheme.slowEffectsSpec<Float>()
    val pageExitSpec = if (reduceMotion) tween<Float>(0) else motionScheme.fastEffectsSpec<Float>()
    // Ukuran label tombol ikut spesifikasi spatial, bukan tween tetap.
    val labelSizeSpec = if (reduceMotion) tween<IntSize>(0) else motionScheme.fastSpatialSpec<IntSize>()

    val submitLabel = when {
        busy && page == PAGE_EMAIL -> "Mengirim…"
        busy && page == PAGE_TOKEN -> "Memeriksa…"
        page == PAGE_TOKEN -> "Masuk"
        page == PAGE_EMAIL -> "Kirim tautan"
        else -> "Lanjut"
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = {
                    if (busy) return@TextButton
                    notice = null
                    when (page) {
                        PAGE_WELCOME -> page = PAGE_EMAIL
                        PAGE_EMAIL -> { page = PAGE_TOKEN; paste() }
                        else -> { page = PAGE_EMAIL; onBackToEmail() }
                    }
                },
                enabled = !busy,
            ) {
                Text(
                    when (page) {
                        PAGE_WELCOME -> "Lewati"
                        PAGE_EMAIL -> "Punya tautan"
                        else -> "Ganti email"
                    },
                )
            }
        }

        AnimatedContent(
            targetState = page,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            label = "loginPage",
            transitionSpec = {
                fadeIn(pageEnterSpec) togetherWith fadeOut(pageExitSpec)
            },
        ) { current ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier
                        .widthIn(max = 440.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (current) {
                        PAGE_WELCOME -> WelcomePage(reduceMotion = reduceMotion)
                        PAGE_EMAIL -> EmailPage(
                            email = email,
                            emailInvalid = emailInvalid,
                            busy = busy,
                            notice = notice,
                            onEmailChange = onEmailChange,
                            onGuest = onGuest,
                            onPasteLink = { paste(); page = PAGE_TOKEN },
                            reduceMotion = reduceMotion,
                        )
                        else -> TokenPage(
                            tokenInput = tokenInput,
                            tokenValid = tokenValid,
                            email = email,
                            busy = busy,
                            notice = notice,
                            onTokenChange = { notice = null; onTokenChange(it) },
                            onPaste = { paste() },
                            onResend = onSend,
                            onChangeEmail = { page = PAGE_EMAIL; onBackToEmail() },
                            reduceMotion = reduceMotion,
                        )
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (page > PAGE_WELCOME) {
                FilledTonalButton(
                    onClick = {
                        notice = null
                        if (page == PAGE_TOKEN) { page = PAGE_EMAIL; onBackToEmail() } else page = PAGE_WELCOME
                    },
                    enabled = !busy,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.height(56.dp),
                ) { Text("Kembali") }
            }
            Button(
                onClick = { submit() },
                enabled = when (page) {
                    PAGE_WELCOME -> !busy
                    PAGE_EMAIL -> emailReady && !busy
                    else -> tokenValid && !busy
                },
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.weight(1f).height(56.dp),
            ) {
                // Indikator tetap di dalam tombol; lebarnya dibuka dengan
                // spesifikasi spatial, alfanya dengan spesifikasi effects.
                AnimatedVisibility(
                    visible = busy,
                    enter = fadeIn(fadeSpec) + expandHorizontally(expandSpec),
                    exit = fadeOut(fadeSpec) + shrinkHorizontally(expandSpec),
                ) {
                    AppLoadingIndicator(
                        size = 18.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        reduceMotion = reduceMotion,
                    )
                    Spacer(Modifier.width(10.dp))
                }
                AnimatedContent(
                    targetState = submitLabel,
                    transitionSpec = {
                        ContentTransform(
                            fadeIn(fadeSpec),
                            fadeOut(fadeSpec),
                            0f,
                            SizeTransform(clip = false) { _, _ -> labelSizeSpec },
                        )
                    },
                    contentAlignment = Alignment.Center,
                    label = "loginSubmitLabel",
                ) { label ->
                    Text(label)
                }
            }
        }
    }
}

/**
 * Pesan bantuan di bawah kolom: satu pesan tampil pada satu waktu, tetapi
 * perpindahan antara petunjuk netral dan galat/notis dianimasikan dengan
 * `expandVertically` + `fadeIn` (dan kebalikannya saat hilang).
 */
@Composable
private fun AnimatedSupportingText(
    hint: String,
    issue: String?,
    reduceMotion: Boolean,
) {
    val fadeSpec = if (reduceMotion) tween<Float>(0) else motionScheme.fastEffectsSpec<Float>()
    val expandSpec = if (reduceMotion) tween<IntSize>(0) else motionScheme.fastSpatialSpec<IntSize>()
    // Teks galat terakhir dipertahankan agar animasi keluar tidak mengosongkan ruang.
    var lastIssue by remember { mutableStateOf("") }
    LaunchedEffect(issue) { if (issue != null) lastIssue = issue }

    Column(Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = issue != null,
            enter = fadeIn(fadeSpec) + expandVertically(expandSpec),
            exit = fadeOut(fadeSpec) + shrinkVertically(expandSpec),
        ) {
            Text(issue ?: lastIssue)
        }
        AnimatedVisibility(
            visible = issue == null,
            enter = fadeIn(fadeSpec) + expandVertically(expandSpec),
            exit = fadeOut(fadeSpec) + shrinkVertically(expandSpec),
        ) {
            Text(hint)
        }
    }
}

@Composable
private fun WelcomePage(reduceMotion: Boolean) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    Text(
        text = "Gofile Expressive",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.staggeredEnter(0, reduceMotion, entered),
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = "Kelola berkas GoFile tanpa WebView: unggah, unduh, pratinjau, dan atur folder.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.staggeredEnter(1, reduceMotion, entered),
    )
    Spacer(Modifier.height(24.dp))
    AssistChip(
        onClick = {},
        label = { Text("Native · Jetpack Compose") },
        leadingIcon = {
            Icon(Icons.Filled.CloudQueue, contentDescription = null, modifier = Modifier.size(18.dp))
        },
        modifier = Modifier.staggeredEnter(2, reduceMotion, entered),
    )
    Spacer(Modifier.height(18.dp))
    Text(
        text = "Masuk dengan tautan email, tempel tautan yang sudah kamu punya, atau coba sebagai tamu.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.staggeredEnter(3, reduceMotion, entered),
    )
}

@Composable
private fun EmailPage(
    email: String,
    emailInvalid: Boolean,
    busy: Boolean,
    notice: String?,
    onEmailChange: (String) -> Unit,
    onGuest: () -> Unit,
    onPasteLink: () -> Unit,
    reduceMotion: Boolean,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    Text(
        text = "Masuk ke GoFile",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.staggeredEnter(0, reduceMotion, entered),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Kami kirim tautan login sekali pakai ke email kamu. Ketuk linknya — langsung membuka aplikasi ini.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.staggeredEnter(1, reduceMotion, entered),
    )
    Spacer(Modifier.height(20.dp))
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text("Alamat email") },
        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
        supportingText = {
            AnimatedSupportingText(
                hint = "Tautan login hanya dikirim ke alamat ini.",
                issue = when {
                    emailInvalid -> "Format email belum benar."
                    notice != null -> notice
                    else -> null
                },
                reduceMotion = reduceMotion,
            )
        },
        isError = emailInvalid,
        enabled = !busy,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { if (!busy) onGuest }),
        modifier = Modifier
            .fillMaxWidth()
            .staggeredEnter(2, reduceMotion, entered),
    )
    Spacer(Modifier.height(4.dp))
    TextButton(
        onClick = onPasteLink,
        enabled = !busy,
        modifier = Modifier
            .fillMaxWidth()
            .staggeredEnter(3, reduceMotion, entered),
    ) {
        Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("Sudah punya tautan? Tempel di sini")
    }
    Spacer(Modifier.height(8.dp))
    FilledTonalButton(
        onClick = onGuest,
        enabled = !busy,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .staggeredEnter(4, reduceMotion, entered),
    ) {
        Icon(Icons.Filled.CloudQueue, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Lanjut sebagai tamu")
    }
}

@Composable
private fun TokenPage(
    tokenInput: String,
    tokenValid: Boolean,
    email: String,
    busy: Boolean,
    notice: String?,
    onTokenChange: (String) -> Unit,
    onPaste: () -> Unit,
    onResend: () -> Unit,
    onChangeEmail: () -> Unit,
    reduceMotion: Boolean,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    Text(
        text = "Cek email kamu",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.staggeredEnter(0, reduceMotion, entered),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Buka email dari GoFile, ketuk linknya untuk langsung masuk — atau tempel link/token di bawah.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.staggeredEnter(1, reduceMotion, entered),
    )
    Spacer(Modifier.height(20.dp))
    OutlinedTextField(
        value = tokenInput,
        onValueChange = onTokenChange,
        label = { Text("Tautan atau token dari email") },
        leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onPaste, enabled = !busy) {
                Icon(Icons.Filled.ContentPaste, contentDescription = "Tempel dari clipboard")
            }
        },
        supportingText = {
            AnimatedSupportingText(
                hint = if (tokenValid) {
                    "Tautan dikenali."
                } else {
                    "Bisa juga tempel token 32 karakter."
                },
                issue = when {
                    notice != null -> notice
                    tokenInput.isNotBlank() && !tokenValid -> "Belum dikenali sebagai tautan GoFile."
                    else -> null
                },
                reduceMotion = reduceMotion,
            )
        },
        isError = tokenInput.isNotBlank() && !tokenValid,
        enabled = !busy,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier
            .fillMaxWidth()
            .staggeredEnter(2, reduceMotion, entered),
    )
    Spacer(Modifier.height(4.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .staggeredEnter(3, reduceMotion, entered),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onResend, enabled = email.isNotBlank() && !busy) { Text("Kirim ulang") }
        TextButton(onClick = onChangeEmail, enabled = !busy) { Text("Ganti email") }
    }
}
