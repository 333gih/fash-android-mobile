package com.pc.fash_android_mobile.ui.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
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
import com.pc.fash_android_mobile.ui.theme.FashTheme

/**
 * Fixed-height onboarding progress strip. [displayProgressStep] is 0-based (0 = empty track at the
 * first step); it increases when moving forward and can decrease when navigating back.
 */
@Composable
fun OnboardingProgressHeader(
    displayProgressStep: Int,
    totalSteps: Int = OnboardingFlowProgress.TOTAL_STEPS,
    modifier: Modifier = Modifier,
) {
    val target = (displayProgressStep.toFloat() / totalSteps.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "onboardingProgress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(OnboardingFlowProgress.PROGRESS_HEADER_HEIGHT)
            .padding(horizontal = FashTheme.spacing.editorialStart),
    ) {
        LinearProgressIndicator(
            progress = { animatedFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .align(androidx.compose.ui.Alignment.Center),
            color = FashColors.Primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

/** @deprecated Use [OnboardingProgressHeader] — step caption removed to avoid layout shift. */
@Composable
fun OnboardingProgressBar(
    currentStep: Int,
    totalSteps: Int = OnboardingFlowProgress.TOTAL_STEPS,
    modifier: Modifier = Modifier,
) {
    OnboardingProgressHeader(
        displayProgressStep = currentStep,
        totalSteps = totalSteps,
        modifier = modifier,
    )
}
