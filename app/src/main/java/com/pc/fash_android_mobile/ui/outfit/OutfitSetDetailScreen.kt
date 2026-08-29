package com.pc.fash_android_mobile.ui.outfit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.recommendation.OutfitSetCard
import com.pc.fash_android_mobile.data.recommendation.OutfitSetItem
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitSetDetailScreen(
    set: OutfitSetCard,
    onBack: () -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = FashTheme.spacing
    val title = set.title.ifBlank { stringResource(R.string.outfit_set_detail_title) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title.take(48),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { inner ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = spacing.editorialStart,
                end = spacing.editorialEnd,
                top = spacing.spacing3,
                bottom = spacing.spacing4,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (set.reasonLabel.isNotBlank()) {
                        Text(
                            text = set.reasonLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = stringResource(R.string.outfit_set_detail_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(set.items, key = { it.listingId }) { item ->
                OutfitSetItemCard(
                    item = item,
                    onClick = { onItemClick(item.listingId) },
                )
            }
        }
    }
}

@Composable
private fun OutfitSetItemCard(
    item: OutfitSetItem,
    onClick: () -> Unit,
) {
    val spacing = FashTheme.spacing
    Surface(
        shape = RoundedCornerShape(spacing.radiusCard),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FashAsyncImage(
                model = item.coverImageUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(spacing.radiusSoftMin)),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = outfitSlotLabel(item.slotRole),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = FashColors.Primary,
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.priceVnd > 0L) {
                Text(
                    text = formatOutfitPriceVnd(item.priceVnd),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.hasRealBadge) {
                    Text(
                        text = stringResource(R.string.outfit_set_item_real_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (item.hasExploreBoost) {
                    Text(
                        text = stringResource(R.string.outfit_set_item_boost),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun outfitSlotLabel(role: String): String {
    return when (role.lowercase(Locale.ROOT)) {
        "top" -> stringResource(R.string.outfit_slot_top)
        "bottom" -> stringResource(R.string.outfit_slot_bottom)
        "shoes" -> stringResource(R.string.outfit_slot_shoes)
        "dress" -> stringResource(R.string.outfit_slot_dress)
        "outer" -> stringResource(R.string.outfit_slot_outer)
        "bag", "accessory" -> stringResource(R.string.outfit_slot_bag)
        else -> stringResource(R.string.outfit_slot_item)
    }
}

private fun formatOutfitPriceVnd(vnd: Long): String {
    val fmt = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${fmt.format(vnd)} ₫"
}
