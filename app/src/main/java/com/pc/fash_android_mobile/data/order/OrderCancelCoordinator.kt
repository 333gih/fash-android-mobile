package com.pc.fash_android_mobile.data.order

import com.pc.fash_android_mobile.data.chat.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Side effects after a buyer successfully cancels a `payment_pending` order: optional chat line so the
 * thread records that the order was cancelled (cancel itself already succeeded).
 */
class OrderCancelCoordinator(
    private val orderRepository: OrderRepository,
    private val chatRepository: ChatRepository,
) {

    companion object {
        /** First line of automated cancel messages; UI parses this to link to order detail. */
        const val ORDER_CANCELLED_ORDER_ID_PREFIX = "FASH_ORDER_CANCELLED_ORDER_ID="
    }

    private fun buildCancelMessageBody(orderId: String, visibleMessage: String): String =
        "${ORDER_CANCELLED_ORDER_ID_PREFIX}${orderId.trim()}\n${visibleMessage.trim()}"

    /**
     * Posts a buyer-visible chat line with embedded order id for deep-link UI. Ignores failures so a chat error
     * never surfaces as a failed cancel.
     */
    suspend fun notifyBuyerCancelledOrder(conversationId: String?, orderId: String, visibleMessage: String) {
        val cid = conversationId?.trim()?.takeIf { it.isNotEmpty() } ?: return
        val body = buildCancelMessageBody(orderId, visibleMessage)
        withContext(Dispatchers.IO) {
            chatRepository.sendMessage(cid, body).onFailure { /* best-effort */ }
        }
    }

    /**
     * Loads order detail to resolve [OrderDetail.conversationId] (e.g. global pending-payment banner).
     * If the order payload has no conversation id, opens the listing thread via [ChatRepository.startConversation].
     */
    suspend fun notifyBuyerCancelledOrderByOrderId(orderId: String, visibleMessage: String) {
        val od = withContext(Dispatchers.IO) {
            orderRepository.getOrderDetail(orderId).getOrNull()
        } ?: return
        var cid = od.conversationId.trim()
        if (cid.isEmpty()) {
            val lid = od.listingId.trim().takeIf { it.isNotEmpty() } ?: return
            cid = withContext(Dispatchers.IO) {
                chatRepository.startConversation(lid).getOrNull().orEmpty()
            }
        }
        notifyBuyerCancelledOrder(cid, orderId, visibleMessage)
    }
}
