package com.pc.fash_android_mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.components.FashBrandMarkText
import com.pc.fash_android_mobile.ui.theme.FashBrandTypography
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import com.pc.fash_android_mobile.ui.theme.fashReadableOnGradient

private fun formatJourneyCount(n: Int): String =
    when {
        n > 99 -> "99+"
        n < 0 -> "0"
        else -> n.toString()
    }

@Composable
fun BuyerHomeJourneyRow(
    stats: BuyerHomeStats,
    onDeliveringClick: () -> Unit,
    onSavedClick: () -> Unit,
    onMessagesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = FashTheme.spacing.editorialStart,
                end = FashTheme.spacing.editorialEnd,
                top = 8.dp,
                bottom = 12.dp,
            ),
    ) {
        Text(
            text = stringResource(R.string.home_journey_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier
                .padding(top = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            JourneyStatCard(
                icon = { Icon(Icons.Default.LocalShipping, contentDescription = null, tint = FashColors.Primary) },
                label = stringResource(R.string.home_journey_delivering),
                value = formatJourneyCount(stats.activeDeliveryOrders),
                onClick = onDeliveringClick,
            )
            JourneyStatCard(
                icon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = FashColors.Primary) },
                label = stringResource(R.string.home_journey_saved),
                value = formatJourneyCount(stats.savedListingsCount),
                onClick = onSavedClick,
            )
            JourneyStatCard(
                icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = FashColors.Primary) },
                label = stringResource(R.string.home_journey_messages),
                value = formatJourneyCount(stats.unreadMessages),
                onClick = onMessagesClick,
            )
        }
    }
}

@Composable
private fun JourneyStatCard(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin)
    Column(
        modifier = modifier
            .widthIn(min = 108.dp, max = 132.dp)
            .clip(shape)
            .background(FashColors.SurfaceVariantCream)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon()
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun HomeHeroBanner(
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val heroTextColor = listOf(FashColors.PrimaryDeep, FashColors.Primary).fashReadableOnGradient()
    val heroSubtitleColor = heroTextColor.copy(alpha = 0.95f)
    val shape = RoundedCornerShape(FashTheme.spacing.radiusCard)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = FashTheme.spacing.editorialStart,
                vertical = 4.dp,
            )
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(FashColors.PrimaryDeep, FashColors.Primary),
                ),
            )
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.home_hero_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = heroTextColor,
            )
            Text(
                text = stringResource(R.string.home_hero_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = heroSubtitleColor,
            )
            OutlinedButton(
                onClick = onExploreClick,
                modifier = Modifier.padding(top = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = FashColors.SurfaceLowest,
                    contentColor = FashColors.Primary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.home_hero_cta),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

@Composable
fun HomeBrandFooterStrip(
    modifier: Modifier = Modifier,
    /** When false, only vertical padding is applied (e.g. parent already uses editorial horizontal inset). */
    includeHorizontalEdgePadding: Boolean = true,
) {
    val horizontal = if (includeHorizontalEdgePadding) {
        FashTheme.spacing.editorialStart to FashTheme.spacing.editorialEnd
    } else {
        0.dp to 0.dp
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = horizontal.first,
                end = horizontal.second,
                top = 28.dp,
                bottom = 24.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.home_brand_footer_sub),
            style = FashBrandTypography.marketplaceSubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
        )
        FashBrandMarkText(
            text = stringResource(R.string.home_brand_marketplace),
            style = FashBrandTypography.markBoldItalicSmall,
            modifier = Modifier.padding(top = 6.dp),
            textAlign = TextAlign.Center,
        )
    }
}
