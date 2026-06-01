package com.pc.fash_android_mobile.ui.listing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.ui.feed.ListingMasonryGrid
import com.pc.fash_android_mobile.ui.theme.FashTheme

/** PDP related listings — Explore-style 2-column masonry with per-card relation badge. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailProductDiscoveryHub(
    entries: List<ProductDiscoveryFeedEntry>,
    onItemClick: (String, String?) -> Unit,
    onLike: (ListingFeedItem) -> Unit,
    onSave: (ListingFeedItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) return

    val relationById = remember(entries) {
        entries.associate { it.item.id to it.relationLabel }
    }
    val columnAssignments = remember { mutableStateMapOf<String, Boolean>() }

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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (entries.any { it.relation == ProductDiscoveryRelation.SELLER }) {
                    RelationLegendChip(stringResource(R.string.product_relation_legend_seller))
                }
                if (entries.any { it.relation == ProductDiscoveryRelation.CATEGORY }) {
                    RelationLegendChip(stringResource(R.string.product_relation_legend_category))
                }
                if (entries.any { it.relation == ProductDiscoveryRelation.BRAND }) {
                    RelationLegendChip(stringResource(R.string.product_relation_legend_brand))
                }
                if (entries.any { it.relation == ProductDiscoveryRelation.STYLE }) {
                    RelationLegendChip(stringResource(R.string.product_relation_legend_style))
                }
            }
        }

        ListingMasonryGrid(
            items = entries.map { it.item },
            onLikeListing = onLike,
            onSaveListing = onSave,
            onListingClick = { item, _ -> onItemClick(item.id, item.sellerId) },
            onRecordView = { _, _ -> },
            onDwell = { _, _, _ -> },
            columnAssignments = columnAssignments,
            relationBadgeForItem = { item -> relationById[item.id] },
        )
    }
}

@Composable
private fun RelationLegendChip(label: String) {
    Surface(
        shape = RoundedCornerShape(FashTheme.spacing.radiusPill),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.65f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}
