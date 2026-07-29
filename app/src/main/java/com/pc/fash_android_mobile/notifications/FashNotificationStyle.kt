package com.pc.fash_android_mobile.notifications

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.pc.fash_android_mobile.R

/**
 * Shared notification branding: white hanger [ic_stat_fash] in the status bar (tinted with
 * [R.color.fash_brand]) and full-color logo [ic_launcher_brand] in the notification header.
 */
object FashNotificationStyle {

    fun builder(context: Context, channelId: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_fash)
            .setColor(ContextCompat.getColor(context, R.color.fash_brand))
            .setLargeIcon(
                BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_brand),
            )
}
