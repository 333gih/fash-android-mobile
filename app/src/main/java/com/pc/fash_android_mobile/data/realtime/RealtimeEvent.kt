package com.pc.fash_android_mobile.data.realtime

/** Events emitted by [RealtimeManager] after parsing incoming WebSocket frames. */
sealed class RealtimeEvent {

    /** Fired once on successful handshake (`type: connected`). */
    data class Connected(val userId: String, val connId: String) : RealtimeEvent()

    /** WebSocket closed or lost; [willReconnect] = true when auto-retry is pending. */
    data class Disconnected(val willReconnect: Boolean) : RealtimeEvent()

    /**
     * A new chat-related message arrived (`type: message.new`).
     * Full message data must be fetched via REST — this is a lightweight routing signal.
     */
    data class MessageNew(
        val conversationId: String,
        val messageId: String,
        val senderId: String,
        val recipientId: String,
        val preview: String,
        val messageType: String,
    ) : RealtimeEvent()

    /** Someone marked messages read (`type: read.receipts`). */
    data class ReadReceipts(
        val conversationId: String,
        val readerId: String,
        val notifyUserId: String,
    ) : RealtimeEvent()

    /** Typing started (`type: typing.start`). */
    data class TypingStart(val conversationId: String, val userId: String) : RealtimeEvent()

    /** Typing stopped (`type: typing.stop`). */
    data class TypingStop(val conversationId: String, val userId: String) : RealtimeEvent()

    /**
     * Order lifecycle changed (`type: order.status_changed`).
     * Re-fetch order details via REST on receipt.
     */
    data class OrderStatusChanged(
        val orderId: String,
        val conversationId: String,
        val newStatus: String,
    ) : RealtimeEvent()

    /** Feed/listing refresh hint (`type: feed.refresh`). */
    object FeedRefresh : RealtimeEvent()

    /** Server pong response (`type: pong`). */
    object Pong : RealtimeEvent()

    /** Raw unrecognised type — for forward compatibility. */
    data class Unknown(val type: String) : RealtimeEvent()
}
