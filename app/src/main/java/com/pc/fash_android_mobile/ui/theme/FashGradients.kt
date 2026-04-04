package com.pc.fash_android_mobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Primary CTA: linear gradient 135° from [FashColors.Primary] → [FashColors.PrimaryDeep].
 * Use with [androidx.compose.foundation.background] on buttons sized by caller.
 */
object FashGradients {
    fun primaryCta(width: Float, height: Float): Brush {
        val end = Offset(width, height)
        return Brush.linearGradient(
            colors = listOf(FashColors.Primary, FashColors.PrimaryDeep),
            start = Offset.Zero,
            end = end,
        )
    }
}

@Composable
fun rememberPrimaryCtaBrush(widthDp: Dp, heightDp: Dp): Brush {
    val density = LocalDensity.current
    return remember(widthDp, heightDp, density) {
        with(density) {
            FashGradients.primaryCta(widthDp.toPx(), heightDp.toPx())
        }
    }
}

/** Ghost border on elevated cards — tuned so the edge reads on the paper canvas. */
fun ghostBorderColor(outlineVariant: Color): Color = outlineVariant.copy(alpha = 0.28f)
