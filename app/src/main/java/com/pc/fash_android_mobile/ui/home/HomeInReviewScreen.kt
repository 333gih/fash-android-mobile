package com.pc.fash_android_mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.ui.common.stableLazyKey
import com.pc.fash_android_mobile.ui.components.FashEmptyState
import com.pc.fash_android_mobile.ui.components.FashPromoSlideDef
import com.pc.fash_android_mobile.ui.components.FashPromoSliderAdFooter
import com.pc.fash_android_mobile.ui.feed.ListingGridCard
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

/**
 * Full-screen hub from Home “Đang duyệt” — seller listings awaiting admin approval (`in_review`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeInReviewScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeInReviewViewModel,
    onBack: () -> Unit,
    onListingClick: (ListingFeedItem) -> Unit,
    onOpenPostListing: () -> Unit,
    onDataMutated: () -> Unit = {},
    promoSlides: List<FashPromoSlideDef> = emptyList(),
    onPromoSlideClick: (FashPromoSlideDef, Int) -> Unit = { _, _ -> },
) {
    val scheme = MaterialTheme.colorScheme
    val listings by viewModel.listings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val pullState = rememberPullToRefreshState()
    val statusLabel = stringResource(R.string.listing_status_in_review)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = scheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
            ),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.home_in_review_screen_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.orders_back),
                                tint = FashColors.Primary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = scheme.surface,
                        titleContentColor = scheme.onSurface,
                    ),
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(scheme.surfaceContainerLow),
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    viewModel.refresh()
                    onDataMutated()
                },
                modifier = Modifier.fillMaxSize(),
                state = pullState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullState,
                        isRefreshing = isRefreshing,
                        color = FashColors.Primary,
                        containerColor = scheme.surface,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                },
            ) {
                when {
                    isLoading && listings.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = FashColors.Primary)
                        }
                    }
                    loadError != null && listings.isEmpty() -> {
                        FashEmptyState(
                            icon = Icons.Outlined.ErrorOutline,
                            title = stringResource(R.string.home_in_review_error_title),
                            subtitle = loadError?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.home_in_review_error_subtitle),
                            modifier = Modifier.fillMaxSize(),
                            scrollable = false,
                            footer = {
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        viewModel.refresh()
                                        onDataMutated()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FashColors.Primary),
                                    shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
                                ) {
                                    Text(stringResource(R.string.feed_retry))
                                }
                            },
                        )
                    }
                    listings.isEmpty() -> {
                        FashEmptyState(
                            icon = Icons.Outlined.RateReview,
                            title = stringResource(R.string.home_in_review_empty_title),
                            subtitle = stringResource(R.string.home_in_review_empty_subtitle),
                            modifier = Modifier.fillMaxSize(),
                            scrollable = false,
                            footer = {
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = onOpenPostListing,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FashColors.Primary),
                                    shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
                                ) {
                                    Text(stringResource(R.string.home_in_review_cta_post))
                                }
                            },
                        )
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = FashTheme.spacing.editorialStart,
                                vertical = 16.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                                Text(
                                    text = stringResource(R.string.home_in_review_list_intro),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                            itemsIndexed(
                                items = listings,
                                key = { index, item -> stableLazyKey(item.id, index, "rev") },
                            ) { _, item ->
                                ListingGridCard(
                                    item = item,
                                    onClick = { onListingClick(item) },
                                    statusOverlayLabel = statusLabel,
                                )
                            }
                        }
                    }
                }
            }
                }

                FashPromoSliderAdFooter(
                    modifier = Modifier.fillMaxWidth(),
                    slides = promoSlides,
                    onSlideClick = onPromoSlideClick,
                )
            }
        }
    }
}
