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
private fun animatedColor(target: Color, label: String): Color {
    val spec = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val value by animateColorAsState(targetValue = target, animationSpec = spec, label = label)
    return value
}

/** Skema warna yang seluruh perannya dianimasikan saat tema berganti. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun animateScheme(target: ColorScheme): ColorScheme = target.copy(
    primary = animatedColor(target.primary, "primary"),
    onPrimary = animatedColor(target.onPrimary, "onPrimary"),
    primaryContainer = animatedColor(target.primaryContainer, "primaryContainer"),
    onPrimaryContainer = animatedColor(target.onPrimaryContainer, "onPrimaryContainer"),
    inversePrimary = animatedColor(target.inversePrimary, "inversePrimary"),
    secondary = animatedColor(target.secondary, "secondary"),
    onSecondary = animatedColor(target.onSecondary, "onSecondary"),
    secondaryContainer = animatedColor(target.secondaryContainer, "secondaryContainer"),
    onSecondaryContainer = animatedColor(target.onSecondaryContainer, "onSecondaryContainer"),
    tertiary = animatedColor(target.tertiary, "tertiary"),
    onTertiary = animatedColor(target.onTertiary, "onTertiary"),
    tertiaryContainer = animatedColor(target.tertiaryContainer, "tertiaryContainer"),
    onTertiaryContainer = animatedColor(target.onTertiaryContainer, "onTertiaryContainer"),
    background = animatedColor(target.background, "background"),
    onBackground = animatedColor(target.onBackground, "onBackground"),
    surface = animatedColor(target.surface, "surface"),
    onSurface = animatedColor(target.onSurface, "onSurface"),
    surfaceVariant = animatedColor(target.surfaceVariant, "surfaceVariant"),
    onSurfaceVariant = animatedColor(target.onSurfaceVariant, "onSurfaceVariant"),
    surfaceContainerLowest = animatedColor(target.surfaceContainerLowest, "surfaceContainerLowest"),
    surfaceContainerLow = animatedColor(target.surfaceContainerLow, "surfaceContainerLow"),
    surfaceContainer = animatedColor(target.surfaceContainer, "surfaceContainer"),
    surfaceContainerHigh = animatedColor(target.surfaceContainerHigh, "surfaceContainerHigh"),
    surfaceContainerHighest = animatedColor(target.surfaceContainerHighest, "surfaceContainerHighest"),
    inverseSurface = animatedColor(target.inverseSurface, "inverseSurface"),
    inverseOnSurface = animatedColor(target.inverseOnSurface, "inverseOnSurface"),
    error = animatedColor(target.error, "error"),
    onError = animatedColor(target.onError, "onError"),
    errorContainer = animatedColor(target.errorContainer, "errorContainer"),
    onErrorContainer = animatedColor(target.onErrorContainer, "onErrorContainer"),
    outline = animatedColor(target.outline, "outline"),
    outlineVariant = animatedColor(target.outlineVariant, "outlineVariant"),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GofileTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentChoice = AccentChoice.CORAL,
    dynamic: Boolean = true,
    fontScale: Float = 1f,
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
    val animated = animateScheme(scheme)
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
