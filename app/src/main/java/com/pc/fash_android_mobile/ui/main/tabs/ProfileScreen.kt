package com.pc.fash_android_mobile.ui.main.tabs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.ui.common.stableLazyKey
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.components.FashDefaultProfileAssets
import com.pc.fash_android_mobile.ui.components.FashProfileAvatarImage
import com.pc.fash_android_mobile.ui.feed.ListingGridCard
import com.pc.fash_android_mobile.ui.components.FashEmptyState
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

/** Cover height; avatar overlaps content below by [ProfileHeroAvatarOverlap]. */
private val ProfileHeroCoverHeight = 168.dp
private val ProfileHeroAvatarOverlap = 40.dp
private val ProfileHeroAvatarRingDp = 88.dp
private val ProfileHeroAvatarInnerDp = 80.dp

@Composable
private fun ProfileHeroSection(
    coverModel: Any,
    avatarUrl: String?,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ProfileHeroCoverHeight),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.surfaceContainerHigh),
        ) {
            FashAsyncImage(
                model = coverModel,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(80.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                scheme.scrim.copy(alpha = 0.38f),
                            ),
                        ),
                    ),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = FashTheme.spacing.editorialStart)
                .offset(y = ProfileHeroAvatarOverlap),
        ) {
            Surface(
                modifier = Modifier.size(ProfileHeroAvatarRingDp),
                shape = CircleShape,
                color = scheme.surface,
                shadowElevation = 3.dp,
                tonalElevation = 0.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                ) {
                    FashProfileAvatarImage(
                        imageUrl = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(ProfileHeroAvatarInnerDp)
                            .clip(CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileAestheticChipsRow(
    profile: com.pc.fash_android_mobile.data.user.ProfileInfo?,
    onAestheticTagClick: (tagName: String, tagId: String?) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val aestheticChips = profile?.let { p ->
        if (p.aestheticTagSnapshots.isNotEmpty()) {
            p.aestheticTagSnapshots.map { it.name to it.id }
        } else {
            p.aestheticTags.map { it to null }
        }
    }.orEmpty()
    if (aestheticChips.isEmpty()) return
    Spacer(modifier = Modifier.height(12.dp))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        itemsIndexed(
            items = aestheticChips,
            key = { i, pair -> "${pair.first}_${pair.second}_$i" },
        ) { _, pair ->
            val (tagName, tagId) = pair
            Text(
                text = tagName,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onPrimaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(FashTheme.spacing.radiusPill))
                    .background(scheme.primaryContainer.copy(alpha = 0.92f))
                    .clickable(onClick = { onAestheticTagClick(tagName, tagId) })
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun ProfileIdentityBlock(
    profile: com.pc.fash_android_mobile.data.user.ProfileInfo?,
    onEditClick: (() -> Unit)?,
    onAestheticTagClick: (tagName: String, tagId: String?) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .padding(top = 44.dp, bottom = 16.dp),
    ) {
        Text(
            text = profile?.displayName?.ifBlank { profile.username ?: "—" } ?: "—",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = scheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "@${profile?.username ?: "—"}",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
        if (onEditClick != null) {
            TextButton(
                onClick = onEditClick,
                modifier = Modifier.padding(top = 2.dp),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = scheme.primary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.profile_edit),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        profile?.bio?.takeIf { it.isNotBlank() }?.let { bio ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = bio,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ProfileAestheticChipsRow(profile = profile, onAestheticTagClick = onAestheticTagClick)
    }
}

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    isLoggingOut: Boolean = false,
    onEditProfile: () -> Unit = { },
    onShippingAddressesClick: () -> Unit = { },
    onOrdersClick: () -> Unit = { },
    onListingClick: (listingId: String, sellerId: String?) -> Unit = { _, _ -> },
    /** 0 = Following tab, 1 = Followers — same as [com.pc.fash_android_mobile.ui.follow.FollowConnectionsScreen]. */
    onOpenFollowConnections: (initialTab: Int) -> Unit = {},
    /** Opens Explore → Posts with filters + optional text search (from aesthetic tag chips). */
    onNavigateToExploreFromProfile: (
        categoryId: String?,
        brandId: String?,
        aestheticTagId: String?,
        searchQuery: String,
        countryId: String?,
        countryIso2: String?,
    ) -> Unit = { _, _, _, _, _, _ -> },
) {
    val profile by viewModel.profile.collectAsState()
    val sellingListings by viewModel.sellingListings.collectAsState()
    val soldListings by viewModel.soldListings.collectAsState()
    val wishlistListings by viewModel.wishlistListings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.ensureProfileLoaded()
    }

    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
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
                    Button(onClick = { viewModel.retryLoad() }) {
                        Text(stringResource(R.string.feed_retry))
                    }
                }
            }
            else -> {
                val listState = remember(selectedTab) { LazyListState(0, 0) }
                val scrollScope = rememberCoroutineScope()
                val items = when (selectedTab) {
                    0 -> sellingListings
                    1 -> soldListings
                    else -> wishlistListings
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .fillMaxHeight(),
                ) {
                    ProfileCollapsingScrollLayout(
                        listState = listState,
                        expandedHeader = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                ProfileHeader(
                                    profile = profile,
                                    onEditClick = onEditProfile,
                                    onAestheticTagClick = { name, id ->
                                        onNavigateToExploreFromProfile(null, null, id, name, null, null)
                                    },
                                )
                                ProfileStats(
                                    profile = profile,
                                    onFollowersClick = { onOpenFollowConnections(1) },
                                    onFollowingClick = { onOpenFollowConnections(0) },
                                )
                                ProfileShippingAddressesRow(onClick = onShippingAddressesClick)
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
                        wishlistTabVisible = true,
                        onListingClick = { item -> onListingClick(item.id, item.sellerId) },
                        showListingQuickActions = true,
                        onListingLike = { viewModel.toggleLike(it) },
                        onListingSave = { viewModel.toggleSave(it) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileShippingAddressesRow(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = scheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = FashColors.Primary,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.profile_shipping_addresses),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.address_list_subtitle_manage),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: com.pc.fash_android_mobile.data.user.ProfileInfo?,
    onEditClick: () -> Unit,
    onAestheticTagClick: (tagName: String, tagId: String?) -> Unit = { _, _ -> },
) {
    val coverUrl = profile?.coverImageUrl?.takeIf { it.isNotBlank() }?.let { resolveImageUrl(it) }
    val avatarUrl = profile?.avatarUrl?.takeIf { it.isNotBlank() }?.let { resolveImageUrl(it) }
    val coverModel: Any = coverUrl ?: FashDefaultProfileAssets.coverRes
    ProfileHeroSection(coverModel = coverModel, avatarUrl = avatarUrl)
    ProfileIdentityBlock(
        profile = profile,
        onEditClick = onEditClick,
        onAestheticTagClick = onAestheticTagClick,
    )
}

/**
 * Storefront header — same hero + identity as [ProfileHeader], without edit action.
 */
@Composable
internal fun SellerProfileHeader(
    profile: com.pc.fash_android_mobile.data.user.ProfileInfo?,
    onAestheticTagClick: (tagName: String, tagId: String?) -> Unit = { _, _ -> },
) {
    val coverUrl = profile?.coverImageUrl?.takeIf { it.isNotBlank() }?.let { resolveImageUrl(it) }
    val avatarUrl = profile?.avatarUrl?.takeIf { it.isNotBlank() }?.let { resolveImageUrl(it) }
    val coverModelSeller: Any = coverUrl ?: FashDefaultProfileAssets.coverRes
    ProfileHeroSection(coverModel = coverModelSeller, avatarUrl = avatarUrl)
    ProfileIdentityBlock(
        profile = profile,
        onEditClick = null,
        onAestheticTagClick = onAestheticTagClick,
    )
}

@Composable
internal fun ProfileStats(
    profile: com.pc.fash_android_mobile.data.user.ProfileInfo?,
    onFollowersClick: () -> Unit = {},
    onFollowingClick: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = scheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ProfileStatItem(
                modifier = Modifier.weight(1f),
                value = formatCount(profile?.followerCount ?: 0),
                label = stringResource(R.string.profile_followers),
                onClick = onFollowersClick,
                contentDescription = stringResource(R.string.profile_followers_open_cd),
                showIdleHint = true,
                idlePhaseOffsetMs = 0,
            )
            ProfileStatItem(
                modifier = Modifier.weight(1f),
                value = (profile?.followingCount ?: 0).toString(),
                label = stringResource(R.string.profile_following),
                onClick = onFollowingClick,
                contentDescription = stringResource(R.string.profile_following_open_cd),
                showIdleHint = true,
                idlePhaseOffsetMs = 120,
            )
            ProfileStatItem(
                modifier = Modifier.weight(1f),
                value = (profile?.productCount ?: 0).toString(),
                label = stringResource(R.string.profile_products),
            )
            ProfileStatItem(
                modifier = Modifier.weight(1f),
                value = (profile?.soldCount ?: 0).toString(),
                label = stringResource(R.string.profile_sold),
            )
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .padding(top = 4.dp, bottom = 16.dp),
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

/**
 * One short “spotlight” pulse for followers/following (accent tint + micro-scale), then back to match products/sold.
 * Only used from [ProfileStatItemWithFashionHint].
 */
@Composable
private fun rememberProfileStatFashionHint(phaseOffsetMs: Int): Float {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(phaseOffsetMs.toLong())
        anim.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
        anim.animateTo(0f, tween(820, easing = FastOutSlowInEasing))
    }
    return anim.value
}

@Composable
internal fun ProfileStatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null,
    /** Brief fashion hint animation for followers & following (tap affordance). */
    showIdleHint: Boolean = false,
    /** Stagger the two pulses (ms). */
    idlePhaseOffsetMs: Int = 0,
) {
    val showIdle = showIdleHint && onClick != null
    if (showIdle) {
        ProfileStatItemWithFashionHint(
            value = value,
            label = label,
            modifier = modifier,
            onClick = onClick!!,
            contentDescription = contentDescription,
            idlePhaseOffsetMs = idlePhaseOffsetMs,
        )
    } else {
        ProfileStatItemPlain(
            value = value,
            label = label,
            modifier = modifier,
            onClick = onClick,
            contentDescription = contentDescription,
        )
    }
}

@Composable
private fun ProfileStatItemWithFashionHint(
    value: String,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
    contentDescription: String?,
    idlePhaseOffsetMs: Int,
) {
    val fashionHint = rememberProfileStatFashionHint(idlePhaseOffsetMs)
    ProfileStatItemContent(
        value = value,
        label = label,
        modifier = modifier,
        onClick = onClick,
        contentDescription = contentDescription,
        fashionHint = fashionHint,
    )
}

@Composable
private fun ProfileStatItemPlain(
    value: String,
    label: String,
    modifier: Modifier,
    onClick: (() -> Unit)?,
    contentDescription: String?,
) {
    ProfileStatItemContent(
        value = value,
        label = label,
        modifier = modifier,
        onClick = onClick,
        contentDescription = contentDescription,
        fashionHint = 0f,
    )
}

@Composable
private fun ProfileStatItemContent(
    value: String,
    label: String,
    modifier: Modifier,
    onClick: (() -> Unit)?,
    contentDescription: String?,
    /** 0 = static; brief pulse blends accent into value/label for tappable stats. */
    fashionHint: Float,
) {
    val scheme = MaterialTheme.colorScheme
    val rippleIndication = LocalIndication.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (onClick != null && pressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "profileStatPress",
    )
    val hintScale = 1f + 0.014f * fashionHint
    val valueColor = lerp(scheme.onSurface, FashColors.Primary, fashionHint * 0.2f)
    val labelColor = lerp(scheme.onSurfaceVariant, FashColors.Primary, fashionHint * 0.38f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(pressScale * hintScale)
            .semantics(mergeDescendants = true) {
                contentDescription?.let { desc -> this.contentDescription = desc }
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = rippleIndication,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = valueColor,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
            color = labelColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 34.dp),
        )
    }
}

@Composable
internal fun ProfileTabs(
    tabLabelResIds: List<Int>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart),
    ) {
        tabLabelResIds.forEachIndexed { index, resId ->
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
                        color = if (selected) scheme.primary else scheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(scheme.primary),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProfileProductGrid(
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
            ListingGridCard(
                item = item,
                onClick = { onItemClick(item.id) },
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

