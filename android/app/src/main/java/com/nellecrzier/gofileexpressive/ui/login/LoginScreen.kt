package com.nellecrzier.gofileexpressive.ui.login

import android.os.Build
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nellecrzier.gofileexpressive.BuildConfig
import com.nellecrzier.gofileexpressive.GItem
import com.nellecrzier.gofileexpressive.GSession
import com.nellecrzier.gofileexpressive.GoFileException
import com.nellecrzier.gofileexpressive.GofileApi
import com.nellecrzier.gofileexpressive.MainActivity
import com.nellecrzier.gofileexpressive.SessionStore
import com.nellecrzier.gofileexpressive.TransferInfo
import com.nellecrzier.gofileexpressive.TransferStore
import com.nellecrzier.gofileexpressive.ui.Crumb
import com.nellecrzier.gofileexpressive.ui.readableSize
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.nellecrzier.gofileexpressive.ui.*

/* ============================ masuk / setup =============================== */

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
