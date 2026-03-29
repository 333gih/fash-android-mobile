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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.annotation.StringRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.pc.fash_android_mobile.ui.post.CreateListingFlowScreen
import com.pc.fash_android_mobile.data.chat.ConversationItem
import com.pc.fash_android_mobile.ui.main.tabs.ChatScreen
import com.pc.fash_android_mobile.ui.main.tabs.NotificationScreen
import com.pc.fash_android_mobile.ui.main.tabs.ProfileScreen
import com.pc.fash_android_mobile.ui.components.FashBrandMarkText
import com.pc.fash_android_mobile.ui.theme.FashBrandTypography
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

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
    profileViewModel: com.pc.fash_android_mobile.ui.main.tabs.ProfileViewModel,
    chatViewModel: com.pc.fash_android_mobile.ui.chat.ChatViewModel,
    /** Total unread messages for chat tab badge ([ChatRepository.getUnreadCount]). */
    chatUnreadCount: Int = 0,
    /** [sellerId] when known — used to open seller edit vs public detail. */
    onListingClick: (listingId: String, sellerId: String?) -> Unit = { _, _ -> },
    onEditProfile: () -> Unit = {},
    onOrdersClick: () -> Unit = {},
    /** [initialTab] 0 = people you follow, 1 = followers (e.g. Explore featured sellers “See all”). */
    onOpenFollowConnections: (initialTab: Int) -> Unit = {},
    onConversationClick: (ConversationItem) -> Unit = {},
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
) {
    var showNotificationScreen by rememberSaveable { mutableStateOf(false) }
    val tabs = MainTab.entries
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
                    onLogoutAll = onLogoutAll,
                    isLoggingOut = isLoggingOut,
                )
                MainTab.Home, MainTab.Post, MainTab.Chat -> MainTopBar(
                    suffixRes = tab.headerSuffixRes,
                    onSearchClick = openExploreSearch,
                    onNotificationsClick = { showNotificationScreen = true },
                )
                else -> MainTopBar(
                    suffixRes = MainTab.Home.headerSuffixRes,
                    onSearchClick = openExploreSearch,
                    onNotificationsClick = { showNotificationScreen = true },
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
                    )
                    MainTab.Explore -> ExploreScreen(
                        viewModel = exploreViewModel,
                        onListingClick = onListingClick,
                        onSeeAllFeaturedSellersClick = { onOpenFollowConnections(0) },
                    )
                    MainTab.Post -> CreateListingFlowScreen(
                        viewModel = postViewModel,
                        onClose = {
                            postViewModel.cancel()
                            onTabChange(MainTab.Home.ordinal)
                        },
                    )
                    MainTab.Chat -> ChatScreen(
                        viewModel = chatViewModel,
                        onConversationClick = onConversationClick,
                        onNavigateToExplore = { onTabChange(MainTab.Explore.ordinal) },
                    )
                    MainTab.Profile -> ProfileScreen(
                        viewModel = profileViewModel,
                        onLogout = onLogout,
                        onLogoutAll = onLogoutAll,
                        isLoggingOut = isLoggingOut,
                        onEditProfile = onEditProfile,
                        onOrdersClick = onOrdersClick,
                        onListingClick = onListingClick,
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
    onLogoutAll: () -> Unit,
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
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.home_logout)) },
                    onClick = { menuExpanded = false; onLogout() },
                    enabled = !isLoggingOut,
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.home_logout_all)) },
                    onClick = { menuExpanded = false; onLogoutAll() },
                    enabled = !isLoggingOut,
                )
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
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

