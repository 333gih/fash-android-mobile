package com.pc.fash_android_mobile.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.FashColors

/**
 * End-of-feed pagination — triggers [onLoadMore] only when the user scrolls to the bottom.
 * Shows a branded loading row while the next page fetches (no mid-scroll prefetch).
 */
@Composable
fun FeedLoadMoreFooter(
    enabled: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(enabled, isLoadingMore) {
        if (enabled && !isLoadingMore) {
            onLoadMore()
        }
    }

    if (isLoadingMore) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = FashColors.Primary,
                strokeWidth = 2.dp,
            )
            Text(
                text = stringResource(R.string.feed_loading_more),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        Spacer(modifier = modifier.height(1.dp))
    }
}
