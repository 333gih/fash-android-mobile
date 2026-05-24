package com.pc.fash_android_mobile.data.explore

import android.content.Context

/** How Explore applies seller ship-from area filtering. */
enum class BrowseLocationMode {
    Off,
    /** Filter by the viewer's default shipping address province/district. */
    NearbyDefault,
    /** Filter by a manually chosen province/district (saved on profile when signed in). */
    Manual,
}

/**
 * Persists the Explore "Khu vực" quick toggle mode in shared app prefs (same file as sizing).
 */
object ExploreBrowseLocationPreference {

    private const val PREFS_NAME = "fash_app_prefs"
    private const val KEY_MODE = "explore_browse_location_mode"

    fun read(context: Context): BrowseLocationMode {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MODE, null)
            ?: return BrowseLocationMode.Off
        return when (raw.lowercase()) {
            "nearby_default" -> BrowseLocationMode.NearbyDefault
            "manual" -> BrowseLocationMode.Manual
            else -> BrowseLocationMode.Off
        }
    }

    fun write(context: Context, mode: BrowseLocationMode) {
        val stored = when (mode) {
            BrowseLocationMode.Off -> "off"
            BrowseLocationMode.NearbyDefault -> "nearby_default"
            BrowseLocationMode.Manual -> "manual"
        }
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, stored)
            .apply()
    }
}
