package com.pc.fash_android_mobile.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Bottom dock for promo content; when [elevated] is true, animates shadow and divider emphasis
 * (used when the inline promo has scrolled off-screen and this copy is shown).
 */
@Composable
fun StickyBottomPromoBar(
    elevated: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shadowElevation by animateDpAsState(
        targetValue = if (elevated) 10.dp else 0.dp,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "stickyBottomPromoShadow",
    )
    val dividerAlpha by animateFloatAsState(
        targetValue = if (elevated) 0.55f else 0.32f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "stickyBottomPromoDividerAlpha",
    )
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 1.dp,
            color = scheme.outlineVariant.copy(alpha = dividerAlpha),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = scheme.surfaceContainerLow,
            tonalElevation = 0.dp,
            shadowElevation = shadowElevation,
        ) {
            content()
        }
    }
}
