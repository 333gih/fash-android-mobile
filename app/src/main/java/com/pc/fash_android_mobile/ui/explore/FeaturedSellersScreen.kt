package com.pc.fash_android_mobile.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.search.FeaturedSellerItem
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.components.ProfilePreviewEmptySlotPlaceholder
import com.pc.fash_android_mobile.ui.components.ProfilePreviewRowCaption
import com.pc.fash_android_mobile.ui.feed.FeedErrorColumn
import com.pc.fash_android_mobile.ui.feed.resolveListingImageUrl
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturedSellersScreen(
    modifier: Modifier = Modifier,
    viewModel: FeaturedSellersViewModel,
    onBack: () -> Unit,
    onSellerClick: (FeaturedSellerItem) -> Unit,
    onListingClick: (listingId: String, sellerId: String?) -> Unit,
) {
    val items by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val loadErrorDetail by viewModel.loadErrorDetail.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val previewCovers by viewModel.previewCoverUrlsBySellerKey.collectAsState()
    val pullState = rememberPullToRefreshState()
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.featured_sellers_all_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = scheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.surface,
                    titleContentColor = scheme.onSurface,
                ),
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            state = pullState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = isRefreshing,
                    color = FashColors.Primary,
                    containerColor = scheme.surface,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
        ) {
            when {
                isLoading && items.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = FashColors.Primary)
                    }
                }
                loadError && items.isEmpty() -> {
                    FeedErrorColumn(
                        message = loadErrorDetail?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.feed_load_error),
                        onRetry = { viewModel.load() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(FashTheme.spacing.editorialStart),
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = FashTheme.spacing.editorialStart,
                            end = FashTheme.spacing.editorialEnd,
                            top = 8.dp,
                            bottom = 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.featured_sellers_all_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        items(
                            items = items,
                            key = { FeaturedSellersViewModel.sellerKey(it) },
                        ) { seller ->
                            LaunchedEffect(seller.userId, seller.username) {
                                viewModel.ensurePreviewCoversLoaded(seller)
                            }
                            val key = FeaturedSellersViewModel.sellerKey(seller)
                            val covers = previewCovers[key].orEmpty()
                            FeaturedSellerFullCard(
                                seller = seller,
                                previewCoverUrls = covers,
                                onSellerClick = { onSellerClick(seller) },
                                onListingClick = { listingId ->
                                    onListingClick(listingId, seller.userId.takeIf { it.isNotBlank() })
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedSellerFullCard(
    seller: FeaturedSellerItem,
    previewCoverUrls: List<String?>,
    onSellerClick: () -> Unit,
    onListingClick: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val cardShape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        color = scheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .clickable(onClick = onSellerClick),
                verticalAlignment = Alignment.Top,
            ) {
                val avatarUrl = seller.avatarUrl.takeIf { it.isNotBlank() }?.let { resolveListingImageUrl(it) }
                com.pc.fash_android_mobile.ui.components.FashAvatarCircle(
                    imageUrl = avatarUrl,
                    contentDescription = null,
                    size = 64.dp,
                )
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = seller.displayName.ifBlank { seller.username }.ifBlank { "—" },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (seller.verified) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = stringResource(R.string.featured_sellers_verified_cd),
                                tint = FashColors.Primary,
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(18.dp),
                            )
                        }
                    }
                    if (seller.username.isNotBlank()) {
                        Text(
                            text = "@${seller.username.trim()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = seller.bio.trim().ifBlank {
                            stringResource(R.string.featured_sellers_no_bio)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (seller.bio.isBlank()) {
                            scheme.onSurfaceVariant.copy(alpha = 0.75f)
                        } else {
                            scheme.onSurfaceVariant
                        },
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(
                                R.string.featured_sellers_stat_followers,
                                formatCount(seller.followerCount),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurface,
                        )
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.outlineVariant,
                        )
                        Text(
                            text = stringResource(R.string.product_seller_listings, seller.listingCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurface,
                        )
                        seller.averageRating?.let { r ->
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.outlineVariant,
                            )
                            Text(
                                text = stringResource(R.string.featured_sellers_rating_value, r),
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurface,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.featured_sellers_preview_heading),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            val filledPreviewIds = seller.previewListingIds.count { it.isNotBlank() }
            if (filledPreviewIds in 1..2 && seller.listingCount > 0) {
                ProfilePreviewRowCaption(
                    previewCount = filledPreviewIds,
                    totalListingCount = seller.listingCount,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(3) { i ->
                    val listingId = seller.previewListingIds.getOrNull(i)?.trim().orEmpty()
                    val coverUrl = previewCoverUrls.getOrNull(i)
                    FeaturedSellerPreviewSlot(
                        modifier = Modifier.weight(1f),
                        listingId = listingId,
                        coverUrl = coverUrl,
                        onListingClick = onListingClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedSellerPreviewSlot(
    modifier: Modifier = Modifier,
    listingId: String,
    coverUrl: String?,
    onListingClick: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val previewCd =
        if (listingId.isNotBlank()) {
            stringResource(R.string.featured_sellers_preview_listing_cd, listingId)
        } else {
            ""
        }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp)),
    ) {
        when {
            listingId.isBlank() -> {
                ProfilePreviewEmptySlotPlaceholder(modifier = Modifier.fillMaxSize())
            }
            else -> {
                val url = coverUrl?.takeIf { !it.isNullOrBlank() }
                    ?.let { resolveListingImageUrl(it) }
                    .orEmpty()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (previewCd.isNotEmpty()) {
                                Modifier.semantics { contentDescription = previewCd }
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onListingClick(listingId) },
                ) {
                    if (url.isNotEmpty()) {
                        FashAsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(scheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = FashColors.Primary,
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatCount(n: Int): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000f)
    n >= 1_000 -> String.format("%.1fk", n / 1_000f)
    else -> n.toString()
}
