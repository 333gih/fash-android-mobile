package com.pc.fash_android_mobile.ui.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.Category
import com.pc.fash_android_mobile.data.user.UserSearchResult
import com.pc.fash_android_mobile.ui.components.FashAvatarCircle
import com.pc.fash_android_mobile.ui.feed.FeedEmptyColumn
import com.pc.fash_android_mobile.ui.feed.FeedErrorColumn
import com.pc.fash_android_mobile.ui.feed.ListingGridCard
import com.pc.fash_android_mobile.ui.feed.resolveListingImageUrl
import com.pc.fash_android_mobile.ui.home.HomeBrandFooterStrip
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import kotlinx.coroutines.flow.distinctUntilChanged

/** Taller tiles on Explore so listing art isn’t read as thin strips (3:5 portrait). */
private val ExploreListingTileAspectRatio = 3f / 5f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel,
    onListingClick: (listingId: String, sellerId: String?) -> Unit = { _, _ -> },
    /** Tap a story-style featured seller chip (e.g. open profile when wired). */
    onFeaturedSellerClick: (UserSearchResult) -> Unit = {},
    /** “See all” in the featured sellers header. */
    onSeeAllFeaturedSellersClick: () -> Unit = {},
) {
    val tags by viewModel.tags.collectAsState()
    val selectedTagIndex by viewModel.selectedTagIndex.collectAsState()
    val featuredSellers by viewModel.featuredSellers.collectAsState()
    val listings by viewModel.listings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val followingIds by viewModel.followingIds.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    val minPriceText by viewModel.minPriceText.collectAsState()
    val maxPriceText by viewModel.maxPriceText.collectAsState()
    val conditionFilter by viewModel.selectedConditionFilter.collectAsState()
    val hasActiveFilters = remember(
        selectedCategoryId,
        selectedTagIndex,
        minPriceText,
        maxPriceText,
        conditionFilter,
    ) {
        selectedCategoryId != null ||
            selectedTagIndex > 0 ||
            minPriceText.isNotEmpty() ||
            maxPriceText.isNotEmpty() ||
            conditionFilter != null
    }
    val filterSummaryLine = exploreFilterSummaryString(
        categories = categories,
        selectedCategoryId = selectedCategoryId,
        tags = tags,
        selectedTagIndex = selectedTagIndex,
        minPriceText = minPriceText,
        maxPriceText = maxPriceText,
        conditionFilter = conditionFilter,
    )
    val hasMore by viewModel.hasMore.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val searchBarExpanded by viewModel.searchBarExpanded.collectAsState()
    val isSearchMode by viewModel.isSearchMode.collectAsState()
    val gridState = rememberLazyGridState()
    val pullState = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        viewModel.scrollExploreToTop.collect {
            gridState.scrollToItem(0)
        }
    }

    if (searchBarExpanded && !isSearchMode) {
        ExploreSearchOverlay(
            modifier = modifier,
            viewModel = viewModel,
        )
    } else {
        LaunchedEffect(gridState) {
            snapshotFlow {
                val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible to listings.size
            }
                .distinctUntilChanged()
                .collect { (lastVisible, n) ->
                    if (n <= 0 || lastVisible < n - 3) return@collect
                    viewModel.loadMore()
                }
        }

        Box(modifier = modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
                state = pullState,
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
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 172.dp),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = FashTheme.spacing.editorialStart,
                        end = FashTheme.spacing.editorialEnd,
                        top = 0.dp,
                        bottom = FashTheme.spacing.spacing6,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing2),
                    verticalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing4),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.explore_feed_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 8.dp),
                            )
                            ExplorePromoCarousel(
                                pagerContentPadding = PaddingValues(0.dp),
                            )
                            ExploreFiltersBar(
                                hasActiveFilters = hasActiveFilters,
                                filterSummaryLine = filterSummaryLine,
                                includeEdgeHorizontalPadding = false,
                                onOpenFilters = { showFilterSheet = true },
                            )
                            if (featuredSellers.isNotEmpty()) {
                                FeaturedSellersStorySection(
                                    sellers = featuredSellers,
                                    followingIds = followingIds,
                                    onSellerClick = onFeaturedSellerClick,
                                    onSeeAllClick = onSeeAllFeaturedSellersClick,
                                    compact = false,
                                    includeEdgeHorizontalPadding = false,
                                )
                            }
                        }
                    }
                    when {
                        isLoading && tags.isEmpty() && listings.isEmpty() -> {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(color = FashColors.Primary)
                                }
                            }
                        }
                        loadError && listings.isEmpty() -> {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                FeedErrorColumn(
                                    message = stringResource(R.string.feed_load_error),
                                    onRetry = { viewModel.retryLoad() },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        listings.isEmpty() -> {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                FeedEmptyColumn(
                                    title = stringResource(R.string.feed_empty_title),
                                    subtitle = stringResource(R.string.explore_grid_empty_subtitle),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        else -> {
                            itemsIndexed(
                                listings,
                                key = { _, item -> item.id },
                            ) { _, item ->
                                LaunchedEffect(item.id) {
                                    viewModel.recordView(item)
                                }
                                ListingGridCard(
                                    item = item,
                                    onClick = { onListingClick(item.id, item.sellerId) },
                                    imageAspectRatio = ExploreListingTileAspectRatio,
                                )
                            }
                            if (isLoadingMore) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            color = FashColors.Primary,
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                }
                            }
                            if (!hasMore && listings.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    HomeBrandFooterStrip(includeHorizontalEdgePadding = false)
                                }
                            }
                        }
                    }
                }
            }

            if (showFilterSheet) {
                ExploreFilterBottomSheet(
                    viewModel = viewModel,
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    tags = tags,
                    selectedTagIndex = selectedTagIndex,
                    onDismiss = { showFilterSheet = false },
                )
            }
        }
    }
}

@Composable
private fun exploreFilterSummaryString(
    categories: List<Category>,
    selectedCategoryId: String?,
    tags: List<String>,
    selectedTagIndex: Int,
    minPriceText: String,
    maxPriceText: String,
    conditionFilter: String?,
): String {
    val default = stringResource(R.string.explore_filter_summary_default)
    val parts = mutableListOf<String>()
    val categoryName =
        selectedCategoryId?.let { id ->
            categories.find { it.id == id }?.name?.trim()
        }
    if (!categoryName.isNullOrEmpty()) parts.add(categoryName)
    if (selectedTagIndex > 0) {
        val tag = tags.getOrNull(selectedTagIndex - 1)?.trim()
        if (!tag.isNullOrEmpty()) parts.add(tag)
    }
    val min = minPriceText.trim()
    val max = maxPriceText.trim()
    when {
        min.isNotEmpty() && max.isNotEmpty() ->
            parts.add(stringResource(R.string.explore_filter_summary_price_range, min, max))
        min.isNotEmpty() ->
            parts.add(stringResource(R.string.explore_filter_summary_price_min, min))
        max.isNotEmpty() ->
            parts.add(stringResource(R.string.explore_filter_summary_price_max, max))
    }
    conditionFilter?.let { cond ->
        val opt = exploreConditionChipOptions.find { it.apiValue == cond }
        if (opt != null) parts.add(stringResource(opt.labelRes))
    }
    return if (parts.isEmpty()) default else parts.joinToString(separator = " · ")
}

@Composable
private fun ExploreFiltersBar(
    hasActiveFilters: Boolean,
    filterSummaryLine: String,
    /** When false, horizontal padding is omitted (parent already applies grid `contentPadding`). */
    includeEdgeHorizontalPadding: Boolean = true,
    onOpenFilters: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = FashTheme.spacing
    val subtitleRes =
        if (hasActiveFilters) R.string.explore_filters_bar_subtitle_active
        else R.string.explore_filters_bar_subtitle_idle
    val cd = stringResource(R.string.explore_filters_toggle_cd)
    val edgeStart = if (includeEdgeHorizontalPadding) spacing.editorialStart else 0.dp
    val edgeEnd = if (includeEdgeHorizontalPadding) spacing.editorialEnd else 0.dp
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = edgeStart,
                end = edgeEnd,
                top = 4.dp,
                bottom = 8.dp,
            )
            .clip(RoundedCornerShape(12.dp))
            .semantics(mergeDescendants = true) {
                contentDescription = cd
                role = Role.Button
            }
            .clickable(onClick = onOpenFilters),
        shape = RoundedCornerShape(12.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.88f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                tint = FashColors.Primary,
                modifier = Modifier.size(26.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.explore_filters_bar_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                )
                Text(
                    text = filterSummaryLine,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = scheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = stringResource(subtitleRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = FashColors.Primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreFilterBottomSheet(
    viewModel: ExploreViewModel,
    categories: List<Category>,
    selectedCategoryId: String?,
    tags: List<String>,
    selectedTagIndex: Int,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = scheme.surface,
        contentColor = scheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = FashTheme.spacing.editorialStart,
                        end = FashTheme.spacing.editorialEnd,
                        top = 4.dp,
                        bottom = 8.dp,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.explore_filters_bar_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                )
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.explore_filter_sheet_done),
                        color = FashColors.Primary,
                    )
                }
            }
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.65f))
            ExploreCategoryStrip(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onSelectCategory = viewModel::selectCategory,
            )
            ExploreStyleTagSection(
                tags = tags,
                selectedTagIndex = selectedTagIndex,
                onSelectTag = viewModel::selectTag,
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = scheme.outlineVariant.copy(alpha = 0.58f),
            )
            ExploreMarketplaceFilters(viewModel = viewModel)
        }
    }
}

@Composable
private fun ExploreStyleTagSection(
    tags: List<String>,
    selectedTagIndex: Int,
    onSelectTag: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val edge = FashTheme.spacing.editorialStart
    val edgeEnd = FashTheme.spacing.editorialEnd
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.explore_style_section_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurface,
            modifier = Modifier.padding(start = edge, end = edgeEnd, bottom = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = edge, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExploreFilterChip(
                label = stringResource(R.string.explore_style_any),
                selected = selectedTagIndex == 0,
                onClick = { onSelectTag(0) },
            )
            tags.forEachIndexed { index, tag ->
                val label = tag.trim()
                if (label.isEmpty()) return@forEachIndexed
                ExploreFilterChip(
                    label = label,
                    selected = selectedTagIndex == index + 1,
                    onClick = { onSelectTag(index + 1) },
                )
            }
        }
    }
}

@Composable
private fun ExploreFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(FashTheme.spacing.radiusPill)
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) scheme.onPrimary else scheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(shape)
            .background(
                if (selected) FashColors.Primary
                else scheme.surfaceContainerHigh,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

private data class ExploreConditionChipOption(
    val apiValue: String?,
    val labelRes: Int,
)

private val exploreConditionChipOptions = listOf(
    ExploreConditionChipOption(null, R.string.explore_filter_condition_any),
    ExploreConditionChipOption("new", R.string.condition_new),
    ExploreConditionChipOption("like_new", R.string.condition_like_new),
    ExploreConditionChipOption("good", R.string.condition_good),
    ExploreConditionChipOption("fair", R.string.condition_fair),
)

private data class ExploreSortChipOption(
    val api: String,
    val labelRes: Int,
)

private val exploreSortChipOptions = listOf(
    ExploreSortChipOption("recent", R.string.explore_sort_recent),
    ExploreSortChipOption("popular", R.string.explore_sort_popular),
    ExploreSortChipOption("price_asc", R.string.explore_sort_price_low),
    ExploreSortChipOption("price_desc", R.string.explore_sort_price_high),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ExploreMarketplaceFilters(viewModel: ExploreViewModel) {
    val minPrice by viewModel.minPriceText.collectAsState()
    val maxPrice by viewModel.maxPriceText.collectAsState()
    val condition by viewModel.selectedConditionFilter.collectAsState()
    val sort by viewModel.sortOption.collectAsState()
    val isSearchMode by viewModel.isSearchMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortApplies = isSearchMode && searchQuery.isNotBlank()

    val scheme = MaterialTheme.colorScheme
    val edge = FashTheme.spacing.editorialStart
    val edgeEnd = FashTheme.spacing.editorialEnd
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = FashColors.Primary.copy(alpha = 0.55f),
        unfocusedBorderColor = scheme.outline.copy(alpha = 0.45f),
        focusedContainerColor = scheme.surfaceVariant.copy(alpha = 0.35f),
        unfocusedContainerColor = scheme.surfaceVariant.copy(alpha = 0.35f),
        cursorColor = FashColors.Primary,
        focusedTextColor = scheme.onSurface,
        unfocusedTextColor = scheme.onSurface,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
    ) {
        Text(
            text = stringResource(R.string.explore_filter_price_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurface,
            modifier = Modifier.padding(start = edge, end = edgeEnd, bottom = 6.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = edge, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = minPrice,
                onValueChange = viewModel::setMinPriceText,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                leadingIcon = {
                    Text(
                        text = "₫",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant,
                    )
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.explore_filter_min_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
            )
            OutlinedTextField(
                value = maxPrice,
                onValueChange = viewModel::setMaxPriceText,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                leadingIcon = {
                    Text(
                        text = "₫",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant,
                    )
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.explore_filter_max_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
            )
        }
        if (minPrice.isNotEmpty() || maxPrice.isNotEmpty()) {
            TextButton(
                onClick = { viewModel.clearPriceFilters() },
                modifier = Modifier.padding(start = edge, top = 2.dp),
            ) {
                Text(
                    text = stringResource(R.string.explore_filter_clear_price),
                    style = MaterialTheme.typography.labelLarge,
                    color = FashColors.Primary,
                )
            }
        }

        Text(
            text = stringResource(R.string.explore_filter_condition_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurface,
            modifier = Modifier.padding(start = edge, end = edgeEnd, top = 12.dp, bottom = 8.dp),
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = edge, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            exploreConditionChipOptions.forEach { opt ->
                val selected = opt.apiValue == condition
                Text(
                    text = stringResource(opt.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) scheme.onPrimary else scheme.onSurface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(FashTheme.spacing.radiusPill))
                        .background(
                            if (selected) FashColors.Primary
                            else scheme.surfaceContainerHigh,
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .clickable { viewModel.selectConditionFilter(opt.apiValue) },
                )
            }
        }

        Text(
            text = stringResource(R.string.explore_filter_sort_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurface,
            modifier = Modifier.padding(start = edge, end = edgeEnd, top = 8.dp, bottom = 4.dp),
        )
        if (!sortApplies) {
            Text(
                text = stringResource(R.string.explore_sort_disabled_hint),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(start = edge, end = edgeEnd, bottom = 6.dp),
            )
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = edge, vertical = 2.dp)
                .alpha(if (sortApplies) 1f else 0.55f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            exploreSortChipOptions.forEach { opt ->
                val selected = sort == opt.api
                Text(
                    text = stringResource(opt.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) scheme.onPrimary else scheme.onSurface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(FashTheme.spacing.radiusPill))
                        .background(
                            if (selected) FashColors.Primary
                            else scheme.surfaceContainerHigh,
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .clickable(
                            enabled = sortApplies,
                            onClick = { viewModel.selectSortOption(opt.api) },
                        ),
                )
            }
        }
    }
}

/** Inner avatar diameter; ring + gap sized to match story-style reference. */
private val FeaturedSellerStoryAvatarInner = 64.dp
private val FeaturedSellerStoryAvatarInnerCompact = 48.dp
private val FeaturedSellerStoryRingStroke = 2.dp
private val FeaturedSellerStoryRingStrokeCompact = 1.5.dp
private val FeaturedSellerStoryRingGap = 3.dp
private val FeaturedSellerStoryRingGapCompact = 2.dp

@Composable
private fun FeaturedSellersStorySection(
    sellers: List<UserSearchResult>,
    followingIds: Set<String>,
    onSellerClick: (UserSearchResult) -> Unit,
    onSeeAllClick: () -> Unit,
    /** Tighter layout for Explore so the product grid can use most of the screen. */
    compact: Boolean = false,
    /** When false, horizontal padding is omitted (parent already applies grid `contentPadding`). */
    includeEdgeHorizontalPadding: Boolean = true,
) {
    val spacing = FashTheme.spacing
    val edgeStart = if (includeEdgeHorizontalPadding) spacing.editorialStart else 0.dp
    val edgeEnd = if (includeEdgeHorizontalPadding) spacing.editorialEnd else 0.dp
    val headerStyle =
        if (compact) {
            MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        } else {
            MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
        }
    val headerTop = if (compact) spacing.spacing2 else spacing.spacing3
    val headerBottom = if (compact) spacing.spacing1 else spacing.spacing2
    val rowBottomPad = if (compact) spacing.spacing2 else spacing.spacing4
    val chipSpacing = if (compact) spacing.spacing3 else spacing.spacing4
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = edgeStart,
                    end = edgeEnd,
                    top = headerTop,
                    bottom = headerBottom,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.explore_featured_sellers),
                style = headerStyle,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.explore_see_all),
                style = MaterialTheme.typography.labelLarge,
                color = FashColors.Primary,
                modifier = Modifier.clickable(onClick = onSeeAllClick),
            )
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = edgeStart,
                end = edgeEnd,
                bottom = rowBottomPad,
            ),
            horizontalArrangement = Arrangement.spacedBy(chipSpacing),
            verticalAlignment = Alignment.Top,
        ) {
            itemsIndexed(
                sellers,
                key = { _, s -> s.userId.ifBlank { s.username } },
            ) { _, seller ->
                val isFollowing =
                    followingIds.contains(seller.userId) || followingIds.contains(seller.username)
                FeaturedSellerStoryItem(
                    seller = seller,
                    showAccentRing = !isFollowing,
                    onClick = { onSellerClick(seller) },
                    compact = compact,
                )
            }
        }
    }
}

@Composable
private fun FeaturedSellerStoryItem(
    seller: UserSearchResult,
    showAccentRing: Boolean,
    onClick: () -> Unit,
    compact: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val handleText = featuredSellerHandle(seller)
    val labelForCd = seller.username.ifBlank { seller.displayName }.ifBlank { handleText }
    val inner = if (compact) FeaturedSellerStoryAvatarInnerCompact else FeaturedSellerStoryAvatarInner
    val ring = if (compact) FeaturedSellerStoryRingStrokeCompact else FeaturedSellerStoryRingStroke
    val gap = if (compact) FeaturedSellerStoryRingGapCompact else FeaturedSellerStoryRingGap

    Column(
        modifier = Modifier
            .widthIn(min = if (compact) 56.dp else 72.dp, max = if (compact) 72.dp else 88.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FeaturedSellerStoryAvatar(
            seller = seller,
            showAccentRing = showAccentRing,
            contentDescription = stringResource(R.string.explore_featured_seller_cd, labelForCd),
            innerSize = inner,
            ringStroke = ring,
            ringGap = gap,
        )
        Spacer(
            modifier = Modifier.height(
                if (compact) FashTheme.spacing.spacing1 else FashTheme.spacing.spacing2,
            ),
        )
        Text(
            text = handleText,
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

private fun featuredSellerHandle(seller: UserSearchResult): String {
    val username = seller.username.trim()
    if (username.isNotBlank()) return "@$username"
    val dn = seller.displayName.trim()
    if (dn.isNotBlank()) {
        val handle = dn.split(Regex("\\s+")).joinToString("_").take(22)
        return "@$handle"
    }
    return "@..."
}

@Composable
private fun FeaturedSellerStoryAvatar(
    seller: UserSearchResult,
    showAccentRing: Boolean,
    contentDescription: String,
    innerSize: Dp = FeaturedSellerStoryAvatarInner,
    ringStroke: Dp = FeaturedSellerStoryRingStroke,
    ringGap: Dp = FeaturedSellerStoryRingGap,
) {
    val scheme = MaterialTheme.colorScheme
    val inner = innerSize
    val ring = ringStroke
    val gap = ringGap
    val outer = inner + ring * 2 + gap * 2
    val imageUrl = seller.avatarUrl.takeIf { it.isNotBlank() }?.let { resolveListingImageUrl(it) }
    val initial =
        (seller.displayName.firstOrNull() ?: seller.username.firstOrNull())?.takeIf { it.isLetter() }

    Box(
        modifier = Modifier.size(outer),
        contentAlignment = Alignment.Center,
    ) {
        if (showAccentRing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(ring, FashColors.Primary, CircleShape)
                    .padding(gap)
                    .background(scheme.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                FashAvatarCircle(
                    imageUrl = imageUrl,
                    contentDescription = contentDescription,
                    size = inner,
                    fallbackInitial = initial,
                )
            }
        } else {
            FashAvatarCircle(
                imageUrl = imageUrl,
                contentDescription = contentDescription,
                size = inner,
                fallbackInitial = initial,
            )
        }
    }
}
