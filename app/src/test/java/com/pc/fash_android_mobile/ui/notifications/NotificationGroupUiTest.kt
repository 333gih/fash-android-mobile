package com.pc.fash_android_mobile.ui.notifications

import com.pc.fash_android_mobile.data.user.InboxNotificationItem
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationGroupUiTest {

    @Test
    fun resolveInboxNotificationGroup_usesServerGroupWhenPresent() {
        val item = InboxNotificationItem(
            id = "1",
            title = "t",
            body = "b",
            dataMap = null,
            payloadType = "marketplace.order.created",
            notificationGroup = "COMMERCE",
            source = null,
            sourceEventId = null,
            readAtIso = null,
            createdAtIso = "2026-01-01T00:00:00Z",
        )
        assertEquals(NotificationGroups.COMMERCE, resolveInboxNotificationGroup(item))
    }

    @Test
    fun resolveInboxNotificationGroup_recommendationFromPayloadType() {
        val item = InboxNotificationItem(
            id = "2",
            title = "t",
            body = "b",
            dataMap = null,
            payloadType = "marketplace.recommendation.style_fresh",
            notificationGroup = null,
            source = null,
            sourceEventId = null,
            readAtIso = null,
            createdAtIso = "2026-01-01T00:00:00Z",
        )
        assertEquals(NotificationGroups.RECOMMENDATION, resolveInboxNotificationGroup(item))
    }

    @Test
    fun resolveInboxNotificationGroup_socialFallback() {
        val item = InboxNotificationItem(
            id = "3",
            title = "t",
            body = "b",
            dataMap = null,
            payloadType = "marketplace.follower.new",
            notificationGroup = null,
            source = null,
            sourceEventId = null,
            readAtIso = null,
            createdAtIso = "2026-01-01T00:00:00Z",
        )
        assertEquals(NotificationGroups.SOCIAL, resolveInboxNotificationGroup(item))
    }
}
