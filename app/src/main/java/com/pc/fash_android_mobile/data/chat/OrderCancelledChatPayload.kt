package com.pc.fash_android_mobile.data.chat

import com.pc.fash_android_mobile.data.order.OrderCancelCoordinator
import org.json.JSONObject

data class OrderCancelledChatPayload(
    val orderId: String,
    val reasonCode: String,
    val cancelledBy: String,
    val amountVnd: Long = 0L,
    val reasonNote: String = "",
)

fun parseOrderCancelledPayload(messageType: String, fullText: String): OrderCancelledChatPayload? {
    if (messageType.equals("order_cancelled", ignoreCase = true)) {
        return try {
            val o = JSONObject(fullText.trim())
            val orderId = o.optString("order_id", o.optString("orderId", "")).trim()
            if (orderId.isEmpty()) return null
            OrderCancelledChatPayload(
                orderId = orderId,
                reasonCode = o.optString("reason_code", o.optString("reasonCode", "")).trim().lowercase(),
                cancelledBy = o.optString("cancelled_by", o.optString("cancelledBy", "buyer")).trim().lowercase(),
                amountVnd = o.optLong("amount_vnd", o.optLong("amountVnd", 0L)),
                reasonNote = o.optString("reason_note", o.optString("reasonNote", "")).trim(),
            )
        } catch (_: Exception) {
            null
        }
    }
    return parseLegacyOrderCancelledEmbeddedMessage(fullText)
}

private fun parseLegacyOrderCancelledEmbeddedMessage(fullText: String): OrderCancelledChatPayload? {
    val firstLine = fullText.lineSequence().firstOrNull() ?: return null
    val prefix = OrderCancelCoordinator.ORDER_CANCELLED_ORDER_ID_PREFIX
    if (!firstLine.startsWith(prefix)) return null
    val orderId = firstLine.removePrefix(prefix).trim().removeSuffix("...")
    if (orderId.isBlank()) return null
    return OrderCancelledChatPayload(
        orderId = orderId,
        reasonCode = "changed_mind",
        cancelledBy = "buyer",
        amountVnd = 0L,
        reasonNote = fullText.substringAfter("\n", "").trim(),
    )
}

fun isOrderCancelledChatMessage(messageType: String, fullText: String): Boolean =
    messageType.equals("order_cancelled", ignoreCase = true) ||
        fullText.trim().startsWith(OrderCancelCoordinator.ORDER_CANCELLED_ORDER_ID_PREFIX)

fun isOrderCancelledEmbeddedMessage(fullText: String): Boolean =
    fullText.trim().startsWith(OrderCancelCoordinator.ORDER_CANCELLED_ORDER_ID_PREFIX)

/** Inbox preview for legacy embedded cancel text. */
fun orderCancelledChatPreviewText(fullText: String, fallback: String): String? {
    if (!isOrderCancelledEmbeddedMessage(fullText)) return null
    val rest = fullText.substringAfter("\n", "").trim()
    return rest.ifBlank { fallback }
}
