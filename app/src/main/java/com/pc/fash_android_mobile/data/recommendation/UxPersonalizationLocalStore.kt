package com.pc.fash_android_mobile.data.recommendation

import android.content.Context
import java.util.Calendar

/**
 * Local fallback for tab defaults when offline or before server personalization loads.
 * Merged with server hints on sync.
 */
object UxPersonalizationLocalStore {
    private const val PREFS = "ux_personalization_local"

    fun readHomeDefaultTab(context: Context, userId: String?): String? {
        val uid = userId?.trim()?.lowercase().orEmpty()
        if (uid.isEmpty()) return null
        return prefs(context).getString(homeDefaultKey(uid), null)
    }

    fun writeHomeDefaultTab(context: Context, userId: String?, tabKey: String) {
        val uid = userId?.trim()?.lowercase().orEmpty()
        if (uid.isEmpty() || tabKey.isBlank()) return
        prefs(context).edit().putString(homeDefaultKey(uid), tabKey).apply()
    }

    fun readProfileDefaultTab(context: Context, userId: String?): String? {
        val uid = userId?.trim()?.lowercase().orEmpty()
        if (uid.isEmpty()) return null
        return prefs(context).getString(profileDefaultKey(uid), null)
    }

    fun writeProfileDefaultTab(context: Context, userId: String?, tabKey: String) {
        val uid = userId?.trim()?.lowercase().orEmpty()
        if (uid.isEmpty() || tabKey.isBlank()) return
        prefs(context).edit().putString(profileDefaultKey(uid), tabKey).apply()
    }

    fun clearForUser(context: Context, userId: String?) {
        val uid = userId?.trim()?.lowercase().orEmpty()
        if (uid.isEmpty()) return
        prefs(context).edit()
            .remove(homeDefaultKey(uid))
            .remove(profileDefaultKey(uid))
            .apply()
    }

    fun currentClientHour(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun homeDefaultKey(userId: String) = "home_default_tab_$userId"
    private fun profileDefaultKey(userId: String) = "profile_default_tab_$userId"
}
