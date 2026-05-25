package com.pc.fash_android_mobile.ui.main.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.user.SellerFocusBrand
import com.pc.fash_android_mobile.data.user.SellerFocusCategory
import com.pc.fash_android_mobile.data.user.SellerFocusTag
import com.pc.fash_android_mobile.data.user.SellerListingFocus
import com.pc.fash_android_mobile.ui.components.FashPromoSlideDef
import com.pc.fash_android_mobile.ui.components.FashPromoSliderAdFooter
import com.pc.fash_android_mobile.ui.components.FashPromoSliderAdFooterContentHeight
import com.pc.fash_android_mobile.ui.guest.GuestLoginReason
import com.pc.fash_android_mobile.ui.profile.ProfileShare
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: SellerProfileViewModel,
    /** Must match `GET …/api/v1/users/{username}` — shown in the top bar as @handle. */
    sellerUsername: String,
    onBack: () -> Unit,
    onListingClick: (listingId: String, sellerId: String?) -> Unit = { _, _ -> },
    /**
     * Category / brand / aesthetic chips (and header aesthetic tags).
     * Host should call [com.pc.fash_android_mobile.ui.explore.ExploreViewModel.openExploreFromProfileFilter],
     * switch to Explore, and dismiss the seller overlay (same as main profile → Explore).
     */
    onNavigateToExploreFromProfile: (
        categoryId: String?,
        brandId: String?,
        aestheticTagId: String?,
        searchQuery: String,
        countryId: String?,
        countryIso2: String?,
    ) -> Unit = { _, _, _, _, _, _ -> },
    /** Same default promo deck as Orders / Explore; tap usually opens Explore. */
    onPromoSlideClick: (FashPromoSlideDef, Int) -> Unit = { _, _ -> },
    promoSlides: List<FashPromoSlideDef> = emptyList(),
    isGuestMode: Boolean = false,
    onRequestLogin: (GuestLoginReason) -> Unit = {},
) {
    val profile by viewModel.profile.collectAsState()
    val sellingListings by viewModel.sellingListings.collectAsState()
    val soldListings by viewModel.soldListings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val isFollowing by viewModel.isFollowing.collectAsState()
    val followInFlight by viewModel.followInFlight.collectAsState()
    val sellerFocus by viewModel.sellerFocus.collectAsState()
    val sellerFocusForbidden by viewModel.sellerFocusForbidden.collectAsState()
    val sellerFocusLoading by viewModel.sellerFocusLoading.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    val titleHandle = remember(sellerUsername) {
        sellerUsername.trim().removePrefix("@")
    }
    val shareContext = LocalContext.current

    LaunchedEffect(sellerUsername) {
        viewModel.loadForSeller(sellerUsername)
    }

    val listState = rememberLazyListState()
    val scrollScope = rememberCoroutineScope()
    val collapseProgress = rememberProfileHeaderCollapseProgress(listState)
    val showPromoFooter by rememberProfilePromoFooterVisible(listState)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        // Match NotificationScreen: bottom inset handled inside edge-to-edge ad strip.
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
        ),
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
                actions = {
                    if (titleHandle.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val displayName = profile?.displayName?.trim()?.takeIf { it.isNotEmpty() }
                                ProfileShare.launch(
                                    context = shareContext,
                                    username = titleHandle,
                                    displayName = displayName,
                                )
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.profile_action_share),
                                tint = FashColors.Primary,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { paddingValues ->
        val scheme = MaterialTheme.colorScheme
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.background),
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
                    val pinnedBottomInset = FashPromoSliderAdFooterContentHeight
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            ProfileCollapsingScrollLayout(
                                listState = listState,
                                expandedHeader = {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        SellerProfileHeader(
                                            profile = profile,
                                            onAestheticTagClick = { _, id ->
                                                onNavigateToExploreFromProfile(null, null, id, "", null, null)
                                            },
                                        )
                                        if (profile != null && viewModel.canShowFollowUi()) {
                                            SellerProfileFollowBlock(
                                                isFollowing = isFollowing && !isGuestMode,
                                                inFlight = followInFlight,
                                                onToggle = {
                                                    if (isGuestMode) {
                                                        onRequestLogin(GuestLoginReason.Follow)
                                                    } else {
                                                        viewModel.toggleFollow()
                                                    }
                                                },
                                            )
                                        }
                                        SellerProfileMetricsCard(profile = profile)
                                        SellerProfileBodyMeasurements(profile = profile)
                                        SellerProfileTopBadges(profile = profile)
                                        SellerListingFocusSection(
                                            focus = sellerFocus,
                                            forbidden = sellerFocusForbidden,
                                            loading = sellerFocusLoading,
                                            onCategoryClick = { categoryId, _ ->
                                                onNavigateToExploreFromProfile(categoryId, null, null, "", null, null)
                                            },
                                            onBrandClick = { brandId, _ ->
                                                onNavigateToExploreFromProfile(null, brandId, null, "", null, null)
                                            },
                                            onAestheticTagClick = { tagId, _ ->
                                                onNavigateToExploreFromProfile(null, null, tagId, "", null, null)
                                            },
                                        )
                                    }
                                },
                                compactHeader = {
                                    ProfileCompactHeaderBar(
                                        profile = profile,
                                        onClick = {
                                            scrollScope.launch {
                                                listState.animateScrollToItem(0)
                                            }
                                        },
                                    )
                                },
                                selectedTab = selectedTab,
                                onTabSelected = { newTab ->
                                    if (newTab != selectedTab) {
                                        selectedTab = newTab
                                        scrollScope.launch {
                                            listState.scrollProfileToPinnedGrid(initialDelayMs = 80)
                                        }
                                    }
                                },
                                items = items,
                                listingTabSet = ProfileListingTabSet.SellerStorefront,
                                onListingClick = { item ->
                                    onListingClick(item.id, item.sellerId ?: profile?.userId)
                                },
                                showListingQuickActions = true,
                                onListingLike = {
                                    if (isGuestMode) onRequestLogin(GuestLoginReason.Like) else viewModel.toggleLike(it)
                                },
                                onListingSave = {
                                    if (isGuestMode) onRequestLogin(GuestLoginReason.Saved) else viewModel.toggleSave(it)
                                },
                                additionalBottomInset = if (showPromoFooter) pinnedBottomInset else 0.dp,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        AnimatedVisibility(
                            visible = showPromoFooter,
                            enter = fadeIn(animationSpec = tween(240)) +
                                slideInVertically(animationSpec = tween(240)) { full -> full / 3 },
                            exit = fadeOut(animationSpec = tween(200)) +
                                slideOutVertically(animationSpec = tween(200)) { full -> full / 4 },
                        ) {
                            FashPromoSliderAdFooter(
                                modifier = Modifier.fillMaxWidth(),
                                slides = promoSlides,
                                onSlideClick = onPromoSlideClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SellerListingFocusSection(
    focus: SellerListingFocus?,
    forbidden: Boolean,
    loading: Boolean,
    onCategoryClick: (categoryId: String, label: String) -> Unit,
    onBrandClick: (brandId: String, name: String) -> Unit,
    onAestheticTagClick: (tagId: String, name: String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as com.pc.fash_android_mobile.FashApplication
    val catalog by app.aestheticTagCatalog.collectAsState()
    val showContent = focus != null && !focus.isEmpty()
    if (!forbidden && !loading && !showContent) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .padding(top = 4.dp, bottom = 8.dp),
    ) {
        HorizontalDivider(
            modifier = Modifier.padding(bottom = 12.dp),
            color = scheme.outlineVariant.copy(alpha = 0.35f),
        )
        Text(
            text = stringResource(R.string.seller_focus_section_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurface,
        )
        Text(
            text = stringResource(R.string.seller_focus_section_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        when {
            forbidden -> {
                Text(
                    text = stringResource(R.string.seller_focus_forbidden),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            loading && !showContent -> {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = FashColors.Primary,
                    trackColor = scheme.surfaceContainerHighest,
                )
            }
            showContent && focus != null -> {
                SellerFocusCategoryRow(
                    label = stringResource(R.string.seller_focus_categories),
                    categories = focus.categories,
                    onCategoryClick = onCategoryClick,
                )
                Spacer(modifier = Modifier.height(12.dp))
                SellerFocusBrandRow(
                    label = stringResource(R.string.seller_focus_brands),
                    brands = focus.brands,
                    onBrandClick = onBrandClick,
                )
                Spacer(modifier = Modifier.height(12.dp))
                SellerFocusAestheticRow(
                    label = stringResource(R.string.seller_focus_aesthetics),
                    tags = focus.aestheticTags,
                    catalog = catalog,
                    onAestheticTagClick = onAestheticTagClick,
                )
            }
        }
    }
}

@Composable
private fun SellerFocusCategoryRow(
    label: String,
    categories: List<SellerFocusCategory>,
    onCategoryClick: (categoryId: String, label: String) -> Unit,
) {
    if (categories.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(
                items = categories,
                key = { i, c -> "${label}_cat_${c.id}_$i" },
            ) { _, c ->
                val text = c.displayLabel()
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(FashTheme.spacing.radiusPill))
                        .background(scheme.surfaceContainerLow)
                        .clickable(onClick = { onCategoryClick(c.id, text) })
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SellerFocusBrandRow(
    label: String,
    brands: List<SellerFocusBrand>,
    onBrandClick: (brandId: String, name: String) -> Unit,
) {
    if (brands.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(
                items = brands,
                key = { i, b -> "${label}_brand_${b.id}_$i" },
            ) { _, b ->
                Text(
                    text = b.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(FashTheme.spacing.radiusPill))
                        .background(scheme.surfaceContainerLow)
                        .clickable(onClick = { onBrandClick(b.id, b.name) })
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SellerFocusAestheticRow(
    label: String,
    tags: List<SellerFocusTag>,
    catalog: List<com.pc.fash_android_mobile.data.common.CommonAestheticTagDto>,
    onAestheticTagClick: (tagId: String, name: String) -> Unit,
) {
    if (tags.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val context = androidx.compose.ui.platform.LocalContext.current
    val isVi = com.pc.fash_android_mobile.data.locale.AppLocale.currentTag(context) != com.pc.fash_android_mobile.data.locale.AppLocale.TAG_EN
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(
                items = tags,
                key = { i, t -> "${label}_aes_${t.id}_$i" },
            ) { _, t ->
                val display = com.pc.fash_android_mobile.data.common.resolveAestheticLabel(catalog, t.id, t.name, isVi)
                Text(
                    text = display,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(FashTheme.spacing.radiusPill))
                        .background(scheme.surfaceContainerLow)
                        .clickable(onClick = { onAestheticTagClick(t.id, t.name) })
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/** Follow CTA or “đang theo dõi” status — placed above shop metrics on seller storefront. */
@Composable
private fun SellerProfileFollowBlock(
    isFollowing: Boolean,
    inFlight: Boolean,
    onToggle: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .padding(top = 4.dp, bottom = 4.dp),
    ) {
        if (isFollowing) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = scheme.surfaceContainerLow,
                border = BorderStroke(1.dp, FashColors.Primary.copy(alpha = 0.22f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = FashColors.Primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.seller_following_status_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = scheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.seller_following_status_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                        )
                    }
                    TextButton(
                        onClick = onToggle,
                        enabled = !inFlight,
                    ) {
                        if (inFlight) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = FashColors.Primary,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.unfollow_action),
                                style = MaterialTheme.typography.labelLarge,
                                color = FashColors.Primary,
                            )
                        }
                    }
                }
            }
        } else {
            Button(
                onClick = onToggle,
                enabled = !inFlight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = Color.White.copy(alpha = 0.7f),
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(FashColors.Primary, FashColors.Primary.copy(alpha = 0.88f)),
                            ),
                            RoundedCornerShape(14.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (inFlight) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.follow_button),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.profile_seller_follow_cta_hint),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}

/** Stats + trust line in one card — storefront only (no tappable follower pulses). */
@Composable
private fun SellerProfileMetricsCard(profile: ProfileInfo?) {
    val p = profile ?: return
    val scheme = MaterialTheme.colorScheme
    val ratingVal = p.rating
    val reviewCount = p.reviewCount
    val hasRatingScore = ratingVal != null && ratingVal > 0f
    val productCount = p.productCount ?: 0
    val soldCount = p.soldCount ?: 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .padding(top = 4.dp, bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = scheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SellerProfileStatCell(
                    modifier = Modifier.weight(1f),
                    value = sellerFormatCount(p.followerCount),
                    label = stringResource(R.string.profile_followers),
                    emphasize = false,
                )
                SellerProfileStatDivider()
                SellerProfileStatCell(
                    modifier = Modifier.weight(1f),
                    value = p.followingCount.toString(),
                    label = stringResource(R.string.profile_following),
                    emphasize = false,
                )
                SellerProfileStatDivider()
                SellerProfileStatCell(
                    modifier = Modifier.weight(1f),
                    value = productCount.toString(),
                    label = stringResource(R.string.profile_products),
                    emphasize = productCount > 0,
                )
                SellerProfileStatDivider()
                SellerProfileStatCell(
                    modifier = Modifier.weight(1f),
                    value = soldCount.toString(),
                    label = stringResource(R.string.profile_sold),
                    emphasize = soldCount > 0,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp),
                color = scheme.outlineVariant.copy(alpha = 0.35f),
            )
            SellerProfileTrustLine(
                hasRatingScore = hasRatingScore,
                ratingVal = ratingVal,
                reviewCount = reviewCount,
                hasShop = productCount > 0,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun SellerProfileBodyMeasurements(profile: ProfileInfo?) {
    val p = profile ?: return
    val height = p.heightCm
    val weight = p.weightKg
    if (height == null && weight == null) return
    val scheme = MaterialTheme.colorScheme
    val parts = buildList {
        height?.let { add(stringResource(R.string.profile_height_cm, it)) }
        weight?.let { w -> add(stringResource(R.string.profile_weight_kg, String.format(Locale.US, "%.1f", w))) }
    }
    Text(
        text = parts.joinToString(" · "),
        style = MaterialTheme.typography.bodySmall,
        color = scheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .padding(bottom = 8.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SellerProfileTopBadges(profile: ProfileInfo?) {
    val badges = profile?.topBadges.orEmpty().filter { it.name.isNotBlank() || it.emoji.isNotBlank() }
    if (badges.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        badges.forEach { badge ->
            Surface(
                shape = RoundedCornerShape(FashTheme.spacing.radiusPill),
                color = scheme.surfaceContainerLow,
            ) {
                Text(
                    text = buildString {
                        if (badge.emoji.isNotBlank()) {
                            append(badge.emoji)
                            append(' ')
                        }
                        append(badge.name.ifBlank { badge.slug })
                        if (badge.count > 1) {
                            append(" ×")
                            append(badge.count)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun SellerProfileStatDivider() {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .height(36.dp)
            .width(1.dp)
            .background(scheme.outlineVariant.copy(alpha = 0.4f)),
    )
}

@Composable
private fun SellerProfileStatCell(
    value: String,
    label: String,
    emphasize: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = if (emphasize) FashColors.Primary else scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            modifier = Modifier.heightIn(min = 28.dp),
        )
    }
}

@Composable
private fun SellerProfileTrustLine(
    hasRatingScore: Boolean,
    ratingVal: Float?,
    reviewCount: Int?,
    hasShop: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (hasRatingScore) Icons.Filled.Star else Icons.Outlined.StarOutline,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (hasRatingScore) FashColors.Primary else scheme.onSurfaceVariant.copy(alpha = 0.55f),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            when {
                hasRatingScore && ratingVal != null -> {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f", ratingVal),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
                    )
                    val sub = when {
                        reviewCount != null && reviewCount >= 0 ->
                            stringResource(R.string.profile_seller_trust_reviews_count, reviewCount)
                        else -> stringResource(R.string.profile_seller_trust_subtitle_score_only)
                    }
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                hasShop -> {
                    Text(
                        text = stringResource(R.string.profile_seller_rating_pending),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = scheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.profile_seller_rating_pending_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                    )
                }
                else -> {
                    Text(
                        text = stringResource(R.string.profile_seller_rating_pending),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun sellerFormatCount(count: Int): String =
    when {
        count >= 1_000_000 -> "${count / 1_000_000}.${(count % 1_000_000) / 100_000}M"
        count >= 1_000 -> "${count / 1_000}.${(count % 1_000) / 100}k"
        else -> count.toString()
    }
