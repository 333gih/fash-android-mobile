package com.pc.fash_android_mobile.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.text.font.FontWeight
import com.pc.fash_android_mobile.ui.explore.ExploreScreen
import com.pc.fash_android_mobile.ui.explore.ExploreTopBar
import com.pc.fash_android_mobile.ui.home.HomeFeedContent
import com.pc.fash_android_mobile.ui.address.AddressBookViewModel
import com.pc.fash_android_mobile.ui.post.CreateListingFlowScreen
import com.pc.fash_android_mobile.data.chat.ConversationItem
import com.pc.fash_android_mobile.data.home.HomeEditorialPostStub
import com.pc.fash_android_mobile.ui.main.tabs.ChatScreen
import com.pc.fash_android_mobile.ui.main.tabs.NotificationScreen
import com.pc.fash_android_mobile.ui.main.tabs.ProfileScreen
import com.pc.fash_android_mobile.ui.notifications.NotificationsViewModel
import com.pc.fash_android_mobile.ui.main.tabs.SettingsScreen
import com.pc.fash_android_mobile.ui.settings.ChangePasswordScreen
import com.pc.fash_android_mobile.ui.settings.ChangePasswordViewModel
import com.pc.fash_android_mobile.ui.components.FashBrandMarkText
import com.pc.fash_android_mobile.ui.components.FashInboxNotificationIconButton
import com.pc.fash_android_mobile.ui.components.FashPromoSlideDef
import com.pc.fash_android_mobile.ui.theme.FashBrandTypography
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import com.pc.fash_android_mobile.data.onboarding.AppFeatureTourStore
import com.pc.fash_android_mobile.data.user.UserSearchResult
import com.pc.fash_android_mobile.ui.guest.GuestLoginReason
import com.pc.fash_android_mobile.ui.guest.GuestTopBarSignInAction
import com.pc.fash_android_mobile.ui.guest.GuestTabPlaceholder
import com.pc.fash_android_mobile.ui.onboarding.AppFeatureTourOverlay
import com.pc.fash_android_mobile.ui.onboarding.AppTourStep
import com.pc.fash_android_mobile.ui.onboarding.FeatureTourAnchor

/** `FASH.` + screen suffix — same typography as Explore (medium mark + titleLarge). */
@Composable
private fun FashScreenTitle(
    @StringRes suffixRes: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FashBrandMarkText(
            text = stringResource(R.string.brand_wordmark),
            style = FashBrandTypography.markBoldItalicMedium,
        )
        Text(
            text = stringResource(suffixRes),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

enum class MainTab(
    val labelRes: Int,
    val icon: ImageVector,
    @StringRes val headerSuffixRes: Int,
) {
    Home(R.string.nav_home, Icons.Default.Home, R.string.brand_header_suffix_home),
    Explore(R.string.nav_explore, Icons.Default.Explore, R.string.brand_header_suffix_explore),
    Post(R.string.nav_post, Icons.Default.Add, R.string.brand_header_suffix_post),
    Chat(R.string.nav_chat, Icons.Default.ChatBubbleOutline, R.string.brand_header_suffix_chat),
    Profile(R.string.nav_profile, Icons.Default.Person, R.string.brand_header_suffix_profile),
}

private val guestLockedTabs = setOf(MainTab.Post, MainTab.Chat, MainTab.Profile)

private fun MainTab.guestLoginReason(): GuestLoginReason? = when (this) {
    MainTab.Profile -> GuestLoginReason.Profile
    MainTab.Chat -> GuestLoginReason.Chat
    MainTab.Post -> GuestLoginReason.Post
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    onLogoutAll: () -> Unit,
    isLoggingOut: Boolean,
    homeViewModel: com.pc.fash_android_mobile.ui.home.HomeViewModel,
    exploreViewModel: com.pc.fash_android_mobile.ui.explore.ExploreViewModel,
    postViewModel: com.pc.fash_android_mobile.ui.post.PostViewModel,
    addressBookViewModel: AddressBookViewModel,
    profileViewModel: com.pc.fash_android_mobile.ui.main.tabs.ProfileViewModel,
    chatViewModel: com.pc.fash_android_mobile.ui.chat.ChatViewModel,
    changePasswordViewModel: ChangePasswordViewModel,
    snackbarHostState: SnackbarHostState,
    /** Total unread messages for chat tab badge ([ChatRepository.getUnreadCount]). */
    chatUnreadCount: Int = 0,
    /** [sellerId] when known — used to open seller edit vs public detail. */
    onListingClick: (listingId: String, sellerId: String?) -> Unit = { _, _ -> },
    onEditProfile: () -> Unit = {},
    onShippingAddressesClick: () -> Unit = {},
    onInviteFriendsClick: () -> Unit = {},
    onOrdersClick: () -> Unit = {},
    /** Home journey “Đang giao” — dedicated hub (env-gated). Falls back to [onOrdersClick] when null. */
    onHomeDeliveringJourneyClick: (() -> Unit)? = null,
    /** [initialTab] 0 = people you follow, 1 = followers. */
    onOpenFollowConnections: (initialTab: Int) -> Unit = {},
    /** Explore featured sellers “See all” — full list from `GET /search/featured-sellers`. */
    onOpenFeaturedSellersAll: () -> Unit = {},
    /** Featured seller chip on Explore — opens seller shop (`GET …/users/{username}`). */
    onFeaturedSellerClick: (UserSearchResult) -> Unit = {},
    onConversationClick: (ConversationItem) -> Unit = {},
    notificationsViewModel: NotificationsViewModel,
    /** Ledger id from FCM tray / `fash://inbox/{id}` — opens inbox overlay and detail when non-null. */
    pendingInboxNotificationIdToOpen: String? = null,
    onConsumePendingInboxNotificationId: () -> Unit = {},
    /** Incremented (e.g. snackbar action) to open the inbox list without a specific row. */
    inboxOpenRequestGeneration: Long = 0L,
    onOpenOrderFromNotification: (String) -> Unit = {},
    onOpenListingFromNotification: (String, String?) -> Unit = { _, _ -> },
    /** Opens Chat tab with a conversation selected (FCM / inbox `conversation_id`). */
    onNavigateToChatConversation: (String) -> Unit = {},
    /** Home cẩm nang card — open in-app reader (host may route Explore CTA from detail). */
    onHomeEditorialPostClick: (HomeEditorialPostStub) -> Unit = {},
    /** Profile / seller shop: open Explore → Posts with filters + search + optional country. */
    onNavigateToExploreFromProfile: (
        categoryId: String?,
        brandId: String?,
        aestheticTagId: String?,
        searchQuery: String,
        countryId: String?,
        countryIso2: String?,
    ) -> Unit = { _, _, _, _, _, _ -> },
    /** When null, screens use built-in promo copy; otherwise from core-service CMS. */
    promoSlides: List<FashPromoSlideDef> = emptyList(),
    onPromoSlideClick: (FashPromoSlideDef, Int) -> Unit = { _, _ -> },
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    /**
     * When returning true, the default reselect reload (scroll-to-top + refresh) is skipped.
     * Use when a fullscreen overlay (e.g. seller shop) is open on top of the selected tab.
     */
    onMainTabReselectedIntercept: ((MainTab) -> Boolean)? = null,
    /** First-launch spotlight tour; completion is stored in [AppFeatureTourStore]. */
    featureTourActive: Boolean = false,
    onFeatureTourFinished: () -> Unit = {},
    /** Browse-only shell: Home + Explore + PDP; locked tabs/actions prompt [onRequestLogin]. */
    isGuestMode: Boolean = false,
    onRequestLogin: (GuestLoginReason) -> Unit = {},
) {
    var showNotificationScreen by rememberSaveable { mutableStateOf(false) }
    /** Tracks overlay visibility to refresh server unread count when user leaves the inbox. */
    var wasNotificationOverlayVisible by remember { mutableStateOf(false) }
    var showSettingsScreen by rememberSaveable { mutableStateOf(false) }
    var showChangePasswordScreen by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    var tourStep by remember { mutableStateOf(AppTourStep.Intro) }
    val tourAnchors = remember { mutableStateMapOf<FeatureTourAnchor, LayoutCoordinates>() }
    val cpCurrent by changePasswordViewModel.currentPassword.collectAsState()
    val cpNew by changePasswordViewModel.newPassword.collectAsState()
    val cpConfirm by changePasswordViewModel.confirmPassword.collectAsState()
    val cpSubmitting by changePasswordViewModel.isSubmitting.collectAsState()
    val canSubmitPw = remember(cpCurrent, cpNew, cpConfirm) {
        cpCurrent.isNotBlank() && cpNew.length in 8..72 && cpNew == cpConfirm
    }
    LaunchedEffect(Unit) {
        changePasswordViewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
            if (msg == context.getString(R.string.password_change_success)) {
                showChangePasswordScreen = false
            }
        }
    }
    val tabs = MainTab.entries

    val homeRefreshing by homeViewModel.isRefreshing.collectAsState()
    val exploreRefreshing by exploreViewModel.isRefreshing.collectAsState()
    val chatRefreshing by chatViewModel.isRefreshing.collectAsState()
    val profileRefreshing by profileViewModel.isRefreshing.collectAsState()
    val postNavReloading by postViewModel.navReselectLoading.collectAsState()

    val defaultMainTabReselected: (MainTab) -> Unit = { tab ->
        when (tab) {
            MainTab.Home -> {
                homeViewModel.requestScrollHomeToTop()
                homeViewModel.refresh()
            }
            MainTab.Explore -> {
                exploreViewModel.requestScrollExploreToTop()
                exploreViewModel.refresh()
            }
            MainTab.Post -> postViewModel.reloadOnNavReselect()
            MainTab.Chat -> chatViewModel.refresh()
            MainTab.Profile -> {
                profileViewModel.requestScrollProfileToTop()
                profileViewModel.refresh()
            }
        }
    }
    val onMainTabReselected: (MainTab) -> Unit = { tab ->
        if (onMainTabReselectedIntercept?.invoke(tab) != true) {
            defaultMainTabReselected(tab)
        }
    }

    val isMainTabNavLoading: (MainTab) -> Boolean = { tab ->
        if (tabs.getOrNull(selectedTab) != tab) {
            false
        } else {
            when (tab) {
                MainTab.Home -> homeRefreshing
                MainTab.Explore -> exploreRefreshing
                MainTab.Post -> postNavReloading
                MainTab.Chat -> chatRefreshing
                MainTab.Profile -> profileRefreshing
            }
        }
    }

    LaunchedEffect(inboxOpenRequestGeneration) {
        if (inboxOpenRequestGeneration <= 0L || isGuestMode) return@LaunchedEffect
        showNotificationScreen = true
    }

    LaunchedEffect(pendingInboxNotificationIdToOpen) {
        if (isGuestMode) return@LaunchedEffect
        val id = pendingInboxNotificationIdToOpen?.trim()?.takeIf { it.isNotEmpty() } ?: return@LaunchedEffect
        showNotificationScreen = true
        notificationsViewModel.openInboxDetailFromPush(id)
        onConsumePendingInboxNotificationId()
    }

    if (!isGuestMode) {
        LaunchedEffect(Unit) {
            chatViewModel.refreshUnreadCount()
        }
        LaunchedEffect(selectedTab) {
            if (tabs.getOrNull(selectedTab) == MainTab.Home) {
                chatViewModel.refreshUnreadCount()
            }
        }
    }
    LaunchedEffect(showNotificationScreen) {
        when {
            showNotificationScreen -> {
                notificationsViewModel.refresh()
                notificationsViewModel.refreshUnreadSummary()
            }
            wasNotificationOverlayVisible -> notificationsViewModel.refreshUnreadSummary()
        }
        wasNotificationOverlayVisible = showNotificationScreen
    }
    val inboxUnreadTotal by notificationsViewModel.unreadCount.collectAsState()
    val notificationDetailId by notificationsViewModel.selectedDetailId.collectAsState()
    val exploreSearchExpanded by exploreViewModel.searchBarExpanded.collectAsState()
    val openExploreSearch: () -> Unit = {
        exploreViewModel.requestSearchBarExpanded()
        onTabChange(MainTab.Explore.ordinal)
    }
    val openNotifications: () -> Unit = {
        if (isGuestMode) {
            onRequestLogin(GuestLoginReason.Notifications)
        } else {
            showNotificationScreen = true
        }
    }
    val openOrders: () -> Unit = {
        if (isGuestMode) {
            onRequestLogin(GuestLoginReason.Orders)
        } else {
            onOrdersClick()
        }
    }
    val openGuestSignIn: () -> Unit = { onRequestLogin(GuestLoginReason.TopBar) }
    val isPostListingFlow = tabs.getOrNull(selectedTab) == MainTab.Post
    val featureTourVisible = featureTourActive && !isGuestMode &&
        !showNotificationScreen &&
        !showSettingsScreen &&
        !showChangePasswordScreen

    LaunchedEffect(featureTourActive) {
        if (!featureTourActive) {
            tourAnchors.clear()
        } else {
            tourStep = AppTourStep.Intro
        }
    }
    LaunchedEffect(featureTourVisible, tourStep) {
        if (!featureTourVisible) return@LaunchedEffect
        tourStep.prepareTab()?.let { tab -> onTabChange(tab.ordinal) }
    }
    LaunchedEffect(selectedTab, featureTourActive) {
        if (!featureTourActive) return@LaunchedEffect
        val tab = tabs.getOrNull(selectedTab)
        if (tab != MainTab.Home && tab != MainTab.Chat && tab != MainTab.Post) {
            tourAnchors.remove(FeatureTourAnchor.TopActionsRow)
        }
    }

    val featureTourActiveState = rememberUpdatedState(featureTourActive)
    val onTourTopActionsPositioned: (LayoutCoordinates?) -> Unit = remember {
        { coords ->
            if (featureTourActiveState.value) {
                val c = coords?.takeIf { it.isAttached }
                if (c == null) {
                    tourAnchors.remove(FeatureTourAnchor.TopActionsRow)
                } else {
                    tourAnchors[FeatureTourAnchor.TopActionsRow] = c
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (!isPostListingFlow) {
                when (val tab = tabs.getOrNull(selectedTab)) {
                MainTab.Explore -> ExploreTopBar(
                    viewModel = exploreViewModel,
                    inboxUnreadCount = if (isGuestMode) 0 else inboxUnreadTotal,
                    onOrdersClick = openOrders,
                    onNotificationsClick = openNotifications,
                    showGuestSignIn = isGuestMode,
                    onGuestSignInClick = openGuestSignIn,
                )
                MainTab.Profile -> ProfileTopBar(
                    inboxUnreadCount = if (isGuestMode) 0 else inboxUnreadTotal,
                    onSearchClick = openExploreSearch,
                    onNotificationsClick = openNotifications,
                    onOrdersClick = openOrders,
                    showGuestSignIn = isGuestMode,
                    onGuestSignInClick = openGuestSignIn,
                    onLogout = onLogout,
                    onOpenSettings = {
                        showNotificationScreen = false
                        showSettingsScreen = true
                    },
                    isLoggingOut = isLoggingOut,
                )
                MainTab.Home -> MainTopBar(
                    suffixRes = tab.headerSuffixRes,
                    inboxUnreadCount = if (isGuestMode) 0 else inboxUnreadTotal,
                    onSearchClick = openExploreSearch,
                    onNotificationsClick = openNotifications,
                    onOrdersClick = openOrders,
                    showGuestSignIn = isGuestMode,
                    onGuestSignInClick = openGuestSignIn,
                    tourTopBarAnchorsEnabled = featureTourActive,
                    onTourTopActionsPositioned = onTourTopActionsPositioned,
                )
                MainTab.Post -> MainTopBar(
                    suffixRes = tab.headerSuffixRes,
                    inboxUnreadCount = if (isGuestMode) 0 else inboxUnreadTotal,
                    onSearchClick = openExploreSearch,
                    onNotificationsClick = openNotifications,
                    showGuestSignIn = isGuestMode,
                    onGuestSignInClick = openGuestSignIn,
                    tourTopBarAnchorsEnabled = featureTourActive,
                    onTourTopActionsPositioned = onTourTopActionsPositioned,
                )
                MainTab.Chat -> MainTopBar(
                    suffixRes = tab.headerSuffixRes,
                    inboxUnreadCount = if (isGuestMode) 0 else inboxUnreadTotal,
                    onSearchClick = openExploreSearch,
                    onNotificationsClick = openNotifications,
                    onOrdersClick = openOrders,
                    showGuestSignIn = isGuestMode,
                    onGuestSignInClick = openGuestSignIn,
                    tourTopBarAnchorsEnabled = featureTourActive,
                    onTourTopActionsPositioned = onTourTopActionsPositioned,
                )
                else -> MainTopBar(
                    suffixRes = MainTab.Home.headerSuffixRes,
                    inboxUnreadCount = if (isGuestMode) 0 else inboxUnreadTotal,
                    onSearchClick = openExploreSearch,
                    onNotificationsClick = openNotifications,
                    onOrdersClick = openOrders,
                    showGuestSignIn = isGuestMode,
                    onGuestSignInClick = openGuestSignIn,
                    tourTopBarAnchorsEnabled = featureTourActive,
                    onTourTopActionsPositioned = onTourTopActionsPositioned,
                )
                }
            }
        },
        bottomBar = {
            if (!isPostListingFlow) {
                MainNavBottomBar(
                    selectedTab = selectedTab,
                    chatUnreadCount = chatUnreadCount,
                    onTabChange = { index ->
                        showNotificationScreen = false
                        val tab = tabs.getOrNull(index)
                        if (isGuestMode && tab != null && tab in guestLockedTabs) {
                            tab.guestLoginReason()?.let(onRequestLogin)
                        } else {
                            onTabChange(index)
                        }
                    },
                    onTabReselected = onMainTabReselected,
                    isTabNavLoading = isMainTabNavLoading,
                    tourAnchorsEnabled = featureTourActive,
                    onTourAnchorPositioned = { key, coords ->
                        if (featureTourActive) {
                            val c = coords?.takeIf { it.isAttached }
                            if (c == null) {
                                tourAnchors.remove(key)
                            } else {
                                tourAnchors[key] = c
                            }
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            LaunchedEffect(selectedTab) {
                val tab = tabs.getOrNull(selectedTab)
                if (tab == MainTab.Explore) {
                    exploreViewModel.onExploreTabSelected()
                } else {
                    exploreViewModel.setSearchBarExpanded(false)
                }
                when (tab) {
                    MainTab.Home -> homeViewModel.refresh()
                    MainTab.Explore -> Unit
                    MainTab.Post -> postViewModel.reloadOnNavReselect()
                    MainTab.Chat -> {
                        chatViewModel.loadConversations()
                        chatViewModel.refreshUnreadCount()
                    }
                    MainTab.Profile -> profileViewModel.refresh()
                    null -> Unit
                }
            }
            AnimatedContent(
                modifier = Modifier.fillMaxSize(),
                targetState = selectedTab,
                transitionSpec = {
                    val postOrd = MainTab.Post.ordinal
                    when {
                        targetState == postOrd && initialState != postOrd -> (
                            fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                )
                            ) togetherWith fadeOut(animationSpec = tween(160))
                        initialState == postOrd && targetState != postOrd ->
                            fadeIn(animationSpec = tween(220)) togetherWith (
                                fadeOut(animationSpec = tween(180)) +
                                    scaleOut(
                                        targetScale = 0.96f,
                                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                                    )
                                )
                        else -> fadeIn(tween(200)) togetherWith fadeOut(tween(180))
                    }
                },
                label = "mainTabContent",
            ) { tabIndex ->
                when (tabs.getOrNull(tabIndex) ?: MainTab.Home) {
                    MainTab.Home -> HomeFeedContent(
                        viewModel = homeViewModel,
                        onListingClick = onListingClick,
                        onNavigateToExplore = { onTabChange(MainTab.Explore.ordinal) },
                        onNavigateToExploreWithTag = { tagName ->
                            // tagName is the display name. We look up the ID from the HomeViewModel's
                            // cached discovery bundle so we can use toggleInterestChipWithId directly.
                            val chipId = homeViewModel.trendingStyleTagChipIdForName(tagName)
                            exploreViewModel.toggleInterestChipWithId(chipId, tagName)
                            onTabChange(MainTab.Explore.ordinal)
                        },
                        onOrdersClick = openOrders,
                        onDeliveringJourneyClick = onHomeDeliveringJourneyClick ?: openOrders,
                        onNavigateToChat = {
                            if (isGuestMode) onRequestLogin(GuestLoginReason.ChatFromHome)
                            else onTabChange(MainTab.Chat.ordinal)
                        },
                        onNavigateToSaved = {
                            if (isGuestMode) {
                                onRequestLogin(GuestLoginReason.Saved)
                            } else {
                                profileViewModel.requestWishlistTabFromHome()
                                onTabChange(MainTab.Profile.ordinal)
                            }
                        },
                        onNavigateToPost = {
                            if (isGuestMode) onRequestLogin(GuestLoginReason.SellFromHome)
                            else onTabChange(MainTab.Post.ordinal)
                        },
                        isGuestBrowse = isGuestMode,
                        onRequestLogin = onRequestLogin,
                        onPromoSlideClick = onPromoSlideClick,
                        promoSlides = promoSlides,
                        onHomeEditorialPostClick = onHomeEditorialPostClick,
                        onFeaturedSellerClick = onFeaturedSellerClick,
                        onOpenFeaturedSellersAll = onOpenFeaturedSellersAll,
                        // Home "Add your size" banner routes to the same editor as Explore's nudge.
                        onOpenSizingSetup = if (isGuestMode) null else onEditProfile,
                    )
                    MainTab.Explore -> ExploreScreen(
                        viewModel = exploreViewModel,
                        onListingClick = onListingClick,
                        onFeaturedSellerClick = onFeaturedSellerClick,
                        onSeeAllFeaturedSellersClick = onOpenFeaturedSellersAll,
                        onPromoSlideClick = onPromoSlideClick,
                        promoSlides = promoSlides,
                        isGuestMode = isGuestMode,
                        onRequestLogin = onRequestLogin,
                        // "Match my size" nudge → open the profile editor where the user can
                        // add a reference size/measurements (reuses the same flow as edit profile).
                        onOpenSizingSetup = onEditProfile,
                    )
                    MainTab.Post -> if (isGuestMode) {
                        GuestTabPlaceholder(
                            titleRes = R.string.guest_tab_post_title,
                            bodyRes = R.string.guest_tab_post_body,
                            onSignIn = { onRequestLogin(GuestLoginReason.Post) },
                        )
                    } else {
                        CreateListingFlowScreen(
                            viewModel = postViewModel,
                            addressBookViewModel = addressBookViewModel,
                            onClose = {
                                postViewModel.cancel()
                                onTabChange(MainTab.Home.ordinal)
                            },
                        )
                    }
                    MainTab.Chat -> if (isGuestMode) {
                        GuestTabPlaceholder(
                            titleRes = R.string.guest_tab_chat_title,
                            bodyRes = R.string.guest_tab_chat_body,
                            onSignIn = { onRequestLogin(GuestLoginReason.Chat) },
                        )
                    } else {
                        ChatScreen(
                            viewModel = chatViewModel,
                            onConversationClick = onConversationClick,
                            onPromoSlideClick = onPromoSlideClick,
                            promoSlides = promoSlides,
                        )
                    }
                    MainTab.Profile -> if (isGuestMode) {
                        GuestTabPlaceholder(
                            titleRes = R.string.guest_tab_profile_title,
                            bodyRes = R.string.guest_tab_profile_body,
                            onSignIn = { onRequestLogin(GuestLoginReason.Profile) },
                        )
                    } else {
                        ProfileScreen(
                            viewModel = profileViewModel,
                            onLogout = onLogout,
                            isLoggingOut = isLoggingOut,
                            onEditProfile = onEditProfile,
                            onShippingAddressesClick = onShippingAddressesClick,
                            onInviteFriendsClick = onInviteFriendsClick,
                            onOrdersClick = onOrdersClick,
                            onListingClick = onListingClick,
                            onOpenFollowConnections = onOpenFollowConnections,
                            onNavigateToExploreFromProfile = onNavigateToExploreFromProfile,
                        )
                    }
                }
            }
        }
    }
    if (featureTourVisible) {
        AppFeatureTourOverlay(
            visible = true,
            anchors = tourAnchors,
            currentStep = tourStep,
            onStepChange = { tourStep = it },
            onSkip = {
                AppFeatureTourStore.markCompletedForCurrentVersion(context.applicationContext)
                onFeatureTourFinished()
            },
            onFinish = {
                AppFeatureTourStore.markCompletedForCurrentVersion(context.applicationContext)
                onFeatureTourFinished()
            },
        )
    }
    if (showNotificationScreen) {
        NotificationScreen(
            modifier = Modifier.fillMaxSize(),
            viewModel = notificationsViewModel,
            onBack = { showNotificationScreen = false },
            onExploreClick = {
                showNotificationScreen = false
                onTabChange(MainTab.Explore.ordinal)
            },
            onPromoSlideClick = onPromoSlideClick,
            promoSlides = promoSlides,
            onOpenOrder = { orderId ->
                showNotificationScreen = false
                onOpenOrderFromNotification(orderId)
            },
            onOpenListing = { listingId, sellerId ->
                showNotificationScreen = false
                onOpenListingFromNotification(listingId, sellerId)
            },
            onOpenChat = { conversationId ->
                showNotificationScreen = false
                onNavigateToChatConversation(conversationId)
            },
            onOpenFollowConnections = { tab ->
                showNotificationScreen = false
                onOpenFollowConnections(tab)
            },
            onOpenExplore = {
                showNotificationScreen = false
                onTabChange(MainTab.Explore.ordinal)
            },
            onOpenInviteFriends = {
                showNotificationScreen = false
                onInviteFriendsClick()
            },
            onPromoMainTab = { tab ->
                showNotificationScreen = false
                onTabChange(tab.ordinal)
            },
            onPromoOpenOrders = {
                showNotificationScreen = false
                onOrdersClick()
            },
        )
    }
    if (showSettingsScreen) {
        SettingsScreen(
            modifier = Modifier.fillMaxSize(),
            onBack = { showSettingsScreen = false },
            onLogout = onLogout,
            onLogoutAll = onLogoutAll,
            isLoggingOut = isLoggingOut,
            onOpenShippingAddresses = {
                showSettingsScreen = false
                onShippingAddressesClick()
            },
            onOpenOrders = {
                showSettingsScreen = false
                onOrdersClick()
            },
            onOpenEditProfile = {
                showSettingsScreen = false
                onEditProfile()
            },
            onOpenChangePassword = {
                showSettingsScreen = false
                showChangePasswordScreen = true
            },
        )
    }
    if (showChangePasswordScreen) {
        ChangePasswordScreen(
            modifier = Modifier.fillMaxSize(),
            currentPassword = cpCurrent,
            newPassword = cpNew,
            confirmPassword = cpConfirm,
            onCurrentPasswordChange = changePasswordViewModel::onCurrentPasswordChange,
            onNewPasswordChange = changePasswordViewModel::onNewPasswordChange,
            onConfirmPasswordChange = changePasswordViewModel::onConfirmPasswordChange,
            canSubmit = canSubmitPw,
            isSubmitting = cpSubmitting,
            onSubmit = changePasswordViewModel::submit,
            onBack = { showChangePasswordScreen = false },
        )
    }
    val hasMainNavOverlayBack = remember(
        featureTourVisible,
        showChangePasswordScreen,
        showSettingsScreen,
        showNotificationScreen,
        notificationDetailId,
        selectedTab,
        exploreSearchExpanded,
    ) {
        featureTourVisible ||
            showChangePasswordScreen ||
            showSettingsScreen ||
            showNotificationScreen ||
            (
                selectedTab in tabs.indices &&
                    tabs[selectedTab] == MainTab.Explore &&
                    exploreSearchExpanded
                )
    }
    BackHandler(enabled = hasMainNavOverlayBack) {
        when {
            showChangePasswordScreen -> showChangePasswordScreen = false
            showSettingsScreen -> showSettingsScreen = false
            showNotificationScreen && notificationDetailId != null -> {
                notificationsViewModel.closeDetail()
            }
            showNotificationScreen -> showNotificationScreen = false
            featureTourVisible -> {
                if (tourStep == AppTourStep.Intro) {
                    AppFeatureTourStore.markCompletedForCurrentVersion(context.applicationContext)
                    onFeatureTourFinished()
                } else {
                    val prev = AppTourStep.entries.getOrNull(tourStep.ordinal - 1)
                    if (prev != null) tourStep = prev
                }
            }
            selectedTab in tabs.indices &&
                tabs[selectedTab] == MainTab.Explore &&
                exploreSearchExpanded -> {
                exploreViewModel.setSearchBarExpanded(false)
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTopBar(
    inboxUnreadCount: Int,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onOrdersClick: () -> Unit,
    showGuestSignIn: Boolean = false,
    onGuestSignInClick: () -> Unit = {},
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    isLoggingOut: Boolean,
) {
    var menuExpanded by androidx.compose.runtime.remember { mutableStateOf(false) }
    TopAppBar(
        title = { FashScreenTitle(suffixRes = MainTab.Profile.headerSuffixRes) },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_label),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (showGuestSignIn) {
                GuestTopBarSignInAction(onClick = onGuestSignInClick)
            } else {
                FashInboxNotificationIconButton(
                    unreadCount = inboxUnreadCount,
                    onClick = onNotificationsClick,
                )
                IconButton(onClick = onOrdersClick) {
                    Icon(
                        imageVector = Icons.Default.LocalMall,
                        contentDescription = stringResource(R.string.orders_icon_cd),
                        tint = FashColors.Primary,
                    )
                }
            }
            // DropdownMenu must share a Box with the anchor IconButton; as a bare Row sibling it mispositions.
            if (!showGuestSignIn) {
            Box(modifier = Modifier.wrapContentSize(align = Alignment.TopEnd)) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.cd_overflow_menu),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.align(Alignment.BottomEnd),
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_logout)) },
                        onClick = { menuExpanded = false; onLogout() },
                        enabled = !isLoggingOut,
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_settings)) },
                        onClick = { menuExpanded = false; onOpenSettings() },
                        enabled = !isLoggingOut,
                    )
                }
            }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    @StringRes suffixRes: Int,
    inboxUnreadCount: Int,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onOrdersClick: (() -> Unit)? = null,
    showGuestSignIn: Boolean = false,
    onGuestSignInClick: () -> Unit = {},
    tourTopBarAnchorsEnabled: Boolean = false,
    onTourTopActionsPositioned: (LayoutCoordinates?) -> Unit = {},
) {
    androidx.compose.material3.TopAppBar(
        title = { FashScreenTitle(suffixRes = suffixRes) },
        actions = {
            Box(
                modifier = if (tourTopBarAnchorsEnabled) {
                    Modifier.onGloballyPositioned { coords ->
                        onTourTopActionsPositioned(coords.takeIf { it.isAttached })
                    }
                } else {
                    Modifier
                },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_label),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (showGuestSignIn) {
                        GuestTopBarSignInAction(onClick = onGuestSignInClick)
                    } else {
                        FashInboxNotificationIconButton(
                            unreadCount = inboxUnreadCount,
                            onClick = onNotificationsClick,
                        )
                        onOrdersClick?.let { openOrders ->
                            IconButton(onClick = openOrders) {
                                Icon(
                                    imageVector = Icons.Default.LocalMall,
                                    contentDescription = stringResource(R.string.orders_icon_cd),
                                    tint = FashColors.Primary,
                                )
                            }
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

