package com.pc.fash_android_mobile.ui.notifications

import java.util.Locale

/** Parsed from FCM / inbox `data` → Explore overlay filters (mirrors backend exploreNavInput). */
data class ExploreNavigationFilter(
    val surface: String? = null,
    val seasonKey: String? = null,
    val seasonLabel: String? = null,
    val categoryId: String? = null,
    val brandId: String? = null,
    val aestheticTagId: String? = null,
    val searchQuery: String? = null,
) {
    fun hasStructuredFilter(): Boolean =
        !surface.isNullOrBlank() ||
            !seasonKey.isNullOrBlank() ||
            !categoryId.isNullOrBlank() ||
            !brandId.isNullOrBlank() ||
            !aestheticTagId.isNullOrBlank() ||
            !searchQuery.isNullOrBlank()
}

object NotificationExploreNavigation {

    private val exploreNavTargets = setOf("explore", "explore_tab")

    fun parseFromNotificationData(data: Map<String, Any?>?): ExploreNavigationFilter? {
        if (data.isNullOrEmpty()) return null
        val nav = firstStringCi(data, "nav_target", "navTarget")?.lowercase(Locale.ROOT).orEmpty()
        val feedSurface = firstStringCi(data, "feed_surface", "feedSurface")
        val seasonKey = firstStringCi(data, "season_key", "seasonKey")
        val seasonLabel = firstStringCi(data, "season_label", "seasonLabel")
        val categoryId = firstStringCi(data, "category_id", "categoryId")
        val brandId = firstStringCi(data, "brand_id", "brandId")
        val aestheticTagId = firstStringCi(data, "aesthetic_tag_id", "aestheticTagId")
        val searchQuery = firstStringCi(data, "search_query", "searchQuery")

        val exploreIntent = isExplorePrimaryIntent(data)
        if (!exploreIntent && feedSurface.isNullOrBlank() && seasonKey.isNullOrBlank()) {
            return null
        }

        val surface = when {
            !feedSurface.isNullOrBlank() -> feedSurface
            nav == "home" && !seasonKey.isNullOrBlank() -> "seasonal_near_you"
            exploreIntent -> "explore"
            else -> null
        }

        val filter = ExploreNavigationFilter(
            surface = surface,
            seasonKey = seasonKey,
            seasonLabel = seasonLabel,
            categoryId = categoryId,
            brandId = brandId,
            aestheticTagId = aestheticTagId,
            searchQuery = searchQuery,
        )
        return filter.takeIf { it.hasStructuredFilter() || exploreIntent }
    }

    fun parseFromStringMap(data: Map<String, String>?): ExploreNavigationFilter? {
        if (data.isNullOrEmpty()) return null
        val anyMap = data.mapValues { it.value as Any? }
        return parseFromNotificationData(anyMap)
    }

    fun isExplorePrimaryIntent(data: Map<String, Any?>?): Boolean {
        if (data.isNullOrEmpty()) return false
        val nav = firstStringCi(data, "nav_target", "navTarget")?.lowercase(Locale.ROOT).orEmpty()
        if (nav in exploreNavTargets) return true
        val feedSurface = firstStringCi(data, "feed_surface", "feedSurface").orEmpty()
        if (nav == "home" && feedSurface.equals("seasonal_near_you", ignoreCase = true)) return true
        if (!feedSurface.isNullOrBlank() && nav in exploreNavTargets) return true
        val ptype = firstStringCi(data, "type", "payload_type", "payloadType").orEmpty()
        if (ptype.contains("season_shift", ignoreCase = true)) return true
        if (ptype.contains("community_quiet", ignoreCase = true)) return true
        if (ptype.contains("style_drought", ignoreCase = true)) return true
        if (ptype.contains("style_fresh", ignoreCase = true)) return true
        if (ptype.contains("hunt_today", ignoreCase = true)) return true
        return false
    }

    /** Guest local reminder payload keys. */
    const val GUEST_ACTION_KEY = "fash_guest_action"
    const val GUEST_EXPLORE_SURFACE_KEY = "fash_explore_surface"
    const val GUEST_EXPLORE_SEASON_LABEL_KEY = "fash_explore_season_label"
    const val GUEST_ACTION_OPEN_EXPLORE = "open_explore"
    const val GUEST_ACTION_OPEN_HOME_SIGNUP = "open_home_signup"

    fun parseFromGuestPayload(data: Map<String, String>): ExploreNavigationFilter? {
        val action = data[GUEST_ACTION_KEY]?.trim().orEmpty()
        if (action != GUEST_ACTION_OPEN_EXPLORE) return null
        return ExploreNavigationFilter(
            surface = data[GUEST_EXPLORE_SURFACE_KEY]?.trim()?.takeIf { it.isNotEmpty() } ?: "explore",
            seasonLabel = data[GUEST_EXPLORE_SEASON_LABEL_KEY]?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    private fun firstStringCi(data: Map<String, Any?>, vararg keys: String): String? {
        val byLower = data.entries.associate { it.key.lowercase(Locale.ROOT) to it.value }
        for (k in keys) {
            val v = byLower[k.lowercase(Locale.ROOT)] ?: continue
            val s = when (v) {
                is String -> v.trim()
                is Number -> v.toString()
                else -> v?.toString()?.trim().orEmpty()
            }
            if (s.isNotEmpty()) return s
        }
        return null
    }
}
