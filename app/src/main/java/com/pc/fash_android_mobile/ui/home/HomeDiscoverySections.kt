@file:OptIn(ExperimentalFoundationApi::class)

package com.pc.fash_android_mobile.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.home.HomeEditorialPostStub
import com.pc.fash_android_mobile.data.listing.Category
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.search.FeaturedSellerItem
import com.pc.fash_android_mobile.data.search.toUserSearchResult
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.feed.ListingGridCard
import com.pc.fash_android_mobile.ui.feed.resolveListingImageUrl
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import com.pc.fash_android_mobile.data.user.UserSearchResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Hero ratio inside the editorial gutter (width = screen minus [FashTheme.spacing.editorialStart/End]). */
private val EditorialImageAspect = 16f / 9f
private const val EditorialAutoAdvanceMs = 5_200L

@Composable
fun HomeEditorialPostsSection(
    posts: List<HomeEditorialPostStub>,
    onPostClick: (HomeEditorialPostStub) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (posts.isEmpty()) return
    val spacing = FashTheme.spacing
    val pagerState = rememberPagerState(pageCount = { posts.size })

    LaunchedEffect(posts.size) {
        if (posts.size <= 1) return@LaunchedEffect
        while (isActive) {
            delay(EditorialAutoAdvanceMs)
            runCatching {
                val next = (pagerState.currentPage + 1) % posts.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HomeSectionHeader(
            title = stringResource(R.string.home_section_editorial_title),
            subtitle = stringResource(R.string.home_section_editorial_subtitle),
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            // Same horizontal inset as [HomeSectionHeader] — card is full width inside this gutter only.
            contentPadding = PaddingValues(
                start = spacing.editorialStart,
                end = spacing.editorialEnd,
            ),
            pageSpacing = spacing.spacing2,
            verticalAlignment = Alignment.CenterVertically,
        ) { page ->
            HomeEditorialPostCard(
                post = posts[page],
                onClick = { onPostClick(posts[page]) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (posts.size > 1) {
            HomeEditorialPagerDots(
                pageCount = posts.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = spacing.editorialStart,
                        end = spacing.editorialEnd,
                        top = spacing.spacing2,
                        bottom = spacing.spacing3,
                    ),
            )
        } else {
            Spacer(modifier = Modifier.height(spacing.spacing3))
        }
    }
}

@Composable
private fun HomeEditorialPagerDots(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { i ->
            val selected = i == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (selected) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) FashColors.Primary
                        else scheme.onSurfaceVariant.copy(alpha = 0.28f),
                    ),
            )
        }
    }
}

@Composable
private fun HomeEditorialPostCard(
    post: HomeEditorialPostStub,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = FashTheme.spacing
    val title = post.title.ifBlank { post.slug }
    val excerpt = post.summary
    val cd = stringResource(R.string.home_editorial_post_open_cd, title)
    val resolved = post.coverImageUrl?.takeIf { it.isNotBlank() }?.let { resolveListingImageUrl(it) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = cd }
            .clip(RoundedCornerShape(spacing.radiusCard))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(spacing.radiusCard),
        color = scheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(EditorialImageAspect)
                    .background(scheme.surfaceVariant),
            ) {
                if (resolved != null) {
                    FashAsyncImage(
                        model = resolved,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Article,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp),
                            tint = FashColors.Primary.copy(alpha = 0.7f),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = spacing.spacing3,
                        vertical = spacing.spacing2,
                    ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (excerpt.isNotBlank()) {
                    Text(
                        text = excerpt,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = stringResource(R.string.home_editorial_read_more),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = FashColors.Primary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
fun HomeTrendingCategoriesSection(
    categories: List<Category>,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (categories.isEmpty()) return
    val spacing = FashTheme.spacing
    Column(modifier = modifier.fillMaxWidth()) {
        HomeSectionHeader(
            title = stringResource(R.string.home_section_trending_title),
            subtitle = stringResource(R.string.home_section_trending_subtitle),
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = spacing.editorialStart,
                end = spacing.editorialEnd,
                bottom = spacing.spacing4,
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
        ) {
            items(categories, key = { it.id }) { cat ->
                val cd = stringResource(R.string.home_trending_category_cd, cat.name)
                FilterChip(
                    selected = false,
                    onClick = { onCategoryClick(cat) },
                    label = {
                        Text(
                            text = cat.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    modifier = Modifier.semantics { contentDescription = cd },
                    shape = FashTheme.spacing.chipShape(),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

@Composable
fun HomeRecommendedSellersSection(
    sellers: List<FeaturedSellerItem>,
    followingIds: Set<String>,
    onSellerClick: (UserSearchResult) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (sellers.isEmpty()) return
    val spacing = FashTheme.spacing
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = spacing.editorialStart,
                    end = spacing.editorialEnd,
                    top = spacing.spacing2,
                    bottom = spacing.spacing1,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_section_recommended_sellers_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = FashColors.Primary,
                )
                Text(
                    text = stringResource(R.string.home_section_recommended_sellers_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = stringResource(R.string.explore_see_all),
                style = MaterialTheme.typography.labelMedium,
                color = FashColors.Primary,
                modifier = Modifier.clickable(onClick = onSeeAllClick),
            )
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = spacing.editorialStart,
                end = spacing.editorialEnd,
                bottom = spacing.spacing4,
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing3),
        ) {
            items(sellers, key = { it.userId.ifBlank { it.username } }) { seller ->
                val accent = !followingIds.contains(seller.userId) &&
                    !followingIds.contains(seller.username)
                HomeCompactSellerStory(
                    seller = seller,
                    showAccentRing = accent,
                    onClick = { onSellerClick(seller.toUserSearchResult()) },
                )
            }
        }
    }
}

@Composable
private fun HomeCompactSellerStory(
    seller: FeaturedSellerItem,
    showAccentRing: Boolean,
    onClick: () -> Unit,
    innerSize: Dp = 52.dp,
    ringStroke: Dp = 2.dp,
    ringGap: Dp = 2.dp,
) {
    val scheme = MaterialTheme.colorScheme
    val handle = when {
        seller.username.isNotBlank() -> "@${seller.username}"
        seller.displayName.isNotBlank() -> "@${seller.displayName.take(18)}"
        else -> "@…"
    }
    val label = seller.username.ifBlank { seller.displayName }.ifBlank { handle }
    val inner = innerSize
    val ring = ringStroke
    val gap = ringGap
    val outer = inner + ring * 2 + gap * 2
    val imageUrl = seller.avatarUrl.takeIf { it.isNotBlank() }?.let { resolveListingImageUrl(it) }

    Column(
        modifier = Modifier
            .width(76.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(outer),
            contentAlignment = Alignment.Center,
        ) {
            if (showAccentRing) {
                Box(
                    Modifier
                        .size(outer)
                        .border(ring, FashColors.Primary, CircleShape),
                )
            }
            Box(
                modifier = Modifier
                    .size(inner + gap * 2)
                    .clip(CircleShape)
                    .background(scheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (imageUrl != null) {
                    FashAsyncImage(
                        model = imageUrl,
                        contentDescription = stringResource(R.string.explore_featured_seller_cd, label),
                        modifier = Modifier
                            .size(inner)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = handle.take(2).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(FashTheme.spacing.spacing1))
        Text(
            text = handle,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** Minimum viewed listings before showing the resume rail (reduces noise for first-time open). */
private const val HomeRecentlyViewedMinItems = 2

@Composable
fun HomeHuntTodaySection(
    items: List<ListingFeedItem>,
    isLoading: Boolean,
    onSeeAllClick: () -> Unit,
    onListingClick: (listingId: String, sellerId: String?) -> Unit,
    onLike: (ListingFeedItem) -> Unit,
    onSave: (ListingFeedItem) -> Unit,
    onRecordView: (ListingFeedItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isLoading && items.isEmpty()) return
    val spacing = FashTheme.spacing
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = spacing.editorialStart,
                    end = spacing.editorialEnd,
                    top = spacing.spacing2,
                    bottom = spacing.spacing1,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_hunt_today_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = FashColors.Primary,
                )
                Text(
                    text = stringResource(R.string.home_hunt_today_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (!isLoading && items.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.home_hunt_today_see_all),
                    style = MaterialTheme.typography.labelMedium,
                    color = FashColors.Primary,
                    modifier = Modifier.clickable(onClick = onSeeAllClick),
                )
            }
        }
        if (isLoading && items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = FashColors.Primary,
                    strokeWidth = 2.dp,
                )
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = spacing.editorialStart,
                    end = spacing.editorialEnd,
                    bottom = spacing.spacing3,
                ),
                horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
            ) {
                items(items, key = { it.id }) { item ->
                    LaunchedEffect(item.id) { onRecordView(item) }
                    ListingGridCard(
                        item = item,
                        showQuickActions = true,
                        onLike = { onLike(item) },
                        onSave = { onSave(item) },
                        onClick = { onListingClick(item.id, item.sellerId) },
                        modifier = Modifier.width(156.dp),
                        imageAspectRatio = 4f / 5f,
                    )
                }
            }
        }
    }
}

/** Compact hint when the follow-based home feed has no listings (no large empty card). */
@Composable
fun HomeFollowFeedEmptyHint(
    onFeaturedSellersClick: () -> Unit,
    modifier: Modifier = Modifier,
    @androidx.annotation.StringRes hintRes: Int = R.string.home_follow_empty_hint,
    /** Guest browse: skip the follow section chrome; copy points to hunt today / Explore. */
    showSectionHeader: Boolean = true,
    /** Only when featured sellers rail will appear (avoids orphan CTA). */
    showFeaturedCta: Boolean = false,
) {
    val spacing = FashTheme.spacing
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = if (showSectionHeader) 0.dp else spacing.spacing1,
                bottom = spacing.spacing3,
            ),
    ) {
        if (showSectionHeader) {
            HomeSectionHeader(
                title = stringResource(R.string.home_top_section_title),
                subtitle = stringResource(R.string.home_top_section_subtitle),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = spacing.editorialStart,
                    end = spacing.editorialEnd,
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.spacing1),
        ) {
            Text(
                text = stringResource(hintRes),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            if (showFeaturedCta) {
                Text(
                    text = stringResource(R.string.home_follow_empty_cta_featured),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = FashColors.Primary,
                    modifier = Modifier.clickable(onClick = onFeaturedSellersClick),
                )
            }
        }
    }
}

@Composable
fun HomeRecentlyViewedSection(
    items: List<ListingFeedItem>,
    onListingClick: (listingId: String, sellerId: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.size < HomeRecentlyViewedMinItems) return
    val spacing = FashTheme.spacing
    Column(modifier = modifier.fillMaxWidth()) {
        HomeSectionHeader(
            title = stringResource(R.string.home_section_recently_viewed_title),
            subtitle = stringResource(R.string.home_section_recently_viewed_subtitle),
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = spacing.editorialStart,
                end = spacing.editorialEnd,
                bottom = spacing.spacing4,
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing2),
        ) {
            items(items, key = { it.id }) { item ->
                ListingGridCard(
                    item = item,
                    onClick = { onListingClick(item.id, item.sellerId) },
                    modifier = Modifier.width(168.dp),
                    showQuickActions = false,
                    compactFooter = true,
                    imageAspectRatio = 4f / 5f,
                )
            }
        }
    }
}

/**
 * Rich empty state for the personalized (following) feed — conversion-focused without breaking Fash surfaces.
 */
@Composable
fun HomePersonalizedFeedEmptyCard(
    onExploreClick: () -> Unit,
    onFeaturedSellersClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = FashTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = spacing.spacing2,
                bottom = spacing.spacing4,
            ),
    ) {
        HomeSectionHeader(
            title = stringResource(R.string.home_top_section_title),
            subtitle = stringResource(R.string.home_top_section_subtitle),
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = spacing.editorialStart,
                    end = spacing.editorialEnd,
                ),
            shape = RoundedCornerShape(spacing.radiusCard),
            color = scheme.surfaceContainerLow,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.spacing3, vertical = spacing.spacing4),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(FashColors.Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = FashColors.Primary,
                    )
                }
                Text(
                    text = stringResource(R.string.home_feed_empty_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.home_feed_empty_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
                ) {
                    Button(
                        onClick = onExploreClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(spacing.radiusSoftMin),
                        colors = ButtonDefaults.buttonColors(containerColor = FashColors.Primary),
                        contentPadding = PaddingValues(vertical = 10.dp),
                    ) {
                        Text(stringResource(R.string.home_empty_cta_explore))
                    }
                    OutlinedButton(
                        onClick = onFeaturedSellersClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(spacing.radiusSoftMin),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FashColors.Primary),
                        contentPadding = PaddingValues(vertical = 10.dp),
                    ) {
                        Text(stringResource(R.string.home_feed_empty_cta_featured))
                    }
                }
            }
        }
    }
}
