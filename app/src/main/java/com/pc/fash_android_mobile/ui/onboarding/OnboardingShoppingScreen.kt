package com.pc.fash_android_mobile.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.components.FashPrimaryButton
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

@Composable
fun OnboardingShoppingScreen(
    buySelected: Boolean,
    sellSelected: Boolean,
    isSubmitting: Boolean,
    displayProgressStep: Int,
    progressTotal: Int = OnboardingFlowProgress.TOTAL_STEPS,
    onToggleBuy: () -> Unit,
    onToggleSell: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    selectedGender: String = "",
    onGenderSelect: (String) -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    Surface(modifier = modifier.fillMaxSize(), color = scheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = FashColors.Primary,
                    )
                }
                Spacer(modifier = Modifier.width(48.dp))
            }
            OnboardingProgressHeader(
                displayProgressStep = displayProgressStep,
                totalSteps = progressTotal,
            )
            Spacer(Modifier.height(16.dp))
            ShoppingPreferencesOnboardScreen(
                buySelected = buySelected,
                sellSelected = sellSelected,
                onToggleBuy = onToggleBuy,
                onToggleSell = onToggleSell,
                selectedGender = selectedGender,
                onGenderSelect = onGenderSelect,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = FashTheme.spacing.editorialStart),
            )
            FashPrimaryButton(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FashTheme.spacing.editorialStart)
                    .padding(bottom = 24.dp),
                label = stringResource(R.string.onboarding_continue),
                enabled = !isSubmitting && (buySelected || sellSelected),
            )
        }
    }
}
