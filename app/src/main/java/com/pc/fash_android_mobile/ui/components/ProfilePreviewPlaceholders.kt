package com.pc.fash_android_mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R

/**
 * Empty tile in a 3-slot profile preview strip — communicates “no extra listing here”
 * (seller has fewer than 3 listings, or fewer preview IDs returned).
 */
@Composable
fun ProfilePreviewEmptySlotPlaceholder(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(cornerRadius)
    val cd = stringResource(R.string.profile_preview_slot_empty_cd)
    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = cd }
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.42f), shape)
            .background(scheme.surfaceContainerHighest.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 6.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Storefront,
                contentDescription = null,
                tint = scheme.outline.copy(alpha = 0.55f),
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.profile_preview_slot_empty_label),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.88f),
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

/**
 * Short caption when fewer than 3 preview tiles are filled but the shop has listings
 * (so users know the strip is intentionally partial, not broken).
 */
@Composable
fun ProfilePreviewRowCaption(
    previewCount: Int,
    totalListingCount: Int,
    modifier: Modifier = Modifier,
) {
    if (previewCount <= 0 || previewCount >= 3) return
    val total = totalListingCount.coerceAtLeast(previewCount)
    if (total <= 0) return
    val text = if (previewCount >= total) {
        stringResource(R.string.profile_preview_caption_all, previewCount)
    } else {
        stringResource(R.string.profile_preview_caption_partial, previewCount, total)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 6.dp),
    )
}
