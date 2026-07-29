package com.pc.fash_android_mobile.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FashNotificationTraySyncTest {

    @Test
    fun trayIdForInboxNotification_isStable() {
        val id = "a1111111-1111-1111-1111-111111111101"
        assertEquals(
            FashNotificationTraySync.trayIdForInboxNotification(id),
            FashNotificationTraySync.trayIdForInboxNotification(id),
        )
    }

    @Test
    fun trayIdForFcmData_prefersUserNotificationId() {
        val data = mapOf(
            "user_notification_id" to "nid-1",
            "conversation_id" to "conv-1",
        )
        assertEquals(
            FashNotificationTraySync.trayIdForInboxNotification("nid-1"),
            FashNotificationTraySync.trayIdForFcmData(data),
        )
    }

    @Test
    fun trayIdForFcmData_differsByInboxRow() {
        val a = FashNotificationTraySync.trayIdForFcmData(mapOf("user_notification_id" to "a"))
        val b = FashNotificationTraySync.trayIdForFcmData(mapOf("user_notification_id" to "b"))
        assertNotEquals(a, b)
    }
}
