package com.nellecrzier.gofly.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Tinggi chrome navigasi yang melayang di atas konten (bilah navigasi bawah).
 * Layar memakai nilai ini sebagai padding bawah supaya baris terakhir tidak
 * tertutup; di layar lebar dengan rel nilainya nol.
 */
val LocalBottomChromeHeight = staticCompositionLocalOf<Dp> { 0.dp }

/** Berapa kali tab ini diaktifkan; naik tiap kali pengguna membuka tab tersebut. */
val LocalTabActivation = compositionLocalOf { 0 }

/**
 * `true` sesaat setelah tab dibuka. Tab dibiarkan hidup antar perpindahan, jadi
 * animasi masuk bertahap diputar ulang lewat sinyal ini, bukan lewat komposisi ulang.
 */
@Composable
fun rememberTabEntered(): Boolean {
    val activation = LocalTabActivation.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(activation) {
        shown = false
        delay(40)
        shown = true
    }
    return shown
}
