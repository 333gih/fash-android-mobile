package com.pc.fash_android_mobile.ui.main.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.ui.common.stableLazyKey
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.components.FashEmptyState
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

/** Scroll distance (first list item) over which the profile header fully collapses. */
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
 * Single scroll: profile block collapses with animation, sticky tabs, then product rows.
 * [expandedHeader] is the full hero + stats block; [compactHeader] is the slim bar when scrolled.
 */
@Composable
fun ProfileCollapsingScrollLayout(
    listState: LazyListState,
    expandedHeader: @Composable () -> Unit,
    compactHeader: @Composable () -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    items: List<ListingFeedItem>,
    isSellingTab: Boolean,
    onListingClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rawProgress = rememberProfileHeaderCollapseProgress(listState).value
    val progress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "profileHeaderCollapse",
    )

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        item(key = "profile_header") {
            CollapsingProfileHeaderSlot(
                progress = progress,
                expandedHeader = expandedHeader,
                compactHeader = compactHeader,
            )
        }
        stickyHeader(key = "tabs_and_title") {
            ProfileStickyTabsBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                showSectionTitle = progress > 0.22f,
            )
        }
        if (items.isEmpty()) {
            item(key = "empty") {
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
                    scrollable = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = 520.dp)
                        .padding(vertical = 24.dp),
                )
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
                            ProfileProductCard(
                                item = item,
                                onClick = { onListingClick(item.id) },
                            )
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsingProfileHeaderSlot(
    progress: Float,
    expandedHeader: @Composable () -> Unit,
    compactHeader: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    // One mode at a time — bottom-aligned compact over expanded caused overlapping text while scrolling.
    val showCompactBar = progress > 0.45f
    Crossfade(
        targetState = showCompactBar,
        animationSpec = tween(durationMillis = 220),
        label = "profileHeaderCollapseMode",
    ) { compact ->
        if (compact) {
            Surface(
                color = scheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = if (progress > 0.35f) 2.dp else 0.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                compactHeader()
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                expandedHeader()
            }
        }
    }
}

@Composable
private fun ProfileStickyTabsBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    showSectionTitle: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    // Single opaque surface for tabs + section title so list content scrolling underneath
    // does not hide labels (e.g. dark rows under a transparent sticky header).
    Surface(
        color = scheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ProfileTabs(
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
                            if (selectedTab == 0) {
                                R.string.profile_tab_selling
                            } else {
                                R.string.profile_tab_sold
                            },
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
    val avatarUrl = profile?.avatarUrl?.takeIf { it.isNotBlank() }?.let { resolveProfileImageUrl(it) }
    val display = profile?.displayName?.ifBlank { profile.username ?: "—" } ?: "—"
    val handle = profile?.username?.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "—"
    val initial = display.firstOrNull()?.takeIf { it.isLetter() }
        ?: profile?.username?.firstOrNull()?.takeIf { it.isLetter() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onClick)
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
            if (avatarUrl != null) {
                FashAsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(FashColors.Primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initial?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = FashColors.Primary,
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = display,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
