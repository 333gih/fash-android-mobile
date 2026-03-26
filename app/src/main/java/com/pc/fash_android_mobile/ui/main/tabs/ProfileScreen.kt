package com.pc.fash_android_mobile.ui.main.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.ui.common.stableLazyKey
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.components.FashEmptyState
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    onLogoutAll: () -> Unit,
    isLoggingOut: Boolean = false,
    onEditProfile: () -> Unit = { },
    onOrdersClick: () -> Unit = { },
    onListingClick: (listingId: String, sellerId: String?) -> Unit = { _, _ -> },
) {
    val profile by viewModel.profile.collectAsState()
    val sellingListings by viewModel.sellingListings.collectAsState()
    val soldListings by viewModel.soldListings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    Column(modifier = modifier.fillMaxSize()) {
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
                    Button(onClick = { viewModel.retryLoad() }) {
                        Text(stringResource(R.string.feed_retry))
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    ProfileHeader(
                        profile = profile,
                        onEditClick = onEditProfile,
                    )
                    ProfileStats(profile = profile)
                    ProfileTabs(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                    )
                    val items = if (selectedTab == 0) sellingListings else soldListings
                    ProfileProductGrid(
                        items = items,
                        onItemClick = { id -> onListingClick(id, profile?.userId) },
                        modifier = Modifier.weight(1f),
                        isSellingTab = selectedTab == 0,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: com.pc.fash_android_mobile.data.user.ProfileInfo?,
    onEditClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val coverUrl = profile?.coverImageUrl?.takeIf { it.isNotBlank() }?.let { resolveImageUrl(it) }
    val avatarUrl = profile?.avatarUrl?.takeIf { it.isNotBlank() }?.let { resolveImageUrl(it) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.surfaceContainerHigh),
        ) {
            if (coverUrl != null) {
                FashAsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        IconButton(
            onClick = onEditClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = scheme.onSurface,
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = FashTheme.spacing.editorialStart, bottom = 0.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(scheme.surfaceContainerHigh)
                    .padding(4.dp),
            ) {
                if (avatarUrl != null) {
                    FashAsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(FashColors.Primary.copy(alpha = 0.2f)),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButton(
                onClick = onEditClick,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .padding(end = 16.dp),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = FashColors.Primary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.profile_edit),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .padding(top = 8.dp, bottom = 16.dp),
    ) {
        Text(
            text = profile?.displayName?.ifBlank { profile?.username ?: "—" } ?: "—",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = scheme.onSurface,
        )
        Text(
            text = "@${profile?.username ?: "—"}",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
        profile?.bio?.takeIf { it.isNotBlank() }?.let { bio ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = bio,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!profile?.aestheticTags.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                profile!!.aestheticTags.forEach { tag ->
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = FashColors.Primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(FashTheme.spacing.radiusPill))
                            .background(FashColors.Primary.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStats(profile: com.pc.fash_android_mobile.data.user.ProfileInfo?) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ProfileStatItem(
            value = formatCount(profile?.followerCount ?: 0),
            label = stringResource(R.string.profile_followers),
        )
        ProfileStatItem(
            value = (profile?.followingCount ?: 0).toString(),
            label = stringResource(R.string.profile_following),
        )
        ProfileStatItem(
            value = (profile?.productCount ?: 0).toString(),
            label = stringResource(R.string.profile_products),
        )
        ProfileStatItem(
            value = (profile?.soldCount ?: 0).toString(),
            label = stringResource(R.string.profile_sold),
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        profile?.rating?.takeIf { it > 0 }?.let { rating ->
            val reviews = profile.reviewCount ?: 0
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = androidx.compose.ui.graphics.Color(0xFFFFC107),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.profile_rating_format, rating, reviews),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
        if (profile?.hasFastDelivery == true) {
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.profile_fast_delivery),
                style = MaterialTheme.typography.labelSmall,
                color = FashColors.Success,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(FashColors.Success.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ProfileStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart),
    ) {
        listOf(
            R.string.profile_tab_selling,
            R.string.profile_tab_sold,
        ).forEachIndexed { index, resId ->
            val selected = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 12.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(resId),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) FashColors.Primary else scheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(FashColors.Primary),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileProductGrid(
    items: List<ListingFeedItem>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isSellingTab: Boolean = true,
) {
    if (items.isEmpty()) {
        FashEmptyState(
            icon = if (isSellingTab) Icons.Outlined.Storefront else Icons.Outlined.CheckCircle,
            title = stringResource(
                if (isSellingTab) {
                    R.string.profile_empty_selling_title
                } else {
                    R.string.profile_empty_sold_title
                },
            ),
            subtitle = stringResource(
                if (isSellingTab) {
                    R.string.profile_empty_selling_subtitle
                } else {
                    R.string.profile_empty_sold_subtitle
                },
            ),
            modifier = modifier,
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = FashTheme.spacing.editorialStart,
            vertical = 16.dp,
        ),
    ) {
        itemsIndexed(
            items,
            key = { index, item -> stableLazyKey(item.id, index, "prof") },
        ) { _, item ->
            ProfileProductCard(
                item = item,
                onClick = { onItemClick(item.id) },
            )
        }
    }
}

@Composable
private fun ProfileProductCard(item: ListingFeedItem, onClick: () -> Unit) {
    val imageUrl = resolveImageUrl(item.coverImageUrl)
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
    ) {
        if (imageUrl.isNotEmpty()) {
            FashAsyncImage(
                model = imageUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.no_image),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color.Transparent,
                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f),
                        ),
                    ),
                )
                .padding(8.dp),
        ) {
            Text(
                text = formatPrice(item.priceVnd),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = androidx.compose.ui.graphics.Color.White,
            )
        }
    }
}

private fun resolveImageUrl(path: String): String {
    if (path.isBlank()) return ""
    if (path.startsWith("http")) return path
    val base = AppEnvironment.apiBaseUrl.trimEnd('/')
    return if (path.startsWith("/")) "$base$path" else "$base/$path"
}

private fun formatCount(count: Int): String =
    when {
        count >= 1_000_000 -> "${count / 1_000_000}.${(count % 1_000_000) / 100_000}M"
        count >= 1_000 -> "${count / 1_000}.${(count % 1_000) / 100}k"
        else -> count.toString()
    }

private fun formatPrice(vnd: Long): String =
    "₫ ${"%,d".format(vnd).replace(',', '.')}"
