package com.pc.fash_android_mobile.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ambient "lift" for floating elements — primary-tinted (~6% opacity), not black.
 * Spec: ~20px blur feel; Material [elevation] maps loosely to blur radius.
 */
fun Modifier.fashAmbientShadow(elevation: Dp = 12.dp, shape: Shape) =
    shadow(
        elevation = elevation,
        shape = shape,
        spotColor = FashColors.AmbientShadowBase.copy(alpha = 0.06f),
        ambientColor = FashColors.AmbientShadowBase.copy(alpha = 0.06f),
        clip = false,
    )

/** Semi-transparent surface for top nav / sheet "glass" stack; pair with content behind. */
@Composable
fun fashGlassScrimColor(alpha: Float = 0.88f): Color =
    MaterialTheme.colorScheme.surface.copy(alpha = alpha)

/**
 * Animated horizontal shimmer sweep — use on image placeholder [Box]es while content loads.
 * Colors are taken from the design system's warm surface tokens so the effect is on-brand
 * in both light (cream → white) and dark (warm charcoal → lighter charcoal) themes.
 */
@Composable
fun Modifier.fashShimmer(): Modifier {
    val scheme = MaterialTheme.colorScheme
    val shimmerColors = listOf(
        scheme.surfaceContainerHigh,
        scheme.surfaceContainerHighest,
        scheme.surfaceContainerHigh,
    )
    val transition = rememberInfiniteTransition(label = "fash_shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_progress",
    )
    return this then Modifier.drawBehind {
        val width = size.width
        val sweepWidth = width * 0.75f
        val startX = (width + sweepWidth) * progress - sweepWidth
        drawRect(
            brush = Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(startX, 0f),
                end = Offset(startX + sweepWidth, 0f),
            ),
        )
    }
}
