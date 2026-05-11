package com.pc.fash_android_mobile.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Android 8+ channels for FCM. Align with server `android_channel_id` when provided.
 * Sound + light vibration use the system default notification tone (user can override per channel in Settings).
 */
object FashNotificationChannels {

    const val CHAT = "fash_chat"
    const val ORDERS = "fash_orders"
    const val GENERAL = "fash_general"

    private fun applyDefaultAlertStyle(channel: NotificationChannel) {
        val soundUri = Settings.System.DEFAULT_NOTIFICATION_URI
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        channel.setSound(soundUri, attrs)
        channel.enableVibration(true)
        channel.enableLights(true)
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val chat = NotificationChannel(
            CHAT,
            context.getString(com.pc.fash_android_mobile.R.string.notification_channel_chat_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(com.pc.fash_android_mobile.R.string.notification_channel_chat_desc)
            applyDefaultAlertStyle(this)
        }
        val orders = NotificationChannel(
            ORDERS,
            context.getString(com.pc.fash_android_mobile.R.string.notification_channel_orders_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(com.pc.fash_android_mobile.R.string.notification_channel_orders_desc)
            applyDefaultAlertStyle(this)
        }
        val general = NotificationChannel(
            GENERAL,
            context.getString(com.pc.fash_android_mobile.R.string.notification_channel_general_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(com.pc.fash_android_mobile.R.string.notification_channel_general_desc)
            applyDefaultAlertStyle(this)
        }
        mgr.createNotificationChannel(chat)
        mgr.createNotificationChannel(orders)
        mgr.createNotificationChannel(general)
    }

    fun areNotificationsEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()
}
