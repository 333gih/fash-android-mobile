package com.pc.fash_android_mobile.data.order

/**
 * Aligns with core-service [CancelByBuyer]: buyer may cancel before payment / before cash meetup completes.
 * Listing returns to `active`; conversation `order_id` is cleared (see order payment expiry use case).
 */
object OrderBuyerCancelPolicy {

    private val BUYER_CANCELLABLE_STATUSES = setOf(
        "fulfillment_pending",
        "payment_pending",
        "cash_meetup_open",
    )

    fun buyerCanCancel(status: String?): Boolean {
        val norm = status?.trim()?.lowercase().orEmpty()
        return norm in BUYER_CANCELLABLE_STATUSES
    }
}
