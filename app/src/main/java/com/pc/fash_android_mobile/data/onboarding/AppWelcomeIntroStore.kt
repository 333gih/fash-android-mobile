package com.pc.fash_android_mobile.data.onboarding

import android.content.Context

/**
 * First-install welcome slide deck — shown once before login/guest shell.
 * Uses [AppFeatureTourStore.PREFS_NAME] pattern via `fash_app_prefs`.
 */
object AppWelcomeIntroStore {

    private const val PREFS_NAME = "fash_app_prefs"
    private const val KEY_COMPLETED_PREFIX = "app_welcome_intro_completed_v"

    /** Bump to replay intro for all users. Keep in sync with iOS [AppWelcomeIntroStore]. */
    const val CURRENT_VERSION: Int = 1

    private fun key(version: Int = CURRENT_VERSION): String = KEY_COMPLETED_PREFIX + version

    fun isCompleted(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        for (version in 1..CURRENT_VERSION) {
            if (prefs.getBoolean(key(version), false)) return true
        }
        return false
    }

    fun markCompleted(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key(), true)
            .commit()
    }
}
