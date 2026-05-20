package com.pc.fash_android_mobile.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.SnackbarHostState
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.data.home.HomeEditorialPostStub
import com.pc.fash_android_mobile.ui.address.AddressBookViewModel
import com.pc.fash_android_mobile.ui.chat.ChatViewModel
import com.pc.fash_android_mobile.ui.explore.ExploreViewModel
import com.pc.fash_android_mobile.ui.guest.GuestLoginReason
import com.pc.fash_android_mobile.ui.guest.GuestLoginSheet
import com.pc.fash_android_mobile.ui.home.HomeViewModel
import com.pc.fash_android_mobile.ui.listing.ProductDetailScreen
import com.pc.fash_android_mobile.ui.listing.ProductDetailViewModel
import com.pc.fash_android_mobile.ui.main.tabs.ProfileViewModel
import com.pc.fash_android_mobile.ui.notifications.NotificationsViewModel
import com.pc.fash_android_mobile.ui.post.PostViewModel
import com.pc.fash_android_mobile.ui.settings.ChangePasswordViewModel

/**
 * Authenticated-light main shell: Home + Explore + listing PDP for users without a session.
 */
@Composable
fun GuestMainShell(
    modifier: Modifier = Modifier,
    fashApp: FashApplication,
    homeViewModel: HomeViewModel,
    exploreViewModel: ExploreViewModel,
    postViewModel: PostViewModel,
    addressBookViewModel: AddressBookViewModel,
    profileViewModel: ProfileViewModel,
    chatViewModel: ChatViewModel,
    changePasswordViewModel: ChangePasswordViewModel,
    notificationsViewModel: NotificationsViewModel,
    productDetailViewModel: ProductDetailViewModel,
    snackbarHostState: SnackbarHostState,
    onExitGuestToLogin: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(MainTab.Home.ordinal) }
    var selectedListingId by rememberSaveable { mutableStateOf<String?>(null) }
    var guestLoginReason by remember { mutableStateOf<GuestLoginReason?>(null) }

    LaunchedEffect(Unit) {
        fashApp.isGuestBrowseActive = true
        homeViewModel.loadFeed()
        exploreViewModel.refresh()
    }
    DisposableEffect(Unit) {
        onDispose { fashApp.isGuestBrowseActive = false }
    }

    val pendingDeepLink by fashApp.pendingDeepLinkListingId.collectAsState()
    LaunchedEffect(pendingDeepLink) {
        val id = pendingDeepLink ?: return@LaunchedEffect
        selectedListingId = id
        fashApp.pendingDeepLinkListingId.value = null
    }

    val requestLogin: (GuestLoginReason) -> Unit = { guestLoginReason = it }

    Box(modifier = modifier.fillMaxSize()) {
        MainNavScreen(
            modifier = Modifier.fillMaxSize(),
            onLogout = onExitGuestToLogin,
            onLogoutAll = onExitGuestToLogin,
            isLoggingOut = false,
            homeViewModel = homeViewModel,
            exploreViewModel = exploreViewModel,
            postViewModel = postViewModel,
            addressBookViewModel = addressBookViewModel,
            profileViewModel = profileViewModel,
            chatViewModel = chatViewModel,
            changePasswordViewModel = changePasswordViewModel,
            notificationsViewModel = notificationsViewModel,
            snackbarHostState = snackbarHostState,
            chatUnreadCount = 0,
            onListingClick = { lid, _ -> selectedListingId = lid },
            onFeaturedSellerClick = { seller ->
                seller.username.takeIf { it.isNotBlank() }?.let { username ->
                    exploreViewModel.openExploreFromProfileFilter(
                        categoryId = null,
                        brandId = null,
                        aestheticTagId = null,
                        searchQuery = username,
                        countryId = null,
                        countryIso2 = null,
                    )
                    selectedTab = MainTab.Explore.ordinal
                }
            },
            onHomeEditorialPostClick = { _: HomeEditorialPostStub ->
                selectedTab = MainTab.Explore.ordinal
            },
            selectedTab = selectedTab,
            onTabChange = { selectedTab = it },
            isGuestMode = true,
            onRequestLogin = requestLogin,
            featureTourActive = false,
        )

        val listingId = selectedListingId
        if (listingId != null) {
            ProductDetailScreen(
                modifier = Modifier.fillMaxSize(),
                listingId = listingId,
                viewModel = productDetailViewModel,
                onBack = {
                    productDetailViewModel.clearCachesForSignedOutUser()
                    selectedListingId = null
                },
                onChat = { requestLogin(GuestLoginReason.BuyOrChat) },
                onBuyNow = { requestLogin(GuestLoginReason.BuyOrChat) },
                onListingClick = { lid, _ -> selectedListingId = lid },
                onVisitSellerShop = { username ->
                    exploreViewModel.openExploreFromProfileFilter(
                        categoryId = null,
                        brandId = null,
                        aestheticTagId = null,
                        searchQuery = username,
                        countryId = null,
                        countryIso2 = null,
                    )
                    selectedListingId = null
                    selectedTab = MainTab.Explore.ordinal
                },
                profileExploreNavigationEnabled = true,
                onNavigateToExploreFromProfile = { categoryId, brandId, aestheticTagId, searchQuery, countryId, countryIso2 ->
                    exploreViewModel.openExploreFromProfileFilter(
                        categoryId = categoryId,
                        brandId = brandId,
                        aestheticTagId = aestheticTagId,
                        searchQuery = searchQuery,
                        countryId = countryId,
                        countryIso2 = countryIso2,
                    )
                    selectedListingId = null
                    selectedTab = MainTab.Explore.ordinal
                },
            )
        }

        val reason = guestLoginReason
        if (reason != null) {
            GuestLoginSheet(
                reason = reason,
                onDismiss = { guestLoginReason = null },
                onSignIn = {
                    guestLoginReason = null
                    fashApp.isGuestBrowseActive = false
                    onExitGuestToLogin()
                },
            )
        }
    }
}
