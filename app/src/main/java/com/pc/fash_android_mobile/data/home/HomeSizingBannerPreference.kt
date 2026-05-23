package com.pc.fash_android_mobile.data.home

import android.content.Context

/**
 * Tracks whether the user explicitly dismissed the Home "Add your size" banner.
 *
 * The banner re-appears whenever the user clears the dismiss flag (e.g. after they save a size and
 * later clear it again, or after a sign-out → sign-in cycle that wipes this pref). Persisted in the
 * shared `fash_app_prefs` SharedPreferences file.
 */
object HomeSizingBannerPreference {

    private const val PREFS_NAME = "fash_app_prefs"
    private const val KEY_DISMISSED = "home_sizing_banner_dismissed"

    fun isDismissed(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DISMISSED, false)

    fun markDismissed(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DISMISSED, true)
            .apply()
    }

    fun reset(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_DISMISSED)
            .apply()
    }
}
