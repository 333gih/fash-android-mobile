package com.pc.fash_android_mobile.data.promo

import org.json.JSONArray
import org.json.JSONObject

/** Deep link / in-app navigation from admin CMS. */
data class AppPromoButtonAction(
    val type: String,
    val payload: String,
)

/** Parsed admin / realtime / FCM promo interstitial payload. */
data class RemoteAppPromoPayload(
    val id: String,
    val version: Int,
    val title: String,
    val description: String,
    val imageUrls: List<String>,
    val badgeLabel: String?,
    val primaryButtonLabel: String,
    val primaryAction: AppPromoButtonAction,
    val secondaryButtonLabel: String?,
    val secondaryAction: AppPromoButtonAction?,
    val priority: Int,
    val scheduleType: String?,
    val maxShowsPerUser: Int?,
    val cooldownHours: Int?,
)

fun parseRemoteAppPromoPayload(json: JSONObject): RemoteAppPromoPayload? {
    val id = json.optString("id", "").trim()
        .ifBlank { json.optString("campaign_id", "").trim() }
    if (id.isBlank()) return null
    val version = json.optInt("version", 1).coerceAtLeast(1)
    val title = json.optString("title", "").trim()
    val description = json.optString("description", "").trim()
        .ifBlank { json.optString("body", "").trim() }
    if (title.isBlank() || description.isBlank()) return null
    val images = json.optJSONArray("image_urls")?.toStringList()
        ?: json.optJSONArray("imageUrls")?.toStringList()
        ?: emptyList()
    val primaryObj = json.optJSONObject("primary_button")
        ?: json.optJSONObject("primaryButton")
    val primaryLabel = primaryObj?.optStringOrNull("label")
        ?: json.optStringOrNull("primary_button_label", "primaryButtonLabel")
        ?: title.takeIf { it.isNotBlank() }
    if (primaryLabel.isNullOrBlank()) return null
    val primaryAction = primaryObj?.optJSONObject("action")?.toButtonAction()
        ?: AppPromoButtonAction(
            type = json.optString("primary_button_action_type", "none"),
            payload = json.optString("primary_button_payload", ""),
        )
    val secondaryObj = json.optJSONObject("secondary_button")
        ?: json.optJSONObject("secondaryButton")
    val secondaryLabel = secondaryObj?.optStringOrNull("label")
        ?: json.optStringOrNull("secondary_button_label", "secondaryButtonLabel")
    val secondaryAction = secondaryObj?.optJSONObject("action")?.toButtonAction()
    return RemoteAppPromoPayload(
        id = id,
        version = version,
        title = title,
        description = description,
        imageUrls = images,
        badgeLabel = json.optStringOrNull("badge_label", "badgeLabel"),
        primaryButtonLabel = primaryLabel,
        primaryAction = primaryAction,
        secondaryButtonLabel = secondaryLabel,
        secondaryAction = secondaryAction,
        priority = json.optInt("priority", 0),
        scheduleType = json.optStringOrNull("schedule_type", "scheduleType"),
        maxShowsPerUser = json.optIntOrNull("max_shows_per_user", "maxShowsPerUser"),
        cooldownHours = json.optIntOrNull("cooldown_hours", "cooldownHours"),
    )
}

/**
 * [JSONObject.optString] returns the literal `"null"` when the JSON value is `null` ([JSONObject.NULL]).
 * Admin sends `badge_label: null` when optional — must not render that on the promo card.
 */
internal fun JSONObject.optIntOrNull(vararg keys: String): Int? {
    for (key in keys) {
        if (!has(key)) continue
        when (val raw = opt(key)) {
            null, JSONObject.NULL -> return null
            is Number -> return raw.toInt()
            is String -> return raw.trim().toIntOrNull()
        }
    }
    return null
}

internal fun JSONObject.optStringOrNull(vararg keys: String): String? {
    for (key in keys) {
        if (!has(key)) continue
        when (val raw = opt(key)) {
            null, JSONObject.NULL -> return null
            is String -> {
                val s = raw.trim()
                if (s.isNotEmpty() && !s.equals("null", ignoreCase = true)) return s
            }
            else -> {
                val s = raw.toString().trim()
                if (s.isNotEmpty() && !s.equals("null", ignoreCase = true)) return s
            }
        }
    }
    return null
}

/** Normalizes values already stored on [AppPromoCampaign] before UI. */
internal fun sanitizePromoDisplayString(value: String?): String? {
    val s = value?.trim().orEmpty()
    if (s.isEmpty() || s.equals("null", ignoreCase = true)) return null
    return s
}

fun RemoteAppPromoPayload.toAppPromoCampaign(): AppPromoCampaign =
    AppPromoCampaign(
        id = id,
        version = version,
        kind = AppPromoCampaignKind.Remote,
        remoteTitle = title,
        remoteMessage = description,
        remoteImageUrls = imageUrls,
        remoteBadge = badgeLabel,
        remotePrimaryLabel = primaryButtonLabel,
        remoteSecondaryLabel = secondaryButtonLabel,
        primaryAction = primaryAction,
        secondaryAction = secondaryAction,
        priority = priority,
        scheduleType = scheduleType,
        maxShowsPerUser = maxShowsPerUser,
        cooldownHours = cooldownHours,
    )

private fun JSONArray.toStringList(): List<String> {
    val out = mutableListOf<String>()
    for (i in 0 until length()) {
        val s = optString(i, "").trim()
        if (s.isNotBlank()) out.add(s)
    }
    return out
}

private fun JSONObject.toButtonAction(): AppPromoButtonAction =
    AppPromoButtonAction(
        type = optString("type", "none"),
        payload = optString("payload", ""),
    )
