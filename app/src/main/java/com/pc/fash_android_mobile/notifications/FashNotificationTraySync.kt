package com.pc.fash_android_mobile.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat

/**
 * Keeps the **system notification tray** aligned with inbox read state.
 *
 * Samsung / DeX launcher badges (the number on the app icon, like WhatsApp) count **active
 * tray notifications** for this app — not the in-app unread API alone. If the user reads a
 * message inside Fash but the tray entry remains, DeX still shows badge "1".
 */
object FashNotificationTraySync {

    fun trayIdForInboxNotification(notificationId: String): Int {
        val id = notificationId.trim()
        require(id.isNotEmpty())
        return stableTrayId("inbox:$id")
    }

    /** Stable tray id for FCM data payloads (inbox row, chat thread, or deep link). */
    fun trayIdForFcmData(data: Map<String, String>): Int {
        data["user_notification_id"]?.trim()?.takeIf { it.isNotEmpty() }?.let {
            return trayIdForInboxNotification(it)
        }
        data["conversation_id"]?.trim()?.takeIf { it.isNotEmpty() }?.let {
            return stableTrayId("chat:$it")
        }
        data["deep_link"]?.trim()?.takeIf { it.isNotEmpty() }?.let {
            return stableTrayId("deeplink:$it")
        }
        val type = data["type"].orEmpty()
        val title = data["title"].orEmpty()
        return stableTrayId("fcm:$type:$title")
    }

    fun cancelInboxNotification(context: Context, notificationId: String) {
        val id = notificationId.trim()
        if (id.isEmpty()) return
        NotificationManagerCompat.from(context.applicationContext)
            .cancel(trayIdForInboxNotification(id))
    }

    fun clearAllTrayNotifications(context: Context) {
        NotificationManagerCompat.from(context.applicationContext).cancelAll()
    }

    /** When server unread is zero, clear tray so Samsung / DeX launcher badge disappears. */
    fun syncTrayWithUnreadCount(context: Context, unreadCount: Int) {
        if (unreadCount <= 0) {
            clearAllTrayNotifications(context)
        }
    }

    private fun stableTrayId(key: String): Int = key.hashCode() and 0x7FFFFFFF
}
