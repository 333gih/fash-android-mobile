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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.home.HomeEditorialPostStub
import com.pc.fash_android_mobile.data.search.TrendingTagChip
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.user.UserSearchResult
import com.pc.fash_android_mobile.ui.common.stableLazyKey
import com.pc.fash_android_mobile.ui.components.FashPromoSlideDef
import com.pc.fash_android_mobile.ui.components.FashPromoSlider
import com.pc.fash_android_mobile.ui.components.FashPromoSliderBlock
import com.pc.fash_android_mobile.ui.components.FashSkeletonGrid
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
    /** Called when the user taps a trending style chip; navigates to Explore pre-filtered by [tagName]. */
    onNavigateToExploreWithTag: (tagName: String) -> Unit = { onNavigateToExplore() },
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
    onFeaturedSellerClick: (UserSearchResult) -> Unit = {},
    onOpenFeaturedSellersAll: () -> Unit = {},
    /** When true, follow-feed empty copy explains guest browse instead of “follow shops”. */
    isGuestBrowse: Boolean = false,
    onRequestLogin: (GuestLoginReason) -> Unit = {},
    /**
     * Opens the profile editor where the user can save reference size/measurements. Wired
     * up to the inline "Add my size" banner on the Home feed; when null the banner is hidden.
     */
    onOpenSizingSetup: (() -> Unit)? = null,
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
    val showSizingBanner by viewModel.showSizingBanner.collectAsState()
    val followingIds by viewModel.followingIds.collectAsState()
    val buyerStats by viewModel.buyerStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMoreItems by viewModel.hasMoreItems.collectAsState()
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

    // Follow-feed infinite scroll: when the user is within ~3 list items of the bottom and the
    // VM still reports `hasMoreItems`, request the next page. Same trigger pattern as Explore
    // (see ExploreScreen pagination). Skipped in guest mode; soft-no-op while a load is in flight.
    LaunchedEffect(hasMoreItems, isGuestBrowse) {
        if (isGuestBrowse) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = info.totalItemsCount
            if (last < 0 || total <= 0) -1 else (total - last)
        }.collect { distanceToEnd ->
            if (distanceToEnd in 0..3) {
                viewModel.loadMoreFollowFeed()
            }
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
                if (showSizingBanner && onOpenSizingSetup != null) {
                    item {
                        HomeSectionReveal(sectionKey = "sizing-banner") {
                            HomeProfileSizingBanner(
                                onCtaClick = onOpenSizingSetup,
                                onDismiss = { viewModel.dismissSizingBanner() },
                            )
                        }
                    }
                }
                item {
                    HomeSectionReveal(sectionKey = "promo") {
                        FashPromoSliderBlock(
                            slides = promoSlides,
                            onSlideClick = onPromoSlideClick,
                        )
                    }
                }
                item {
                    HomeSectionReveal(sectionKey = "quick-actions") {
                        HomeQuickActionsRow(
                            onExplore = onNavigateToExplore,
                            onSell = onNavigateToPost,
                            onOrders = onOrdersClick,
                        )
                    }
                }

                item {
                    HomeSectionReveal(sectionKey = "hunt-today") {
                        HomeHuntTodaySection(
                            items = huntTodayItems,
                            isLoading = huntTodayLoading,
                            onSeeAllClick = onNavigateToExplore,
                            onListingClick = { id, sid ->
                                huntTodayItems.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { pos ->
                                    viewModel.reportListingClick(huntTodayItems[pos], "hunt_today", pos)
                                }
                                onListingClick(id, sid)
                            },
                            onLike = onLikeListing,
                            onSave = onSaveListing,
                            onRecordView = { item ->
                                val pos = huntTodayItems.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                                viewModel.recordView(item, position = pos, surface = "hunt_today")
                            },
                        )
                    }
                }

                // Prefer id-aware chips when available; fall back to name-only wrapping.
                val styleChips = if (discovery.trendingStyleTagChips.isNotEmpty()) {
                    discovery.trendingStyleTagChips
                } else {
                    discovery.trendingStyleTags.map { TrendingTagChip(id = "", name = it) }
                }
                if (styleChips.isNotEmpty()) {
                    item {
                        HomeSectionReveal(sectionKey = "trending-styles") {
                            HomeTrendingStylesSection(
                                tags = styleChips,
                                onTagClick = { chip -> onNavigateToExploreWithTag(chip.name) },
                            )
                        }
                    }
                }

                if (discovery.forYou.size >= 2) {
                    item {
                        HomeSectionReveal(sectionKey = "for-you") {
                            HomeForYouSection(
                                items = discovery.forYou,
                                onListingClick = { id, sid ->
                                    discovery.forYou.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { pos ->
                                        viewModel.reportListingClick(discovery.forYou[pos], "recommendation_for_you", pos)
                                    }
                                    onListingClick(id, sid)
                                },
                                onLike = onLikeListing,
                                onSave = onSaveListing,
                                onSeeAllClick = onNavigateToExplore,
                                onRecordView = { item, pos -> viewModel.recordView(item, position = pos, surface = "recommendation_for_you") },
                            )
                        }
                    }
                }

                when {
                    isLoading && items.isEmpty() -> {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                HomeSectionHeader(
                                    title = stringResource(R.string.home_top_section_title),
                                    subtitle = stringResource(R.string.home_top_section_subtitle),
                                )
                                FashSkeletonGrid(
                                    modifier = Modifier.padding(top = FashTheme.spacing.spacing2),
                                    rows = 3,
                                    imageAspectRatio = 4f / 5f,
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
                    items.isEmpty() && isGuestBrowse -> {
                        item {
                            // Guest path: short hint only — explore + featured rails below already cover discovery.
                            HomeFollowFeedEmptyHint(
                                onFeaturedSellersClick = onOpenFeaturedSellersAll,
                                hintRes = R.string.home_guest_follow_hint,
                                showSectionHeader = false,
                                showFeaturedCta = false,
                            )
                        }
                    }
                    items.isEmpty() -> {
                        item {
                            // Rich empty card with dual CTA: Explore + Featured shops. Conversion-focused
                            // when the buyer follows nobody yet.
                            HomePersonalizedFeedEmptyCard(
                                onExploreClick = onNavigateToExplore,
                                onFeaturedSellersClick = onOpenFeaturedSellersAll,
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
                                row.forEachIndexed { col, feedItem ->
                                    val gridPosition = index * 2 + col
                                    LaunchedEffect(feedItem.id) {
                                        viewModel.recordView(feedItem, position = gridPosition, surface = "home")
                                    }
                                    ListingGridCard(
                                        item = feedItem,
                                        showQuickActions = true,
                                        onLike = { onLikeListing(feedItem) },
                                        onSave = { onSaveListing(feedItem) },
                                        onClick = {
                                            viewModel.reportListingClick(feedItem, "home", gridPosition)
                                            onListingClick(feedItem.id, feedItem.sellerId)
                                        },
                                        onDwell = { dwellMs ->
                                            viewModel.recordDwell(feedItem, "home", gridPosition, dwellMs)
                                        },
                                        modifier = Modifier.weight(1f),
                                        imageAspectRatio = 4f / 5f,
                                    )
                                }
                                repeat(2 - row.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        if (isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = FashColors.Primary,
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                        }
                    }
                }

                if (discovery.stylePicks.size >= 2) {
                    item {
                        HomeSectionReveal(sectionKey = "style-picks") {
                            HomeHuntTodaySection(
                                items = discovery.stylePicks,
                                isLoading = false,
                                onSeeAllClick = onNavigateToExplore,
                                onListingClick = { id, sid ->
                                    discovery.stylePicks.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { pos ->
                                        viewModel.reportListingClick(discovery.stylePicks[pos], "style_picks", pos)
                                    }
                                    onListingClick(id, sid)
                                },
                                onLike = onLikeListing,
                                onSave = onSaveListing,
                                onRecordView = { item ->
                                    val pos = discovery.stylePicks.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                                    viewModel.recordView(item, position = pos, surface = "style_picks")
                                },
                                titleRes = R.string.home_style_picks_title,
                                subtitleRes = R.string.home_style_picks_subtitle,
                            )
                        }
                    }
                }

                if (discovery.similarToSaved.size >= 2) {
                    item {
                        HomeSectionReveal(sectionKey = "similar-saved") {
                            HomeSimilarToSavedSection(
                                items = discovery.similarToSaved,
                                onListingClick = { id, sid ->
                                    discovery.similarToSaved.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { pos ->
                                        viewModel.reportListingClick(discovery.similarToSaved[pos], "similar_to_saved", pos)
                                    }
                                    onListingClick(id, sid)
                                },
                                onLike = onLikeListing,
                                onSave = onSaveListing,
                                onRecordView = { item, pos -> viewModel.recordView(item, position = pos, surface = "similar_to_saved") },
                            )
                        }
                    }
                }

                if (recentlyViewed.size >= 2) {
                    item {
                        HomeSectionReveal(sectionKey = "recently-viewed") {
                            HomeRecentlyViewedSection(
                                items = recentlyViewed,
                                onListingClick = onListingClick,
                            )
                        }
                    }
                }

                item {
                    HomeSectionReveal(sectionKey = "recommended-sellers") {
                        HomeRecommendedSellersSection(
                            sellers = recommendedSellers,
                            followingIds = followingIds,
                            onSellerClick = onFeaturedSellerClick,
                            onSeeAllClick = onOpenFeaturedSellersAll,
                        )
                    }
                }

                item {
                    HomeSectionReveal(sectionKey = "editorial") {
                        HomeEditorialPostsSection(
                            posts = discovery.editorialPosts,
                            onPostClick = onHomeEditorialPostClick,
                        )
                    }
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

/**
 * Inline Home prompt encouraging users who haven't entered sizing data to fill in their reference
 * size. Dismissible (one-shot, persisted via [HomeSizingBannerPreference]).
 */
@Composable
private fun HomeProfileSizingBanner(
    onCtaClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = FashTheme.spacing.editorialStart,
                end = FashTheme.spacing.editorialEnd,
                top = 8.dp,
                bottom = 4.dp,
            )
            .clip(shape),
        shape = shape,
        color = FashColors.Primary.copy(alpha = 0.08f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCtaClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Straighten,
                contentDescription = null,
                tint = FashColors.Primary,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_sizing_banner_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_sizing_banner_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onCtaClick) {
                Text(
                    text = stringResource(R.string.home_sizing_banner_cta),
                    color = FashColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = "dismiss"
                },
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.home_sizing_banner_dismiss_cd),
                    tint = scheme.onSurfaceVariant,
                )
            }
        }
    }
}
