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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import com.pc.fash_android_mobile.data.recommendation.HomeExploreShortcut
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.search.FeaturedSellerItem
import com.pc.fash_android_mobile.data.user.UserSearchResult
import com.pc.fash_android_mobile.ui.components.FashEmptyState
import com.pc.fash_android_mobile.ui.components.FashSkeletonGrid
import com.pc.fash_android_mobile.ui.feed.FeedErrorColumn
import com.pc.fash_android_mobile.ui.feed.FeedLoadMoreFooter
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
    ;

    companion object {
        fun guestTabs(): List<HomeFeedTab> = listOf(HuntToday, ForYou, Following)

        fun signedInTabs(): List<HomeFeedTab> = entries.toList()

        /** Tabs backed by `home-sections` (one API call covers all three). */
        fun recommendationSectionTabs(): Set<HomeFeedTab> = setOf(ForYou, StylePicks, SimilarSaved)

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
    followingIds: Set<String>,
    onFeaturedSellerClick: (UserSearchResult) -> Unit,
    onFeaturedSellersSeeAll: () -> Unit,
    huntTodayItems: List<ListingFeedItem>,
    shellLoading: Boolean,
    tabsLoading: Set<HomeFeedTab>,
    tabsLoadError: Set<HomeFeedTab>,
    forYouItems: List<ListingFeedItem>,
    followingItems: List<ListingFeedItem>,
    followingHasMore: Boolean,
    followingLoadingMore: Boolean,
    onLoadMoreFollowing: () -> Unit,
    onRetryTab: () -> Unit,
    stylePickItems: List<ListingFeedItem>,
    similarSavedItems: List<ListingFeedItem>,
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
        )
    }
    val isLoading = !showGuestGate && (
        safeSelected in tabsLoading ||
            (shellLoading && gridItems.isEmpty())
        )
    val loadError = !showGuestGate && safeSelected in tabsLoadError
    val hasMore = !showGuestGate && safeSelected == HomeFeedTab.Following && followingHasMore
    val gridState = rememberLazyStaggeredGridState()
    val masonryColumnWidthDp = rememberListingMasonryColumnWidthDp()
    val scheme = MaterialTheme.colorScheme
    val hasFeaturedSellers = featuredSellers.isNotEmpty()
    var tabSwipeConsuming by remember { mutableStateOf(false) }
    var suppressListingClicks by remember { mutableStateOf(false) }
    val swipeScope = rememberCoroutineScope()
    val selectedVisualIndex = tabs.indexOf(safeSelected).coerceAtLeast(0)
    val showJourneyRow = !isGuestBrowse && buyerStats.hasJourneyActivity()
    val showExploreShortcut = !isGuestBrowse && exploreShortcut != null
    val tabRowIndex = (if (showJourneyRow) 1 else 0) +
        (if (showSizingBanner && onOpenSizingSetup != null) 1 else 0) +
        (if (hasFeaturedSellers) 1 else 0) +
        (if (showExploreShortcut) 1 else 0)
    val listingStartIndex = tabRowIndex + 1
    val analyticsSurface = safeSelected.analyticsSurface
    val showStickyTabs by remember(tabRowIndex) {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return@derivedStateOf false
            !layoutInfo.visibleItemsInfo.any { it.index == tabRowIndex }
        }
    }

    LaunchedEffect(isGuestBrowse, tabs) {
        if (selectedTab !in tabs) {
            onTabSelected(HomeFeedTab.HuntToday)
        }
    }

    LaunchedEffect(onScrollToTopRequest) {
        onScrollToTopRequest.collect {
            gridState.animateScrollToItem(0)
        }
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
                    if (safeSelected == HomeFeedTab.Following && (hasMore || followingLoadingMore)) {
                        item(span = StaggeredGridItemSpan.FullLine, key = "home_feed_load_more") {
                            FeedLoadMoreFooter(
                                enabled = hasMore,
                                isLoadingMore = followingLoadingMore,
                                onLoadMore = onLoadMoreFollowing,
                            )
                        }
                    }
                }
            }

            item(span = StaggeredGridItemSpan.FullLine, key = "footer") {
                HomeBrandFooterStrip(includeHorizontalEdgePadding = false)
            }
        }

        if (showStickyTabs) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .zIndex(1f),
                color = scheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 2.dp,
            ) {
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
): List<ListingFeedItem> = when (tab) {
    HomeFeedTab.HuntToday -> huntTodayItems
    HomeFeedTab.ForYou -> forYouItems
    HomeFeedTab.Following -> followingItems
    HomeFeedTab.StylePicks -> stylePickItems
    HomeFeedTab.SimilarSaved -> similarSavedItems
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
