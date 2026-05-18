package com.pc.fash_android_mobile.ui.main.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.user.SellerFocusBrand
import com.pc.fash_android_mobile.data.user.SellerFocusCategory
import com.pc.fash_android_mobile.data.user.SellerFocusTag
import com.pc.fash_android_mobile.data.user.SellerListingFocus
import com.pc.fash_android_mobile.ui.components.FashPromoSlideDef
import com.pc.fash_android_mobile.ui.components.FashPromoSliderAdFooter
import com.pc.fash_android_mobile.ui.components.FashPromoSliderAdFooterContentHeight
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
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
    /** Bottom strip below the promo slider — same as Orders / Notifications. */
    onExploreClick: () -> Unit = {},
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

    LaunchedEffect(sellerUsername) {
        viewModel.loadForSeller(sellerUsername)
    }

    val listState = remember(selectedTab) { LazyListState(0, 0) }
    val scrollScope = rememberCoroutineScope()
    val collapseProgress = rememberProfileHeaderCollapseProgress(listState)
    val showPromoFooter by rememberProfilePromoFooterVisible(listState)

    LaunchedEffect(selectedTab) {
        listState.scrollToItem(0, 0)
    }

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
                                            onAestheticTagClick = { name, id ->
                                                onNavigateToExploreFromProfile(null, null, id, name, null, null)
                                            },
                                        )
                                        ProfileTrustCard(profile = profile)
                                        ProfileStats(profile = profile)
                                        if (profile != null && viewModel.canFollowSeller()) {
                                            SellerFollowRow(
                                                isFollowing = isFollowing,
                                                inFlight = followInFlight,
                                                onToggle = { viewModel.toggleFollow() },
                                            )
                                        }
                                        SellerListingFocusSection(
                                            focus = sellerFocus,
                                            forbidden = sellerFocusForbidden,
                                            loading = sellerFocusLoading,
                                            onCategoryClick = { categoryId, label ->
                                                onNavigateToExploreFromProfile(categoryId, null, null, label, null, null)
                                            },
                                            onBrandClick = { brandId, name ->
                                                onNavigateToExploreFromProfile(null, brandId, null, name, null, null)
                                            },
                                            onAestheticTagClick = { tagId, name ->
                                                onNavigateToExploreFromProfile(null, null, tagId, name, null, null)
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
                                onTabSelected = { selectedTab = it },
                                items = items,
                                wishlistTabVisible = false,
                                onListingClick = { item ->
                                    onListingClick(item.id, item.sellerId ?: profile?.userId)
                                },
                                showListingQuickActions = true,
                                onListingLike = { viewModel.toggleLike(it) },
                                onListingSave = { viewModel.toggleSave(it) },
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
                                onExploreClick = onExploreClick,
                                slides = promoSlides,
                                onSlideClick = onPromoSlideClick,
                                edgeToEdgeAdStrip = true,
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
    onAestheticTagClick: (tagId: String, name: String) -> Unit,
) {
    if (tags.isEmpty()) return
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
                items = tags,
                key = { i, t -> "${label}_aes_${t.id}_$i" },
            ) { _, t ->
                Text(
                    text = t.name,
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = scheme.surfaceContainerLow,
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
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.seller_following_status_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = scheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.seller_following_status_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
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
