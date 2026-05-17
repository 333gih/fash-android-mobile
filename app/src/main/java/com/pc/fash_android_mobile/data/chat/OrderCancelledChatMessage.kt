package com.pc.fash_android_mobile.data.chat

@Deprecated("Use parseOrderCancelledPayload", ReplaceWith("parseOrderCancelledPayload(\"text\", fullText)"))
fun parseOrderCancelledEmbeddedMessage(fullText: String): Pair<String, String>? {
    val payload = parseOrderCancelledPayload("text", fullText) ?: return null
    return payload.orderId to payload.reasonNote
}
