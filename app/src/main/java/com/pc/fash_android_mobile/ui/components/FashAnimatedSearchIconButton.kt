package com.pc.fash_android_mobile.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R

/**
 * Home top-bar search with an occasional gentle shake to draw attention.
 */
@Composable
fun FashAnimatedSearchIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    animateHint: Boolean = false,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
) {
    val shake = if (animateHint) {
        val transition = rememberInfiniteTransition(label = "searchShakeHint")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 5200
                    0f at 0
                    0f at 4200
                    0.55f at 4280 using FastOutSlowInEasing
                    -0.55f at 4360 using FastOutSlowInEasing
                    0.45f at 4440 using FastOutSlowInEasing
                    -0.35f at 4520 using FastOutSlowInEasing
                    0f at 4600 using FastOutSlowInEasing
                    0f at 5200
                },
                repeatMode = RepeatMode.Restart,
            ),
            label = "searchShake",
        ).value
    } else {
        0f
    }

    IconButton(onClick = onClick, modifier = modifier) {
        Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.search_label),
                tint = iconTint,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        rotationZ = shake * 7f
                        translationX = shake * 1.8f
                    },
            )
        }
    }
}
