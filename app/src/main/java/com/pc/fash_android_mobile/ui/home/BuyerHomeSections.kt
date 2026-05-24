package com.pc.fash_android_mobile.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.components.FashBrandMarkText
import com.pc.fash_android_mobile.ui.theme.FashBrandTypography
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import com.pc.fash_android_mobile.ui.theme.editorialHorizontalPadding
private fun formatJourneyCount(n: Int): String =
    when {
        n > 99 -> "99+"
        n < 0 -> "0"
        else -> n.toString()
    }

@Composable
fun HomeSizingBanner(
    onAddSizeClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    includeHorizontalEdgePadding: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin)
    val dismissCd = stringResource(R.string.home_sizing_banner_dismiss_cd)
    val edgeStart = if (includeHorizontalEdgePadding) FashTheme.spacing.editorialStart else 0.dp
    val edgeEnd = if (includeHorizontalEdgePadding) FashTheme.spacing.editorialEnd else 0.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = edgeStart,
                end = edgeEnd,
                top = 4.dp,
                bottom = 8.dp,
            )
            .clip(shape)
            .background(scheme.primaryContainer.copy(alpha = 0.45f))
            .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.home_sizing_banner_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface,
            )
            Text(
                text = stringResource(R.string.home_sizing_banner_body),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onAddSizeClick,
                modifier = Modifier.padding(top = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FashColors.Primary),
            ) {
                Text(stringResource(R.string.home_sizing_banner_cta))
            }
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.semantics { contentDescription = dismissCd },
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun BuyerHomeJourneyRow(
    stats: BuyerHomeStats,
    onDeliveringClick: () -> Unit,
    onSavedClick: () -> Unit,
    onInReviewClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (compact) {
        BuyerHomeJourneyCompactBar(
            stats = stats,
            onDeliveringClick = onDeliveringClick,
            onSavedClick = onSavedClick,
            onInReviewClick = onInReviewClick,
            modifier = modifier,
        )
        return
    }
    var titleVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { titleVisible = true }
    val titleAlpha by animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "journeyTitleAlpha",
    )

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
            modifier = Modifier.graphicsLayer { alpha = titleAlpha },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StaggeredEntrance(index = 0, modifier = Modifier.weight(1f)) {
                JourneyStatCard(
                    icon = {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = FashColors.Primary,
                        )
                    },
                    label = stringResource(R.string.home_journey_delivering),
                    value = formatJourneyCount(stats.activeDeliveryOrders),
                    onClick = onDeliveringClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            StaggeredEntrance(index = 1, modifier = Modifier.weight(1f)) {
                JourneyStatCard(
                    icon = {
                        Icon(
                            Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = FashColors.Primary,
                        )
                    },
                    label = stringResource(R.string.home_journey_saved),
                    value = formatJourneyCount(stats.savedListingsCount),
                    onClick = onSavedClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            StaggeredEntrance(index = 2, modifier = Modifier.weight(1f)) {
                JourneyStatCard(
                    icon = {
                        Icon(
                            Icons.Default.RateReview,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = FashColors.Primary,
                        )
                    },
                    label = stringResource(R.string.home_journey_in_review),
                    value = formatJourneyCount(stats.listingsInReviewCount),
                    onClick = onInReviewClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Slim journey chips — icon, label, count in one horizontal row (no section title). */
@Composable
fun BuyerHomeJourneyCompactBar(
    stats: BuyerHomeStats,
    onDeliveringClick: () -> Unit,
    onSavedClick: () -> Unit,
    onInReviewClick: () -> Unit,
    modifier: Modifier = Modifier,
    includeHorizontalEdgePadding: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val edgeStart = if (includeHorizontalEdgePadding) FashTheme.spacing.editorialStart else 0.dp
    val edgeEnd = if (includeHorizontalEdgePadding) FashTheme.spacing.editorialEnd else 0.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = edgeStart,
                end = edgeEnd,
                top = 6.dp,
                bottom = 4.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        JourneyCompactChip(
            icon = {
                Icon(
                    Icons.Default.LocalShipping,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = FashColors.Primary,
                )
            },
            label = stringResource(R.string.home_journey_delivering),
            value = formatJourneyCount(stats.activeDeliveryOrders),
            onClick = onDeliveringClick,
            modifier = Modifier.weight(1f),
        )
        JourneyCompactChip(
            icon = {
                Icon(
                    Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = FashColors.Primary,
                )
            },
            label = stringResource(R.string.home_journey_saved),
            value = formatJourneyCount(stats.savedListingsCount),
            onClick = onSavedClick,
            modifier = Modifier.weight(1f),
        )
        JourneyCompactChip(
            icon = {
                Icon(
                    Icons.Default.RateReview,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = FashColors.Primary,
                )
            },
            label = stringResource(R.string.home_journey_in_review),
            value = formatJourneyCount(stats.listingsInReviewCount),
            onClick = onInReviewClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun JourneyCompactChip(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(FashTheme.spacing.radiusPill)
    Row(
        modifier = modifier
            .clip(shape)
            .background(scheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface,
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
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin)
    Column(
        modifier = modifier
            .clip(shape)
            .background(scheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon()
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
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
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = horizontal.first,
                end = horizontal.second,
                top = 36.dp,
                bottom = 40.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            thickness = 1.dp,
            color = scheme.outlineVariant.copy(alpha = 0.35f),
        )
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
