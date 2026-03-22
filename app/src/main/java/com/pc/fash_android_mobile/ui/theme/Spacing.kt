package com.pc.fash_android_mobile.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Layout rhythm: prefer vertical whitespace over dividers ([spacing3]/[spacing4] between list rows).
 * [spacing5] matches spec ~1.7rem between feed blocks.
 */
@Immutable
data class FashSpacing(
    val spacing1: Dp = 4.dp,
    val spacing2: Dp = 8.dp,
    val spacing3: Dp = 12.dp,
    val spacing4: Dp = 16.dp,
    val spacing5: Dp = 27.dp,
    val spacing6: Dp = 32.dp,
    val spacing7: Dp = 40.dp,
    val spacing8: Dp = 48.dp,
    /** Primary button height */
    val buttonHeight: Dp = 48.dp,
    /** Editorial asymmetry — section horizontal inset */
    val editorialStart: Dp = 24.dp,
    val editorialEnd: Dp = 16.dp,
    /** Minimum tactile radius (components) */
    val radiusSoftMin: Dp = 12.dp,
    val radiusCard: Dp = 16.dp,
    val radiusPill: Dp = 20.dp,
) {
    fun chipShape() = RoundedCornerShape(radiusPill)
}

fun FashSpacing.editorialHorizontalPadding(): PaddingValues =
    PaddingValues(start = editorialStart, end = editorialEnd)

fun FashSpacing.editorialHorizontalPadding(vertical: Dp): PaddingValues =
    PaddingValues(start = editorialStart, end = editorialEnd, top = vertical, bottom = vertical)
