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
)

fun parseRemoteAppPromoPayload(json: JSONObject): RemoteAppPromoPayload? {
    val id = json.optString("id", "").trim()
    if (id.isBlank()) return null
    val version = json.optInt("version", 1).coerceAtLeast(1)
    val title = json.optString("title", "").trim()
    val description = json.optString("description", "").trim()
    if (title.isBlank() || description.isBlank()) return null
    val images = json.optJSONArray("image_urls")?.toStringList()
        ?: json.optJSONArray("imageUrls")?.toStringList()
        ?: emptyList()
    val primaryObj = json.optJSONObject("primary_button")
        ?: json.optJSONObject("primaryButton")
    val primaryLabel = primaryObj?.optString("label", "")?.trim()
        ?: json.optString("primary_button_label", "").trim()
    if (primaryLabel.isNullOrBlank()) return null
    val primaryAction = primaryObj?.optJSONObject("action")?.toButtonAction()
        ?: AppPromoButtonAction(
            type = json.optString("primary_button_action_type", "none"),
            payload = json.optString("primary_button_payload", ""),
        )
    val secondaryObj = json.optJSONObject("secondary_button")
        ?: json.optJSONObject("secondaryButton")
    val secondaryLabel = secondaryObj?.optString("label", "")?.trim()
        ?: json.optString("secondary_button_label", "").trim().ifBlank { null }
    val secondaryAction = secondaryObj?.optJSONObject("action")?.toButtonAction()
    return RemoteAppPromoPayload(
        id = id,
        version = version,
        title = title,
        description = description,
        imageUrls = images,
        badgeLabel = json.optString("badge_label", "").trim().ifBlank { null },
        primaryButtonLabel = primaryLabel,
        primaryAction = primaryAction,
        secondaryButtonLabel = secondaryLabel,
        secondaryAction = secondaryAction,
        priority = json.optInt("priority", 0),
        scheduleType = json.optString("schedule_type", "").ifBlank { null },
    )
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
