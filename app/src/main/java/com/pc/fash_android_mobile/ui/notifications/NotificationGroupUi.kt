package com.pc.fash_android_mobile.ui.notifications

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Recommend
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.ui.graphics.vector.ImageVector
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.user.NotificationGroupSummaryItem

/** Canonical inbox groups — keep in sync with core-service/internal/domain/notifications/groups.go */
object NotificationGroups {
    const val SYSTEM = "SYSTEM"
    const val COMMERCE = "COMMERCE"
    const val SOCIAL = "SOCIAL"
    const val RECOMMENDATION = "RECOMMENDATION"
    const val ADS = "ADS"
    const val REENGAGEMENT = "REENGAGEMENT"
    const val REALTIME = "REALTIME"

    val displayOrder = listOf(
        SYSTEM,
        COMMERCE,
        SOCIAL,
        RECOMMENDATION,
        ADS,
        REENGAGEMENT,
        REALTIME,
    )
}

@StringRes
fun notificationGroupTitleRes(group: String): Int = when (group) {
    NotificationGroups.SYSTEM -> R.string.notification_group_system
    NotificationGroups.COMMERCE -> R.string.notification_group_commerce
    NotificationGroups.SOCIAL -> R.string.notification_group_social
    NotificationGroups.RECOMMENDATION -> R.string.notification_group_recommendation
    NotificationGroups.ADS -> R.string.notification_group_ads
    NotificationGroups.REENGAGEMENT -> R.string.notification_group_reengagement
    NotificationGroups.REALTIME -> R.string.notification_group_realtime
    else -> R.string.notification_group_system
}

@StringRes
fun notificationGroupSubtitleRes(group: String): Int = when (group) {
    NotificationGroups.SYSTEM -> R.string.notification_group_system_desc
    NotificationGroups.COMMERCE -> R.string.notification_group_commerce_desc
    NotificationGroups.SOCIAL -> R.string.notification_group_social_desc
    NotificationGroups.RECOMMENDATION -> R.string.notification_group_recommendation_desc
    NotificationGroups.ADS -> R.string.notification_group_ads_desc
    NotificationGroups.REENGAGEMENT -> R.string.notification_group_reengagement_desc
    NotificationGroups.REALTIME -> R.string.notification_group_realtime_desc
    else -> R.string.notification_group_system_desc
}

fun notificationGroupHasActivity(item: NotificationGroupSummaryItem): Boolean =
    item.unreadCount > 0 || !item.latestId.isNullOrBlank()

fun notificationGroupIcon(group: String): ImageVector = when (group) {
    NotificationGroups.SYSTEM -> Icons.Outlined.Settings
    NotificationGroups.COMMERCE -> Icons.Outlined.ShoppingBag
    NotificationGroups.SOCIAL -> Icons.Outlined.PeopleOutline
    NotificationGroups.RECOMMENDATION -> Icons.Outlined.Recommend
    NotificationGroups.ADS -> Icons.Outlined.Campaign
    NotificationGroups.REENGAGEMENT -> Icons.Outlined.StarOutline
    NotificationGroups.REALTIME -> Icons.Outlined.ChatBubbleOutline
    else -> Icons.Outlined.Notifications
}

fun notificationPayloadIcon(payloadType: String?): ImageVector {
    return when (payloadType?.trim()?.lowercase()) {
        "marketplace.follower.new", "marketplace.follower.batch" -> Icons.Outlined.PeopleOutline
        "marketplace.listing.liked", "marketplace.listing.liked.batch" -> Icons.Outlined.StarOutline
        "marketplace.listing.approved_for_followers", "marketplace.listing.approved" -> Icons.Outlined.LocalOffer
        "marketplace.chat.message" -> Icons.Outlined.ChatBubbleOutline
        "marketplace.chat.offer_received", "marketplace.chat.offer_accepted", "marketplace.chat.offer_declined" -> Icons.Outlined.ShoppingBag
        "marketplace.order.created", "marketplace.order.shipped", "marketplace.order.cancelled",
        "marketplace.order.meetup_aborted", "marketplace.order.funds_released", "marketplace.order.dispute_opened",
        -> Icons.Outlined.ShoppingBag
        "marketplace.review.received" -> Icons.Outlined.StarOutline
        "marketplace.referral.invite_rewarded" -> Icons.Outlined.StarOutline
        "admin.mobile_push.promo", "admin.app_promo_interstitial" -> Icons.Outlined.Campaign
        "admin.mobile_push.ops", "admin.mobile_push.transactional", "admin.mobile_push.announcement", "admin.mobile_push" -> Icons.Outlined.Settings
        else -> Icons.Outlined.Notifications
    }
}
