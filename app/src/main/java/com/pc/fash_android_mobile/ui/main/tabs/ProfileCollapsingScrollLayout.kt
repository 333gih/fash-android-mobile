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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import com.pc.fash_android_mobile.ui.components.LocalFashTabSwipeConsuming
import com.pc.fash_android_mobile.ui.components.fashTabSwipe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.width
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextOverflow
import com.pc.fash_android_mobile.ui.theme.FashColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.ui.common.stableLazyKey
import com.pc.fash_android_mobile.ui.components.FashProfileAvatarImage
import com.pc.fash_android_mobile.ui.components.FashEmptyState
import com.pc.fash_android_mobile.ui.feed.FeedLoadMoreFooter
import com.pc.fash_android_mobile.ui.feed.listingMasonryProfileChunkItems
import com.pc.fash_android_mobile.ui.feed.makeStableColumnLayout
import com.pc.fash_android_mobile.ui.feed.rememberListingMasonryColumnWidthDp
import com.pc.fash_android_mobile.ui.listing.listingStatusOverlayLabel
import com.pc.fash_android_mobile.ui.theme.FashTheme
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.withFrameMillis

/**
 * Scroll past the hero so sticky tabs + listing grid (or empty state) are visible.
 * Retries briefly so navigation from Home (tab switch + refresh) still lands on the grid.
 */
suspend fun LazyListState.scrollProfileToPinnedGrid(
    initialDelayMs: Long = 0,
    instant: Boolean = false,
) {
    if (initialDelayMs > 0) delay(initialDelayMs)
    repeat(5) { attempt ->
        snapshotFlow {
            layoutInfo.totalItemsCount to layoutInfo.visibleItemsInfo.isNotEmpty()
        }.first { (total, hasVisible) -> total > 1 && hasVisible }
        withFrameMillis { 0 }
        val total = layoutInfo.totalItemsCount
        val targetIndex = when {
            total > 2 -> 2
            total > 1 -> 1
            else -> return
        }
        val scrollBlock: suspend () -> Unit = {
            if (instant && attempt == 0) {
                scrollToItem(targetIndex, scrollOffset = 0)
            } else {
                animateScrollToItem(targetIndex, scrollOffset = 0)
            }
        }
        runCatching { scrollBlock() }
        if (firstVisibleItemIndex >= targetIndex) return
        delay(80L * (attempt + 1))
    }
}

/** Scroll distance (first list item) used to derive collapse progress for the hero item only. */
private val ProfileHeaderCollapseScrollDp: Dp = 280.dp

/**
 * Bottom promo chrome (slider + “Khám phá…” strip) on seller profile — visible only after the hero
 * has scrolled away and the sticky tab row is pinned (lazy index 0 = expanded header).
 *
 * Must use [remember] keyed on [listState]: each profile tab has its own [LazyListState]; a
 * [derivedStateOf] created once would keep reading a previous tab’s scroll position.
 */
@Composable
fun rememberProfilePromoFooterVisible(listState: LazyListState): State<Boolean> {
    return remember(listState) {
        derivedStateOf {
            // Index 0 = expanded hero; index 1+ = sticky tabs (+ grid) pinned under the top bar.
            listState.firstVisibleItemIndex > 0
        }
    }
}

@Composable
fun rememberProfileHeaderCollapseProgress(listState: LazyListState): androidx.compose.runtime.State<Float> {
    val density = LocalDensity.current
    val collapsePx = remember(density) { with(density) { ProfileHeaderCollapseScrollDp.toPx() } }
    return remember(listState) {
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
    /** Own profile (5 tabs) vs seller storefront (2 tabs). */
    listingTabSet: ProfileListingTabSet = ProfileListingTabSet.OwnProfile,
    /** Visual order of logical tab indices; defaults to natural order. */
    orderedTabIndices: List<Int> = emptyList(),
    onListingClick: (ListingFeedItem) -> Unit,
    /** When true, shows like/save on grid cards (e.g. profile storefronts). */
    showListingQuickActions: Boolean = false,
    onListingLike: (ListingFeedItem) -> Unit = {},
    onListingSave: (ListingFeedItem) -> Unit = {},
    /** Own profile: show marketplace status chip on listing cards (Selling / Sold tabs). */
    showListingStatusOverlay: Boolean = false,
    /** Extra space at list end (e.g. seller profile bottom promo overlay). */
    additionalBottomInset: Dp = 0.dp,
    /** First-page listing load under tabs — skeleton grid instead of empty state. */
    showGridLoading: Boolean = false,
    /** When set, enables paginated grid (seller storefront). */
    enableGridPagination: Boolean = false,
    gridHasMore: Boolean = false,
    gridIsLoadingMore: Boolean = false,
    onGridLoadMore: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val allTabLabelResIds = profileTabLabelResIds(listingTabSet)
    val tabIndices = remember(listingTabSet, orderedTabIndices) {
        val base = (0 until allTabLabelResIds.size).toList()
        orderedTabIndices.filter { it in base.indices }.ifEmpty { base }
    }
    val tabLabelResIds = remember(tabIndices, allTabLabelResIds) {
        tabIndices.map { allTabLabelResIds[it] }
    }
    val tabCount = tabLabelResIds.size
    val safeSelectedTab = selectedTab.coerceIn(0, ProfileListingTab.LAST)
    val columnAssignments = remember { mutableStateMapOf<String, Boolean>() }
    val masonryLayout = remember(items) { makeStableColumnLayout(items, columnAssignments) }
    val masonryColumnWidthDp = rememberListingMasonryColumnWidthDp()
    var tabSwipeConsuming by remember { mutableStateOf(false) }
    var suppressListingClicks by remember { mutableStateOf(false) }
    val swipeScope = rememberCoroutineScope()
    val visualSelectedIndex = tabIndices.indexOf(safeSelectedTab).coerceAtLeast(0)
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

    LaunchedEffect(listState, items.size, gridHasMore, gridIsLoadingMore, showGridLoading, enableGridPagination) {
        if (!enableGridPagination) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to info.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisible, totalItems) ->
                if (
                    !showGridLoading &&
                    items.isNotEmpty() &&
                    gridHasMore &&
                    !gridIsLoadingMore &&
                    totalItems > 0 &&
                    lastVisible >= totalItems - 4
                ) {
                    onGridLoadMore()
                }
            }
    }

    val tabSwipeModifier = if (tabCount > 1) {
        Modifier.fashTabSwipe(
            enabled = true,
            tabCount = tabCount,
            currentVisualIndex = visualSelectedIndex,
            onVisualIndexChanged = { index -> onTabSelected(tabIndices[index]) },
            onConsumingChanged = { active ->
                tabSwipeConsuming = active
                if (active) suppressListingClicks = true
            },
            onTabSwipeCommitted = {
                suppressListingClicks = true
                swipeScope.launch {
                    delay(320)
                    suppressListingClicks = false
                }
            },
        )
    } else {
        Modifier
    }

    val listBg = MaterialTheme.colorScheme.background
    CompositionLocalProvider(
        LocalFashTabSwipeConsuming provides (tabSwipeConsuming || suppressListingClicks),
    ) {
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
                selectedTab = safeSelectedTab,
                onTabSelected = onTabSelected,
                tabLabelResIds = tabLabelResIds,
                tabIndices = tabIndices,
                modifier = tabSwipeModifier,
            )
        }
        if (showGridLoading) {
            item(key = "loading_$safeSelectedTab") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(tabSwipeModifier),
                ) {
                    com.pc.fash_android_mobile.ui.components.FashSkeletonGrid(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        rows = 2,
                        staggered = true,
                    )
                    Spacer(modifier = Modifier.height(totalBottomPad))
                }
            }
        } else if (items.isEmpty()) {
            item(key = "empty_$safeSelectedTab") {
                val scheme = MaterialTheme.colorScheme
                val tabsPinnedToTop = listState.firstVisibleItemIndex > 0
                val (emptyIcon, emptyTitle, emptySubtitle) = profileTabEmptyCopy(listingTabSet, safeSelectedTab)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(tabSwipeModifier)
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
                    if (tabsPinnedToTop) {
                        profileTabPinnedFooterRes(listingTabSet, safeSelectedTab)?.let { footerRes ->
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
                    }
                    Spacer(modifier = Modifier.height(totalBottomPad))
                }
            }
        } else {
            listingMasonryProfileChunkItems(
                items = items,
                layout = masonryLayout,
                keyPrefix = "prof_$safeSelectedTab",
                columnWidthDp = masonryColumnWidthDp,
                showQuickActions = showListingQuickActions,
                showListingStatusOverlay = showListingStatusOverlay,
                chunkModifier = tabSwipeModifier,
                onListingClick = onListingClick,
                onListingLike = onListingLike,
                onListingSave = onListingSave,
            )
            if (enableGridPagination && (gridHasMore || gridIsLoadingMore)) {
                item(key = "profile_grid_load_more_$safeSelectedTab") {
                    FeedLoadMoreFooter(
                        enabled = gridHasMore,
                        isLoadingMore = gridIsLoadingMore,
                        onLoadMore = onGridLoadMore,
                        modifier = tabSwipeModifier,
                    )
                }
            }
            item(key = "list_bottom_pad") {
                Spacer(modifier = Modifier.height(totalBottomPad))
            }
        }
    }
    }
}

private fun profileTabEmptyCopy(
    tabSet: ProfileListingTabSet,
    selectedTab: Int,
): Triple<androidx.compose.ui.graphics.vector.ImageVector, Int, Int> =
    when (tabSet) {
        ProfileListingTabSet.OwnProfile -> when (selectedTab) {
            ProfileListingTab.IN_REVIEW -> Triple(
                Icons.Outlined.RateReview,
                R.string.profile_empty_in_review_title,
                R.string.profile_empty_in_review_subtitle,
            )
            ProfileListingTab.REJECTED -> Triple(
                Icons.Outlined.ErrorOutline,
                R.string.profile_empty_rejected_title,
                R.string.profile_empty_rejected_subtitle,
            )
            ProfileListingTab.SOLD -> Triple(
                Icons.Outlined.CheckCircle,
                R.string.profile_empty_sold_title,
                R.string.profile_empty_sold_subtitle,
            )
            ProfileListingTab.WISHLIST -> Triple(
                Icons.Outlined.BookmarkBorder,
                R.string.profile_empty_wishlist_title,
                R.string.profile_empty_wishlist_subtitle,
            )
            else -> Triple(
                Icons.Outlined.Storefront,
                R.string.profile_empty_selling_title,
                R.string.profile_empty_selling_subtitle,
            )
        }
        ProfileListingTabSet.SellerStorefront -> when (selectedTab) {
            1 -> Triple(
                Icons.Outlined.CheckCircle,
                R.string.profile_empty_sold_title,
                R.string.profile_empty_sold_subtitle,
            )
            else -> Triple(
                Icons.Outlined.Storefront,
                R.string.profile_empty_selling_title,
                R.string.profile_empty_selling_subtitle,
            )
        }
    }

private fun profileTabPinnedFooterRes(tabSet: ProfileListingTabSet, selectedTab: Int): Int? =
    when (tabSet) {
        ProfileListingTabSet.OwnProfile -> when (selectedTab) {
            ProfileListingTab.ACTIVE -> R.string.profile_empty_pinned_footer_selling
            ProfileListingTab.IN_REVIEW -> R.string.profile_empty_pinned_footer_in_review
            ProfileListingTab.REJECTED -> R.string.profile_empty_pinned_footer_rejected
            ProfileListingTab.SOLD -> R.string.profile_empty_pinned_footer_sold
            ProfileListingTab.WISHLIST -> R.string.profile_empty_pinned_footer_wishlist
            else -> null
        }
        ProfileListingTabSet.SellerStorefront -> when (selectedTab) {
            0 -> R.string.profile_empty_pinned_footer_selling
            1 -> R.string.profile_empty_pinned_footer_sold
            else -> null
        }
    }

/** Scrollable tab strip — shows up to 3 tabs in the viewport (same pattern as Home feed tabs). */
@Composable
internal fun ProfileTabSwitcher(
    tabLabelResIds: List<Int>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabIndices: List<Int> = tabLabelResIds.indices.toList(),
) {
    if (tabLabelResIds.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val edgePad = FashTheme.spacing.editorialStart
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val viewportWidth = (screenWidth - edgePad * 2).coerceAtLeast(1.dp)
    val visibleSlots = minOf(3, tabLabelResIds.size)
    val tabWidth = viewportWidth / visibleSlots
    val visualSelected = tabIndices.indexOf(selectedTab).coerceAtLeast(0)

    ScrollableTabRow(
        selectedTabIndex = visualSelected,
        modifier = Modifier.fillMaxWidth(),
        edgePadding = edgePad,
        containerColor = scheme.surface,
        contentColor = scheme.onSurface,
        divider = {},
        indicator = { positions ->
            if (visualSelected in positions.indices) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(positions[visualSelected])
                        .padding(horizontal = 8.dp),
                    height = 2.dp,
                    color = FashColors.Primary,
                )
            }
        },
    ) {
        tabLabelResIds.forEachIndexed { index, resId ->
            val selected = index == visualSelected
            Tab(
                selected = selected,
                onClick = { onTabSelected(tabIndices.getOrElse(index) { index }) },
                modifier = Modifier.width(tabWidth),
                text = {
                    Text(
                        text = stringResource(resId),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (selected) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.75f),
                    )
                },
            )
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
    tabIndices: List<Int> = tabLabelResIds.indices.toList(),
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val headerScrolledOff by remember(listState) {
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
        modifier = modifier.fillMaxWidth(),
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
            ProfileTabSwitcher(
                tabLabelResIds = tabLabelResIds,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                tabIndices = tabIndices,
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
