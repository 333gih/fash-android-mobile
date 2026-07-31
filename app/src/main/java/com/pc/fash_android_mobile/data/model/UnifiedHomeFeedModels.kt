package com.pc.fash_android_mobile.data.model

import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnifiedHomeFeedResponse(
    @SerialName("rails") val rails: List<HomeRail>,
    @SerialName("hero") val hero: HeroCard? = null,
    @SerialName("recommendation_meta") val meta: RecommendationMeta? = null
)

@Serializable
data class HomeRail(
    @SerialName("rail_id") val railId: String,
    @SerialName("rail_type") val railType: String,
    @SerialName("title") val title: String,
    @SerialName("subtitle") val subtitle: String? = null,
    @SerialName("items") val items: List<ListingWithMatch>,
    @SerialName("see_all_url") val seeAllUrl: String? = null,
    @SerialName("reason_label") val reasonLabel: String? = null
)

@Serializable
data class ListingWithMatch(
    @SerialName("listing") val listing: ListingFeedItem,
    @SerialName("size_match") val sizeMatch: SizeMatchInfo? = null,
    @SerialName("measurements") val measurements: Map<String, Double>? = null,
    @SerialName("recommend_reason") val recommendReason: String? = null,
    @SerialName("image_aspect_ratio") val imageAspectRatio: String? = null
)

@Serializable
data class SizeMatchInfo(
    @SerialName("badge") val badge: String,
    @SerialName("confidence") val confidence: Double,
    @SerialName("reason") val reason: String
)

@Serializable
data class HeroCard(
    @SerialName("type") val type: String,
    @SerialName("image_url") val imageURL: String? = null,
    @SerialName("title") val title: String,
    @SerialName("subtitle") val subtitle: String? = null,
    @SerialName("cta_url") val ctaURL: String? = null,
    @SerialName("aspect_ratio") val aspectRatio: String
)

// Rail Types
object RailType {
    const val GRID = "grid"
    const val HORIZONTAL_SCROLL = "horizontal_scroll"
    const val HERO = "hero"
}

// Size Match Badges
enum class SizeMatchBadge(val badge: String, val displayText: String, val badgeColor: String) {
    YOUR_SIZE("your_size", "Your Size", "green"),
    CLOSE_FIT("close_fit", "Close Fit", "blue"),
    SIZE_UP("size_up", "Size Up", "orange"),
    SIZE_DOWN("size_down", "Size Down", "orange");

    companion object {
        fun fromBadge(badge: String): SizeMatchBadge? {
            return values().find { it.badge == badge }
        }
    }
}

@Serializable
data class RecommendationMeta(
    @SerialName("active_experiments") val activeExperiments: List<String> = emptyList()
)
