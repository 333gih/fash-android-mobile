package com.pc.fash_android_mobile.ui.listing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.activity.compose.BackHandler
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.listing.ListingDetail
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

private const val DESCRIPTION_PREVIEW_LINES = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    modifier: Modifier = Modifier,
    listingId: String,
    viewModel: ProductDetailViewModel,
    onBack: () -> Unit,
    onChat: (String) -> Unit = {},
    onBuyNow: (String) -> Unit = {},
    onShare: (String) -> Unit = {},
    onListingClick: (String) -> Unit = {},
) {
    val detail by viewModel.detail.collectAsState()
    val sellerProfile by viewModel.sellerProfile.collectAsState()
    val moreFromSeller by viewModel.moreFromSeller.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val isFollowing by viewModel.isFollowing.collectAsState()

    LaunchedEffect(listingId) {
        viewModel.loadDetail(listingId)
    }

    BackHandler {
        onBack()
    }

    when {
        isLoading && detail == null -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = FashColors.Primary)
            }
        }
        loadError != null && detail == null -> {
            val err = loadError!!
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.retryLoad(listingId) }) {
                        Text(stringResource(R.string.feed_retry))
                    }
                }
            }
        }
        detail != null -> {
            val d: ListingDetail = requireNotNull(detail)
            Box(modifier = modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Column {
                            ProductImageSection(
                            detail = d,
                            onBack = onBack,
                            onShare = { onShare(d.id) },
                            onSave = { viewModel.toggleSave() },
                        )
                        SellerInfoCard(
                            detail = d,
                            profile = sellerProfile,
                            isFollowing = isFollowing,
                            onFollow = { viewModel.follow(d.sellerId) },
                            onUnfollow = { viewModel.unfollow(d.sellerId) },
                        )
                        PriceAndTitleSection(detail = d)
                        SpecsGrid(detail = d)
                        DescriptionAndTagsSection(detail = d)
                        if (moreFromSeller.isNotEmpty()) {
                            MoreFromSellerSection(
                                sellerUsername = d.sellerUsername,
                                items = moreFromSeller,
                                onItemClick = onListingClick,
                                excludeId = d.id,
                            )
                        }
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                    BottomActionBar(
                        priceVnd = d.priceVnd,
                        onChat = { onChat(d.id) },
                        onBuyNow = { onBuyNow(d.id) },
                    )
                }
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.orders_back),
                                tint = FashColors.Primary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        navigationIconContentColor = FashColors.Primary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ProductImageSection(
    detail: ListingDetail,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
) {
    val imageUrl = resolveImageUrl(
        detail.imageUrls.firstOrNull() ?: ""
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        if (imageUrl.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = detail.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.no_image),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            CircleShape,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.share),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(
                    onClick = onSave,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            CircleShape,
                        ),
                ) {
                    Icon(
                        imageVector = if (detail.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = stringResource(R.string.save),
                        tint = if (detail.isSaved) FashColors.Primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
        }
    }
}

@Composable
private fun SellerInfoCard(
    detail: ListingDetail,
    profile: ProfileInfo?,
    isFollowing: Boolean,
    onFollow: () -> Unit,
    onUnfollow: () -> Unit,
) {
    val avatarUrl = resolveImageUrl(profile?.avatarUrl ?: detail.sellerAvatarUrl ?: "")
    val displayName = profile?.displayName?.ifBlank { null }
        ?: detail.sellerDisplayName?.ifBlank { null }
        ?: detail.sellerUsername?.let { "@$it" }
        ?: "—"
    val username = detail.sellerUsername ?: "user"
    val rating = profile?.rating
    val reviewCount = profile?.reviewCount

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .offset(y = (-24).dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                )
                .padding(FashTheme.spacing.spacing4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                if (avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(avatarUrl).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (rating != null && rating > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = FashColors.Primary,
                        )
                        Text(
                            text = "%.1f".format(rating),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        reviewCount?.let { count ->
                            Text(
                                text = "($count)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = if (isFollowing) onUnfollow else onFollow,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = FashColors.Primary,
                ),
            ) {
                Text(
                    text = stringResource(if (isFollowing) R.string.follow_following else R.string.follow_button),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun PriceAndTitleSection(detail: ListingDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart, vertical = FashTheme.spacing.spacing3),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = formatPrice(detail.priceVnd),
                style = FashTheme.textStyles.price,
                color = FashColors.Primary,
            )
            if (detail.condition.isNotBlank()) {
                Text(
                    text = formatCondition(detail.condition),
                    style = MaterialTheme.typography.labelSmall,
                    color = FashColors.Success,
                    modifier = Modifier
                        .background(
                            FashColors.Success.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = detail.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SpecsGrid(detail: ListingDetail) {
    val specs = listOf(
        stringResource(R.string.product_category) to (detail.category ?: "—"),
        stringResource(R.string.product_size) to (detail.size ?: "—"),
        stringResource(R.string.product_brand) to (detail.brand ?: "—"),
        stringResource(R.string.product_material) to (detail.material ?: "—"),
    ).filter { it.second != "—" }.ifEmpty {
        listOf(
            stringResource(R.string.product_category) to (detail.category ?: "—"),
            stringResource(R.string.product_size) to (detail.size ?: "—"),
            stringResource(R.string.product_brand) to (detail.brand ?: "—"),
            stringResource(R.string.product_material) to (detail.material ?: "—"),
        )
    }
    if (specs.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart, vertical = FashTheme.spacing.spacing2),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    RoundedCornerShape(FashTheme.spacing.radiusCard),
                )
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                specs.chunked(2).forEach { rowSpecs ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        rowSpecs.forEach { (label, value) ->
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
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
private fun DescriptionAndTagsSection(detail: ListingDetail) {
    var expanded by remember { mutableStateOf(false) }
    val description = detail.description
    val hasMore = description.length > 120

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart, vertical = FashTheme.spacing.spacing3),
    ) {
        if (description.isNotBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else DESCRIPTION_PREVIEW_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            if (hasMore && !expanded) {
                Text(
                    text = stringResource(R.string.product_see_more),
                    style = MaterialTheme.typography.labelLarge,
                    color = FashColors.Primary,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { expanded = true },
                )
            }
        }
        if (detail.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                detail.tags.forEach { tag ->
                    Text(
                        text = "#$tag",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                RoundedCornerShape(FashTheme.spacing.radiusPill),
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreFromSellerSection(
    sellerUsername: String?,
    items: List<ListingFeedItem>,
    onItemClick: (String) -> Unit,
    excludeId: String,
) {
    val username = sellerUsername ?: "user"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart, vertical = FashTheme.spacing.spacing4),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.product_more_from_seller, "@$username"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items.filter { it.id != excludeId }.take(5).forEach { item ->
                MoreFromSellerCard(
                    item = item,
                    onClick = { onItemClick(item.id) },
                )
            }
        }
    }
}

@Composable
private fun MoreFromSellerCard(
    item: ListingFeedItem,
    onClick: () -> Unit,
) {
    val imageUrl = resolveImageUrl(item.coverImageUrl)
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            if (imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).build(),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = formatPrice(item.priceVnd),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = FashColors.Primary,
        )
    }
}

@Composable
private fun BottomActionBar(
    priceVnd: Long,
    onChat: () -> Unit,
    onBuyNow: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(FashTheme.spacing.spacing4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f).widthIn(min = 90.dp)) {
            Text(
                text = stringResource(R.string.product_total_payment),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatPrice(priceVnd),
                style = FashTheme.textStyles.price,
                color = FashColors.Primary,
                maxLines = 1,
                softWrap = false,
            )
        }
        OutlinedButton(
            onClick = onChat,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                contentColor = FashColors.Primary,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.Message,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.product_chat))
        }
        Button(
            onClick = onBuyNow,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = FashColors.Primary,
                contentColor = FashColors.OnPrimary,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.LocalMall,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.product_buy_now))
        }
    }
}

private fun resolveImageUrl(path: String): String {
    if (path.startsWith("http")) return path
    val base = AppEnvironment.apiBaseUrl.trimEnd('/')
    return if (path.startsWith("/")) "$base$path" else "$base/$path".takeIf { path.isNotBlank() } ?: ""
}

private fun formatPrice(vnd: Long): String =
    "đ ${"%,d".format(vnd).replace(',', '.')}"

private fun formatCondition(condition: String): String = when (condition.lowercase()) {
    "like_new", "like new", "như mới" -> "Như mới"
    "good", "tốt" -> "Tốt"
    "fair", "khá" -> "Khá"
    else -> condition.ifBlank { "—" }
}
