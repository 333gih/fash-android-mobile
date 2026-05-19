package com.pc.fash_android_mobile.ui.notifications

import com.pc.fash_android_mobile.data.user.InboxNotificationItem
import java.util.Locale

/** Parsed from inbox `data` / FCM data map for primary actions on the detail screen. */
data class NotificationDetailActions(
    val orderId: String?,
    val listingId: String?,
    val sellerUserId: String?,
    val conversationId: String?,
    val openFollowersTab: Boolean,
    val openFollowingTab: Boolean,
    val openExploreTab: Boolean,
    val openInviteFriends: Boolean,
    val richDetailBody: String?,
    val imageUrl: String?,
)

fun parseNotificationDetailActions(item: InboxNotificationItem): NotificationDetailActions {
    parseAppPromoCampaignFromInbox(item)?.let { promo ->
        val rich = promo.remoteMessage?.takeIf { it.isNotBlank() && it != item.body }
        val image = promo.remoteImageUrls.firstOrNull()
        return NotificationDetailActions(
            orderId = null,
            listingId = null,
            sellerUserId = null,
            conversationId = null,
            openFollowersTab = false,
            openFollowingTab = false,
            openExploreTab = false,
            openInviteFriends = false,
            richDetailBody = rich,
            imageUrl = image,
        )
    }
    val data = item.dataMap
    val orderId = firstStringFromDataCi(data, "order_id", "marketplace_order_id", "orderId")
    val listingId = firstStringFromDataCi(data, "listing_id", "listingId")
    val sellerUserId = sellerUserIdFromData(data)
    val conversationId = firstStringFromDataCi(data, "conversation_id", "conversationId")
    val nav = firstStringFromDataCi(data, "nav_target", "navTarget")?.lowercase(Locale.ROOT).orEmpty()
    val screen = firstStringFromDataCi(data, "screen")?.lowercase(Locale.ROOT).orEmpty()
    val ptype = item.payloadType?.lowercase(Locale.ROOT).orEmpty()

    val openFollowersTab = nav == "followers_tab" ||
        ptype.equals("marketplace.follower.new", ignoreCase = true) ||
        ptype.equals("marketplace.follower.batch", ignoreCase = true)

    val openFollowingTab = nav == "following_tab"

    val openExploreTab = nav == "explore_tab"

    val openInviteFriends = nav == "in_app_invite_friends" ||
        ptype.equals("marketplace.referral.invite_rewarded", ignoreCase = true)

    val rich = firstStringFromDataCi(data, "detail_body", "detailBody", "rich_body", "richBody")
    val imageUrl = firstStringFromDataCi(
        data,
        "notification_image_url",
        "notificationImageUrl",
        "image_url",
        "imageUrl",
    )

    val wantsChat = nav == "chat" || screen == "chat"
    val chatId = conversationId?.takeIf { wantsChat && it.isNotBlank() }

    return NotificationDetailActions(
        orderId = orderId,
        listingId = listingId,
        sellerUserId = sellerUserId,
        conversationId = chatId,
        openFollowersTab = openFollowersTab,
        openFollowingTab = openFollowingTab,
        openExploreTab = openExploreTab,
        openInviteFriends = openInviteFriends,
        richDetailBody = rich,
        imageUrl = imageUrl,
    )
}

internal fun firstStringFromDataCi(data: Map<String, Any?>?, vararg keys: String): String? {
    if (data == null) return null
    val byLower = data.entries.associate { it.key.lowercase(Locale.ROOT) to it.value }
    for (k in keys) {
        val v = byLower[k.lowercase(Locale.ROOT)] ?: continue
        val s = when (v) {
            is String -> v.trim()
            is Number -> v.toString()
            else -> v?.toString()?.trim().orEmpty()
        }
        if (s.isNotEmpty()) return s
    }
    return null
}

internal fun sellerUserIdFromData(data: Map<String, Any?>?): String? =
    firstStringFromDataCi(data, "seller_user_id", "sellerUserId", "seller_id", "sellerId")
