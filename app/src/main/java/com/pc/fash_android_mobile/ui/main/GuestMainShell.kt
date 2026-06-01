package com.pc.fash_android_mobile.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.SnackbarHostState
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.data.locale.AppLocale
import com.pc.fash_android_mobile.ui.common.ReloadWhenVisible
import com.pc.fash_android_mobile.ui.components.FashPromoSlideDef
import com.pc.fash_android_mobile.ui.components.toFashPromoSlideDef
import com.pc.fash_android_mobile.data.home.HomeEditorialPostStub
import com.pc.fash_android_mobile.data.search.toUserSearchResult
import com.pc.fash_android_mobile.data.user.UserSearchResult
import com.pc.fash_android_mobile.ui.address.AddressBookViewModel
import com.pc.fash_android_mobile.ui.chat.ChatViewModel
import com.pc.fash_android_mobile.ui.explore.ExploreViewModel
import com.pc.fash_android_mobile.ui.explore.FeaturedSellersScreen
import com.pc.fash_android_mobile.ui.explore.FeaturedSellersViewModel
import com.pc.fash_android_mobile.ui.guest.GuestLoginReason
import com.pc.fash_android_mobile.ui.guest.GuestLoginSheet
import com.pc.fash_android_mobile.ui.home.HomeEditorialDetailScreen
import com.pc.fash_android_mobile.ui.home.HomeEditorialListScreen
import com.pc.fash_android_mobile.ui.home.UserExperienceSurveyScreen
import com.pc.fash_android_mobile.ui.home.HomeViewModel
import com.pc.fash_android_mobile.ui.listing.ProductDetailScreen
import com.pc.fash_android_mobile.ui.listing.ProductDetailViewModel
import com.pc.fash_android_mobile.ui.main.tabs.ProfileViewModel
import com.pc.fash_android_mobile.ui.main.tabs.SellerProfileScreen
import com.pc.fash_android_mobile.ui.main.tabs.SellerProfileViewModel
import com.pc.fash_android_mobile.ui.notifications.NotificationsViewModel
import com.pc.fash_android_mobile.ui.orders.OrdersViewModel
import com.pc.fash_android_mobile.ui.post.PostViewModel
import com.pc.fash_android_mobile.ui.settings.ChangePasswordViewModel
import com.pc.fash_android_mobile.ui.settings.NotificationPreferencesViewModel

/**
 * Authenticated-light main shell: Home + Explore + listing PDP + editorial reader + seller shop for guests.
 */
@Composable
fun GuestMainShell(
    modifier: Modifier = Modifier,
    fashApp: FashApplication,
    homeViewModel: HomeViewModel,
    exploreViewModel: ExploreViewModel,
    ordersViewModel: OrdersViewModel,
    postViewModel: PostViewModel,
    addressBookViewModel: AddressBookViewModel,
    profileViewModel: ProfileViewModel,
    chatViewModel: ChatViewModel,
    changePasswordViewModel: ChangePasswordViewModel,
    notificationPreferencesViewModel: NotificationPreferencesViewModel,
    notificationsViewModel: NotificationsViewModel,
    productDetailViewModel: ProductDetailViewModel,
    sellerProfileViewModel: SellerProfileViewModel,
    featuredSellersViewModel: FeaturedSellersViewModel,
    promoSlidesViewModel: PromoSlidesViewModel,
    snackbarHostState: SnackbarHostState,
    onExitGuestToLogin: () -> Unit,
) {
    val context = LocalContext.current
    var exploreOverlayOpenNonce by rememberSaveable { mutableLongStateOf(0L) }
    var selectedTab by rememberSaveable { mutableIntStateOf(MainTab.Home.ordinal) }
    var selectedListingId by rememberSaveable { mutableStateOf<String?>(null) }
    var homeEditorialSlug by rememberSaveable { mutableStateOf<String?>(null) }
    var showEditorialListScreen by rememberSaveable { mutableStateOf(false) }
    var uxSurveyKey by rememberSaveable { mutableStateOf<String?>(null) }
    var sellerShopUsername by rememberSaveable { mutableStateOf<String?>(null) }
    var showFeaturedSellersAll by rememberSaveable { mutableStateOf(false) }
    var guestLoginReason by remember { mutableStateOf<GuestLoginReason?>(null) }

    LaunchedEffect(Unit) {
        fashApp.isGuestBrowseActive = true
        homeViewModel.onGuestBrowseEntered()
        exploreViewModel.refresh()
        promoSlidesViewModel.refresh()
    }
    val localeRev by AppLocale.localeRevisionFlow.collectAsState()
    LaunchedEffect(localeRev) {
        promoSlidesViewModel.refresh()
    }
    val remotePromo by promoSlidesViewModel.remoteSlides.collectAsState()
    val scheme = MaterialTheme.colorScheme
    val mappedPromoSlides = remember(remotePromo, scheme) {
        remotePromo.map { it.toFashPromoSlideDef(scheme) }
    }
    val requestLogin: (GuestLoginReason) -> Unit = { guestLoginReason = it }
    val handlePromoClick: (FashPromoSlideDef, Int) -> Unit = { slide, _ ->
        val nav = slide.navigation
        val t = nav?.type?.trim()?.lowercase().orEmpty()
        when (t) {
            "", "none" -> Unit
            "in_app_explore" -> exploreOverlayOpenNonce++
            "in_app_orders" -> requestLogin(GuestLoginReason.Orders)
            "in_app_chat" -> requestLogin(GuestLoginReason.ChatFromHome)
            "in_app_product_packages" -> requestLogin(GuestLoginReason.Post)
            "in_app_invite_friends" -> requestLogin(GuestLoginReason.Profile)
            "in_app_editorial_guides" -> {
                val slug = nav?.payload?.trim().orEmpty()
                if (slug.isNotEmpty()) {
                    showEditorialListScreen = false
                    homeEditorialSlug = slug
                } else {
                    homeEditorialSlug = null
                    showEditorialListScreen = true
                }
            }
            "in_app_ux_survey" -> {
                val key = nav?.payload?.trim().orEmpty().ifBlank { "fash_ux_v1" }
                uxSurveyKey = key
            }
            "external_url" -> {
                val url = nav?.payload?.trim().orEmpty()
                if (url.isNotEmpty()) {
                    runCatching {
                        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
                    }
                }
            }
            "deeplink" -> {
                val u = nav?.payload?.trim().orEmpty()
                if (u.isNotEmpty()) {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
                    }
                }
            }
            else -> Unit
        }
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
    val pendingSellerDeepLink by fashApp.pendingDeepLinkSellerUsername.collectAsState()
    LaunchedEffect(pendingSellerDeepLink) {
        val handle = pendingSellerDeepLink ?: return@LaunchedEffect
        sellerShopUsername = handle
        fashApp.pendingDeepLinkSellerUsername.value = null
    }
    val pendingInviteFriends by fashApp.pendingOpenInviteFriends.collectAsState()
    LaunchedEffect(pendingInviteFriends) {
        if (!pendingInviteFriends) return@LaunchedEffect
        requestLogin(GuestLoginReason.Invite)
    }

    LaunchedEffect(sellerShopUsername) {
        sellerShopUsername?.let { sellerProfileViewModel.loadForSeller(it) }
    }

    val openSellerShop: (UserSearchResult) -> Unit = { seller ->
        val u = seller.username.trim()
        if (u.isNotEmpty()) {
            homeEditorialSlug = null
            showFeaturedSellersAll = false
            sellerShopUsername = u
        }
    }

    ReloadWhenVisible(showFeaturedSellersAll, sellerShopUsername, selectedListingId) {
        if (showFeaturedSellersAll && sellerShopUsername == null && selectedListingId == null) {
            featuredSellersViewModel.refresh()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        MainNavScreen(
            modifier = Modifier.fillMaxSize(),
            onLogout = onExitGuestToLogin,
            onLogoutAll = onExitGuestToLogin,
            isLoggingOut = false,
            homeViewModel = homeViewModel,
            exploreViewModel = exploreViewModel,
            ordersViewModel = ordersViewModel,
            postViewModel = postViewModel,
            addressBookViewModel = addressBookViewModel,
            profileViewModel = profileViewModel,
            chatViewModel = chatViewModel,
            changePasswordViewModel = changePasswordViewModel,
            notificationPreferencesViewModel = notificationPreferencesViewModel,
            notificationsViewModel = notificationsViewModel,
            snackbarHostState = snackbarHostState,
            chatUnreadCount = 0,
            onListingClick = { lid, _ ->
                homeEditorialSlug = null
                showFeaturedSellersAll = false
                sellerShopUsername = null
                selectedListingId = lid
            },
            onFeaturedSellerClick = openSellerShop,
            onOpenFeaturedSellersAll = { showFeaturedSellersAll = true },
            onHomeEditorialPostClick = { post: HomeEditorialPostStub ->
                val slug = post.slug.trim().ifBlank { post.id.trim() }
                if (slug.isNotEmpty()) {
                    sellerShopUsername = null
                    showFeaturedSellersAll = false
                    homeEditorialSlug = slug
                }
            },
            selectedTab = selectedTab,
            onTabChange = { selectedTab = it },
            exploreOverlayOpenNonce = exploreOverlayOpenNonce,
            isGuestMode = true,
            onRequestLogin = requestLogin,
            featureTourActive = false,
            promoSlides = mappedPromoSlides,
            onPromoSlideClick = handlePromoClick,
        )

        val editorialSlug = homeEditorialSlug
        if (uxSurveyKey != null && selectedListingId == null) {
            UserExperienceSurveyScreen(
                surveyKey = uxSurveyKey!!,
                modifier = Modifier.fillMaxSize(),
                onBack = { uxSurveyKey = null },
            )
        } else if (showEditorialListScreen && editorialSlug == null && selectedListingId == null) {
            HomeEditorialListScreen(
                modifier = Modifier.fillMaxSize(),
                onBack = { showEditorialListScreen = false },
                onPostClick = { post ->
                    val slug = post.slug.trim().ifBlank { post.id.trim() }
                    if (slug.isNotEmpty()) {
                        showEditorialListScreen = false
                        homeEditorialSlug = slug
                    }
                },
            )
        } else if (editorialSlug != null && selectedListingId == null) {
            HomeEditorialDetailScreen(
                modifier = Modifier.fillMaxSize(),
                slug = editorialSlug,
                onBack = { homeEditorialSlug = null },
            )
        }

        if (showFeaturedSellersAll && sellerShopUsername == null && selectedListingId == null && editorialSlug == null && !showEditorialListScreen && uxSurveyKey == null) {
            FeaturedSellersScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = featuredSellersViewModel,
                onBack = { showFeaturedSellersAll = false },
                onSellerClick = { seller -> openSellerShop(seller.toUserSearchResult()) },
                onListingClick = { lid, _ ->
                    showFeaturedSellersAll = false
                    selectedListingId = lid
                },
            )
        }

        val shopUsername = sellerShopUsername
        if (shopUsername != null && selectedListingId == null && editorialSlug == null) {
            SellerProfileScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                viewModel = sellerProfileViewModel,
                sellerUsername = shopUsername,
                listingPreviewViewModel = homeViewModel,
                onBack = { sellerShopUsername = null },
                onListingClick = { lid, _ -> selectedListingId = lid },
                onNavigateToExploreFromProfile = { cat, brand, aes, q, countryId, countryIso2 ->
                    exploreViewModel.openExploreFromProfileFilter(
                        categoryId = cat,
                        brandId = brand,
                        aestheticTagId = aes,
                        searchQuery = q,
                        countryId = countryId,
                        countryIso2 = countryIso2,
                    )
                    sellerShopUsername = null
                    exploreOverlayOpenNonce++
                },
                isGuestMode = true,
                onRequestLogin = requestLogin,
            )
        }

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
                    val u = username.trim()
                    if (u.isNotEmpty()) {
                        selectedListingId = null
                        sellerShopUsername = u
                    }
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
                    sellerShopUsername = null
                    exploreOverlayOpenNonce++
                },
                isGuestMode = true,
                onRequestLogin = requestLogin,
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

        val hasGuestOverlayBack = remember(
            selectedListingId,
            sellerShopUsername,
            homeEditorialSlug,
            showEditorialListScreen,
            uxSurveyKey,
            showFeaturedSellersAll,
        ) {
            selectedListingId != null ||
                sellerShopUsername != null ||
                homeEditorialSlug != null ||
                showEditorialListScreen ||
                uxSurveyKey != null ||
                showFeaturedSellersAll
        }
        BackHandler(enabled = hasGuestOverlayBack) {
            when {
                uxSurveyKey != null -> uxSurveyKey = null
                homeEditorialSlug != null -> homeEditorialSlug = null
                showEditorialListScreen -> showEditorialListScreen = false
                showFeaturedSellersAll && sellerShopUsername == null -> showFeaturedSellersAll = false
                sellerShopUsername != null -> sellerShopUsername = null
                selectedListingId != null -> {
                    productDetailViewModel.clearCachesForSignedOutUser()
                    selectedListingId = null
                }
            }
        }
    }
}
