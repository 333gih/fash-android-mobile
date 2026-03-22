package com.pc.fash_android_mobile.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.pc.fash_android_mobile.ui.home.HomeFeedContent
import com.pc.fash_android_mobile.ui.post.CreateListingFlowScreen
import com.pc.fash_android_mobile.ui.main.tabs.ChatScreen
import com.pc.fash_android_mobile.ui.main.tabs.NotificationScreen
import com.pc.fash_android_mobile.ui.main.tabs.ProfileScreen
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

enum class MainTab(
    val labelRes: Int,
    val icon: ImageVector,
) {
    Home(R.string.nav_home, Icons.Default.Home),
    Explore(R.string.nav_explore, Icons.Default.Explore),
    Post(R.string.nav_post, Icons.Default.Add),
    Chat(R.string.nav_chat, Icons.Default.ChatBubbleOutline),
    Profile(R.string.nav_profile, Icons.Default.Person),
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
    onListingClick: (String) -> Unit = {},
    onEditProfile: () -> Unit = {},
    onOrdersClick: () -> Unit = {},
    onConversationClick: (String) -> Unit = {},
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
) {
    var showNotificationScreen by rememberSaveable { mutableStateOf(false) }
    val tabs = MainTab.entries

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            when (tabs[selectedTab]) {
                MainTab.Explore -> ExploreTopBar(
                    onSearchClick = { /* TODO */ },
                    onCartClick = { /* TODO */ },
                    onNotificationsClick = { showNotificationScreen = true },
                )
                MainTab.Profile -> ProfileTopBar(
                    onSearchClick = { /* TODO */ },
                    onNotificationsClick = { showNotificationScreen = true },
                    onOrdersClick = onOrdersClick,
                    onLogout = onLogout,
                    onLogoutAll = onLogoutAll,
                    isLoggingOut = isLoggingOut,
                )
                else -> MainTopBar(
                    onSearchClick = { /* TODO */ },
                    onNotificationsClick = { showNotificationScreen = true },
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = FashColors.Primary,
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = selectedTab == index
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = stringResource(tab.labelRes),
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(tab.labelRes),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        selected = selected,
                        onClick = {
                            showNotificationScreen = false
                            onTabChange(index)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FashColors.Primary,
                            selectedTextColor = FashColors.Primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = FashColors.Primary.copy(alpha = 0.12f),
                        ),
                    )
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (tabs[selectedTab]) {
                MainTab.Home -> HomeFeedContent(viewModel = homeViewModel, onListingClick = onListingClick)
                MainTab.Explore -> ExploreScreen(viewModel = exploreViewModel, onListingClick = onListingClick)
                MainTab.Post -> CreateListingFlowScreen(
                    viewModel = postViewModel,
                    onClose = { /* stay on post tab with cleared draft */ postViewModel.cancel() },
                )
                MainTab.Chat -> ChatScreen(
                    viewModel = chatViewModel,
                    onConversationClick = onConversationClick,
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
        title = {
            Text(
                text = stringResource(R.string.brand_wordmark),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = FashColors.Primary,
            )
        },
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
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
) {
    androidx.compose.material3.TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.brand_wordmark),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = FashColors.Primary,
            )
        },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreTopBar(
    onSearchClick: () -> Unit,
    onCartClick: () -> Unit,
    onNotificationsClick: () -> Unit,
) {
    androidx.compose.material3.TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.explore_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        navigationIcon = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_label),
                    tint = FashColors.Primary,
                )
            }
        },
        actions = {
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
            IconButton(onClick = onCartClick) {
                Icon(
                    imageVector = Icons.Default.LocalMall,
                    contentDescription = stringResource(R.string.add_to_cart),
                    tint = FashColors.Primary,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}
