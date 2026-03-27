package com.pc.fash_android_mobile.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.common.stableLazyKey
import com.pc.fash_android_mobile.ui.feed.FeedEmptyColumn
import com.pc.fash_android_mobile.ui.feed.FeedErrorColumn
import com.pc.fash_android_mobile.ui.feed.FeedSectionHeader
import com.pc.fash_android_mobile.ui.feed.ListingGridCard
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

/**
 * Home tab: [GET /api/v1/listings/home] — listings from followed sellers only.
 * Buyer layout: journey row → hero → grid (shared with Explore) → brand footer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedContent(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    onListingClick: (listingId: String, sellerId: String?) -> Unit = { _, _ -> },
    onNavigateToExplore: () -> Unit = {},
    onOrdersClick: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    /** e.g. open Profile for saved items / account context. */
    onNavigateToSaved: () -> Unit = {},
    onNavigateToPost: () -> Unit = {},
) {
    val items by viewModel.items.collectAsState()
    val buyerStats by viewModel.buyerStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = FashTheme.spacing.spacing6),
        ) {
            item {
                BuyerHomeJourneyRow(
                    stats = buyerStats,
                    onDeliveringClick = onOrdersClick,
                    onSavedClick = onNavigateToSaved,
                    onMessagesClick = onNavigateToChat,
                )
            }
            item {
                HomeHeroBanner(onExploreClick = onNavigateToExplore)
            }
            item {
                HomeQuickActionsRow(
                    onExplore = onNavigateToExplore,
                    onSell = onNavigateToPost,
                    onOrders = onOrdersClick,
                )
            }

            when {
                isLoading && items.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = FashColors.Primary)
                        }
                    }
                }
                loadError && items.isEmpty() -> {
                    item {
                        FeedErrorColumn(
                            message = stringResource(R.string.feed_load_error),
                            onRetry = { viewModel.retryLoad() },
                        )
                    }
                }
                items.isEmpty() -> {
                    item {
                        FeedEmptyColumn(
                            title = stringResource(R.string.home_feed_empty_title),
                            subtitle = stringResource(R.string.home_feed_empty_subtitle),
                            primaryActionLabel = stringResource(R.string.home_empty_cta_explore),
                            onPrimaryAction = onNavigateToExplore,
                        )
                    }
                }
                else -> {
                    item {
                        FeedSectionHeader(
                            title = stringResource(R.string.home_top_section_title),
                            subtitle = stringResource(R.string.home_top_section_subtitle),
                        )
                    }
                    val rows = items.chunked(2)
                    itemsIndexed(
                        items = rows,
                        key = { index, row ->
                            stableLazyKey(row.firstOrNull()?.id, index, "home")
                        },
                    ) { index, row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = FashTheme.spacing.editorialStart,
                                    end = FashTheme.spacing.editorialEnd,
                                )
                                .padding(
                                    top = if (index == 0) 4.dp else 0.dp,
                                    bottom = FashTheme.spacing.spacing3,
                                ),
                            horizontalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing2),
                        ) {
                            row.forEach { feedItem ->
                                LaunchedEffect(feedItem.id) {
                                    viewModel.recordView(feedItem)
                                }
                                ListingGridCard(
                                    item = feedItem,
                                    showQuickActions = true,
                                    onLike = { viewModel.toggleLike(feedItem) },
                                    onSave = { viewModel.toggleSave(feedItem) },
                                    onClick = { onListingClick(feedItem.id, feedItem.sellerId) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(2 - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            item {
                HomeBrandFooterStrip()
            }
        }
    }
}
