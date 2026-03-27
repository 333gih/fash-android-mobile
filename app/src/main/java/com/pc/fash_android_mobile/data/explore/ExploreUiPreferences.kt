package com.pc.fash_android_mobile.data.explore

import android.app.Application
import android.content.Context
import androidx.core.content.edit

/**
 * Persists Explore screen UI choices (filter strip visibility). Not tied to login session.
 */
class ExploreUiPreferences(
    application: Application,
) {
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** When true, category / style / featured-seller strips are shown. */
    fun readFiltersExpanded(): Boolean =
        prefs.getBoolean(KEY_FILTERS_EXPANDED, DEFAULT_FILTERS_EXPANDED)

    fun writeFiltersExpanded(expanded: Boolean) {
        prefs.edit { putBoolean(KEY_FILTERS_EXPANDED, expanded) }
    }

    private companion object {
        private const val PREFS_NAME = "fash_explore_ui"
        private const val KEY_FILTERS_EXPANDED = "filters_expanded"
        private const val DEFAULT_FILTERS_EXPANDED = true
    }
}
