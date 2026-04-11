package com.pc.fash_android_mobile.ui.chat

import com.pc.fash_android_mobile.data.chat.ChatMessage
import java.time.Instant

/** Parses RFC3339-ish timestamps from chat/order payloads. */
internal fun parseChatInstant(raw: String): Instant? {
    val t = raw.trim()
    if (t.isEmpty()) return null
    return try {
        val normalized = when {
            t.contains("T") -> t
            t.contains(" ") -> t.replace(" ", "T")
            else -> "${t}T00:00:00Z"
        }
        Instant.parse(normalized)
    } catch (_: Exception) {
        null
    }
}

/**
 * A meetup row still blocks scheduling another when the server marks it pending/confirmed
 * and [scheduledAt] is still in the future (or now). Past slots are treated as expired so
 * the deal banner can offer "Schedule meetup" again.
 */
internal fun isMeetupSlotStillUpcoming(
    status: String,
    scheduledAtRaw: String,
    now: Instant = Instant.now(),
): Boolean {
    val st = status.trim().lowercase()
    if (st != "pending" && st != "confirmed") return false
    val at = parseChatInstant(scheduledAtRaw) ?: return true
    return !at.isBefore(now)
}

/**
 * True when the linked order or any chat [meeting_proposal] row has an active upcoming meetup.
 * Cancelled orders ignore the order payload but still respect future chat proposals if any.
 */
internal fun hasActiveFutureMeetupBlockingReschedule(
    orderStatus: String?,
    orderApptStatus: String?,
    orderApptScheduledAt: String?,
    messages: List<ChatMessage>,
): Boolean {
    val ord = orderStatus?.trim()?.lowercase().orEmpty()
    if (ord != "cancelled") {
        val oSt = orderApptStatus?.trim().orEmpty()
        val oAt = orderApptScheduledAt?.trim().orEmpty()
        if (oSt.isNotEmpty() && oAt.isNotEmpty() && isMeetupSlotStillUpcoming(oSt, oAt)) {
            return true
        }
    }
    return messages.any { msg ->
        msg.messageType.equals("meeting_proposal", ignoreCase = true) &&
            msg.meetingAppointment?.let { ap ->
                isMeetupSlotStillUpcoming(ap.status, ap.scheduledAt)
            } == true
    }
}

/**
 * Buyer sent at least one initial [offer] and the seller sent at least one [counter_offer]
 * in this thread (from the current viewer’s perspective).
 */
internal fun conversationHadBuyerOfferAndSellerCounter(
    messages: List<ChatMessage>,
    viewerIsBuyer: Boolean,
): Boolean {
    val hasBuyerOffer = messages.any { m ->
        m.messageType == "offer" &&
            if (viewerIsBuyer) m.isFromMe else !m.isFromMe
    }
    val hasSellerCounter = messages.any { m ->
        m.messageType.equals("counter_offer", ignoreCase = true) &&
            if (viewerIsBuyer) !m.isFromMe else m.isFromMe
    }
    return hasBuyerOffer && hasSellerCounter
}

/**
 * After both sides have proposed prices, block **new** buyer offers while an upcoming meetup
 * is still active (per server status + scheduled time). When the slot is in the past, negotiation
 * can continue and a new meetup can be scheduled from the banner.
 *
 * Call only when there is no active blocking order (caller should reject [order_id] flows first).
 */
internal fun shouldBlockBuyerNewPriceOffer(
    messages: List<ChatMessage>,
    viewerIsBuyer: Boolean,
    orderStatus: String?,
    orderApptStatus: String?,
    orderApptScheduledAt: String?,
): Boolean {
    if (!viewerIsBuyer) return false
    if (!conversationHadBuyerOfferAndSellerCounter(messages, viewerIsBuyer = true)) return false
    return hasActiveFutureMeetupBlockingReschedule(
        orderStatus = orderStatus,
        orderApptStatus = orderApptStatus,
        orderApptScheduledAt = orderApptScheduledAt,
        messages = messages,
    )
}

/**
 * Hides the deal-banner "Schedule meetup" CTA while the linked order still has an active
 * meetup row (`pending` / `confirmed`). Unlike [hasActiveFutureMeetupBlockingReschedule], this
 * does **not** treat a past [scheduled_at] as "free to schedule again" — both parties may have
 * checked in and the escrow handoff may still be in progress.
 *
 * When the order is [delivered_confirmed] or [disputed], the banner must not offer a new meetup.
 * [cancelled] is usually filtered by the screen; message fall-through still applies if needed.
 */
internal fun dealBannerShouldHideScheduleMeetup(
    orderStatus: String?,
    orderApptStatus: String?,
    @Suppress("UNUSED_PARAMETER") orderApptScheduledAt: String?,
    messages: List<ChatMessage>,
): Boolean {
    val ord = orderStatus?.trim()?.lowercase().orEmpty()
    if (ord == "delivered_confirmed" || ord == "disputed") return true

    val oStRaw = orderApptStatus?.trim().orEmpty()
    val oSt = oStRaw.lowercase()
    if (oSt == "pending" || oSt == "confirmed") return true
    // Order payload already describes a finished / withdrawn meetup — do not use stale chat cards.
    if (oStRaw.isNotEmpty()) return false

    val latest = messages
        .filter { it.messageType.equals("meeting_proposal", ignoreCase = true) }
        .maxByOrNull { it.timestamp }
    val apSt = latest?.meetingAppointment?.status?.trim()?.lowercase().orEmpty()
    return apSt == "pending" || apSt == "confirmed"
}
