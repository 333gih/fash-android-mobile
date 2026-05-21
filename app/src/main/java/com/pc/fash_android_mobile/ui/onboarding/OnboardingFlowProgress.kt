package com.pc.fash_android_mobile.ui.onboarding

/**
 * Onboarding chain after login (server [com.pc.fash_android_mobile.data.user.UserAccessStatus]):
 * 1. Password → 2. Aesthetic tags → 3. Shopping prefs → 4. Sizing → 5. Username.
 *
 * Progress bar uses 1-based [progressStep] / [TOTAL_STEPS] (OTP is not part of this bar).
 */
object OnboardingFlowProgress {
    const val TOTAL_STEPS: Int = 5

    fun progressStep(step: OnboardingStep): Int =
        when (step) {
            OnboardingStep.SetupPassword -> 1
            OnboardingStep.AestheticTags -> 2
            OnboardingStep.ShoppingPreferences -> 3
            OnboardingStep.SizingReference -> 4
            OnboardingStep.UsernameOnboard -> 5
            OnboardingStep.Completed -> TOTAL_STEPS
        }

    /**
     * Promo dialogs, feature tour, and WS promos must wait until profile setup is finished.
     * [MainActivity] also treats [com.pc.fash_android_mobile.ui.onboarding.OnboardingStep] as part
     * of the gate when the server briefly reports [canAccessHome] before the client flow ends.
     */
    fun blocksShellPromosAndTours(needsOnboarding: Boolean?): Boolean = needsOnboarding != false
}
