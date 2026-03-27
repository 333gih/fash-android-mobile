package com.pc.fash_android_mobile.ui.follow

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.user.UserSearchResult
import com.pc.fash_android_mobile.ui.components.FashAvatarCircle
import com.pc.fash_android_mobile.ui.feed.FeedErrorColumn
import com.pc.fash_android_mobile.ui.theme.FashColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowConnectionsScreen(
    modifier: Modifier = Modifier,
    viewModel: FollowConnectionsViewModel,
    onBack: () -> Unit,
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val following by viewModel.following.collectAsState()
    val followers by viewModel.followers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
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
                    when {
                        isLoading && following.isEmpty() && followers.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = FashColors.Primary)
                            }
                        }
                        selectedTab == 0 -> FollowUserList(
                            users = following,
                            loadFailed = followingFailed,
                            emptyTitle = stringResource(R.string.follow_list_empty_following),
                            onRetry = { viewModel.load() },
                        )
                        else -> FollowUserList(
                            users = followers,
                            loadFailed = followersFailed,
                            emptyTitle = stringResource(R.string.follow_list_empty_followers),
                            onRetry = { viewModel.load() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowUserList(
    users: List<UserSearchResult>,
    loadFailed: Boolean,
    emptyTitle: String,
    onRetry: () -> Unit,
) {
    when {
        loadFailed && users.isEmpty() -> {
            FeedErrorColumn(
                message = stringResource(R.string.feed_load_error),
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )
        }
        users.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = emptyTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(users, key = { it.userId.ifBlank { it.username } }) { user ->
                    FollowUserRow(user = user)
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
        val initial = user.displayName.trim().firstOrNull()?.takeIf { it.isLetter() }
            ?: user.username.trim().firstOrNull()?.takeIf { it.isLetter() }
        FashAvatarCircle(
            imageUrl = user.avatarUrl,
            contentDescription = null,
            size = 48.dp,
            fallbackInitial = initial,
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
        }
    }
}
