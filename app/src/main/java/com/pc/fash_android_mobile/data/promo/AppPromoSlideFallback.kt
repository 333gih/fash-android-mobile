package com.pc.fash_android_mobile.data.promo

import com.pc.fash_android_mobile.data.advertising.AppAdvertisingSlideItem

/**
 * When admin interstitial catalog is empty, derive a Shopee-style app-open dialog
 * from the first live promo carousel slide (`promo_slider_main`).
 */
object AppPromoSlideFallback {

    private const val ID_PREFIX = "promo_slide_"
    private const val VERSION = 1
    private const val PRIORITY = 50
    private const val MAX_SHOWS_PER_USER = 7
    private const val COOLDOWN_HOURS = 4

    fun fromSlide(slide: AppAdvertisingSlideItem): AppPromoCampaign? {
        val id = slide.id.trim()
        val title = slide.title.trim()
        val message = slide.subtitle.trim().ifEmpty { title }
        if (id.isEmpty() || title.isEmpty()) return null
        val images = slide.bannerImageUrl.trim().takeIf { it.isNotEmpty() }?.let { listOf(it) }.orEmpty()
        return AppPromoCampaign(
            id = "$ID_PREFIX$id",
            version = VERSION,
            kind = AppPromoCampaignKind.Remote,
            remoteTitle = title,
            remoteMessage = message,
            remoteImageUrls = images,
            remoteBadge = slide.badgeLabel.trim().takeIf { it.isNotEmpty() },
            remotePrimaryLabel = title,
            remoteSecondaryLabel = null,
            primaryAction = AppPromoButtonAction(
                type = slide.navigationType.ifBlank { "in_app_explore" },
                payload = slide.navigationPayload,
            ),
            priority = PRIORITY,
            scheduleType = "on_app_open",
            maxShowsPerUser = MAX_SHOWS_PER_USER,
            cooldownHours = COOLDOWN_HOURS,
        )
    }
}
