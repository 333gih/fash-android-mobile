package com.pc.fash_android_mobile.data.promo

import org.json.JSONObject

/** FCM / Kafka / WebSocket data map type for admin interstitial campaigns. */
const val ADMIN_APP_PROMO_PAYLOAD_TYPE = "admin.app_promo_interstitial"

fun isAppPromoPushData(data: Map<String, String>?): Boolean {
    if (data.isNullOrEmpty()) return false
    return data["type"]?.trim().equals(ADMIN_APP_PROMO_PAYLOAD_TYPE, ignoreCase = true) == true ||
        !data["promo_payload"].isNullOrBlank()
}

/**
 * Parses full admin JSON when present; otherwise builds a minimal campaign from flat keys
 * (backup when [promo_payload] was truncated in transit).
 */
fun parseAppPromoFromPushData(
    data: Map<String, String>,
    fallbackTitle: String? = null,
    fallbackBody: String? = null,
): AppPromoCampaign? {
    val raw = data["promo_payload"]?.trim().orEmpty()
    if (raw.isNotEmpty()) {
        runCatching {
            parseRemoteAppPromoPayload(JSONObject(raw))?.toAppPromoCampaign()
        }.getOrNull()?.let { return it }
    }
    val id = data["campaign_id"]?.trim().orEmpty()
    val title = data["title"]?.trim().orEmpty().ifBlank { fallbackTitle?.trim().orEmpty() }
    val body = data["body"]?.trim()
        ?: data["detail_body"]?.trim()
        ?: fallbackBody?.trim()
        .orEmpty()
    if (id.isBlank() || title.isBlank() || body.isBlank()) return null
    val version = data["campaign_version"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
    return AppPromoCampaign(
        id = id,
        version = version,
        kind = AppPromoCampaignKind.Remote,
        remoteTitle = title,
        remoteMessage = body,
        remotePrimaryLabel = title,
        primaryAction = AppPromoButtonAction(type = "none", payload = ""),
    )
}
