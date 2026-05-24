package com.pc.fash_android_mobile.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.search.FeaturedSellerItem
import com.pc.fash_android_mobile.data.user.UserSearchResult
import com.pc.fash_android_mobile.ui.components.FashEmptyState
import com.pc.fash_android_mobile.ui.components.FashSkeletonGrid
import com.pc.fash_android_mobile.ui.feed.FeedErrorColumn
import com.pc.fash_android_mobile.ui.feed.listingMasonryFeedRows
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
    featuredSellers: List<FeaturedSellerItem>,
    followingIds: Set<String>,
    onFeaturedSellerClick: (UserSearchResult) -> Unit,
    onFeaturedSellersSeeAll: () -> Unit,
    huntTodayItems: List<ListingFeedItem>,
    discoveryLoading: Boolean,
    discoveryLoadError: Boolean,
    forYouItems: List<ListingFeedItem>,
    followingItems: List<ListingFeedItem>,
    followingLoading: Boolean,
    followingLoadError: Boolean,
    followingHasMore: Boolean,
    followingLoadingMore: Boolean,
    onLoadMoreFollowing: () -> Unit,
    onRetryFollowing: () -> Unit,
    onRetryDiscovery: () -> Unit,
    stylePickItems: List<ListingFeedItem>,
    similarSavedItems: List<ListingFeedItem>,
    isGuestBrowse: Boolean,
    showSizingBanner: Boolean,
    onDismissSizingBanner: () -> Unit,
    onOpenSizingSetup: (() -> Unit)?,
    buyerStats: BuyerHomeStats,
    onDeliveringJourneyClick: () -> Unit,
    onSavedJourneyClick: () -> Unit,
    onMessagesJourneyClick: () -> Unit,
    onLikeListing: (ListingFeedItem) -> Unit,
    onSaveListing: (ListingFeedItem) -> Unit,
    onListingClick: (ListingFeedItem, Int, String) -> Unit,
    onRecordView: (ListingFeedItem, Int, String) -> Unit,
    onDwell: (ListingFeedItem, Int, Int, String) -> Unit,
    followingEmptyContent: @Composable () -> Unit,
    onRequestLogin: (GuestLoginReason) -> Unit,
    onScrollToTopRequest: kotlinx.coroutines.flow.SharedFlow<Unit>,
) {
    val tabs = remember(isGuestBrowse) { HomeFeedTab.tabsFor(isGuestBrowse) }
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
    val isLoading = !showGuestGate && tabLoadingFor(
        tab = safeSelected,
        discoveryLoading = discoveryLoading,
        followingLoading = followingLoading,
    )
    val loadError = !showGuestGate && when (safeSelected) {
        HomeFeedTab.Following -> followingLoadError
        else -> discoveryLoadError
    }
    val hasMore = !showGuestGate && safeSelected == HomeFeedTab.Following && followingHasMore
    val listState = rememberLazyListState()
    val edge = FashTheme.spacing.editorialStart
    val edgeEnd = FashTheme.spacing.editorialEnd
    val scheme = MaterialTheme.colorScheme
    var horizontalDrag by remember { mutableFloatStateOf(0f) }
    val hasFeaturedSellers = featuredSellers.isNotEmpty()
    val showJourneyRow = !isGuestBrowse && buyerStats.hasJourneyActivity()
    val headerItemCount = (if (hasFeaturedSellers) 1 else 0) +
        (if (showJourneyRow) 1 else 0) +
        (if (showSizingBanner) 1 else 0)
    val stickyTabIndex = headerItemCount
    val feedStartIndex = headerItemCount + 1
    val feedRowCount = (gridItems.size + 1) / 2

    LaunchedEffect(isGuestBrowse, tabs) {
        if (selectedTab !in tabs) {
            onTabSelected(HomeFeedTab.HuntToday)
        }
    }

    LaunchedEffect(onScrollToTopRequest) {
        onScrollToTopRequest.collect {
            listState.animateScrollToItem(0)
        }
    }

    var skipInitialTabScroll by remember { mutableStateOf(true) }

    LaunchedEffect(safeSelected) {
        if (skipInitialTabScroll) {
            skipInitialTabScroll = false
            return@LaunchedEffect
        }
        val maxIndex = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
        listState.animateScrollToItem(stickyTabIndex.coerceAtMost(maxIndex))
    }

    if (safeSelected == HomeFeedTab.Following && hasMore) {
        LaunchedEffect(listState, gridItems.size, hasMore, feedStartIndex, feedRowCount) {
            snapshotFlow {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible to gridItems.size
            }
                .distinctUntilChanged()
                .collect { (lastVisible, itemCount) ->
                    val rowCount = (itemCount + 1) / 2
                    if (itemCount <= 0 || rowCount <= 0) return@collect
                    val lastFeedRowIndex = feedStartIndex + rowCount - 1
                    if (lastVisible >= lastFeedRowIndex - 1) {
                        onLoadMoreFollowing()
                    }
                }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .pointerInput(tabs, safeSelected) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val threshold = 72f
                        when {
                            horizontalDrag <= -threshold -> {
                                val idx = tabs.indexOf(safeSelected)
                                if (idx < tabs.lastIndex) onTabSelected(tabs[idx + 1])
                            }
                            horizontalDrag >= threshold -> {
                                val idx = tabs.indexOf(safeSelected)
                                if (idx > 0) onTabSelected(tabs[idx - 1])
                            }
                        }
                        horizontalDrag = 0f
                    },
                    onHorizontalDrag = { _, delta -> horizontalDrag += delta },
                )
            },
        contentPadding = PaddingValues(
            bottom = FashTheme.spacing.spacing4 + bottomScrollInset,
        ),
    ) {
        if (showJourneyRow) {
            item(key = "home_journey_row") {
                BuyerHomeJourneyCompactBar(
                    stats = buyerStats,
                    onDeliveringClick = onDeliveringJourneyClick,
                    onSavedClick = onSavedJourneyClick,
                    onMessagesClick = onMessagesJourneyClick,
                )
            }
        }

        if (showSizingBanner && onOpenSizingSetup != null) {
            item(key = "home_sizing_banner") {
                HomeSizingBanner(
                    onAddSizeClick = onOpenSizingSetup,
                    onDismiss = onDismissSizingBanner,
                )
            }
        }

        if (hasFeaturedSellers) {
            item(key = "home_featured_sellers") {
                HomeRecommendedSellersSection(
                    sellers = featuredSellers,
                    followingIds = followingIds,
                    onSellerClick = onFeaturedSellerClick,
                    onSeeAllClick = onFeaturedSellersSeeAll,
                )
            }
        }

        stickyHeader(key = "home_feed_tabs") {
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
                item(key = "home_feed_loading_${safeSelected.name}") {
                    FashSkeletonGrid(
                        modifier = Modifier.padding(
                            start = edge,
                            end = edgeEnd,
                            top = 8.dp,
                        ),
                        rows = 4,
                        staggered = true,
                    )
                }
            }
            loadError && gridItems.isEmpty() -> {
                item(key = "home_feed_error_${safeSelected.name}") {
                    FeedErrorColumn(
                        message = stringResource(R.string.feed_load_error),
                        onRetry = if (safeSelected == HomeFeedTab.Following) onRetryFollowing else onRetryDiscovery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = edge, end = edgeEnd, top = 8.dp),
                    )
                }
            }
            gridItems.isEmpty() -> {
                item(key = "home_feed_empty_${safeSelected.name}") {
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
                            safeSelected == HomeFeedTab.Following -> followingEmptyContent()
                            else -> HomeFeedTabGenericEmpty(tab = safeSelected)
                        }
                    }
                }
            }
            else -> {
                listingMasonryFeedRows(
                    items = gridItems,
                    keyPrefix = "home_feed_${safeSelected.name}",
                    onLikeListing = onLikeListing,
                    onSaveListing = onSaveListing,
                    onListingClick = { item, index ->
                        onListingClick(item, index, safeSelected.analyticsSurface)
                    },
                    onRecordView = { item, index ->
                        onRecordView(item, index, safeSelected.analyticsSurface)
                    },
                    onDwell = { item, index, dwellMs ->
                        onDwell(item, index, dwellMs, safeSelected.analyticsSurface)
                    },
                )
                if (followingLoadingMore && safeSelected == HomeFeedTab.Following) {
                    item(key = "home_feed_loading_more") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = FashColors.Primary,
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
            }
        }

        item(key = "footer") {
            HomeBrandFooterStrip(
                modifier = Modifier.padding(horizontal = edge),
            )
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
        HomeFeedTab.Following -> R.string.home_feed_empty_title to R.string.home_feed_empty_subtitle
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

private fun tabLoadingFor(
    tab: HomeFeedTab,
    discoveryLoading: Boolean,
    followingLoading: Boolean,
): Boolean = when (tab) {
    HomeFeedTab.HuntToday, HomeFeedTab.ForYou, HomeFeedTab.StylePicks, HomeFeedTab.SimilarSaved -> discoveryLoading
    HomeFeedTab.Following -> followingLoading
}
