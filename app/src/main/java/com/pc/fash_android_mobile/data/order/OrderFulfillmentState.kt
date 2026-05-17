package com.pc.fash_android_mobile.data.order

import com.pc.fash_android_mobile.ui.orders.normalizeOrderStatus

/** Buyer must pick meetup vs online before channel-specific UI. */
fun isAwaitingFulfillmentChoice(status: String?, fulfillmentChannel: String? = null): Boolean {
    val st = normalizeOrderStatus(status.orEmpty())
    if (st == "fulfillment_pending") return true
    val ch = fulfillmentChannel?.trim()?.lowercase().orEmpty()
    return ch == "pending" || ch == "unchosen"
}
