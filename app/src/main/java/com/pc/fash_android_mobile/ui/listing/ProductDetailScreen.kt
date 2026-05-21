@file:OptIn(ExperimentalMaterial3Api::class)

package com.pc.fash_android_mobile.ui.listing

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Storefront
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.json.JSONObject
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.config.BusinessFlowConfig
import com.pc.fash_android_mobile.data.listing.ListingDetail
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.ui.feed.ListingGridCard
import com.pc.fash_android_mobile.ui.guest.GuestLoginReason
import com.pc.fash_android_mobile.data.listing.ListingShippingAddress
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.ui.commerce.DealAgreedPriceBanner
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.components.FashProfileAvatarImage
import com.pc.fash_android_mobile.ui.feed.formatListingPriceVnd
import com.pc.fash_android_mobile.ui.feed.resolveListingImageUrl
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

/** Brand accent on PDP; aligned with [FashColors.Primary]. */
private val DetailPrimary = FashColors.Primary
private val DetailCardShape = RoundedCornerShape(16.dp)
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
    /**
     * When false, category/brand/tag taps on this screen do not apply [ExploreViewModel] filters
     * (e.g. while a seller shop overlay is shown above PDP, or briefly after it closes to avoid
     * duplicate pointer handling updating Explore state).
     */
    profileExploreNavigationEnabled: Boolean = true,
    /** Same contract as profile / seller shop: opens Explore with filters + optional text search + country. */
    onNavigateToExploreFromProfile: (
        categoryId: String?,
        brandId: String?,
        aestheticTagId: String?,
        searchQuery: String,
        countryId: String?,
        countryIso2: String?,
    ) -> Unit = { _, _, _, _, _, _ -> },
    isGuestMode: Boolean = false,
    onRequestLogin: (GuestLoginReason) -> Unit = {},
) {
    val onExploreFromProfile: (
        String?,
        String?,
        String?,
        String,
        String?,
        String?,
    ) -> Unit = { c, b, a, q, cid, iso ->
        if (profileExploreNavigationEnabled) onNavigateToExploreFromProfile(c, b, a, q, cid, iso)
    }
    val detail by viewModel.detail.collectAsState()
    val sellerProfile by viewModel.sellerProfile.collectAsState()
    val moreFromSeller by viewModel.moreFromSeller.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val isOpeningChat by viewModel.isOpeningChat.collectAsState()
    val bottomBarMode by viewModel.bottomBarMode.collectAsState()
    val buyerActiveOrder by viewModel.buyerActiveOrder.collectAsState()
    val showPurchaseGuide by viewModel.showPurchaseGuide.collectAsState()
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(listingId) {
        viewModel.loadDetail(listingId)
    }

    val buyNowEnabled = BusinessFlowConfig.c2cBuyNowEnabled

    if (showPurchaseGuide) {
        ProductPurchaseGuideDialog(
            buyNowEnabled = buyNowEnabled,
            onDismiss = { viewModel.dismissPurchaseGuide() },
        )
    }


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
                val scrollState = rememberScrollState()
                val density = LocalDensity.current
                val configuration = LocalConfiguration.current
                // Hero is square (aspect 1:1); seller row sits directly below it.
                val heroHeightPx = remember(configuration.screenWidthDp, density) {
                    with(density) { configuration.screenWidthDp.dp.toPx() }
                }
                val topBarPx = remember(density) { with(density) { 56.dp.toPx() } }
                val defaultSellerRowPx = remember(density) { with(density) { 88.dp.toPx() } }
                var measuredSellerRowHeightPx by remember { mutableStateOf<Float?>(null) }
                val sellerRowHeightPx = measuredSellerRowHeightPx ?: defaultSellerRowPx
                // Pinned strip appears only after the in-content seller row has scrolled off above the area below the top bar.
                val pinnedRevealScrollPx = remember(heroHeightPx, sellerRowHeightPx, topBarPx) {
                    (heroHeightPx + sellerRowHeightPx - topBarPx).coerceAtLeast(0f)
                }
                val hysteresisPx = remember(density) { with(density) { 32.dp.toPx() } }
                var showPinnedSellerStrip by remember { mutableStateOf(false) }
                // Read scroll every frame so this block recomposes while scrolling (SideEffect alone missed updates).
                val scrollY = scrollState.value
                LaunchedEffect(scrollY) {
                    val y = scrollY.toFloat()
                    when {
                        y > pinnedRevealScrollPx + 8f -> showPinnedSellerStrip = true
                        y < pinnedRevealScrollPx - hysteresisPx -> showPinnedSellerStrip = false
                    }
                }
                val scrollScope = rememberCoroutineScope()
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .weight(1f)
                                .verticalScroll(scrollState),
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .background(scheme.background),
                            ) {
                                DetailHeroImage(
                                    detail = d,
                                    onLike = {
                                        if (isGuestMode) onRequestLogin(GuestLoginReason.Like)
                                        else viewModel.toggleLike()
                                    },
                                    onSave = {
                                        if (isGuestMode) onRequestLogin(GuestLoginReason.Saved)
                                        else viewModel.toggleSave()
                                    },
                                )
                                Box(
                                    Modifier.onGloballyPositioned { coords ->
                                        measuredSellerRowHeightPx = coords.size.height.toFloat()
                                    },
                                ) {
                                    DetailSellerCard(
                                        detail = d,
                                        profile = sellerProfile,
                                        onVisitShop = onVisitSellerShop,
                                    )
                                }
                                DetailPriceInfoCard(
                                    detail = d,
                                    onNavigateToExplore = onExploreFromProfile,
                                )
                                buyerActiveOrder?.let { order ->
                                    if (order.amountVnd >= 1000L) {
                                        DealAgreedPriceBanner(
                                            amountVnd = order.amountVnd,
                                            fromBuyNow = order.status.equals("payment_pending", ignoreCase = true),
                                            modifier = Modifier.padding(
                                                horizontal = FashTheme.spacing.editorialStart,
                                                vertical = 8.dp,
                                            ),
                                        )
                                    }
                                }
                                DetailAtGlanceCard(
                                    detail = d,
                                    onNavigateToExplore = onExploreFromProfile,
                                )
                                if (detailHasMeasurements(d)) {
                                    DetailMeasurementsCard(detail = d)
                                }
                                DetailShippingSectionCard(detail = d)
                                DetailAboutCard(
                                    detail = d,
                                    onNavigateToExplore = onExploreFromProfile,
                                )
                                if (moreFromSeller.isNotEmpty()) {
                                    DetailMoreFromSellerSection(
                                        username = d.sellerUsername,
                                        items = moreFromSeller,
                                        excludeId = d.id,
                                        onItemClick = onListingClick,
                                        onLike = { item ->
                                            if (isGuestMode) onRequestLogin(GuestLoginReason.Like)
                                            else viewModel.toggleLikeMoreFromSeller(item)
                                        },
                                        onSave = { item ->
                                            if (isGuestMode) onRequestLogin(GuestLoginReason.Saved)
                                            else viewModel.toggleSaveMoreFromSeller(item)
                                        },
                                    )
                                }
                                Spacer(Modifier.height(96.dp))
                            }
                        }
                        DetailBottomBar(
                            mode = bottomBarMode,
                            chatLoading = isOpeningChat,
                            buyNowEnabled = buyNowEnabled,
                            buyerOrderAmountVnd = buyerActiveOrder?.amountVnd ?: 0L,
                            onChat = { onChat(d.id) },
                            onBuyNow = { onBuyNow(d.id) },
                        )
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .zIndex(1f),
                    ) {
                        DetailTopBar(
                            onBack = onBack,
                            onShare = { onShare(d.id, d.title) },
                        )
                        AnimatedVisibility(
                            visible = showPinnedSellerStrip,
                            enter = fadeIn(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            ) +
                                slideInVertically(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                    initialOffsetY = { -it / 5 },
                                ),
                            exit = fadeOut(animationSpec = tween(180)) +
                                slideOutVertically(
                                    animationSpec = tween(180),
                                    targetOffsetY = { -it / 6 },
                                ),
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = scheme.outlineVariant.copy(alpha = 0.45f),
                                )
                                Surface(
                                    color = scheme.surface,
                                    tonalElevation = 0.dp,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    DetailSellerRowMini(
                                        detail = d,
                                        profile = sellerProfile,
                                        onVisitShop = onVisitSellerShop,
                                        onBriefClick = {
                                            scrollScope.launch {
                                                val target =
                                                    heroHeightPx.toInt().coerceIn(0, scrollState.maxValue)
                                                val start = scrollState.value
                                                val end = target
                                                if (start == end) return@launch
                                                val durationMs = 320
                                                val steps = 28
                                                for (i in 1..steps) {
                                                    val t = i / steps.toFloat()
                                                    val eased = FastOutSlowInEasing.transform(t)
                                                    val v = (start + (end - start) * eased).toInt()
                                                    scrollState.scrollTo(v)
                                                    delay((durationMs / steps).toLong())
                                                }
                                                scrollState.scrollTo(end)
                                            }
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                    }
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
private fun Modifier.detailSectionOuter(): Modifier = this
    .fillMaxWidth()
    .padding(horizontal = FashTheme.spacing.editorialStart)
    .padding(bottom = 10.dp)

@Composable
private fun DetailSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = DetailCardShape,
        color = scheme.surfaceContainerLow,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            content = content,
        )
    }
}

@Composable
private fun DetailSectionTitle(
    title: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = DetailPrimary,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurface,
        )
    }
}

@Composable
private fun DetailSellerRowMini(
    detail: ListingDetail,
    profile: ProfileInfo?,
    onVisitShop: (sellerUsername: String) -> Unit,
    modifier: Modifier = Modifier,
    /** Tap avatar + text to scroll the in-page seller card back into view (sticky bar). */
    onBriefClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val shopUsername = detail.sellerUsername?.takeIf { it.isNotBlank() }
    val avatarForUi = resolveSellerAvatarUrl(profile, detail)
    val name = profile?.displayName?.ifBlank { null }
        ?: detail.sellerDisplayName?.ifBlank { null }
        ?: detail.sellerUsername.orEmpty()
    val username = detail.sellerUsername ?: "user"
    val count = detail.sellerListingCount ?: profile?.productCount
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (onBriefClick != null) {
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onBriefClick)
                    } else {
                        Modifier
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(scheme.surfaceVariant),
            ) {
                FashProfileAvatarImage(
                    imageUrl = avatarForUi,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = name.ifBlank { "@$username" },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    ),
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
                    text = sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        OutlinedButton(
            onClick = { shopUsername?.let(onVisitShop) },
            enabled = shopUsername != null,
            border = BorderStroke(1.dp, DetailPrimary.copy(alpha = 0.55f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DetailPrimary),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(
                stringResource(R.string.product_visit_shop),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
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
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 52.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(urls.size) { index ->
                        val selected = index == pageIdx
                        Box(
                            modifier = Modifier
                                .size(if (selected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) Color.White else Color.White.copy(alpha = 0.45f),
                                ),
                        )
                    }
                }
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = scheme.surface.copy(alpha = 0.94f),
            shadowElevation = 3.dp,
            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.25f)),
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
private fun DetailSellerCard(
    detail: ListingDetail,
    profile: ProfileInfo?,
    onVisitShop: (sellerUsername: String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shopUsername = detail.sellerUsername?.takeIf { it.isNotBlank() }
    val avatarForUi = resolveSellerAvatarUrl(profile, detail)
    val name = profile?.displayName?.ifBlank { null }
        ?: detail.sellerDisplayName?.ifBlank { null }
        ?: detail.sellerUsername.orEmpty()
    val username = detail.sellerUsername ?: "user"
    val count = detail.sellerListingCount ?: profile?.productCount
    Column(Modifier.detailSectionOuter().padding(top = 10.dp)) {
        DetailSectionTitle(
            title = stringResource(R.string.product_section_seller),
            icon = Icons.Outlined.Storefront,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        DetailSectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(scheme.surfaceContainerHighest),
                ) {
                    FashProfileAvatarImage(
                        imageUrl = avatarForUi,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
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
                    border = BorderStroke(1.dp, DetailPrimary.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DetailPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        stringResource(R.string.product_visit_shop),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
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
            DetailCategoryChip(
                label = parent,
                onClick = { onNavigateToExplore(parentId, null, null, "", null, null) },
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
            DetailCategoryChip(
                label = child,
                onClick = { onNavigateToExplore(childId, null, null, "", null, null) },
            )
        }
    }
}

@Composable
private fun DetailCategoryChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DetailPrimary.copy(alpha = 0.1f),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = DetailPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun DetailPriceInfoCard(
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
    val hasBread = detail.parentCategoryName?.isNotBlank() == true ||
        detail.category?.isNotBlank() == true
    Column(Modifier.detailSectionOuter()) {
        DetailSectionCard {
            if (hasBread) {
                DetailCategoryBreadcrumb(
                    detail = detail,
                    onNavigateToExplore = onNavigateToExplore,
                )
                Spacer(Modifier.height(12.dp))
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    formatPriceVnd(detail.priceVnd),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
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
            Spacer(Modifier.height(10.dp))
            Text(
                detail.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 26.sp,
                ),
                color = scheme.onSurface,
            )
            detail.createdAtIso?.takeIf { it.isNotBlank() }?.let { iso ->
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.product_listed_on, formatShortDate(iso)),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailAtGlanceCard(
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
            { onNavigateToExplore(null, detail.brandId, null, "", null, null) }
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
    Column(Modifier.detailSectionOuter()) {
        DetailSectionTitle(
            title = stringResource(R.string.product_section_at_a_glance),
            icon = Icons.Outlined.GridView,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        DetailSectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AttrCell(
                        Modifier.weight(1f).fillMaxHeight(),
                        Icons.Default.Storefront,
                        stringResource(R.string.product_brand),
                        brand,
                        valueColor = scheme.onSurface,
                        onValueClick = brandClick,
                    )
                    AttrCell(
                        Modifier.weight(1f).fillMaxHeight(),
                        Icons.Default.Public,
                        stringResource(R.string.product_spec_origin),
                        origin,
                        valueColor = scheme.onSurface,
                        onValueClick = originClick,
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AttrCell(
                        Modifier.weight(1f).fillMaxHeight(),
                        Icons.Default.Straighten,
                        stringResource(R.string.product_size_label_short),
                        sizeLine,
                        valueColor = scheme.onSurface,
                    )
                    AttrCell(
                        Modifier.weight(1f).fillMaxHeight(),
                        Icons.Outlined.CheckCircle,
                        stringResource(R.string.product_condition_label),
                        cond,
                        valueColor = conditionColor,
                    )
                }
            }
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
        color = scheme.surfaceContainerHighest.copy(alpha = 0.65f),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(icon, null, Modifier.size(18.dp), tint = DetailPrimary)
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp, max = 44.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp,
                    ),
                    color = if (clickable) FashColors.Primary else valueColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (clickable) {
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onValueClick!!)
                                    .padding(vertical = 2.dp, horizontal = 2.dp)
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun DetailMeasurementsCard(detail: ListingDetail) {
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
    Column(Modifier.detailSectionOuter()) {
        DetailSectionTitle(
            title = stringResource(R.string.product_section_measurements),
            icon = Icons.Default.Straighten,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        DetailSectionCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.surfaceContainerHighest.copy(alpha = 0.5f)),
            ) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    MeasCell(stringResource(R.string.product_meas_chest), chest)
                    VerticalDivider(Modifier.fillMaxHeight(), color = scheme.outlineVariant.copy(alpha = 0.5f))
                    MeasCell(stringResource(R.string.product_meas_length), len)
                }
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    MeasCell(stringResource(R.string.product_meas_shoulders), shoulder)
                    VerticalDivider(Modifier.fillMaxHeight(), color = scheme.outlineVariant.copy(alpha = 0.5f))
                    MeasCell(stringResource(R.string.product_meas_sleeve), sleeve)
                }
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
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
private fun DetailShippingSectionCard(detail: ListingDetail) {
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
    Column(Modifier.detailSectionOuter()) {
        DetailSectionTitle(
            title = stringResource(R.string.product_section_shipping),
            icon = Icons.Default.LocalShipping,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        DetailSectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DetailPrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.LocalShipping,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = DetailPrimary,
                    )
                }
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
}

@Composable
private fun DetailAboutCard(
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
    val extraTags = detail.tags.mapNotNull { normalizeTag(it) }
        .filter { it.lowercase(Locale.getDefault()) !in aestheticLower }
    val hasTags = detail.aestheticTagRefs.isNotEmpty() || extraTags.isNotEmpty()
    if (detail.description.isBlank() && !hasTags) return
    Column(Modifier.detailSectionOuter()) {
        DetailSectionTitle(
            title = stringResource(R.string.product_section_about),
            icon = Icons.Outlined.Description,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        DetailSectionCard {
            if (detail.description.isNotBlank()) {
                Text(
                    detail.description,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = scheme.onSurface,
                )
            }
            if (hasTags) {
                if (detail.description.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.product_section_style),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    detail.aestheticTagRefs.forEach { ref ->
                        DetailDescriptionTagChip(
                            label = ref.label,
                            onClick = {
                                onNavigateToExplore(null, null, ref.id, "", null, null)
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
private fun DetailMoreFromSellerSection(
    username: String?,
    items: List<ListingFeedItem>,
    excludeId: String,
    onItemClick: (String, String?) -> Unit,
    onLike: (ListingFeedItem) -> Unit,
    onSave: (ListingFeedItem) -> Unit,
) {
    val u = username ?: "user"
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .padding(top = 4.dp, bottom = 12.dp),
    ) {
        DetailSectionTitle(
            title = stringResource(R.string.product_more_from_seller, "@$u"),
            icon = Icons.Outlined.Storefront,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items.filter { it.id != excludeId }.take(5).forEach { item ->
                ListingGridCard(
                    item = item,
                    modifier = Modifier.width(132.dp),
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
private fun ProductPurchaseGuideDialog(
    buyNowEnabled: Boolean,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Message,
                contentDescription = null,
                tint = FashColors.Primary,
            )
        },
        title = { Text(stringResource(R.string.product_purchase_guide_title)) },
        text = {
            Text(
                text = stringResource(
                    if (buyNowEnabled) {
                        R.string.product_purchase_guide_body
                    } else {
                        R.string.product_purchase_guide_body_no_buy_now
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.product_purchase_guide_got_it))
            }
        },
    )
}

@Composable
private fun DetailBottomBar(
    mode: ProductBottomBarMode,
    chatLoading: Boolean,
    buyNowEnabled: Boolean = true,
    buyerOrderAmountVnd: Long = 0L,
    onChat: () -> Unit,
    onBuyNow: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    when (mode) {
        ProductBottomBarMode.Normal -> {
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.35f))
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(scheme.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (buyNowEnabled) {
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
                        DetailChatButtonContent(chatLoading = chatLoading)
                    }
                    DetailPrimaryGradientButton(
                        onClick = onBuyNow,
                        enabled = true,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.LocalMall, null, Modifier.size(20.dp), tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.product_buy_now),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                    }
                } else {
                    DetailPrimaryGradientButton(
                        onClick = onChat,
                        enabled = !chatLoading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        DetailChatButtonContent(chatLoading = chatLoading, contentColor = Color.White)
                    }
                }
            }
        }
        ProductBottomBarMode.ReservedOther -> StatusBar(stringResource(R.string.product_reserved_other), Color(0xFFFFF8E1), Color(0xFFF57C00))
        ProductBottomBarMode.ReservedBuyer -> {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(scheme.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = if (buyerOrderAmountVnd >= 1000L) {
                        stringResource(
                            R.string.product_reserved_buyer_continue,
                            formatListingPriceVnd(buyerOrderAmountVnd),
                        )
                    } else {
                        stringResource(R.string.product_reserved_buyer)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (buyNowEnabled) {
                    Button(
                        onClick = onBuyNow,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FashColors.Primary),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            stringResource(R.string.product_continue_checkout),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }
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

private fun resolveImageUrl(path: String): String = resolveListingImageUrl(path)

/** Listing detail seller block + sticky mini bar — profile fetch, then listing snapshot. */
private fun resolveSellerAvatarUrl(profile: ProfileInfo?, detail: ListingDetail): String? {
    val raw = profile?.avatarUrl?.trim()?.takeIf { it.isNotEmpty() }
        ?: detail.sellerAvatarUrl?.trim()?.takeIf { it.isNotEmpty() }
        ?: return null
    return resolveListingImageUrl(raw).takeIf { it.isNotEmpty() }
}

@Composable
private fun RowScope.DetailChatButtonContent(
    chatLoading: Boolean,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    if (chatLoading) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = contentColor)
    } else {
        Icon(Icons.AutoMirrored.Filled.Message, null, Modifier.size(20.dp), tint = contentColor)
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.product_chat),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = contentColor,
        )
    }
}

@Composable
private fun DetailPrimaryGradientButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White.copy(alpha = 0.65f),
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
            Row(verticalAlignment = Alignment.CenterVertically, content = content)
        }
    }
}
