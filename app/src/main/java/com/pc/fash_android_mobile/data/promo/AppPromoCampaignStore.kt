package com.pc.fash_android_mobile.data.promo

import android.content.Context

/**
 * Persists dismissals and lightweight session counters for app-open promos.
 * Shares [PREFS_NAME] with other UX prefs ([com.pc.fash_android_mobile.data.locale.AppLocale]).
 */
object AppPromoCampaignStore {

    private const val PREFS_NAME = "fash_app_prefs"
    private const val KEY_DISMISSED_PREFIX = "app_promo_dismissed_"
    private const val KEY_APP_OPEN_COUNT = "app_promo_app_open_count"
    /** Legacy key from the first [WelcomeDialogStore] implementation. */
    private const val LEGACY_WELCOME_DISMISSED_PREFIX = "welcome_center_banner_dismissed_v"

    private fun dismissedKey(campaignId: String, version: Int): String =
        "${KEY_DISMISSED_PREFIX}${campaignId}_v$version"

    fun isDismissed(context: Context, campaign: AppPromoCampaign): Boolean =
        isDismissed(context, campaign.id, campaign.version)

    fun isDismissed(context: Context, campaignId: String, version: Int): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (campaignId == "welcome" &&
            prefs.getBoolean(LEGACY_WELCOME_DISMISSED_PREFIX + version, false)
        ) {
            return true
        }
        return prefs.getBoolean(dismissedKey(campaignId, version), false)
    }

    fun markDismissed(context: Context, campaign: AppPromoCampaign) {
        markDismissed(context, campaign.id, campaign.version)
    }

    fun markDismissed(context: Context, campaignId: String, version: Int) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(dismissedKey(campaignId, version), true)
            .apply()
    }

    fun incrementAppOpenCount(context: Context): Int {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val next = prefs.getInt(KEY_APP_OPEN_COUNT, 0) + 1
        prefs.edit().putInt(KEY_APP_OPEN_COUNT, next).apply()
        return next
    }

    fun readAppOpenCount(context: Context): Int =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_APP_OPEN_COUNT, 0)
}
