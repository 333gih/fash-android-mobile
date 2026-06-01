package com.pc.fash_android_mobile.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.ui.listing.listingStatusOverlayLabel
import com.pc.fash_android_mobile.ui.theme.FashTheme

/** Clamp width/height so extreme photos do not break the two-column grid. */
private const val MIN_LISTING_MASONRY_ASPECT = 3f / 5f
private const val MAX_LISTING_MASONRY_ASPECT = 5f / 4f


fun clampListingMasonryAspectRatio(raw: Float): Float =
    raw.coerceIn(MIN_LISTING_MASONRY_ASPECT, MAX_LISTING_MASONRY_ASPECT)

private const val DEFAULT_MASONRY_ASPECT = 4f / 5f

/** Pinterest-style width/height from cover pixels; neutral 4:5 when API omits dimensions. */
fun listingMasonryAspectRatio(item: ListingFeedItem): Float {
    val w = item.coverImageWidth
    val h = item.coverImageHeight
    if (w != null && h != null && w > 0 && h > 0) {
        return clampListingMasonryAspectRatio(w.toFloat() / h.toFloat())
    }
    return DEFAULT_MASONRY_ASPECT
}

/** cardHeight = columnWidth * imageHeight / imageWidth */
fun listingMasonryTileHeightDp(columnWidthDp: Float, item: ListingFeedItem): Dp {
    if (columnWidthDp <= 0f) return 0.dp
    val aspect = listingMasonryAspectRatio(item).coerceAtLeast(0.01f)
    return (columnWidthDp / aspect).dp
}

fun masonryColumnWidthDp(
    screenWidthDp: Float,
    leadingInsetDp: Float,
    trailingInsetDp: Float,
    columnGapDp: Float,
): Float {
    val inner = (screenWidthDp - leadingInsetDp - trailingInsetDp - columnGapDp).coerceAtLeast(0f)
    return inner / 2f
}

@Composable
fun rememberListingMasonryColumnWidthDp(): Float {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
    val inset = maxOf(FashTheme.spacing.editorialStart.value, FashTheme.spacing.editorialEnd.value)
    return masonryColumnWidthDp(
        screenWidthDp = screenWidthDp,
        leadingInsetDp = inset,
        trailingInsetDp = inset,
        columnGapDp = FashTheme.spacing.spacing2.value,
    )
}

fun Modifier.listingMasonryTileSize(columnWidthDp: Float, item: ListingFeedItem): Modifier =
    fillMaxWidth().height(listingMasonryTileHeightDp(columnWidthDp, item))

/** Left/right columns with stable ids across load-more (iOS [ListingMasonryColumnLayout]). */
data class ListingMasonryColumnLayout(
    val left: List<Pair<Int, ListingFeedItem>>,
    val right: List<Pair<Int, ListingFeedItem>>,
) {
    val isEmpty: Boolean get() = left.isEmpty() && right.isEmpty()
}

/**
 * Shortest-column masonry with stable column per listing id — existing tiles do not move when appending.
 */
fun makeStableColumnLayout(
    items: List<ListingFeedItem>,
    assignedIsRightColumn: MutableMap<String, Boolean>,
): ListingMasonryColumnLayout {
    val liveIds = items.map { it.id }.toSet()
    assignedIsRightColumn.keys.retainAll(liveIds)
    if (items.isEmpty()) return ListingMasonryColumnLayout(emptyList(), emptyList())

    val left = mutableListOf<Pair<Int, ListingFeedItem>>()
    val right = mutableListOf<Pair<Int, ListingFeedItem>>()
    var leftHeight = 0f
    var rightHeight = 0f

    items.forEachIndexed { index, item ->
        val unitHeight = 1f / listingMasonryAspectRatio(item)
        val placeRight = assignedIsRightColumn[item.id]
            ?: (leftHeight > rightHeight).also { assignedIsRightColumn[item.id] = it }
        if (placeRight) {
            right += index to item
            rightHeight += unitHeight
        } else {
            left += index to item
            leftHeight += unitHeight
        }
    }
    return ListingMasonryColumnLayout(left, right)
}

/** Page-sized chunks for profile/home lazy column virtualization (iOS [ListingMasonryFeedPages]). */
object ListingMasonryFeedPages {
    const val DEFAULT_CHUNK_SIZE = 20

    data class Chunk(
        val id: Int,
        val entries: List<Pair<Int, ListingFeedItem>>,
    )

    fun chunks(items: List<ListingFeedItem>, pageSize: Int = DEFAULT_CHUNK_SIZE): List<Chunk> {
        if (items.isEmpty() || pageSize <= 0) return emptyList()
        val result = mutableListOf<Chunk>()
        var pageIndex = 0
        var start = 0
        while (start < items.size) {
            val end = minOf(start + pageSize, items.size)
            val entries = (start until end).map { it to items[it] }
            result += Chunk(id = pageIndex, entries = entries)
            pageIndex++
            start = end
        }
        return result
    }
}

/**
 * Virtualized two-column feed rows for [androidx.compose.foundation.lazy.LazyColumn].
 * Each lazy item is one row (up to two tiles) balanced by shortest-column layout.
 */
fun LazyListScope.listingMasonryFeedRows(
    items: List<ListingFeedItem>,
    keyPrefix: String,
    columnAssignments: MutableMap<String, Boolean>,
    onLikeListing: (ListingFeedItem) -> Unit,
    onSaveListing: (ListingFeedItem) -> Unit,
    onListingClick: (ListingFeedItem, Int) -> Unit,
    onRecordView: (ListingFeedItem, Int) -> Unit,
    onDwell: (ListingFeedItem, Int, Int) -> Unit,
) {
    val layout = makeStableColumnLayout(items, columnAssignments)
    val rowCount = maxOf(layout.left.size, layout.right.size)
    for (rowIndex in 0 until rowCount) {
        val leftEntry = layout.left.getOrNull(rowIndex)
        val rightEntry = layout.right.getOrNull(rowIndex)
        val rowKey = leftEntry?.second?.id ?: rightEntry?.second?.id ?: "row_$rowIndex"
        item(key = "${keyPrefix}_row_$rowKey") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = FashTheme.spacing.editorialStart,
                        end = FashTheme.spacing.editorialEnd,
                    ),
                horizontalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing2),
            ) {
                if (leftEntry != null) {
                    ListingMasonryTile(
                        feedItem = leftEntry.second,
                        index = leftEntry.first,
                        onLikeListing = onLikeListing,
                        onSaveListing = onSaveListing,
                        onListingClick = onListingClick,
                        onRecordView = onRecordView,
                        onDwell = onDwell,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (rightEntry != null) {
                    ListingMasonryTile(
                        feedItem = rightEntry.second,
                        index = rightEntry.first,
                        onLikeListing = onLikeListing,
                        onSaveListing = onSaveListing,
                        onListingClick = onListingClick,
                        onRecordView = onRecordView,
                        onDwell = onDwell,
                        modifier = Modifier.weight(1f),
                    )
                } else {
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
 * Prefer [listingMasonryFeedRows] or [listingMasonryProfileChunkItems] for long feeds.
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
    columnAssignments: MutableMap<String, Boolean> = mutableMapOf(),
    relationBadgeForItem: (ListingFeedItem) -> String? = { null },
) {
    val layout = remember(items, columnAssignments) {
        makeStableColumnLayout(items, columnAssignments)
    }
    val columnWidthDp = rememberListingMasonryColumnWidthDp()
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
            layout.left.forEach { (index, feedItem) ->
                ListingMasonryTile(
                    feedItem = feedItem,
                    index = index,
                    columnWidthDp = columnWidthDp,
                    onLikeListing = onLikeListing,
                    onSaveListing = onSaveListing,
                    onListingClick = onListingClick,
                    onRecordView = onRecordView,
                    onDwell = onDwell,
                    relationBadgeLabel = relationBadgeForItem(feedItem),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            layout.right.forEach { (index, feedItem) ->
                ListingMasonryTile(
                    feedItem = feedItem,
                    index = index,
                    columnWidthDp = columnWidthDp,
                    onLikeListing = onLikeListing,
                    onSaveListing = onSaveListing,
                    onListingClick = onListingClick,
                    onRecordView = onRecordView,
                    onDwell = onDwell,
                    relationBadgeLabel = relationBadgeForItem(feedItem),
                )
            }
        }
    }
}

/**
 * LazyColumn items: page chunks with side-by-side [Column]s (true Pinterest stagger).
 */
fun LazyListScope.listingMasonryProfileChunkItems(
    items: List<ListingFeedItem>,
    layout: ListingMasonryColumnLayout,
    keyPrefix: String,
    columnWidthDp: Float,
    showQuickActions: Boolean,
    showListingStatusOverlay: Boolean,
    onListingClick: (ListingFeedItem) -> Unit,
    onListingLike: (ListingFeedItem) -> Unit,
    onListingSave: (ListingFeedItem) -> Unit,
) {
    val chunks = ListingMasonryFeedPages.chunks(items)
    itemsIndexed(
        chunks,
        key = { _, chunk -> "${keyPrefix}_chunk_${chunk.id}" },
    ) { _, chunk ->
        val chunkIds = chunk.entries.map { it.second.id }.toSet()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = FashTheme.spacing.editorialStart,
                    end = FashTheme.spacing.editorialEnd,
                ),
            horizontalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing2),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing2),
            ) {
                layout.left
                    .filter { it.second.id in chunkIds }
                    .forEach { (_, item) ->
                        ListingMasonryProfileCard(
                            item = item,
                            columnWidthDp = columnWidthDp,
                            showQuickActions = showQuickActions,
                            statusOverlayLabel = if (showListingStatusOverlay) {
                                listingStatusOverlayLabel(item.listingStatus)
                            } else {
                                null
                            },
                            onClick = { onListingClick(item) },
                            onLike = { onListingLike(item) },
                            onSave = { onListingSave(item) },
                        )
                    }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing2),
            ) {
                layout.right
                    .filter { it.second.id in chunkIds }
                    .forEach { (_, item) ->
                        ListingMasonryProfileCard(
                            item = item,
                            columnWidthDp = columnWidthDp,
                            showQuickActions = showQuickActions,
                            statusOverlayLabel = if (showListingStatusOverlay) {
                                listingStatusOverlayLabel(item.listingStatus)
                            } else {
                                null
                            },
                            onClick = { onListingClick(item) },
                            onLike = { onListingLike(item) },
                            onSave = { onListingSave(item) },
                        )
                    }
            }
        }
    }
}

@Composable
private fun ListingMasonryProfileCard(
    item: ListingFeedItem,
    columnWidthDp: Float,
    showQuickActions: Boolean,
    statusOverlayLabel: String?,
    onClick: () -> Unit,
    onLike: () -> Unit,
    onSave: () -> Unit,
) {
    ListingGridCard(
        item = item,
        onClick = onClick,
        imageAspectRatio = listingMasonryAspectRatio(item),
        showQuickActions = showQuickActions,
        onLike = onLike,
        onSave = onSave,
        statusOverlayLabel = statusOverlayLabel,
        columnWidthDp = columnWidthDp,
        modifier = Modifier.listingMasonryTileSize(columnWidthDp, item),
    )
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
    columnWidthDp: Float = rememberListingMasonryColumnWidthDp(),
    relationBadgeLabel: String? = null,
) {
    val aspect = listingMasonryAspectRatio(feedItem)
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
            .listingMasonryTileSize(columnWidthDp, feedItem)
            .padding(bottom = FashTheme.spacing.spacing2),
        imageAspectRatio = aspect,
        columnWidthDp = columnWidthDp,
        relationBadgeLabel = relationBadgeLabel,
    )
}
