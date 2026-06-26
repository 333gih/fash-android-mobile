package com.pc.fash_android_mobile.ui.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.common.CommonAestheticTagDto
import com.pc.fash_android_mobile.data.common.displayLabel
import com.pc.fash_android_mobile.data.common.CommonBrandDto
import com.pc.fash_android_mobile.data.common.CommonCountryDto
import com.pc.fash_android_mobile.data.locale.AppLocale
import com.pc.fash_android_mobile.data.explore.BrowseLocationMode
import com.pc.fash_android_mobile.data.listing.Category
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.search.FeaturedSellerItem
import com.pc.fash_android_mobile.data.search.toUserSearchResult
import com.pc.fash_android_mobile.data.user.UserSearchResult
import com.pc.fash_android_mobile.ui.components.FashAvatarCircle
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.components.FashPromoSlideDef
import com.pc.fash_android_mobile.ui.components.FashPromoSlider
import com.pc.fash_android_mobile.ui.components.StickyBottomPromoBar
import com.pc.fash_android_mobile.ui.components.ProfilePreviewEmptySlotPlaceholder
import com.pc.fash_android_mobile.ui.components.ProfilePreviewRowCaption
import com.pc.fash_android_mobile.ui.feed.FeedEmptyColumn
import com.pc.fash_android_mobile.ui.feed.FeedErrorColumn
import com.pc.fash_android_mobile.ui.feed.FeedLoadMoreFooter
import com.pc.fash_android_mobile.ui.feed.ListingGridCard
import com.pc.fash_android_mobile.ui.feed.listingMasonryAspectRatio
import com.pc.fash_android_mobile.ui.feed.listingMasonryTileSize
import com.pc.fash_android_mobile.ui.feed.rememberListingMasonryColumnWidthDp
import com.pc.fash_android_mobile.ui.guest.GuestLoginReason
import com.pc.fash_android_mobile.ui.feed.resolveListingImageUrl
import com.pc.fash_android_mobile.ui.feed.resolveProfileImageUrl
import com.pc.fash_android_mobile.ui.home.HomeBrandFooterStrip
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged


/** Filter bottom sheet: cap height so the map/list behind stays partly visible; content scrolls inside. */
private const val ExploreFilterSheetMaxHeightFraction = 0.7f

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
    /** Same promo deck as Orders / Notifications ([FashPromoSlider]); from core-service CMS only. */
    onPromoSlideClick: (FashPromoSlideDef, Int) -> Unit = { _, _ -> },
    promoSlides: List<FashPromoSlideDef> = emptyList(),
    isGuestMode: Boolean = false,
    onRequestLogin: (GuestLoginReason) -> Unit = {},
    /**
     * Opens the profile editor (or sizing onboarding) when the user accepts the
     * "Set up your size" nudge from the sizing setup sheet. Optional: when null the
     * sheet simply dismisses without routing.
     */
    onOpenSizingSetup: (() -> Unit)? = null,
    /** Opens saved shipping addresses when nearby filter needs a default address. */
    onOpenShippingAddresses: (() -> Unit)? = null,
) {
    val aestheticTagsCatalog by viewModel.aestheticTagsCatalog.collectAsState()
    val selectedAestheticTagIds by viewModel.selectedAestheticTagIds.collectAsState()
    val brands by viewModel.brands.collectAsState()
    val selectedBrandId by viewModel.selectedBrandId.collectAsState()
    val countriesCatalog by viewModel.countriesCatalog.collectAsState()
    val selectedCountryId by viewModel.selectedCountryId.collectAsState()
    val selectedCountryIso2 by viewModel.selectedCountryIso2.collectAsState()
    val sizingMode by viewModel.sizingMode.collectAsState()
    val browseLocationMode by viewModel.browseLocationMode.collectAsState()
    val manualBrowseLocation by viewModel.manualBrowseLocation.collectAsState()
    val defaultAddressLocation by viewModel.defaultAddressLocation.collectAsState()
    var showBrowseLocationPicker by rememberSaveable { mutableStateOf(false) }
    var showBrowseLocationSetupSheet by rememberSaveable { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val fashApp = remember(context) { context.applicationContext as com.pc.fash_android_mobile.FashApplication }
    var showSizingSetupSheet by rememberSaveable { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.showSizingSetupNudge.collect { showSizingSetupSheet = true }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.showBrowseLocationSetupNudge.collect { showBrowseLocationSetupSheet = true }
    }
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
        selectedAestheticTagIds,
        minPriceText,
        maxPriceText,
        conditionFilter,
        selectedBrandId,
        selectedCountryId,
        selectedCountryIso2,
        sizingMode,
        browseLocationMode,
    ) {
        selectedCategoryId != null ||
            selectedAestheticTagIds.isNotEmpty() ||
            minPriceText.isNotEmpty() ||
            maxPriceText.isNotEmpty() ||
            conditionFilter != null ||
            selectedBrandId != null ||
            selectedCountryId != null ||
            !selectedCountryIso2.isNullOrBlank() ||
            sizingMode != "all" ||
            browseLocationMode != BrowseLocationMode.Off
    }
    val filterSummaryParts = exploreFilterSummaryParts(
        categories = categories,
        selectedCategoryId = selectedCategoryId,
        aestheticTagsCatalog = aestheticTagsCatalog,
        selectedAestheticTagIds = selectedAestheticTagIds,
        brands = brands,
        selectedBrandId = selectedBrandId,
        countriesCatalog = countriesCatalog,
        selectedCountryId = selectedCountryId,
        selectedCountryIso2 = selectedCountryIso2,
        sizingMode = sizingMode,
        browseLocationMode = browseLocationMode,
        browseLocationLabel = viewModel.activeBrowseLocationLabel(),
        minPriceText = minPriceText,
        maxPriceText = maxPriceText,
        conditionFilter = conditionFilter,
    )
    val filterSummaryLine = exploreFilterSummaryLineFromParts(filterSummaryParts)
    val hasMore by viewModel.hasMore.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val quickInterestChips by viewModel.quickInterestChips.collectAsState()
    val shoppingContextChip by viewModel.shoppingContext.collectAsState()
    val selectedInterestChipNames = remember(
        quickInterestChips,
        aestheticTagsCatalog,
        selectedAestheticTagIds,
    ) {
        quickInterestChips.filter { chipName ->
            aestheticTagsCatalog.any { tag ->
                tag.id in selectedAestheticTagIds &&
                    (tag.name.equals(chipName, ignoreCase = true) ||
                        tag.displayName.equals(chipName, ignoreCase = true))
            }
        }.toSet()
    }
    val searchBarExpanded by viewModel.searchBarExpanded.collectAsState()
    val isSearchMode by viewModel.isSearchMode.collectAsState()
    val committedListingSearchQuery by viewModel.committedListingSearchQuery.collectAsState()
    val committedSellerSearchQuery by viewModel.committedSellerSearchQuery.collectAsState()
    val primarySection by viewModel.primarySection.collectAsState()
    val listingPreview by viewModel.listingPreview.collectAsState()
    var pendingListingDetail by remember { mutableStateOf<Pair<String, String?>?>(null) }
    LaunchedEffect(pendingListingDetail) {
        pendingListingDetail?.let { (listingId, sellerId) ->
            onListingClick(listingId, sellerId)
            pendingListingDetail = null
        }
    }
    val sellerBrowseResults by viewModel.sellerBrowseResults.collectAsState()
    val sellerPreviewPosts by viewModel.sellerPreviewPosts.collectAsState()
    val sellersLoading by viewModel.sellersLoading.collectAsState()
    val sellersLoadError by viewModel.sellersLoadError.collectAsState()
    val gridState = rememberLazyStaggeredGridState()
    val masonryColumnWidthDp = rememberListingMasonryColumnWidthDp()
    val sellersListState = rememberLazyListState()
    val pullState = rememberPullToRefreshState()
    val density = LocalDensity.current
    val showStickyExploreChrome by remember {
        derivedStateOf {
            val thresholdPx = with(density) { 88.dp.roundToPx() }
            when (primarySection) {
                ExplorePrimarySection.Listings -> {
                    val idx = gridState.firstVisibleItemIndex
                    val off = gridState.firstVisibleItemScrollOffset
                    idx > 0 || off > thresholdPx
                }
                ExplorePrimarySection.Sellers -> {
                    val idx = sellersListState.firstVisibleItemIndex
                    val off = sellersListState.firstVisibleItemScrollOffset
                    idx > 0 || off > thresholdPx
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.scrollExploreToTop.collect {
            when (viewModel.primarySection.value) {
                ExplorePrimarySection.Listings -> gridState.scrollToItem(0)
                ExplorePrimarySection.Sellers -> sellersListState.scrollToItem(0)
            }
        }
    }

    if (searchBarExpanded && !isSearchMode) {
        ExploreSearchOverlay(
            modifier = modifier,
            viewModel = viewModel,
        )
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = showStickyExploreChrome,
                enter = slideInVertically(
                    animationSpec = tween(240),
                    initialOffsetY = { full -> -full },
                ) + fadeIn(animationSpec = tween(240)),
                exit = slideOutVertically(
                    animationSpec = tween(200),
                    targetOffsetY = { full -> -full },
                ) + fadeOut(animationSpec = tween(200)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .zIndex(1f),
            ) {
                ExploreStickyExploreChrome(
                    primarySection = primarySection,
                    onSelectSection = viewModel::setPrimarySection,
                    hasActiveFilters = hasActiveFilters,
                    filterSummaryParts = filterSummaryParts,
                    filterSummaryLine = filterSummaryLine,
                    onOpenFilters = { showFilterSheet = true },
                    onClearFilters = if (hasActiveFilters) {
                        { viewModel.clearMarketplaceFilters() }
                    } else {
                        null
                    },
                    activeSearchQuery = when (primarySection) {
                        ExplorePrimarySection.Listings ->
                            if (isSearchMode) committedListingSearchQuery.trim() else ""
                        ExplorePrimarySection.Sellers -> committedSellerSearchQuery.trim()
                    }.takeIf { it.isNotEmpty() },
                    onClearActiveSearch = when {
                        primarySection == ExplorePrimarySection.Listings &&
                            isSearchMode && committedListingSearchQuery.isNotBlank() ->
                            ({ viewModel.clearListingSearch() })
                        primarySection == ExplorePrimarySection.Sellers &&
                            committedSellerSearchQuery.isNotBlank() ->
                            ({ viewModel.clearSellerSearch() })
                        else -> null
                    },
                )
            }
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
                when (primarySection) {
                    ExplorePrimarySection.Listings -> {
                        Column(Modifier.fillMaxSize()) {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            state = gridState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(
                                start = FashTheme.spacing.editorialStart,
                                end = FashTheme.spacing.editorialEnd,
                                top = 0.dp,
                                bottom = FashTheme.spacing.spacing3,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing2),
                            verticalItemSpacing = FashTheme.spacing.spacing2,
                        ) {
                            if (!showStickyExploreChrome) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        ExplorePrimarySectionSwitcher(
                                            selected = primarySection,
                                            onSelect = viewModel::setPrimarySection,
                                            modifier = Modifier.padding(bottom = 8.dp),
                                        )
                                        if (isSearchMode && committedListingSearchQuery.isNotBlank()) {
                                            ExploreActiveSearchQueryBanner(
                                                query = committedListingSearchQuery,
                                                onClear = { viewModel.clearListingSearch() },
                                                modifier = Modifier.padding(bottom = 8.dp),
                                            )
                                        }
                                        ExploreFiltersBar(
                                            hasActiveFilters = hasActiveFilters,
                                            filterSummaryParts = filterSummaryParts,
                                            filterSummaryLine = filterSummaryLine,
                                            includeEdgeHorizontalPadding = false,
                                            compact = true,
                                            onOpenFilters = { showFilterSheet = true },
                                            onClearFilters = if (hasActiveFilters) {
                                                { viewModel.clearMarketplaceFilters() }
                                            } else {
                                                null
                                            },
                                        )
                                    }
                                }
                                if (
                                    sizingMode.equals("match_profile", ignoreCase = true) ||
                                    browseLocationMode != BrowseLocationMode.Off ||
                                    shoppingContextChip?.chipLabel() != null
                                ) {
                                    item(span = StaggeredGridItemSpan.FullLine) {
                                        ExploreActivePersonalFilterChips(
                                            sizingActive = sizingMode.equals("match_profile", ignoreCase = true),
                                            browseLocationMode = browseLocationMode,
                                            browseLocationLabel = viewModel.activeBrowseLocationLabel(),
                                            seasonContextLabel = shoppingContextChip?.chipLabel(),
                                            onClearSizing = { viewModel.setSizingModeFilter("all") },
                                            onClearLocation = { viewModel.clearBrowseLocationFilter() },
                                            onOpenFilters = { showFilterSheet = true },
                                        )
                                    }
                                }
                                if (
                                    quickInterestChips.isNotEmpty() &&
                                    !hasActiveFilters &&
                                    !isSearchMode
                                ) {
                                    item(span = StaggeredGridItemSpan.FullLine) {
                                        ExploreInterestChipsRow(
                                            chips = quickInterestChips,
                                            selectedChipNames = selectedInterestChipNames,
                                            onChipClick = { viewModel.toggleInterestChip(it) },
                                        )
                                    }
                                }
                            }
                            when {
                                isLoading && listings.isEmpty() -> {
                                    item(span = StaggeredGridItemSpan.FullLine) {
                                        // Skeleton 2-col masonry — matches Home follow feed tile geometry.
                                        com.pc.fash_android_mobile.ui.components.FashSkeletonGrid(
                                            rows = 4,
                                            staggered = true,
                                        )
                                    }
                                }
                                loadError && listings.isEmpty() -> {
                                    item(span = StaggeredGridItemSpan.FullLine) {
                                        FeedErrorColumn(
                                            message = stringResource(R.string.feed_load_error),
                                            onRetry = { viewModel.retryLoad() },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                                listings.isEmpty() -> {
                                    item(span = StaggeredGridItemSpan.FullLine) {
                                        if (hasActiveFilters || isSearchMode) {
                                            ExploreFilteredEmptyCard(
                                                modifier = Modifier.fillMaxWidth(),
                                                onClearConstraints = { viewModel.clearExploreConstraints() },
                                                onEditFilters = { showFilterSheet = true },
                                            )
                                        } else {
                                            FeedEmptyColumn(
                                                title = stringResource(R.string.feed_empty_title),
                                                subtitle = stringResource(R.string.explore_grid_empty_subtitle),
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    itemsIndexed(
                                        listings,
                                        key = { _, item -> item.id },
                                    ) { index, item ->
                                        LaunchedEffect(item.id) {
                                            viewModel.recordView(item, position = index)
                                        }
                                        ListingGridCard(
                                            item = item,
                                            onClick = {
                                                viewModel.openListingPreview(item, position = index)
                                            },
                                            onDwell = { dwellMs ->
                                                viewModel.recordListingDwell(item, "explore", index, dwellMs)
                                            },
                                            imageAspectRatio = listingMasonryAspectRatio(item),
                                            columnWidthDp = masonryColumnWidthDp,
                                            modifier = Modifier.listingMasonryTileSize(
                                                masonryColumnWidthDp,
                                                item,
                                            ),
                                            showQuickActions = true,
                                            onLike = {
                                                if (isGuestMode) onRequestLogin(GuestLoginReason.Like)
                                                else viewModel.toggleLike(item)
                                            },
                                            onSave = {
                                                if (isGuestMode) onRequestLogin(GuestLoginReason.Saved)
                                                else viewModel.toggleSave(item)
                                            },
                                        )
                                    }
                                    if (hasMore || isLoadingMore) {
                                        item(span = StaggeredGridItemSpan.FullLine, key = "explore_load_more") {
                                            FeedLoadMoreFooter(
                                                enabled = hasMore,
                                                isLoadingMore = isLoadingMore,
                                                onLoadMore = { viewModel.loadMore() },
                                            )
                                        }
                                    }
                                    if (!hasMore && listings.isNotEmpty()) {
                                        item(span = StaggeredGridItemSpan.FullLine) {
                                            HomeBrandFooterStrip(includeHorizontalEdgePadding = false)
                                        }
                                    }
                                }
                            }
                        }
                        AnimatedVisibility(
                            visible = promoSlides.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            enter = slideInVertically(
                                animationSpec = tween(280, easing = FastOutSlowInEasing),
                                initialOffsetY = { it },
                            ) + fadeIn(animationSpec = tween(280)),
                            exit = slideOutVertically(
                                animationSpec = tween(240, easing = FastOutSlowInEasing),
                                targetOffsetY = { it },
                            ) + fadeOut(animationSpec = tween(200)),
                        ) {
                            StickyBottomPromoBar(elevated = true) {
                                FashPromoSlider(
                                    modifier = Modifier.fillMaxWidth(),
                                    slides = promoSlides,
                                    contentPadding = PaddingValues(0.dp),
                                    onSlideClick = onPromoSlideClick,
                                )
                            }
                        }
                        }
                    }
                    ExplorePrimarySection.Sellers -> {
                        ExploreSellersDiscoveryColumn(
                            state = sellersListState,
                            selectedSection = primarySection,
                            onSelectSection = viewModel::setPrimarySection,
                            featuredSellers = featuredSellers,
                            followingIds = followingIds,
                            onFeaturedSellerClick = onFeaturedSellerClick,
                            onSeeAllFeaturedSellersClick = onSeeAllFeaturedSellersClick,
                            sellerBrowseResults = sellerBrowseResults,
                            sellerPreviewPosts = sellerPreviewPosts,
                            sellersLoading = sellersLoading,
                            sellersLoadError = sellersLoadError,
                            committedSellerSearchQuery = committedSellerSearchQuery,
                            onClearSellerSearch = viewModel::clearSellerSearch,
                            onSellerClick = onFeaturedSellerClick,
                            onListingPreviewClick = { item ->
                                viewModel.openListingPreview(item, position = 0)
                            },
                            onRetrySellers = viewModel::retrySellerBrowse,
                        )
                    }
                }
            }

            if (showFilterSheet) {
                ExploreFilterBottomSheet(
                    viewModel = viewModel,
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    aestheticTagsCatalog = aestheticTagsCatalog,
                    selectedAestheticTagIds = selectedAestheticTagIds,
                    brands = brands,
                    selectedBrandId = selectedBrandId,
                    countriesCatalog = countriesCatalog,
                    selectedCountryId = selectedCountryId,
                    selectedCountryIso2 = selectedCountryIso2,
                    sizingMode = sizingMode,
                    browseLocationMode = browseLocationMode,
                    manualBrowseLocation = manualBrowseLocation,
                    defaultAddressLocation = defaultAddressLocation,
                    isGuestMode = isGuestMode,
                    onRequestLogin = onRequestLogin,
                    onOpenBrowseLocationPicker = { showBrowseLocationPicker = true },
                    onDismiss = { showFilterSheet = false },
                )
            }

            if (showSizingSetupSheet) {
                ExploreSizingSetupSheet(
                    onSetUpClick = {
                        showSizingSetupSheet = false
                        onOpenSizingSetup?.invoke()
                    },
                    onSkip = { showSizingSetupSheet = false },
                    onDismiss = { showSizingSetupSheet = false },
                )
            }

            if (showBrowseLocationSetupSheet) {
                ExploreBrowseLocationSetupSheet(
                    onAddAddressClick = {
                        showBrowseLocationSetupSheet = false
                        if (isGuestMode) {
                            onRequestLogin(GuestLoginReason.BrowseLocation)
                        } else {
                            onOpenShippingAddresses?.invoke()
                        }
                    },
                    onPickManualClick = {
                        showBrowseLocationSetupSheet = false
                        showBrowseLocationPicker = true
                    },
                    onDismiss = { showBrowseLocationSetupSheet = false },
                )
            }

            if (showBrowseLocationPicker) {
                ExploreBrowseLocationPickerSheet(
                    visible = true,
                    commonServiceRepository = fashApp.commonServiceRepository,
                    initialProvinceId = manualBrowseLocation.provinceId,
                    initialDistrictId = manualBrowseLocation.districtId,
                    initialWardId = manualBrowseLocation.wardId,
                    onDismiss = { showBrowseLocationPicker = false },
                    onConfirm = { provinceId, provinceName, districtId, districtName, wardId, wardName ->
                        showBrowseLocationPicker = false
                        viewModel.setBrowseLocationFilter(
                            provinceId = provinceId,
                            provinceName = provinceName,
                            districtId = districtId,
                            districtName = districtName,
                            wardId = wardId,
                            wardName = wardName,
                        )
                    },
                )
            }

            listingPreview?.let { preview ->
                ExploreListingPreviewSheet(
                    feedItem = preview.feedItem,
                    detail = preview.detail,
                    isDetailLoading = preview.isDetailLoading,
                    onDismiss = { viewModel.closeListingPreview() },
                    onViewDetail = {
                        val nav = viewModel.openListingDetailFromPreview()
                        if (nav != null) pendingListingDetail = nav
                    },
                    onLike = { viewModel.toggleLike(preview.feedItem) },
                    onSave = { viewModel.toggleSave(preview.feedItem) },
                    isGuestMode = isGuestMode,
                    onRequestLogin = onRequestLogin,
                    onMessageSeller = {
                        if (isGuestMode) {
                            onRequestLogin(GuestLoginReason.BuyOrChat)
                        } else {
                            val nav = viewModel.openChatFromPreview()
                            if (nav != null) pendingListingDetail = nav
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreSizingSetupSheet(
    onSetUpClick: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                .padding(
                    start = FashTheme.spacing.editorialStart,
                    end = FashTheme.spacing.editorialEnd,
                    bottom = 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.explore_sizing_setup_sheet_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
            )
            Text(
                text = stringResource(R.string.explore_sizing_setup_sheet_body),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onSetUpClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FashColors.Primary,
                    contentColor = scheme.onPrimary,
                ),
                shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin),
            ) {
                Text(text = stringResource(R.string.explore_sizing_setup_sheet_cta))
            }
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.explore_sizing_setup_sheet_skip),
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExplorePrimarySectionSwitcher(
    selected: ExplorePrimarySection,
    onSelect: (ExplorePrimarySection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = FashTheme.spacing
    val scheme = MaterialTheme.colorScheme
    val track = RoundedCornerShape(spacing.radiusPill)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = track,
        color = scheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ExploreSegmentChip(
                label = stringResource(R.string.explore_section_listings),
                selected = selected == ExplorePrimarySection.Listings,
                onClick = { onSelect(ExplorePrimarySection.Listings) },
                modifier = Modifier.weight(1f),
            )
            ExploreSegmentChip(
                label = stringResource(R.string.explore_section_sellers),
                selected = selected == ExplorePrimarySection.Sellers,
                onClick = { onSelect(ExplorePrimarySection.Sellers) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ExploreSegmentChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(FashTheme.spacing.radiusPill)
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = if (selected) scheme.onPrimary else scheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(shape)
            .background(if (selected) FashColors.Primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
    )
}

@Composable
private fun ExploreSellersDiscoveryColumn(
    state: LazyListState,
    selectedSection: ExplorePrimarySection,
    onSelectSection: (ExplorePrimarySection) -> Unit,
    featuredSellers: List<FeaturedSellerItem>,
    followingIds: Set<String>,
    onFeaturedSellerClick: (UserSearchResult) -> Unit,
    onSeeAllFeaturedSellersClick: () -> Unit,
    sellerBrowseResults: List<UserSearchResult>,
    sellerPreviewPosts: Map<String, List<ListingFeedItem>>,
    sellersLoading: Boolean,
    sellersLoadError: Boolean,
    committedSellerSearchQuery: String,
    onClearSellerSearch: () -> Unit,
    onSellerClick: (UserSearchResult) -> Unit,
    onListingPreviewClick: (ListingFeedItem) -> Unit,
    onRetrySellers: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = FashTheme.spacing
    val edge = spacing.editorialStart
    val edgeEnd = spacing.editorialEnd
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = edge,
            end = edgeEnd,
            top = 0.dp,
            bottom = spacing.spacing6,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ExplorePrimarySectionSwitcher(
                selected = selectedSection,
                onSelect = onSelectSection,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        if (committedSellerSearchQuery.isNotBlank()) {
            item {
                ExploreActiveSearchQueryBanner(
                    query = committedSellerSearchQuery,
                    onClear = onClearSellerSearch,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
        }
        item {
            Text(
                text = stringResource(R.string.explore_sellers_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        if (featuredSellers.isNotEmpty()) {
            item {
                FeaturedSellersStorySection(
                    sellers = featuredSellers,
                    followingIds = followingIds,
                    onSellerClick = { seller ->
                        onFeaturedSellerClick(seller.toUserSearchResult())
                    },
                    onSeeAllClick = onSeeAllFeaturedSellersClick,
                    compact = false,
                    includeEdgeHorizontalPadding = false,
                )
            }
        }
        when {
            sellersLoading && sellerBrowseResults.isEmpty() -> {
                item {
                    com.pc.fash_android_mobile.ui.components.FashSkeletonSellerStrip(
                        cellCount = 8,
                    )
                }
                items(count = 4) {
                    com.pc.fash_android_mobile.ui.components.FashSkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = FashTheme.spacing.editorialStart,
                                end = FashTheme.spacing.editorialEnd,
                                bottom = FashTheme.spacing.spacing3,
                            )
                            .height(220.dp),
                        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
                    )
                }
            }
            sellersLoadError && sellerBrowseResults.isEmpty() -> {
                item {
                    FeedErrorColumn(
                        message = stringResource(R.string.feed_load_error),
                        onRetry = onRetrySellers,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            !sellersLoading && sellerBrowseResults.isEmpty() -> {
                item {
                    Text(
                        text = stringResource(R.string.explore_sellers_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
            else -> {
                items(
                    sellerBrowseResults,
                    key = { it.userId.ifBlank { it.username } },
                ) { seller ->
                    val storeKey = seller.userId.trim().ifBlank { seller.username.trim() }
                    val previews = sellerPreviewPosts[storeKey]
                    ExploreSellerTikTokCard(
                        user = seller,
                        previewPosts = previews,
                        onSellerClick = { onSellerClick(seller) },
                        onListingClick = onListingPreviewClick,
                    )
                }
            }
        }
    }
}

private fun formatExploreFollowersCount(n: Int): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000f)
    n >= 1_000 -> String.format("%.1fk", n / 1_000f)
    else -> n.toString()
}

private fun formatExplorePriceVnd(vnd: Long): String =
    "₫${"%,d".format(vnd).replace(',', '.')}"

@Composable
private fun ExploreSellerNoListingsBanner() {
    val scheme = MaterialTheme.colorScheme
    val spacing = FashTheme.spacing
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(spacing.radiusSoftMin),
        color = scheme.surfaceContainerHighest,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.explore_seller_no_listings_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.explore_seller_no_listings_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ExploreSellerTikTokCard(
    user: UserSearchResult,
    /** `null` = previews still loading for this seller. */
    previewPosts: List<ListingFeedItem>?,
    onSellerClick: () -> Unit,
    onListingClick: (ListingFeedItem) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = FashTheme.spacing
    val cardShape = RoundedCornerShape(spacing.radiusSoftMin)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        color = scheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(cardShape)
                    .clickable(onClick = onSellerClick),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FashAvatarCircle(
                    imageUrl = user.avatarUrl.takeIf { it.isNotBlank() }?.let { resolveProfileImageUrl(it) },
                    contentDescription = null,
                    size = 52.dp,
                )
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    val name = user.displayName.trim().ifBlank { user.username }
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (user.username.isNotBlank()) {
                        Text(
                            text = "@${user.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    val followPart =
                        "${formatExploreFollowersCount(user.followerCount)} ${stringResource(R.string.explore_followers)}"
                    val stats =
                        if (user.listingCount > 0) {
                            "$followPart · ${stringResource(R.string.product_seller_listings, user.listingCount)}"
                        } else {
                            followPart
                        }
                    Text(
                        text = stats,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = FashColors.Primary.copy(alpha = 0.75f),
                    modifier = Modifier.size(20.dp),
                )
            }
            when {
                previewPosts == null -> {
                    com.pc.fash_android_mobile.ui.components.FashSkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp),
                        shape = RoundedCornerShape(spacing.radiusSoftMin),
                    )
                }
                previewPosts.isEmpty() -> {
                    ExploreSellerNoListingsBanner()
                }
                else -> {
                    val n = previewPosts.size
                    val totalListings = user.listingCount.coerceAtLeast(n)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (n in 1..2 && totalListings > 0) {
                            ProfilePreviewRowCaption(
                                previewCount = n,
                                totalListingCount = totalListings,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            val slots = (0 until 3).map { i -> previewPosts.getOrNull(i) }
                            slots.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp)),
                                ) {
                                    if (item != null) {
                                        val url = resolveListingImageUrl(item.coverImageUrl)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable { onListingClick(item) },
                                        ) {
                                            if (url.isNotEmpty()) {
                                                FashAsyncImage(
                                                    model = url,
                                                    contentDescription = item.title,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop,
                                                )
                                            } else {
                                                Box(
                                                    Modifier
                                                        .fillMaxSize()
                                                        .background(scheme.surfaceContainerHigh),
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .align(Alignment.BottomCenter)
                                                    .background(Color.Black.copy(alpha = 0.42f))
                                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                            ) {
                                                Text(
                                                    text = formatExplorePriceVnd(item.priceVnd),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                    ),
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                    } else {
                                        ProfilePreviewEmptySlotPlaceholder(
                                            modifier = Modifier.fillMaxSize(),
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
}

@Composable
private fun exploreFilterSummaryParts(
    categories: List<Category>,
    selectedCategoryId: String?,
    aestheticTagsCatalog: List<CommonAestheticTagDto>,
    selectedAestheticTagIds: Set<String>,
    brands: List<CommonBrandDto>,
    selectedBrandId: String?,
    countriesCatalog: List<CommonCountryDto>,
    selectedCountryId: String?,
    selectedCountryIso2: String?,
    sizingMode: String,
    browseLocationMode: BrowseLocationMode,
    browseLocationLabel: String,
    minPriceText: String,
    maxPriceText: String,
    conditionFilter: String?,
): List<String> {
    val isVi = AppLocale.currentTag(androidx.compose.ui.platform.LocalContext.current) != AppLocale.TAG_EN
    val parts = mutableListOf<String>()
    val categoryName =
        selectedCategoryId?.let { id ->
            categories.find { it.id == id }?.name?.trim()
        }
    if (!categoryName.isNullOrEmpty()) parts.add(categoryName)
    if (selectedAestheticTagIds.isNotEmpty()) {
        selectedAestheticTagIds.forEach { id ->
            val label = aestheticTagsCatalog.find { it.id == id }?.let { t ->
                t.displayLabel(isVi).trim().takeIf { it.isNotEmpty() }
            }
            if (!label.isNullOrEmpty()) parts.add(label)
        }
    }
    selectedBrandId?.let { bid ->
        brands.find { it.id == bid }?.name?.trim()?.takeIf { it.isNotEmpty() }?.let { parts.add(it) }
    }
    if (!selectedCountryId.isNullOrBlank() || !selectedCountryIso2.isNullOrBlank()) {
        val byId = selectedCountryId?.let { id -> countriesCatalog.find { it.id == id } }
        val byIso = if (byId == null && !selectedCountryIso2.isNullOrBlank()) {
            countriesCatalog.find { it.iso2.equals(selectedCountryIso2, ignoreCase = true) }
        } else {
            null
        }
        (byId ?: byIso)?.name?.trim()?.takeIf { it.isNotEmpty() }?.let { parts.add(it) }
    }
    if (sizingMode.equals("match_profile", ignoreCase = true)) {
        parts.add(stringResource(R.string.explore_filter_summary_sizing_match))
    }
    if (browseLocationMode != BrowseLocationMode.Off) {
        val label = browseLocationLabel.trim().takeIf { it.isNotEmpty() }
            ?: stringResource(R.string.explore_filter_summary_location_active)
        parts.add(label)
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
    return parts
}

@Composable
private fun exploreFilterSummaryLineFromParts(parts: List<String>): String {
    if (parts.isEmpty()) return stringResource(R.string.explore_filter_summary_default)
    return parts.joinToString(separator = " · ")
}

/**
 * Horizontal scrolling row of trending interest chips shown above the Explore grid when no
 * filters are active. Tapping a chip toggles the matching aesthetic-tag filter.
 */
@Composable
private fun ExploreInterestChipsRow(
    chips: List<String>,
    selectedChipNames: Set<String>,
    onChipClick: (tagName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (chips.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.explore_style_chips_title),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentPadding = PaddingValues(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(chips, key = { it }) { tag ->
                val selected = tag in selectedChipNames
                androidx.compose.material3.FilterChip(
                    selected = selected,
                    onClick = { onChipClick(tag) },
                    label = {
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FashColors.Primary.copy(alpha = 0.14f),
                        selectedLabelColor = FashColors.Primary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

/**
 * Trailing affordance on the filter bar: clear (active filters) or chevron (open sheet).
 * Mutually exclusive — never both at once.
 */
@Composable
private fun ExploreFiltersBarTrailingAction(
    hasActiveFilters: Boolean,
    onClearFilters: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val clearCd = stringResource(R.string.explore_filters_clear_cd)
    if (hasActiveFilters && onClearFilters != null) {
        IconButton(
            onClick = onClearFilters,
            modifier = modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = clearCd,
                tint = FashColors.Primary,
                modifier = Modifier.size(22.dp),
            )
        }
    } else {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = FashColors.Primary.copy(alpha = 0.9f),
            modifier = modifier.size(22.dp),
        )
    }
}

/** Compact reminders when personal filters are on — one slim row, not full-width cards. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExploreActivePersonalFilterChips(
    sizingActive: Boolean,
    browseLocationMode: BrowseLocationMode,
    browseLocationLabel: String,
    seasonContextLabel: String? = null,
    onClearSizing: () -> Unit,
    onClearLocation: () -> Unit,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val edge = FashTheme.spacing.editorialStart
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        contentPadding = PaddingValues(horizontal = edge),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (sizingActive) {
            item {
                ExplorePersonalFilterPill(
                    label = stringResource(R.string.explore_filter_summary_sizing_match),
                    onClear = onClearSizing,
                )
            }
        }
        if (browseLocationMode != BrowseLocationMode.Off) {
            item {
                val locLabel = browseLocationLabel.trim().takeIf { it.isNotEmpty() }
                    ?: stringResource(R.string.explore_filter_summary_location_active)
                ExplorePersonalFilterPill(
                    label = locLabel,
                    onClear = onClearLocation,
                )
            }
        }
        seasonContextLabel?.trim()?.takeIf { it.isNotEmpty() }?.let { seasonLabel ->
            item {
                ExploreSeasonContextPill(label = seasonLabel)
            }
        }
        item {
            Text(
                text = stringResource(R.string.explore_personal_filters_edit),
                style = MaterialTheme.typography.labelMedium,
                color = FashColors.Primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(FashTheme.spacing.radiusPill))
                    .clickable(onClick = onOpenFilters)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ExploreSeasonContextPill(label: String) {
    val shape = RoundedCornerShape(FashTheme.spacing.radiusPill)
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ExplorePersonalFilterPill(
    label: String,
    onClear: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(FashTheme.spacing.radiusPill)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(FashColors.Primary.copy(alpha = 0.12f))
            .clickable(onClick = onClear)
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.explore_filters_clear),
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreBrowseLocationSetupSheet(
    onAddAddressClick: () -> Unit,
    onPickManualClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.explore_location_setup_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = stringResource(R.string.explore_location_setup_body),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            Button(
                onClick = onAddAddressClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = FashColors.Primary),
            ) {
                Text(stringResource(R.string.explore_location_setup_add_address))
            }
            OutlinedButton(
                onClick = onPickManualClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.explore_location_setup_pick_manual))
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.explore_location_setup_skip))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExploreFiltersBar(
    hasActiveFilters: Boolean,
    filterSummaryParts: List<String>,
    filterSummaryLine: String,
    /** When false, horizontal padding is omitted (parent already applies grid `contentPadding`). */
    includeEdgeHorizontalPadding: Boolean = true,
    compact: Boolean = false,
    onOpenFilters: () -> Unit,
    onClearFilters: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = FashTheme.spacing
    val cd = stringResource(R.string.explore_filters_toggle_cd)
    val edgeStart = if (includeEdgeHorizontalPadding) spacing.editorialStart else 0.dp
    val edgeEnd = if (includeEdgeHorizontalPadding) spacing.editorialEnd else 0.dp
    val chipShape = RoundedCornerShape(spacing.radiusPill)
    val cardShape = RoundedCornerShape(spacing.radiusSoftMin)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = edgeStart,
                end = edgeEnd,
                top = 4.dp,
                bottom = 8.dp,
            )
            .clip(cardShape),
        shape = cardShape,
        color = scheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {
                        contentDescription = cd
                        role = Role.Button
                    }
                    .clickable(onClick = onOpenFilters),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            BadgedBox(
                badge = {
                    if (hasActiveFilters && filterSummaryParts.isNotEmpty()) {
                        Badge(
                            containerColor = FashColors.Primary,
                            contentColor = scheme.onPrimary,
                        ) {
                            Text(
                                text = filterSummaryParts.size.coerceAtMost(99).toString(),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                },
            ) {
                ExploreFilterIconPulse(active = hasActiveFilters) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = FashColors.Primary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.explore_filters_bar_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                )
                if (filterSummaryParts.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .heightIn(max = 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        filterSummaryParts.forEach { part ->
                            Text(
                                text = part,
                                style = MaterialTheme.typography.labelMedium,
                                color = FashColors.Primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .clip(chipShape)
                                    .background(FashColors.Primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                } else {
                    ExploreFilterIdleTeaser(
                        modifier = Modifier.padding(top = if (compact) 2.dp else 4.dp),
                        compact = compact,
                    )
                }
            }
            }
            ExploreFiltersBarTrailingAction(
                hasActiveFilters = hasActiveFilters,
                onClearFilters = onClearFilters,
            )
        }
    }
}

/**
 * Pinned under the app bar when the user scrolls past the first header block.
 * Always includes **Posts / Sellers** tabs; on **Posts**, the filter strip is directly under the tabs.
 */
@Composable
private fun ExploreStickyExploreChrome(
    primarySection: ExplorePrimarySection,
    onSelectSection: (ExplorePrimarySection) -> Unit,
    hasActiveFilters: Boolean,
    filterSummaryParts: List<String>,
    filterSummaryLine: String,
    onOpenFilters: () -> Unit,
    onClearFilters: (() -> Unit)? = null,
    activeSearchQuery: String? = null,
    onClearActiveSearch: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = FashTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = spacing.spacing2),
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = scheme.outlineVariant.copy(alpha = 0.38f),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.surfaceContainerLow),
        ) {
            ExplorePrimarySectionSwitcher(
                selected = primarySection,
                onSelect = onSelectSection,
                modifier = Modifier.padding(
                    start = spacing.editorialStart,
                    end = spacing.editorialEnd,
                    top = 8.dp,
                    bottom = if (primarySection == ExplorePrimarySection.Listings) 4.dp else 8.dp,
                ),
            )
            if (primarySection == ExplorePrimarySection.Listings) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = scheme.outlineVariant.copy(alpha = 0.45f),
                )
                ExploreStickyFilterRow(
                    hasActiveFilters = hasActiveFilters,
                    filterSummaryParts = filterSummaryParts,
                    filterSummaryLine = filterSummaryLine,
                    onOpenFilters = onOpenFilters,
                    onClearFilters = onClearFilters,
                )
            }
            if (!activeSearchQuery.isNullOrBlank() && onClearActiveSearch != null) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = scheme.outlineVariant.copy(alpha = 0.45f),
                )
                ExploreStickyActiveSearchRow(
                    query = activeSearchQuery,
                    onClear = onClearActiveSearch,
                )
            }
        }
    }
}

@Composable
private fun ExploreStickyActiveSearchRow(
    query: String,
    onClear: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = FashTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = spacing.editorialStart,
                end = spacing.editorialEnd,
                top = 6.dp,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = FashColors.Primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = query,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onClear) {
            Text(
                text = stringResource(R.string.explore_search_clear_active),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun ExploreActiveSearchQueryBanner(
    query: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val pill = RoundedCornerShape(FashTheme.spacing.radiusPill)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = pill,
        color = scheme.surfaceContainerHighest,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.explore_search_active_cd),
                tint = FashColors.Primary,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.explore_search_active_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = query,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onClear) {
                Text(
                    text = stringResource(R.string.explore_search_clear_active),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/** Single tappable row opening the Explore filter sheet (used under tabs in [ExploreStickyExploreChrome]). */
@Composable
private fun ExploreStickyFilterRow(
    hasActiveFilters: Boolean,
    filterSummaryParts: List<String>,
    filterSummaryLine: String,
    onOpenFilters: () -> Unit,
    onClearFilters: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = FashTheme.spacing
    val cd = stringResource(R.string.explore_filters_toggle_cd)
    val summaryCompact = when {
        filterSummaryParts.isEmpty() -> filterSummaryLine
        filterSummaryParts.size == 1 -> filterSummaryParts[0]
        else -> {
            val first = filterSummaryParts[0]
            val rest = filterSummaryParts.size - 1
            "$first · +$rest"
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLow)
            .padding(
                start = spacing.editorialStart,
                end = spacing.editorialEnd,
                top = spacing.spacing3,
                bottom = spacing.spacing3,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.spacing3),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {
                    contentDescription = cd
                    role = Role.Button
                }
                .clickable(onClick = onOpenFilters),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.spacing3),
        ) {
            BadgedBox(
                badge = {
                    if (hasActiveFilters && filterSummaryParts.isNotEmpty()) {
                        Badge(
                            containerColor = FashColors.Primary,
                            contentColor = scheme.onPrimary,
                        ) {
                            Text(
                                text = filterSummaryParts.size.coerceAtMost(99).toString(),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                },
            ) {
                ExploreFilterIconPulse(active = hasActiveFilters) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(FashColors.Primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = FashColors.Primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.explore_filters_bar_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                )
                if (hasActiveFilters) {
                    Text(
                        text = summaryCompact,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                } else {
                    ExploreFilterIdleTeaser(
                        modifier = Modifier.padding(top = 2.dp),
                        compact = true,
                    )
                }
            }
        }
        ExploreFiltersBarTrailingAction(
            hasActiveFilters = hasActiveFilters,
            onClearFilters = onClearFilters,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreFilterBottomSheet(
    viewModel: ExploreViewModel,
    categories: List<Category>,
    selectedCategoryId: String?,
    aestheticTagsCatalog: List<CommonAestheticTagDto>,
    selectedAestheticTagIds: Set<String>,
    brands: List<CommonBrandDto>,
    selectedBrandId: String?,
    countriesCatalog: List<CommonCountryDto>,
    selectedCountryId: String?,
    selectedCountryIso2: String?,
    sizingMode: String,
    browseLocationMode: BrowseLocationMode,
    manualBrowseLocation: ExploreViewModel.BrowseLocationFilter,
    defaultAddressLocation: ExploreViewModel.BrowseLocationFilter,
    isGuestMode: Boolean,
    onRequestLogin: (GuestLoginReason) -> Unit,
    onOpenBrowseLocationPicker: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showCategoryPicker by rememberSaveable { mutableStateOf(false) }
    var showBrandPicker by rememberSaveable { mutableStateOf(false) }
    var showCountryPicker by rememberSaveable { mutableStateOf(false) }
    var showAestheticPicker by rememberSaveable { mutableStateOf(false) }
    val minPriceText by viewModel.minPriceText.collectAsState()
    val maxPriceText by viewModel.maxPriceText.collectAsState()
    val conditionFilter by viewModel.selectedConditionFilter.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme
    val configuration = LocalConfiguration.current
    val maxFilterSheetHeight = remember(configuration.screenHeightDp) {
        (configuration.screenHeightDp * ExploreFilterSheetMaxHeightFraction).roundToInt().dp
    }
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
                .heightIn(max = maxFilterSheetHeight)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
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
            Text(
                text = stringResource(R.string.explore_filter_sheet_journey_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = FashTheme.spacing.editorialStart,
                    end = FashTheme.spacing.editorialEnd,
                    top = 8.dp,
                    bottom = 4.dp,
                ),
            )
            ExploreFilterHintCarousel()
            val personalActiveCount = run {
                var n = 0
                if (!sizingMode.equals("all", ignoreCase = true)) n++
                if (browseLocationMode != BrowseLocationMode.Off) n++
                n
            }
            val productActiveCount = run {
                var n = 0
                if (!selectedCategoryId.isNullOrBlank()) n++
                if (selectedAestheticTagIds.isNotEmpty()) n++
                if (!selectedBrandId.isNullOrBlank()) n++
                if (!selectedCountryId.isNullOrBlank() || !selectedCountryIso2.isNullOrBlank()) n++
                n
            }
            val priceActiveCount = run {
                var n = 0
                if (minPriceText.isNotBlank() || maxPriceText.isNotBlank()) n++
                if (!conditionFilter.isNullOrBlank()) n++
                n
            }
            ExploreFilterExpandableGroup(
                title = stringResource(R.string.explore_filter_personal_title),
                subtitle = stringResource(R.string.explore_filter_group_personal_subtitle),
                expandedInitially = true,
                activeCount = personalActiveCount,
            ) {
                ExploreSizingFilterSection(
                    sizingMode = sizingMode,
                    isGuestMode = isGuestMode,
                    onSelect = viewModel::setSizingModeFilter,
                    onRequestLogin = onRequestLogin,
                )
                ExploreBrowseLocationFilterSection(
                    mode = browseLocationMode,
                    manualLocation = manualBrowseLocation,
                    defaultAddressLocation = defaultAddressLocation,
                    isGuestMode = isGuestMode,
                    onSelectMode = viewModel::setBrowseLocationModeFilter,
                    onPickManual = onOpenBrowseLocationPicker,
                    onRequestLogin = onRequestLogin,
                )
            }
            ExploreFilterExpandableGroup(
                title = stringResource(R.string.explore_filter_group_product_title),
                subtitle = stringResource(R.string.explore_filter_group_product_subtitle),
                expandedInitially = productActiveCount > 0,
                activeCount = productActiveCount,
            ) {
                ExploreCategoryFilterRow(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onOpenPicker = { showCategoryPicker = true },
                )
                ExploreAestheticFilterRow(
                    catalog = aestheticTagsCatalog,
                    selectedIds = selectedAestheticTagIds,
                    onOpenPicker = { showAestheticPicker = true },
                )
                ExploreBrandFilterRow(
                    brands = brands,
                    selectedBrandId = selectedBrandId,
                    onOpenPicker = { showBrandPicker = true },
                )
                ExploreCountryFilterRow(
                    countries = countriesCatalog,
                    selectedCountryId = selectedCountryId,
                    selectedCountryIso2 = selectedCountryIso2,
                    onOpenPicker = { showCountryPicker = true },
                )
            }
            ExploreFilterExpandableGroup(
                title = stringResource(R.string.explore_filter_group_price_title),
                subtitle = stringResource(R.string.explore_filter_group_price_subtitle),
                expandedInitially = priceActiveCount > 0,
                activeCount = priceActiveCount,
            ) {
                ExploreMarketplaceFilters(viewModel = viewModel)
            }
        }
    }
    ExploreCategoryPickerSheet(
        visible = showCategoryPicker,
        onDismiss = { showCategoryPicker = false },
        categories = categories,
        selectedCategoryId = selectedCategoryId,
        onSelectCategory = viewModel::selectCategory,
    )
    ExploreBrandPickerSheet(
        visible = showBrandPicker,
        onDismiss = { showBrandPicker = false },
        brands = brands,
        selectedBrandId = selectedBrandId,
        onSelectBrand = viewModel::selectBrandFilter,
    )
    ExploreCountryPickerSheet(
        visible = showCountryPicker,
        onDismiss = { showCountryPicker = false },
        countries = countriesCatalog,
        selectedCountryId = selectedCountryId,
        selectedCountryIso2 = selectedCountryIso2,
        onSelectCountry = viewModel::selectCountryFilter,
    )
    ExploreAestheticTagsPickerSheet(
        visible = showAestheticPicker,
        onDismiss = { showAestheticPicker = false },
        catalog = aestheticTagsCatalog,
        selectedIds = selectedAestheticTagIds,
        onToggle = viewModel::toggleAestheticTagFilter,
        onClear = viewModel::clearAestheticTagFilters,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExploreSizingFilterSection(
    sizingMode: String,
    isGuestMode: Boolean,
    onSelect: (String) -> Unit,
    onRequestLogin: (GuestLoginReason) -> Unit,
) {
    val edge = FashTheme.spacing.editorialStart
    val isAll = sizingMode.equals("all", ignoreCase = true)
    val isMatch = !isGuestMode && sizingMode.equals("match_profile", ignoreCase = true)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = FashTheme.spacing.spacing2),
    ) {
        ExploreFilterSectionLabel(text = stringResource(R.string.explore_filter_sizing_title))
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = edge, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExploreFilterChip(
                label = stringResource(R.string.explore_filter_sizing_all),
                selected = isAll,
                onClick = { onSelect("all") },
            )
            ExploreFilterChip(
                label = stringResource(R.string.explore_filter_sizing_match_profile),
                selected = isMatch,
                onClick = {
                    if (isGuestMode) {
                        onRequestLogin(GuestLoginReason.SizingMatch)
                    } else {
                        onSelect("match_profile")
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExploreBrowseLocationFilterSection(
    mode: BrowseLocationMode,
    manualLocation: ExploreViewModel.BrowseLocationFilter,
    defaultAddressLocation: ExploreViewModel.BrowseLocationFilter,
    isGuestMode: Boolean,
    onSelectMode: (BrowseLocationMode) -> Unit,
    onPickManual: () -> Unit,
    onRequestLogin: (GuestLoginReason) -> Unit,
) {
    val edge = FashTheme.spacing.editorialStart
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = FashTheme.spacing.spacing2),
    ) {
        ExploreFilterSectionLabel(text = stringResource(R.string.explore_filter_location_title))
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = edge, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExploreFilterChip(
                label = stringResource(R.string.explore_filter_location_all),
                selected = mode == BrowseLocationMode.Off,
                onClick = { onSelectMode(BrowseLocationMode.Off) },
            )
            ExploreFilterChip(
                label = stringResource(R.string.explore_filter_location_nearby),
                selected = mode == BrowseLocationMode.NearbyDefault,
                onClick = {
                    if (isGuestMode) {
                        onRequestLogin(GuestLoginReason.BrowseLocation)
                    } else {
                        onSelectMode(BrowseLocationMode.NearbyDefault)
                    }
                },
            )
        }
        val manualSummary = if (manualLocation.hasSelection) {
            manualLocation.chipLabel
        } else {
            stringResource(R.string.explore_filter_location_manual)
        }
        FilterSelectionSummaryCard(
            summary = manualSummary,
            onClick = onPickManual,
            contentDescription = stringResource(R.string.explore_filter_location_manual_cd, manualSummary),
            modifier = Modifier.padding(horizontal = edge, vertical = 4.dp),
        )
        if (defaultAddressLocation.hasSelection && mode != BrowseLocationMode.Off) {
            Text(
                text = when (mode) {
                    BrowseLocationMode.NearbyDefault ->
                        stringResource(R.string.explore_filter_location_nearby_hint, defaultAddressLocation.chipLabel)
                    BrowseLocationMode.Manual ->
                        stringResource(R.string.explore_filter_location_manual_hint, manualLocation.chipLabel)
                    BrowseLocationMode.Off -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = edge, vertical = 4.dp),
            )
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
    val committedListingSearchQuery by viewModel.committedListingSearchQuery.collectAsState()
    // Sort always selectable now — user controls order regardless of search/filter context.
    // (Browse endpoint may ignore sort today, but the selected value is preserved for the next
    // search submission; this avoids the previous "disabled but still selectable" confusion.)
    val sortApplies = true

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
    sellers: List<FeaturedSellerItem>,
    followingIds: Set<String>,
    onSellerClick: (FeaturedSellerItem) -> Unit,
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
    seller: FeaturedSellerItem,
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

/** Empty grid when filters and/or search are active but API returns no listings — matches Fash empty/surface patterns. */
@Composable
private fun ExploreFilteredEmptyCard(
    modifier: Modifier = Modifier,
    onClearConstraints: () -> Unit,
    onEditFilters: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val spacing = FashTheme.spacing
    Surface(
        modifier = modifier.padding(top = spacing.spacing2, bottom = spacing.spacing1),
        shape = RoundedCornerShape(spacing.radiusSoftMin),
        color = scheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.spacing4, vertical = spacing.spacing5),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.spacing3),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(FashColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = FashColors.Primary,
                )
            }
            Text(
                text = stringResource(R.string.explore_empty_filtered_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.explore_grid_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.spacing2),
            ) {
                OutlinedButton(
                    onClick = onEditFilters,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(spacing.radiusSoftMin),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FashColors.Primary),
                ) {
                    Text(
                        text = stringResource(R.string.explore_empty_edit_filters),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
                TextButton(
                    onClick = onClearConstraints,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = scheme.onSurfaceVariant),
                ) {
                    Text(
                        text = stringResource(R.string.explore_empty_clear_all),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

private fun featuredSellerHandle(seller: FeaturedSellerItem): String {
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
    seller: FeaturedSellerItem,
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
    val imageUrl = seller.avatarUrl.takeIf { it.isNotBlank() }?.let { resolveProfileImageUrl(it) }

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
                )
            }
        } else {
            FashAvatarCircle(
                imageUrl = imageUrl,
                contentDescription = contentDescription,
                size = inner,
            )
        }
    }
}
