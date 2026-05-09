package com.pc.fash_android_mobile.data.advertising

import org.json.JSONObject

/** Wire DTO for `GET /api/v1/app/advertising/slides`. */
data class AppAdvertisingSlidesResponse(
    val placementKey: String,
    val items: List<AppAdvertisingSlideItem>,
)

data class AppAdvertisingSlideItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val bannerImageUrl: String,
    val stylePreset: String,
    val contentType: String,
    val advertiserScope: String,
    val partnerDisclosure: String,
    val badgeLabel: String,
    val navigationType: String,
    val navigationPayload: String,
)

fun parseAppAdvertisingSlidesResponse(raw: String): AppAdvertisingSlidesResponse {
    val root = JSONObject(raw.trim())
    val placement = root.optString("placement_key", "promo_slider_main")
    val arr = root.optJSONArray("items") ?: return AppAdvertisingSlidesResponse(placement, emptyList())
    val items = buildList {
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val nav = o.optJSONObject("navigation")
            val navType = nav?.optString("type").orEmpty().ifEmpty { o.optString("navigation_type", "none") }
            val navPayload = nav?.optString("payload").orEmpty().ifEmpty { o.optString("navigation_payload") }
            add(
                AppAdvertisingSlideItem(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    subtitle = o.optString("subtitle"),
                    bannerImageUrl = o.optString("banner_image_url"),
                    stylePreset = o.optString("style_preset", "gradient_primary"),
                    contentType = o.optString("content_type", "announcement"),
                    advertiserScope = o.optString("advertiser_scope", "platform"),
                    partnerDisclosure = o.optString("partner_disclosure"),
                    badgeLabel = o.optString("badge_label"),
                    navigationType = navType,
                    navigationPayload = navPayload,
                ),
            )
        }
    }
    return AppAdvertisingSlidesResponse(placement, items)
}
