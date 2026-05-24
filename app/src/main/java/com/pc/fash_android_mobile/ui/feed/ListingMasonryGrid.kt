package com.pc.fash_android_mobile.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.ui.theme.FashTheme

/** Vary tile height by listing id hash for masonry-style listing grids (Home follow feed, Explore). */
fun listingMasonryStaggerAspectRatio(listingId: String): Float {
    val bucket = (listingId.hashCode() and Int.MAX_VALUE) % 3
    return when (bucket) {
        0 -> 3f / 4f
        1 -> 4f / 5f
        else -> 5f / 6f
    }
}

/**
 * Two-column masonry grid for listing feeds. Uses a non-scrollable layout so it can live
 * inside [androidx.compose.foundation.lazy.LazyColumn] without infinite-height constraint crashes.
 */
@Composable
fun ListingMasonryGrid(
    items: List<ListingFeedItem>,
    onLikeListing: (ListingFeedItem) -> Unit,
    onSaveListing: (ListingFeedItem) -> Unit,
    onListingClick: (ListingFeedItem, Int) -> Unit,
    onRecordView: (ListingFeedItem, Int) -> Unit,
    onDwell: (ListingFeedItem, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columnItems = remember(items) {
        val left = mutableListOf<Pair<Int, ListingFeedItem>>()
        val right = mutableListOf<Pair<Int, ListingFeedItem>>()
        items.forEachIndexed { index, item ->
            if (index % 2 == 0) left += index to item else right += index to item
        }
        left to right
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = FashTheme.spacing.editorialStart,
                end = FashTheme.spacing.editorialEnd,
            ),
        horizontalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing2),
    ) {
        Column(Modifier.weight(1f)) {
            columnItems.first.forEach { (index, feedItem) ->
                ListingMasonryTile(
                    feedItem = feedItem,
                    index = index,
                    onLikeListing = onLikeListing,
                    onSaveListing = onSaveListing,
                    onListingClick = onListingClick,
                    onRecordView = onRecordView,
                    onDwell = onDwell,
                )
            }
        }
        Column(Modifier.weight(1f)) {
            columnItems.second.forEach { (index, feedItem) ->
                ListingMasonryTile(
                    feedItem = feedItem,
                    index = index,
                    onLikeListing = onLikeListing,
                    onSaveListing = onSaveListing,
                    onListingClick = onListingClick,
                    onRecordView = onRecordView,
                    onDwell = onDwell,
                )
            }
        }
    }
}

@Composable
fun ListingMasonryTile(
    feedItem: ListingFeedItem,
    index: Int,
    onLikeListing: (ListingFeedItem) -> Unit,
    onSaveListing: (ListingFeedItem) -> Unit,
    onListingClick: (ListingFeedItem, Int) -> Unit,
    onRecordView: (ListingFeedItem, Int) -> Unit,
    onDwell: (ListingFeedItem, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    showQuickActions: Boolean = true,
) {
    val aspect = listingMasonryStaggerAspectRatio(feedItem.id)
    LaunchedEffect(feedItem.id) {
        onRecordView(feedItem, index)
    }
    ListingGridCard(
        item = feedItem,
        showQuickActions = showQuickActions,
        onLike = { onLikeListing(feedItem) },
        onSave = { onSaveListing(feedItem) },
        onClick = { onListingClick(feedItem, index) },
        onDwell = { dwellMs -> onDwell(feedItem, index, dwellMs) },
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = FashTheme.spacing.spacing2),
        imageAspectRatio = aspect,
    )
}
