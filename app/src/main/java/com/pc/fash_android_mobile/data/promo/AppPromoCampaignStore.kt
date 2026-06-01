package com.pc.fash_android_mobile.data.promo

import android.content.Context

/**
 * Persists dismissals, show counts, and lightweight session counters for app-open promos.
 * Shares [PREFS_NAME] with other UX prefs ([com.pc.fash_android_mobile.data.locale.AppLocale]).
 */
object AppPromoCampaignStore {

    private const val PREFS_NAME = "fash_app_prefs"
    private const val KEY_DISMISSED_PREFIX = "app_promo_dismissed_"
    private const val KEY_SHOW_COUNT_PREFIX = "app_promo_shows_"
    private const val KEY_LAST_SHOWN_PREFIX = "app_promo_last_shown_"
    private const val KEY_APP_OPEN_COUNT = "app_promo_app_open_count"
    /** Legacy key from the first [WelcomeDialogStore] implementation. */
    private const val LEGACY_WELCOME_DISMISSED_PREFIX = "welcome_center_banner_dismissed_v"

    private fun dismissedKey(campaignId: String, version: Int): String =
        "${KEY_DISMISSED_PREFIX}${campaignId}_v$version"

    private fun showCountKey(campaignId: String, version: Int): String =
        "${KEY_SHOW_COUNT_PREFIX}${campaignId}_v$version"

    private fun lastShownKey(campaignId: String, version: Int): String =
        "${KEY_LAST_SHOWN_PREFIX}${campaignId}_v$version"

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

    fun canShow(context: Context, campaign: AppPromoCampaign): Boolean {
        if (isDismissed(context, campaign)) return false
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val shows = prefs.getInt(showCountKey(campaign.id, campaign.version), 0)
        val max = campaign.maxShowsPerUser
        if (max != null && max > 0 && shows >= max) return false
        val hours = campaign.cooldownHours
        if (hours != null && hours > 0) {
            val lastMs = prefs.getLong(lastShownKey(campaign.id, campaign.version), 0L)
            if (lastMs > 0L) {
                val elapsedHours = (System.currentTimeMillis() - lastMs) / 3_600_000.0
                if (elapsedHours < hours) return false
            }
        }
        return true
    }

    fun recordShow(context: Context, campaign: AppPromoCampaign) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = showCountKey(campaign.id, campaign.version)
        prefs.edit()
            .putInt(key, prefs.getInt(key, 0) + 1)
            .putLong(lastShownKey(campaign.id, campaign.version), System.currentTimeMillis())
            .apply()
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
