package com.pc.fash_android_mobile.data.promo

import android.content.Context
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.welcome.WelcomeDialogStore

/**
 * Picks the highest-priority promo that passes [AppPromoGateContext] and has not been dismissed.
 * Add or reorder [providers] to change what users see on app entry.
 */
object AppPromoCampaignResolver {

    private const val WELCOME_ID = WelcomeDialogStore.CAMPAIGN_ID
    private const val WELCOME_VERSION = WelcomeDialogStore.CURRENT_WELCOME_VERSION
    private const val KYC_ID = "kyc_verify"
    private const val KYC_VERSION = 1
    private const val SELLER_PACKAGE_ID = "seller_package"
    private const val SELLER_PACKAGE_VERSION = 1
    private const val APP_RATING_ID = "app_rating"
    private const val APP_RATING_VERSION = 1

    /** Minimum cold opens before the Play Store rating interstitial is eligible. */
    private const val APP_RATING_MIN_OPENS = 3

    fun interface AppPromoCampaignProvider {
        fun candidate(context: AppPromoGateContext, appContext: Context): AppPromoCampaign?
    }

    private val providers: List<AppPromoCampaignProvider> = listOf(
        AppPromoCampaignProvider { ctx, appCtx ->
            if (!ctx.baseEligible()) return@AppPromoCampaignProvider null
            AppPromoPendingQueue.pollHighest()?.let { remote ->
                if (!AppPromoCampaignStore.isDismissed(appCtx, remote)) return@AppPromoCampaignProvider remote
            }
            null
        },
        AppPromoCampaignProvider { ctx, appCtx ->
            if (!ctx.baseEligible()) return@AppPromoCampaignProvider null
            if (AppPromoCampaignStore.isDismissed(appCtx, WELCOME_ID, WELCOME_VERSION)) return@AppPromoCampaignProvider null
            AppPromoCampaign(
                id = WELCOME_ID,
                version = WELCOME_VERSION,
                kind = AppPromoCampaignKind.Welcome,
                titleRes = R.string.welcome_banner_dialog_title,
                messageRes = R.string.welcome_banner_dialog_message,
                primaryActionRes = R.string.welcome_banner_dialog_action,
            )
        },
        AppPromoCampaignProvider { ctx, appCtx ->
            if (!ctx.baseEligible()) return@AppPromoCampaignProvider null
            if (!ctx.meetingKycReverifyRequired || !ctx.identityVerifyUrlAvailable) return@AppPromoCampaignProvider null
            val campaign = AppPromoCampaign(
                id = KYC_ID,
                version = KYC_VERSION,
                kind = AppPromoCampaignKind.KycVerification,
                titleRes = R.string.app_promo_kyc_title,
                messageRes = R.string.app_promo_kyc_message,
                primaryActionRes = R.string.app_promo_kyc_primary,
                secondaryActionRes = R.string.app_promo_secondary_later,
                badgeRes = R.string.app_promo_kyc_badge,
            )
            if (AppPromoCampaignStore.isDismissed(appCtx, campaign)) return@AppPromoCampaignProvider null
            campaign
        },
        AppPromoCampaignProvider { ctx, appCtx ->
            if (!ctx.baseEligible()) return@AppPromoCampaignProvider null
            if (!ctx.sellerPackagePromoEnabled) return@AppPromoCampaignProvider null
            val campaign = AppPromoCampaign(
                id = SELLER_PACKAGE_ID,
                version = SELLER_PACKAGE_VERSION,
                kind = AppPromoCampaignKind.SellerPackage,
                titleRes = R.string.app_promo_seller_package_title,
                messageRes = R.string.app_promo_seller_package_message,
                primaryActionRes = R.string.app_promo_seller_package_primary,
                secondaryActionRes = R.string.app_promo_secondary_later,
                badgeRes = R.string.app_promo_seller_package_badge,
            )
            if (AppPromoCampaignStore.isDismissed(appCtx, campaign)) return@AppPromoCampaignProvider null
            campaign
        },
        AppPromoCampaignProvider { ctx, appCtx ->
            if (!ctx.baseEligible()) return@AppPromoCampaignProvider null
            if (ctx.appOpenCount < APP_RATING_MIN_OPENS) return@AppPromoCampaignProvider null
            val campaign = AppPromoCampaign(
                id = APP_RATING_ID,
                version = APP_RATING_VERSION,
                kind = AppPromoCampaignKind.AppRating,
                titleRes = R.string.app_promo_rating_title,
                messageRes = R.string.app_promo_rating_message,
                primaryActionRes = R.string.app_promo_rating_primary,
                secondaryActionRes = R.string.app_promo_secondary_later,
            )
            if (AppPromoCampaignStore.isDismissed(appCtx, campaign)) return@AppPromoCampaignProvider null
            campaign
        },
    )

    fun resolve(context: AppPromoGateContext, appContext: Context): AppPromoCampaign? {
        for (provider in providers) {
            provider.candidate(context, appContext)?.let { return it }
        }
        return null
    }

    private fun AppPromoGateContext.baseEligible(): Boolean =
        splashFinished &&
            isAuthenticated &&
            !profileSetupBlocksShellChrome() &&
            !blockPromoBecauseOtherUi
}
