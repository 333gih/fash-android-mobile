package com.pc.fash_android_mobile.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.data.promo.AppPromoCampaign
import com.pc.fash_android_mobile.data.promo.AppPromoCampaignKind
import com.pc.fash_android_mobile.data.promo.sanitizePromoDisplayString

/**
 * Resolved promo card content — drives which blocks render and hero sizing.
 * Keeps the overlay tight: no empty hero for text-only remote promos.
 */
internal data class AppPromoDialogLayout(
    val title: String,
    val message: String?,
    val primaryLabel: String,
    val secondaryLabel: String?,
    val badge: String?,
    val imageUrls: List<String>,
    val showImageHero: Boolean,
    val showIconHero: Boolean,
) {
    val showTitle: Boolean get() = title.isNotBlank()
    val showMessage: Boolean get() = !message.isNullOrBlank()
    val showBadgeOnHero: Boolean get() = !badge.isNullOrBlank() && (showImageHero || showIconHero)
    val showBadgeInline: Boolean get() = !badge.isNullOrBlank() && !showBadgeOnHero
    val showSecondary: Boolean get() = !secondaryLabel.isNullOrBlank()

    fun heroHeight(cardWidth: Dp): Dp? = when {
        showImageHero -> (cardWidth * IMAGE_ASPECT).coerceIn(MIN_IMAGE_HERO, MAX_IMAGE_HERO)
        showIconHero -> ICON_HERO
        else -> null
    }

    companion object {
        private val MIN_IMAGE_HERO = 120.dp
        private val MAX_IMAGE_HERO = 200.dp
        private val ICON_HERO = 88.dp
        private const val IMAGE_ASPECT = 0.52f

        fun from(
            campaign: AppPromoCampaign,
            title: String,
            message: String,
            primaryLabel: String,
            secondaryLabel: String?,
            badge: String?,
        ): AppPromoDialogLayout {
            val images = campaign.remoteImageUrls.map { it.trim() }.filter { it.isNotEmpty() }
            val showImageHero = campaign.kind == AppPromoCampaignKind.Remote && images.isNotEmpty()
            val showIconHero = !showImageHero && campaign.kind != AppPromoCampaignKind.Remote
            return AppPromoDialogLayout(
                title = title.trim(),
                message = message.trim().takeIf { it.isNotEmpty() },
                primaryLabel = primaryLabel.trim(),
                secondaryLabel = sanitizePromoDisplayString(secondaryLabel),
                badge = sanitizePromoDisplayString(badge),
                imageUrls = images,
                showImageHero = showImageHero,
                showIconHero = showIconHero,
            )
        }
    }
}
