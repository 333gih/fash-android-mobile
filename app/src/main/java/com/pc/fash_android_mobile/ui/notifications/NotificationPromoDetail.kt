package com.pc.fash_android_mobile.ui.notifications

import com.pc.fash_android_mobile.data.promo.AppPromoCampaign
import com.pc.fash_android_mobile.data.promo.AppPromoCampaignKind
import com.pc.fash_android_mobile.data.promo.parseRemoteAppPromoPayload
import com.pc.fash_android_mobile.data.promo.toAppPromoCampaign
import com.pc.fash_android_mobile.data.user.InboxNotificationItem
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Inbox rows created from admin app promo interstitial dispatch ([admin.app_promo_interstitial]). */
fun isAppPromoInboxNotification(item: InboxNotificationItem): Boolean {
    val pt = item.payloadType?.trim()?.lowercase(Locale.ROOT).orEmpty()
    val dataType = firstStringFromDataCi(item.dataMap, "type")?.lowercase(Locale.ROOT).orEmpty()
    return pt == ADMIN_APP_PROMO_PAYLOAD_TYPE ||
        dataType == ADMIN_APP_PROMO_PAYLOAD_TYPE ||
        firstStringFromDataCi(item.dataMap, "promo_payload", "promoPayload") != null
}

/**
 * Rebuilds [AppPromoCampaign] from inbox `data.promo_payload` (FCM / ledger snapshot).
 * Falls back to nested `campaign` JSON or flat keys when present.
 */
fun parseAppPromoCampaignFromInbox(item: InboxNotificationItem): AppPromoCampaign? {
    val data = item.dataMap ?: return null
    parsePromoJson(firstStringFromDataCi(data, "promo_payload", "promoPayload"))
        ?.toAppPromoCampaign()
        ?.let { return it }

    val campaignRaw = data["campaign"]?.let { value ->
        when (value) {
            is String -> value.trim().takeIf { it.isNotEmpty() }
            else -> value?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        }
    }
    parsePromoJson(campaignRaw)?.toAppPromoCampaign()?.let { return it }

    val id = firstStringFromDataCi(data, "campaign_id", "campaignId", "id") ?: return null
    val title = firstStringFromDataCi(data, "title")?.takeIf { it.isNotBlank() } ?: item.title
    val description = firstStringFromDataCi(
        data,
        "description",
        "detail_body",
        "detailBody",
    )?.takeIf { it.isNotBlank() } ?: item.body
    if (title.isBlank() || description.isBlank()) return null

    val images = imageUrlsFromDataMap(data)
    val primaryLabel = firstStringFromDataCi(
        data,
        "primary_button_label",
        "primaryButtonLabel",
    ) ?: return null

    return AppPromoCampaign(
        id = id,
        version = firstStringFromDataCi(data, "campaign_version", "version")?.toIntOrNull() ?: 1,
        kind = AppPromoCampaignKind.Remote,
        remoteTitle = title,
        remoteMessage = description,
        remoteImageUrls = images,
        remoteBadge = firstStringFromDataCi(data, "badge_label", "badgeLabel"),
        remotePrimaryLabel = primaryLabel,
        remoteSecondaryLabel = firstStringFromDataCi(
            data,
            "secondary_button_label",
            "secondaryButtonLabel",
        ),
        primaryAction = null,
        secondaryAction = null,
    )
}

internal const val ADMIN_APP_PROMO_PAYLOAD_TYPE = "admin.app_promo_interstitial"

private fun parsePromoJson(raw: String?): com.pc.fash_android_mobile.data.promo.RemoteAppPromoPayload? {
    if (raw.isNullOrBlank()) return null
    return runCatching { parseRemoteAppPromoPayload(JSONObject(raw.trim())) }.getOrNull()
}

private fun imageUrlsFromDataMap(data: Map<String, Any?>): List<String> {
    val raw = data["image_urls"] ?: data["imageUrls"] ?: return emptyList()
    when (raw) {
        is JSONArray -> return raw.toUrlList()
        is String -> {
            val t = raw.trim()
            if (t.startsWith("[")) {
                return runCatching { JSONArray(t).toUrlList() }.getOrElse { emptyList() }
            }
            if (t.isNotBlank()) return listOf(t)
        }
    }
    return emptyList()
}

private fun JSONArray.toUrlList(): List<String> {
    val out = mutableListOf<String>()
    for (i in 0 until length()) {
        val s = optString(i, "").trim()
        if (s.isNotBlank()) out.add(s)
    }
    return out
}
