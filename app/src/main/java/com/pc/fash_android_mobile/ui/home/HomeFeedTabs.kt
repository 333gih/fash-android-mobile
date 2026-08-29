package com.pc.fash_android_mobile.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import com.pc.fash_android_mobile.ui.components.LocalFashTabSwipeConsuming
import com.pc.fash_android_mobile.ui.components.fashTabSwipe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import com.pc.fash_android_mobile.data.recommendation.HomeExploreShortcut
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.search.FeaturedSellerItem
import com.pc.fash_android_mobile.data.user.UserSearchResult
import com.pc.fash_android_mobile.ui.components.FashEmptyState
import com.pc.fash_android_mobile.ui.components.FashSkeletonGrid
import com.pc.fash_android_mobile.ui.feed.FeedEmptyColumn
import com.pc.fash_android_mobile.ui.feed.FeedErrorColumn
import com.pc.fash_android_mobile.ui.feed.FeedLoadMoreFooter
import com.pc.fash_android_mobile.ui.feed.FeedStaggeredGridScrollPreserveEffect
import com.pc.fash_android_mobile.ui.feed.ListingGridCard
import com.pc.fash_android_mobile.ui.feed.listingMasonryAspectRatio
import com.pc.fash_android_mobile.ui.feed.listingMasonryTileSize
import com.pc.fash_android_mobile.ui.feed.rememberListingMasonryColumnWidthDp
import com.pc.fash_android_mobile.ui.guest.GuestLoginReason
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import kotlinx.coroutines.flow.distinctUntilChanged

/** Home discovery tabs — swipeable like TikTok, each tab is a masonry product grid. */
enum class HomeFeedTab(
    @StringRes val titleRes: Int,
    val analyticsSurface: String,
    val requiresAuth: Boolean = false,
    @StringRes val guestTitleRes: Int = 0,
    @StringRes val guestBodyRes: Int = 0,
    val guestLoginReason: GuestLoginReason = GuestLoginReason.TopBar,
) {
    HuntToday(R.string.home_hunt_today_title, "hunt_today"),
    ForYou(
        titleRes = R.string.home_section_for_you_title,
        analyticsSurface = "recommendation_for_you",
        requiresAuth = true,
        guestTitleRes = R.string.home_guest_tab_for_you_title,
        guestBodyRes = R.string.home_guest_tab_for_you_body,
        guestLoginReason = GuestLoginReason.HomeForYou,
    ),
    Following(
        titleRes = R.string.home_top_section_title,
        analyticsSurface = "home",
        requiresAuth = true,
        guestTitleRes = R.string.home_guest_tab_following_title,
        guestBodyRes = R.string.home_guest_tab_following_body,
        guestLoginReason = GuestLoginReason.HomeFollowing,
    ),
    StylePicks(
        titleRes = R.string.home_style_picks_title,
        analyticsSurface = "style_picks",
        requiresAuth = true,
        guestTitleRes = R.string.home_guest_tab_style_title,
        guestBodyRes = R.string.home_guest_tab_style_body,
        guestLoginReason = GuestLoginReason.HomeStylePicks,
    ),
    SimilarSaved(
        titleRes = R.string.home_section_similar_to_saved_title,
        analyticsSurface = "similar_to_saved",
        requiresAuth = true,
        guestTitleRes = R.string.home_guest_tab_similar_title,
        guestBodyRes = R.string.home_guest_tab_similar_body,
        guestLoginReason = GuestLoginReason.HomeSimilarSaved,
    ),
    SeasonalNearYou(
        titleRes = R.string.home_seasonal_near_you_title,
        analyticsSurface = "seasonal_near_you",
        requiresAuth = true,
        guestTitleRes = R.string.home_guest_tab_seasonal_title,
        guestBodyRes = R.string.home_guest_tab_seasonal_body,
        guestLoginReason = GuestLoginReason.HomeForYou,
    ),
    ;

    companion object {
        fun guestTabs(): List<HomeFeedTab> = listOf(HuntToday, ForYou, Following)

        fun signedInTabs(): List<HomeFeedTab> = entries.toList()

        /** Tabs backed by `home-sections` (one API call covers all three). */
        fun recommendationSectionTabs(): Set<HomeFeedTab> =
            setOf(ForYou, StylePicks, SimilarSaved, SeasonalNearYou)

        fun tabsFor(isGuestBrowse: Boolean): List<HomeFeedTab> =
            if (isGuestBrowse) guestTabs() else signedInTabs()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeFeedTabHost(
    modifier: Modifier = Modifier,
    bottomScrollInset: Dp = 0.dp,
    selectedTab: HomeFeedTab,
    onTabSelected: (HomeFeedTab) -> Unit,
    orderedTabs: List<HomeFeedTab> = HomeFeedTab.signedInTabs(),
    exploreShortcut: com.pc.fash_android_mobile.data.recommendation.HomeExploreShortcut? = null,
    onExploreShortcutClick: () -> Unit = {},
    featuredSellers: List<FeaturedSellerItem>,
    featuredSellersLoading: Boolean = false,
    followingIds: Set<String>,
    onFeaturedSellerClick: (UserSearchResult) -> Unit,
    onFeaturedSellersSeeAll: () -> Unit,
    huntTodayItems: List<ListingFeedItem>,
    shellLoading: Boolean,
    tabsLoading: Set<HomeFeedTab>,
    tabsLoadError: Set<HomeFeedTab>,
    tabsLoadStalled: Set<HomeFeedTab>,
    forYouItems: List<ListingFeedItem>,
    followingItems: List<ListingFeedItem>,
    selectedTabHasMore: Boolean,
    selectedTabLoadingMore: Boolean,
    showBrandFooter: Boolean,
    onLoadMoreActiveTab: () -> Unit,
    onRetryTab: () -> Unit,
    stylePickItems: List<ListingFeedItem>,
    similarSavedItems: List<ListingFeedItem>,
    seasonalNearYouItems: List<ListingFeedItem>,
    dailyOutfitDropSets: List<com.pc.fash_android_mobile.data.recommendation.OutfitSetCard> = emptyList(),
    onOutfitListingClick: (String) -> Unit = {},
    shoppingContextChip: String? = null,
    isGuestBrowse: Boolean,
    showSizingBanner: Boolean,
    onDismissSizingBanner: () -> Unit,
    onOpenSizingSetup: (() -> Unit)?,
    buyerStats: BuyerHomeStats,
    onDeliveringJourneyClick: () -> Unit,
    onSavedJourneyClick: () -> Unit,
    onInReviewJourneyClick: () -> Unit,
    onLikeListing: (ListingFeedItem) -> Unit,
    onSaveListing: (ListingFeedItem) -> Unit,
    onListingClick: (ListingFeedItem, Int, String) -> Unit,
    onRecordView: (ListingFeedItem, Int, String) -> Unit,
    onDwell: (ListingFeedItem, Int, Int, String) -> Unit,
    onRequestLogin: (GuestLoginReason) -> Unit,
    onScrollToTopRequest: kotlinx.coroutines.flow.SharedFlow<Unit>,
    onScrollToFeedTopRequest: kotlinx.coroutines.flow.SharedFlow<Unit>,
) {
    val tabs = remember(isGuestBrowse, orderedTabs) {
        val allowed = HomeFeedTab.tabsFor(isGuestBrowse)
        orderedTabs.filter { it in allowed }.ifEmpty { allowed }
    }
    val safeSelected = if (selectedTab in tabs) selectedTab else HomeFeedTab.HuntToday
    val showGuestGate = isGuestBrowse && safeSelected.requiresAuth
    val gridItems = if (showGuestGate) {
        emptyList()
    } else {
        tabItemsFor(
            tab = safeSelected,
            huntTodayItems = huntTodayItems,
            forYouItems = forYouItems,
            followingItems = followingItems,
            stylePickItems = stylePickItems,
            similarSavedItems = similarSavedItems,
            seasonalNearYouItems = seasonalNearYouItems,
        )
    }
    val isLoading = !showGuestGate && (
        safeSelected in tabsLoading && gridItems.isEmpty()
        )
    val loadError = !showGuestGate && safeSelected in tabsLoadError
    val loadStall = !showGuestGate && safeSelected in tabsLoadStalled
    val hasMore = !showGuestGate && selectedTabHasMore
    val loadingMore = !showGuestGate && selectedTabLoadingMore
    val gridStates = remember { mutableMapOf<HomeFeedTab, androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState>() }
    val gridState = gridStates.getOrPut(safeSelected) {
        androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState()
    }
    FeedStaggeredGridScrollPreserveEffect(
        state = gridState,
        itemCount = gridItems.size,
        enabled = !showGuestGate && gridItems.isNotEmpty(),
    )
    val masonryColumnWidthDp = rememberListingMasonryColumnWidthDp()
    val scheme = MaterialTheme.colorScheme
    val hasFeaturedSellers = featuredSellers.isNotEmpty()
    val showFeaturedSellersSkeleton = featuredSellersLoading && featuredSellers.isEmpty()
    // Guest home: reserve featured-sellers rail while shell loads so tabs are never the first row.
    val showGuestFeaturedSkeleton = isGuestBrowse && featuredSellers.isEmpty() &&
        (featuredSellersLoading || shellLoading)
    val hasFeaturedSellersBlock = hasFeaturedSellers || showFeaturedSellersSkeleton || showGuestFeaturedSkeleton
    val hasOutfitDrop = dailyOutfitDropSets.isNotEmpty()
    var tabSwipeConsuming by remember { mutableStateOf(false) }
    var suppressListingClicks by remember { mutableStateOf(false) }
    val swipeScope = rememberCoroutineScope()
    val selectedVisualIndex = tabs.indexOf(safeSelected).coerceAtLeast(0)
    val showJourneyRow = !isGuestBrowse
    val showExploreShortcut = !isGuestBrowse && exploreShortcut != null
    val tabRowIndex = (if (!shoppingContextChip.isNullOrBlank()) 1 else 0) +
        (if (showJourneyRow) 1 else 0) +
        (if (showSizingBanner && onOpenSizingSetup != null) 1 else 0) +
        (if (hasFeaturedSellersBlock) 1 else 0) +
        (if (hasOutfitDrop) 1 else 0) +
        (if (showExploreShortcut) 1 else 0)
    var stableTabRowIndex by remember { mutableIntStateOf(-1) }
    LaunchedEffect(tabRowIndex, shellLoading, featuredSellersLoading) {
        if (stableTabRowIndex < 0 && !shellLoading && !featuredSellersLoading) {
            stableTabRowIndex = tabRowIndex
        }
    }
    val scrollTabRowIndex = if (stableTabRowIndex >= 0) stableTabRowIndex else tabRowIndex
    val listingStartIndex = tabRowIndex + 1
    val analyticsSurface = safeSelected.analyticsSurface
    var stickyTabsLatch by remember(tabRowIndex) { mutableStateOf(false) }
    // Guests: never pin tabs — featured sellers above tabs caused sticky latch and blocked scroll to top.
    val enableStickyTabs = !isGuestBrowse && tabRowIndex > 0
    LaunchedEffect(gridState, tabRowIndex, enableStickyTabs) {
        if (!enableStickyTabs) {
            stickyTabsLatch = false
            return@LaunchedEffect
        }
        snapshotFlow {
            gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
        }
            .distinctUntilChanged()
            .collect { (firstIndex, firstOffset) ->
                val tabsFullyVisible = firstIndex < tabRowIndex ||
                    (firstIndex == tabRowIndex && firstOffset <= 4)
                if (tabsFullyVisible) {
                    stickyTabsLatch = false
                } else if (firstIndex > tabRowIndex ||
                    (firstIndex == tabRowIndex && firstOffset > 4)
                ) {
                    stickyTabsLatch = true
                }
            }
    }
    val showStickyTabs = enableStickyTabs && stickyTabsLatch

    LaunchedEffect(isGuestBrowse, tabRowIndex) {
        if (isGuestBrowse) {
            stickyTabsLatch = false
        }
    }

    LaunchedEffect(isGuestBrowse, tabs) {
        if (selectedTab !in tabs) {
            onTabSelected(HomeFeedTab.HuntToday)
        }
    }

    LaunchedEffect(onScrollToTopRequest) {
        onScrollToTopRequest.collect {
            stickyTabsLatch = false
            gridState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(onScrollToFeedTopRequest, scrollTabRowIndex, isGuestBrowse) {
        onScrollToFeedTopRequest.collect {
            stickyTabsLatch = false
            val targetIndex = if (isGuestBrowse) 0 else scrollTabRowIndex
            gridState.animateScrollToItem(targetIndex)
        }
    }

    var guestColdStartScrollPinned by remember { mutableStateOf(false) }
    // Guest cold start: pin to absolute top after waiting screen (parity with bottom-nav re-tap / reload).
    LaunchedEffect(isGuestBrowse, shellLoading, hasFeaturedSellersBlock) {
        if (!isGuestBrowse || guestColdStartScrollPinned) return@LaunchedEffect
        if (shellLoading && gridItems.isEmpty()) return@LaunchedEffect
        guestColdStartScrollPinned = true
        stickyTabsLatch = false
        gridState.scrollToItem(0)
        delay(80)
        gridState.scrollToItem(0)
        delay(200)
        gridState.scrollToItem(0)
    }

    CompositionLocalProvider(
        LocalFashTabSwipeConsuming provides (tabSwipeConsuming || suppressListingClicks),
    ) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .fashTabSwipe(
                    tabCount = tabs.size,
                    currentVisualIndex = selectedVisualIndex,
                    onVisualIndexChanged = { index -> onTabSelected(tabs[index]) },
                    onConsumingChanged = { active ->
                        tabSwipeConsuming = active
                        if (active) suppressListingClicks = true
                    },
                    onTabSwipeCommitted = {
                        suppressListingClicks = true
                        swipeScope.launch {
                            delay(320)
                            suppressListingClicks = false
                        }
                    },
                ),
            contentPadding = PaddingValues(
                start = FashTheme.spacing.editorialStart,
                end = FashTheme.spacing.editorialEnd,
                bottom = FashTheme.spacing.spacing4 + bottomScrollInset,
            ),
            horizontalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing2),
            verticalItemSpacing = FashTheme.spacing.spacing2,
        ) {
            if (!shoppingContextChip.isNullOrBlank()) {
                item(span = StaggeredGridItemSpan.FullLine, key = "home_shopping_context_chip") {
                    val scheme = MaterialTheme.colorScheme
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        shape = RoundedCornerShape(50),
                        color = scheme.surfaceContainerLow,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Eco,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = com.pc.fash_android_mobile.ui.theme.FashColors.Primary,
                            )
                            Text(
                                text = shoppingContextChip!!,
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            if (showJourneyRow) {
                item(span = StaggeredGridItemSpan.FullLine, key = "home_journey_row") {
                    BuyerHomeJourneyCompactBar(
                        stats = buyerStats,
                        onDeliveringClick = onDeliveringJourneyClick,
                        onSavedClick = onSavedJourneyClick,
                        onInReviewClick = onInReviewJourneyClick,
                        modifier = Modifier.fillMaxWidth(),
                        includeHorizontalEdgePadding = false,
                    )
                }
            }

            if (showSizingBanner && onOpenSizingSetup != null) {
                item(span = StaggeredGridItemSpan.FullLine, key = "home_sizing_banner") {
                    HomeSizingBanner(
                        onAddSizeClick = onOpenSizingSetup,
                        onDismiss = onDismissSizingBanner,
                        includeHorizontalEdgePadding = false,
                    )
                }
            }

            if (hasFeaturedSellers) {
                item(span = StaggeredGridItemSpan.FullLine, key = "home_featured_sellers") {
                    HomeRecommendedSellersSection(
                        sellers = featuredSellers,
                        followingIds = followingIds,
                        onSellerClick = onFeaturedSellerClick,
                        onSeeAllClick = onFeaturedSellersSeeAll,
                        includeHorizontalEdgePadding = false,
                    )
                }
            } else if (showFeaturedSellersSkeleton || showGuestFeaturedSkeleton) {
                item(span = StaggeredGridItemSpan.FullLine, key = "home_featured_sellers_skeleton") {
                    HomeRecommendedSellersSkeleton(includeHorizontalEdgePadding = false)
                }
            }

            if (hasOutfitDrop) {
                item(span = StaggeredGridItemSpan.FullLine, key = "home_daily_outfit_drop") {
                    HomeDailyOutfitDropSection(
                        sets = dailyOutfitDropSets,
                        onListingClick = onOutfitListingClick,
                        includeHorizontalEdgePadding = false,
                    )
                }
            }

            if (showExploreShortcut && exploreShortcut != null) {
                item(span = StaggeredGridItemSpan.FullLine, key = "home_explore_shortcut") {
                    HomeExploreShortcutBanner(
                        shortcut = exploreShortcut,
                        onClick = onExploreShortcutClick,
                        includeHorizontalEdgePadding = false,
                    )
                }
            }

            item(span = StaggeredGridItemSpan.FullLine, key = "home_feed_tabs") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = scheme.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    HomeFeedTabSwitcher(
                        tabs = tabs,
                        selectedIndex = tabs.indexOf(safeSelected).coerceAtLeast(0),
                        isGuestBrowse = isGuestBrowse,
                        onSelect = { index -> onTabSelected(tabs[index]) },
                    )
                }
            }

            when {
                isLoading && gridItems.isEmpty() -> {
                    item(span = StaggeredGridItemSpan.FullLine, key = "home_feed_loading_${safeSelected.name}") {
                        FashSkeletonGrid(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            rows = 4,
                            staggered = true,
                        )
                    }
                }
                loadError && gridItems.isEmpty() -> {
                    item(span = StaggeredGridItemSpan.FullLine, key = "home_feed_error_${safeSelected.name}") {
                        FeedErrorColumn(
                            message = stringResource(R.string.feed_load_error),
                            onRetry = onRetryTab,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        )
                    }
                }
                loadStall && gridItems.isEmpty() -> {
                    item(span = StaggeredGridItemSpan.FullLine, key = "home_feed_stall_${safeSelected.name}") {
                        FeedEmptyColumn(
                            title = stringResource(R.string.feed_load_error),
                            subtitle = stringResource(R.string.feed_load_stall_subtitle),
                            primaryActionLabel = stringResource(R.string.feed_retry),
                            onPrimaryAction = onRetryTab,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        )
                    }
                }
                gridItems.isEmpty() -> {
                    item(span = StaggeredGridItemSpan.FullLine, key = "home_feed_empty_${safeSelected.name}") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        ) {
                            when {
                                showGuestGate -> HomeFeedTabGuestGate(
                                    tab = safeSelected,
                                    onSignIn = { onRequestLogin(safeSelected.guestLoginReason) },
                                )
                                else -> HomeFeedTabGenericEmpty(tab = safeSelected)
                            }
                        }
                    }
                }
                else -> {
                    itemsIndexed(
                        items = gridItems,
                        key = { _, item -> "home_feed_${safeSelected.name}_${item.id}" },
                    ) { index, item ->
                        LaunchedEffect(item.id) {
                            onRecordView(item, index, analyticsSurface)
                        }
                        ListingGridCard(
                            item = item,
                            onClick = { onListingClick(item, index, analyticsSurface) },
                            onDwell = { dwellMs -> onDwell(item, index, dwellMs, analyticsSurface) },
                            imageAspectRatio = listingMasonryAspectRatio(item),
                            columnWidthDp = masonryColumnWidthDp,
                            modifier = Modifier.listingMasonryTileSize(masonryColumnWidthDp, item),
                            showQuickActions = true,
                            onLike = { onLikeListing(item) },
                            onSave = { onSaveListing(item) },
                        )
                    }
                    if (hasMore || loadingMore) {
                        item(span = StaggeredGridItemSpan.FullLine, key = "home_feed_load_more") {
                            FeedLoadMoreFooter(
                                enabled = hasMore,
                                isLoadingMore = loadingMore,
                                anchorItemCount = gridItems.size,
                                onLoadMore = onLoadMoreActiveTab,
                            )
                        }
                    }
                }
            }

            if (showBrandFooter) {
                item(span = StaggeredGridItemSpan.FullLine, key = "footer") {
                    HomeBrandFooterStrip(includeHorizontalEdgePadding = false)
                }
            }
        }

        if (showStickyTabs) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .zIndex(20f),
                color = scheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 2.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HomeFeedTabSwitcher(
                        tabs = tabs,
                        selectedIndex = tabs.indexOf(safeSelected).coerceAtLeast(0),
                        isGuestBrowse = isGuestBrowse,
                        onSelect = { index -> onTabSelected(tabs[index]) },
                        modifier = Modifier.padding(
                            start = FashTheme.spacing.editorialStart,
                            end = FashTheme.spacing.editorialEnd,
                        ),
                    )
                    HorizontalDivider(
                        color = scheme.outlineVariant.copy(alpha = 0.35f),
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun HomeFeedTabSwitcher(
    tabs: List<HomeFeedTab>,
    selectedIndex: Int,
    isGuestBrowse: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier.fillMaxWidth(),
        edgePadding = 0.dp,
        containerColor = scheme.surface,
        contentColor = scheme.onSurface,
        divider = {},
        indicator = { positions ->
            if (selectedIndex in positions.indices) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(positions[selectedIndex])
                        .padding(horizontal = 6.dp),
                    height = 2.dp,
                    color = FashColors.Primary,
                )
            }
        },
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            Tab(
                selected = selected,
                onClick = { onSelect(index) },
                modifier = Modifier.padding(horizontal = 2.dp),
                text = {
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (isGuestBrowse && tab.requiresAuth) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (selected) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.75f),
                            )
                        }
                        Text(
                            text = stringResource(tab.titleRes),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.75f),
                        )
                    }
                },
            )
        }
    }
}

@Composable
fun HomeFeedTabGuestGate(
    tab: HomeFeedTab,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FashEmptyState(
            icon = Icons.Outlined.Lock,
            title = stringResource(tab.guestTitleRes),
            subtitle = stringResource(tab.guestBodyRes),
            scrollable = false,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        val signInCd = stringResource(R.string.home_guest_tab_sign_in_cd)
        Button(
            onClick = onSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = signInCd },
        ) {
            Text(stringResource(R.string.home_guest_tab_sign_in))
        }
    }
}

@Composable
private fun HomeFeedTabGenericEmpty(tab: HomeFeedTab) {
    val (titleRes, subtitleRes) = when (tab) {
        HomeFeedTab.HuntToday -> R.string.home_tab_empty_hunt_title to R.string.home_tab_empty_hunt_subtitle
        HomeFeedTab.ForYou -> R.string.home_tab_empty_for_you_title to R.string.home_tab_empty_for_you_subtitle
        HomeFeedTab.StylePicks -> R.string.home_tab_empty_style_title to R.string.home_tab_empty_style_subtitle
        HomeFeedTab.SimilarSaved -> R.string.home_tab_empty_similar_title to R.string.home_tab_empty_similar_subtitle
        HomeFeedTab.SeasonalNearYou -> R.string.home_tab_empty_seasonal_title to R.string.home_tab_empty_seasonal_subtitle
        HomeFeedTab.Following -> R.string.home_tab_empty_following_title to R.string.home_tab_empty_following_subtitle
    }
    FashEmptyState(
        icon = Icons.Outlined.Inventory2,
        title = stringResource(titleRes),
        subtitle = stringResource(subtitleRes),
        scrollable = false,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
    )
}

private fun tabItemsFor(
    tab: HomeFeedTab,
    huntTodayItems: List<ListingFeedItem>,
    forYouItems: List<ListingFeedItem>,
    followingItems: List<ListingFeedItem>,
    stylePickItems: List<ListingFeedItem>,
    similarSavedItems: List<ListingFeedItem>,
    seasonalNearYouItems: List<ListingFeedItem>,
): List<ListingFeedItem> = when (tab) {
    HomeFeedTab.HuntToday -> huntTodayItems
    HomeFeedTab.ForYou -> forYouItems
    HomeFeedTab.Following -> followingItems
    HomeFeedTab.StylePicks -> stylePickItems
    HomeFeedTab.SimilarSaved -> similarSavedItems
    HomeFeedTab.SeasonalNearYou -> seasonalNearYouItems
}

@Composable
fun HomeExploreShortcutBanner(
    shortcut: HomeExploreShortcut,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    includeHorizontalEdgePadding: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val edgeStart = if (includeHorizontalEdgePadding) FashTheme.spacing.editorialStart else 0.dp
    val edgeEnd = if (includeHorizontalEdgePadding) FashTheme.spacing.editorialEnd else 0.dp
    val labelRes = when (shortcut.labelKey) {
        "home_explore_shortcut_category" -> R.string.home_explore_shortcut_category
        else -> R.string.home_explore_shortcut_style
    }
    val detail = shortcut.aestheticTagName?.takeIf { it.isNotBlank() }
        ?: shortcut.aestheticTagId?.takeIf { it.isNotBlank() }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = edgeStart, end = edgeEnd, top = 4.dp, bottom = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = scheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = FashColors.Primary,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!detail.isNullOrBlank()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = stringResource(R.string.home_explore_shortcut_action),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = FashColors.Primary,
            )
        }
    }
}
