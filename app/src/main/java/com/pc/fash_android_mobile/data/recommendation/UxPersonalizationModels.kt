package com.pc.fash_android_mobile.data.recommendation

/** Explore shortcut from GET /recommendations/ux-personalization. */
data class HomeExploreShortcut(
    val labelKey: String = "",
    val aestheticTagId: String? = null,
    val aestheticTagName: String? = null,
    val categoryId: String? = null,
    val brandId: String? = null,
)

/** Home tab personalization from server + local signals. */
data class HomeUxPersonalization(
    val defaultTabKey: String = "hunt_today",
    val tabOrder: List<String> = emptyList(),
    val prefetchTabs: List<String> = emptyList(),
    val sectionLimits: Map<String, Int> = emptyMap(),
    val exploreShortcut: HomeExploreShortcut? = null,
)

/** Profile tab personalization. */
data class ProfileUxPersonalization(
    val defaultTabKey: String = "selling",
    val tabOrderKeys: List<String> = emptyList(),
    val primaryMode: String = "balanced",
)

data class UxPersonalizationBundle(
    val home: HomeUxPersonalization = HomeUxPersonalization(),
    val profile: ProfileUxPersonalization = ProfileUxPersonalization(),
)

data class UxEventPayload(
    val scope: String,
    val tabKey: String,
    val clientHour: Int? = null,
    val dwellMs: Int? = null,
)

object HomeFeedTabKeys {
    const val HUNT_TODAY = "hunt_today"
    const val FOR_YOU = "for_you"
    const val FOLLOWING = "following"
    const val STYLE_PICKS = "style_picks"
    const val SIMILAR_SAVED = "similar_saved"
}

object ProfileTabKeys {
    const val SELLING = "selling"
    const val IN_REVIEW = "in_review"
    const val REJECTED = "rejected"
    const val SOLD = "sold"
    const val WISHLIST = "wishlist"
}
