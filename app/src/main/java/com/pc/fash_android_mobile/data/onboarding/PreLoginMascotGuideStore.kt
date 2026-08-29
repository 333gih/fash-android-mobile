package com.pc.fash_android_mobile.data.onboarding

import android.content.Context

/** Pre-login mascot guide — independent from welcome intro and post-login feature tour. */
object PreLoginMascotGuideStore {

    private const val PREFS_NAME = "fash_app_prefs"
    private const val KEY_PREFIX = "pre_login_mascot_guide_completed_v"
    const val CURRENT_VERSION: Int = 1

    private fun key(version: Int = CURRENT_VERSION): String = KEY_PREFIX + version

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
