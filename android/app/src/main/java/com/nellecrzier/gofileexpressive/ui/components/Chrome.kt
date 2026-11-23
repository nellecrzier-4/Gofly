package com.nellecrzier.gofileexpressive.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tinggi chrome navigasi yang melayang di atas konten (bilah navigasi bawah).
 * Layar memakai nilai ini sebagai padding bawah supaya baris terakhir tidak
 * tertutup; di layar lebar dengan rel nilainya nol.
 */
val LocalBottomChromeHeight = staticCompositionLocalOf<Dp> { 0.dp }
