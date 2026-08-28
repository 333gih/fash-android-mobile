package com.pc.fash_android_mobile.data.recommendation

import android.content.Intent
import java.util.Locale

/** Reports `notification_open` feed events from FCM / in-app notification payloads. */
object NotificationEngagementReporter {
    fun reportOpen(reporter: FeedEventReporter, data: Map<String, String>?) {
        val listingId = firstString(data, "listing_id", "listingId")
        val scenarioId = firstString(data, "scenario_id", "scenarioId", "experiment_id")
        reporter.notificationOpen(listingId = listingId, scenarioId = scenarioId)
    }

    fun reportOpenFromIntent(reporter: FeedEventReporter, intent: Intent?) {
        if (intent == null || !isNotificationLaunchIntent(intent)) return
        reportOpen(reporter, notificationDataFromIntent(intent))
    }

    fun isNotificationLaunchIntent(intent: Intent): Boolean {
        if (!intent.getStringExtra(EXTRA_NOTIFICATION_ID).isNullOrBlank()) return true
        if (!intent.getStringExtra(EXTRA_NOTIFICATION_LISTING_ID).isNullOrBlank()) return true
        if (!intent.getStringExtra(EXTRA_NOTIFICATION_SCENARIO_ID).isNullOrBlank()) return true
        if (!intent.getStringExtra("scenario_id").isNullOrBlank()) return true
        val deepLink = intent.getStringExtra("deep_link") ?: intent.dataString
        return deepLink?.contains("inbox/", ignoreCase = true) == true
    }

    fun attachEngagementExtras(intent: Intent, data: Map<String, String>) {
        firstString(data, "listing_id", "listingId")?.let {
            intent.putExtra(EXTRA_NOTIFICATION_LISTING_ID, it)
        }
        firstString(data, "scenario_id", "scenarioId")?.let {
            intent.putExtra(EXTRA_NOTIFICATION_SCENARIO_ID, it)
        }
        firstString(data, "type", "payload_type")?.let {
            intent.putExtra("notification_payload_type", it)
        }
    }

    private fun notificationDataFromIntent(intent: Intent): Map<String, String> {
        val out = mutableMapOf<String, String>()
        intent.getStringExtra(EXTRA_NOTIFICATION_LISTING_ID)?.trim()?.takeIf { it.isNotEmpty() }?.let {
            out["listing_id"] = it
        }
        intent.getStringExtra(EXTRA_NOTIFICATION_SCENARIO_ID)?.trim()?.takeIf { it.isNotEmpty() }?.let {
            out["scenario_id"] = it
        }
        intent.getStringExtra("scenario_id")?.trim()?.takeIf { it.isNotEmpty() }?.let {
            out["scenario_id"] = it
        }
        intent.getStringExtra("notification_payload_type")?.trim()?.takeIf { it.isNotEmpty() }?.let {
            out["payload_type"] = it
        }
        return out
    }

    private fun firstString(data: Map<String, String>?, vararg keys: String): String? {
        if (data == null) return null
        val byLower = data.entries.associate { it.key.lowercase(Locale.ROOT) to it.value }
        for (key in keys) {
            val value = byLower[key.lowercase(Locale.ROOT)]?.trim().orEmpty()
            if (value.isNotEmpty()) return value
        }
        return null
    }

    const val EXTRA_NOTIFICATION_ID = "notification_id"
    const val EXTRA_NOTIFICATION_LISTING_ID = "notification_listing_id"
    const val EXTRA_NOTIFICATION_SCENARIO_ID = "notification_scenario_id"
}
