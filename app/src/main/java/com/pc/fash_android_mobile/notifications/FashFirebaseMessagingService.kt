package com.pc.fash_android_mobile.notifications

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.MainActivity
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.deeplink.AccountSwitchDeepLinks
import com.pc.fash_android_mobile.deeplink.AccountSwitchPrompt
import com.pc.fash_android_mobile.data.promo.ADMIN_APP_PROMO_PAYLOAD_TYPE
import com.pc.fash_android_mobile.data.promo.parseAppPromoFromPushData
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
        AccountSwitchDeepLinks.parseFromFcmData(message.data)?.let { prompt ->
            handleAccountSwitchPrompt(prompt, message)
            return
        }
        if (!isNotificationForLoggedInUser(message)) {
            return
        }
        if (message.data["inbox_refresh"] == "1") {
            (applicationContext as? FashApplication)?.requestInboxUnreadRefreshDebounced()
        }
        if (message.data["type"] == ADMIN_APP_PROMO_PAYLOAD_TYPE) {
            parseAppPromoFromPushData(
                data = message.data,
                fallbackTitle = message.notification?.title ?: message.data["title"],
                fallbackBody = message.notification?.body ?: message.data["body"],
            )?.let { promo ->
                (applicationContext as? FashApplication)?.requestShowAppPromo(promo)
            }
            (applicationContext as? FashApplication)?.requestInboxUnreadRefreshDebounced()
            if (shouldSuppressTrayForPresence()) return
        }
        if (shouldSuppressTrayForPresence()) {
            val inAppTitle = message.notification?.title ?: message.data["title"]
            if (!inAppTitle.isNullOrBlank()) {
                (applicationContext as? FashApplication)?.showInAppNotificationFromRealtime(
                    title = inAppTitle,
                    body = message.notification?.body ?: message.data["body"].orEmpty(),
                    data = message.data,
                    userNotificationId = message.data["user_notification_id"]?.trim()?.takeIf { it.isNotEmpty() },
                )
            }
            return
        }

        val title = message.notification?.title
            ?: message.data["title"]
            ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        val channelId = resolveChannelId(message)

        val deepLink = message.data["deep_link"]?.takeIf { it.isNotBlank() }
            ?: message.data["user_notification_id"]?.takeIf { it.isNotBlank() }?.let { id ->
                "fash://inbox/${id.trim()}"
            }
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            deepLink?.let { putExtra("deep_link", it) }
            message.data["user_notification_id"]?.takeIf { it.isNotBlank() }?.let {
                putExtra("notification_id", it.trim())
            }
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
            // Pre-Oreo: channel sound does not apply; use system default tone + short vibration.
            .apply {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
                }
            }
            .build()

        val notifId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        NotificationManagerCompat.from(this).notify(notifId, notification)
    }

    private fun isNotificationForLoggedInUser(message: RemoteMessage): Boolean {
        val app = applicationContext as? FashApplication ?: return true
        val sessionUid = app.authManager.sessionStore.read()?.userId?.trim().orEmpty()
        if (sessionUid.isEmpty()) return true
        val rid = message.data["recipient_user_id"]?.trim().orEmpty()
        if (rid.isEmpty()) return true
        return sessionUid.equals(rid, ignoreCase = true)
    }

    private fun shouldSuppressTrayForPresence(): Boolean {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle.currentState
        if (!lifecycle.isAtLeast(Lifecycle.State.STARTED)) return false
        val app = applicationContext as? FashApplication ?: return false
        return app.realtimeManager.state.value == RealtimeManager.State.CONNECTED
    }

    private fun handleAccountSwitchPrompt(prompt: AccountSwitchPrompt, message: RemoteMessage) {
        val app = applicationContext as? FashApplication ?: return
        val activeUserId = app.authManager.sessionStore.read()?.userId?.trim().orEmpty()
        if (activeUserId.isNotEmpty() && activeUserId.equals(prompt.pendingUserId, ignoreCase = true)) {
            app.requestInboxUnreadRefreshDebounced()
            return
        }
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.account_switch_notification_title)
        val body = message.notification?.body
            ?: message.data["body"]
            ?: getString(R.string.account_switch_notification_body, prompt.emailMasked ?: "…", prompt.unreadCount)
        showAccountSwitchTray(title, body, prompt)
    }

    private fun showAccountSwitchTray(
        title: String,
        body: String,
        prompt: AccountSwitchPrompt,
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AccountSwitchDeepLinks.EXTRA_PENDING_USER_ID, prompt.pendingUserId)
            prompt.emailMasked?.let { putExtra(AccountSwitchDeepLinks.EXTRA_PENDING_EMAIL_MASKED, it) }
            putExtra(AccountSwitchDeepLinks.EXTRA_UNREAD_COUNT, prompt.unreadCount)
        }
        val pending = PendingIntent.getActivity(
            this,
            ("account_switch_" + prompt.pendingUserId).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, FashNotificationChannels.GENERAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .apply {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
                }
            }
            .build()
        NotificationManagerCompat.from(this).notify(
            ("account_switch_" + prompt.pendingUserId).hashCode() and 0x7FFFFFFF,
            notification,
        )
    }

    private fun resolveChannelId(message: RemoteMessage): String {
        message.notification?.channelId?.takeIf { it.isNotBlank() }?.let { return it }
        message.data["channel_id"]?.takeIf { it.isNotBlank() }?.let { return it }
        return when (message.data["type"]?.lowercase()) {
            "chat", "message", "message.new" -> FashNotificationChannels.CHAT
            "order", "orders" -> FashNotificationChannels.ORDERS
            AccountSwitchDeepLinks.FCM_TYPE -> FashNotificationChannels.GENERAL
            else -> FashNotificationChannels.GENERAL
        }
    }
}
