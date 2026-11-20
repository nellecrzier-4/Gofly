package com.nellecrzier.gofileexpressive.ui

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF242424),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDEDEDE),
    onPrimaryContainer = Color(0xFF242424),
    secondary = Color(0xFF6B6B6B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6E6E6),
    onSecondaryContainer = Color(0xFF191919),
    tertiary = Color(0xFF007AFF),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDEDEDE),
    onTertiaryContainer = Color(0xFF191919),
    background = Color(0xFFF1F1F1),
    onBackground = Color(0xFF191919),
    surface = Color(0xFFF1F1F1),
    onSurface = Color(0xFF191919),
    surfaceVariant = Color(0xFFE6E6E6),
    onSurfaceVariant = Color(0xFF6B6B6B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFE6E6E6),
    surfaceContainerHighest = Color(0xFFDEDEDE),
    error = Color(0xFFD70015),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE5E5),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFFC8C8C8),
    outlineVariant = Color(0x1F000000),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE2E2E2),
    onPrimary = Color(0xFF151515),
    primaryContainer = Color(0xFF363636),
    onPrimaryContainer = Color(0xFFE2E2E2),
    secondary = Color(0xFFA3A3A3),
    onSecondary = Color(0xFF151515),
    secondaryContainer = Color(0xFF333333),
    onSecondaryContainer = Color(0xFFE6E6E6),
    tertiary = Color(0xFF007AFF),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDEDEDE),
    onTertiaryContainer = Color(0xFF242424),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE6E6E6),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = Color(0xFF333333),
    onSurfaceVariant = Color(0xFFA3A3A3),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF111111),
    surfaceContainer = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF333333),
    surfaceContainerHighest = Color(0xFF3A3A3A),
    error = Color(0xFFFF453A),
    onError = Color(0xFF1A0000),
    errorContainer = Color(0xFF4A0B08),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0x33FFFFFF),
    outlineVariant = Color(0x33FFFFFF),
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(30.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(38.dp),
)

@Composable
fun GofileTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 46.sp,
        lineHeight = 60.sp,
        letterSpacing = (-1).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.5).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.2.sp,
    ),
)
