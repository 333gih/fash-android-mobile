package com.pc.fash_android_mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.ui.theme.FashTheme

/**
 * Pinned footer height (divider + slider), excluding navigation-bar inset.
 * Use for list [contentPadding] bottom inset on seller profile and similar screens.
 */
val FashPromoSliderAdFooterContentHeight =
    1.dp + 6.dp + FashPromoCarouselCardHeight + 4.dp

/**
 * Compact bottom chrome for Orders / Notifications: divider → CMS promo slider only.
 */
@Composable
fun FashPromoSliderAdFooter(
    modifier: Modifier = Modifier,
    slides: List<FashPromoSlideDef> = emptyList(),
    onSlideClick: (FashPromoSlideDef, Int) -> Unit = { _, _ -> },
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
    }
}
