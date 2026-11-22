package com.nellecrzier.gofileexpressive.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nellecrzier.gofileexpressive.AccentChoice
import com.nellecrzier.gofileexpressive.ThemeMode

/**
 * Bahasa visual HyperSpring: latar nyaris hitam (atau kertas hangat di mode
 * terang), satu aksen yang dipilih pengguna, kartu yang dibedakan oleh garis
 * rambut (bukan bayangan), dan tipografi tebal berjarak rapat.
 *
 * Token dibaca lewat [Hyper] supaya seluruh layar tidak perlu diubah saat
 * palet berganti: [GofileTheme] mengganti set token aktif lalu memaksa subtree
 * digambar ulang dengan `key(...)`. Ini sengaja sederhana — satu sumber token,
 * tanpa dua sistem desain.
 */
data class HyperTokens(
    val dark: Boolean,
    val bg: Color,
    val card: Color,
    val cardRaised: Color,
    val hairline: Color,
    val hairlineStrong: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val accent: Color,
    val accentInk: Color,
    val accentSoft: Color,
    val accentEdge: Color,
    val track: Color,
    val pressed: Color,
    val chip: Color,
    val ok: Color,
    val warn: Color,
    val glassFill: Color,
    val glassEdge: Color,
    val glassHighlight: Color,
    val sheetScrim: Color,
) {
    /** Warna teks di atas aksen: hitam di aksen terang, putih di aksen gelap. */
    val onAccent: Color get() = if (accent.luminance() > 0.45f) Color(0xFF0A0A0A) else Color.White

    private fun Color.luminance(): Float = (0.2126f * red + 0.7152f * green + 0.0722f * blue)
}

/** Warna aksen yang tersedia di Setelan. */
private fun accentColor(choice: AccentChoice): Color = when (choice) {
    AccentChoice.CORAL -> Color(0xFFFF5F45)
    AccentChoice.AMBER -> Color(0xFFFFB547)
    AccentChoice.MINT -> Color(0xFF6FD99A)
    AccentChoice.VIOLET -> Color(0xFFA78BFA)
    AccentChoice.BLUE -> Color(0xFF5B9DFF)
}

/** Aksen yang lebih dalam supaya kontras AA tetap lolos di atas kertas terang. */
private fun accentOnLight(choice: AccentChoice): Color = when (choice) {
    AccentChoice.CORAL -> Color(0xFFD93B22)
    AccentChoice.AMBER -> Color(0xFF8A5A00)
    AccentChoice.MINT -> Color(0xFF1C7A4A)
    AccentChoice.VIOLET -> Color(0xFF5B3EC8)
    AccentChoice.BLUE -> Color(0xFF1F5FC4)
}

private fun darkTokens(choice: AccentChoice): HyperTokens {
    val accent = accentColor(choice)
    return HyperTokens(
        dark = true,
        bg = Color(0xFF000000),
        card = Color(0xFF0E0E10),
        cardRaised = Color(0xFF17171A),
        hairline = Color(0x17FFFFFF),
        hairlineStrong = Color(0x2EFFFFFF),
        text = Color(0xFFF5F5F7),
        text2 = Color(0xFF8E8E96),
        text3 = Color(0xFF5C5C66),
        accent = accent,
        accentInk = Color(0xFF000000),
        accentSoft = accent.copy(alpha = 0.12f),
        accentEdge = accent.copy(alpha = 0.40f),
        track = Color(0xFF26262B),
        pressed = Color(0x1FFFFFFF),
        chip = Color(0x12FFFFFF),
        ok = Color(0xFF6FD99A),
        warn = Color(0xFFFFB547),
        glassFill = Color(0x66121212),
        glassEdge = Color(0x24FFFFFF),
        glassHighlight = Color(0x29FFFFFF),
        sheetScrim = Color(0x99000000),
    )
}

private fun lightTokens(choice: AccentChoice): HyperTokens {
    val accent = accentOnLight(choice)
    return HyperTokens(
        dark = false,
        // Kertas hangat, bukan putih murni: kartu putih tetap terbaca.
        bg = Color(0xFFF6F4F1),
        card = Color(0xFFFFFFFF),
        cardRaised = Color(0xFFEFEBE6),
        hairline = Color(0x1A000000),
        hairlineStrong = Color(0x33000000),
        text = Color(0xFF141210),
        text2 = Color(0xFF6A645C),
        text3 = Color(0xFF958E85),
        accent = accent,
        accentInk = Color(0xFFFFFFFF),
        accentSoft = accent.copy(alpha = 0.12f),
        accentEdge = accent.copy(alpha = 0.35f),
        track = Color(0xFFE2DCD5),
        pressed = Color(0x14000000),
        chip = Color(0x0D000000),
        ok = Color(0xFF1C7A4A),
        warn = Color(0xFF8A5A00),
        glassFill = Color(0xCCFFFFFF),
        glassEdge = Color(0x1A000000),
        glassHighlight = Color(0x33FFFFFF),
        sheetScrim = Color(0x66000000),
    )
}

/** Token yang diturunkan dari skema Material You saat warna dinamis aktif. */
private fun tokensFromScheme(scheme: ColorScheme, dark: Boolean): HyperTokens = HyperTokens(
    dark = dark,
    bg = scheme.surface,
    card = scheme.surfaceContainer,
    cardRaised = scheme.surfaceContainerHigh,
    hairline = if (dark) Color(0x17FFFFFF) else Color(0x1A000000),
    hairlineStrong = if (dark) Color(0x2EFFFFFF) else Color(0x33000000),
    text = scheme.onSurface,
    text2 = scheme.onSurfaceVariant,
    text3 = scheme.onSurfaceVariant.copy(alpha = 0.66f),
    accent = scheme.primary,
    accentInk = scheme.onPrimary,
    accentSoft = scheme.primary.copy(alpha = 0.12f),
    accentEdge = scheme.primary.copy(alpha = 0.38f),
    track = scheme.surfaceVariant,
    pressed = if (dark) Color(0x1FFFFFFF) else Color(0x14000000),
    chip = if (dark) Color(0x12FFFFFF) else Color(0x0D000000),
    ok = if (dark) Color(0xFF6FD99A) else Color(0xFF1C7A4A),
    warn = if (dark) Color(0xFFFFB547) else Color(0xFF8A5A00),
    glassFill = if (dark) Color(0x66121212) else Color(0xCCFFFFFF),
    glassEdge = if (dark) Color(0x24FFFFFF) else Color(0x1A000000),
    glassHighlight = if (dark) Color(0x29FFFFFF) else Color(0x33FFFFFF),
    sheetScrim = if (dark) Color(0x99000000) else Color(0x66000000),
)

/**
 * Token aktif. Getter (bukan `val` tetap) supaya [GofileTheme] bisa menukar
 * palet saat pengguna mengganti tema tanpa menyentuh satu pun pemanggil.
 */
object Hyper {
    @Volatile
    internal var tokens: HyperTokens = darkTokens(AccentChoice.CORAL)

    val Bg: Color get() = tokens.bg
    val Card: Color get() = tokens.card
    val CardRaised: Color get() = tokens.cardRaised
    val Hairline: Color get() = tokens.hairline
    val HairlineStrong: Color get() = tokens.hairlineStrong
    val Text: Color get() = tokens.text
    val Text2: Color get() = tokens.text2
    val Text3: Color get() = tokens.text3
    val Accent: Color get() = tokens.accent
    val AccentInk: Color get() = tokens.onAccent
    val AccentSoft: Color get() = tokens.accentSoft
    val AccentEdge: Color get() = tokens.accentEdge
    val Track: Color get() = tokens.track
    val Pressed: Color get() = tokens.pressed
    val Chip: Color get() = tokens.chip
    val Ok: Color get() = tokens.ok
    val Warn: Color get() = tokens.warn
    val GlassFill: Color get() = tokens.glassFill
    val GlassEdge: Color get() = tokens.glassEdge
    val GlassHighlight: Color get() = tokens.glassHighlight
    val SheetScrim: Color get() = tokens.sheetScrim

    /** Benar saat palet terang aktif — dipakai untuk memilih ikon status bar. */
    val isLight: Boolean get() = !tokens.dark
}

private fun schemeFor(dark: Boolean, choice: AccentChoice): ColorScheme {
    val accent = if (dark) accentColor(choice) else accentOnLight(choice)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = if (dark) Color(0xFF000000) else Color(0xFFFFFFFF),
        primaryContainer = if (dark) Color(0xFF26130F) else accent.copy(alpha = 0.16f),
        onPrimaryContainer = if (dark) accent.copy(alpha = 0.85f) else accent,
        inversePrimary = accent,
        secondary = if (dark) Color(0xFF8E8E96) else Color(0xFF6A645C),
        onSecondary = if (dark) Color(0xFF000000) else Color(0xFFFFFFFF),
        secondaryContainer = if (dark) Color(0xFF17171A) else Color(0xFFEFEBE6),
        onSecondaryContainer = if (dark) Color(0xFFF5F5F7) else Color(0xFF141210),
        tertiary = accent,
        onTertiary = if (dark) Color(0xFF000000) else Color(0xFFFFFFFF),
        background = if (dark) Color(0xFF000000) else Color(0xFFF6F4F1),
        onBackground = if (dark) Color(0xFFF5F5F7) else Color(0xFF141210),
        surface = if (dark) Color(0xFF000000) else Color(0xFFF6F4F1),
        onSurface = if (dark) Color(0xFFF5F5F7) else Color(0xFF141210),
        surfaceVariant = if (dark) Color(0xFF26262B) else Color(0xFFE2DCD5),
        onSurfaceVariant = if (dark) Color(0xFF8E8E96) else Color(0xFF6A645C),
        surfaceTint = Color.Transparent,
        surfaceContainerLowest = if (dark) Color(0xFF000000) else Color(0xFFFBF9F7),
        surfaceContainerLow = if (dark) Color(0xFF000000) else Color(0xFFF6F4F1),
        surfaceContainer = if (dark) Color(0xFF0E0E10) else Color(0xFFFFFFFF),
        surfaceContainerHigh = if (dark) Color(0xFF0E0E10) else Color(0xFFFFFFFF),
        surfaceContainerHighest = if (dark) Color(0xFF17171A) else Color(0xFFEFEBE6),
        // HyperSpring tidak memakai merah: aksi merusak tetap aksen.
        error = accent,
        onError = if (dark) Color(0xFF000000) else Color(0xFFFFFFFF),
        errorContainer = if (dark) Color(0xFF2A1310) else accent.copy(alpha = 0.14f),
        onErrorContainer = if (dark) accent.copy(alpha = 0.88f) else accent,
        outline = if (dark) Color(0x17FFFFFF) else Color(0x1A000000),
        outlineVariant = if (dark) Color(0x17FFFFFF) else Color(0x1A000000),
        scrim = Color(0xFF000000),
    )
}

private val HyperShapes = Shapes(
    extraSmall = RoundedCornerShape(9.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

/**
 * Tema aplikasi.
 *
 * @param mode System / Dark / Light.
 * @param accent aksen pilihan pengguna (koral secara default).
 * @param dynamic ikuti warna dinamis Material You (Android 12+).
 * @param fontScale pengali ukuran teks 0,85–1,3 dari Setelan; skala font sistem
 *   tetap berlaku di atasnya.
 */
@Composable
fun GofileTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentChoice = AccentChoice.CORAL,
    dynamic: Boolean = false,
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
    val tokens = if (dynamicAvailable) tokensFromScheme(scheme, dark) else {
        if (dark) darkTokens(accent) else lightTokens(accent)
    }
    Hyper.tokens = tokens
    val typography = if (fontScale == 1f) HyperTypography else HyperTypography.scaled(fontScale)

    MaterialTheme(
        colorScheme = scheme,
        typography = typography,
        shapes = HyperShapes,
    ) {
        // Palet dibaca lewat getter global: kunci ini memaksa subtree menggambar
        // ulang saat tema, aksen, atau skala teks berubah.
        key(tokens, fontScale) { content() }
    }
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

private fun style(
    size: Float,
    weight: FontWeight,
    lineHeight: Float,
    letterSpacing: Float = 0f,
) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

/**
 * Skala HyperSpring. Judul layar 40sp dengan jarak huruf negatif, judul
 * bagian huruf kapital kecil berjarak lebar, baris 15,5sp.
 */
private val HyperTypography = Typography(
    displayLarge = style(40f, FontWeight.Bold, 41f, -1.0f),
    displayMedium = style(32f, FontWeight.Bold, 33f, -0.8f),
    displaySmall = style(28f, FontWeight.Bold, 29f, -0.7f),
    headlineLarge = style(32f, FontWeight.Bold, 33f, -0.8f),
    headlineMedium = style(26f, FontWeight.Bold, 27f, -0.65f),
    headlineSmall = style(22f, FontWeight.Bold, 24f, -0.5f),
    titleLarge = style(20f, FontWeight.Bold, 23f, -0.4f),
    titleMedium = style(16f, FontWeight.SemiBold, 20f, 0f),
    titleSmall = style(15.5f, FontWeight.Medium, 19f, 0f),
    bodyLarge = style(16f, FontWeight.Normal, 24f, 0f),
    bodyMedium = style(14f, FontWeight.Normal, 19f, 0f),
    bodySmall = style(12.5f, FontWeight.Normal, 15f, 0f),
    labelLarge = style(14f, FontWeight.SemiBold, 18f, 0f),
    labelMedium = style(12f, FontWeight.Medium, 15f, 0.2f),
    labelSmall = style(10f, FontWeight.SemiBold, 13f, 1.2f),
)
