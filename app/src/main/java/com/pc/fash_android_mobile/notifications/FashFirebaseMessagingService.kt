package com.pc.fash_android_mobile.notifications

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.MainActivity
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.realtime.RealtimeManager

/**
 * Handles FCM token refresh and incoming messages.
 *
 * When the app is in the foreground and the realtime WebSocket is **connected**, tray
 * notifications are skipped — core-service should already suppress FCM via Redis presence;
 * this avoids duplicate heads-up if a push races with in-app delivery.
 */
class FashFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        (applicationContext as? FashApplication)?.fcmTokenRegistrar?.registerTokenAsync(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (message.data["inbox_refresh"] == "1") {
            (applicationContext as? FashApplication)?.requestInboxUnreadRefreshDebounced()
        }
        if (shouldSuppressTrayForPresence()) return

        val title = message.notification?.title
            ?: message.data["title"]
            ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        val channelId = resolveChannelId(message)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            message.data["deep_link"]?.let { putExtra("deep_link", it) }
        }
        val pending = PendingIntent.getActivity(
            this,
            (title + body).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        val notifId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        NotificationManagerCompat.from(this).notify(notifId, notification)
    }

    private fun shouldSuppressTrayForPresence(): Boolean {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle.currentState
        if (!lifecycle.isAtLeast(Lifecycle.State.STARTED)) return false
        val app = applicationContext as? FashApplication ?: return false
        return app.realtimeManager.state.value == RealtimeManager.State.CONNECTED
    }

    private fun resolveChannelId(message: RemoteMessage): String {
        message.notification?.channelId?.takeIf { it.isNotBlank() }?.let { return it }
        message.data["channel_id"]?.takeIf { it.isNotBlank() }?.let { return it }
        return when (message.data["type"]?.lowercase()) {
            "chat", "message", "message.new" -> FashNotificationChannels.CHAT
            "order", "orders" -> FashNotificationChannels.ORDERS
            else -> FashNotificationChannels.GENERAL
        }
    }
}
