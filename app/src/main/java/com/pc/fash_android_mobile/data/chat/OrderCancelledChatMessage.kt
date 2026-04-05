package com.pc.fash_android_mobile.data.chat

import com.pc.fash_android_mobile.data.order.OrderCancelCoordinator

/**
 * Parses a buyer-cancel system line: first line is [OrderCancelCoordinator.ORDER_CANCELLED_ORDER_ID_PREFIX]
 * plus order id; remaining lines are the visible message.
 *
 * @return pair of (orderId, display text) or null if not a cancel payload.
 */
fun parseOrderCancelledEmbeddedMessage(fullText: String): Pair<String, String>? {
    val firstLine = fullText.lineSequence().firstOrNull() ?: return null
    val prefix = OrderCancelCoordinator.ORDER_CANCELLED_ORDER_ID_PREFIX
    if (!firstLine.startsWith(prefix)) return null
    val orderId = firstLine.removePrefix(prefix).trim()
    if (orderId.isBlank()) return null
    val rest = fullText.substringAfter("\n", "").trim()
    val display = if (rest.isNotEmpty()) rest else fullText
    return orderId to display
}
