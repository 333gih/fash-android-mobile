package com.pc.fash_android_mobile.ui.chat

import com.pc.fash_android_mobile.data.user.UserRepository
import com.pc.fash_android_mobile.ui.notifications.isAppPromoInboxNotification
import com.pc.fash_android_mobile.ui.notifications.matchesPromoCampaign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Marks inbox rows read when their payload targets an open chat thread or a seen app promo. */
object InboxNotificationSync {
    private val chatGroups = listOf("REALTIME", "REENGAGEMENT")
    private val promoGroups = listOf("ADS", "SYSTEM", "GENERAL")

    suspend fun markAppPromoNotificationsRead(
        campaignId: String,
        version: Int,
        userNotificationId: String? = null,
        userRepository: UserRepository,
    ) = withContext(Dispatchers.IO) {
        val cid = campaignId.trim()
        if (cid.isEmpty()) return@withContext

        userNotificationId?.trim()?.takeIf { it.isNotEmpty() }?.let { nid ->
            userRepository.markNotificationRead(nid)
        }

        for (group in promoGroups) {
            val page = userRepository.listMyNotifications(limit = 100, group = group).getOrNull() ?: continue
            markPromoUnreadItems(page.items, cid, version, userRepository)
        }
        userRepository.listMyNotifications(limit = 100, group = null).getOrNull()?.let { page ->
            markPromoUnreadItems(page.items, cid, version, userRepository)
        }
    }

    private suspend fun markPromoUnreadItems(
        items: List<com.pc.fash_android_mobile.data.user.InboxNotificationItem>,
        campaignId: String,
        version: Int,
        userRepository: UserRepository,
    ) {
        for (item in items) {
            if (!item.isUnread) continue
            if (!isAppPromoInboxNotification(item)) continue
            if (!matchesPromoCampaign(item, campaignId, version)) continue
            userRepository.markNotificationRead(item.id)
        }
    }

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
