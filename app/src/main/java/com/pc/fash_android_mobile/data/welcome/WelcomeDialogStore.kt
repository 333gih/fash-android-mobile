package com.pc.fash_android_mobile.data.welcome

import android.content.Context
import com.pc.fash_android_mobile.data.promo.AppPromoCampaignStore

/**
 * Welcome interstitial — backed by [com.pc.fash_android_mobile.data.promo.AppPromoCampaignStore].
 * The post-login feature tour runs after this campaign is dismissed.
 */
object WelcomeDialogStore {

    const val CAMPAIGN_ID = "welcome"

    /** Bump when marketing wants the welcome interstitial to show again for everyone. */
    const val CURRENT_WELCOME_VERSION: Int = 2

    fun isDismissedForCurrentVersion(context: Context): Boolean =
        AppPromoCampaignStore.isDismissed(context, CAMPAIGN_ID, CURRENT_WELCOME_VERSION)

    fun markDismissedForCurrentVersion(context: Context) {
        AppPromoCampaignStore.markDismissed(context, CAMPAIGN_ID, CURRENT_WELCOME_VERSION)
    }
}
