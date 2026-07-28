package com.pc.fash_android_mobile.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pc.fash_android_mobile.MainActivity
import com.pc.fash_android_mobile.R

/** Fires the guest local re-engagement notification (no FCM). */
class GuestLocalReengagementReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (
            action != GuestLocalReengagementScheduler.ACTION_FIRE &&
            action != GuestLocalReengagementScheduler.ACTION_FIRE_EVENING
        ) {
            return
        }
        if (!FashNotificationChannels.areNotificationsEnabled(context)) return
        if (!GuestLocalReengagementScheduler.canFireToday(context)) {
            Log.d(TAG, "daily cap hit — skip")
            GuestLocalReengagementScheduler.scheduleAfterBackground(context)
            return
        }
        FashNotificationChannels.ensureChannels(context)
        val notificationId = if (action == GuestLocalReengagementScheduler.ACTION_FIRE_EVENING) {
            GuestLocalReengagementScheduler.NOTIFICATION_ID_EVENING
        } else {
            GuestLocalReengagementScheduler.NOTIFICATION_ID
        }
        val openIntent = Intent(context, MainActivity::class.java).apply {
            this.action = GuestLocalReengagementScheduler.ACTION_OPEN_GUEST_HOME
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val body = GuestLocalReengagementScheduler.reminderBody(context)
        val notification = NotificationCompat.Builder(context, FashNotificationChannels.GUEST_REENGAGEMENT)
            .setSmallIcon(R.drawable.ic_stat_fash)
            .setColor(ContextCompat.getColor(context, R.color.fash_brand))
            .setContentTitle(GuestLocalReengagementScheduler.reminderTitle(context))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
        GuestLocalReengagementScheduler.markFiredToday(context)
        GuestLocalReengagementScheduler.scheduleAfterBackground(context)
    }

    companion object {
        private const val TAG = "GuestLocalReminderRx"
    }
}
