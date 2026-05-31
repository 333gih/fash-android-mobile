package com.pc.fash_android_mobile.data.user

import org.json.JSONObject

data class NotificationPreferences(
    val recommendationPushEnabled: Boolean,
    val recommendationEmailEnabled: Boolean,
    val quietHoursStart: Int?,
    val quietHoursEnd: Int?,
)

fun parseNotificationPreferencesJson(o: JSONObject): NotificationPreferences {
    val root = if (o.has("data") && o.opt("data") is JSONObject) {
        o.getJSONObject("data")
    } else {
        o
    }
    val quietStart = root.opt("quiet_hours_start").let { v ->
        when (v) {
            null, JSONObject.NULL -> null
            is Number -> v.toInt().coerceIn(0, 23)
            else -> null
        }
    }
    val quietEnd = root.opt("quiet_hours_end").let { v ->
        when (v) {
            null, JSONObject.NULL -> null
            is Number -> v.toInt().coerceIn(0, 23)
            else -> null
        }
    }
    return NotificationPreferences(
        recommendationPushEnabled = root.optBoolean("recommendation_push_enabled", true),
        recommendationEmailEnabled = root.optBoolean("recommendation_email_enabled", true),
        quietHoursStart = quietStart,
        quietHoursEnd = quietEnd,
    )
}

fun NotificationPreferences.toPutJson(): JSONObject = JSONObject().apply {
    put("recommendation_push_enabled", recommendationPushEnabled)
    put("recommendation_email_enabled", recommendationEmailEnabled)
    if (quietHoursStart != null) {
        put("quiet_hours_start", quietHoursStart)
    } else {
        put("quiet_hours_start", JSONObject.NULL)
    }
    if (quietHoursEnd != null) {
        put("quiet_hours_end", quietHoursEnd)
    } else {
        put("quiet_hours_end", JSONObject.NULL)
    }
}
