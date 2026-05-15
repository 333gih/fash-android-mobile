package com.pc.fash_android_mobile.ui.onboarding

/**
 * Onboarding chain after login (server [com.pc.fash_android_mobile.data.user.UserAccessStatus]):
 * 1. Password → 2. Aesthetic tags → 3. Sizing reference → 4. Username.
 *
 * Progress bar uses 1-based [progressStep] / [TOTAL_STEPS] (OTP is not part of this bar).
 */
object OnboardingFlowProgress {
    const val TOTAL_STEPS: Int = 4

    fun progressStep(step: OnboardingStep): Int =
        when (step) {
            OnboardingStep.SetupPassword -> 1
            OnboardingStep.AestheticTags -> 2
            OnboardingStep.SizingReference -> 3
            OnboardingStep.UsernameOnboard -> 4
            OnboardingStep.Completed -> TOTAL_STEPS
        }

    /** Promo dialogs, feature tour, and WS promos must wait until profile setup is finished. */
    fun blocksShellPromosAndTours(needsOnboarding: Boolean?): Boolean = needsOnboarding != false
}
