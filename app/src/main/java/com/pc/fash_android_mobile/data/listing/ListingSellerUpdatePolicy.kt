package com.pc.fash_android_mobile.data.listing

import java.util.Locale

/**
 * Core seller `PUT /listings/:id` guard (listing update use case):
 * only [in_review], [rejected], and [active] may be updated by the seller.
 * [inactive] → inactive error; [sold], [reserved], [deleted], … → not available.
 */
fun isListingStatusSellerPutAllowed(status: String?): Boolean {
    val s = status?.trim()?.lowercase(Locale.ROOT).orEmpty()
    if (s.isEmpty()) return true
    return s == "in_review" || s == "rejected" || s == "active"
}
