package com.pc.fash_android_mobile.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Foreground color that stays readable on [this] solid fill.
 * Use instead of hard-coded [FashColors.OnPrimary] / [Color.White] so light or dynamic primaries
 * (Material You, OEM color profiles) still get dark text when the fill is light.
 */
fun Color.fashReadableOn(): Color =
    if (luminance() > 0.5f) FashColors.OnSurface else Color(0xFFFFFFFF)

/**
 * Text on a horizontal/linear gradient: use the darkest stop (most conservative for light text).
 */
fun List<Color>.fashReadableOnGradient(): Color {
    if (isEmpty()) return Color(0xFFFFFFFF)
    return minBy { it.luminance() }.fashReadableOn()
}
