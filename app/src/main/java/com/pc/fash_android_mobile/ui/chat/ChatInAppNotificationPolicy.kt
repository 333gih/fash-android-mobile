package com.pc.fash_android_mobile.ui.chat

/**
 * When to surface chat / deal in-app toasts — suppress while the user is in that conversation.
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

    fun isChatRelatedType(type: String?): Boolean {
        val normalized = type?.trim()?.lowercase().orEmpty()
        if (normalized.isEmpty()) return false
        if (normalized.contains("chat") || normalized.contains("message")) return true
        if (normalized.startsWith("marketplace.chat.")) return true
        if (normalized.startsWith("meeting_") || normalized == "deal_complete_nudge") return true
        return false
    }

    fun isChatRelated(data: Map<String, String>?): Boolean {
        if (data.isNullOrEmpty()) return false
        if (isChatRelatedType(data["type"])) return true
        if (isChatRelatedType(data["event"])) return true
        return conversationId(data) != null
    }

    fun isOpenConversation(conversationId: String, openConversationId: String?): Boolean {
        val target = conversationId.trim()
        val open = openConversationId?.trim().orEmpty()
        if (target.isEmpty() || open.isEmpty()) return false
        return target.equals(open, ignoreCase = true)
    }

    fun shouldSuppressInApp(data: Map<String, String>?, openConversationId: String?): Boolean {
        val target = conversationId(data) ?: return false
        return isOpenConversation(target, openConversationId)
    }

    fun shouldShowMessageNewInApp(
        conversationId: String,
        senderId: String,
        recipientId: String,
        messageType: String,
        systemSubtype: String?,
        myUserId: String,
        openConversationId: String?,
    ): Boolean {
        val myId = myUserId.trim()
        if (myId.isEmpty()) return false

        val sender = senderId.trim()
        if (sender.isEmpty() || sender.equals(myId, ignoreCase = true)) return false

        val recipient = recipientId.trim()
        if (recipient.isNotEmpty() && !recipient.equals(myId, ignoreCase = true)) return false

        if (shouldSuppressInApp(mapOf("conversation_id" to conversationId), openConversationId)) {
            return false
        }

        val cid = conversationId.trim()
        if (cid.isEmpty()) return false

        if (messageType.equals("system", ignoreCase = true)) {
            val sub = systemSubtype?.lowercase().orEmpty()
            if (sub.startsWith("conversation.")) return false
        }
        return true
    }
}
