package com.nellecrzier.gofileexpressive.ui.expressive

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tiruan API Material 3 Expressive untuk material3 1.4.0.
 *
 * Nama dan tanda tangannya dibuat semirip mungkin dengan API resmi 1.5.0
 * (`MotionScheme`, `ConnectedButtonGroup`, `FloatingToolbar`, indikator) supaya
 * saat AGP 9 / compileSdk 37 tersedia, migrasinya cukup mengganti import dari
 * `ui.expressive.*` ke `androidx.compose.material3.*`.
 *
 * Nilai spring diambil dari spesifikasi Expressive, bukan dikira-kira.
 */
@Immutable
interface ExpressiveMotionScheme {
    fun <T> fastSpatialSpec(): FiniteAnimationSpec<T>
    fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T>
    fun <T> slowSpatialSpec(): FiniteAnimationSpec<T>
    fun <T> fastEffectsSpec(): FiniteAnimationSpec<T>
    fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T>
    fun <T> slowEffectsSpec(): FiniteAnimationSpec<T>
}

/** Implementasi sesuai angka resmi: spatial boleh overshoot, effects tidak. */
object ExpressiveMotion : ExpressiveMotionScheme {
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.6f, stiffness = 800f)

    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 380f)

    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 200f)

    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = 3800f)

    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = 1600f)

    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = 800f)
}

val LocalMotionScheme = staticCompositionLocalOf<ExpressiveMotionScheme> { ExpressiveMotion }

/** Akses singkat, sepadan dengan `MaterialTheme.motionScheme`. */
val motionScheme: ExpressiveMotionScheme
    @Composable get() = LocalMotionScheme.current

/**
 * Sudut bersambung untuk daftar bersegmen: sudut luar besar, sudut dalam kecil.
 * Item pertama, tengah, terakhir, dan tunggal mendapat kombinasi berbeda.
 */
fun connectedShape(
    index: Int,
    count: Int,
    outer: Dp = 24.dp,
    inner: Dp = 6.dp,
): Shape {
    if (count <= 1) return RoundedCornerShape(outer)
    val first = index == 0
    val last = index == count - 1
    return when {
        first -> RoundedCornerShape(topStart = outer, bottomStart = outer, topEnd = inner, bottomEnd = inner)
        last -> RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = outer, bottomEnd = outer)
        else -> RoundedCornerShape(inner)
    }
}

/**
 * Grup tombol bersambung. Pengganti `ConnectedButtonGroup` 1.5.0 selama API itu
 * belum tersedia; perilaku dan bentuknya sama.
 */
@Composable
fun ConnectedButtonGroup(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    }
}

/** Toolbar melayang: kapsul tonal di atas konten, untuk aksi pilihan. */
@Composable
fun FloatingToolbar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            content = content,
        )
    }
}

/** Spesifikasi spring untuk komponen kecil (dipakai helper gerak). */
@Composable
fun fastSpatial(): FiniteAnimationSpec<Float> = motionScheme.fastSpatialSpec<Float>()

@Composable
fun defaultSpatial(): FiniteAnimationSpec<Float> = motionScheme.defaultSpatialSpec<Float>()

@Composable
fun slowSpatial(): FiniteAnimationSpec<Float> = motionScheme.slowSpatialSpec<Float>()

@Composable
fun defaultEffects(): FiniteAnimationSpec<Float> = motionScheme.defaultEffectsSpec<Float>()

@Suppress("unused")
private val unusedSpec: SpringSpec<Float> = spring()
