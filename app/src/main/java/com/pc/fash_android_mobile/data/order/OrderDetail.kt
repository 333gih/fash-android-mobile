package com.pc.fash_android_mobile.data.order

/**
 * Full order payload from `GET /api/v1/orders/{order_id}` (PascalCase or snake_case).
 * Extra fields are optional in JSON; the app maps all known order states and shows sensible UI when absent.
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
    /** Shipping / checkout — buyer-facing. */
    val shippingFeeVnd: Long = 0L,
    /** Promotion / coupon (positive number = amount off). */
    val discountVnd: Long = 0L,
    /** Total the buyer paid or will pay; if 0, UI uses [effectiveBuyerTotal]. */
    val buyerTotalVnd: Long = 0L,
    val recipientName: String = "",
    val recipientPhone: String = "",
    /** Single formatted line or multiline shipping address. */
    val shippingAddressFormatted: String = "",
    /** ISO-8601 timestamps from API when present. */
    val createdAt: String = "",
    val paidAt: String = "",
    val shippedAt: String = "",
    val deliveredAt: String = "",
    val cancelledAt: String = "",
    val expectedDeliveryAt: String = "",
    /** Seller ship-by deadline (e.g. avoid auto-cancel). */
    val shipByAt: String = "",
    val escrowReleaseAt: String = "",
    val disputeSummary: String = "",
    /** e.g. "Size M · Beige" from listing metadata. */
    val listingVariantLabel: String = "",
    /** Opens chat with counterparty when set. */
    val conversationId: String = "",
    /** Last-mile status line (carrier scan), if API provides it. */
    val trackingStatusSummary: String = "",
    /** Seller may ship (payment held, etc.) — also inferred when API omits the flag. */
    val canShip: Boolean = false,
)

/** Product + shipping − discount when [buyerTotalVnd] is not set by the API. */
fun OrderDetail.effectiveBuyerTotal(): Long =
    when {
        buyerTotalVnd > 0L -> buyerTotalVnd
        else -> (amountVnd + shippingFeeVnd - discountVnd).coerceAtLeast(0L)
    }
