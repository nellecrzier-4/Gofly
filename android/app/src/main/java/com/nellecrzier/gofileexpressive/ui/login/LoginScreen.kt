package com.nellecrzier.gofileexpressive.ui.login

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nellecrzier.gofileexpressive.GofileApi
import com.nellecrzier.gofileexpressive.ui.BottomVeil
import com.nellecrzier.gofileexpressive.ui.DotGridBackground
import com.nellecrzier.gofileexpressive.ui.DropletPager
import com.nellecrzier.gofileexpressive.ui.Hyper
import com.nellecrzier.gofileexpressive.ui.HyperButton
import com.nellecrzier.gofileexpressive.ui.HyperExpoOut
import com.nellecrzier.gofileexpressive.ui.HyperEase
import com.nellecrzier.gofileexpressive.ui.HyperPower4Out
import com.nellecrzier.gofileexpressive.ui.HyperTextField
import com.nellecrzier.gofileexpressive.ui.SplitTitle
import com.nellecrzier.gofileexpressive.ui.TopScrim
import com.nellecrzier.gofileexpressive.ui.hyperSpring
import kotlinx.coroutines.delay

private const val PAGE_WELCOME = 0
private const val PAGE_EMAIL = 1
private const val PAGE_TOKEN = 2

/* ============================ masuk / setup =============================== */

/**
 * Tiga halaman: sambutan → email → tautan. Navigasi halaman dikendalikan di
 * sini; [step] hanya menyinkronkan saat ViewModel memindahkan alur (mis. setelah
 * link terkirim atau setelah App Link masuk).
 *
 * Perbaikan penting: halaman tautan selalu bisa dibuka langsung dari halaman
 * email ("Punya tautan? Tempel di sini"), jadi menempel link tidak lagi
 * bergantung pada email yang berhasil terkirim lebih dulu. Tombol aksi utama
 * membaca isian yang sudah di-trim, menerima clipboard lewat ikon tempel, dan
 * bar aksi berada di atas konten (`zIndex`) supaya tidak tertutup input.
 */
@OptIn(ExperimentalAnimationApi::class)
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
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current

    var page by rememberSaveable { mutableStateOf(if (step == "token") PAGE_TOKEN else PAGE_WELCOME) }
    var pasteNotice by remember { mutableStateOf<String?>(null) }

    // Sinkronkan hanya saat ViewModel benar-benar memindahkan alur.
    LaunchedEffect(step) {
        when (step) {
            "token" -> if (page != PAGE_TOKEN) page = PAGE_TOKEN
            "email" -> if (page == PAGE_TOKEN && tokenInput.isBlank()) page = PAGE_EMAIL
        }
    }

    BackHandler(enabled = page > PAGE_WELCOME && !busy) {
        pasteNotice = null
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
    val tokenLooksValid = trimmedToken.isNotBlank() && GofileApi.extractToken(trimmedToken) != null

    fun tap() {
        if (!reduceMotion) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun pasteFromClipboard(): Boolean {
        val text = clipboard.getText()?.text?.trim().orEmpty()
        if (text.isBlank()) {
            pasteNotice = "Clipboard kosong. Salin tautan dari email GoFile dulu."
            return false
        }
        val token = GofileApi.extractToken(text)
        if (token == null) {
            pasteNotice = "Teks di clipboard bukan tautan login GoFile."
            return false
        }
        pasteNotice = "Tautan ditempel dari clipboard."
        onTokenChange(token)
        page = PAGE_TOKEN
        return true
    }

    fun submit(pageIndex: Int): Boolean = when (pageIndex) {
        PAGE_WELCOME -> {
            page = PAGE_EMAIL
            true
        }

        PAGE_EMAIL -> if (emailReady && !busy) {
            onSend()
            true
        } else {
            false
        }

        else -> {
            val token = GofileApi.extractToken(trimmedToken)
            if (token == null) {
                pasteNotice = "Tempel tautan lengkap dari email GoFile, atau token 32 karakternya."
                false
            } else if (busy) {
                false
            } else {
                pasteNotice = null
                onTokenChange(token)
                onLogin(token)
                true
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        DotGridBackground(
            Modifier.fillMaxSize(),
            hero = page == PAGE_WELCOME,
            reduceMotion = reduceMotion,
        )
        TopScrim(Modifier.align(Alignment.TopCenter), height = 148.dp)
        BottomVeil(Modifier.align(Alignment.BottomCenter), height = 268.dp)

        // Pil kecil di kanan atas: lewati sambutan, atau pindah ke halaman tautan.
        Row(
            Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 10.dp, end = 14.dp),
        ) {
            GlassChip(
                text = when (page) {
                    PAGE_WELCOME -> "Lewati"
                    PAGE_EMAIL -> "Punya tautan"
                    else -> "Ganti email"
                },
                onClick = {
                    if (busy) return@GlassChip
                    tap()
                    pasteNotice = null
                    when (page) {
                        PAGE_WELCOME -> page = PAGE_EMAIL
                        PAGE_EMAIL -> {
                            page = PAGE_TOKEN
                            pasteFromClipboard()
                        }

                        else -> {
                            page = PAGE_EMAIL
                            onBackToEmail()
                        }
                    }
                },
            )
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
                                SizeTransform(clip = true),
                            )
                        } else {
                            ContentTransform(
                                slideInHorizontally(slide) { full -> -full } + fadeIn(fade),
                                slideOutHorizontally(tween(340, easing = HyperEase)) { full -> full / 4 } +
                                    fadeOut(tween(340)),
                                0f,
                                SizeTransform(clip = true),
                            )
                        }
                    }
                },
            ) { current ->
                val entrance = remember(current) { Animatable(0f) }
                LaunchedEffect(current) {
                    if (reduceMotion) {
                        entrance.snapTo(1f)
                    } else {
                        entrance.snapTo(0f)
                        entrance.animateTo(1f, hyperSpring(0.58f, 0.86f))
                    }
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        Modifier
                            .widthIn(max = 460.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .graphicsLayer {
                                alpha = entrance.value
                                translationY = 34f * (1f - entrance.value)
                                scaleX = 0.98f + 0.02f * entrance.value
                                scaleY = 0.98f + 0.02f * entrance.value
                            }
                            .padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when (current) {
                            PAGE_WELCOME -> WelcomePage(reduceMotion)

                            PAGE_EMAIL -> GlassPanel {
                                EmailPage(
                                    email = email,
                                    emailInvalid = emailInvalid,
                                    emailReady = emailReady,
                                    busy = busy,
                                    notice = pasteNotice,
                                    onEmailChange = onEmailChange,
                                    onGuest = {
                                        tap()
                                        onGuest()
                                    },
                                    onSend = {
                                        tap()
                                        submit(PAGE_EMAIL)
                                    },
                                    onPasteLink = {
                                        tap()
                                        if (!pasteFromClipboard()) page = PAGE_TOKEN
                                    },
                                )
                            }

                            else -> GlassPanel {
                                TokenPage(
                                    tokenInput = tokenInput,
                                    tokenValid = tokenLooksValid,
                                    email = email,
                                    busy = busy,
                                    notice = pasteNotice,
                                    onTokenChange = {
                                        pasteNotice = null
                                        onTokenChange(it)
                                    },
                                    onPaste = {
                                        tap()
                                        pasteFromClipboard()
                                    },
                                    onLogin = {
                                        tap()
                                        submit(PAGE_TOKEN)
                                    },
                                    onResend = { onSend() },
                                    onChangeEmail = {
                                        page = PAGE_EMAIL
                                        onBackToEmail()
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                DropletPager(count = 3, index = page)
            }

            ActionBar(
                page = page,
                busy = busy,
                emailReady = emailReady,
                tokenLooksValid = tokenLooksValid,
                reduceMotion = reduceMotion,
                onBack = {
                    tap()
                    pasteNotice = null
                    if (page == PAGE_TOKEN) {
                        page = PAGE_EMAIL
                        onBackToEmail()
                    } else {
                        page = PAGE_WELCOME
                    }
                },
                onNext = {
                    tap()
                    submit(page)
                },
            )
        }
    }
}

/* ------------------------------- bingkai kaca ----------------------------- */

/**
 * Panel kaca HyperSpring: isi gelap tembus pandang, garis rambut, kilau tipis
 * di tepi atas, dan sudut besar. Dipakai untuk dua halaman formulir supaya
 * bidang isian punya "bahan" yang sama dengan kartu di layar lain.
 */
@Composable
private fun GlassPanel(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Hyper.Card.copy(alpha = 0.92f),
                        Hyper.Card.copy(alpha = 0.78f),
                    ),
                ),
            )
            .graphicsLayer { alpha = 1f }
            .padding(20.dp),
    ) {
        // Kilau atas: garis putih 6% yang memudar ke tengah.
        Box(
            Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.verticalGradient(
                        0f to Hyper.GlassHighlight,
                        0.09f to Color.Transparent,
                        1f to Color.Transparent,
                    ),
                ),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}

@Composable
private fun GlassChip(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Hyper.GlassFill,
        border = BorderStroke(0.8.dp, Hyper.Hairline),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Hyper.Text2,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/**
 * Bar aksi yang selalu terlihat. `zIndex` memastikan bar ini menerima ketukan
 * walaupun konten halaman menggambar sampai tepi bawah saat keyboard terbuka.
 */
@Composable
private fun ActionBar(
    page: Int,
    busy: Boolean,
    emailReady: Boolean,
    tokenLooksValid: Boolean,
    reduceMotion: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val nextEnabled = when (page) {
        PAGE_WELCOME -> !busy
        PAGE_EMAIL -> emailReady && !busy
        else -> tokenLooksValid && !busy
    }
    // Satu tombol aksi selebar layar; "Kembali" jadi aksi tenang di bawahnya.
    // Tidak memakai weight di dalam Row: pada skala teks besar isi tombol bisa
    // melebihi lebar yang dialokasikan dan kapsul tergambar keluar layar.
    Column(
        Modifier
            .zIndex(2f)
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Hyper.Bg.copy(alpha = 0.94f)),
                ),
            )
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HyperButton(
            text = when {
                busy && page == PAGE_EMAIL -> "Mengirim…"
                busy && page == PAGE_TOKEN -> "Memeriksa…"
                page == PAGE_TOKEN -> "Masuk"
                page == PAGE_EMAIL -> "Kirim tautan"
                else -> "Lanjut"
            },
            primary = true,
            squash = true,
            loading = busy,
            reduceMotion = reduceMotion,
            enabled = nextEnabled,
            height = 56.dp,
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
        )
        if (page > PAGE_WELCOME) {
            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = onBack,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Kembali",
                    color = Hyper.Text2,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

/* --------------------------------- halaman -------------------------------- */

@Composable
private fun WelcomePage(reduceMotion: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AccentGlow(Modifier.fillMaxWidth().height(160.dp))
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
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Klien cloud native untuk mengunggah, mengunduh, dan merapikan folder tanpa WebView.",
            style = MaterialTheme.typography.bodyLarge,
            color = Hyper.Text2,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(26.dp))
        WelcomeFeature(
            icon = Icons.Filled.CloudQueue,
            title = "Native dan ringan",
            body = "Dibangun dengan Kotlin dan Jetpack Compose.",
            delayIndex = 0,
            reduceMotion = reduceMotion,
        )
        Spacer(Modifier.height(10.dp))
        WelcomeFeature(
            icon = Icons.Filled.Upload,
            title = "Transfer latar belakang",
            body = "Progres tetap berjalan lewat foreground service.",
            delayIndex = 1,
            reduceMotion = reduceMotion,
        )
        Spacer(Modifier.height(10.dp))
        WelcomeFeature(
            icon = Icons.Filled.Folder,
            title = "Folder rapi",
            body = "Buat, buka, dan hapus isi cloud langsung dari aplikasi.",
            delayIndex = 2,
            reduceMotion = reduceMotion,
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Masuk dengan tautan email, tempel tautan yang sudah kamu punya, atau coba sebagai tamu.",
            style = MaterialTheme.typography.bodySmall,
            color = Hyper.Text3,
            textAlign = TextAlign.Center,
        )
    }
}

/** Cahaya koral lembut di belakang judul — khas HyperSpring, bukan kartu. */
@Composable
private fun AccentGlow(modifier: Modifier = Modifier) {
    Box(
        modifier.background(
            Brush.radialGradient(
                colors = listOf(
                    Hyper.Accent.copy(alpha = 0.16f),
                    Hyper.Accent.copy(alpha = 0.05f),
                    Color.Transparent,
                ),
                center = Offset.Unspecified,
                radius = 520f,
            ),
        ),
    )
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
            .background(Hyper.GlassFill)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(15.dp),
            color = Hyper.AccentSoft,
            border = BorderStroke(0.8.dp, Hyper.AccentEdge),
            modifier = Modifier.size(46.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Hyper.Accent, modifier = Modifier.size(21.dp))
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
    notice: String?,
    onEmailChange: (String) -> Unit,
    onGuest: () -> Unit,
    onSend: () -> Unit,
    onPasteLink: () -> Unit,
) {
    Eyebrow("TAUTAN EMAIL")
    Spacer(Modifier.height(8.dp))
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
    Spacer(Modifier.height(22.dp))
    HyperTextField(
        value = email,
        onValueChange = onEmailChange,
        label = "Alamat email",
        placeholder = "nama@contoh.com",
        leadingIcon = Icons.Filled.Email,
        supporting = when {
            emailInvalid -> "Format email belum benar."
            notice != null -> notice
            else -> "Tautan login hanya dikirim ke alamat ini."
        },
        isError = emailInvalid,
        enabled = !busy,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Send,
        ),
        keyboardActions = KeyboardActions(onSend = { if (emailReady && !busy) onSend() }),
    )
    Spacer(Modifier.height(4.dp))
    // Jalan pintas: jangan paksa mengirim email dulu kalau tautannya sudah ada.
    TextButton(
        onClick = onPasteLink,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            Icons.Filled.Link,
            contentDescription = null,
            tint = Hyper.Accent,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text("Sudah punya tautan? Tempel di sini", color = Hyper.Accent, style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(6.dp))
    HyperButton(
        text = "Lanjut sebagai tamu",
        icon = Icons.Filled.CloudQueue,
        primary = false,
        enabled = !busy,
        onClick = onGuest,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(10.dp))
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
    tokenValid: Boolean,
    email: String,
    busy: Boolean,
    notice: String?,
    onTokenChange: (String) -> Unit,
    onPaste: () -> Unit,
    onLogin: () -> Unit,
    onResend: () -> Unit,
    onChangeEmail: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current

    Eyebrow("TAUTAN LOGIN")
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Cek email kamu",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.ExtraBold,
        color = Hyper.Text,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = "Buka email dari GoFile, ketuk linknya untuk langsung masuk ke aplikasi — atau tempel link/token di bawah.",
        style = MaterialTheme.typography.bodyMedium,
        color = Hyper.Text2,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(22.dp))
    HyperTextField(
        value = tokenInput,
        onValueChange = onTokenChange,
        label = "Tautan atau token dari email",
        placeholder = "https://gofile.io/login/...",
        leadingIcon = Icons.Filled.Key,
        trailing = {
            IconButton(
                onClick = onPaste,
                enabled = !busy,
            ) {
                Icon(
                    Icons.Filled.ContentPaste,
                    contentDescription = "Tempel dari clipboard",
                    tint = Hyper.Accent,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        supporting = when {
            notice != null -> notice
            tokenInput.isNotBlank() && !tokenValid -> "Belum dikenali sebagai tautan GoFile."
            tokenValid -> "Tautan dikenali. Tekan Masuk."
            else -> "Bisa juga tempel token 32 karakter tanpa tautan."
        },
        isError = tokenInput.isNotBlank() && !tokenValid,
        enabled = !busy,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = { if (tokenValid && !busy) onLogin() },
        ),
    )
    Spacer(Modifier.height(6.dp))
    if (clipboard.getText() != null) {
        TextButton(onClick = onPaste, enabled = !busy) {
            Text("Tempel dari clipboard", color = Hyper.Accent)
        }
    }
    Spacer(Modifier.height(2.dp))
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

@Composable
private fun Eyebrow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Hyper.Accent),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.4.sp,
            color = Hyper.Text2,
        )
    }
}
