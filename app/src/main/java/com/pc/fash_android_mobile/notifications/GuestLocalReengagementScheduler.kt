package com.pc.fash_android_mobile.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import com.pc.fash_android_mobile.ui.notifications.NotificationExploreNavigation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Schedules local notifications for guest browse users with copy rotation and up to 2/day (device local timezone).
 * See core-service/docs/guest-local-reminder.md.
 */
object GuestLocalReengagementScheduler {

    private const val TAG = "GuestLocalReminder"
    private const val PREFS = "guest_reengagement"
    private const val KEY_LAST_FIRED_DAY = "last_fired_day_vn"
    private const val KEY_FIRED_COUNT = "fired_count_today"
    private const val KEY_FIRED_COUNT_DAY = "fired_count_day_vn"
    private const val KEY_REMINDER_TITLE = "reminder_title"
    private const val KEY_REMINDER_BODY = "reminder_body"
    private const val KEY_SESSION_COUNT = "browse_session_count"
    private const val KEY_NUDGE_LAST_SHOWN_MS = "signup_nudge_last_shown_ms"
    private const val KEY_PERMISSION_PROMPTED = "notif_permission_prompted"
    private const val KEY_GUEST_ACTIVE = "guest_session_active"

    const val ACTION_FIRE = "com.pc.fash_android_mobile.GUEST_LOCAL_REMINDER_FIRE"
    const val ACTION_FIRE_EVENING = "com.pc.fash_android_mobile.GUEST_LOCAL_REMINDER_EVENING"
    const val ACTION_OPEN_GUEST_HOME = "com.pc.fash_android_mobile.GUEST_REENGAGEMENT"
    const val REQUEST_CODE_ALARM = 88001
    const val REQUEST_CODE_ALARM_EVENING = 88002
    const val NOTIFICATION_ID = 88001
    const val NOTIFICATION_ID_EVENING = 88002

    private val vnZone: ZoneId = ZoneId.systemDefault()
    private val inactiveMs = TimeUnit.HOURS.toMillis(24)
    private val nudgeCooldownMs = TimeUnit.DAYS.toMillis(7)
    private const val MAX_DAILY_REMINDERS = 2
    private const val EVENING_HOUR_LOCAL = 20

    private data class ReminderVariant(
        val titleVi: String,
        val bodyVi: String,
        val titleEn: String,
        val bodyEn: String,
        val action: String,
        val exploreSurface: String? = null,
        val seasonLabelVi: String? = null,
        val seasonLabelEn: String? = null,
    )

    private val variants = listOf(
        ReminderVariant(
            "Fash đang chờ bạn",
            "Khám phá thêm đồ second-hand — đăng ký để nhận gợi ý riêng mỗi ngày.",
            "Fash is waiting for you",
            "Discover more pre-loved fashion — sign up for daily picks made for you.",
            NotificationExploreNavigation.GUEST_ACTION_OPEN_HOME_SIGNUP,
        ),
        ReminderVariant(
            "Style mới vừa lên kệ",
            "Xem bộ sưu tập pre-loved hôm nay — mở Fash không cần đăng nhập.",
            "Fresh pre-loved drops",
            "Browse today's curated second-hand picks — no login required.",
            NotificationExploreNavigation.GUEST_ACTION_OPEN_EXPLORE,
            exploreSurface = "explore",
        ),
        ReminderVariant(
            "Mùa này mặc gì?",
            "Gợi ý outfit second-hand phù hợp khí hậu VN — khám phá ngay trên Fash.",
            "What to wear this season?",
            "Climate-friendly pre-loved outfit ideas are waiting on Fash.",
            NotificationExploreNavigation.GUEST_ACTION_OPEN_EXPLORE,
            exploreSurface = "seasonal_near_you",
            seasonLabelVi = "Mùa này",
            seasonLabelEn = "This season",
        ),
        ReminderVariant(
            "Lưu món yêu thích",
            "Đăng ký miễn phí để lưu listing và nhận thông báo giảm giá.",
            "Save what you love",
            "Sign up free to save listings and get price-drop alerts.",
            NotificationExploreNavigation.GUEST_ACTION_OPEN_HOME_SIGNUP,
        ),
        ReminderVariant(
            "Cộng đồng Fash đang sôi động",
            "Người bán C2C đang đăng hàng mới — ghé xem trước khi hết size.",
            "Fash community is buzzing",
            "C2C sellers just listed new pieces — browse before they're gone.",
            NotificationExploreNavigation.GUEST_ACTION_OPEN_EXPLORE,
            exploreSurface = "explore",
        ),
        ReminderVariant(
            "Deal second-hand hôm nay",
            "Món đẹp, giá tốt — mở Fash khám phá kho pre-loved gần bạn.",
            "Today's pre-loved deals",
            "Great style, better prices — explore pre-loved near you on Fash.",
            NotificationExploreNavigation.GUEST_ACTION_OPEN_EXPLORE,
            exploreSurface = "explore",
        ),
    )

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

    fun pickVariant(context: Context): Pair<String, String> {
        val v = selectedVariant(context)
        val en = Locale.getDefault().language.startsWith("en")
        return if (en) v.titleEn to v.bodyEn else v.titleVi to v.bodyVi
    }

    fun guestOpenPayload(context: Context): Map<String, String> {
        val v = selectedVariant(context)
        val en = Locale.getDefault().language.startsWith("en")
        val out = linkedMapOf(NotificationExploreNavigation.GUEST_ACTION_KEY to v.action)
        v.exploreSurface?.trim()?.takeIf { it.isNotEmpty() }?.let {
            out[NotificationExploreNavigation.GUEST_EXPLORE_SURFACE_KEY] = it
        }
        val seasonLabel = if (en) v.seasonLabelEn else v.seasonLabelVi
        seasonLabel?.trim()?.takeIf { it.isNotEmpty() }?.let {
            out[NotificationExploreNavigation.GUEST_EXPLORE_SEASON_LABEL_KEY] = it
        }
        out["scenario_id"] = "guest_local"
        out[com.pc.fash_android_mobile.data.recommendation.NotificationEngagementReporter.EXTRA_NOTIFICATION_SCENARIO_ID] = "guest_local"
        return out
    }

    private fun selectedVariant(context: Context): ReminderVariant {
        val cachedTitle = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_REMINDER_TITLE, null)?.trim().orEmpty()
        val cachedBody = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_REMINDER_BODY, null)?.trim().orEmpty()
        if (cachedTitle.isNotEmpty() && cachedBody.isNotEmpty()) {
            return variants.firstOrNull { it.titleVi == cachedTitle || it.titleEn == cachedTitle }
                ?: variants.first()
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val session = prefs.getInt(KEY_SESSION_COUNT, 0)
        val hour = LocalDateTime.now(vnZone).hour
        val day = LocalDate.now(vnZone).dayOfMonth
        val idx = (session + hour + day) % variants.size
        return variants[idx]
    }

    fun reminderTitle(context: Context): String = pickVariant(context).first

    fun reminderBody(context: Context): String = pickVariant(context).second

    fun scheduleAfterBackground(context: Context) {
        if (!FashNotificationChannels.areNotificationsEnabled(context)) {
            Log.d(TAG, "skip schedule — notifications disabled")
            return
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_GUEST_ACTIVE, false)) {
            Log.d(TAG, "skip schedule — no active guest session flag")
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + inactiveMs
        scheduleAlarm(context, alarmManager, triggerAt, REQUEST_CODE_ALARM, ACTION_FIRE)
        if (remainingToday(context) > 0) {
            scheduleEveningAlarm(context, alarmManager)
        }
        Log.d(TAG, "scheduled guest reminder in 24h + optional evening slot")
    }

    private fun scheduleEveningAlarm(context: Context, alarmManager: AlarmManager) {
        val now = LocalDateTime.now(vnZone)
        var fire = LocalDateTime.of(now.toLocalDate(), LocalTime.of(EVENING_HOUR_LOCAL, 0))
        if (!fire.isAfter(now)) {
            fire = fire.plusDays(1)
        }
        val triggerAt = fire.atZone(vnZone).toInstant().toEpochMilli()
        scheduleAlarm(context, alarmManager, triggerAt, REQUEST_CODE_ALARM_EVENING, ACTION_FIRE_EVENING)
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        triggerAt: Long,
        requestCode: Int,
        action: String,
    ) {
        val pi = alarmPendingIntent(context, requestCode, action)
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    try {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                    } catch (_: SecurityException) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                    }
                }
                else -> alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "failed to schedule alarm", e)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(alarmPendingIntent(context, REQUEST_CODE_ALARM, ACTION_FIRE))
        alarmManager.cancel(alarmPendingIntent(context, REQUEST_CODE_ALARM_EVENING, ACTION_FIRE_EVENING))
    }

    fun clearGuestState(context: Context) {
        cancel(context)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            remove(KEY_REMINDER_TITLE)
            remove(KEY_REMINDER_BODY)
            remove(KEY_FIRED_COUNT)
            remove(KEY_FIRED_COUNT_DAY)
            putBoolean(KEY_GUEST_ACTIVE, false)
        }
    }

    fun canFireToday(context: Context): Boolean = remainingToday(context) > 0

    private fun remainingToday(context: Context): Int {
        resetDailyCountIfNeeded(context)
        val count = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_FIRED_COUNT, 0)
        return (MAX_DAILY_REMINDERS - count).coerceAtLeast(0)
    }

    fun markFiredToday(context: Context) {
        resetDailyCountIfNeeded(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_FIRED_COUNT, 0) + 1
        val today = LocalDate.now(vnZone).toString()
        prefs.edit {
            putInt(KEY_FIRED_COUNT, count)
            putString(KEY_LAST_FIRED_DAY, today)
            putString(KEY_FIRED_COUNT_DAY, today)
        }
    }

    private fun resetDailyCountIfNeeded(context: Context) {
        val today = LocalDate.now(vnZone).toString()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_FIRED_COUNT_DAY, null) != today) {
            prefs.edit {
                putInt(KEY_FIRED_COUNT, 0)
                putString(KEY_FIRED_COUNT_DAY, today)
            }
        }
    }

    private fun alarmPendingIntent(context: Context, requestCode: Int, action: String): PendingIntent {
        val intent = Intent(context, GuestLocalReengagementReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
