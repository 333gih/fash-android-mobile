package com.pc.fash_android_mobile.data.order

/**
 * Optional `meeting_appointment` on order JSON (snake_case fields from backend).
 * Shown on order detail when the linked conversation has an active meetup proposal.
 */
data class OrderMeetingAppointment(
    val id: String,
    val status: String,
    val locationUrl: String,
    val scheduledAt: String,
    val reminderOffsetMinutes: Int,
    val reminderEnabled: Boolean,
    val reminderSentAt: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    /** From `GET /orders/:id` → `meeting_appointment` after `POST …/check-in` (RFC3339). */
    val buyerCheckInAt: String = "",
    val sellerCheckInAt: String = "",
)

/**
 * Server-driven meetup window / check-in / no-show flags from `GET /orders/:id` (`meeting_grace`).
 * Field names are best-effort; unknown keys are ignored.
 */
data class OrderMeetingGrace(
    val canCheckIn: Boolean = false,
    /** From `meeting_grace.self_checked_in` — viewer already checked in at meetup. */
    val selfCheckedIn: Boolean = false,
    val canReportNoShow: Boolean = false,
    /** Both parties checked in — server may enable SOS / safety affordances (`meeting_grace.sos_unlocked`). */
    val sosUnlocked: Boolean = false,
    val checkInHint: String = "",
    val noShowHint: String = "",
    val buyerCheckedInAt: String = "",
    val sellerCheckedInAt: String = "",
    val phase: String = "",
)

/** Hide check-in when server or appointment timestamps say this party already arrived. */
fun OrderMeetingGrace.viewerShouldShowCheckIn(
    isBuyer: Boolean,
    appointment: OrderMeetingAppointment?,
): Boolean {
    if (!canCheckIn) return false
    if (selfCheckedIn) return false
    val graceSelfAt = if (isBuyer) buyerCheckedInAt else sellerCheckedInAt
    if (graceSelfAt.isNotBlank()) return false
    val appt = appointment ?: return true
    val apptSelfAt = if (isBuyer) appt.buyerCheckInAt else appt.sellerCheckInAt
    return apptSelfAt.isBlank()
}

/** Buyer review on a completed order (`buyer_review` / `BuyerReview` on GET order). */
data class OrderBuyerReview(
    val id: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val createdAt: String = "",
)

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
    /** `online_escrow` | `cash_meetup` | `pending` (awaiting buyer choice). */
    val fulfillmentChannel: String = "",
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
    val buyerReview: OrderBuyerReview? = null,
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
    /** Present when GET order preloads an active chat meetup for this deal. */
    val meetingAppointment: OrderMeetingAppointment? = null,
    /** Check-in / no-show / SOS hints for meetup orders. */
    val meetingGrace: OrderMeetingGrace? = null,
    /**
     * ISO-8601 from `meetup_deadline_at` when meetup-linked payment deadline applies
     * (typically `scheduled_at + 30m` at meetup confirm while still `payment_pending`).
     */
    val meetupDeadlineAt: String = "",
    /** RFC3339 auto-cancel instant from GET order (`order_expires_at`). */
    val orderExpiresAt: String = "",
    /** Seconds until [orderExpiresAt]; 0 when past due. */
    val remainingSeconds: Long = 0L,
    /** `fulfillment_choice` | `payment` | `meetup_payment` */
    val expiryKind: String = "",
    /**
     * Seller-only: `POST /orders/:id/confirm-handoff` available (meetup / in-person handoff after scheduled time).
     */
    val canConfirmHandoff: Boolean = false,
    /** Seller: `POST /orders/:id/acknowledge-offline-cash` when API exposes the action. */
    val canAcknowledgeOfflineCash: Boolean = false,
    /** Dev/staging: `POST /dev/shipment/:id/advance` when mock provider is active. */
    val canAdvanceMockShipment: Boolean = false,
)

/** Product + shipping − discount when [buyerTotalVnd] is not set by the API. */
fun OrderDetail.effectiveBuyerTotal(): Long =
    when {
        buyerTotalVnd > 0L -> buyerTotalVnd
        else -> (amountVnd + shippingFeeVnd - discountVnd).coerceAtLeast(0L)
    }

/**
 * Seller-only: show «Confirm handoff» when the API allows it, or when both parties checked in
 * (`meeting_grace.sos_unlocked`) while the order is still in a meetup handoff state — some backends
 * omit `can_confirm_handoff` for `cash_meetup_open`. Server still validates time/role on POST.
 */
fun sellerConfirmHandoffCtaVisible(
    canConfirmHandoff: Boolean,
    meetupBothPartiesCheckedIn: Boolean,
    orderStatusRaw: String?,
): Boolean {
    if (canConfirmHandoff) return true
    if (!meetupBothPartiesCheckedIn) return false
    val st = orderStatusRaw?.trim()?.lowercase().orEmpty()
    return st == "payment_held" || st == "cash_meetup_open"
}

fun OrderDetail.sellerShowsConfirmHandoffCta(): Boolean {
    val g = meetingGrace
    val bothCheckedIn = when {
        g == null -> false
        g.sosUnlocked -> true
        else -> g.buyerCheckedInAt.isNotBlank() && g.sellerCheckedInAt.isNotBlank()
    }
    return sellerConfirmHandoffCtaVisible(
        canConfirmHandoff = canConfirmHandoff,
        meetupBothPartiesCheckedIn = bothCheckedIn,
        orderStatusRaw = status,
    )
}
