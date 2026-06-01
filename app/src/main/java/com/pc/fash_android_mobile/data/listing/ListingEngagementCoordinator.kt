package com.pc.fash_android_mobile.data.listing

import java.util.concurrent.ConcurrentHashMap

/** Prevents overlapping like/save API calls for the same listing (rapid taps). */
object ListingEngagementCoordinator {
    private val saveInFlight = ConcurrentHashMap.newKeySet<String>()
    private val likeInFlight = ConcurrentHashMap.newKeySet<String>()

    fun beginSaveToggle(listingId: String): Boolean = saveInFlight.add(listingId)

    fun endSaveToggle(listingId: String) {
        saveInFlight.remove(listingId)
    }

    fun beginLikeToggle(listingId: String): Boolean = likeInFlight.add(listingId)

    fun endLikeToggle(listingId: String) {
        likeInFlight.remove(listingId)
    }
}
