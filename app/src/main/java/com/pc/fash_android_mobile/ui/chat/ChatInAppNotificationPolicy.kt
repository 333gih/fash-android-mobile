package com.pc.fash_android_mobile.ui.chat

/**
 * Suppresses chat/deal in-app toasts while the user is already inside that conversation.
 */
object ChatInAppNotificationPolicy {
    fun conversationId(data: Map<String, String>?): String? {
        if (data.isNullOrEmpty()) return null
        for (key in listOf("conversation_id", "conversationId", "ConversationID")) {
            val value = data[key]?.trim().orEmpty()
            if (value.isNotEmpty()) return value
        }
        return null
    }

    fun isChatRelated(data: Map<String, String>?): Boolean {
        if (data.isNullOrEmpty()) return false
        val type = data["type"]?.trim()?.lowercase().orEmpty()
        val event = data["event"]?.trim()?.lowercase().orEmpty()
        if (type.contains("chat") || type.contains("message") || type.startsWith("marketplace.chat.")) {
            return true
        }
        if (event.startsWith("meeting_") || event == "deal_complete_nudge") return true
        return conversationId(data) != null
    }

    fun shouldSuppressInApp(data: Map<String, String>?, openConversationId: String?): Boolean {
        val target = conversationId(data) ?: return false
        val open = openConversationId?.trim().orEmpty()
        if (open.isEmpty()) return false
        return target.equals(open, ignoreCase = true)
    }
}
