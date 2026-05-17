package com.pc.fash_android_mobile.data.order

import androidx.annotation.StringRes
import com.pc.fash_android_mobile.R

/** Must match [entities.ValidBuyerCancelReasonCodes] in core-service. */
data class OrderCancelReasonOption(
    val code: String,
    @StringRes val labelRes: Int,
    val requiresNote: Boolean = false,
)

object OrderCancelReasons {
    val options: List<OrderCancelReasonOption> = listOf(
        OrderCancelReasonOption("changed_mind", R.string.order_cancel_reason_changed_mind),
        OrderCancelReasonOption("found_elsewhere", R.string.order_cancel_reason_found_elsewhere),
        OrderCancelReasonOption("price_concern", R.string.order_cancel_reason_price_concern),
        OrderCancelReasonOption("shipping_not_suitable", R.string.order_cancel_reason_shipping_not_suitable),
        OrderCancelReasonOption("meetup_not_possible", R.string.order_cancel_reason_meetup_not_possible),
        OrderCancelReasonOption("seller_slow", R.string.order_cancel_reason_seller_slow),
        OrderCancelReasonOption("payment_problem", R.string.order_cancel_reason_payment_problem),
        OrderCancelReasonOption("other", R.string.order_cancel_reason_other, requiresNote = true),
    )

    fun labelResForCode(code: String): Int? =
        options.find { it.code == code.trim().lowercase() }?.labelRes
}
