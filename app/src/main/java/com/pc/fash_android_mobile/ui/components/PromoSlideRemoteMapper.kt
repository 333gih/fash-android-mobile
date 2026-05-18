package com.pc.fash_android_mobile.ui.components

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.pc.fash_android_mobile.data.advertising.AppAdvertisingSlideItem
import com.pc.fash_android_mobile.data.promo.sanitizePromoDisplayString
import com.pc.fash_android_mobile.ui.theme.FashColors

/**
 * Maps core-service CMS slides to in-app promo card visuals.
 */
fun AppAdvertisingSlideItem.toFashPromoSlideDef(scheme: ColorScheme): FashPromoSlideDef {
    val gradient = when (stylePreset.trim()) {
        "gradient_warm" -> listOf(FashColors.SecondaryWarm, FashColors.TertiaryAccent)
        "gradient_neutral" -> listOf(scheme.surfaceContainerLow, scheme.surfaceVariant)
        else -> listOf(FashColors.PrimaryDeep, FashColors.Primary)
    }
    val border = if (stylePreset.trim() == "gradient_neutral") {
        scheme.outlineVariant.copy(alpha = 0.65f)
    } else {
        null
    }
    val navType = navigationType.trim().ifEmpty { "none" }
    val payload = navigationPayload.trim()
    val navigation = if (navType == "none") null else FashPromoNav(type = navType, payload = payload)
    return FashPromoSlideDef(
        id = id.trim().ifEmpty { "slide_${hashCode()}" },
        titleText = title.trim(),
        subtitleText = subtitle.trim(),
        titleRes = null,
        subtitleRes = null,
        gradient = gradient,
        border = border,
        badgeText = sanitizePromoDisplayString(badgeLabel),
        bannerImageUrl = bannerImageUrl.trim().ifEmpty { null },
        navigation = navigation,
    )
}
