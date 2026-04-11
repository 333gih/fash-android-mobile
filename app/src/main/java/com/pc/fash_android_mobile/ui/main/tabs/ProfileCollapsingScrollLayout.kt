package com.pc.fash_android_mobile.ui.main.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.ui.common.stableLazyKey
import com.pc.fash_android_mobile.ui.components.FashProfileAvatarImage
import com.pc.fash_android_mobile.ui.components.FashEmptyState
import com.pc.fash_android_mobile.ui.feed.ListingGridCard
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

/** Scroll distance (first list item) used to derive collapse progress for the hero item only. */
private val ProfileHeaderCollapseScrollDp: Dp = 280.dp

@Composable
fun rememberProfileHeaderCollapseProgress(listState: LazyListState): androidx.compose.runtime.State<Float> {
    val density = LocalDensity.current
    val collapsePx = remember(density) { with(density) { ProfileHeaderCollapseScrollDp.toPx() } }
    return remember {
        derivedStateOf {
            val offsetPx = if (listState.firstVisibleItemIndex == 0) {
                listState.firstVisibleItemScrollOffset
            } else {
                collapsePx.toInt()
            }
            (offsetPx / collapsePx).coerceIn(0f, 1f)
        }
    }
}

/**
 * Single scroll: full hero in the first item (does not shrink — avoids tabs sticking too early),
 * then one sticky block: optional compact identity + tabs + section title, then product rows.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileCollapsingScrollLayout(
    listState: LazyListState,
    expandedHeader: @Composable () -> Unit,
    compactHeader: @Composable () -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    items: List<ListingFeedItem>,
    /** Own profile: Selling / Sold / Saved. Seller storefront: Selling / Sold only. */
    wishlistTabVisible: Boolean = true,
    onListingClick: (ListingFeedItem) -> Unit,
    /** When true, shows like/save on grid cards (e.g. profile storefronts). */
    showListingQuickActions: Boolean = false,
    onListingLike: (ListingFeedItem) -> Unit = {},
    onListingSave: (ListingFeedItem) -> Unit = {},
    /** Extra space at list end (e.g. seller profile bottom promo overlay). */
    additionalBottomInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val tabLabelResIds: List<Int> = if (wishlistTabVisible) {
        listOf(
            R.string.profile_tab_selling,
            R.string.profile_tab_sold,
            R.string.profile_tab_wishlist,
        )
    } else {
        listOf(
            R.string.profile_tab_selling,
            R.string.profile_tab_sold,
        )
    }
    val rawProgress = rememberProfileHeaderCollapseProgress(listState).value
    val progress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "profileHeaderCollapse",
    )
    val screenHeightDpInt = LocalConfiguration.current.screenHeightDp
    val bottomScrollPad = remember(screenHeightDpInt) {
        (screenHeightDpInt * 0.28f).dp.coerceIn(120.dp, 280.dp)
    }
    val totalBottomPad = bottomScrollPad + additionalBottomInset

    val listBg = MaterialTheme.colorScheme.background
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(listBg),
    ) {
        item(key = "profile_header") {
            // Only the expanded hero + stats — never swap to compact here (that was shrinking item 0 and
            // making the sticky tabs pin while the user was still in the hero).
            Column(modifier = Modifier.fillMaxWidth()) {
                expandedHeader()
            }
        }
        stickyHeader(key = "profile_sticky_chrome") {
            ProfileStickyProfileChrome(
                listState = listState,
                progress = progress,
                compactHeader = compactHeader,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                tabLabelResIds = tabLabelResIds,
            )
        }
        if (items.isEmpty()) {
            item(key = "empty") {
                val scheme = MaterialTheme.colorScheme
                val tabsPinnedToTop = listState.firstVisibleItemIndex > 0
                val (emptyIcon, emptyTitle, emptySubtitle) = when (selectedTab) {
                    0 -> Triple(
                        Icons.Outlined.Storefront,
                        R.string.profile_empty_selling_title,
                        R.string.profile_empty_selling_subtitle,
                    )
                    1 -> Triple(
                        Icons.Outlined.CheckCircle,
                        R.string.profile_empty_sold_title,
                        R.string.profile_empty_sold_subtitle,
                    )
                    else -> Triple(
                        Icons.Outlined.BookmarkBorder,
                        R.string.profile_empty_wishlist_title,
                        R.string.profile_empty_wishlist_subtitle,
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                ) {
                    FashEmptyState(
                        icon = emptyIcon,
                        title = stringResource(emptyTitle),
                        subtitle = stringResource(emptySubtitle),
                        scrollable = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 280.dp, max = 560.dp)
                            .padding(vertical = 24.dp),
                    )
                    if (tabsPinnedToTop && wishlistTabVisible) {
                        val footerRes = when (selectedTab) {
                            0 -> R.string.profile_empty_pinned_footer_selling
                            1 -> R.string.profile_empty_pinned_footer_sold
                            else -> R.string.profile_empty_pinned_footer_wishlist
                        }
                        HorizontalDivider(
                            color = scheme.outlineVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 12.dp),
                        )
                        Text(
                            text = stringResource(footerRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 8.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(totalBottomPad))
                }
            }
        } else {
            itemsIndexed(
                items.chunked(2),
                key = { index, row -> stableLazyKey(row.firstOrNull()?.id, index, "profrow") },
            ) { _, pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = FashTheme.spacing.editorialStart,
                            end = FashTheme.spacing.editorialEnd,
                            top = 4.dp,
                            bottom = 4.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    pair.forEach { item ->
                        Box(modifier = Modifier.weight(1f)) {
                            ListingGridCard(
                                item = item,
                                onClick = { onListingClick(item) },
                                showQuickActions = showListingQuickActions,
                                onLike = { onListingLike(item) },
                                onSave = { onListingSave(item) },
                            )
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            item(key = "list_bottom_pad") {
                Spacer(modifier = Modifier.height(totalBottomPad))
            }
        }
    }
}

/**
 * Sticky block: brief profile (when hero has scrolled away) + tabs + section title.
 * Sticks as one unit under the status bar area once the user scrolls past the tall hero item.
 */
@Composable
private fun ProfileStickyProfileChrome(
    listState: LazyListState,
    progress: Float,
    compactHeader: @Composable () -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabLabelResIds: List<Int>,
) {
    val scheme = MaterialTheme.colorScheme
    val headerScrolledOff by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
    // Hysteresis so elastic scroll doesn't flash the brief row.
    var showBriefBar by remember { mutableStateOf(false) }
    SideEffect {
        when {
            headerScrolledOff || progress > 0.52f -> showBriefBar = true
            progress < 0.36f && !headerScrolledOff -> showBriefBar = false
        }
    }
    // "Selling" / "Sold" label under tabs: after hero has scrolled away or user has scrolled most of the hero.
    // (Do not tie to grid item index — while the sticky bar is index 1, firstVisibleItemIndex often stays 1.)
    val showSectionTitle = headerScrolledOff || progress > 0.55f

    Surface(
        color = scheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = if (showBriefBar || headerScrolledOff) 3.dp else 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AnimatedVisibility(
                visible = showBriefBar,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    compactHeader()
                    HorizontalDivider(
                        color = scheme.outlineVariant.copy(alpha = 0.45f),
                        thickness = 1.dp,
                    )
                }
            }
            ProfileTabs(
                tabLabelResIds = tabLabelResIds,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
            )
            AnimatedVisibility(
                visible = showSectionTitle,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        color = scheme.outlineVariant.copy(alpha = 0.58f),
                    )
                    Text(
                        text = stringResource(
                            tabLabelResIds.getOrElse(selectedTab) { tabLabelResIds.first() },
                        ),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                        modifier = Modifier.padding(
                            horizontal = FashTheme.spacing.editorialStart,
                            vertical = 8.dp,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Slim bar shown when the header is collapsed — avatar + name + @handle (design system body/title).
 */
@Composable
fun ProfileCompactHeaderBar(
    profile: ProfileInfo?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val scrollToTopCd = stringResource(R.string.profile_cd_brief_scroll_to_top)
    val avatarUrl = profile?.avatarUrl?.takeIf { it.isNotBlank() }?.let { resolveProfileImageUrl(it) }
    val display = profile?.displayName?.ifBlank { profile.username ?: "—" } ?: "—"
    val handle = profile?.username?.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "—"

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (onClick != null && pressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "profileBriefPressScale",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.semantics { contentDescription = scrollToTopCd }
                } else {
                    Modifier
                },
            )
            .scale(pressScale)
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = LocalIndication.current,
                            onClick = onClick,
                        )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(scheme.surfaceContainerHigh),
        ) {
            FashProfileAvatarImage(
                imageUrl = avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = display,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (profile?.verified == true) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = stringResource(R.string.profile_verified_cd),
                        tint = FashColors.Primary,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(16.dp),
                    )
                }
            }
            Text(
                text = handle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun resolveProfileImageUrl(path: String): String {
    if (path.isBlank()) return ""
    if (path.startsWith("http")) return path
    val base = com.pc.fash_android_mobile.config.AppEnvironment.apiBaseUrl.trimEnd('/')
    return if (path.startsWith("/")) "$base$path" else "$base/$path"
}
