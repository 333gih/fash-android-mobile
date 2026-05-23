package com.pc.fash_android_mobile.data.explore

import android.content.Context

/**
 * Persists the Explore screen "Match my size" sticky toggle.
 *
 * Stored in the shared SharedPreferences file (`fash_app_prefs`) — same as theme + locale —
 * so we don't fragment per-user prefs across many files. The value is one of:
 *  - `"all"` (default)
 *  - `"match_profile"` — backend Typesense sizing filter on Explore/Home rails.
 *
 * Reads are synchronous (called from ViewModel init) and writes are best-effort (`apply()` so the
 * caller doesn't block on disk).
 */
object ExploreSizingPreference {

    private const val PREFS_NAME = "fash_app_prefs"
    private const val KEY_SIZING_MODE = "explore_sizing_mode"

    const val MODE_ALL: String = "all"
    const val MODE_MATCH_PROFILE: String = "match_profile"

    fun read(context: Context): String {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SIZING_MODE, null)
            ?: return MODE_ALL
        return if (raw.equals(MODE_MATCH_PROFILE, ignoreCase = true)) MODE_MATCH_PROFILE else MODE_ALL
    }

    fun write(context: Context, mode: String) {
        val normalized = if (mode.equals(MODE_MATCH_PROFILE, ignoreCase = true)) MODE_MATCH_PROFILE else MODE_ALL
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SIZING_MODE, normalized)
            .apply()
    }
}
