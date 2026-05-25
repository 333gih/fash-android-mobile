package com.pc.fash_android_mobile.data.recommendation

import com.pc.fash_android_mobile.ui.home.HomeFeedTab
import com.pc.fash_android_mobile.ui.main.tabs.ProfileListingTab

fun HomeFeedTab.toUxTabKey(): String = when (this) {
    HomeFeedTab.HuntToday -> HomeFeedTabKeys.HUNT_TODAY
    HomeFeedTab.ForYou -> HomeFeedTabKeys.FOR_YOU
    HomeFeedTab.Following -> HomeFeedTabKeys.FOLLOWING
    HomeFeedTab.StylePicks -> HomeFeedTabKeys.STYLE_PICKS
    HomeFeedTab.SimilarSaved -> HomeFeedTabKeys.SIMILAR_SAVED
}

fun homeFeedTabFromKey(key: String): HomeFeedTab? = when (key.trim().lowercase()) {
    HomeFeedTabKeys.HUNT_TODAY -> HomeFeedTab.HuntToday
    HomeFeedTabKeys.FOR_YOU -> HomeFeedTab.ForYou
    HomeFeedTabKeys.FOLLOWING -> HomeFeedTab.Following
    HomeFeedTabKeys.STYLE_PICKS -> HomeFeedTab.StylePicks
    HomeFeedTabKeys.SIMILAR_SAVED -> HomeFeedTab.SimilarSaved
    else -> null
}

fun profileTabIndexFromKey(key: String): Int? = when (key.trim().lowercase()) {
    ProfileTabKeys.SELLING -> ProfileListingTab.ACTIVE
    ProfileTabKeys.IN_REVIEW -> ProfileListingTab.IN_REVIEW
    ProfileTabKeys.REJECTED -> ProfileListingTab.REJECTED
    ProfileTabKeys.SOLD -> ProfileListingTab.SOLD
    ProfileTabKeys.WISHLIST -> ProfileListingTab.WISHLIST
    else -> null
}

fun profileTabKeyFromIndex(index: Int): String = when (index) {
    ProfileListingTab.IN_REVIEW -> ProfileTabKeys.IN_REVIEW
    ProfileListingTab.REJECTED -> ProfileTabKeys.REJECTED
    ProfileListingTab.SOLD -> ProfileTabKeys.SOLD
    ProfileListingTab.WISHLIST -> ProfileTabKeys.WISHLIST
    else -> ProfileTabKeys.SELLING
}

fun orderedHomeFeedTabs(
    isGuestBrowse: Boolean,
    tabOrderKeys: List<String>,
): List<HomeFeedTab> {
    val allowed = HomeFeedTab.tabsFor(isGuestBrowse)
    if (tabOrderKeys.isEmpty()) return allowed
    val ordered = tabOrderKeys.mapNotNull { homeFeedTabFromKey(it) }.filter { it in allowed }
    return (ordered + allowed.filter { it !in ordered }).distinct()
}

fun orderedProfileTabIndices(tabOrderKeys: List<String>): List<Int> {
    val default = listOf(
        ProfileListingTab.ACTIVE,
        ProfileListingTab.IN_REVIEW,
        ProfileListingTab.REJECTED,
        ProfileListingTab.SOLD,
        ProfileListingTab.WISHLIST,
    )
    if (tabOrderKeys.isEmpty()) return default
    val ordered = tabOrderKeys.mapNotNull { profileTabIndexFromKey(it) }
    return (ordered + default.filter { it !in ordered }).distinct()
}
