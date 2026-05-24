package com.pc.fash_android_mobile.ui.onboarding

import com.pc.fash_android_mobile.ui.main.MainTab

/**
 * Anchors registered via [onGloballyPositioned] for the feature tour overlay.
 * Keep this list small and stable — copy lives in `strings.xml` / `values-en/strings.xml`.
 */
enum class FeatureTourAnchor {
    BottomHome,
    BottomOrders,
    BottomPostFab,
    BottomChat,
    BottomProfile,
    TopActionsRow,
}

enum class AppTourStep {
    Intro,
    NavHome,
    NavOrders,
    NavPost,
    NavChat,
    NavProfile,
    TopBarActions,
    ;

    fun anchor(): FeatureTourAnchor? = when (this) {
        Intro -> null
        NavHome -> FeatureTourAnchor.BottomHome
        NavOrders -> FeatureTourAnchor.BottomOrders
        NavPost -> FeatureTourAnchor.BottomPostFab
        NavChat -> FeatureTourAnchor.BottomChat
        NavProfile -> FeatureTourAnchor.BottomProfile
        TopBarActions -> FeatureTourAnchor.TopActionsRow
    }

    /**
     * Tab to select before measuring the anchor (Post stays on Home so the bottom bar stays visible).
     */
    fun prepareTab(): MainTab? = when (this) {
        Intro -> MainTab.Home
        NavHome -> MainTab.Home
        NavOrders -> MainTab.Orders
        NavPost -> MainTab.Home
        NavChat -> MainTab.Chat
        NavProfile -> MainTab.Profile
        TopBarActions -> MainTab.Home
    }
}
