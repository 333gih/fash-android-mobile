package com.pc.fash_android_mobile.ui.chat

import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Marks inbox rows read when their payload targets an open chat thread. */
object InboxNotificationSync {
    private val chatGroups = listOf("REALTIME", "REENGAGEMENT")

    suspend fun markChatNotificationsRead(
        conversationId: String,
        userRepository: UserRepository,
    ) = withContext(Dispatchers.IO) {
        val cid = conversationId.trim()
        if (cid.isEmpty()) return@withContext
        for (group in chatGroups) {
            val page = userRepository.listMyNotifications(limit = 40, group = group).getOrNull() ?: continue
            for (item in page.items) {
                if (!item.isUnread) continue
                val itemConv = conversationIdFromData(item.dataMap) ?: continue
                if (!itemConv.equals(cid, ignoreCase = true)) continue
                userRepository.markNotificationRead(item.id)
            }
        }
    }

    private fun conversationIdFromData(data: Map<String, Any?>?): String? {
        if (data.isNullOrEmpty()) return null
        for (key in listOf("conversation_id", "conversationId", "ConversationID")) {
            val value = data[key]?.toString()?.trim().orEmpty()
            if (value.isNotEmpty()) return value
        }
        return null
    }
}
