package com.pc.fash_android_mobile.ui.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.user.UserSearchResult
import com.pc.fash_android_mobile.ui.common.stableLazyKey
import com.pc.fash_android_mobile.ui.components.FashAvatarCircle
import com.pc.fash_android_mobile.ui.feed.FeedEmptyColumn
import com.pc.fash_android_mobile.ui.feed.FeedErrorColumn
import com.pc.fash_android_mobile.ui.feed.FeedSectionHeader
import com.pc.fash_android_mobile.ui.feed.ListingGridCard
import com.pc.fash_android_mobile.ui.feed.resolveListingImageUrl
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

/** Taller tiles on Explore so listing art isn’t read as thin strips (3:5 portrait). */
private val ExploreListingTileAspectRatio = 3f / 5f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel,
    onListingClick: (listingId: String, sellerId: String?) -> Unit = { _, _ -> },
    /** Tap a story-style featured seller chip (e.g. open profile when wired). */
    onFeaturedSellerClick: (UserSearchResult) -> Unit = {},
    /** “See all” in the featured sellers header. */
    onSeeAllFeaturedSellersClick: () -> Unit = {},
) {
    val tags by viewModel.tags.collectAsState()
    val selectedTagIndex by viewModel.selectedTagIndex.collectAsState()
    val featuredSellers by viewModel.featuredSellers.collectAsState()
    val listings by viewModel.listings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val followingIds by viewModel.followingIds.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val filtersExpanded by viewModel.filtersExpanded.collectAsState()
    val pullState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = modifier.fillMaxSize(),
        state = pullState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = isRefreshing,
                color = FashColors.Primary,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FeedSectionHeader(subtitle = stringResource(R.string.explore_feed_subtitle))

            ExplorePromoCarousel()

            ExploreFiltersToggleRow(
                expanded = filtersExpanded,
                onToggle = { viewModel.setFiltersExpanded(!filtersExpanded) },
            )

            AnimatedVisibility(visible = filtersExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ExploreCategoryStrip(
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        onSelectCategory = { viewModel.selectCategory(it) },
                    )

                    val displayTags = listOf(stringResource(R.string.explore_style_any)) + tags
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.explore_style_section_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(
                                start = FashTheme.spacing.editorialStart,
                                end = FashTheme.spacing.editorialEnd,
                                bottom = 8.dp,
                            ),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            displayTags.forEachIndexed { index, tag ->
                                val selected = index == selectedTagIndex
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(FashTheme.spacing.radiusPill))
                                        .background(
                                            if (selected) FashColors.Primary
                                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .clickable { viewModel.selectTag(index) },
                                )
                            }
                        }
                    }

                    if (featuredSellers.isNotEmpty()) {
                        FeaturedSellersStorySection(
                            sellers = featuredSellers,
                            followingIds = followingIds,
                            onSellerClick = onFeaturedSellerClick,
                            onSeeAllClick = onSeeAllFeaturedSellersClick,
                        )
                    }
                }
            }

            if (isLoading && tags.isEmpty() && listings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = FashColors.Primary)
                }
                return@Column
            }

            when {
                loadError && listings.isEmpty() -> {
                    FeedErrorColumn(
                        message = stringResource(R.string.feed_load_error),
                        onRetry = { viewModel.retryLoad() },
                        modifier = Modifier.weight(1f),
                    )
                }
                listings.isEmpty() -> {
                    FeedEmptyColumn(
                        title = stringResource(R.string.feed_empty_title),
                        subtitle = stringResource(R.string.explore_grid_empty_subtitle),
                        modifier = Modifier.weight(1f),
                    )
                }
                else -> {
                    // Adaptive min width + taller portrait tiles so previews read clearly (Depop-style grid).
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 172.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            horizontal = FashTheme.spacing.editorialStart,
                            vertical = 8.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing2),
                        verticalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing4),
                    ) {
                        itemsIndexed(
                            listings,
                            key = { index, item -> stableLazyKey(item.id, index, "exp") },
                        ) { _, item ->
                            LaunchedEffect(item.id) {
                                viewModel.recordView(item)
                            }
                            ListingGridCard(
                                item = item,
                                onClick = { onListingClick(item.id, item.sellerId) },
                                imageAspectRatio = ExploreListingTileAspectRatio,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreFiltersToggleRow(
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val cd = stringResource(R.string.explore_filters_toggle_cd)
    val spacing = FashTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = spacing.editorialStart,
                end = spacing.editorialEnd,
                top = 2.dp,
                bottom = 4.dp,
            ),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onToggle,
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = cd
            },
        ) {
            Text(
                text = stringResource(
                    if (expanded) R.string.explore_filters_hide else R.string.explore_filters_show,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = FashColors.Primary,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = FashColors.Primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Inner avatar diameter; ring + gap sized to match story-style reference. */
private val FeaturedSellerStoryAvatarInner = 64.dp
private val FeaturedSellerStoryRingStroke = 2.dp
private val FeaturedSellerStoryRingGap = 3.dp

@Composable
private fun FeaturedSellersStorySection(
    sellers: List<UserSearchResult>,
    followingIds: Set<String>,
    onSellerClick: (UserSearchResult) -> Unit,
    onSeeAllClick: () -> Unit,
) {
    val spacing = FashTheme.spacing
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = spacing.editorialStart,
                    end = spacing.editorialEnd,
                    top = spacing.spacing3,
                    bottom = spacing.spacing2,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.explore_featured_sellers),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.explore_see_all),
                style = MaterialTheme.typography.labelLarge,
                color = FashColors.Primary,
                modifier = Modifier.clickable(onClick = onSeeAllClick),
            )
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = spacing.editorialStart,
                end = spacing.editorialEnd,
                bottom = spacing.spacing4,
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing4),
            verticalAlignment = Alignment.Top,
        ) {
            itemsIndexed(
                sellers,
                key = { _, s -> s.userId.ifBlank { s.username } },
            ) { _, seller ->
                val isFollowing =
                    followingIds.contains(seller.userId) || followingIds.contains(seller.username)
                FeaturedSellerStoryItem(
                    seller = seller,
                    showAccentRing = !isFollowing,
                    onClick = { onSellerClick(seller) },
                )
            }
        }
    }
}

@Composable
private fun FeaturedSellerStoryItem(
    seller: UserSearchResult,
    showAccentRing: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val handleText = featuredSellerHandle(seller)
    val labelForCd = seller.username.ifBlank { seller.displayName }.ifBlank { handleText }

    Column(
        modifier = Modifier
            .widthIn(min = 72.dp, max = 88.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FeaturedSellerStoryAvatar(
            seller = seller,
            showAccentRing = showAccentRing,
            contentDescription = stringResource(R.string.explore_featured_seller_cd, labelForCd),
        )
        Spacer(modifier = Modifier.height(FashTheme.spacing.spacing2))
        Text(
            text = handleText,
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

private fun featuredSellerHandle(seller: UserSearchResult): String {
    val username = seller.username.trim()
    if (username.isNotBlank()) return "@$username"
    val dn = seller.displayName.trim()
    if (dn.isNotBlank()) {
        val handle = dn.split(Regex("\\s+")).joinToString("_").take(22)
        return "@$handle"
    }
    return "@..."
}

@Composable
private fun FeaturedSellerStoryAvatar(
    seller: UserSearchResult,
    showAccentRing: Boolean,
    contentDescription: String,
) {
    val scheme = MaterialTheme.colorScheme
    val inner = FeaturedSellerStoryAvatarInner
    val ring = FeaturedSellerStoryRingStroke
    val gap = FeaturedSellerStoryRingGap
    val outer = inner + ring * 2 + gap * 2
    val imageUrl = seller.avatarUrl.takeIf { it.isNotBlank() }?.let { resolveListingImageUrl(it) }
    val initial =
        (seller.displayName.firstOrNull() ?: seller.username.firstOrNull())?.takeIf { it.isLetter() }

    Box(
        modifier = Modifier.size(outer),
        contentAlignment = Alignment.Center,
    ) {
        if (showAccentRing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(ring, FashColors.Primary, CircleShape)
                    .padding(gap)
                    .background(scheme.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                FashAvatarCircle(
                    imageUrl = imageUrl,
                    contentDescription = contentDescription,
                    size = inner,
                    fallbackInitial = initial,
                )
            }
        } else {
            FashAvatarCircle(
                imageUrl = imageUrl,
                contentDescription = contentDescription,
                size = inner,
                fallbackInitial = initial,
            )
        }
    }
}
