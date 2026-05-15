package com.pc.fash_android_mobile.data.onboarding

import android.content.Context

/**
 * One-time guided tour over the main shell (tabs + top actions). Uses the same [PREFS_NAME] as
 * [com.pc.fash_android_mobile.data.welcome.WelcomeDialogStore] so lightweight UX prefs stay together.
 *
 * Bump [CURRENT_TOUR_VERSION] when you want every user to see the tour again (e.g. after a major redesign).
 */
object AppFeatureTourStore {

    private const val PREFS_NAME = "fash_app_prefs"
    private const val KEY_COMPLETED_PREFIX = "app_feature_tour_completed_v"

    /** Increment to replay the tour for all users. */
    const val CURRENT_TOUR_VERSION: Int = 1

    private fun key(): String = KEY_COMPLETED_PREFIX + CURRENT_TOUR_VERSION

    fun isCompletedForCurrentVersion(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(key(), false)

    fun markCompletedForCurrentVersion(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key(), true)
            .apply()
    }
}
