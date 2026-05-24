package com.pc.fash_android_mobile.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
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
 * Virtualized two-column feed rows for [androidx.compose.foundation.lazy.LazyColumn].
 * Each lazy item is one row (up to two tiles) so off-screen cards are not composed.
 */
fun LazyListScope.listingMasonryFeedRows(
    items: List<ListingFeedItem>,
    keyPrefix: String,
    onLikeListing: (ListingFeedItem) -> Unit,
    onSaveListing: (ListingFeedItem) -> Unit,
    onListingClick: (ListingFeedItem, Int) -> Unit,
    onRecordView: (ListingFeedItem, Int) -> Unit,
    onDwell: (ListingFeedItem, Int, Int) -> Unit,
) {
    items.chunked(2).forEachIndexed { rowIndex, rowItems ->
        item(key = "${keyPrefix}_row_${rowItems.first().id}") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = FashTheme.spacing.editorialStart,
                        end = FashTheme.spacing.editorialEnd,
                    ),
                horizontalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing2),
            ) {
                rowItems.forEachIndexed { colInRow, feedItem ->
                    val index = rowIndex * 2 + colInRow
                    ListingMasonryTile(
                        feedItem = feedItem,
                        index = index,
                        onLikeListing = onLikeListing,
                        onSaveListing = onSaveListing,
                        onListingClick = onListingClick,
                        onRecordView = onRecordView,
                        onDwell = onDwell,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Two-column masonry grid for listing feeds. Uses a non-scrollable layout so it can live
 * inside [androidx.compose.foundation.lazy.LazyColumn] without infinite-height constraint crashes.
 *
 * Prefer [listingMasonryFeedRows] inside [androidx.compose.foundation.lazy.LazyColumn] for long feeds.
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
