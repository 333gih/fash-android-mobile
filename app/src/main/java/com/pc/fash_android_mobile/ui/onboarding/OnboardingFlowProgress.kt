package com.pc.fash_android_mobile.ui.onboarding

import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.data.user.UserAccessStatus

/**
 * Canonical onboarding chain (same order for every user session):
 * Password (if needed) → Aesthetic → Shopping → Profile photo → Sizing → Username.
 *
 * On cold entry the UI lands on the **first incomplete** step only; [buildPriorSteps] still
 * includes every earlier step so Back can revisit completed steps for editing.
 *
 * Progress bar: index 0 = empty track, increases by one per step forward (can decrease on Back).
 */
object OnboardingFlowProgress {
    /** Max steps when password gate is included. Actual [buildCanonicalSteps] size may be 5 without it. */
    const val TOTAL_STEPS: Int = 6

    val PROGRESS_HEADER_HEIGHT = 20.dp

    fun buildCanonicalSteps(
        includePassword: Boolean,
        includeSizing: Boolean = true,
    ): List<OnboardingStep> = buildList {
        if (includePassword) add(OnboardingStep.SetupPassword)
        add(OnboardingStep.AestheticTags)
        add(OnboardingStep.ShoppingPreferences)
        add(OnboardingStep.ProfilePhoto)
        if (includeSizing) add(OnboardingStep.SizingReference)
        add(OnboardingStep.UsernameOnboard)
    }

    fun includesPasswordStep(status: UserAccessStatus): Boolean =
        status.needsPasswordSetup() ||
            status.nextStep?.trim()?.equals("password", ignoreCase = true) == true

    fun isStepComplete(
        step: OnboardingStep,
        status: UserAccessStatus,
        skippedAestheticTags: Boolean,
        skippedProfilePhoto: Boolean,
        skippedSizing: Boolean,
        hasAvatar: Boolean,
        skipSizingEnv: Boolean,
    ): Boolean = when (step) {
        OnboardingStep.SetupPassword ->
            !status.needsPasswordSetup() &&
                status.nextStep?.trim()?.equals("password", ignoreCase = true) != true
        OnboardingStep.AestheticTags ->
            status.aestheticTagsConfigured || skippedAestheticTags
        OnboardingStep.ShoppingPreferences ->
            status.shoppingPreferencesConfigured
        OnboardingStep.ProfilePhoto ->
            hasAvatar || skippedProfilePhoto
        OnboardingStep.SizingReference ->
            status.sizingReferenceCompleted || skippedSizing || skipSizingEnv
        OnboardingStep.UsernameOnboard ->
            status.onboardingDone
        OnboardingStep.Completed -> true
    }

    fun firstIncompleteStep(
        flow: List<OnboardingStep>,
        status: UserAccessStatus,
        skippedAestheticTags: Boolean,
        skippedProfilePhoto: Boolean,
        skippedSizing: Boolean,
        hasAvatar: Boolean,
        skipSizingEnv: Boolean,
    ): OnboardingStep {
        for (step in flow) {
            if (!isStepComplete(
                    step,
                    status,
                    skippedAestheticTags,
                    skippedProfilePhoto,
                    skippedSizing,
                    hasAvatar,
                    skipSizingEnv,
                )
            ) {
                return step
            }
        }
        return if (status.canAccessHome || status.onboardingDone) {
            OnboardingStep.Completed
        } else {
            flow.lastOrNull() ?: OnboardingStep.Completed
        }
    }

    fun buildPriorSteps(flow: List<OnboardingStep>, current: OnboardingStep): List<OnboardingStep> {
        val idx = flow.indexOf(current)
        if (idx <= 0) return emptyList()
        return flow.subList(0, idx)
    }

    fun nextStepInFlow(flow: List<OnboardingStep>, current: OnboardingStep): OnboardingStep? {
        val idx = flow.indexOf(current)
        if (idx < 0 || idx >= flow.lastIndex) return null
        return flow[idx + 1]
    }

    /** 0-based progress index — first step shows an empty bar (0 / total). */
    fun progressDisplayIndex(step: OnboardingStep, flow: List<OnboardingStep>): Int = when (step) {
        OnboardingStep.Completed -> flow.size
        else -> flow.indexOf(step).coerceAtLeast(0)
    }

    fun blocksShellPromosAndTours(needsOnboarding: Boolean?): Boolean = needsOnboarding != false
}
