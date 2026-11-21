package com.nellecrzier.gofileexpressive.ui

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/* ------------------------------- kurva ------------------------------- */

/** Kurva bawaan HyperSpring, dibangun dari bezier agar stabil di Compose. */
val HyperEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val HyperExpoOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
val HyperPower3Out = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)
val HyperPower4Out = CubicBezierEasing(0.165f, 0.84f, 0.44f, 1f)
val HyperBackOut = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/**
 * Spring HyperSpring dinyatakan sebagai response + damping, sama seperti
 * `Motion.spring()` di aplikasi aslinya: stiffness diturunkan dari response.
 */
fun <T> hyperSpring(response: Float = 0.4f, damping: Float = 0.8f) = spring<T>(
    dampingRatio = damping,
    stiffness = (2f * PI.toFloat() / response).pow(2),
)

/** Menghormati skala animator sistem Android. */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

/* ------------------------------ latar ------------------------------- */

/**
 * Latar titik putih di atas hitam, dengan dua sumber cahaya koral yang
 * bergerak pelan di jalur Lissajous. Titik di dekat jari menyala koral.
 */
@Composable
fun DotGridBackground(
    modifier: Modifier = Modifier,
    hero: Boolean = false,
    reduceMotion: Boolean = false,
    interactive: Boolean = true,
    ambient: Boolean = true,
) {
    val density = LocalDensity.current
    val cell = with(density) { 22.dp.toPx() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var touch by remember { mutableStateOf<Offset?>(null) }

    val points = remember(size, cell) {
        if (size.width <= 0) emptyList()
        else buildList {
            var y = cell
            while (y < size.height) {
                var x = cell
                while (x < size.width) {
                    add(Offset(x, y))
                    x += cell
                }
                y += cell
            }
        }
    }

    val baseAlpha = if (hero) 0.16f else 0.10f
    val glowRadius = with(density) { 120.dp.toPx() }

    val transition = if (reduceMotion) null else rememberInfiniteTransition(label = "dots")
    val t by (transition?.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(31_000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ambient",
    ) ?: remember { mutableStateOf(0.25f) })

    Canvas(
        modifier
            .onSizeChanged { size = it }
            .pointerInput(interactive) {
                if (!interactive) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        touch = when (event.type) {
                            PointerEventType.Exit -> null
                            else -> event.changes.firstOrNull()?.position
                        }
                    }
                }
            },
    ) {
        drawPoints(
            points = points,
            pointMode = PointMode.Points,
            color = Color.White.copy(alpha = baseAlpha),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round,
        )

        if (ambient) {
            val w = size.width
            val h = size.height
            val light1 = Offset(
                x = w * (0.5f + 0.38f * cos(2f * PI.toFloat() * t)),
                y = h * (0.22f + 0.12f * sin(2f * PI.toFloat() * t * 31f / 23f)),
            )
            val light2 = Offset(
                x = w * (0.5f + 0.30f * cos(2f * PI.toFloat() * t * 31f / 23f + 2.1f)),
                y = h * (0.72f + 0.14f * sin(2f * PI.toFloat() * t + 1.3f)),
            )
            val r1 = with(density) { 190.dp.toPx() }
            val r2 = with(density) { 150.dp.toPx() }
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Hyper.Accent.copy(alpha = 0.20f), Color.Transparent),
                    center = light1,
                    radius = r1,
                ),
                radius = r1,
                center = light1,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Hyper.Accent.copy(alpha = 0.13f), Color.Transparent),
                    center = light2,
                    radius = r2,
                ),
                radius = r2,
                center = light2,
            )
        }

        touch?.let { finger ->
            for (point in points) {
                val distance = (point - finger).getDistance()
                if (distance < glowRadius) {
                    val fade = 1f - distance / glowRadius
                    drawCircle(
                        color = Hyper.Accent.copy(alpha = 0.35f + 0.55f * fade),
                        radius = 1.7.dp.toPx() * (0.7f + 0.5f * fade),
                        center = point,
                    )
                }
            }
        }
    }
}

/** Scrim atas untuk layar setup; konten memudar ke hitam di balik pil. */
@Composable
fun TopScrim(modifier: Modifier = Modifier, height: Dp = 96.dp) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    listOf(Hyper.Bg.copy(alpha = 0.94f), Hyper.Bg.copy(alpha = 0.69f), Color.Transparent),
                ),
            ),
    )
}

/** Selubung bawah untuk layar setup: tombol duduk di atas gradasi. */
@Composable
fun BottomVeil(modifier: Modifier = Modifier, height: Dp = 168.dp) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Hyper.Bg.copy(alpha = 0.88f), Hyper.Bg.copy(alpha = 0.96f)),
                ),
            ),
    )
}

/* ------------------------------ permukaan --------------------------- */

/**
 * Kartu HyperSpring: hitam yang nyaris sama dengan latar, dibedakan oleh
 * garis rambut 0,8dp. Tidak ada bayangan.
 */
@Composable
fun HyperCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    border: Color = Hyper.Hairline,
    fill: Color = Hyper.Card,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = fill,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, border),
    ) {
        Column(Modifier.padding(vertical = 2.dp), content = content)
    }
}

/** Kelompok baris tanpa kartu pembungkus, untuk daftar panjang. */
@Composable
fun HyperGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    HyperCard(modifier = modifier.fillMaxWidth(), content = content)
}

/** Judul bagian: tanda pisah koral, huruf kapital, jarak huruf lebar. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(14.dp)
                .height(2.dp)
                .background(Hyper.Accent),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.6.sp,
            color = Hyper.Text2,
        )
    }
}

/** Chip ikon kecil di dalam baris: 32dp, sudut 9dp, garis rambut. */
@Composable
fun IconChip(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    tint: Color = Hyper.Text,
    fill: Color = Hyper.Chip,
    edge: Color = Hyper.Hairline,
    iconSize: Dp = 18.dp,
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = fill,
        border = androidx.compose.foundation.BorderStroke(0.7.dp, edge),
        modifier = modifier.size(size),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
        }
    }
}

/** Baris pengaturan: judul 15,5sp, subjudul 12,5sp, nilai di kanan. */
@Composable
fun HyperRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = Hyper.Text,
    iconFill: Color = Hyper.Chip,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = ripple(color = Hyper.Pressed),
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IconChip(icon, tint = iconTint, fill = iconFill)
            Spacer(Modifier.width(13.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Hyper.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Hyper.Text2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        if (value != null) {
            Spacer(Modifier.width(12.dp))
            ValuePill(value)
        }
        trailing?.let {
            Spacer(Modifier.width(10.dp))
            it()
        }
    }
}

/** Pil nilai: latar koral 12%, teks koral, sudut 8dp. */
@Composable
fun ValuePill(text: String, modifier: Modifier = Modifier, accent: Boolean = true) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (accent) Hyper.AccentSoft else Hyper.CardRaised,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (accent) Hyper.Accent else Hyper.Text,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
        )
    }
}

/** Pil status dengan titik berdenyut. */
@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color = Hyper.Ok,
    pulse: Boolean = true,
    reduceMotion: Boolean = false,
) {
    Surface(
        shape = CircleShape,
        color = Hyper.Card,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Hyper.Hairline),
        modifier = modifier,
    ) {
        Row(
            Modifier.padding(start = 8.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PulseDot(color = dotColor, pulse = pulse && !reduceMotion)
            Spacer(Modifier.width(7.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = Hyper.Text2,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun PulseDot(color: Color, modifier: Modifier = Modifier, pulse: Boolean = true) {
    val transition = if (pulse) rememberInfiniteTransition(label = "pulse") else null
    val ring by (transition?.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = HyperPower3Out),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ring",
    ) ?: remember { mutableStateOf(1f) })

    Canvas(modifier.size(18.dp)) {
        val r = size.minDimension * 0.22f
        if (pulse) {
            drawCircle(
                color = color.copy(alpha = 0.5f * (1f - ring)),
                radius = r * (1f + 1.4f * ring),
                center = center,
            )
        }
        drawCircle(color = color, radius = r, center = center)
    }
}

/* ------------------------------- tombol ----------------------------- */

/**
 * Tombol pil HyperSpring. Primer mengisi koral dengan teks hitam, sekunder
 * memakai kartu dengan garis rambut. `squash` menambahkan tekan karet:
 * melebar 5% dan memendek 12%, lalu memantul dengan kosinus teredam.
 */
@Composable
fun HyperButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = true,
    enabled: Boolean = true,
    loading: Boolean = false,
    squash: Boolean = false,
    reduceMotion: Boolean = false,
    height: Dp = 56.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = rememberRubberScale(pressed, squash && !reduceMotion)
    val shine = remember { Animatable(0f) }

    LaunchedEffect(pressed) {
        if (pressed) {
            shine.animateTo(1f, tween(300, easing = HyperPower3Out))
            shine.animateTo(0f, tween(220))
        }
    }

    val fill = when {
        primary -> Hyper.Accent
        else -> Hyper.Card
    }
    val contentColor = when {
        !enabled -> Hyper.Text3
        primary -> Hyper.AccentInk
        else -> Hyper.Text
    }
    val edge = when {
        primary -> Color.Transparent
        else -> Hyper.HairlineStrong
    }

    Box(
        Modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale.first
                scaleY = scale.second
            },
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled && !loading,
            interactionSource = interaction,
            shape = CircleShape,
            color = if (enabled) fill else Hyper.Card,
            border = androidx.compose.foundation.BorderStroke(0.8.dp, edge),
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (loading) {
                    ExpressiveLoader(
                        size = 18.dp,
                        color = contentColor,
                        reduceMotion = reduceMotion,
                    )
                    Spacer(Modifier.width(10.dp))
                } else if (icon != null) {
                    Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(9.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                )
            }
        }
        ShineStripe(shine.value, Modifier.fillMaxSize())
    }
}

/** Kilau sendok putih yang meluncur ke kanan saat ditekan. */
@Composable
private fun ShineStripe(alpha: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (alpha > 0.02f) {
            val w = size.width
            val h = size.height
            val seg = 0.36f * (w + h)
            val cx = -seg + (w + 2f * seg) * alpha
            val op = sin(alpha.toDouble() * PI).toFloat().coerceIn(0f, 1f)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.55f * op),
                topLeft = Offset(cx - seg, 0f),
                size = Size(seg, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f),
                blendMode = BlendMode.Plus,
            )
        }
    }
}

/** Tombol ikon 44dp berbentuk lingkaran, sama seperti tombol kembali di HyperSpring. */
@Composable
fun HyperIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = CircleShape,
        color = Hyper.Card,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Hyper.Hairline),
        modifier = modifier.size(44.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = description,
                tint = if (enabled) Hyper.Text else Hyper.Text3,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Tekan karet: 1,05 × 0,88 saat ditekan, lalu kosinus teredam 700ms. */
@Composable
fun rememberRubberScale(pressed: Boolean, enabled: Boolean): Pair<Float, Float> {
    var sx by remember { mutableStateOf(1f) }
    var sy by remember { mutableStateOf(1f) }

    LaunchedEffect(pressed, enabled) {
        if (!enabled) {
            sx = 1f
            sy = 1f
            return@LaunchedEffect
        }
        if (pressed) {
            sx = 1.05f
            sy = 0.88f
            return@LaunchedEffect
        }
        var elapsed = 0f
        var last = 0L
        while (elapsed < 700f) {
            val (stamp, delta) = frameDeltaMillis(last)
            last = stamp
            elapsed += delta
            val e = exp(-elapsed / 150f)
            val c = cos(2f * PI.toFloat() * elapsed / 280f)
            sx = 1f + 0.05f * e * c
            sy = 1f - 0.12f * e * c
        }
        sx = 1f
        sy = 1f
    }
    return sx to sy
}

private suspend fun frameDeltaMillis(last: Long): Pair<Long, Float> {
    val now = withFrameNanos { it }
    val base = if (last == 0L) now else last
    val delta = ((now - base) / 1_000_000f).coerceIn(1f, 34f)
    return now to delta
}

/* ------------------------------ tipografi --------------------------- */

/**
 * Judul dengan huruf yang muncul satu per satu, 26ms jarak, 820ms per
 * huruf dengan kurva expo-out — detail tanda tangan HyperSpring.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SplitTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayLarge,
    color: Color = Hyper.Text,
    reduceMotion: Boolean = false,
) {
    val progress = remember(text) { Animatable(0f) }
    LaunchedEffect(text, reduceMotion) {
        if (reduceMotion) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(820, easing = HyperExpoOut))
        }
    }

    val words = text.split(" ")
    var index = 0
    FlowRow(modifier, horizontalArrangement = Arrangement.Start) {
        words.forEach { word ->
            word.forEach { char ->
                val order = index++
                val raw = ((progress.value - order * 0.026f) / 0.82f).coerceIn(0f, 1f)
                val p = 1f - (1f - raw).pow(3)
                Text(
                    text = char.toString(),
                    style = style,
                    color = color,
                    modifier = Modifier.graphicsLayer {
                        alpha = p
                        translationY = 16f * (1f - p)
                    },
                )
            }
            if (index < text.length) {
                Text(" ", style = style, color = Color.Transparent)
            }
        }
    }
}

/* -------------------------------- toggle ---------------------------- */

/** Toggle HyperSpring: rel 50×30, kenop 25dp yang memipih saat ditekan. */
@Composable
fun HyperToggle(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val position by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = hyperSpring(0.42f, 0.72f),
        label = "togglePosition",
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = hyperSpring(if (pressed) 0.3f else 0.4f, if (pressed) 0.62f else 0.8f),
        label = "togglePress",
    )

    Canvas(
        modifier
            .size(62.dp, 40.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onClick = { onChange(!checked) },
            ),
    ) {
        val trackLeft = (size.width - 50.dp.toPx()) / 2f
        val trackTop = (size.height - 30.dp.toPx()) / 2f
        val trackW = 50.dp.toPx()
        val trackH = 30.dp.toPx()
        val trackColor = lerp(Hyper.Track, Hyper.Accent, position)
            .let { if (enabled) it else it.copy(alpha = 0.4f) }

        drawRoundRect(
            color = trackColor,
            topLeft = Offset(trackLeft, trackTop),
            size = Size(trackW, trackH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackH / 2f),
        )

        val travel = trackW - trackH
        val knobRadius = trackH / 2f - 2.5.dp.toPx()
        val cx = trackLeft + trackH / 2f + travel * position
        val stretch = 1f + 0.55f * press
        val knobColor = if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
        drawCircle(
            color = Color.Black.copy(alpha = 0.25f),
            radius = knobRadius + 1.dp.toPx(),
            center = Offset(cx, trackTop + trackH / 2f + 1.dp.toPx()),
        )
        drawOval(
            color = knobColor,
            topLeft = Offset(cx - knobRadius * stretch, trackTop + trackH / 2f - knobRadius / stretch),
            size = Size(knobRadius * 2f * stretch, knobRadius * 2f / stretch),
        )
    }
}

/* -------------------------------- pager ----------------------------- */

/**
 * Indikator halaman berbentuk titik yang melebur: kepala memakai spring
 * cepat, ekor spring lambat, jadi pilnya sempat meregang seperti tetesan.
 */
@Composable
fun DropletPager(count: Int, index: Int, modifier: Modifier = Modifier) {
    val x by animateDpAsState(
        targetValue = (index * 18 - 3).dp,
        animationSpec = hyperSpring(0.62f, 0.86f),
        label = "pagerTail",
    )
    val width by animateDpAsState(
        targetValue = 14.dp,
        animationSpec = hyperSpring(0.32f, 0.78f),
        label = "pagerHead",
    )
    Box(modifier.width((count * 18).dp).height(10.dp)) {
        repeat(count) { i ->
            Box(
                Modifier
                    .offset(x = (i * 18 + 3).dp, y = 2.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
            )
        }
        Box(
            Modifier
                .offset(x = x, y = 2.dp)
                .width(width)
                .height(6.dp)
                .clip(CircleShape)
                .background(Hyper.Accent),
        )
    }
}

/* ---------------------------- segmented ----------------------------- */

/** Kontrol segmen: rel gelap, pilihan mengisi koral dengan teks hitam. */
@Composable
fun HyperSegmented(
    options: List<Pair<String, String>>,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Hyper.Track,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { (key, label) ->
                val selected = value == key
                val container by animateColorAsState(
                    targetValue = if (selected) Hyper.Accent else Color.Transparent,
                    animationSpec = tween(220, easing = HyperEase),
                    label = "segmentFill",
                )
                val content by animateColorAsState(
                    targetValue = if (selected) Hyper.AccentInk else Hyper.Text2,
                    animationSpec = tween(180, easing = HyperEase),
                    label = "segmentText",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(container)
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onChange(key) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = content,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/* ------------------------------- input ------------------------------ */

/** Bidang teks HyperSpring: sudut 18dp, garis rambut, fokus koral. */
@Composable
fun HyperTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supporting: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = Hyper.Text3) } },
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, tint = Hyper.Text2) } },
        trailingIcon = trailing,
        supportingText = supporting?.let {
            {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) Hyper.Accent else Hyper.Text3,
                )
            }
        },
        isError = isError,
        enabled = enabled,
        singleLine = singleLine,
        shape = RoundedCornerShape(18.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Hyper.Text,
            unfocusedTextColor = Hyper.Text,
            disabledTextColor = Hyper.Text3,
            focusedBorderColor = Hyper.Accent,
            unfocusedBorderColor = Hyper.HairlineStrong,
            disabledBorderColor = Hyper.Hairline,
            errorBorderColor = Hyper.Accent,
            focusedLabelColor = Hyper.Accent,
            unfocusedLabelColor = Hyper.Text2,
            cursorColor = Hyper.Accent,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/* ------------------------------ pemberitahuan ----------------------- */

/** Pemberitahuan bergaris koral; HyperSpring tidak memakai kartu merah. */
@Composable
fun NoticeBanner(
    text: String,
    modifier: Modifier = Modifier,
    error: Boolean = true,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Hyper.Card,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Hyper.AccentEdge),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon ?: if (error) {
                    Icons.Filled.ErrorOutline
                } else {
                    Icons.Filled.CheckCircle
                },
                contentDescription = null,
                tint = Hyper.Accent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Hyper.Text,
                modifier = Modifier.weight(1f),
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = Hyper.Accent,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onAction)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            if (onDismiss != null) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Tutup",
                    tint = Hyper.Text2,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                        .padding(6.dp),
                )
            }
        }
    }
}

/* --------------------------- hold to confirm ------------------------ */

/**
 * Konfirmasi tahan: isi koral mengalir dari kiri ke kanan selama 900ms.
 * Ketukan pendek ditolak dengan getar dan goyangan kecil.
 */
@Composable
fun HoldToConfirm(
    label: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }

    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(CircleShape)
                .background(Hyper.AccentSoft)
                .border(0.8.dp, Hyper.AccentEdge, CircleShape)
                .semantics {
                    contentDescription = label
                    onClick("Aktifkan $label") {
                        onConfirm()
                        true
                    }
                }
                .graphicsLayer { translationX = sin(shake.value * PI.toFloat() * 6f) * 5f * (1f - shake.value) }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onPress = {
                            val job = scope.launch {
                                progress.animateTo(
                                    1f,
                                    tween(900, easing = androidx.compose.animation.core.LinearEasing),
                                )
                            }
                            val released = tryAwaitRelease()
                            job.cancel()
                            if (released && progress.value >= 0.98f) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onConfirm()
                            } else {
                                progress.animateTo(0f, tween(280, easing = HyperPower3Out))
                                if (!released) {
                                    shake.snapTo(0f)
                                    shake.animateTo(1f, tween(320, easing = HyperPower3Out))
                                }
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    color = Hyper.Accent.copy(alpha = 0.30f),
                    size = Size(size.width * progress.value, size.height),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Hyper.Accent,
            )
        }
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = Hyper.Text3,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            )
        }
    }
}

/* ------------------------------ halaman ----------------------------- */

/**
 * Kerangka halaman: strip judul mini 52dp yang menempel di atas, judul
 * besar 40sp yang memudar dan bergerak naik saat digulir, lalu konten.
 */
@Composable
fun HyperPage(
    title: String,
    subtitle: String?,
    scroll: ScrollState,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val collapse = with(density) { 130.dp.toPx() }
    val y = scroll.value.coerceAtLeast(0)
    val f = (y / collapse).coerceIn(0f, 1f)
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier.fillMaxSize().background(Hyper.Bg)) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Spacer(Modifier.height(topInset + 10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
            Spacer(Modifier.height(4.dp))
            Column(
                Modifier
                    .graphicsLayer {
                        alpha = 1f - f
                        translationY = y * 0.45f
                        rotationX = 18f * (1f - f)
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    .padding(horizontal = 16.dp),
            ) {
                SplitTitle(title, reduceMotion = reduceMotion)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Hyper.Text2,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = bottomInset + 108.dp),
                content = content,
            )
        }
    }
}

/* -------------------------------- nav ------------------------------- */

data class HyperTab(val key: String, val label: String, val icon: ImageVector)

/**
 * Bar navigasi kaca: satu kapsul melayang, pil putih 16% untuk tab aktif,
 * label tab terbuka dengan spring, ikon dan label aktif berwarna koral.
 */
@Composable
fun HyperNavBar(
    tabs: List<HyperTab>,
    current: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
    backdrop: GraphicsLayer? = null,
    onBoundsChange: ((Offset, IntSize) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
    val widths = tabs.map { tab ->
        with(density) { measurer.measure(tab.label, labelStyle).size.width.toDp() + 6.dp }
    }

    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            Modifier
                .onGloballyPositioned { coordinates ->
                    onBoundsChange?.invoke(coordinates.positionInRoot(), coordinates.size)
                }
                .clip(CircleShape)
                // Snapshot konten di belakang kapsul, sudah diburamkan pemanggil.
                .drawBehind {
                    val layer = backdrop
                    if (layer != null) drawLayer(layer)
                }
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.10f),
                            Hyper.GlassFill,
                            Hyper.GlassFill,
                        ),
                    ),
                )
                // Tint hitam supaya kapsul terbaca sebagai kaca di atas konten.
                .drawBehind {
                    drawRect(Color.Black.copy(alpha = if (backdrop != null) 0.18f else 0.42f))
                }
                .border(1.dp, Hyper.GlassEdge, CircleShape)
                .drawBehind {
                    drawLine(
                        color = Color.White.copy(alpha = 0.28f),
                        start = Offset(size.width * 0.18f, 1f),
                        end = Offset(size.width * 0.82f, 1f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = current == tab.key
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val openWidth by animateDpAsState(
                    targetValue = if (selected) widths[index] else 0.dp,
                    animationSpec = hyperSpring(0.4f, 0.85f),
                    label = "navLabel",
                )
                val pressScale by animateFloatAsState(
                    targetValue = if (pressed && !reduceMotion) 0.94f else 1f,
                    animationSpec = hyperSpring(0.3f, 0.55f),
                    label = "navPress",
                )
                val fill by animateColorAsState(
                    targetValue = if (selected) Hyper.GlassHighlight else Color.Transparent,
                    animationSpec = tween(220, easing = HyperEase),
                    label = "navFill",
                )
                val content by animateColorAsState(
                    targetValue = if (selected) Hyper.Accent else Hyper.Text2,
                    animationSpec = tween(200, easing = HyperEase),
                    label = "navContent",
                )

                Box(
                    Modifier
                        .graphicsLayer {
                            scaleX = pressScale
                            scaleY = pressScale
                        }
                        .height(58.dp)
                        .defaultMinSize(minWidth = 58.dp)
                        .clip(CircleShape)
                        .background(fill)
                        .selectable(
                            selected = selected,
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(tab.key) },
                        )
                        .padding(horizontal = 17.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = content,
                            modifier = Modifier.size(24.dp),
                        )
                        Box(Modifier.width(openWidth).clipToBounds()) {
                            Text(
                                text = tab.label,
                                style = labelStyle,
                                color = content,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------ daftar kosong ----------------------- */

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(28.dp).widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Hyper.Card,
                border = androidx.compose.foundation.BorderStroke(0.8.dp, Hyper.Hairline),
                modifier = Modifier.size(76.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Hyper.Accent, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = Hyper.Text)
            Spacer(Modifier.height(6.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = Hyper.Text2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(20.dp))
                HyperButton(actionLabel, onAction, height = 46.dp)
            }
        }
    }
}

/** Lebar baca maksimum supaya tablet tidak melebar. */
val HyperMaxWidth: Dp = 720.dp

/** Tinggi bar navigasi beserta jaraknya dari tepi bawah layar. */
val HyperNavHeight: Dp = 74.dp

/** Sudut kartu standar HyperSpring. */
val CardShape = RoundedCornerShape(18.dp)

/** Sudut lembar bawah. */
val SheetShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
