package com.pc.fash_android_mobile.ui.post

import java.util.Locale

/**
 * Condition values shared by create ([CreateListingPostStep5]) and edit listing flows.
 * The API may return snake_case; UI uses the same labels as the post wizard.
 */
object ListingConditionOptions {
    val uiValues: List<String> = listOf("New", "Like new", "Good", "Fair", "Worn")

    fun normalizeApiToUi(raw: String): String {
        val t = raw.trim()
        if (t.isEmpty()) return ""
        val compact = t.lowercase(Locale.ROOT).replace(" ", "_").replace("-", "_")
        return when (compact) {
            "new" -> "New"
            "like_new" -> "Like new"
            "good" -> "Good"
            "fair" -> "Fair"
            "worn" -> "Worn"
            else -> uiValues.firstOrNull { it.equals(t, ignoreCase = true) } ?: t
        }
    }

    fun normalizeUiToApi(ui: String): String {
        val t = ui.trim()
        return when (t) {
            "New" -> "new"
            "Like new" -> "like_new"
            "Good" -> "good"
            "Fair" -> "fair"
            "Worn" -> "worn"
            else -> t.lowercase(Locale.ROOT).replace(" ", "_").replace("-", "_")
        }
    }

    /** Canonical API-style slug for comparing listing detail vs form regardless of format. */
    fun canonicalApi(raw: String): String = normalizeUiToApi(normalizeApiToUi(raw.trim()))
}
