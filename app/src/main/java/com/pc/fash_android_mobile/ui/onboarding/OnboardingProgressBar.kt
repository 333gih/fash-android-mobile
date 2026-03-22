package com.pc.fash_android_mobile.ui.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
    val fraction = (currentStep.toFloat() / totalSteps).coerceIn(0f, 1f)
    LinearProgressIndicator(
        progress = { fraction },
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .padding(horizontal = 24.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
}
