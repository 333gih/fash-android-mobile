@file:OptIn(ExperimentalMaterial3Api::class)

package com.pc.fash_android_mobile.ui.listing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.json.JSONObject
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.listing.ListingDetail
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.ui.feed.ListingGridCard
import com.pc.fash_android_mobile.data.listing.ListingShippingAddress
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

/** Brand accent on PDP; body/label text uses [ColorScheme.onSurface] / [ColorScheme.onSurfaceVariant]. */
private val DetailPrimary = Color(0xFFE9334A)
private val DetailConditionGreenLight = Color(0xFF2E7D32)
private val DetailConditionGreenDark = Color(0xFF81C784)
private const val DEFAULT_EST_SHIPPING_VND = 30_000L

@Composable
private fun detailConditionValueColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.surface.luminance() < 0.4f) DetailConditionGreenDark else DetailConditionGreenLight
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProductDetailScreen(
    modifier: Modifier = Modifier,
    listingId: String,
    viewModel: ProductDetailViewModel,
    onBack: () -> Unit,
    onChat: (String) -> Unit = {},
    onBuyNow: (String) -> Unit = {},
    /** Share sheet: [listingId] + [title] for message text. */
    onShare: (listingId: String, title: String) -> Unit = { _, _ -> },
    onListingClick: (listingId: String, sellerId: String?) -> Unit = { _, _ -> },
    /** Seller username only — passed to `GET …/api/v1/users/{username}`. */
    onVisitSellerShop: (sellerUsername: String) -> Unit = {},
    /** Same contract as profile / seller shop: opens Explore with filters + optional text search + country. */
    onNavigateToExploreFromProfile: (
        categoryId: String?,
        brandId: String?,
        aestheticTagId: String?,
        searchQuery: String,
        countryId: String?,
        countryIso2: String?,
    ) -> Unit = { _, _, _, _, _, _ -> },
) {
    val detail by viewModel.detail.collectAsState()
    val sellerProfile by viewModel.sellerProfile.collectAsState()
    val moreFromSeller by viewModel.moreFromSeller.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val isOpeningChat by viewModel.isOpeningChat.collectAsState()
    val bottomBarMode by viewModel.bottomBarMode.collectAsState()
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(listingId) {
        viewModel.loadDetail(listingId)
    }

    BackHandler(onBack = onBack)

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading && detail == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scheme.background)
                        .padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = FashColors.Primary)
                }
            }
            loadError != null && detail == null -> {
                val err = loadError!!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scheme.background)
                        .padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.retryLoad(listingId) }) {
                            Text(stringResource(R.string.feed_retry))
                        }
                    }
                }
            }
            detail != null -> {
                val d = requireNotNull(detail)
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .background(scheme.background),
                            ) {
                                DetailHeroImage(
                                    detail = d,
                                    onLike = { viewModel.toggleLike() },
                                    onSave = { viewModel.toggleSave() },
                                )
                                DetailSellerRow(
                                    detail = d,
                                    profile = sellerProfile,
                                    onVisitShop = onVisitSellerShop,
                                )
                                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.78f), thickness = 1.dp)
                                DetailBreadcrumbPriceTitle(
                                    detail = d,
                                    onNavigateToExplore = onNavigateToExploreFromProfile,
                                )
                                DetailAttributeGrid(
                                    detail = d,
                                    onNavigateToExplore = onNavigateToExploreFromProfile,
                                )
                                if (detailHasMeasurements(d)) {
                                    DetailMeasurementsHeader()
                                    DetailMeasurementsTable(detail = d)
                                }
                                DetailShippingCard(detail = d)
                                DetailDescriptionBlock(
                                    detail = d,
                                    onNavigateToExplore = onNavigateToExploreFromProfile,
                                )
                                if (moreFromSeller.isNotEmpty()) {
                                    DetailMoreFromSeller(
                                        username = d.sellerUsername,
                                        items = moreFromSeller,
                                        excludeId = d.id,
                                        onItemClick = onListingClick,
                                        onLike = { viewModel.toggleLikeMoreFromSeller(it) },
                                        onSave = { viewModel.toggleSaveMoreFromSeller(it) },
                                    )
                                }
                                Spacer(Modifier.height(96.dp))
                            }
                        }
                        DetailBottomBar(
                            mode = bottomBarMode,
                            chatLoading = isOpeningChat,
                            onChat = { onChat(d.id) },
                            onBuyNow = { onBuyNow(d.id) },
                        )
                    }
                    DetailTopBar(
                        onBack = onBack,
                        onShare = { onShare(d.id, d.title) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailTopBar(onBack: () -> Unit, onShare: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.product_detail_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.orders_back),
                    tint = DetailPrimary,
                )
            }
        },
        actions = {
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(R.string.product_action_share),
                    tint = DetailPrimary,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = scheme.surface,
            titleContentColor = scheme.onSurface,
            navigationIconContentColor = DetailPrimary,
            actionIconContentColor = DetailPrimary,
        ),
    )
}

@Composable
private fun DetailHeroImage(
    detail: ListingDetail,
    onLike: () -> Unit,
    onSave: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val urls = remember(detail.id, detail.imageUrls) {
        detail.imageUrls.map { resolveImageUrl(it) }.filter { it.isNotEmpty() }
    }
    val pagerState = rememberPagerState(pageCount = { maxOf(urls.size, 1) })
    val pageIdx = when {
        urls.isEmpty() -> 0
        urls.size <= 1 -> 0
        else -> pagerState.currentPage
    }
    val total = maxOf(urls.size, 1)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        when {
            urls.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(scheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.no_image),
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            urls.size == 1 -> {
                FashAsyncImage(
                    model = urls[0],
                    contentDescription = detail.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            else -> {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    FashAsyncImage(
                        model = urls[page],
                        contentDescription = detail.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = scheme.surfaceContainerHighest.copy(alpha = 0.92f),
            shadowElevation = 2.dp,
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                StatMini(Icons.Default.Visibility, formatStat(detail.viewCount), null, null)
                StatMini(
                    imageVector = if (detail.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    value = formatStat(detail.likeCount),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onLike()
                    },
                    tint = if (detail.isLiked) DetailPrimary else scheme.onSurface,
                )
                StatMini(
                    imageVector = if (detail.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    value = formatStat(detail.saveCount),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onSave()
                    },
                    tint = if (detail.isSaved) DetailPrimary else scheme.onSurface,
                )
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.45f),
        ) {
            Text(
                "${pageIdx + 1}/$total",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun StatMini(
    imageVector: ImageVector,
    value: String,
    onClick: (() -> Unit)?,
    tint: Color?,
) {
    val scheme = MaterialTheme.colorScheme
    val mod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = mod,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = tint ?: scheme.onSurface,
        )
        Text(
            value,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurface,
        )
    }
}

@Composable
private fun DetailSellerRow(
    detail: ListingDetail,
    profile: ProfileInfo?,
    onVisitShop: (sellerUsername: String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shopUsername = detail.sellerUsername?.takeIf { it.isNotBlank() }
    val avatarUrl = resolveImageUrl(profile?.avatarUrl ?: detail.sellerAvatarUrl.orEmpty())
    val name = profile?.displayName?.ifBlank { null }
        ?: detail.sellerDisplayName?.ifBlank { null }
        ?: detail.sellerUsername.orEmpty()
    val username = detail.sellerUsername ?: "user"
    val count = detail.sellerListingCount ?: profile?.productCount
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerHighest)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(scheme.surfaceVariant),
        ) {
            if (avatarUrl.isNotEmpty()) {
                FashAsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name.ifBlank { "@$username" },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = buildString {
                count?.takeIf { it >= 0 }?.let {
                    append(stringResource(R.string.product_seller_products_count, it))
                    append(" • ")
                }
                append("@$username")
            }
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        OutlinedButton(
            onClick = { shopUsername?.let(onVisitShop) },
            enabled = shopUsername != null,
            border = BorderStroke(1.dp, DetailPrimary.copy(alpha = 0.55f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DetailPrimary),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(
                stringResource(R.string.product_visit_shop),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Composable
private fun DetailCategoryBreadcrumb(
    detail: ListingDetail,
    onNavigateToExplore: (
        categoryId: String?,
        brandId: String?,
        aestheticTagId: String?,
        searchQuery: String,
        countryId: String?,
        countryIso2: String?,
    ) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val parent = detail.parentCategoryName?.trim()?.takeIf { it.isNotEmpty() }
    val child = detail.category?.trim()?.takeIf { it.isNotEmpty() }
    val parentId = detail.parentCategoryId?.takeIf { it.isNotBlank() }
    val childId = detail.categoryId?.takeIf { it.isNotBlank() }
    if (parent == null && child == null) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (parent != null) {
            Text(
                text = parent.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = FashColors.Primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateToExplore(parentId, null, null, parent, null, null) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
            if (child != null) {
                Text(
                    text = "›",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        if (child != null) {
            Text(
                text = child.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = FashColors.Primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateToExplore(childId, null, null, child, null, null) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun DetailBreadcrumbPriceTitle(
    detail: ListingDetail,
    onNavigateToExplore: (
        categoryId: String?,
        brandId: String?,
        aestheticTagId: String?,
        searchQuery: String,
        countryId: String?,
        countryIso2: String?,
    ) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val listPrice = detail.listPriceVnd?.takeIf { it > detail.priceVnd }
    Column(
        Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerHighest)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        DetailCategoryBreadcrumb(
            detail = detail,
            onNavigateToExplore = onNavigateToExplore,
        )
        val hasBread = detail.parentCategoryName?.isNotBlank() == true ||
            detail.category?.isNotBlank() == true
        if (hasBread) {
            Spacer(Modifier.height(8.dp))
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                formatPriceVnd(detail.priceVnd),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = DetailPrimary,
            )
            listPrice?.let { orig ->
                Text(
                    formatPriceVnd(orig),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurfaceVariant,
                    textDecoration = TextDecoration.LineThrough,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            detail.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = scheme.onSurface,
        )
        detail.createdAtIso?.takeIf { it.isNotBlank() }?.let { iso ->
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.product_listed_on, formatShortDate(iso)),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailAttributeGrid(
    detail: ListingDetail,
    onNavigateToExplore: (
        categoryId: String?,
        brandId: String?,
        aestheticTagId: String?,
        searchQuery: String,
        countryId: String?,
        countryIso2: String?,
    ) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val conditionColor = detailConditionValueColor()
    val brand = detail.brand?.takeIf { it.isNotBlank() } ?: "—"
    val origin = originLine(detail)
    val sizeLine = formatSizeEu(detail)
    val cond = formatConditionUi(detail.condition)
    val brandClick =
        if (detail.brandId != null && brand != "—") {
            { onNavigateToExplore(null, detail.brandId, null, detail.brand?.trim().orEmpty(), null, null) }
        } else {
            null
        }
    val originQuery = detail.countryName?.trim()?.takeIf { it.isNotEmpty() }
        ?: detail.countryIso2?.trim()?.takeIf { it.isNotEmpty() }
    val countryIdWire = detail.countryId?.takeIf { it.isNotBlank() }
    val countryIsoWire = detail.countryIso2?.trim()?.uppercase(Locale.US)
        ?.takeIf { it.length == 2 && it.all { c -> c in 'A'..'Z' } }
    val originClick: (() -> Unit)? = when {
        countryIdWire != null || countryIsoWire != null ->
            { { onNavigateToExplore(null, null, null, "", countryIdWire, countryIsoWire) } }
        originQuery != null && origin != "—" ->
            { { onNavigateToExplore(null, null, null, originQuery, null, null) } }
        else -> null
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerHighest)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AttrCell(
                Modifier.weight(1f),
                Icons.Default.Storefront,
                stringResource(R.string.product_brand).uppercase(Locale.getDefault()),
                brand,
                valueColor = scheme.onSurface,
                onValueClick = brandClick,
            )
            AttrCell(
                Modifier.weight(1f),
                Icons.Default.Public,
                stringResource(R.string.product_spec_origin).uppercase(Locale.getDefault()),
                origin,
                valueColor = scheme.onSurface,
                onValueClick = originClick,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AttrCell(
                Modifier.weight(1f),
                Icons.Default.Straighten,
                stringResource(R.string.product_size_label_short).uppercase(Locale.getDefault()),
                sizeLine,
                valueColor = scheme.onSurface,
            )
            AttrCell(
                Modifier.weight(1f),
                Icons.Default.Straighten,
                stringResource(R.string.product_condition_label).uppercase(Locale.getDefault()),
                "● $cond",
                valueColor = conditionColor,
            )
        }
    }
}

@Composable
private fun AttrCell(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color,
    onValueClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val clickable = onValueClick != null && value != "—"
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = scheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, null, Modifier.size(18.dp), tint = DetailPrimary)
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (clickable) FashColors.Primary else valueColor,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = if (clickable) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onValueClick!!)
                        .padding(vertical = 2.dp, horizontal = 2.dp)
                } else {
                    Modifier
                },
            )
        }
    }
}

@Composable
private fun DetailMeasurementsHeader() {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerHighest)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("📖", style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.product_section_detailed_specs).uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = scheme.onSurface,
        )
    }
}

@Composable
private fun DetailMeasurementsTable(detail: ListingDetail) {
    val scheme = MaterialTheme.colorScheme
    val empty = stringResource(R.string.product_meas_empty)
    val mu = detail.measurementUnit?.trim()?.takeIf { it.isNotEmpty() } ?: "cm"
    fun fmt(v: Double?): String = v?.let { x ->
        val s = if (x % 1.0 == 0.0) x.toInt().toString() else x.toString()
        "$s $mu"
    } ?: empty
    val chest = fmt(detail.measurementChest)
    val len = fmt(detail.measurementLength)
    val shoulder = fmt(detail.measurementShoulders)
    val sleeve = fmt(detail.measurementSleeveLength)
    val hem = fmt(detail.measurementHem)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.surfaceContainerLow),
    ) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            MeasCell(stringResource(R.string.product_meas_chest), chest)
            VerticalDivider(Modifier.fillMaxHeight(), color = scheme.outlineVariant)
            MeasCell(stringResource(R.string.product_meas_length), len)
        }
        HorizontalDivider(color = scheme.outlineVariant, thickness = 1.dp)
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            MeasCell(stringResource(R.string.product_meas_shoulders), shoulder)
            VerticalDivider(Modifier.fillMaxHeight(), color = scheme.outlineVariant)
            MeasCell(stringResource(R.string.product_meas_sleeve), sleeve)
        }
        HorizontalDivider(color = scheme.outlineVariant, thickness = 1.dp)
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                stringResource(R.string.product_meas_hem),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                hem,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
            )
        }
    }
}

@Composable
private fun RowScope.MeasCell(label: String, value: String) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.weight(1f).padding(12.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurface,
        )
    }
}

@Composable
private fun DetailShippingCard(detail: ListingDetail) {
    val scheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    var showShippingInfo by remember { mutableStateOf(false) }
    val feeVnd = detail.estimatedShippingVnd ?: DEFAULT_EST_SHIPPING_VND
    val region = shipFromRegion(detail.shippingAddress)
    if (showShippingInfo) {
        AlertDialog(
            onDismissRequest = { showShippingInfo = false },
            confirmButton = {
                TextButton(onClick = { showShippingInfo = false }) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.product_shipping_info_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.product_shipping_info_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = scheme.surface,
        )
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = scheme.surfaceContainerHighest,
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.LocalShipping,
                null,
                Modifier.size(28.dp),
                tint = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.product_shipping_estimate, formatPriceVnd(feeVnd)),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.product_ship_from_upper, region),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    showShippingInfo = true
                },
            ) {
                Icon(
                    Icons.Default.Info,
                    stringResource(R.string.product_shipping_info_cd),
                    Modifier.size(22.dp),
                    tint = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailDescriptionBlock(
    detail: ListingDetail,
    onNavigateToExplore: (
        categoryId: String?,
        brandId: String?,
        aestheticTagId: String?,
        searchQuery: String,
        countryId: String?,
        countryIso2: String?,
    ) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val aestheticLower = detail.aestheticTagRefs.map { it.label.lowercase(Locale.getDefault()) }.toSet()
    Column(
        Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerHighest)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            stringResource(R.string.product_section_description).uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = scheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        if (detail.description.isNotBlank()) {
            Text(
                detail.description,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface,
            )
        }
        val extraTags = detail.tags.mapNotNull { normalizeTag(it) }
            .filter { it.lowercase(Locale.getDefault()) !in aestheticLower }
        if (detail.aestheticTagRefs.isNotEmpty() || extraTags.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                detail.aestheticTagRefs.forEach { ref ->
                    DetailDescriptionTagChip(
                        label = ref.label,
                        onClick = {
                            onNavigateToExplore(null, null, ref.id, ref.label, null, null)
                        },
                    )
                }
                extraTags.forEach { t ->
                    DetailDescriptionTagChip(
                        label = t,
                        onClick = { onNavigateToExplore(null, null, null, t, null, null) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailDescriptionTagChip(
    label: String,
    onClick: () -> Unit,
) {
    Text(
        text = "#$label",
        style = MaterialTheme.typography.labelSmall,
        color = FashColors.Primary,
        modifier = Modifier
            .clip(RoundedCornerShape(FashTheme.spacing.radiusPill))
            .background(FashColors.Primary.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun DetailMoreFromSeller(
    username: String?,
    items: List<ListingFeedItem>,
    excludeId: String,
    onItemClick: (String, String?) -> Unit,
    onLike: (ListingFeedItem) -> Unit,
    onSave: (ListingFeedItem) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val u = username ?: "user"
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            stringResource(R.string.product_more_from_seller, "@$u"),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = scheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items.filter { it.id != excludeId }.take(5).forEach { item ->
                ListingGridCard(
                    item = item,
                    modifier = Modifier.width(120.dp),
                    compactFooter = true,
                    showQuickActions = true,
                    onLike = { onLike(item) },
                    onSave = { onSave(item) },
                    onClick = { onItemClick(item.id, item.sellerId) },
                )
            }
        }
    }
}

@Composable
private fun DetailBottomBar(
    mode: ProductBottomBarMode,
    chatLoading: Boolean,
    onChat: () -> Unit,
    onBuyNow: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    when (mode) {
        ProductBottomBarMode.Normal -> {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(scheme.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onChat,
                    enabled = !chatLoading,
                    modifier = Modifier.weight(1f).height(52.dp),
                    border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = scheme.onSurface,
                        containerColor = scheme.surfaceContainerLow,
                        disabledContainerColor = scheme.surfaceContainerLow,
                        disabledContentColor = scheme.onSurfaceVariant,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (chatLoading) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = FashColors.Primary)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Message, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.product_chat),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
                Button(
                    onClick = onBuyNow,
                    modifier = Modifier.weight(1f).height(52.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(DetailPrimary, DetailPrimary.copy(alpha = 0.88f)),
                                ),
                                RoundedCornerShape(12.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalMall, null, Modifier.size(20.dp), tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.product_buy_now),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
        ProductBottomBarMode.ReservedOther -> StatusBar(stringResource(R.string.product_reserved_other), Color(0xFFFFF8E1), Color(0xFFF57C00))
        ProductBottomBarMode.ReservedBuyer -> StatusBar(stringResource(R.string.product_reserved_buyer), Color(0xFFE8F5E9), Color(0xFF2E7D32))
        ProductBottomBarMode.Sold -> StatusBar(
            stringResource(R.string.product_listing_sold_bar),
            scheme.surfaceContainerHighest,
            scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusBar(text: String, bg: Color, fg: Color) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .background(scheme.background)
            .navigationBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(bg)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}

private fun detailHasMeasurements(d: ListingDetail): Boolean =
    listOf(
        d.measurementHem,
        d.measurementChest,
        d.measurementLength,
        d.measurementShoulders,
        d.measurementSleeveLength,
    ).any { it != null }

private fun originLine(d: ListingDetail): String {
    val name = d.countryName?.trim()?.takeIf { it.isNotEmpty() }
    val iso = d.countryIso2?.trim()?.takeIf { it.isNotEmpty() }
    val flag = iso?.takeIf { it.length == 2 }?.let { countryIsoToFlag(it) }.orEmpty()
    return when {
        name != null && flag.isNotEmpty() -> "$name $flag"
        name != null -> name
        iso != null -> iso
        else -> "—"
    }
}

private fun countryIsoToFlag(iso: String): String {
    val upper = iso.uppercase(Locale.US)
    if (upper.length != 2) return ""
    val a = upper[0].code - 'A'.code + 0x1F1E6
    val b = upper[1].code - 'A'.code + 0x1F1E6
    if (a !in 0x1F1E6..0x1F1FF || b !in 0x1F1E6..0x1F1FF) return ""
    return String(Character.toChars(a)) + String(Character.toChars(b))
}

private fun formatSizeEu(d: ListingDetail): String {
    val s = d.size?.trim()?.takeIf { it.isNotEmpty() } ?: return "—"
    return "Size $s (EU)"
}

private fun formatConditionUi(raw: String): String = when (raw.lowercase()) {
    "like_new", "like new", "như mới" -> "Like new"
    else -> raw.ifBlank { "—" }
}

private fun shipFromRegion(a: ListingShippingAddress?): String {
    val cc = a?.countryCode?.trim()?.takeIf { it.isNotEmpty() }
    return when {
        cc == null -> "VIETNAM"
        cc.equals("VN", true) -> "VIETNAM"
        else -> cc.uppercase(Locale.US)
    }
}

private fun normalizeTag(raw: String): String? {
    val t = raw.trim()
    if (t.isEmpty()) return null
    if (!t.startsWith("{")) return t
    return try {
        JSONObject(t).optString("name", "").ifBlank { null }
    } catch (_: Exception) {
        null
    }
}

private fun formatStat(n: Int): String =
    if (n >= 1000) String.format(Locale.US, "%.1fk", n / 1000f) else n.toString()

private fun formatPriceVnd(vnd: Long): String =
    "₫ ${"%,d".format(vnd).replace(',', '.')}"

private fun formatShortDate(iso: String): String = try {
    val utc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val out = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    val d = utc.parse(iso.take(19)) ?: return iso
    out.format(d)
} catch (_: Exception) {
    iso
}

private fun resolveImageUrl(path: String): String {
    if (path.startsWith("http")) return path
    val base = AppEnvironment.apiBaseUrl.trimEnd('/')
    return if (path.startsWith("/")) "$base$path" else "$base/$path".takeIf { path.isNotBlank() } ?: ""
}
