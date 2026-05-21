package com.pc.fash_android_mobile.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.home.HomeEditorialPostStub
import com.pc.fash_android_mobile.data.listing.Category
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.user.UserSearchResult
import com.pc.fash_android_mobile.ui.common.stableLazyKey
import com.pc.fash_android_mobile.ui.components.FashPromoSlideDef
import com.pc.fash_android_mobile.ui.components.FashPromoSlider
import com.pc.fash_android_mobile.ui.components.FashPromoSliderBlock
import com.pc.fash_android_mobile.ui.components.StickyBottomPromoBar
import com.pc.fash_android_mobile.ui.feed.FeedErrorColumn
import com.pc.fash_android_mobile.ui.feed.ListingGridCard
import com.pc.fash_android_mobile.ui.guest.GuestLoginReason
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

/**
 * Home tab: follow feed + marketplace preview (Explore heat) + discovery rails.
 * Promo sits inline (journey → promo → quick actions → hunt today → follow feed → …). When that row scrolls off-screen,
 * a duplicate promo docks at the bottom with animation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedContent(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    onListingClick: (listingId: String, sellerId: String?) -> Unit = { _, _ -> },
    onNavigateToExplore: () -> Unit = {},
    onOrdersClick: () -> Unit = {},
    /** Home journey “Đang giao” — opens dedicated in-transit hub (not the full orders list). */
    onDeliveringJourneyClick: () -> Unit = onOrdersClick,
    onNavigateToChat: () -> Unit = {},
    /** e.g. open Profile for saved items / account context. */
    onNavigateToSaved: () -> Unit = {},
    onNavigateToPost: () -> Unit = {},
    onPromoSlideClick: (FashPromoSlideDef, Int) -> Unit = { _, _ -> onNavigateToExplore() },
    promoSlides: List<FashPromoSlideDef> = emptyList(),
    /** Admin editorial posts (blog-style); host maps tap to Explore until in-app reader exists. */
    onHomeEditorialPostClick: (HomeEditorialPostStub) -> Unit = {},
    onHomeTrendingCategoryClick: (Category) -> Unit = {},
    onFeaturedSellerClick: (UserSearchResult) -> Unit = {},
    onOpenFeaturedSellersAll: () -> Unit = {},
    /** When true, follow-feed empty copy explains guest browse instead of “follow shops”. */
    isGuestBrowse: Boolean = false,
    onRequestLogin: (GuestLoginReason) -> Unit = {},
) {
    val onLikeListing: (ListingFeedItem) -> Unit = { item ->
        if (isGuestBrowse) onRequestLogin(GuestLoginReason.Like) else viewModel.toggleLike(item)
    }
    val onSaveListing: (ListingFeedItem) -> Unit = { item ->
        if (isGuestBrowse) onRequestLogin(GuestLoginReason.Saved) else viewModel.toggleSave(item)
    }
    val items by viewModel.items.collectAsState()
    val huntTodayItems by viewModel.huntTodayItems.collectAsState()
    val huntTodayLoading by viewModel.huntTodayLoading.collectAsState()
    val discovery by viewModel.discoveryBundle.collectAsState()
    val followingIds by viewModel.followingIds.collectAsState()
    val buyerStats by viewModel.buyerStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val pullState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    val showJourneyRow = buyerStats.hasJourneyActivity()
    val homePromoLazyIndex = if (showJourneyRow) 1 else 0
    val recentlyViewed = discovery.recentlyViewed
    val recommendedSellers = discovery.recommendedSellers

    LaunchedEffect(Unit) {
        viewModel.scrollHomeToTop.collect {
            listState.animateScrollToItem(0)
        }
    }

    val showStickyPromo by remember(showJourneyRow) {
        val promoIndex = homePromoLazyIndex
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return@derivedStateOf false
            val inlinePromoVisible =
                layoutInfo.visibleItemsInfo.any { it.index == promoIndex }
            !inlinePromoVisible
        }
    }

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
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = FashTheme.spacing.spacing8 + 72.dp),
            ) {
                if (showJourneyRow) {
                    item {
                        BuyerHomeJourneyRow(
                            stats = buyerStats,
                            onDeliveringClick = onDeliveringJourneyClick,
                            onSavedClick = onNavigateToSaved,
                            onMessagesClick = onNavigateToChat,
                        )
                    }
                }
                item {
                    FashPromoSliderBlock(
                        slides = promoSlides,
                        onSlideClick = onPromoSlideClick,
                    )
                }
                item {
                    HomeQuickActionsRow(
                        onExplore = onNavigateToExplore,
                        onSell = onNavigateToPost,
                        onOrders = onOrdersClick,
                    )
                }

                item {
                    HomeHuntTodaySection(
                        items = huntTodayItems,
                        isLoading = huntTodayLoading,
                        onSeeAllClick = onNavigateToExplore,
                        onListingClick = onListingClick,
                        onLike = onLikeListing,
                        onSave = onSaveListing,
                        onRecordView = { viewModel.recordView(it) },
                    )
                }

                when {
                    isLoading && items.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = FashColors.Primary,
                                    strokeWidth = 2.dp,
                                )
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
                            HomeFollowFeedEmptyHint(
                                onFeaturedSellersClick = onOpenFeaturedSellersAll,
                                hintRes = if (isGuestBrowse) {
                                    R.string.home_guest_follow_hint
                                } else {
                                    R.string.home_follow_empty_hint
                                },
                                showSectionHeader = !isGuestBrowse,
                                // Guest already has "Shops worth a look" below — orphan CTA looked like an empty section.
                                showFeaturedCta = recommendedSellers.isNotEmpty() && !isGuestBrowse,
                            )
                        }
                    }
                    else -> {
                        item {
                            HomeSectionHeader(
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
                                        top = if (index == 0) 2.dp else 0.dp,
                                        bottom = FashTheme.spacing.spacing2,
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
                                        onLike = { onLikeListing(feedItem) },
                                        onSave = { onSaveListing(feedItem) },
                                        onClick = { onListingClick(feedItem.id, feedItem.sellerId) },
                                        modifier = Modifier.weight(1f),
                                        imageAspectRatio = 4f / 5f,
                                    )
                                }
                                repeat(2 - row.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                if (discovery.stylePicks.size >= 2) {
                    item {
                        HomeSectionHeader(
                            title = stringResource(R.string.home_style_picks_title),
                            subtitle = stringResource(R.string.home_style_picks_subtitle),
                        )
                    }
                    item {
                        HomeHuntTodaySection(
                            items = discovery.stylePicks,
                            isLoading = false,
                            onSeeAllClick = onNavigateToExplore,
                            onListingClick = onListingClick,
                            onLike = onLikeListing,
                            onSave = onSaveListing,
                            onRecordView = { viewModel.recordView(it) },
                        )
                    }
                }

                if (recentlyViewed.size >= 2) {
                    item {
                        HomeRecentlyViewedSection(
                            items = recentlyViewed,
                            onListingClick = onListingClick,
                        )
                    }
                }

                item {
                    HomeRecommendedSellersSection(
                        sellers = recommendedSellers,
                        followingIds = followingIds,
                        onSellerClick = onFeaturedSellerClick,
                        onSeeAllClick = onOpenFeaturedSellersAll,
                    )
                }

                item {
                    HomeEditorialPostsSection(
                        posts = discovery.editorialPosts,
                        onPostClick = onHomeEditorialPostClick,
                    )
                }

                item {
                    HomeBrandFooterStrip()
                }
            }

            AnimatedVisibility(
                visible = showStickyPromo,
                modifier = Modifier.fillMaxWidth(),
                enter = slideInVertically(
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    initialOffsetY = { it },
                ) + fadeIn(animationSpec = tween(280)),
                exit = slideOutVertically(
                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                    targetOffsetY = { it },
                ) + fadeOut(animationSpec = tween(200)),
            ) {
                StickyBottomPromoBar(elevated = true) {
                    FashPromoSlider(
                        modifier = Modifier.fillMaxWidth(),
                        slides = promoSlides,
                        onSlideClick = onPromoSlideClick,
                    )
                }
            }
        }
    }
}
