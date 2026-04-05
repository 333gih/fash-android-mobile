package com.pc.fash_android_mobile.data.order

/**
 * `GET /api/v1/orders/pending-payment` — buyer orders awaiting payment.
 */
data class PendingPaymentApiResponse(
    val paymentWindowMinutes: Int,
    val orders: List<PendingPaymentOrderDto>,
)

data class PendingPaymentOrderDto(
    val orderId: String,
    val listingId: String,
    val amountVnd: Long,
    val status: String,
    val createdAt: String,
    val paymentDeadlineAt: String,
    val remainingSeconds: Int,
    val expired: Boolean,
    val listingTitle: String,
    val coverImageUrl: String,
)
