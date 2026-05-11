package com.pc.fash_android_mobile.data.welcome

import android.content.Context

/**
 * One-time center welcome dialog after the user reaches home (onboarding complete).
 * Uses the same [PREFS_NAME] as [com.pc.fash_android_mobile.data.locale.AppLocale] so all lightweight UX prefs stay in one file.
 */
object WelcomeDialogStore {

    private const val PREFS_NAME = "fash_app_prefs"
    private const val KEY_CENTER_WELCOME_DISMISSED_PREFIX = "welcome_center_banner_dismissed_v"

    /** Bump this string suffix when marketing wants to show the welcome again for everyone. */
    const val CURRENT_WELCOME_VERSION: Int = 1

    private fun key(): String = KEY_CENTER_WELCOME_DISMISSED_PREFIX + CURRENT_WELCOME_VERSION

    fun isDismissedForCurrentVersion(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(key(), false)

    fun markDismissedForCurrentVersion(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key(), true)
            .apply()
    }
}
