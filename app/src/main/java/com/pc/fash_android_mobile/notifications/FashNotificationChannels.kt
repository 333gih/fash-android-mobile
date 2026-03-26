package com.pc.fash_android_mobile.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

/**
 * Android 8+ channels for FCM. Align with server `android_channel_id` when provided.
 */
object FashNotificationChannels {

    const val CHAT = "fash_chat"
    const val ORDERS = "fash_orders"
    const val GENERAL = "fash_general"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val chat = NotificationChannel(
            CHAT,
            context.getString(com.pc.fash_android_mobile.R.string.notification_channel_chat_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(com.pc.fash_android_mobile.R.string.notification_channel_chat_desc)
        }
        val orders = NotificationChannel(
            ORDERS,
            context.getString(com.pc.fash_android_mobile.R.string.notification_channel_orders_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(com.pc.fash_android_mobile.R.string.notification_channel_orders_desc)
        }
        val general = NotificationChannel(
            GENERAL,
            context.getString(com.pc.fash_android_mobile.R.string.notification_channel_general_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(com.pc.fash_android_mobile.R.string.notification_channel_general_desc)
        }
        mgr.createNotificationChannel(chat)
        mgr.createNotificationChannel(orders)
        mgr.createNotificationChannel(general)
    }

    fun areNotificationsEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()
}
