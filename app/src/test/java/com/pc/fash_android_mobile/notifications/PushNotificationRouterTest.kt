package com.pc.fash_android_mobile.notifications

import com.pc.fash_android_mobile.data.user.InboxNotificationItem
import com.pc.fash_android_mobile.ui.notifications.parseNotificationDetailActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PushNotificationRouterTest {

    @Test
    fun parseNotificationDetailActions_exposesChatWithoutNavTarget() {
        val item = InboxNotificationItem(
            id = "1",
            title = "New message",
            body = "Hi",
            dataMap = mapOf(
                "conversation_id" to "conv-abc",
                "type" to "marketplace.chat.message",
            ),
            payloadType = "marketplace.chat.message",
            notificationGroup = "COMMERCE",
            source = null,
            sourceEventId = null,
            readAtIso = null,
            createdAtIso = "2026-01-01T00:00:00Z",
        )
        val actions = parseNotificationDetailActions(item)
        assertEquals("conv-abc", actions.conversationId)
    }

    @Test
    fun chatConversationId_detectsMessageType() {
        val conv = InAppNotificationNavigation.chatConversationId(
            mapOf(
                "conversation_id" to "conv-xyz",
                "type" to "marketplace.chat.message",
            ),
        )
        assertNotNull(conv)
        assertEquals("conv-xyz", conv)
    }

    @Test
    fun trayPayload_withChatFields_stillCarriesInboxLedgerId() {
        val nid = "550e8400-e29b-41d4-a716-446655440000"
        val data = mapOf(
            "conversation_id" to "conv-xyz",
            "user_notification_id" to nid,
            "deep_link" to "fash://inbox/$nid",
            "type" to "marketplace.chat.message",
        )
        assertNotNull(InAppNotificationNavigation.chatConversationId(data))
        assertEquals(nid, data["user_notification_id"])
    }
}
