package com.pc.fash_android_mobile.data.onboarding

import android.content.Context

/**
 * Persists optional onboarding skips per user so skipped steps are not shown again.
 * Cleared on logout via [clearAll].
 */
class OnboardingLocalStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun skippedAestheticTags(userId: String): Boolean =
        prefs.getBoolean(key(KEY_AESTHETIC, userId), false)

    fun setSkippedAestheticTags(userId: String, value: Boolean) {
        prefs.edit().putBoolean(key(KEY_AESTHETIC, userId), value).apply()
    }

    fun skippedSizing(userId: String): Boolean =
        prefs.getBoolean(key(KEY_SIZING, userId), false)

    fun setSkippedSizing(userId: String, value: Boolean) {
        prefs.edit().putBoolean(key(KEY_SIZING, userId), value).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun key(suffix: String, userId: String): String {
        val id = userId.ifBlank { "anonymous" }
        return "${suffix}_$id"
    }

    private companion object {
        const val PREFS_NAME = "onboarding_flow"
        const val KEY_AESTHETIC = "skipped_aesthetic_tags"
        const val KEY_SIZING = "skipped_sizing"
    }
}
