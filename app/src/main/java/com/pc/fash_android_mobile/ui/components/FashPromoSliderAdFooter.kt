package com.pc.fash_android_mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.ui.theme.FashTheme

/** Gap between the promo carousel and the “Khám phá thời trang pre-loved” card. */
val FashPromoSliderToAdStripGap = 12.dp

/**
 * Pinned footer height (divider + slider + gap + ad strip), excluding navigation-bar inset on the ad strip.
 * Use for list [contentPadding] bottom inset on seller profile and similar screens.
 */
val FashPromoSliderAdFooterContentHeight =
    1.dp + 6.dp + FashPromoCarouselCardHeight + 4.dp + FashPromoSliderToAdStripGap + FashPromoCarouselCardHeight

/**
 * Compact bottom chrome for Orders / Notifications: divider → slider → gap → ad card
 * with the **same height as the promo carousel card** ([FashPromoCarouselCardHeight]).
 *
 * @param edgeToEdgeAdStrip When true, the pre-loved banner is full-bleed (no side inset).
 */
@Composable
fun FashPromoSliderAdFooter(
    modifier: Modifier = Modifier,
    onExploreClick: () -> Unit,
    slides: List<FashPromoSlideDef> = emptyList(),
    onSlideClick: (FashPromoSlideDef, Int) -> Unit = { _, _ -> },
    edgeToEdgeAdStrip: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLow),
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = scheme.outlineVariant.copy(alpha = 0.35f),
        )
        FashPromoSlider(
            modifier = Modifier.fillMaxWidth(),
            slides = slides,
            onSlideClick = onSlideClick,
            reportPendingPaymentAnchor = true,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = FashTheme.spacing.editorialStart,
            ),
        )
        Spacer(Modifier.height(FashPromoSliderToAdStripGap))
        FashBottomPromoAdStrip(
            modifier = Modifier.fillMaxWidth(),
            onExploreClick = onExploreClick,
            cardHeight = FashPromoCarouselCardHeight,
            edgeToEdge = edgeToEdgeAdStrip,
        )
    }
}
