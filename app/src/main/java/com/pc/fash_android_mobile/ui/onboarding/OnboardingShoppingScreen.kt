package com.pc.fash_android_mobile.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.ui.components.FashPrimaryButton
import com.pc.fash_android_mobile.ui.theme.FashTheme

@Composable
fun OnboardingShoppingScreen(
    buySelected: Boolean,
    sellSelected: Boolean,
    isSubmitting: Boolean,
    progressStep: Int,
    progressTotal: Int = OnboardingFlowProgress.TOTAL_STEPS,
    onToggleBuy: () -> Unit,
    onToggleSell: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = FashTheme.spacing.editorialStart),
        ) {
            OnboardingProgressBar(currentStep = progressStep, totalSteps = progressTotal)
            Spacer(Modifier.height(24.dp))
            ShoppingPreferencesOnboardScreen(
                buySelected = buySelected,
                sellSelected = sellSelected,
                onToggleBuy = onToggleBuy,
                onToggleSell = onToggleSell,
                modifier = Modifier.weight(1f),
            )
            FashPrimaryButton(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                label = stringResource(com.pc.fash_android_mobile.R.string.onboarding_continue),
                enabled = !isSubmitting && (buySelected || sellSelected),
            )
        }
    }
}
