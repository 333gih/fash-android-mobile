package com.pc.fash_android_mobile.ui.listing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.recommendation.OutfitSetCard
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.theme.FashTheme

@Composable
fun CompleteTheLookSection(
    set: OutfitSetCard?,
    quotaHit: Boolean,
    onListingClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (set == null && !quotaHit) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.product_complete_the_look_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        if (quotaHit) {
            Text(
                text = stringResource(R.string.product_complete_the_look_quota),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            return
        }
        val card = set ?: return
        Text(
            text = card.reasonLabel.ifBlank { stringResource(R.string.product_complete_the_look_subtitle) },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            card.items.forEach { item ->
                Surface(
                    shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin),
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onListingClick(item.listingId) },
                ) {
                    Column {
                        FashAsyncImage(
                            model = item.coverImageUrl,
                            contentDescription = item.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.75f)
                                .clip(RoundedCornerShape(topStart = FashTheme.spacing.radiusSoftMin, topEnd = FashTheme.spacing.radiusSoftMin)),
                            contentScale = ContentScale.Crop,
                        )
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            modifier = Modifier.padding(6.dp),
                        )
                    }
                }
            }
        }
    }
}
