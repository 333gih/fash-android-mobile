package com.pc.fash_android_mobile.data.promo

import androidx.annotation.StringRes

/** Visual / action family for the center interstitial card. */
enum class AppPromoCampaignKind {
    Welcome,
    AppRating,
    SellerPackage,
    KycVerification,
    /** Admin CMS — title, body, images and actions from API / realtime / FCM. */
    Remote,
}

/**
 * In-app promotional interstitial shown over the main shell (blocking until the user closes it).
 * Copy is resolved from string resources; persistence uses [id] + [version].
 */
data class AppPromoCampaign(
    val id: String,
    val version: Int,
    val kind: AppPromoCampaignKind,
    @StringRes val titleRes: Int? = null,
    @StringRes val messageRes: Int? = null,
    @StringRes val primaryActionRes: Int? = null,
    @StringRes val secondaryActionRes: Int? = null,
    @StringRes val badgeRes: Int? = null,
    val remoteTitle: String? = null,
    val remoteMessage: String? = null,
    val remoteImageUrls: List<String> = emptyList(),
    val remoteBadge: String? = null,
    val remotePrimaryLabel: String? = null,
    val remoteSecondaryLabel: String? = null,
    val primaryAction: AppPromoButtonAction? = null,
    val secondaryAction: AppPromoButtonAction? = null,
    val priority: Int = 0,
    val scheduleType: String? = null,
) {
    val isRemote: Boolean get() = kind == AppPromoCampaignKind.Remote
}

/** Inputs for [AppPromoCampaignResolver] — extend when new gating rules are needed. */
data class AppPromoGateContext(
    val splashFinished: Boolean,
    val isAuthenticated: Boolean,
    val needsOnboarding: Boolean?,
    /** When true, do not compete with chat composer overlays, etc. */
    val blockPromoBecauseOtherUi: Boolean = false,
    val meetingKycReverifyRequired: Boolean = false,
    val identityVerifyUrlAvailable: Boolean = false,
    /** Remote config / admin — enable seller upsell interstitial. */
    val sellerPackagePromoEnabled: Boolean = false,
    val appOpenCount: Int = 0,
)
