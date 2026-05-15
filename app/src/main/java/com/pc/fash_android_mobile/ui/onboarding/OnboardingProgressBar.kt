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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.FashColors

/**
 * Profile-setup progress (1…[totalSteps]): password → aesthetics → sizing → username.
 * See [OnboardingFlowProgress].
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

@Composable
fun OnboardingStepCaption(
    currentStep: Int,
    totalSteps: Int = OnboardingFlowProgress.TOTAL_STEPS,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.onboarding_progress_step, currentStep, totalSteps),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}
