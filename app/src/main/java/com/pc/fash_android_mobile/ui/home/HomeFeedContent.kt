package com.pc.fash_android_mobile.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.data.home.HomeEditorialPostStub
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.user.UserSearchResult
import com.pc.fash_android_mobile.ui.components.FashPromoSlideDef
import com.pc.fash_android_mobile.ui.components.FashPromoSlider
import com.pc.fash_android_mobile.ui.components.FashStickyPromoDockHeight
import com.pc.fash_android_mobile.ui.components.StickyBottomPromoBar
import com.pc.fash_android_mobile.ui.explore.ExploreListingPreviewSheet
import com.pc.fash_android_mobile.ui.guest.GuestLoginReason
import com.pc.fash_android_mobile.ui.theme.FashColors

/**
 * Home tab: journey shortcuts, featured sellers, sticky feed tabs, virtualized masonry grid,
 * pinned promo above bottom nav.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedContent(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    onListingClick: (listingId: String, sellerId: String?) -> Unit = { _, _ -> },
    onNavigateToExplore: () -> Unit = {},
    onNavigateToExploreWithTag: (tagName: String) -> Unit = { onNavigateToExplore() },
    onNavigateToExploreWithShortcut: (com.pc.fash_android_mobile.data.recommendation.HomeExploreShortcut) -> Unit = { onNavigateToExplore() },
    onDeliveringJourneyClick: () -> Unit = {},
    onInReviewJourneyClick: () -> Unit = {},
    onNavigateToSaved: () -> Unit = {},
    onPromoSlideClick: (FashPromoSlideDef, Int) -> Unit = { _, _ -> onNavigateToExplore() },
    promoSlides: List<FashPromoSlideDef> = emptyList(),
    @Suppress("UNUSED_PARAMETER")
    onHomeEditorialPostClick: (HomeEditorialPostStub) -> Unit = {},
    onFeaturedSellerClick: (UserSearchResult) -> Unit = {},
    onOpenFeaturedSellersAll: () -> Unit = {},
    isGuestBrowse: Boolean = false,
    onRequestLogin: (GuestLoginReason) -> Unit = {},
    onOpenSizingSetup: (() -> Unit)? = null,
) {
    val ui by viewModel.feedUiState.collectAsState()
    val onLikeListing: (ListingFeedItem) -> Unit = { item ->
        if (isGuestBrowse) onRequestLogin(GuestLoginReason.Like) else viewModel.toggleLike(item)
    }
    val onSaveListing: (ListingFeedItem) -> Unit = { item ->
        if (isGuestBrowse) onRequestLogin(GuestLoginReason.Saved) else viewModel.toggleSave(item)
    }
    val pullState = rememberPullToRefreshState()
    val promoDockInset: Dp = if (promoSlides.isNotEmpty()) FashStickyPromoDockHeight else 0.dp

    LaunchedEffect(isGuestBrowse) {
        viewModel.normalizeSelectedFeedTab(isGuestBrowse)
    }

    var pendingListingDetail by remember { mutableStateOf<Pair<String, String?>?>(null) }
    LaunchedEffect(pendingListingDetail) {
        pendingListingDetail?.let { (listingId, sellerId) ->
            onListingClick(listingId, sellerId)
            pendingListingDetail = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = ui.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize(),
            state = pullState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = ui.isRefreshing,
                    color = FashColors.Primary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                HomeFeedTabHost(
                modifier = Modifier.fillMaxSize(),
                bottomScrollInset = promoDockInset,
                selectedTab = ui.selectedFeedTab,
                onTabSelected = viewModel::setSelectedFeedTab,
                orderedTabs = ui.orderedFeedTabs,
                exploreShortcut = ui.exploreShortcut,
                onExploreShortcutClick = {
                    val shortcut = ui.exploreShortcut ?: return@HomeFeedTabHost
                    onNavigateToExploreWithShortcut(shortcut)
                },
                featuredSellers = ui.featuredSellers,
                featuredSellersLoading = ui.featuredSellersLoading,
                followingIds = ui.followingIds,
                onFeaturedSellerClick = onFeaturedSellerClick,
                onFeaturedSellersSeeAll = onOpenFeaturedSellersAll,
                huntTodayItems = ui.discovery.huntToday,
                shellLoading = ui.isLoading,
                tabsLoading = ui.tabsLoading,
                tabsLoadError = ui.tabsLoadError,
                tabsLoadStalled = ui.tabsLoadStalled,
                forYouItems = ui.discovery.forYou,
                followingItems = ui.items,
                selectedTabHasMore = ui.selectedTabHasMore,
                selectedTabLoadingMore = ui.selectedTabLoadingMore,
                showBrandFooter = ui.showBrandFooter,
                onLoadMoreActiveTab = viewModel::loadMoreActiveTab,
                onRetryTab = { viewModel.retryTab(ui.selectedFeedTab) },
                stylePickItems = ui.discovery.stylePicks,
                similarSavedItems = ui.discovery.similarToSaved,
                seasonalNearYouItems = ui.discovery.seasonalNearYou,
                shoppingContextChip = ui.discovery.shoppingContext?.chipLabel(),
                isGuestBrowse = isGuestBrowse,
                showSizingBanner = ui.showSizingBanner && !isGuestBrowse,
                onDismissSizingBanner = viewModel::dismissSizingBanner,
                onOpenSizingSetup = onOpenSizingSetup,
                buyerStats = ui.buyerStats,
                onDeliveringJourneyClick = onDeliveringJourneyClick,
                onSavedJourneyClick = onNavigateToSaved,
                onInReviewJourneyClick = onInReviewJourneyClick,
                onLikeListing = onLikeListing,
                onSaveListing = onSaveListing,
                onListingClick = { item, index, surface ->
                    viewModel.openListingPreview(item, surface, index)
                },
                onRecordView = { item, index, surface ->
                    viewModel.recordView(item, position = index, surface = surface)
                },
                onDwell = { item, index, dwellMs, surface ->
                    viewModel.recordDwell(item, surface, index, dwellMs)
                },
                onRequestLogin = onRequestLogin,
                onScrollToTopRequest = viewModel.scrollHomeToTop,
                onScrollToFeedTopRequest = viewModel.scrollHomeFeedToTop,
            )

                if (promoSlides.isNotEmpty()) {
                    StickyBottomPromoBar(
                        elevated = false,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
                        FashPromoSlider(
                            modifier = Modifier.fillMaxWidth(),
                            slides = promoSlides,
                            onSlideClick = onPromoSlideClick,
                            reportPendingPaymentAnchor = true,
                        )
                    }
                }
            }
        }

        ui.listingPreview?.let { preview ->
            ExploreListingPreviewSheet(
                feedItem = preview.feedItem,
                detail = preview.detail,
                isDetailLoading = preview.isDetailLoading,
                onDismiss = { viewModel.closeListingPreview() },
                onViewDetail = {
                    val nav = viewModel.openListingDetailFromPreview()
                    if (nav != null) pendingListingDetail = nav
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
                        if (nav != null) pendingListingDetail = nav
                    }
                },
            )
        }
    }
}
