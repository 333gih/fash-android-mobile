package com.pc.fash_android_mobile.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.pc.fash_android_mobile.data.model.*
import com.pc.fash_android_mobile.ui.feed.FashPromoSlider
import java.text.NumberFormat
import java.util.*

/**
 * New unified home feed - single scroll with mixed rails (replaces tabbed UI)
 */
@Composable
fun UnifiedHomeScreen(
    viewModel: UnifiedHomeViewModel = hiltViewModel(),
    onListingClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSavedClick: () -> Unit,
    onSeeAllClick: (String) -> Unit
) {
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = viewModel.isRefreshing)

    Box(modifier = Modifier.fillMaxSize()) {
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.refresh() }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp) // Space for promo dock
            ) {
                // Hero Card (Daily Style Story)
                item {
                    viewModel.heroCard?.let { hero ->
                        HeroCardView(
                            hero = hero,
                            onClick = { /* Handle hero click */ },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // Quick Actions Bar
                item {
                    QuickActionsBar(
                        onSizeFilterClick = { viewModel.toggleSizeFilter() },
                        onSearchClick = onSearchClick,
                        onSavedClick = onSavedClick,
                        isSizeFilterActive = viewModel.showOnlyMySize,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                // Rails (mixed content sections)
                items(viewModel.rails) { rail ->
                    RailSection(
                        rail = rail,
                        onListingClick = onListingClick,
                        onSeeAllClick = { rail.seeAllUrl?.let(onSeeAllClick) },
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }

                // Loading more indicator
                item {
                    if (viewModel.isLoadingMore) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Brand footer
                item {
                    if (viewModel.rails.isNotEmpty() && !viewModel.hasMore) {
                        HomeBrandFooter(
                            modifier = Modifier.padding(vertical = 32.dp)
                        )
                    }
                }
            }
        }

        // Sticky promo dock at bottom
        if (viewModel.promoSlides.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                FashPromoSlider(
                    slides = viewModel.promoSlides,
                    onSlideClick = { /* Handle promo click */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }
    }
}

@Composable
fun HeroCardView(
    hero: HeroCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        // Hero image (3:4 portrait)
        hero.imageURL?.let { imageUrl ->
            AsyncImage(
                model = imageUrl,
                contentDescription = hero.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Text overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = hero.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            hero.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun QuickActionsBar(
    onSizeFilterClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSavedClick: () -> Unit,
    isSizeFilterActive: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Size filter toggle
        Button(
            onClick = onSizeFilterClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSizeFilterActive) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "My Size",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Search
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            }

            // Saved
            IconButton(onClick = onSavedClick) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Saved"
                )
            }
        }
    }
}

@Composable
fun RailSection(
    rail: HomeRail,
    onListingClick: (String) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Rail header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rail.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                rail.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (rail.seeAllUrl != null) {
                TextButton(onClick = onSeeAllClick) {
                    Text("See All")
                }
            }
        }

        // Rail content based on type
        when (rail.railType) {
            RailType.GRID -> {
                RailGridContent(
                    items = rail.items.take(6),
                    onListingClick = onListingClick
                )
            }
            RailType.HORIZONTAL_SCROLL -> {
                RailHorizontalScrollContent(
                    items = rail.items.take(10),
                    onListingClick = onListingClick
                )
            }
        }
    }
}

@Composable
fun RailGridContent(
    items: List<ListingWithMatch>,
    onListingClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier
            .fillMaxWidth()
            .height(600.dp), // Fixed height for grid rail
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp,
        userScrollEnabled = false // Disable internal scrolling
    ) {
        items(items) { item ->
            PortraitListingCard(
                item = item,
                onClick = { onListingClick(item.listing.id) }
            )
        }
    }
}

@Composable
fun RailHorizontalScrollContent(
    items: List<ListingWithMatch>,
    onListingClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            PortraitListingCard(
                item = item,
                onClick = { onListingClick(item.listing.id) },
                modifier = Modifier.width(160.dp)
            )
        }
    }
}

@Composable
fun PortraitListingCard(
    item: ListingWithMatch,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Box {
            // Image (3:4 portrait)
            AsyncImage(
                model = item.listing.coverImageURL,
                contentDescription = item.listing.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(8.dp))
            )

            // Size match badge (top right)
            item.sizeMatch?.let { sizeMatch ->
                SizeMatchBadge(
                    badge = sizeMatch.badge,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
        }

        // Title
        Text(
            text = item.listing.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Price & Brand
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatPrice(item.listing.price),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            item.listing.brand?.let { brand ->
                Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = brand,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Recommendation reason (if available)
        item.recommendReason?.let { reason ->
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun SizeMatchBadge(
    badge: String,
    modifier: Modifier = Modifier
) {
    val badgeInfo = SizeMatchBadge.fromBadge(badge)
    val (text, color) = when (badgeInfo) {
        SizeMatchBadge.YOUR_SIZE -> badgeInfo.displayText to Color(0xFF4CAF50)
        SizeMatchBadge.CLOSE_FIT -> badgeInfo.displayText to Color(0xFF2196F3)
        SizeMatchBadge.SIZE_UP, SizeMatchBadge.SIZE_DOWN -> badgeInfo.displayText to Color(0xFFFF9800)
        null -> badge to Color.Gray
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = Color.White,
        modifier = modifier
            .background(color, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun HomeBrandFooter(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "You've reached the end",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Pull to refresh for new items",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatPrice(price: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    formatter.maximumFractionDigits = 0
    return "${formatter.format(price)}₫"
}
