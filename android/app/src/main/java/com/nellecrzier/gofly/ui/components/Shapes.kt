package com.nellecrzier.gofly.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Sudut bersambung untuk daftar bergaya Material 3 Expressive: sudut luar
 * besar, sudut dalam kecil. Item pertama, tengah, terakhir dan tunggal
 * mendapat kombinasi berbeda.
 */
fun connectedShape(
    index: Int,
    count: Int,
    outer: Dp = 26.dp,
    inner: Dp = 6.dp,
): Shape {
    if (count <= 1) return RoundedCornerShape(outer)
    val first = index == 0
    val last = index == count - 1
    return when {
        first -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner)
        last -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer)
        else -> RoundedCornerShape(inner)
    }
}

/**
 * Versi yang menganimasikan perubahan sudut saat item disisipkan, dihapus,
 * atau berubah posisi. Memakai spesifikasi spatial tercepat.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun animatedConnectedShape(
    index: Int,
    count: Int,
    reduceMotion: Boolean,
    outer: Dp = 26.dp,
    inner: Dp = 6.dp,
): Shape {
    val first = index == 0
    val last = index == count - 1
    val single = count <= 1
    val targetTopStart = if (single || first) outer else inner
    val targetTopEnd = if (single || first) outer else inner
    val targetBottomStart = if (single || last) outer else inner
    val targetBottomEnd = if (single || last) outer else inner
    val spec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()
    val fallback = androidx.compose.animation.core.tween<Dp>(0)
    val topStart by animateDpAsState(targetTopStart, if (reduceMotion) fallback else spec, label = "topStart")
    val topEnd by animateDpAsState(targetTopEnd, if (reduceMotion) fallback else spec, label = "topEnd")
    val bottomEnd by animateDpAsState(targetBottomEnd, if (reduceMotion) fallback else spec, label = "bottomEnd")
    val bottomStart by animateDpAsState(targetBottomStart, if (reduceMotion) fallback else spec, label = "bottomStart")
    return RoundedCornerShape(topStart = topStart, topEnd = topEnd, bottomEnd = bottomEnd, bottomStart = bottomStart)
}
