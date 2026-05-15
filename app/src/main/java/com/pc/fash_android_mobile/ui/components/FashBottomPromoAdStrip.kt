package com.pc.fash_android_mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

private val PromoAdStripTopCornerRadius = 18.dp

/**
 * Bottom promo strip (title, subtitle, CTA) — shared by Orders, Notifications, Seller profile.
 *
 * @param cardHeight When set (e.g. [FashPromoCarouselCardHeight] from [FashPromoSliderAdFooter]),
 *   the card uses a fixed height matching the promo slider; otherwise it wraps content.
 * @param edgeToEdge Full-bleed width (no horizontal inset); background fills container side curves.
 *   Top corners stay rounded; bottom is square so the strip meets the screen edge cleanly.
 * @param extendToBottomEdge When true, applies [WindowInsets.navigationBars] as inner padding (non-[edgeToEdge] only).
 */
@Composable
fun FashBottomPromoAdStrip(
    modifier: Modifier = Modifier,
    onExploreClick: () -> Unit,
    cardHeight: Dp? = null,
    edgeToEdge: Boolean = false,
    extendToBottomEdge: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val horizontalMargin = FashTheme.spacing.editorialStart
    val cardShape = if (edgeToEdge) {
        RoundedCornerShape(
            topStart = PromoAdStripTopCornerRadius,
            topEnd = PromoAdStripTopCornerRadius,
        )
    } else {
        RoundedCornerShape(PromoAdStripTopCornerRadius)
    }
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val isCarouselHeight = cardHeight != null && cardHeight >= FashPromoCarouselCardHeight

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!edgeToEdge) {
                    Modifier
                        .padding(horizontal = horizontalMargin)
                        .padding(bottom = 8.dp)
                } else {
                    Modifier
                },
            )
            .then(cardHeight?.let { height ->
                if (edgeToEdge && navBarBottom > 0.dp) {
                    Modifier.height(height + navBarBottom)
                } else {
                    Modifier.height(height)
                }
            } ?: Modifier)
            .then(
                if (extendToBottomEdge && !edgeToEdge) {
                    Modifier.padding(WindowInsets.navigationBars.asPaddingValues())
                } else {
                    Modifier
                },
            ),
        shape = cardShape,
        color = scheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (cardHeight != null) Modifier.fillMaxHeight() else Modifier)
                .padding(
                    start = if (edgeToEdge) horizontalMargin else 12.dp,
                    end = if (edgeToEdge) horizontalMargin else 12.dp,
                    top = if (isCarouselHeight) 14.dp else 8.dp,
                    bottom = if (edgeToEdge) {
                        (if (isCarouselHeight) 14.dp else 8.dp) + navBarBottom
                    } else {
                        if (isCarouselHeight) 14.dp else 8.dp
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (isCarouselHeight) 4.dp else 2.dp),
            ) {
                Text(
                    text = stringResource(R.string.orders_ad_title),
                    style = if (isCarouselHeight) {
                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    } else {
                        MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    },
                    color = scheme.onSurface,
                    maxLines = if (isCarouselHeight) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.orders_ad_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = if (isCarouselHeight) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(
                onClick = onExploreClick,
                shape = RoundedCornerShape(percent = 50),
                contentPadding = PaddingValues(
                    horizontal = if (isCarouselHeight) 18.dp else 14.dp,
                    vertical = if (isCarouselHeight) 10.dp else 6.dp,
                ),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FashColors.Primary),
            ) {
                Text(
                    text = stringResource(R.string.orders_ad_cta),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }
        }
    }
}
