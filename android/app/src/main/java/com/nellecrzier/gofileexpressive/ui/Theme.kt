package com.nellecrzier.gofileexpressive.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bahasa visual HyperSpring: hitam murni, satu aksen koral, kartu yang
 * dibedakan oleh garis rambut (bukan bayangan), dan tipografi tebal
 * berjarak rapat.
 *
 * Tidak ada mode terang dan tidak ada warna dinamis. HyperSpring memakai
 * palet tetap supaya tampilan identik di semua perangkat.
 */
object Hyper {
    /** Latar halaman: hitam murni untuk panel OLED. */
    val Bg = Color(0xFF000000)

    /** Permukaan kartu: nyaris tidak lebih terang dari latar. */
    val Card = Color(0xFF0E0E10)

    /** Kartu yang sedikit terangkat (baris di dalam kartu, panel transfer). */
    val CardRaised = Color(0xFF17171A)

    /** Garis rambut kartu: putih 9%. */
    val Hairline = Color(0x17FFFFFF)

    /** Garis rambut yang lebih tegas untuk tombol sekunder: putih 18%. */
    val HairlineStrong = Color(0x2EFFFFFF)

    val Text = Color(0xFFF5F5F7)
    val Text2 = Color(0xFF8E8E96)
    val Text3 = Color(0xFF5C5C66)

    /** Satu-satunya aksen aplikasi. */
    val Accent = Color(0xFFFF5F45)
    val AccentInk = Color(0xFF000000)
    val AccentSoft = Color(0x1FFF5F45)
    val AccentEdge = Color(0x66FF5F45)

    /** Rel toggle/slider saat mati. */
    val Track = Color(0xFF26262B)

    /** Riak tekan baris. */
    val Pressed = Color(0x1FFFFFFF)

    /** Isi chip ikon kecil di dalam baris. */
    val Chip = Color(0x12FFFFFF)

    val Ok = Color(0xFF6FD99A)
    val Warn = Color(0xFFFFB547)

    /** Kaca: isi, tepi, dan kilau atas. */
    val GlassFill = Color(0x66121212)
    val GlassEdge = Color(0x24FFFFFF)
    val GlassHighlight = Color(0x29FFFFFF)

    /** Scrim lembar dan menu. */
    val SheetScrim = Color(0x99000000)
}

private val HyperColors: ColorScheme = darkColorScheme(
    primary = Hyper.Accent,
    onPrimary = Hyper.AccentInk,
    primaryContainer = Color(0xFF26130F),
    onPrimaryContainer = Color(0xFFFF8A72),
    inversePrimary = Hyper.Accent,

    secondary = Hyper.Text2,
    onSecondary = Hyper.AccentInk,
    secondaryContainer = Hyper.CardRaised,
    onSecondaryContainer = Hyper.Text,

    tertiary = Hyper.Accent,
    onTertiary = Hyper.AccentInk,
    tertiaryContainer = Hyper.CardRaised,
    onTertiaryContainer = Hyper.Text,

    background = Hyper.Bg,
    onBackground = Hyper.Text,
    surface = Hyper.Bg,
    onSurface = Hyper.Text,
    surfaceVariant = Hyper.Track,
    onSurfaceVariant = Hyper.Text2,
    surfaceTint = Color.Transparent,
    inverseSurface = Hyper.Text,
    inverseOnSurface = Hyper.Bg,

    surfaceContainerLowest = Hyper.Bg,
    surfaceContainerLow = Hyper.Bg,
    surfaceContainer = Hyper.Card,
    surfaceContainerHigh = Hyper.Card,
    surfaceContainerHighest = Hyper.CardRaised,

    // HyperSpring tidak memakai merah: aksi merusak tetap koral.
    error = Hyper.Accent,
    onError = Hyper.AccentInk,
    errorContainer = Color(0xFF2A1310),
    onErrorContainer = Color(0xFFFF9A8A),

    outline = Hyper.Hairline,
    outlineVariant = Hyper.Hairline,
    scrim = Color(0xFF000000),
)

private val HyperShapes = Shapes(
    extraSmall = RoundedCornerShape(9.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun GofileTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = HyperColors,
        typography = HyperTypography,
        shapes = HyperShapes,
        content = content,
    )
}

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
