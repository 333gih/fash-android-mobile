package com.pc.fash_android_mobile.ui.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.ui.theme.FashColors

/**
 * Common progress bar for the onboarding chain: OTP → Style selection → Profile setup.
 * @param currentStep 1-based step index (1=OTP, 2=Style, 3=Username)
 * @param totalSteps Total steps in the chain (default 3)
 */
@Composable
fun OnboardingProgressBar(
    currentStep: Int,
    totalSteps: Int = 3,
    modifier: Modifier = Modifier,
) {
    val target = (currentStep.toFloat() / totalSteps).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "onboardingProgress",
    )
    LinearProgressIndicator(
        progress = { animatedFraction },
        modifier = modifier
            .fillMaxWidth()
            .height(5.dp)
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(3.dp)),
        color = FashColors.Primary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
}
