package com.pc.fash_android_mobile.data.order

/**
 * Full order payload from `GET /api/v1/orders/{order_id}` (PascalCase or snake_case).
 */
data class OrderDetail(
    val orderId: String,
    val listingId: String,
    val buyerUserId: String,
    val sellerUserId: String,
    val amountVnd: Long,
    val platformFeeVnd: Long,
    val sellerPayoutVnd: Long,
    val status: String,
    val trackingNumber: String,
    val carrier: String,
    val listingTitle: String,
    val listingImageUrl: String,
    val listingPriceVnd: Long,
    val listingStatus: String,
    val buyerUsername: String,
    val buyerDisplayName: String,
    val buyerAvatarUrl: String,
    val sellerUsername: String,
    val sellerDisplayName: String,
    val sellerAvatarUrl: String,
    val canConfirm: Boolean,
    val canReview: Boolean,
)
