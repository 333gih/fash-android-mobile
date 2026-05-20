package com.pc.fash_android_mobile.ui.main.tabs

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storefront
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.OutlinedButton
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
import java.util.Locale
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
import com.pc.fash_android_mobile.ui.profile.ProfileShare
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.data.user.ProfileInfo
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = profile?.displayName?.ifBlank { profile.username ?: "—" } ?: "—",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (profile?.verified == true) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = stringResource(R.string.profile_verified_cd),
                    tint = FashColors.Primary,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(22.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "@${profile?.username ?: "—"}",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
        profile?.accountEmail?.takeIf { it.isNotBlank() }?.let { em ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.profile_account_email, em),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
        profile?.accountPhone?.takeIf { it.isNotBlank() }?.let { ph ->
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.profile_account_phone, ph),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
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
        if (onEditClick != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onEditClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, FashColors.Primary.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FashColors.Primary),
            ) {
                Text(
                    text = stringResource(R.string.profile_edit),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    isLoggingOut: Boolean = false,
    onEditProfile: () -> Unit = { },
    onShippingAddressesClick: () -> Unit = { },
    onInviteFriendsClick: () -> Unit = { },
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
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val meetingReverifyRequired by viewModel.meetingSchedulingReverifyRequired.collectAsState()
    val meetingSuspendedUntil by viewModel.meetingSchedulingSuspendedUntil.collectAsState()
    val ackMeetingReverifyInFlight by viewModel.ackMeetingReverifyInFlight.collectAsState()
    val profileContext = LocalContext.current

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
                val pullState = rememberPullToRefreshState()
                LaunchedEffect(Unit) {
                    viewModel.scrollProfileToTop.collect {
                        listState.animateScrollToItem(0)
                    }
                }
                val wishlistTabOpenGen by viewModel.wishlistTabOpenGeneration.collectAsState()
                /** 0 = Selling, 1 = Sold, 2 = Saved (wishlist) — see [ProfileCollapsingScrollLayout] tab labels. */
                val wishlistTabIndex = 2

                LaunchedEffect(wishlistTabOpenGen, selectedTab) {
                    if (wishlistTabOpenGen == 0L) return@LaunchedEffect
                    if (selectedTab != wishlistTabIndex) {
                        selectedTab = wishlistTabIndex
                        return@LaunchedEffect
                    }
                    delay(90)
                    val total = listState.layoutInfo.totalItemsCount
                    scrollScope.launch {
                        when {
                            total > 2 -> runCatching { listState.animateScrollToItem(2, scrollOffset = 0) }
                            total > 1 -> runCatching { listState.animateScrollToItem(1, scrollOffset = 0) }
                        }
                    }
                }
                val items = when (selectedTab) {
                    0 -> sellingListings
                    1 -> soldListings
                    else -> wishlistListings
                }
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    state = pullState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullState,
                            isRefreshing = isRefreshing,
                            color = FashColors.Primary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    },
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
                                ProfileOwnMetricsCard(
                                    profile = profile,
                                    onFollowersClick = { onOpenFollowConnections(1) },
                                    onFollowingClick = { onOpenFollowConnections(0) },
                                )
                                ProfileSizingReferenceCard(
                                    profile = profile,
                                    onEdit = onEditProfile,
                                )
                                if (meetingReverifyRequired) {
                                    ProfileMeetingIdentityReverifyBanner(
                                        suspendedUntil = meetingSuspendedUntil,
                                        verifyUrl = AppEnvironment.identityReverifyUrl.takeIf { it.isNotEmpty() },
                                        isAckInFlight = ackMeetingReverifyInFlight,
                                        onOpenVerification = {
                                            val u = AppEnvironment.identityReverifyUrl.trim()
                                            if (u.isNotEmpty()) {
                                                runCatching {
                                                    CustomTabsIntent.Builder()
                                                        .setShowTitle(true)
                                                        .build()
                                                        .launchUrl(profileContext, Uri.parse(u))
                                                }
                                            }
                                        },
                                        onAckCompleted = { viewModel.ackMeetingIdentityReverify() },
                                    )
                                }
                                ProfileQuickActionsCard(
                                    profile = profile,
                                    onShippingAddressesClick = onShippingAddressesClick,
                                    onInviteFriendsClick = onInviteFriendsClick,
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
                        wishlistTabVisible = true,
                        onListingClick = { item -> onListingClick(item.id, item.sellerId) },
                        showListingQuickActions = true,
                        onListingLike = { viewModel.toggleLike(it) },
                        onListingSave = { viewModel.toggleSave(it) },
                        showListingStatusOverlay = selectedTab == 0 || selectedTab == 1,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileMeetingIdentityReverifyBanner(
    suspendedUntil: String?,
    verifyUrl: String?,
    isAckInFlight: Boolean,
    onOpenVerification: () -> Unit,
    onAckCompleted: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp)),
        color = scheme.errorContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = scheme.onErrorContainer,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(R.string.profile_meeting_identity_reverify_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onErrorContainer,
                )
            }
            Text(
                text = stringResource(R.string.profile_meeting_identity_reverify_body),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onErrorContainer,
            )
            suspendedUntil?.takeIf { it.isNotBlank() }?.let { until ->
                Text(
                    text = stringResource(R.string.profile_meeting_identity_reverify_suspended_until, until),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onErrorContainer,
                )
            }
            if (!verifyUrl.isNullOrBlank()) {
                OutlinedButton(
                    onClick = onOpenVerification,
                    enabled = !isAckInFlight,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.meeting_identity_reverify_open_link))
                }
            }
            Button(
                onClick = onAckCompleted,
                enabled = !isAckInFlight,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isAckInFlight) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = scheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.meeting_identity_reverify_ack_done))
                }
            }
        }
    }
}

@Composable
private fun ProfileInviteFriendsRow(onClick: () -> Unit) {
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
                imageVector = Icons.Outlined.PersonAdd,
                contentDescription = null,
                tint = FashColors.Primary,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.profile_invite_friends_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.profile_invite_friends_subtitle),
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

/** Own profile: stats + shop trust in one card (tappable followers / following). */
@Composable
private fun ProfileOwnMetricsCard(
    profile: ProfileInfo?,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
) {
    val p = profile ?: return
    val scheme = MaterialTheme.colorScheme
    val ratingVal = p.rating
    val reviewCount = p.reviewCount
    val hasRatingScore = ratingVal != null && ratingVal > 0f
    val productCount = p.productCount ?: 0
    val soldCount = p.soldCount ?: 0
    val rep = p.reputationPoints?.takeIf { it > 0 }
    val fast = p.hasFastDelivery
    val showTrustFooter = hasRatingScore || productCount > 0 || rep != null || fast

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .padding(top = 4.dp, bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = scheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileStatItem(
                    modifier = Modifier.weight(1f),
                    value = formatCount(p.followerCount),
                    label = stringResource(R.string.profile_followers),
                    onClick = onFollowersClick,
                    contentDescription = stringResource(R.string.profile_followers_open_cd),
                    showIdleHint = true,
                    idlePhaseOffsetMs = 0,
                )
                ProfileOwnStatDivider()
                ProfileStatItem(
                    modifier = Modifier.weight(1f),
                    value = p.followingCount.toString(),
                    label = stringResource(R.string.profile_following),
                    onClick = onFollowingClick,
                    contentDescription = stringResource(R.string.profile_following_open_cd),
                    showIdleHint = true,
                    idlePhaseOffsetMs = 120,
                )
                ProfileOwnStatDivider()
                ProfileStatItem(
                    modifier = Modifier.weight(1f),
                    value = productCount.toString(),
                    label = stringResource(R.string.profile_products),
                )
                ProfileOwnStatDivider()
                ProfileStatItem(
                    modifier = Modifier.weight(1f),
                    value = soldCount.toString(),
                    label = stringResource(R.string.profile_sold),
                )
            }
            if (showTrustFooter) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    color = scheme.outlineVariant.copy(alpha = 0.35f),
                )
                ProfileOwnTrustFooter(
                    hasRatingScore = hasRatingScore,
                    ratingVal = ratingVal,
                    reviewCount = reviewCount,
                    hasShop = productCount > 0,
                    reputationPoints = rep,
                    hasFastDelivery = fast,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ProfileOwnStatDivider() {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .height(40.dp)
            .width(1.dp)
            .background(scheme.outlineVariant.copy(alpha = 0.4f)),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileOwnTrustFooter(
    hasRatingScore: Boolean,
    ratingVal: Float?,
    reviewCount: Int?,
    hasShop: Boolean,
    reputationPoints: Int?,
    hasFastDelivery: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        Text(text = sub, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
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
                }
            }
        }
        if (reputationPoints != null || hasFastDelivery) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                reputationPoints?.let { pts ->
                    ProfileTrustBadge(
                        text = stringResource(R.string.profile_reputation_points, pts),
                        contentColor = scheme.primary,
                        containerColor = scheme.primaryContainer.copy(alpha = 0.45f),
                    )
                }
                if (hasFastDelivery) {
                    ProfileTrustBadge(
                        text = stringResource(R.string.profile_fast_delivery),
                        contentColor = FashColors.Success,
                        containerColor = FashColors.Success.copy(alpha = 0.12f),
                    )
                }
            }
        }
    }
}

/** Size reference — own profile only; always visible with empty state + edit entry. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileSizingReferenceCard(
    profile: ProfileInfo?,
    onEdit: () -> Unit,
) {
    val p = profile ?: return
    val scheme = MaterialTheme.colorScheme
    val unit = p.referenceMeasurementUnit?.trim()?.takeIf { it.isNotEmpty() }
        ?: stringResource(R.string.profile_sizing_ref_unit_default)
    val measurementLabels = buildList {
        p.referenceMeasurementChest?.takeIf { it.isFinite() && it > 0 }?.let {
            add(stringResource(R.string.profile_sizing_ref_chest, it, unit))
        }
        p.referenceMeasurementHem?.takeIf { it.isFinite() && it > 0 }?.let {
            add(stringResource(R.string.profile_sizing_ref_hem, it, unit))
        }
        p.referenceMeasurementLength?.takeIf { it.isFinite() && it > 0 }?.let {
            add(stringResource(R.string.profile_sizing_ref_length, it, unit))
        }
        p.referenceMeasurementShoulders?.takeIf { it.isFinite() && it > 0 }?.let {
            add(stringResource(R.string.profile_sizing_ref_shoulders, it, unit))
        }
        p.referenceMeasurementSleeveLength?.takeIf { it.isFinite() && it > 0 }?.let {
            add(stringResource(R.string.profile_sizing_ref_sleeve, it, unit))
        }
    }
    val refSize = p.referenceSize?.trim()?.takeIf { it.isNotEmpty() }
    val hasSizeData = refSize != null || measurementLabels.isNotEmpty()
    val fallbackOnly = !hasSizeData && p.sizingReferenceCompleted
    val chipShape = RoundedCornerShape(10.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = scheme.surfaceContainerLow,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Straighten,
                    contentDescription = null,
                    tint = FashColors.Primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.profile_sizing_ref_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.profile_sizing_ref_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                    )
                }
                TextButton(onClick = onEdit) {
                    Text(
                        text = stringResource(R.string.profile_sizing_ref_edit),
                        style = MaterialTheme.typography.labelLarge,
                        color = FashColors.Primary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            when {
                fallbackOnly -> {
                    Text(
                        text = stringResource(R.string.profile_sizing_ref_completed_only),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface,
                    )
                }
                !hasSizeData -> {
                    Text(
                        text = stringResource(R.string.profile_sizing_ref_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, FashColors.Primary.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FashColors.Primary),
                    ) {
                        Text(stringResource(R.string.profile_sizing_ref_edit))
                    }
                }
                else -> {
                    refSize?.let { size ->
                        Surface(
                            shape = chipShape,
                            color = FashColors.Primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, FashColors.Primary.copy(alpha = 0.25f)),
                        ) {
                            Text(
                                text = stringResource(R.string.profile_sizing_ref_size, size),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = FashColors.Primary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            )
                        }
                        if (measurementLabels.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    if (measurementLabels.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            measurementLabels.forEach { label ->
                                Surface(
                                    shape = chipShape,
                                    color = scheme.surfaceContainerHighest,
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = scheme.onSurface,
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

/** Share shop + shipping + invite grouped under one account section. */
@Composable
private fun ProfileQuickActionsCard(
    profile: ProfileInfo?,
    onShippingAddressesClick: () -> Unit,
    onInviteFriendsClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shareContext = LocalContext.current
    val username = profile?.username?.trim()?.removePrefix("@").orEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .padding(bottom = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.profile_quick_actions_title),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = scheme.surfaceContainerLow,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (username.isNotBlank()) {
                    ProfileQuickActionRow(
                        icon = Icons.Outlined.Share,
                        title = stringResource(R.string.profile_share_shop_title),
                        subtitle = stringResource(R.string.profile_share_shop_subtitle),
                        onClick = {
                            ProfileShare.launch(
                                context = shareContext,
                                username = username,
                                displayName = profile?.displayName,
                            )
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        color = scheme.outlineVariant.copy(alpha = 0.35f),
                    )
                }
                ProfileQuickActionRow(
                    icon = Icons.Outlined.LocationOn,
                    title = stringResource(R.string.profile_shipping_addresses),
                    subtitle = stringResource(R.string.address_list_subtitle_manage),
                    onClick = onShippingAddressesClick,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    color = scheme.outlineVariant.copy(alpha = 0.35f),
                )
                ProfileQuickActionRow(
                    icon = Icons.Outlined.PersonAdd,
                    title = stringResource(R.string.profile_invite_friends_title),
                    subtitle = stringResource(R.string.profile_invite_friends_subtitle),
                    onClick = onInviteFriendsClick,
                )
            }
        }
    }
}

@Composable
private fun ProfileQuickActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(FashColors.Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FashColors.Primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = scheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Same trust/rating card on own profile and seller storefront — matches [ProfileStats] chrome. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileTrustCard(profile: ProfileInfo?) {
    val p = profile ?: return
    val scheme = MaterialTheme.colorScheme
    val cardShape = RoundedCornerShape(FashTheme.spacing.radiusCard)
    val hasShop = (p.productCount ?: 0) > 0
    val ratingVal = p.rating
    val reviewCount = p.reviewCount
    val hasRatingScore = ratingVal != null && ratingVal > 0f
    val showNewShopLine = hasShop && !hasRatingScore
    val rep = p.reputationPoints?.takeIf { it > 0 }
    val fast = p.hasFastDelivery
    if (!hasRatingScore && !showNewShopLine && rep == null && !fast) return

    val showRatingRow = hasRatingScore || showNewShopLine
    val starTint = if (hasRatingScore) {
        FashColors.Primary
    } else {
        scheme.onSurfaceVariant.copy(alpha = 0.45f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 8.dp),
        shape = cardShape,
        color = scheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (showRatingRow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = starTint,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        when {
                            hasRatingScore && reviewCount != null && reviewCount >= 0 -> {
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f", ratingVal!!),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = scheme.onSurface,
                                )
                                Text(
                                    text = stringResource(
                                        R.string.profile_seller_trust_reviews_count,
                                        reviewCount,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                            hasRatingScore -> {
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f", ratingVal!!),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = scheme.onSurface,
                                )
                                Text(
                                    text = stringResource(R.string.profile_seller_trust_subtitle_score_only),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                            else -> {
                                Text(
                                    text = stringResource(R.string.profile_rating_none_seller),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            if (rep != null || fast) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    rep?.let { pts ->
                        ProfileTrustBadge(
                            text = stringResource(R.string.profile_reputation_points, pts),
                            contentColor = scheme.primary,
                            containerColor = scheme.primaryContainer.copy(alpha = 0.45f),
                        )
                    }
                    if (fast) {
                        ProfileTrustBadge(
                            text = stringResource(R.string.profile_fast_delivery),
                            contentColor = FashColors.Success,
                            containerColor = FashColors.Success.copy(alpha = 0.12f),
                        )
                    }
                }
            }
        }
    }
}

/** @deprecated Use [ProfileTrustCard] — kept as alias for storefront call sites. */
@Composable
internal fun ProfileSellerTrustBanner(profile: ProfileInfo?) {
    ProfileTrustCard(profile = profile)
}

@Composable
private fun ProfileTrustBadge(
    text: String,
    contentColor: androidx.compose.ui.graphics.Color,
    containerColor: androidx.compose.ui.graphics.Color,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
        color = contentColor,
        modifier = Modifier
            .clip(RoundedCornerShape(FashTheme.spacing.radiusSoftMin))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 5.dp),
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
    Spacer(modifier = Modifier.height(4.dp))
    if (profile?.meetingNoShowWarning == true) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FashTheme.spacing.editorialStart)
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.profile_meeting_no_show_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
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

