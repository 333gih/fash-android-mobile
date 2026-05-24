package com.pc.fash_android_mobile.ui.home

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.home.HomeEditorialPostStub
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.order.OrderItem
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
 * pinned promo above bottom nav. Journey hubs (Đang giao / Đang duyệt) replace the feed in-place
 * so bottom nav + promo slider stay visible.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedContent(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    journeyHub: HomeJourneyHub = HomeJourneyHub.Feed,
    onJourneyHubChange: (HomeJourneyHub) -> Unit = {},
    deliveringViewModel: HomeDeliveringViewModel? = null,
    inReviewViewModel: HomeInReviewViewModel? = null,
    onOrderClick: (OrderItem) -> Unit = {},
    onInReviewListingClick: (ListingFeedItem) -> Unit = {},
    onListingClick: (listingId: String, sellerId: String?) -> Unit = { _, _ -> },
    onNavigateToExplore: () -> Unit = {},
    onNavigateToExploreWithTag: (tagName: String) -> Unit = { onNavigateToExplore() },
    onOrdersClick: () -> Unit = {},
    onDeliveringJourneyClick: () -> Unit = { onJourneyHubChange(HomeJourneyHub.Delivering) },
    onInReviewJourneyClick: () -> Unit = { onJourneyHubChange(HomeJourneyHub.InReview) },
    onNavigateToSaved: () -> Unit = {},
    onNavigateToPost: () -> Unit = {},
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
    when (journeyHub) {
        HomeJourneyHub.Delivering -> {
            BackHandler { onJourneyHubChange(HomeJourneyHub.Feed) }
            val vm = deliveringViewModel ?: return
            LaunchedEffect(Unit) {
                vm.loadIfShippingEnabled(AppEnvironment.shippingEnabled)
            }
            HomeDeliveringScreen(
                modifier = modifier.fillMaxSize(),
                viewModel = vm,
                onBack = { onJourneyHubChange(HomeJourneyHub.Feed) },
                onOrderClick = onOrderClick,
                onOpenAllOrders = {
                    onJourneyHubChange(HomeJourneyHub.Feed)
                    onOrdersClick()
                },
                onDataMutated = { viewModel.refresh() },
                embeddedInMainNav = true,
                promoSlides = promoSlides,
                onPromoSlideClick = onPromoSlideClick,
            )
        }
        HomeJourneyHub.InReview -> {
            BackHandler { onJourneyHubChange(HomeJourneyHub.Feed) }
            val vm = inReviewViewModel ?: return
            LaunchedEffect(Unit) { vm.loadIfNeeded() }
            HomeInReviewScreen(
                modifier = modifier.fillMaxSize(),
                viewModel = vm,
                onBack = { onJourneyHubChange(HomeJourneyHub.Feed) },
                onListingClick = onInReviewListingClick,
                onOpenPostListing = {
                    onJourneyHubChange(HomeJourneyHub.Feed)
                    onNavigateToPost()
                },
                onDataMutated = { viewModel.refresh() },
                embeddedInMainNav = true,
                promoSlides = promoSlides,
                onPromoSlideClick = onPromoSlideClick,
            )
        }
        HomeJourneyHub.Feed -> HomeFeedMainContent(
            modifier = modifier,
            viewModel = viewModel,
            onListingClick = onListingClick,
            onNavigateToExplore = onNavigateToExplore,
            onDeliveringJourneyClick = onDeliveringJourneyClick,
            onInReviewJourneyClick = onInReviewJourneyClick,
            onNavigateToSaved = onNavigateToSaved,
            onPromoSlideClick = onPromoSlideClick,
            promoSlides = promoSlides,
            onFeaturedSellerClick = onFeaturedSellerClick,
            onOpenFeaturedSellersAll = onOpenFeaturedSellersAll,
            isGuestBrowse = isGuestBrowse,
            onRequestLogin = onRequestLogin,
            onOpenSizingSetup = onOpenSizingSetup,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeFeedMainContent(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    onListingClick: (listingId: String, sellerId: String?) -> Unit,
    onNavigateToExplore: () -> Unit,
    onDeliveringJourneyClick: () -> Unit,
    onInReviewJourneyClick: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onPromoSlideClick: (FashPromoSlideDef, Int) -> Unit,
    promoSlides: List<FashPromoSlideDef>,
    onFeaturedSellerClick: (UserSearchResult) -> Unit,
    onOpenFeaturedSellersAll: () -> Unit,
    isGuestBrowse: Boolean,
    onRequestLogin: (GuestLoginReason) -> Unit,
    onOpenSizingSetup: (() -> Unit)?,
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

    PullToRefreshBox(
        isRefreshing = ui.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = modifier.fillMaxSize(),
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
                featuredSellers = ui.discovery.recommendedSellers,
                followingIds = ui.followingIds,
                onFeaturedSellerClick = onFeaturedSellerClick,
                onFeaturedSellersSeeAll = onOpenFeaturedSellersAll,
                huntTodayItems = ui.discovery.huntToday,
                shellLoading = ui.isLoading,
                tabsLoading = ui.tabsLoading,
                tabsLoadError = ui.tabsLoadError,
                forYouItems = ui.discovery.forYou,
                followingItems = ui.items,
                followingHasMore = ui.hasMoreItems,
                followingLoadingMore = ui.isLoadingMore,
                onLoadMoreFollowing = { viewModel.loadMoreFollowFeed() },
                onRetryTab = { viewModel.retryTab(ui.selectedFeedTab) },
                stylePickItems = ui.discovery.stylePicks,
                similarSavedItems = ui.discovery.similarToSaved,
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
                followingEmptyContent = {
                    HomePersonalizedFeedEmptyCard(
                        onExploreClick = onNavigateToExplore,
                        onFeaturedSellersClick = onOpenFeaturedSellersAll,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                onRequestLogin = onRequestLogin,
                onScrollToTopRequest = viewModel.scrollHomeToTop,
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

        ui.listingPreview?.let { preview ->
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
