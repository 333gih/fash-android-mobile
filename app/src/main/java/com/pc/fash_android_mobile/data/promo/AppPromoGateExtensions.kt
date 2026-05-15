package com.pc.fash_android_mobile.data.promo

import com.pc.fash_android_mobile.ui.onboarding.OnboardingFlowProgress

fun AppPromoGateContext.profileSetupBlocksShellChrome(): Boolean =
    OnboardingFlowProgress.blocksShellPromosAndTours(needsOnboarding)
