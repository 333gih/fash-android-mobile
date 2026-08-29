package com.pc.fash_android_mobile.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.res.stringResource
import com.pc.fash_android_mobile.R

@Composable
fun AppFeatureTourOverlay(
    visible: Boolean,
    anchors: Map<FeatureTourAnchor, LayoutCoordinates>,
    currentStep: AppTourStep,
    onStepChange: (AppTourStep) -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    val steps = AppTourStep.entries
    MascotSpotlightOverlay(
        title = stepTitle(currentStep),
        bodyText = stepBody(currentStep),
        anchor = currentStep.anchor(),
        anchors = anchors,
        stepIndex = currentStep.ordinal,
        stepCount = steps.size,
        showBack = currentStep != AppTourStep.Intro,
        isLast = currentStep == steps.last(),
        onBack = {
            val prev = steps.getOrNull(currentStep.ordinal - 1)
            if (prev != null) onStepChange(prev)
        },
        onNext = {
            val next = steps.getOrNull(currentStep.ordinal + 1)
            if (next == null) onFinish() else onStepChange(next)
        },
        onSkip = onSkip,
        modifier = modifier,
    )
}

@Composable
private fun stepTitle(step: AppTourStep): String = stringResource(
    when (step) {
        AppTourStep.Intro -> R.string.app_tour_intro_title
        AppTourStep.NavHome -> R.string.app_tour_nav_home_title
        AppTourStep.NavOrders -> R.string.app_tour_nav_orders_title
        AppTourStep.NavPost -> R.string.app_tour_nav_post_title
        AppTourStep.NavChat -> R.string.app_tour_nav_chat_title
        AppTourStep.NavProfile -> R.string.app_tour_nav_profile_title
        AppTourStep.TopBarActions -> R.string.app_tour_top_bar_title
    },
)

@Composable
private fun stepBody(step: AppTourStep): String = stringResource(
    when (step) {
        AppTourStep.Intro -> R.string.app_tour_intro_body
        AppTourStep.NavHome -> R.string.app_tour_nav_home_body
        AppTourStep.NavOrders -> R.string.app_tour_nav_orders_body
        AppTourStep.NavPost -> R.string.app_tour_nav_post_body
        AppTourStep.NavChat -> R.string.app_tour_nav_chat_body
        AppTourStep.NavProfile -> R.string.app_tour_nav_profile_body
        AppTourStep.TopBarActions -> R.string.app_tour_top_bar_body
    },
)
