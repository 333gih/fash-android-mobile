package com.pc.fash_android_mobile.ui.main.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: SellerProfileViewModel,
    /** Must match `GET …/api/v1/users/{username}` — shown in the top bar as @handle. */
    sellerUsername: String,
    onBack: () -> Unit,
    onListingClick: (listingId: String, sellerId: String?) -> Unit = { _, _ -> },
) {
    val profile by viewModel.profile.collectAsState()
    val sellingListings by viewModel.sellingListings.collectAsState()
    val soldListings by viewModel.soldListings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val isFollowing by viewModel.isFollowing.collectAsState()
    val followInFlight by viewModel.followInFlight.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    val titleHandle = remember(sellerUsername) {
        sellerUsername.trim().removePrefix("@")
    }

    LaunchedEffect(sellerUsername) {
        viewModel.loadForSeller(sellerUsername)
    }

    val listState = rememberLazyListState()
    val collapseProgress = rememberProfileHeaderCollapseProgress(listState)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (titleHandle.isNotBlank()) "@$titleHandle" else "—",
                        style = if (collapseProgress.value > 0.45f) {
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        } else {
                            MaterialTheme.typography.titleLarge
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                isLoading && profile == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = FashColors.Primary)
                    }
                }
                loadError && profile == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.profile_load_error),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.retryLoad(sellerUsername) }) {
                            Text(stringResource(R.string.feed_retry))
                        }
                    }
                }
                else -> {
                    val items = if (selectedTab == 0) sellingListings else soldListings
                    ProfileCollapsingScrollLayout(
                        listState = listState,
                        expandedHeader = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SellerProfileHeader(profile = profile)
                                if (profile != null && viewModel.canFollowSeller()) {
                                    SellerFollowRow(
                                        isFollowing = isFollowing,
                                        inFlight = followInFlight,
                                        onToggle = { viewModel.toggleFollow() },
                                    )
                                }
                                ProfileStats(profile = profile)
                            }
                        },
                        compactHeader = {
                            ProfileCompactHeaderBar(profile = profile)
                        },
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        items = items,
                        isSellingTab = selectedTab == 0,
                        onListingClick = { id -> onListingClick(id, profile?.userId) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SellerFollowRow(
    isFollowing: Boolean,
    inFlight: Boolean,
    onToggle: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .padding(top = 8.dp, bottom = 8.dp),
    ) {
        if (isFollowing) {
            OutlinedButton(
                onClick = onToggle,
                enabled = !inFlight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FashTheme.spacing.buttonHeight),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FashColors.Primary),
            ) {
                if (inFlight) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = FashColors.Primary,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.following_button),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        } else {
            Button(
                onClick = onToggle,
                enabled = !inFlight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FashTheme.spacing.buttonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                ),
            ) {
                if (inFlight) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = scheme.onPrimary,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.follow_button),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}
