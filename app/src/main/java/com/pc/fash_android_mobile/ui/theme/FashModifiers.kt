package com.pc.fash_android_mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ambient “lift” for floating elements — primary-tinted (~6% opacity), not black.
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

/** Semi-transparent surface for top nav / sheet “glass” stack; pair with content behind. */
@Composable
fun fashGlassScrimColor(alpha: Float = 0.88f): Color =
    MaterialTheme.colorScheme.surface.copy(alpha = alpha)
