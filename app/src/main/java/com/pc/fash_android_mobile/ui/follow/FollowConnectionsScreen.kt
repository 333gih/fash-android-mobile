package com.pc.fash_android_mobile.ui.follow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.user.UserSearchResult
import com.pc.fash_android_mobile.ui.components.FashAvatarCircle
import com.pc.fash_android_mobile.ui.components.FashEmptyBulletTipLine
import com.pc.fash_android_mobile.ui.components.FashEmptyState
import com.pc.fash_android_mobile.ui.feed.FeedErrorColumn
import com.pc.fash_android_mobile.ui.theme.FashColors
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowConnectionsScreen(
    modifier: Modifier = Modifier,
    viewModel: FollowConnectionsViewModel,
    onBack: () -> Unit,
    /** Same pattern as chat empty inbox — opens Explore. */
    onExploreClick: () -> Unit = {},
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val following by viewModel.following.collectAsState()
    val followers by viewModel.followers.collectAsState()
    val followingTotal by viewModel.followingTotal.collectAsState()
    val followersTotal by viewModel.followersTotal.collectAsState()
    val followingLoading by viewModel.followingLoading.collectAsState()
    val followersLoading by viewModel.followersLoading.collectAsState()
    val followingLoadingMore by viewModel.followingLoadingMore.collectAsState()
    val followersLoadingMore by viewModel.followersLoadingMore.collectAsState()
    val followingFailed by viewModel.followingFailed.collectAsState()
    val followersFailed by viewModel.followersFailed.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.follow_connections_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = FashColors.Primary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        text = {
                            Text(
                                text = stringResource(R.string.profile_following),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        text = {
                            Text(
                                text = stringResource(R.string.profile_followers),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    when (selectedTab) {
                        0 -> FollowUserList(
                            users = following,
                            total = followingTotal,
                            loadFailed = followingFailed,
                            isLoading = followingLoading,
                            isLoadingMore = followingLoadingMore,
                            onRetry = { viewModel.retryActiveTab() },
                            onLoadMore = { viewModel.loadMoreFollowing() },
                            emptyContent = {
                                FollowConnectionsEmptyState(
                                    isFollowingTab = true,
                                    onExploreClick = onExploreClick,
                                )
                            },
                        )
                        else -> FollowUserList(
                            users = followers,
                            total = followersTotal,
                            loadFailed = followersFailed,
                            isLoading = followersLoading,
                            isLoadingMore = followersLoadingMore,
                            onRetry = { viewModel.retryActiveTab() },
                            onLoadMore = { viewModel.loadMoreFollowers() },
                            emptyContent = {
                                FollowConnectionsEmptyState(
                                    isFollowingTab = false,
                                    onExploreClick = onExploreClick,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowConnectionsEmptyState(
    isFollowingTab: Boolean,
    onExploreClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val title = stringResource(
        if (isFollowingTab) {
            R.string.follow_list_empty_following_title
        } else {
            R.string.follow_list_empty_followers_title
        },
    )
    val subtitle = stringResource(
        if (isFollowingTab) {
            R.string.follow_list_empty_following_subtitle
        } else {
            R.string.follow_list_empty_followers_subtitle
        },
    )
    val tip1 = stringResource(
        if (isFollowingTab) {
            R.string.follow_list_empty_following_tip_1
        } else {
            R.string.follow_list_empty_followers_tip_1
        },
    )
    val tip2 = stringResource(
        if (isFollowingTab) {
            R.string.follow_list_empty_following_tip_2
        } else {
            R.string.follow_list_empty_followers_tip_2
        },
    )
    FashEmptyState(
        icon = if (isFollowingTab) Icons.Outlined.People else Icons.Outlined.PersonAdd,
        title = title,
        subtitle = subtitle,
        footer = {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.chat_empty_suggestions_title),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FashEmptyBulletTipLine(text = tip1)
                FashEmptyBulletTipLine(text = tip2)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onExploreClick,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FashColors.Primary),
            ) {
                Text(
                    text = stringResource(R.string.home_empty_cta_explore),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
        },
    )
}

@Composable
private fun FollowUserList(
    users: List<UserSearchResult>,
    total: Int,
    loadFailed: Boolean,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    emptyContent: @Composable () -> Unit,
) {
    val listState = rememberLazyListState()
    val hasMore = users.size < total

    LaunchedEffect(listState, users.size, total, hasMore, isLoadingMore) {
        snapshotFlow {
            if (users.isEmpty()) return@snapshotFlow false
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val lastIndex = users.lastIndex
            lastVisible >= 0 && lastIndex >= 0 && lastVisible >= lastIndex - 2
        }
            .distinctUntilChanged()
            .collect { nearEnd ->
                if (nearEnd && hasMore && !isLoadingMore && users.isNotEmpty()) {
                    onLoadMore()
                }
            }
    }

    when {
        loadFailed && users.isEmpty() -> {
            FeedErrorColumn(
                message = stringResource(R.string.feed_load_error),
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )
        }
        isLoading && users.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = FashColors.Primary)
            }
        }
        users.isEmpty() -> {
            emptyContent()
        }
        else -> {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(
                    users,
                    key = { it.userId.ifBlank { it.username } },
                ) { user ->
                    FollowUserRow(user = user)
                }
                if (hasMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isLoadingMore) {
                                CircularProgressIndicator(
                                    color = FashColors.Primary,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowUserRow(user: UserSearchResult) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FashAvatarCircle(
            imageUrl = user.avatarUrl,
            contentDescription = null,
            size = 48.dp,
        )
        Column(modifier = Modifier.padding(start = 14.dp)) {
            val name = user.displayName.trim().ifBlank { user.username }
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (user.username.isNotBlank()) {
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (user.listingCount > 0) {
                Text(
                    text = stringResource(R.string.product_seller_listings, user.listingCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
