@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.nellecrzier.gofly.ui.login

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.nellecrzier.gofly.GofileApi
import com.nellecrzier.gofly.R
import com.nellecrzier.gofly.ThemeMode
import com.nellecrzier.gofly.ui.GofileTheme
import com.nellecrzier.gofly.ui.components.AppLoadingIndicator
import com.nellecrzier.gofly.ui.components.animatedConnectedShape
import com.nellecrzier.gofly.ui.motion.pressMorphShape
import com.nellecrzier.gofly.ui.motion.motionScheme
import com.nellecrzier.gofly.ui.motion.staggeredEnter
import kotlinx.coroutines.delay

private const val PAGE_WELCOME = 0
private const val PAGE_EMAIL = 1
private const val PAGE_TOKEN = 2
private const val STEP_COUNT = 3

/**
 * Masuk tiga langkah dalam satu layar kontinu.
 *
 * Hero, judul, kartu formulir, dan tombol utama tetap berada di pohon
 * komposisi yang sama; perpindahan langkah hanya mengganti isi kartu dengan
 * shared axis kecil + fade, dan tinggi kartu dianimasikan. Semua gerak memakai
 * `MaterialTheme.motionScheme` dan menghormati setelan kurangi gerak.
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var page by rememberSaveable { mutableStateOf(if (step == "token") PAGE_TOKEN else PAGE_WELCOME) }
    var notice by remember { mutableStateOf<String?>(null) }
    var tokenVisible by remember { mutableStateOf(false) }
    var guidanceOpen by rememberSaveable { mutableStateOf(false) }
    var clipboardToken by remember { mutableStateOf<String?>(null) }
    var emailSettled by remember { mutableStateOf(false) }

    LaunchedEffect(step) {
        when (step) {
            "token" -> if (page != PAGE_TOKEN) page = PAGE_TOKEN
            "email" -> if (page == PAGE_TOKEN && tokenInput.isBlank()) page = PAGE_EMAIL
        }
    }

    // Debounce validasi email 400 ms supaya galat tidak berkedip saat mengetik.
    LaunchedEffect(email) {
        emailSettled = false
        delay(400)
        emailSettled = true
    }

    // Chip tempel hanya muncul bila clipboard berisi tautan/token yang dikenali.
    LaunchedEffect(page, tokenInput) {
        if (page == PAGE_TOKEN) {
            clipboardToken = GofileApi.extractToken(clipboard.getText()?.text.orEmpty())
        }
    }

    val trimmedEmail = email.trim()
    val emailInvalid = trimmedEmail.isNotBlank() &&
        !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()
    val emailReady = trimmedEmail.isNotBlank() && !emailInvalid
    val showEmailError = emailSettled && emailInvalid
    val trimmedToken = tokenInput.trim()
    val tokenValid = GofileApi.extractToken(trimmedToken) != null

    fun goTo(target: Int) {
        if (busy || target == page) return
        notice = null
        if (page == PAGE_TOKEN && target == PAGE_EMAIL) onBackToEmail()
        page = target
    }

    fun paste(): Boolean {
        val text = clipboard.getText()?.text?.trim().orEmpty()
        if (text.isBlank()) {
            notice = context.getString(R.string.login_clipboard_empty)
            return false
        }
        val token = GofileApi.extractToken(text)
        if (token == null) {
            notice = context.getString(R.string.login_clipboard_invalid)
            return false
        }
        onTokenChange(token)
        page = PAGE_TOKEN
        notice = null
        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
        return true
    }

    fun submit(): Boolean = when (page) {
        PAGE_WELCOME -> { page = PAGE_EMAIL; true }
        PAGE_EMAIL -> if (emailReady && !busy) { onSend(); true } else false
        else -> {
            val token = GofileApi.extractToken(trimmedToken)
            if (token == null) {
                notice = context.getString(R.string.login_token_paste_hint)
                false
            } else if (busy) false else {
                onTokenChange(token)
                onLogin(token)
                true
            }
        }
    }

    val spatial = motionScheme.defaultSpatialSpec<Float>()
    val spatialDp = motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.Dp>()
    val effects = motionScheme.fastEffectsSpec<Float>()
    val heroSize by animateDpAsState(
        targetValue = if (page == PAGE_WELCOME) 84.dp else 56.dp,
        animationSpec = if (reduceMotion) tween(0) else spatialDp,
        label = "heroSize",
    )
    val heroProgress by animateFloatAsState(
        targetValue = if (page == PAGE_WELCOME) 0f else 1f,
        animationSpec = if (reduceMotion) tween(0) else spatial,
        label = "heroProgress",
    )

    var backProgress by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    PredictiveBackHandler(enabled = page > PAGE_WELCOME && !busy) { events ->
        try {
            events.collect { backProgress = it.progress }
            goTo(if (page == PAGE_TOKEN) PAGE_EMAIL else PAGE_WELCOME)
        } finally {
            backProgress = 0f
        }
    }
    BackHandler(enabled = page > PAGE_WELCOME && !busy) {
        goTo(if (page == PAGE_TOKEN) PAGE_EMAIL else PAGE_WELCOME)
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
    ) {
        val twoPane = maxWidth >= 720.dp
        if (twoPane) {
            Row(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 40.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(48.dp),
            ) {
                HeroPanel(
                    page = page,
                    progress = heroProgress,
                    size = 128.dp,
                    reduceMotion = reduceMotion,
                    modifier = Modifier.weight(0.9f),
                )
                LoginFormColumn(
                    page = page,
                    email = email,
                    emailReady = emailReady,
                    showEmailError = showEmailError,
                    tokenInput = tokenInput,
                    tokenValid = tokenValid,
                    tokenVisible = tokenVisible,
                    guidanceOpen = guidanceOpen,
                    clipboardToken = clipboardToken,
                    notice = notice,
                    busy = busy,
                    reduceMotion = reduceMotion,
                    backProgress = backProgress,
                    heroSize = heroSize,
                    heroProgress = heroProgress,
                    modifier = Modifier.weight(1.1f).widthIn(max = 460.dp),
                    onEmailChange = { notice = null; onEmailChange(it) },
                    onTokenChange = { notice = null; onTokenChange(it) },
                    onTokenVisible = { tokenVisible = !tokenVisible },
                    onGuidance = { guidanceOpen = !guidanceOpen },
                    onPaste = { paste() },
                    onGuest = onGuest,
                    onPasteLink = { page = PAGE_TOKEN; paste() },
                    onSend = onSend,
                    onBack = { goTo(if (page == PAGE_TOKEN) PAGE_EMAIL else PAGE_WELCOME) },
                    onSubmit = { submit() },
                    onSkip = { goTo(PAGE_EMAIL) },
                )
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp),
            ) {
                LoginFormColumn(
                    page = page,
                    email = email,
                    emailReady = emailReady,
                    showEmailError = showEmailError,
                    tokenInput = tokenInput,
                    tokenValid = tokenValid,
                    tokenVisible = tokenVisible,
                    guidanceOpen = guidanceOpen,
                    clipboardToken = clipboardToken,
                    notice = notice,
                    busy = busy,
                    reduceMotion = reduceMotion,
                    backProgress = backProgress,
                    heroSize = heroSize,
                    heroProgress = heroProgress,
                    modifier = Modifier.fillMaxSize(),
                    onEmailChange = { notice = null; onEmailChange(it) },
                    onTokenChange = { notice = null; onTokenChange(it) },
                    onTokenVisible = { tokenVisible = !tokenVisible },
                    onGuidance = { guidanceOpen = !guidanceOpen },
                    onPaste = { paste() },
                    onGuest = onGuest,
                    onPasteLink = { page = PAGE_TOKEN; paste() },
                    onSend = onSend,
                    onBack = { goTo(if (page == PAGE_TOKEN) PAGE_EMAIL else PAGE_WELCOME) },
                    onSubmit = { submit() },
                    onSkip = { goTo(PAGE_EMAIL) },
                )
            }
        }
    }
}

@Composable
private fun LoginFormColumn(
    page: Int,
    email: String,
    emailReady: Boolean,
    showEmailError: Boolean,
    tokenInput: String,
    tokenValid: Boolean,
    tokenVisible: Boolean,
    guidanceOpen: Boolean,
    clipboardToken: String?,
    notice: String?,
    busy: Boolean,
    reduceMotion: Boolean,
    backProgress: Float,
    heroSize: androidx.compose.ui.unit.Dp,
    heroProgress: Float,
    modifier: Modifier,
    onEmailChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onTokenVisible: () -> Unit,
    onGuidance: () -> Unit,
    onPaste: () -> Unit,
    onGuest: () -> Unit,
    onPasteLink: () -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit,
) {
    val spatial = motionScheme.defaultSpatialSpec<Float>()
    val spatialInt = motionScheme.defaultSpatialSpec<IntOffset>()
    val spatialSize = motionScheme.defaultSpatialSpec<IntSize>()
    val effects = motionScheme.fastEffectsSpec<Float>()
    val snap = tween<Float>(0)
    val interaction = remember { MutableInteractionSource() }
    val ctaShape = pressMorphShape(interaction, reduceMotion)

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // Bar kembali + lewati.
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = page > PAGE_WELCOME,
                enter = fadeIn(if (reduceMotion) tween(100) else effects) +
                    expandHorizontally(spatialSize) { it / 2 },
                exit = fadeOut(if (reduceMotion) tween(80) else effects) +
                    shrinkHorizontally(spatialSize) { it / 2 },
            ) {
                FilledTonalIconButton(onClick = onBack, enabled = !busy, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.login_back),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (page == PAGE_WELCOME) {
                TextButton(onClick = onSkip, enabled = !busy) {
                    Text(stringResource(R.string.login_skip))
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        HeroInline(
            page = page,
            progress = heroProgress,
            size = heroSize,
            reduceMotion = reduceMotion,
        )

        Spacer(Modifier.height(12.dp))
        // Area tengah bisa digulir: keyboard, font scale 1.3, atau layar pendek
        // tidak boleh memampatkan teks/CTA.
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        val forward = targetState > initialState
                        (
                            fadeIn(if (reduceMotion) tween(100) else effects) +
                                slideInHorizontally(if (reduceMotion) tween(0) else spatialInt) { full ->
                                    if (forward) full / 14 else -full / 14
                                }
                            ) togetherWith (
                            fadeOut(if (reduceMotion) tween(80) else effects) +
                                slideOutHorizontally(if (reduceMotion) tween(0) else spatialInt) { full ->
                                    if (forward) -full / 18 else full / 18
                                }
                            )
                    },
                    label = "loginStep",
                ) { current ->
                    when (current) {
                        PAGE_WELCOME -> Column(Modifier.fillMaxWidth()) {
                            WelcomeContent(reduceMotion = reduceMotion)
                        }
                        PAGE_EMAIL -> Column(Modifier.fillMaxWidth()) {
                            EmailContent(
                            email = email,
                            emailReady = emailReady,
                            showError = showEmailError,
                            notice = notice,
                            busy = busy,
                            reduceMotion = reduceMotion,
                            onEmailChange = onEmailChange,
                            onGuest = onGuest,
                                onPasteLink = onPasteLink,
                                onSubmit = onSubmit,
                            )
                        }
                        else -> Column(Modifier.fillMaxWidth()) {
                            TokenContent(
                            tokenInput = tokenInput,
                            tokenValid = tokenValid,
                            tokenVisible = tokenVisible,
                            guidanceOpen = guidanceOpen,
                            clipboardToken = clipboardToken,
                            notice = notice,
                            busy = busy,
                            reduceMotion = reduceMotion,
                            onTokenChange = onTokenChange,
                            onTokenVisible = onTokenVisible,
                            onGuidance = onGuidance,
                            onPaste = onPaste,
                                onResend = onSend,
                                onSubmit = onSubmit,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        StepProgress(page = page, reduceMotion = reduceMotion)
        Spacer(Modifier.height(14.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onSubmit,
                enabled = when (page) {
                    PAGE_WELCOME -> !busy
                    PAGE_EMAIL -> emailReady && !busy
                    else -> tokenValid && !busy
                },
                shape = ctaShape,
                interactionSource = interaction,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("login_cta")
                    .graphicsLayer {
                        val p = backProgress
                        if (p > 0f) {
                            translationX = 18.dp.toPx() * p
                            scaleX = 1f - 0.03f * p
                        }
                    },
            ) {
                AnimatedVisibility(
                    visible = busy,
                    enter = fadeIn(if (reduceMotion) snap else effects) + expandHorizontally(spatialSize),
                    exit = fadeOut(if (reduceMotion) snap else effects) + shrinkHorizontally(spatialSize),
                ) {
                    AppLoadingIndicator(
                        size = 18.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        reduceMotion = reduceMotion,
                    )
                    Spacer(Modifier.width(10.dp))
                }
                AnimatedContent(
                    targetState = submitLabel(page, busy),
                    transitionSpec = {
                        ContentTransform(
                            fadeIn(if (reduceMotion) snap else effects),
                            fadeOut(if (reduceMotion) snap else effects),
                            0f,
                            SizeTransform(clip = false) { _, _ -> spatialSize },
                        )
                    },
                    label = "loginCtaLabel",
                ) { label ->
                    Text(stringResource(label))
                }
            }
        }
    }
}

private fun submitLabel(page: Int, busy: Boolean): Int = when {
    busy && page == PAGE_EMAIL -> R.string.login_cta_sending
    busy && page == PAGE_TOKEN -> R.string.login_cta_checking
    page == PAGE_TOKEN -> R.string.login_cta_signin
    page == PAGE_EMAIL -> R.string.login_cta_email
    else -> R.string.login_cta_start
}

/* ------------------------------- hero -------------------------------- */

@Composable
private fun HeroInline(
    page: Int,
    progress: Float,
    size: androidx.compose.ui.unit.Dp,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HeroMark(size = size, progress = progress, reduceMotion = reduceMotion)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    when (page) {
                        PAGE_WELCOME -> R.string.login_welcome_title
                        PAGE_EMAIL -> R.string.login_email_title
                        else -> R.string.login_token_title
                    },
                ),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    when (page) {
                        PAGE_WELCOME -> R.string.login_welcome_body
                        PAGE_EMAIL -> R.string.login_email_body
                        else -> R.string.login_token_body
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HeroPanel(
    page: Int,
    progress: Float,
    size: androidx.compose.ui.unit.Dp,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.Center) {
        HeroMark(size = size, progress = progress, reduceMotion = reduceMotion)
        Spacer(Modifier.height(26.dp))
        Text(
            text = stringResource(R.string.login_welcome_title),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.login_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Tanda aplikasi di dalam poligon Material yang bermorfosis antar langkah. */
@Composable
private fun HeroMark(
    size: androidx.compose.ui.unit.Dp,
    progress: Float,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = if (progress < 0.5f) {
        MaterialShapes.Sunny.toShape()
    } else {
        MaterialShapes.Gem.toShape()
    }
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary),
    )
}

/* ----------------------------- isi langkah ---------------------------- */

@Composable
private fun WelcomeContent(reduceMotion: Boolean) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val features = listOf(
        Triple(Icons.Filled.CloudQueue, R.string.login_feature_upload, R.string.login_feature_upload_body),
        Triple(Icons.Filled.Search, R.string.login_feature_search, R.string.login_feature_search_body),
        Triple(Icons.Filled.Lock, R.string.login_feature_private, R.string.login_feature_private_body),
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        features.forEachIndexed { index, (icon, title, body) ->
            Surface(
                shape = animatedConnectedShape(index, features.size, reduceMotion),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .staggeredEnter(index + 2, reduceMotion, entered),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            stringResource(body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmailContent(
    email: String,
    emailReady: Boolean,
    showError: Boolean,
    notice: String?,
    busy: Boolean,
    reduceMotion: Boolean,
    onEmailChange: (String) -> Unit,
    onGuest: () -> Unit,
    onPasteLink: () -> Unit,
    onSubmit: () -> Unit,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val shake = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(showError) {
        if (showError && !reduceMotion) {
            haptic.performHapticFeedback(HapticFeedbackType.Reject)
            repeat(2) {
                shake.animateTo(1f, tween(45, easing = androidx.compose.animation.core.LinearEasing))
                shake.animateTo(-1f, tween(45, easing = androidx.compose.animation.core.LinearEasing))
            }
            shake.animateTo(0f, tween(45))
        }
    }
    val spatial = motionScheme.fastSpatialSpec<Float>()
    val effects = motionScheme.fastEffectsSpec<Float>()

    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text(stringResource(R.string.login_email_label)) },
        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
        trailingIcon = {
            AnimatedVisibility(
                visible = email.isNotEmpty(),
                enter = fadeIn(effects) + scaleIn(spatial, initialScale = 0.7f),
                exit = fadeOut(effects) + scaleOut(spatial, targetScale = 0.7f),
            ) {
                IconButton(onClick = { onEmailChange("") }, enabled = !busy) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.login_clear),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
        supportingText = {
            AnimatedSupportingText(
                hint = stringResource(R.string.login_email_hint),
                issue = when {
                    showError -> stringResource(R.string.login_email_invalid)
                    notice != null -> notice
                    else -> null
                },
                reduceMotion = reduceMotion,
            )
        },
        isError = showError,
        enabled = !busy,
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            autoCorrectEnabled = false,
        ),
        keyboardActions = KeyboardActions(onNext = { onSubmit() }),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("login_email")
            .graphicsLayer { translationX = shake.value * 5.dp.toPx() }
            .staggeredEnter(2, reduceMotion, entered),
    )
    Spacer(Modifier.height(2.dp))
    TextButton(
        onClick = onPasteLink,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth().staggeredEnter(3, reduceMotion, entered),
    ) {
        Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.login_have_link))
    }
    FilledTonalButton(
        onClick = onGuest,
        enabled = !busy,
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .staggeredEnter(4, reduceMotion, entered),
    ) {
        Icon(Icons.Filled.CloudQueue, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.login_guest))
    }
}

@Composable
private fun TokenContent(
    tokenInput: String,
    tokenValid: Boolean,
    tokenVisible: Boolean,
    guidanceOpen: Boolean,
    clipboardToken: String?,
    notice: String?,
    busy: Boolean,
    reduceMotion: Boolean,
    onTokenChange: (String) -> Unit,
    onTokenVisible: () -> Unit,
    onGuidance: () -> Unit,
    onPaste: () -> Unit,
    onResend: () -> Unit,
    onSubmit: () -> Unit,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val spatial = motionScheme.fastSpatialSpec<Float>()
    val spatialSize = motionScheme.fastSpatialSpec<IntSize>()
    val effects = motionScheme.fastEffectsSpec<Float>()
    val chevron by animateFloatAsState(
        targetValue = if (guidanceOpen) 180f else 0f,
        animationSpec = if (reduceMotion) tween(0) else spatial,
        label = "guidanceChevron",
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .staggeredEnter(2, reduceMotion, entered),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.login_guidance_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onGuidance) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.login_guidance_toggle),
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = chevron },
                    )
                }
            }
            AnimatedVisibility(
                visible = guidanceOpen,
                enter = expandVertically(spatialSize) + fadeIn(effects),
                exit = shrinkVertically(spatialSize) + fadeOut(effects),
            ) {
                Column(Modifier.padding(bottom = 6.dp)) {
                    listOf(
                        R.string.login_guidance_step1,
                        R.string.login_guidance_step2,
                        R.string.login_guidance_step3,
                    ).forEachIndexed { index, text ->
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Text(
                                "${index + 1}.",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(20.dp),
                            )
                            Text(
                                stringResource(text),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = tokenInput,
        onValueChange = onTokenChange,
        label = { Text(stringResource(R.string.login_token_label)) },
        leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTokenVisible, enabled = !busy) {
                    Icon(
                        if (tokenVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = stringResource(
                            if (tokenVisible) R.string.login_hide_token else R.string.login_show_token,
                        ),
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onPaste, enabled = !busy) {
                    Icon(
                        Icons.Filled.ContentPaste,
                        contentDescription = stringResource(R.string.login_paste),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
        supportingText = {
            AnimatedSupportingText(
                hint = stringResource(R.string.login_token_hint),
                issue = when {
                    notice != null -> notice
                    tokenInput.isNotBlank() && !tokenValid -> stringResource(R.string.login_token_invalid)
                    else -> null
                },
                reduceMotion = reduceMotion,
            )
        },
        isError = tokenInput.isNotBlank() && !tokenValid,
        enabled = !busy,
        singleLine = true,
        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done,
            autoCorrectEnabled = false,
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("login_token")
            .staggeredEnter(3, reduceMotion, entered),
    )

    if (clipboardToken != null) {
        Spacer(Modifier.height(6.dp))
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(effects) + expandVertically(spatialSize),
            exit = fadeOut(effects) + shrinkVertically(spatialSize),
        ) {
            AssistChip(
                onClick = { onTokenChange(clipboardToken) },
                label = { Text(stringResource(R.string.login_paste_chip)) },
                leadingIcon = {
                    Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                },
            )
        }
    }

    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = onResend, enabled = !busy) {
            Text(stringResource(R.string.login_resend))
        }
    }
}

/* ---------------------------- progress ------------------------------- */

@Composable
private fun StepProgress(page: Int, reduceMotion: Boolean) {
    val progress = (page + 1f) / STEP_COUNT
    val label = stringResource(R.string.login_step, page + 1, STEP_COUNT)
    Column(Modifier.fillMaxWidth()) {
        LinearWavyProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .testTag("login_progress")
                .semantics {
                    contentDescription = label
                    stateDescription = label
                },
            amplitude = { if (reduceMotion) 0f else 0.55f },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/* ---------------------------- supporting ----------------------------- */

@Composable
private fun AnimatedSupportingText(
    hint: String,
    issue: String?,
    reduceMotion: Boolean,
) {
    val effects = if (reduceMotion) tween<Float>(0) else motionScheme.fastEffectsSpec<Float>()
    val sizeSpec = if (reduceMotion) tween<IntSize>(0) else motionScheme.fastSpatialSpec<IntSize>()
    var lastIssue by remember { mutableStateOf("") }
    LaunchedEffect(issue) { if (issue != null) lastIssue = issue }
    Column(Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = issue != null,
            enter = fadeIn(effects) + expandVertically(sizeSpec),
            exit = fadeOut(effects) + shrinkVertically(sizeSpec),
        ) {
            Text(issue ?: lastIssue)
        }
        AnimatedVisibility(
            visible = issue == null,
            enter = fadeIn(effects) + expandVertically(sizeSpec),
            exit = fadeOut(effects) + shrinkVertically(sizeSpec),
        ) {
            Text(hint)
        }
    }
}

/* ------------------------------ preview ------------------------------ */

@Preview(showBackground = true, name = "Welcome light")
@Composable
private fun PreviewWelcomeLight() = LoginPreview(PAGE_WELCOME, dark = false)

@Preview(showBackground = true, name = "Email dark")
@Composable
private fun PreviewEmailDark() = LoginPreview(PAGE_EMAIL, dark = true)

@Preview(showBackground = true, name = "Email error")
@Composable
private fun PreviewEmailError() = LoginPreview(PAGE_EMAIL, dark = false, email = "bukan-email")

@Preview(showBackground = true, name = "Token light")
@Composable
private fun PreviewTokenLight() = LoginPreview(PAGE_TOKEN, dark = false)

@Preview(showBackground = true, name = "Token loading")
@Composable
private fun PreviewTokenLoading() = LoginPreview(PAGE_TOKEN, dark = true, busy = true)

@Composable
private fun LoginPreview(
    page: Int,
    dark: Boolean,
    email: String = "nama@contoh.id",
    busy: Boolean = false,
) {
    GofileTheme(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
        LoginScreen(
            step = if (page == PAGE_TOKEN) "token" else "email",
            email = email,
            tokenInput = "",
            busy = busy,
            reduceMotion = false,
            onEmailChange = {},
            onTokenChange = {},
            onBackToEmail = {},
            onSend = {},
            onLogin = {},
            onGuest = {},
        )
    }
}
