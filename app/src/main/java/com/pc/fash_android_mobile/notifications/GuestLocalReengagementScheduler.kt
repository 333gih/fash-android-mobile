package com.pc.fash_android_mobile.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import com.pc.fash_android_mobile.R
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Schedules a single local notification for guest browse users after ~24h inactive.
 * See core-service/docs/guest-local-reminder.md.
 */
object GuestLocalReengagementScheduler {

    private const val TAG = "GuestLocalReminder"
    private const val PREFS = "guest_reengagement"
    private const val KEY_LAST_FIRED_DAY = "last_fired_day_vn"
    private const val KEY_REMINDER_TITLE = "reminder_title"
    private const val KEY_REMINDER_BODY = "reminder_body"
    private const val KEY_SESSION_COUNT = "browse_session_count"
    private const val KEY_NUDGE_LAST_SHOWN_MS = "signup_nudge_last_shown_ms"
    private const val KEY_PERMISSION_PROMPTED = "notif_permission_prompted"
    private const val KEY_GUEST_ACTIVE = "guest_session_active"

    const val ACTION_FIRE = "com.pc.fash_android_mobile.GUEST_LOCAL_REMINDER_FIRE"
    const val ACTION_OPEN_GUEST_HOME = "com.pc.fash_android_mobile.GUEST_REENGAGEMENT"
    const val REQUEST_CODE_ALARM = 88001
    const val NOTIFICATION_ID = 88001

    private val vnZone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
    private val inactiveMs = TimeUnit.HOURS.toMillis(24)
    private val nudgeCooldownMs = TimeUnit.DAYS.toMillis(7)

    fun onGuestShellEntered(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val next = prefs.getInt(KEY_SESSION_COUNT, 0) + 1
        prefs.edit {
            putInt(KEY_SESSION_COUNT, next)
            putBoolean(KEY_GUEST_ACTIVE, true)
        }
        return next
    }

    fun shouldShowSignupNudge(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_SESSION_COUNT, 0) < 2) return false
        val last = prefs.getLong(KEY_NUDGE_LAST_SHOWN_MS, 0L)
        return System.currentTimeMillis() - last >= nudgeCooldownMs
    }

    fun markSignupNudgeShown(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putLong(KEY_NUDGE_LAST_SHOWN_MS, System.currentTimeMillis())
        }
    }

    fun wasNotificationPermissionPrompted(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PERMISSION_PROMPTED, false)

    fun markNotificationPermissionPrompted(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_PERMISSION_PROMPTED, true)
        }
    }

    fun updateReminderCopy(context: Context, title: String?, body: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            if (!title.isNullOrBlank()) putString(KEY_REMINDER_TITLE, title.trim())
            if (!body.isNullOrBlank()) putString(KEY_REMINDER_BODY, body.trim())
        }
    }

    fun reminderTitle(context: Context): String {
        val cached = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_REMINDER_TITLE, null)?.trim().orEmpty()
        return cached.ifEmpty { context.getString(R.string.guest_local_reminder_title) }
    }

    fun reminderBody(context: Context): String {
        val cached = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_REMINDER_BODY, null)?.trim().orEmpty()
        return cached.ifEmpty { context.getString(R.string.guest_local_reminder_body) }
    }

    fun scheduleAfterBackground(context: Context) {
        if (!FashNotificationChannels.areNotificationsEnabled(context)) {
            Log.d(TAG, "skip schedule — notifications disabled")
            return
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // After reboot / package replace, only re-arm if a guest session was active recently.
        if (!prefs.getBoolean(KEY_GUEST_ACTIVE, false)) {
            Log.d(TAG, "skip schedule — no active guest session flag")
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + inactiveMs
        val pi = alarmPendingIntent(context)
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    // Prefer exact-while-idle when the OS allows it; fall back to inexact (Play-safe).
                    try {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                    } catch (_: SecurityException) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                    }
                }
                else -> alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
            Log.d(TAG, "scheduled guest reminder in 24h")
        } catch (e: SecurityException) {
            Log.w(TAG, "failed to schedule alarm", e)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(alarmPendingIntent(context))
    }

    fun clearGuestState(context: Context) {
        cancel(context)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            remove(KEY_REMINDER_TITLE)
            remove(KEY_REMINDER_BODY)
            putBoolean(KEY_GUEST_ACTIVE, false)
        }
    }

    fun canFireToday(context: Context): Boolean {
        val today = LocalDate.now(vnZone).toString()
        val last = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_FIRED_DAY, null)
        return last != today
    }

    fun markFiredToday(context: Context) {
        val today = LocalDate.now(vnZone).toString()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_LAST_FIRED_DAY, today)
        }
    }

    private fun alarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GuestLocalReengagementReceiver::class.java).apply {
            action = ACTION_FIRE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
