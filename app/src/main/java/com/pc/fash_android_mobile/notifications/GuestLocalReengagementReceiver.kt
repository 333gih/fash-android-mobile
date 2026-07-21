package com.pc.fash_android_mobile.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pc.fash_android_mobile.MainActivity
import com.pc.fash_android_mobile.R

/** Fires the guest local re-engagement notification (no FCM). */
class GuestLocalReengagementReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != GuestLocalReengagementScheduler.ACTION_FIRE) return
        if (!FashNotificationChannels.areNotificationsEnabled(context)) return
        if (!GuestLocalReengagementScheduler.canFireToday(context)) {
            Log.d(TAG, "daily cap hit — skip")
            GuestLocalReengagementScheduler.scheduleAfterBackground(context)
            return
        }
        FashNotificationChannels.ensureChannels(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = GuestLocalReengagementScheduler.ACTION_OPEN_GUEST_HOME
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            context,
            GuestLocalReengagementScheduler.NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, FashNotificationChannels.GUEST_REENGAGEMENT)
            .setSmallIcon(R.drawable.ic_stat_fash)
            .setContentTitle(GuestLocalReengagementScheduler.reminderTitle(context))
            .setContentText(GuestLocalReengagementScheduler.reminderBody(context))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(GuestLocalReengagementScheduler.reminderBody(context)),
            )
            .setContentIntent(contentPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(
            GuestLocalReengagementScheduler.NOTIFICATION_ID,
            notification,
        )
        GuestLocalReengagementScheduler.markFiredToday(context)
        GuestLocalReengagementScheduler.scheduleAfterBackground(context)
    }

    companion object {
        private const val TAG = "GuestLocalReminderRx"
    }
}
