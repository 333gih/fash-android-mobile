package com.pc.fash_android_mobile.ui.chat

import com.pc.fash_android_mobile.FashApplication

/** Side effects when chat-related in-app notifications are suppressed (user already in that thread). */
object ChatNotificationPresence {

    fun openConversationId(
        shellSelectedConversationId: String?,
        activeChatConversationId: String?,
    ): String? {
        val routerId = shellSelectedConversationId?.trim().orEmpty()
        if (routerId.isNotEmpty()) return routerId
        val active = activeChatConversationId?.trim().orEmpty()
        return active.takeIf { it.isNotEmpty() }
    }

    fun registerOpenConversation(app: FashApplication, conversationId: String) {
        val id = conversationId.trim()
        if (id.isEmpty()) return
        app.activeChatConversationId = id
        val session = app.inAppNotification.value
        if (session != null && ChatInAppNotificationPolicy.shouldSuppressInApp(session.data, id)) {
            app.dismissInAppNotification()
        }
    }

    fun clearOpenConversation(app: FashApplication, conversationId: String) {
        val id = conversationId.trim()
        if (id.isEmpty()) return
        if (app.activeChatConversationId.equals(id, ignoreCase = true)) {
            app.activeChatConversationId = null
        }
    }

    fun handleSuppressedChatNotification(app: FashApplication, data: Map<String, String>?) {
        app.runSuppressedChatNotificationSideEffects(data)
    }
}
