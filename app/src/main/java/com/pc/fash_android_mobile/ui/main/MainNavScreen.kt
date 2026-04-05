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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BadgedBox
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.pc.fash_android_mobile.ui.main.tabs.ChatScreen
import com.pc.fash_android_mobile.ui.main.tabs.NotificationScreen
import com.pc.fash_android_mobile.ui.main.tabs.ProfileScreen
import com.pc.fash_android_mobile.ui.main.tabs.SettingsScreen
import com.pc.fash_android_mobile.ui.settings.ChangePasswordScreen
import com.pc.fash_android_mobile.ui.settings.ChangePasswordViewModel
import com.pc.fash_android_mobile.ui.components.FashBrandMarkText
import com.pc.fash_android_mobile.ui.theme.FashBrandTypography
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import com.pc.fash_android_mobile.data.user.UserSearchResult

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
    onOrdersClick: () -> Unit = {},
    /** [initialTab] 0 = people you follow, 1 = followers. */
    onOpenFollowConnections: (initialTab: Int) -> Unit = {},
    /** Explore featured sellers “See all” — full list from `GET /search/featured-sellers`. */
    onOpenFeaturedSellersAll: () -> Unit = {},
    /** Featured seller chip on Explore — opens seller shop (`GET …/users/{username}`). */
    onFeaturedSellerClick: (UserSearchResult) -> Unit = {},
    onConversationClick: (ConversationItem) -> Unit = {},
    /** Profile / seller shop: open Explore → Posts with filters + search + optional country. */
    onNavigateToExploreFromProfile: (
        categoryId: String?,
        brandId: String?,
        aestheticTagId: String?,
        searchQuery: String,
        countryId: String?,
        countryIso2: String?,
    ) -> Unit = { _, _, _, _, _, _ -> },
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
) {
    var showNotificationScreen by rememberSaveable { mutableStateOf(false) }
    var showSettingsScreen by rememberSaveable { mutableStateOf(false) }
    var showChangePasswordScreen by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
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
    LaunchedEffect(Unit) {
        chatViewModel.refreshUnreadCount()
    }
    LaunchedEffect(selectedTab) {
        if (tabs.getOrNull(selectedTab) == MainTab.Home) {
            chatViewModel.refreshUnreadCount()
        }
    }
    val exploreSearchExpanded by exploreViewModel.searchBarExpanded.collectAsState()
    val openExploreSearch: () -> Unit = {
        exploreViewModel.requestSearchBarExpanded()
        onTabChange(MainTab.Explore.ordinal)
    }
    val isPostListingFlow = tabs.getOrNull(selectedTab) == MainTab.Post

    Box(modifier = modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (!isPostListingFlow) {
                when (val tab = tabs.getOrNull(selectedTab)) {
                MainTab.Explore -> ExploreTopBar(
                    viewModel = exploreViewModel,
                    onOrdersClick = onOrdersClick,
                    onNotificationsClick = { showNotificationScreen = true },
                )
                MainTab.Profile -> ProfileTopBar(
                    onSearchClick = openExploreSearch,
                    onNotificationsClick = { showNotificationScreen = true },
                    onOrdersClick = onOrdersClick,
                    onLogout = onLogout,
                    onOpenSettings = {
                        showNotificationScreen = false
                        showSettingsScreen = true
                    },
                    isLoggingOut = isLoggingOut,
                )
                MainTab.Home -> MainTopBar(
                    suffixRes = tab.headerSuffixRes,
                    onSearchClick = openExploreSearch,
                    onNotificationsClick = { showNotificationScreen = true },
                    onOrdersClick = onOrdersClick,
                )
                MainTab.Post, MainTab.Chat -> MainTopBar(
                    suffixRes = tab.headerSuffixRes,
                    onSearchClick = openExploreSearch,
                    onNotificationsClick = { showNotificationScreen = true },
                )
                else -> MainTopBar(
                    suffixRes = MainTab.Home.headerSuffixRes,
                    onSearchClick = openExploreSearch,
                    onNotificationsClick = { showNotificationScreen = true },
                    onOrdersClick = onOrdersClick,
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
                        onTabChange(index)
                    },
                    onExploreReselected = { exploreViewModel.requestScrollExploreToTop() },
                    onChatReselected = {
                        chatViewModel.loadConversations()
                        chatViewModel.refreshUnreadCount()
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
                if (selectedTab in tabs.indices && tabs[selectedTab] == MainTab.Explore) {
                    exploreViewModel.onExploreTabSelected()
                } else {
                    exploreViewModel.setSearchBarExpanded(false)
                }
                if (selectedTab in tabs.indices && tabs[selectedTab] == MainTab.Chat) {
                    chatViewModel.loadConversations()
                    chatViewModel.refreshUnreadCount()
                }
            }
            if (!showNotificationScreen &&
                selectedTab in tabs.indices &&
                tabs[selectedTab] == MainTab.Explore &&
                exploreSearchExpanded
            ) {
                BackHandler {
                    exploreViewModel.setSearchBarExpanded(false)
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
                        onOrdersClick = onOrdersClick,
                        onNavigateToChat = { onTabChange(MainTab.Chat.ordinal) },
                        onNavigateToSaved = { onTabChange(MainTab.Profile.ordinal) },
                        onNavigateToPost = { onTabChange(MainTab.Post.ordinal) },
                        onPromoSlideClick = { _, _ -> onTabChange(MainTab.Explore.ordinal) },
                        promoSlides = null,
                    )
                    MainTab.Explore -> ExploreScreen(
                        viewModel = exploreViewModel,
                        onListingClick = onListingClick,
                        onFeaturedSellerClick = onFeaturedSellerClick,
                        onSeeAllFeaturedSellersClick = onOpenFeaturedSellersAll,
                        onPromoSlideClick = { _, _ -> },
                        promoSlides = null,
                    )
                    MainTab.Post -> CreateListingFlowScreen(
                        viewModel = postViewModel,
                        addressBookViewModel = addressBookViewModel,
                        onClose = {
                            postViewModel.cancel()
                            onTabChange(MainTab.Home.ordinal)
                        },
                    )
                    MainTab.Chat -> ChatScreen(
                        viewModel = chatViewModel,
                        onConversationClick = onConversationClick,
                        onPromoSlideClick = { _, _ -> onTabChange(MainTab.Explore.ordinal) },
                        promoSlides = null,
                    )
                    MainTab.Profile -> ProfileScreen(
                        viewModel = profileViewModel,
                        onLogout = onLogout,
                        isLoggingOut = isLoggingOut,
                        onEditProfile = onEditProfile,
                        onShippingAddressesClick = onShippingAddressesClick,
                        onOrdersClick = onOrdersClick,
                        onListingClick = onListingClick,
                        onOpenFollowConnections = onOpenFollowConnections,
                        onNavigateToExploreFromProfile = onNavigateToExploreFromProfile,
                    )
                }
            }
        }
    }
    if (showNotificationScreen) {
        BackHandler { showNotificationScreen = false }
        NotificationScreen(
            modifier = Modifier.fillMaxSize(),
            onBack = { showNotificationScreen = false },
            onExploreClick = {
                showNotificationScreen = false
                onTabChange(MainTab.Explore.ordinal)
            },
        )
    }
    if (showSettingsScreen) {
        BackHandler { showSettingsScreen = false }
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
        BackHandler { showChangePasswordScreen = false }
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTopBar(
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onOrdersClick: () -> Unit,
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
            BadgedBox(
                badge = {
                    if (true) {
                        androidx.compose.material3.Badge(containerColor = FashColors.Primary)
                    }
                },
            ) {
                IconButton(onClick = onNotificationsClick) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = stringResource(R.string.notifications),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            IconButton(onClick = onOrdersClick) {
                Icon(
                    imageVector = Icons.Default.LocalMall,
                    contentDescription = stringResource(R.string.orders_icon_cd),
                    tint = FashColors.Primary,
                )
            }
            // DropdownMenu must share a Box with the anchor IconButton; as a bare Row sibling it mispositions.
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
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onOrdersClick: (() -> Unit)? = null,
) {
    androidx.compose.material3.TopAppBar(
        title = { FashScreenTitle(suffixRes = suffixRes) },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_label),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            BadgedBox(
                badge = {
                    if (true) {
                        androidx.compose.material3.Badge(containerColor = FashColors.Primary)
                    }
                },
            ) {
                IconButton(onClick = onNotificationsClick) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = stringResource(R.string.notifications),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            onOrdersClick?.let { openOrders ->
                IconButton(onClick = openOrders) {
                    Icon(
                        imageVector = Icons.Default.LocalMall,
                        contentDescription = stringResource(R.string.orders_icon_cd),
                        tint = FashColors.Primary,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

