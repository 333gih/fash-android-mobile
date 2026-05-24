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
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
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
 * Home tab: featured sellers at scroll top, sticky feed tabs, masonry grid, pinned promo above bottom nav.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedContent(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    onListingClick: (listingId: String, sellerId: String?) -> Unit = { _, _ -> },
    onNavigateToExplore: () -> Unit = {},
    onNavigateToExploreWithTag: (tagName: String) -> Unit = { onNavigateToExplore() },
    onOrdersClick: () -> Unit = {},
    onDeliveringJourneyClick: () -> Unit = onOrdersClick,
    onNavigateToChat: () -> Unit = {},
    onNavigateToSaved: () -> Unit = {},
    onNavigateToPost: () -> Unit = {},
    onPromoSlideClick: (FashPromoSlideDef, Int) -> Unit = { _, _ -> onNavigateToExplore() },
    promoSlides: List<FashPromoSlideDef> = emptyList(),
    onHomeEditorialPostClick: (HomeEditorialPostStub) -> Unit = {},
    onFeaturedSellerClick: (UserSearchResult) -> Unit = {},
    onOpenFeaturedSellersAll: () -> Unit = {},
    isGuestBrowse: Boolean = false,
    onRequestLogin: (GuestLoginReason) -> Unit = {},
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
    val discoveryLoading by viewModel.discoveryLoading.collectAsState()
    val discovery by viewModel.discoveryBundle.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val hasMoreItems by viewModel.hasMoreItems.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val listingPreview by viewModel.listingPreview.collectAsState()
    val selectedFeedTab by viewModel.selectedFeedTab.collectAsState()
    val followingIds by viewModel.followingIds.collectAsState()
    val pullState = rememberPullToRefreshState()
    val promoDockInset: Dp = if (promoSlides.isNotEmpty()) FashStickyPromoDockHeight else 0.dp

    LaunchedEffect(isGuestBrowse) {
        viewModel.normalizeSelectedFeedTab(isGuestBrowse)
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
        Box(modifier = Modifier.fillMaxSize()) {
            HomeFeedTabHost(
                modifier = Modifier.fillMaxSize(),
                bottomScrollInset = promoDockInset,
                selectedTab = selectedFeedTab,
                onTabSelected = viewModel::setSelectedFeedTab,
                featuredSellers = discovery.recommendedSellers,
                followingIds = followingIds,
                onFeaturedSellerClick = onFeaturedSellerClick,
                onFeaturedSellersSeeAll = onOpenFeaturedSellersAll,
                huntTodayItems = huntTodayItems,
                huntTodayLoading = huntTodayLoading,
                discoveryLoading = discoveryLoading,
                forYouItems = discovery.forYou,
                followingItems = items,
                followingLoading = isLoading,
                followingLoadError = loadError,
                followingHasMore = hasMoreItems,
                onLoadMoreFollowing = { viewModel.loadMoreFollowFeed() },
                onRetryFollowing = { viewModel.retryLoad() },
                stylePickItems = discovery.stylePicks,
                similarSavedItems = discovery.similarToSaved,
                isGuestBrowse = isGuestBrowse,
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
