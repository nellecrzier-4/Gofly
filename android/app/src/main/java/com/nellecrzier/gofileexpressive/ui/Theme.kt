package com.nellecrzier.gofileexpressive.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nellecrzier.gofileexpressive.AccentChoice
import com.nellecrzier.gofileexpressive.ThemeMode

/**
 * Tema aplikasi berbasis Material 3 Expressive.
 *
 * Warna mengikuti skema dinamis Material You di Android 12+, atau skema tonal
 * yang diturunkan dari aksen pilihan pengguna. Perubahan tema dianimasikan
 * dengan spesifikasi effects dari [MotionScheme] Expressive; tipografi memakai
 * skala tipe M3 (dengan pengali ukuran teks dari Setelan).
 */

private fun accentColor(choice: AccentChoice): Color = when (choice) {
    AccentChoice.CORAL -> Color(0xFFFF5F45)
    AccentChoice.AMBER -> Color(0xFFFFB547)
    AccentChoice.MINT -> Color(0xFF6FD99A)
    AccentChoice.VIOLET -> Color(0xFFA78BFA)
    AccentChoice.BLUE -> Color(0xFF5B9DFF)
}

/** Aksen yang lebih dalam supaya kontras AA tetap lolos di atas permukaan terang. */
private fun accentOnLight(choice: AccentChoice): Color = when (choice) {
    AccentChoice.CORAL -> Color(0xFFD93B22)
    AccentChoice.AMBER -> Color(0xFF8A5A00)
    AccentChoice.MINT -> Color(0xFF1C7A4A)
    AccentChoice.VIOLET -> Color(0xFF5B3EC8)
    AccentChoice.BLUE -> Color(0xFF1F5FC4)
}

/** Warna teks di atas aksen: hitam di aksen terang, putih di aksen gelap. */
private fun onAccent(color: Color): Color {
    val luminance = 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
    return if (luminance > 0.45f) Color(0xFF0A0A0A) else Color.White
}

/**
 * Skema tonal cadangan saat warna dinamis tidak tersedia: aksen pengguna
 * menjadi benih untuk primary/secondary/tertiary beserta kontainernya, di atas
 * peran permukaan tonal M3.
 */
private fun schemeFor(dark: Boolean, choice: AccentChoice): ColorScheme {
    val accent = if (dark) accentColor(choice) else accentOnLight(choice)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent(accent),
        primaryContainer = if (dark) accent.copy(alpha = 0.26f) else accent.copy(alpha = 0.18f),
        onPrimaryContainer = if (dark) accent.copy(alpha = 0.92f) else accent.copy(alpha = 0.95f),
        inversePrimary = accent,
        secondary = if (dark) Color(0xFFCAC4D0) else Color(0xFF625B71),
        secondaryContainer = if (dark) Color(0xFF4A4458) else Color(0xFFE8DEF8),
        onSecondaryContainer = if (dark) Color(0xFFE8DEF8) else Color(0xFF1D192B),
        tertiary = if (dark) Color(0xFFEFB8C8) else Color(0xFF7D5260),
        tertiaryContainer = if (dark) Color(0xFF633B48) else Color(0xFFFFD8E4),
        onTertiaryContainer = if (dark) Color(0xFFFFD8E4) else Color(0xFF31111D),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun animatedColor(target: Color, label: String, reduceMotion: Boolean): Color {
    val spec = if (reduceMotion) androidx.compose.animation.core.tween(0) else MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val value by animateColorAsState(targetValue = target, animationSpec = spec, label = label)
    return value
}

/** Skema warna yang seluruh perannya dianimasikan saat tema berganti. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun animateScheme(target: ColorScheme, reduceMotion: Boolean): ColorScheme = target.copy(
    primary = animatedColor(target.primary, "primary", reduceMotion),
    onPrimary = animatedColor(target.onPrimary, "onPrimary", reduceMotion),
    primaryContainer = animatedColor(target.primaryContainer, "primaryContainer", reduceMotion),
    onPrimaryContainer = animatedColor(target.onPrimaryContainer, "onPrimaryContainer", reduceMotion),
    inversePrimary = animatedColor(target.inversePrimary, "inversePrimary", reduceMotion),
    secondary = animatedColor(target.secondary, "secondary", reduceMotion),
    onSecondary = animatedColor(target.onSecondary, "onSecondary", reduceMotion),
    secondaryContainer = animatedColor(target.secondaryContainer, "secondaryContainer", reduceMotion),
    onSecondaryContainer = animatedColor(target.onSecondaryContainer, "onSecondaryContainer", reduceMotion),
    tertiary = animatedColor(target.tertiary, "tertiary", reduceMotion),
    onTertiary = animatedColor(target.onTertiary, "onTertiary", reduceMotion),
    tertiaryContainer = animatedColor(target.tertiaryContainer, "tertiaryContainer", reduceMotion),
    onTertiaryContainer = animatedColor(target.onTertiaryContainer, "onTertiaryContainer", reduceMotion),
    background = animatedColor(target.background, "background", reduceMotion),
    onBackground = animatedColor(target.onBackground, "onBackground", reduceMotion),
    surface = animatedColor(target.surface, "surface", reduceMotion),
    onSurface = animatedColor(target.onSurface, "onSurface", reduceMotion),
    surfaceVariant = animatedColor(target.surfaceVariant, "surfaceVariant", reduceMotion),
    onSurfaceVariant = animatedColor(target.onSurfaceVariant, "onSurfaceVariant", reduceMotion),
    surfaceContainerLowest = animatedColor(target.surfaceContainerLowest, "surfaceContainerLowest", reduceMotion),
    surfaceContainerLow = animatedColor(target.surfaceContainerLow, "surfaceContainerLow", reduceMotion),
    surfaceContainer = animatedColor(target.surfaceContainer, "surfaceContainer", reduceMotion),
    surfaceContainerHigh = animatedColor(target.surfaceContainerHigh, "surfaceContainerHigh", reduceMotion),
    surfaceContainerHighest = animatedColor(target.surfaceContainerHighest, "surfaceContainerHighest", reduceMotion),
    inverseSurface = animatedColor(target.inverseSurface, "inverseSurface", reduceMotion),
    inverseOnSurface = animatedColor(target.inverseOnSurface, "inverseOnSurface", reduceMotion),
    error = animatedColor(target.error, "error", reduceMotion),
    onError = animatedColor(target.onError, "onError", reduceMotion),
    errorContainer = animatedColor(target.errorContainer, "errorContainer", reduceMotion),
    onErrorContainer = animatedColor(target.onErrorContainer, "onErrorContainer", reduceMotion),
    outline = animatedColor(target.outline, "outline", reduceMotion),
    outlineVariant = animatedColor(target.outlineVariant, "outlineVariant", reduceMotion),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GofileTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentChoice = AccentChoice.CORAL,
    dynamic: Boolean = true,
    amoled: Boolean = false,
    fontScale: Float = 1f,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }
    val dynamicAvailable = dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scheme = when {
        dynamicAvailable -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> schemeFor(dark, accent)
    }
    val base = if (amoled && dark) {
        scheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF070709),
            surfaceContainer = Color(0xFF0C0C0E),
            surfaceContainerHigh = Color(0xFF141416),
            surfaceContainerHighest = Color(0xFF1C1C1F),
        )
    } else {
        scheme
    }
    val animated = animateScheme(base, reduceMotion)
    val typography = if (fontScale == 1f) Typography() else Typography().scaled(fontScale)
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

    MaterialExpressiveTheme(
        colorScheme = animated,
        motionScheme = MotionScheme.expressive(),
        typography = typography,
        shapes = shapes,
        content = content,
    )
}

private fun Typography.scaled(factor: Float): Typography = copy(
    displayLarge = displayLarge.scaled(factor),
    displayMedium = displayMedium.scaled(factor),
    displaySmall = displaySmall.scaled(factor),
    headlineLarge = headlineLarge.scaled(factor),
    headlineMedium = headlineMedium.scaled(factor),
    headlineSmall = headlineSmall.scaled(factor),
    titleLarge = titleLarge.scaled(factor),
    titleMedium = titleMedium.scaled(factor),
    titleSmall = titleSmall.scaled(factor),
    bodyLarge = bodyLarge.scaled(factor),
    bodyMedium = bodyMedium.scaled(factor),
    bodySmall = bodySmall.scaled(factor),
    labelLarge = labelLarge.scaled(factor),
    labelMedium = labelMedium.scaled(factor),
    labelSmall = labelSmall.scaled(factor),
)

private fun TextStyle.scaled(factor: Float): TextStyle = copy(
    fontSize = (fontSize.value * factor).sp,
    lineHeight = (lineHeight.value * factor).sp,
)
