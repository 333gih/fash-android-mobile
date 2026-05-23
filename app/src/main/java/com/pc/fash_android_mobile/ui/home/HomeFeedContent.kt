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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
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
import com.pc.fash_android_mobile.ui.explore.ExploreListingPreviewSheet
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
    val hasMoreItems by viewModel.hasMoreItems.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val listingPreview by viewModel.listingPreview.collectAsState()
    val pullState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    val showJourneyRow = buyerStats.hasJourneyActivity()
    // Lazy item index of the inline promo row — must match item order below (journey → sizing banner → promo).
    val homePromoLazyIndex = remember(showJourneyRow, showSizingBanner, onOpenSizingSetup) {
        var idx = 0
        if (showJourneyRow) idx++
        if (showSizingBanner && onOpenSizingSetup != null) idx++
        idx
    }
    val recentlyViewed = discovery.recentlyViewed
    val recommendedSellers = discovery.recommendedSellers

    val styleChips = if (discovery.trendingStyleTagChips.isNotEmpty()) {
        discovery.trendingStyleTagChips
    } else {
        discovery.trendingStyleTags.map { TrendingTagChip(id = "", name = it) }
    }
    val followFeedLazyRange = remember(
        showJourneyRow,
        showSizingBanner,
        onOpenSizingSetup,
        styleChips.size,
        discovery.forYou.size,
        items.size,
        isLoading,
        loadError,
        isGuestBrowse,
    ) {
        computeFollowFeedLazyIndexRange(
            showJourneyRow = showJourneyRow,
            showSizingBanner = showSizingBanner && onOpenSizingSetup != null,
            styleChipCount = styleChips.size,
            forYouCount = discovery.forYou.size,
            feedItems = items,
            isLoading = isLoading,
            loadError = loadError,
            isGuestBrowse = isGuestBrowse,
        )
    }

    LaunchedEffect(Unit) {
        viewModel.scrollHomeToTop.collect {
            listState.animateScrollToItem(0)
        }
    }

    // Paginate follow feed only while the viewport is still **inside** the grid (not when style
    // picks / similar-saved are visible). Silent — no spinner overlay near the sticky promo bar.
    LaunchedEffect(hasMoreItems, isGuestBrowse, followFeedLazyRange) {
        if (isGuestBrowse || items.isEmpty()) return@LaunchedEffect
        val range = followFeedLazyRange ?: return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            val first = info.visibleItemsInfo.firstOrNull()?.index ?: -1
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            first to last
        }
            .debounce(180)
            .distinctUntilChanged()
            .collect { (first, last) ->
                if (first < 0 || last < 0) return@collect
                val viewportInsideFeed = first >= range.first && last <= range.last
                val atFeedBottom = last >= range.last - 1
                if (viewportInsideFeed && atFeedBottom && hasMoreItems) {
                    viewModel.loadMoreFollowFeed()
                }
            }
    }

    // Sticky promo: show only after the inline promo row has scrolled off the top. Using
    // firstVisibleItemIndex avoids oscillation at the promo boundary (checking visibleItems
    // toggles every frame when the row is partially visible). Render as a bottom overlay so
    // showing/hiding the bar does not resize the LazyColumn (that resize caused a feedback
    // loop — bar appears → list shrinks → promo re-enters viewport → bar hides → repeat).
    val showStickyPromo by remember(homePromoLazyIndex) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return@derivedStateOf false
            val firstVisible = layoutInfo.visibleItemsInfo.first().index
            firstVisible > homePromoLazyIndex
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
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
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
                            onListingClick = { id, _ ->
                                huntTodayItems.find { it.id == id }?.let { item ->
                                    val pos = huntTodayItems.indexOfFirst { it.id == id }.coerceAtLeast(0)
                                    viewModel.openListingPreview(item, "hunt_today", pos)
                                }
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
                                onListingClick = { id, _ ->
                                    discovery.forYou.find { it.id == id }?.let { item ->
                                        val pos = discovery.forYou.indexOfFirst { it.id == id }.coerceAtLeast(0)
                                        viewModel.openListingPreview(item, "recommendation_for_you", pos)
                                    }
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
                                            viewModel.openListingPreview(feedItem, "home", gridPosition)
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
                    }
                }

                if (discovery.stylePicks.size >= 2) {
                    item {
                        HomeHuntTodaySection(
                                items = discovery.stylePicks,
                                isLoading = false,
                                onSeeAllClick = onNavigateToExplore,
                                onListingClick = { id, _ ->
                                    discovery.stylePicks.find { it.id == id }?.let { item ->
                                        val pos = discovery.stylePicks.indexOfFirst { it.id == id }.coerceAtLeast(0)
                                        viewModel.openListingPreview(item, "style_picks", pos)
                                    }
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

                if (discovery.similarToSaved.size >= 2) {
                    item {
                        HomeSimilarToSavedSection(
                                items = discovery.similarToSaved,
                                onListingClick = { id, _ ->
                                    discovery.similarToSaved.find { it.id == id }?.let { item ->
                                        val pos = discovery.similarToSaved.indexOfFirst { it.id == id }.coerceAtLeast(0)
                                        viewModel.openListingPreview(item, "similar_to_saved", pos)
                                    }
                                },
                                onLike = onLikeListing,
                                onSave = onSaveListing,
                                onRecordView = { item, pos -> viewModel.recordView(item, position = pos, surface = "similar_to_saved") },
                            )
                    }
                }

                if (recentlyViewed.size >= 2) {
                    item {
                        HomeRecentlyViewedSection(
                                items = recentlyViewed,
                                onListingClick = { id, _ ->
                                    recentlyViewed.find { it.id == id }?.let { item ->
                                        val pos = recentlyViewed.indexOfFirst { it.id == id }.coerceAtLeast(0)
                                        viewModel.openListingPreview(item, "recently_viewed", pos)
                                    }
                                },
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
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
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

        listingPreview?.let { preview ->
            ExploreListingPreviewSheet(
                feedItem = preview.feedItem,
                detail = preview.detail,
                isDetailLoading = preview.isDetailLoading,
                onDismiss = { viewModel.closeListingPreview() },
                onViewDetail = {
                    val nav = viewModel.openListingDetailFromPreview()
                    if (nav != null) onListingClick(nav.first, nav.second)
                },
                onLike = { viewModel.toggleLike(preview.feedItem) },
                onSave = { viewModel.toggleSave(preview.feedItem) },
                isGuestMode = isGuestBrowse,
                onRequestLogin = onRequestLogin,
                onMessageSeller = {
                    if (isGuestBrowse) {
                        onRequestLogin(GuestLoginReason.BuyOrChat)
                    } else {
                        val nav = viewModel.openChatFromPreview()
                        if (nav != null) onListingClick(nav.first, nav.second)
                    }
                },
            )
        }
    }
}

/**
 * LazyColumn index range for the follow-feed block (header + grid rows). Used to scope infinite
 * scroll so pagination does not run while the user reads discovery rails below the grid.
 */
private fun computeFollowFeedLazyIndexRange(
    showJourneyRow: Boolean,
    showSizingBanner: Boolean,
    styleChipCount: Int,
    forYouCount: Int,
    feedItems: List<ListingFeedItem>,
    isLoading: Boolean,
    loadError: Boolean,
    isGuestBrowse: Boolean,
): IntRange? {
    var idx = 0
    if (showJourneyRow) idx++
    if (showSizingBanner) idx++
    idx += 3 // promo, quick actions, hunt today
    if (styleChipCount > 0) idx++
    if (forYouCount >= 2) idx++
    val start = idx
    return when {
        isLoading && feedItems.isEmpty() -> start..start
        loadError && feedItems.isEmpty() -> start..start
        feedItems.isEmpty() -> start..start
        isGuestBrowse && feedItems.isEmpty() -> start..start
        else -> {
            val rowCount = feedItems.chunked(2).size
            // Header at `start`, grid rows at start+1 … start+rowCount.
            start..(start + rowCount)
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
