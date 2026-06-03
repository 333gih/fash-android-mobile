package com.pc.fash_android_mobile.notifications

import com.pc.fash_android_mobile.FashInAppNotificationSession
import com.pc.fash_android_mobile.deeplink.InboxDeepLinks
import com.pc.fash_android_mobile.ui.chat.ChatInAppNotificationPolicy

/** Resolves in-app banner taps to navigation targets (chat-first for marketplace threads). */
object InAppNotificationNavigation {

    fun chatConversationId(data: Map<String, String>?): String? {
        if (data.isNullOrEmpty()) return null
        val conv = ChatInAppNotificationPolicy.conversationId(data) ?: return null
        val nav = normalized(data["nav_target"] ?: data["navTarget"])
        val screen = normalized(data["screen"])
        val type = normalized(data["type"])
        val event = normalized(data["event"])

        if (nav == "chat" || screen == "chat") return conv
        if (ChatInAppNotificationPolicy.isChatRelatedType(type)) return conv
        if (ChatInAppNotificationPolicy.isChatRelatedType(event)) return conv
        if (type.startsWith("meeting_") || event.startsWith("meeting_") || event == "deal_complete_nudge") {
            return conv
        }
        if (ChatInAppNotificationPolicy.isChatRelated(data)) return conv
        return null
    }

    fun orderId(data: Map<String, String>?): String? {
        if (data.isNullOrEmpty()) return null
        for (key in listOf("order_id", "marketplace_order_id", "orderId")) {
            val value = data[key]?.trim().orEmpty()
            if (value.isNotEmpty()) return value
        }
        return null
    }

    /**
     * @return true when navigation was handled (caller should dismiss banner).
     */
    fun handleBannerTap(
        session: FashInAppNotificationSession,
        onOpenChat: (String) -> Unit,
        onOpenOrder: (String) -> Unit,
        onOpenInviteFriends: () -> Unit,
        onOpenNotificationDetail: (String) -> Unit,
        onOpenNotificationInbox: () -> Unit,
        onOpenDeepLink: (String) -> Unit,
    ): Boolean {
        val data = session.data

        chatConversationId(data)?.let { conv ->
            onOpenChat(conv)
            return true
        }

        data?.get("deep_link")?.trim()?.takeIf { it.isNotEmpty() }?.let { deepLink ->
            onOpenDeepLink(deepLink)
            return true
        }

        val nav = normalized(data?.get("nav_target") ?: data?.get("navTarget"))
        if (nav == "in_app_invite_friends") {
            onOpenInviteFriends()
            return true
        }
        if (nav == "order") {
            orderId(data)?.let { oid ->
                onOpenOrder(oid)
                return true
            }
        }

        val deepNid = data?.entries
            ?.find { it.key.equals("deep_link", ignoreCase = true) }
            ?.value
            ?.let { InboxDeepLinks.parseNotificationIdFromDeepLinkString(it) }
        val nid = session.userNotificationId?.trim()?.takeIf { it.isNotEmpty() }
            ?: data?.get("user_notification_id")?.trim()?.takeIf { it.isNotEmpty() }
            ?: deepNid
        if (!nid.isNullOrBlank()) {
            onOpenNotificationDetail(nid)
            return true
        }

        onOpenNotificationInbox()
        return true
    }

    private fun normalized(value: String?): String = value?.trim()?.lowercase().orEmpty()
}
