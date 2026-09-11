package com.pc.fash_android_mobile.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pc.fash_android_mobile.ui.components.FashPromoSlideDef
import com.pc.fash_android_mobile.ui.guest.GuestLoginReason

/**
 * Full-screen Explore opened from Home search (or deep links). No bottom navigation —
 * [onClose] returns to Home.
 */
@Composable
fun ExploreOverlayHost(
    viewModel: ExploreViewModel,
    onClose: () -> Unit,
    onListingClick: (listingId: String, sellerId: String?) -> Unit,
    onFeaturedSellerClick: (com.pc.fash_android_mobile.data.user.UserSearchResult) -> Unit,
    onSeeAllFeaturedSellersClick: () -> Unit,
    onPromoSlideClick: (FashPromoSlideDef, Int) -> Unit,
    promoSlides: List<FashPromoSlideDef>,
    isGuestMode: Boolean,
    onRequestLogin: (GuestLoginReason) -> Unit,
    onOpenSizingSetup: (() -> Unit)?,
    onOpenShippingAddresses: (() -> Unit)?,
    onStartChatFromListing: (listingId: String) -> Unit = {},
    existingChatListingIds: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ExploreTopBar(
                viewModel = viewModel,
                onCloseOverlay = onClose,
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            ExploreScreen(
                viewModel = viewModel,
                onListingClick = onListingClick,
                onFeaturedSellerClick = onFeaturedSellerClick,
                onSeeAllFeaturedSellersClick = onSeeAllFeaturedSellersClick,
                onPromoSlideClick = onPromoSlideClick,
                promoSlides = promoSlides,
                isGuestMode = isGuestMode,
                onRequestLogin = onRequestLogin,
                onOpenSizingSetup = onOpenSizingSetup,
                onOpenShippingAddresses = onOpenShippingAddresses,
                onStartChatFromListing = onStartChatFromListing,
                existingChatListingIds = existingChatListingIds,
            )
        }
    }
}
