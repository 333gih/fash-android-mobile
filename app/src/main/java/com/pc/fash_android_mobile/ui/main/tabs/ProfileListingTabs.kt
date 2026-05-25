package com.pc.fash_android_mobile.ui.main.tabs

import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingFeedItem

/** Tab indices for the signed-in user's own profile listing grid. */
object ProfileListingTab {
    const val ACTIVE = 0
    const val IN_REVIEW = 1
    const val REJECTED = 2
    const val SOLD = 3
    const val WISHLIST = 4
    const val LAST = WISHLIST
}

enum class ProfileListingTabSet {
    /** Đang bán / Đang duyệt / Từ chối / Đã bán / Đã lưu */
    OwnProfile,
    /** Public seller shop: Đang bán / Đã bán */
    SellerStorefront,
}

fun profileTabLabelResIds(tabSet: ProfileListingTabSet): List<Int> = when (tabSet) {
    ProfileListingTabSet.OwnProfile -> listOf(
        R.string.profile_tab_selling,
        R.string.profile_tab_in_review,
        R.string.profile_tab_rejected,
        R.string.profile_tab_sold,
        R.string.profile_tab_wishlist,
    )
    ProfileListingTabSet.SellerStorefront -> listOf(
        R.string.profile_tab_selling,
        R.string.profile_tab_sold,
    )
}

fun ListingFeedItem.listingStatusNorm(): String =
    listingStatus?.trim()?.lowercase().orEmpty().ifBlank { "active" }

fun ListingFeedItem.isActiveListing(): Boolean = listingStatusNorm() == "active"

fun ListingFeedItem.isInReviewListing(): Boolean = listingStatusNorm() == "in_review"

fun ListingFeedItem.isRejectedListing(): Boolean = listingStatusNorm() == "rejected"

fun ListingFeedItem.isSoldListingStatus(): Boolean = listingStatusNorm() == "sold"
