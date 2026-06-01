package com.pc.fash_android_mobile.ui.listing

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingDetail
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.ui.feed.ListingGridCard
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

/** PDP related listings — one connected surface (seller, category, brand, style). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailProductDiscoveryHub(
    detail: ListingDetail,
    sellerLabel: String,
    sellerItems: List<ListingFeedItem>,
    categoryLabel: String?,
    categoryItems: List<ListingFeedItem>,
    brandLabel: String?,
    brandItems: List<ListingFeedItem>,
    styleItems: List<ListingFeedItem>,
    excludeId: String,
    onItemClick: (String, String?) -> Unit,
    onLike: (ListingFeedItem) -> Unit,
    onSave: (ListingFeedItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasAny = sellerItems.isNotEmpty() || categoryItems.isNotEmpty() ||
        brandItems.isNotEmpty() || styleItems.isNotEmpty()
    if (!hasAny) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = FashTheme.spacing.editorialStart),
        ) {
            Text(
                text = stringResource(R.string.product_discovery_hub_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.product_discovery_hub_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
            DiscoveryRelationChips(detail = detail)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = FashTheme.spacing.editorialStart)
                .padding(top = 4.dp),
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                FashColors.Primary,
                                FashColors.Primary.copy(alpha = 0.35f),
                            ),
                        ),
                    ),
            )
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f))
                    .padding(vertical = 8.dp),
            ) {
                var showsDivider = false
                if (sellerItems.isNotEmpty()) {
                    DiscoveryConnectedRail(
                        title = stringResource(R.string.product_more_from_seller, sellerLabel),
                        icon = Icons.Outlined.Storefront,
                        items = sellerItems,
                        excludeId = excludeId,
                        showsTopDivider = false,
                        onItemClick = onItemClick,
                        onLike = onLike,
                        onSave = onSave,
                    )
                    showsDivider = true
                }
                if (categoryItems.isNotEmpty()) {
                    val catTitle = categoryLabel?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.product_related_category_fallback)
                    DiscoveryConnectedRail(
                        title = stringResource(R.string.product_related_category, catTitle),
                        icon = Icons.Outlined.Category,
                        items = categoryItems,
                        excludeId = excludeId,
                        showsTopDivider = showsDivider,
                        onItemClick = onItemClick,
                        onLike = onLike,
                        onSave = onSave,
                    )
                    showsDivider = true
                }
                if (brandItems.isNotEmpty()) {
                    val brandTitle = brandLabel?.takeIf { it.isNotBlank() } ?: detail.brand.orEmpty()
                    if (brandTitle.isNotBlank()) {
                        DiscoveryConnectedRail(
                            title = stringResource(R.string.product_related_brand, brandTitle),
                            icon = Icons.Outlined.LocalOffer,
                            items = brandItems,
                            excludeId = excludeId,
                            showsTopDivider = showsDivider,
                            onItemClick = onItemClick,
                            onLike = onLike,
                            onSave = onSave,
                        )
                        showsDivider = true
                    }
                }
                if (styleItems.isNotEmpty()) {
                    DiscoveryConnectedRail(
                        title = stringResource(R.string.product_related_style),
                        icon = Icons.Outlined.AutoAwesome,
                        items = styleItems,
                        excludeId = excludeId,
                        showsTopDivider = showsDivider,
                        onItemClick = onItemClick,
                        onLike = onLike,
                        onSave = onSave,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoveryRelationChips(detail: ListingDetail) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        detail.category?.trim()?.takeIf { it.isNotEmpty() }?.let {
            DiscoveryRelationChip(label = it, icon = Icons.Outlined.Category)
        }
        detail.brand?.trim()?.takeIf { it.isNotEmpty() }?.let {
            DiscoveryRelationChip(label = it, icon = Icons.Outlined.LocalOffer)
        }
        val seller = detail.sellerUsername?.trim()?.takeIf { it.isNotEmpty() }
            ?: detail.sellerDisplayName?.trim()?.takeIf { it.isNotEmpty() }
        seller?.let {
            DiscoveryRelationChip(label = "@$it", icon = Icons.Outlined.Storefront)
        }
        detail.aestheticTagRefs.take(3).forEach { tag ->
            if (tag.label.isNotBlank()) {
                DiscoveryRelationChip(label = tag.label, icon = Icons.Outlined.AutoAwesome)
            }
        }
    }
}

@Composable
private fun DiscoveryRelationChip(label: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(FashTheme.spacing.radiusPill))
            .background(FashColors.Primary.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FashColors.Primary,
            modifier = Modifier.width(14.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = FashColors.Primary,
            maxLines = 1,
        )
    }
}

@Composable
private fun DiscoveryConnectedRail(
    title: String,
    icon: ImageVector,
    items: List<ListingFeedItem>,
    excludeId: String,
    showsTopDivider: Boolean,
    onItemClick: (String, String?) -> Unit,
    onLike: (ListingFeedItem) -> Unit,
    onSave: (ListingFeedItem) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        if (showsTopDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            )
        }
        DiscoverySectionTitle(
            title = title,
            icon = icon,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp, top = 4.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items.filter { it.id != excludeId }.forEach { item ->
                ListingGridCard(
                    item = item,
                    modifier = Modifier.width(132.dp),
                    compactFooter = true,
                    showQuickActions = true,
                    onLike = { onLike(item) },
                    onSave = { onSave(item) },
                    onClick = { onItemClick(item.id, item.sellerId) },
                )
            }
        }
    }
}

@Composable
private fun DiscoverySectionTitle(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = FashColors.Primary,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
