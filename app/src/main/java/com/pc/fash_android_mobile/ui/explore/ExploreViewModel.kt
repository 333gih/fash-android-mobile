package com.pc.fash_android_mobile.ui.explore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.common.CategoryTreeNode
import com.pc.fash_android_mobile.data.common.CommonAestheticTagDto
import com.pc.fash_android_mobile.data.common.CommonBrandDto
import com.pc.fash_android_mobile.data.common.CommonCountryDto
import com.pc.fash_android_mobile.data.listing.Category
import com.pc.fash_android_mobile.data.listing.ListingDetail
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.ui.feed.FeedLoadMoreThrottle
import com.pc.fash_android_mobile.ui.components.emitSnackbarMessage
import com.pc.fash_android_mobile.ui.feed.FeedListingImagePrefetch
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.realtime.RealtimeEvent
import com.pc.fash_android_mobile.data.realtime.RealtimeManager
import com.pc.fash_android_mobile.data.common.CommonServiceRepository
import com.pc.fash_android_mobile.data.explore.BrowseLocationMode
import com.pc.fash_android_mobile.data.explore.ExploreBrowseLocationPreference
import com.pc.fash_android_mobile.data.explore.ExploreSizingPreference
import com.pc.fash_android_mobile.data.search.FeaturedSellerItem
import com.pc.fash_android_mobile.data.recommendation.FeedEventReporter
import com.pc.fash_android_mobile.data.recommendation.ShoppingContext
import com.pc.fash_android_mobile.data.search.SearchRepository
import com.pc.fash_android_mobile.data.search.TrendingQueryItem
import com.pc.fash_android_mobile.data.user.UserRepository
import com.pc.fash_android_mobile.data.user.UserSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

private const val ExploreFeedPageSize = 20
private const val ExploreStaleThresholdMs = 60_000L

/** Top-level Explore: product grid vs seller discovery (same tab, clear mental model). */
enum class ExplorePrimarySection {
    Listings,
    Sellers,
}

/** Half-screen quick look — feed row plus optional enriched detail from GET /listings/:id. */
data class ExploreListingPreviewState(
    val feedItem: ListingFeedItem,
    val gridPosition: Int = 0,
    /** Feed rail surface for recommendation attribution (explore, for_you, …). */
    val surface: String = "explore",
    val openedAtMs: Long = System.currentTimeMillis(),
    /** True after user opens PDP/chat from preview — suppresses preview_dismiss. */
    val outcomeRecorded: Boolean = false,
    val detail: ListingDetail? = null,
    val isDetailLoading: Boolean = false,
)

class ExploreViewModel(application: Application) : AndroidViewModel(application) {

    private val fashApp: FashApplication = application as FashApplication
    private val listingRepository: ListingRepository = fashApp.listingRepository
    private val searchRepository: SearchRepository = fashApp.searchRepository
    private fun isGuestBrowse(): Boolean = fashApp.isGuestBrowseActive
    private val userRepository: UserRepository =
        (application as FashApplication).userRepository
    private val userShippingAddressRepository =
        (application as FashApplication).userShippingAddressRepository
    private val commonServiceRepository: CommonServiceRepository =
        (application as FashApplication).commonServiceRepository
    private val realtimeManager: RealtimeManager =
        (application as FashApplication).realtimeManager

    private val feedEventReporter = FeedEventReporter(
        repository = fashApp.recommendationRepository,
        sessionIdProvider = {
            val uid = fashApp.authManager.sessionStore.read()?.userId
            if (!uid.isNullOrBlank()) fashApp.browseSessionStore.sessionIdForUser(uid)
            else fashApp.browseSessionStore.sessionId()
        },
        publicBrowse = { isGuestBrowse() },
        scope = viewModelScope,
    )

    /** Full aesthetic tag catalog from common-service (filter sheet). */
    private val _aestheticTagsCatalog = MutableStateFlow<List<CommonAestheticTagDto>>(emptyList())
    val aestheticTagsCatalog: StateFlow<List<CommonAestheticTagDto>> = _aestheticTagsCatalog.asStateFlow()

    /** OR filter — listing must match any selected tag. */
    private val _selectedAestheticTagIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedAestheticTagIds: StateFlow<Set<String>> = _selectedAestheticTagIds.asStateFlow()

    private val _brands = MutableStateFlow<List<CommonBrandDto>>(emptyList())
    val brands: StateFlow<List<CommonBrandDto>> = _brands.asStateFlow()

    private val _selectedBrandId = MutableStateFlow<String?>(null)
    val selectedBrandId: StateFlow<String?> = _selectedBrandId.asStateFlow()

    private val _countriesCatalog = MutableStateFlow<List<CommonCountryDto>>(emptyList())
    val countriesCatalog: StateFlow<List<CommonCountryDto>> = _countriesCatalog.asStateFlow()

    private val _selectedCountryId = MutableStateFlow<String?>(null)
    val selectedCountryId: StateFlow<String?> = _selectedCountryId.asStateFlow()

    private val _selectedCountryIso2 = MutableStateFlow<String?>(null)
    val selectedCountryIso2: StateFlow<String?> = _selectedCountryIso2.asStateFlow()

    /**
     * `all` (default) or `match_profile` — passed as `sizing_mode` when not `all`.
     *
     * Seeded from [ExploreSizingPreference] so the user's choice survives process death
     * (the toggle is sticky across sessions, not just within a single app lifecycle).
     */
    private val _sizingMode = MutableStateFlow(ExploreSizingPreference.read(application))
    val sizingMode: StateFlow<String> = _sizingMode.asStateFlow()

    private val _featuredSellers = MutableStateFlow<List<FeaturedSellerItem>>(emptyList())
    val featuredSellers: StateFlow<List<FeaturedSellerItem>> = _featuredSellers.asStateFlow()

    private val _primarySection = MutableStateFlow(ExplorePrimarySection.Listings)
    val primarySection: StateFlow<ExplorePrimarySection> = _primarySection.asStateFlow()

    /** Seller tab: discover users (seed search) + storefront preview posts per seller. */
    private val _sellerBrowseResults = MutableStateFlow<List<UserSearchResult>>(emptyList())
    val sellerBrowseResults: StateFlow<List<UserSearchResult>> = _sellerBrowseResults.asStateFlow()

    /** `user_id` → up to 3 active listings for TikTok-style preview strip. */
    private val _sellerPreviewPosts = MutableStateFlow<Map<String, List<ListingFeedItem>>>(emptyMap())
    val sellerPreviewPosts: StateFlow<Map<String, List<ListingFeedItem>>> = _sellerPreviewPosts.asStateFlow()

    private val _sellersLoading = MutableStateFlow(false)
    val sellersLoading: StateFlow<Boolean> = _sellersLoading.asStateFlow()

    private val _sellersLoadError = MutableStateFlow(false)
    val sellersLoadError: StateFlow<Boolean> = _sellersLoadError.asStateFlow()

    private val sellersBrowseGeneration = AtomicInteger(0)

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    /** `null` = all categories (no `category_id` filter). */
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _listings = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val listings: StateFlow<List<ListingFeedItem>> = _listings.asStateFlow()

    private val _listingPreview = MutableStateFlow<ExploreListingPreviewState?>(null)
    val listingPreview: StateFlow<ExploreListingPreviewState?> = _listingPreview.asStateFlow()
    private var listingPreviewDetailJob: Job? = null

    /** More pages available (`GET /search/listings` returned a full page). */
    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _loadError = MutableStateFlow(false)
    val loadError: StateFlow<Boolean> = _loadError.asStateFlow()

    private val _followingIds = MutableStateFlow<Set<String>>(emptySet())
    val followingIds: StateFlow<Set<String>> = _followingIds.asStateFlow()
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    /** Bottom nav: tap Explore again while Explore is already selected — scroll feed to top. */
    private val _scrollExploreToTop = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scrollExploreToTop: SharedFlow<Unit> = _scrollExploreToTop.asSharedFlow()

    /** Top bar: expanded search field (from any tab’s search icon or Explore’s search). */
    private val _searchBarExpanded = MutableStateFlow(false)
    val searchBarExpanded: StateFlow<Boolean> = _searchBarExpanded.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** True when the user ran a text search (non-browse). */
    private val _isSearchMode = MutableStateFlow(false)
    val isSearchMode: StateFlow<Boolean> = _isSearchMode.asStateFlow()

    /** Last submitted **Posts** search text — survives collapsing the search bar (used for API + UI banner). */
    private val _committedListingSearchQuery = MutableStateFlow("")
    val committedListingSearchQuery: StateFlow<String> = _committedListingSearchQuery.asStateFlow()

    /** Last submitted **Sellers** search text — survives collapsing the bar (used for API + UI banner). */
    private val _committedSellerSearchQuery = MutableStateFlow("")
    val committedSellerSearchQuery: StateFlow<String> = _committedSellerSearchQuery.asStateFlow()

    /** Search hub (expanded bar, no submitted query yet): recent / trending / tags from search endpoints. */
    private val _searchOverlayRecentQueries = MutableStateFlow<List<String>>(emptyList())
    val searchOverlayRecentQueries: StateFlow<List<String>> = _searchOverlayRecentQueries.asStateFlow()

    private val _searchOverlayTrendingQueries = MutableStateFlow<List<TrendingQueryItem>>(emptyList())
    val searchOverlayTrendingQueries: StateFlow<List<TrendingQueryItem>> = _searchOverlayTrendingQueries.asStateFlow()

    private val _searchOverlayTrendingTags = MutableStateFlow<List<String>>(emptyList())
    val searchOverlayTrendingTags: StateFlow<List<String>> = _searchOverlayTrendingTags.asStateFlow()

    /** Trending tag names shown as quick-filter chips above the Explore grid (loaded eagerly on init). */
    private val _quickInterestChips = MutableStateFlow<List<String>>(emptyList())
    val quickInterestChips: StateFlow<List<String>> = _quickInterestChips.asStateFlow()

    private val _searchOverlayLoading = MutableStateFlow(false)
    val searchOverlayLoading: StateFlow<Boolean> = _searchOverlayLoading.asStateFlow()

    private val _autocompleteSuggestions = MutableStateFlow<List<String>>(emptyList())
    val autocompleteSuggestions: StateFlow<List<String>> = _autocompleteSuggestions.asStateFlow()

    private val _autocompleteLoading = MutableStateFlow(false)
    val autocompleteLoading: StateFlow<Boolean> = _autocompleteLoading.asStateFlow()

    private var autocompleteJob: Job? = null

    /**
     * Bumps on each new first-page request so stale responses (filter change, tab refresh) are dropped.
     * [loadMore] snapshots this at start and aborts if it changed mid-flight.
     */
    private val listingsFetchGeneration = AtomicInteger(0)

    private var lastSuccessfulExploreRefreshAtMs = 0L
    private var lastExploreLoadMoreAtMs = 0L
    private var exploreLoadMoreBlockedUntilMs = 0L

    /** Digits-only VND hints; empty = no bound. */
    private val _minPriceText = MutableStateFlow("")
    val minPriceText: StateFlow<String> = _minPriceText.asStateFlow()

    private val _maxPriceText = MutableStateFlow("")
    val maxPriceText: StateFlow<String> = _maxPriceText.asStateFlow()

    /** `null` = any condition. API values: `new`, `like_new`, `good`, `fair`. */
    private val _selectedConditionFilter = MutableStateFlow<String?>(null)
    val selectedConditionFilter: StateFlow<String?> = _selectedConditionFilter.asStateFlow()

    /**
     * Coarse classification of the viewer's stored sizing reference; drives the "Match my size"
     * quick toggle copy + nudge sheet.
     *
     * - `Unknown` — viewer profile not loaded yet (guests stay Unknown).
     * - `Missing` — no reference_size and no height/weight: turning the toggle on is a no-op on the
     *   server; we surface a setup sheet on Explore + banner on Home.
     * - `EstimateOnly` — height/weight provided but no reference_size: backend can apply a best-effort
     *   estimate; we badge the toggle as "Estimated".
     * - `Full` — reference_size present (with or without measurements): toggle reads normally.
     */
    enum class ProfileSizingState { Unknown, Missing, EstimateOnly, Full }

    private val _profileSizingState = MutableStateFlow(ProfileSizingState.Unknown)
    val profileSizingState: StateFlow<ProfileSizingState> = _profileSizingState.asStateFlow()

    /**
     * One-shot event raised when the user toggles match_profile ON while `profileSizingState`
     * is `Missing` — the UI listens to this to show the setup nudge sheet on Explore.
     */
    private val _showSizingSetupNudge = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val showSizingSetupNudge: SharedFlow<Unit> = _showSizingSetupNudge.asSharedFlow()

    /** `recent`, `popular`, `price_asc`, `price_desc` — applied only when text search has non-empty `q`. */
    private val _sortOption = MutableStateFlow("recent")
    val sortOption: StateFlow<String> = _sortOption.asStateFlow()

    data class BrowseLocationFilter(
        val provinceId: String? = null,
        val provinceName: String = "",
        val districtId: String? = null,
        val districtName: String = "",
        val wardId: String? = null,
        val wardName: String = "",
    ) {
        val hasSelection: Boolean
            get() = !provinceId.isNullOrBlank()
        val chipLabel: String
            get() = listOfNotNull(
                wardName.takeIf { it.isNotBlank() },
                districtName.takeIf { it.isNotBlank() },
                provinceName.takeIf { it.isNotBlank() },
            ).joinToString(", ")
    }

    private data class SellerLocationQuery(
        val provinceId: String? = null,
        val districtId: String? = null,
        val wardId: String? = null,
    )

    /** Off / nearby (default address) / manual province+district — persisted locally. */
    private val _browseLocationMode = MutableStateFlow(ExploreBrowseLocationPreference.read(application))
    val browseLocationMode: StateFlow<BrowseLocationMode> = _browseLocationMode.asStateFlow()

    /** Manual pick synced to profile when signed in. */
    private val _manualBrowseLocation = MutableStateFlow(BrowseLocationFilter())
    val manualBrowseLocation: StateFlow<BrowseLocationFilter> = _manualBrowseLocation.asStateFlow()

    /** Default shipping address province/district for nearby mode. */
    private val _defaultAddressLocation = MutableStateFlow(BrowseLocationFilter())
    val defaultAddressLocation: StateFlow<BrowseLocationFilter> = _defaultAddressLocation.asStateFlow()

    private val _shoppingContext = MutableStateFlow<ShoppingContext?>(null)
    val shoppingContext: StateFlow<ShoppingContext?> = _shoppingContext.asStateFlow()

    /** @deprecated Use [manualBrowseLocation] — kept for gradual UI migration. */
    val browseLocation: StateFlow<BrowseLocationFilter> = _manualBrowseLocation.asStateFlow()

    private val _showBrowseLocationSetupNudge = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val showBrowseLocationSetupNudge: SharedFlow<Unit> = _showBrowseLocationSetupNudge.asSharedFlow()

    private var priceFilterDebounceJob: Job? = null

    init {
        loadAll()
        viewModelScope.launch {
            realtimeManager.events.collect { event ->
                if (event is RealtimeEvent.FeedRefresh &&
                    !_isSearchMode.value &&
                    _primarySection.value == ExplorePrimarySection.Listings
                ) {
                    fetchListingsFirstPage()
                }
            }
        }
    }

    fun setPrimarySection(section: ExplorePrimarySection) {
        val prev = _primarySection.value
        if (prev == section) return
        _primarySection.value = section
        _autocompleteSuggestions.value = emptyList()
        if (section == ExplorePrimarySection.Sellers) {
            viewModelScope.launch {
                refreshSellerBrowse()
            }
        }
    }

    /**
     * From profile / seller shop / PDP: switch to **Posts** (listings), apply category / brand / aesthetic tag / country
     * by id, optionally run text search when [searchQuery] is non-empty and no structured filter id is set.
     * Display labels must not be passed as [searchQuery] — only user-typed search text.
     * If only [searchQuery] is set (no ids), tries to match an aesthetic tag in the catalog by name.
     */
    fun openExploreFromProfileFilter(
        categoryId: String? = null,
        brandId: String? = null,
        aestheticTagId: String? = null,
        searchQuery: String,
        countryId: String? = null,
        countryIso2: String? = null,
    ) {
        viewModelScope.launch {
            _primarySection.value = ExplorePrimarySection.Listings
            setSearchBarExpanded(false)
            lastSuccessfulExploreRefreshAtMs = 0L
            _isLoading.value = true
            _loadError.value = false
            _listings.value = emptyList()
            _hasMore.value = true
            withContext(Dispatchers.IO) {
                loadTags()
                loadCategories()
            }
            val q = searchQuery.trim()
            val cat = categoryId?.takeIf { it.isNotBlank() }
            val brand = brandId?.takeIf { it.isNotBlank() }
            var tag = aestheticTagId?.takeIf { it.isNotBlank() }
            if (tag == null && cat == null && brand == null && q.isNotBlank()) {
                tag = _aestheticTagsCatalog.value.firstOrNull { t ->
                    t.name.equals(q, ignoreCase = true) ||
                        t.displayName.equals(q, ignoreCase = true) ||
                        t.displayNameVi.equals(q, ignoreCase = true)
                }?.id
            }
            when {
                cat != null -> {
                    _selectedCategoryId.value = cat
                    _selectedBrandId.value = null
                    _selectedAestheticTagIds.value = emptySet()
                }
                brand != null -> {
                    _selectedCategoryId.value = null
                    _selectedBrandId.value = brand
                    _selectedAestheticTagIds.value = emptySet()
                }
                tag != null -> {
                    _selectedCategoryId.value = null
                    _selectedBrandId.value = null
                    _selectedAestheticTagIds.value = setOf(tag)
                }
                else -> {
                    _selectedCategoryId.value = null
                    _selectedBrandId.value = null
                    _selectedAestheticTagIds.value = emptySet()
                }
            }
            _selectedCountryId.value = countryId?.takeIf { !it.isNullOrBlank() }
            _selectedCountryIso2.value = normalizeCountryIso2(countryIso2)
            val hasStructuredFilter = cat != null || brand != null || tag != null ||
                !countryId.isNullOrBlank() || !normalizeCountryIso2(countryIso2).isNullOrBlank()
            when {
                hasStructuredFilter -> {
                    // Category / brand / tag / country — filter ids only; label must not appear in search UI.
                    _committedListingSearchQuery.value = ""
                    _isSearchMode.value = false
                    fetchListingsFirstPage()
                }
                q.isNotBlank() -> {
                    _committedListingSearchQuery.value = q
                    _isSearchMode.value = true
                    runSearchWithCurrentFilters()
                }
                else -> {
                    _committedListingSearchQuery.value = ""
                    _isSearchMode.value = false
                    fetchListingsFirstPage()
                }
            }
            _searchQuery.value = ""
            _isLoading.value = false
            requestScrollExploreToTop()
        }
    }

    private suspend fun refreshSellerBrowse() {
        val gen = sellersBrowseGeneration.incrementAndGet()
        _committedSellerSearchQuery.value = ""
        _sellersLoading.value = true
        _sellersLoadError.value = false
        _sellerPreviewPosts.value = emptyMap()
        val guest = isGuestBrowse()
        val result = withContext(Dispatchers.IO) {
            userRepository.searchUsers("a", limit = 40, publicBrowse = guest)
        }
        result.fold(
            onSuccess = { users ->
                if (gen != sellersBrowseGeneration.get()) return@fold
                _sellerBrowseResults.value = users
                _sellersLoadError.value = false
                lastSuccessfulExploreRefreshAtMs = System.currentTimeMillis()
            },
            onFailure = {
                if (gen != sellersBrowseGeneration.get()) return@fold
                _sellerBrowseResults.value = emptyList()
                _sellerPreviewPosts.value = emptyMap()
                _sellersLoadError.value = true
                _events.tryEmit(
                    it.message?.takeIf { m -> m.isNotBlank() }
                        ?: getApplication<Application>().getString(R.string.feed_load_error),
                )
            },
        )
        _sellersLoading.value = false
        if (gen == sellersBrowseGeneration.get() && _sellerBrowseResults.value.isNotEmpty()) {
            loadSellerListingPreviews(gen)
        }
    }

    private fun loadSellerListingPreviews(expectedGen: Int) {
        viewModelScope.launch {
            _sellerPreviewPosts.value = emptyMap()
            val sellers = _sellerBrowseResults.value
            coroutineScope {
                sellers.forEach { seller ->
                    val key = seller.userId.trim().ifBlank { seller.username.trim() }
                    if (key.isBlank()) return@forEach
                    launch(Dispatchers.IO) {
                        val listings = if (isGuestBrowse()) {
                            listingRepository.getListingsBySellerPublic(
                                sellerId = key,
                                limit = 3,
                                offset = 0,
                            )
                        } else {
                            listingRepository.getListingsBySeller(
                                sellerId = key,
                                status = null,
                                limit = 3,
                                offset = 0,
                            )
                        }.getOrElse { emptyList() }.take(3)
                        if (expectedGen != sellersBrowseGeneration.get()) return@launch
                        _sellerPreviewPosts.update { cur -> cur + (key to listings) }
                        syncSellerFollowingFromListings(listings)
                    }
                }
            }
        }
    }

    fun retrySellerBrowse() {
        viewModelScope.launch {
            refreshSellerBrowse()
        }
    }

    fun requestSearchBarExpanded() {
        _searchBarExpanded.value = true
        when (_primarySection.value) {
            ExplorePrimarySection.Listings -> {
                if (_isSearchMode.value && _committedListingSearchQuery.value.isNotBlank()) {
                    _searchQuery.value = _committedListingSearchQuery.value
                }
            }
            ExplorePrimarySection.Sellers -> {
                if (_committedSellerSearchQuery.value.isNotBlank()) {
                    _searchQuery.value = _committedSellerSearchQuery.value
                }
            }
        }
        loadSearchOverlayData()
    }

    fun setSearchBarExpanded(expanded: Boolean) {
        _searchBarExpanded.value = expanded
        if (!expanded) {
            autocompleteJob?.cancel()
            _autocompleteSuggestions.value = emptyList()
            _autocompleteLoading.value = false
            _searchQuery.value = ""
        }
    }

    /** Clears active **Posts** text search and returns to browse. */
    fun clearListingSearch() {
        viewModelScope.launch {
            _isSearchMode.value = false
            _committedListingSearchQuery.value = ""
            _isLoading.value = true
            _loadError.value = false
            fetchListingsFirstPage()
            _isLoading.value = false
        }
    }

    /** Clears active **Sellers** text search and reloads default seller browse. */
    fun clearSellerSearch() {
        viewModelScope.launch {
            refreshSellerBrowse()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        autocompleteJob?.cancel()
        if (query.isBlank()) {
            _autocompleteSuggestions.value = emptyList()
            _autocompleteLoading.value = false
            return
        }
        _autocompleteLoading.value = true
        val snapshot = query.trim()
        autocompleteJob = viewModelScope.launch {
            try {
                delay(280)
                if (_searchQuery.value.trim() != snapshot) return@launch
                val suggestions = withContext(Dispatchers.IO) {
                    when (_primarySection.value) {
                        ExplorePrimarySection.Listings ->
                            searchRepository.autocompleteListingTitles(snapshot).getOrElse { emptyList() }
                        ExplorePrimarySection.Sellers ->
                            userRepository.searchUsers(snapshot, limit = 8, publicBrowse = isGuestBrowse()).fold(
                                onSuccess = { list ->
                                    list.mapNotNull { u ->
                                        val uu = u.username.trim()
                                        when {
                                            uu.isNotBlank() -> "@$uu"
                                            u.displayName.isNotBlank() -> u.displayName.trim()
                                            else -> null
                                        }
                                    }.distinct()
                                },
                                onFailure = { emptyList() },
                            )
                    }
                }
                if (_searchQuery.value.trim() != snapshot) return@launch
                _autocompleteSuggestions.value = suggestions
            } finally {
                if (isActive && _searchQuery.value.trim() == snapshot) {
                    _autocompleteLoading.value = false
                }
            }
        }
    }

    fun loadSearchOverlayData() {
        viewModelScope.launch {
            _searchOverlayLoading.value = true
            withContext(Dispatchers.IO) {
                val recent = searchRepository.getRecentQueries().getOrElse { emptyList() }
                val trending = searchRepository.getTrendingQueries().getOrElse { emptyList() }
                val tagList = searchRepository.getTrendingTags().getOrElse { emptyList() }
                _searchOverlayRecentQueries.value = recent
                _searchOverlayTrendingQueries.value = trending
                _searchOverlayTrendingTags.value = tagList
            }
            _searchOverlayLoading.value = false
        }
    }

    /** Applies a title suggestion or recent/trending query and runs search. */
    fun selectSearchSuggestionAndSubmit(text: String) {
        val t = text.trim()
        if (t.isBlank()) return
        autocompleteJob?.cancel()
        _searchQuery.value = t
        _autocompleteSuggestions.value = emptyList()
        _autocompleteLoading.value = false
        submitSearch()
    }

    /** Trending tag chip: match catalog by name, then filter by id; else run text search. */
    fun selectSearchOverlayTrendingTag(tag: String) {
        val cleaned = tag.trim()
        if (cleaned.isBlank()) return
        if (_primarySection.value == ExplorePrimarySection.Sellers) {
            selectSearchSuggestionAndSubmit(cleaned)
            return
        }
        val match = _aestheticTagsCatalog.value.firstOrNull { t ->
            t.name.equals(cleaned, ignoreCase = true) ||
                t.displayName.equals(cleaned, ignoreCase = true) ||
                t.displayNameVi.equals(cleaned, ignoreCase = true)
        }
        if (match != null) {
            _selectedAestheticTagIds.value = setOf(match.id)
            _isSearchMode.value = false
            _committedListingSearchQuery.value = ""
            setSearchBarExpanded(false)
            viewModelScope.launch {
                _isLoading.value = true
                _loadError.value = false
                if (_isSearchMode.value) {
                    runSearchWithCurrentFilters()
                } else {
                    fetchListingsFirstPage()
                }
                _isLoading.value = false
            }
        } else {
            selectSearchSuggestionAndSubmit(cleaned)
        }
    }

    /**
     * Submit from the Explore search bar: [ExplorePrimarySection.Listings] → `GET /search/listings`;
     * [ExplorePrimarySection.Sellers] → `GET …/users/search` (user search), then storefront previews.
     */
    fun submitSearch() {
        viewModelScope.launch {
            val q = _searchQuery.value.trim()
            if (q.isBlank()) return@launch
            when (_primarySection.value) {
                ExplorePrimarySection.Listings -> {
                    _isLoading.value = true
                    _loadError.value = false
                    _committedListingSearchQuery.value = q
                    _isSearchMode.value = true
                    runSearchWithCurrentFilters()
                    _isLoading.value = false
                    setSearchBarExpanded(false)
                }
                ExplorePrimarySection.Sellers -> {
                    val gen = sellersBrowseGeneration.incrementAndGet()
                    _sellersLoading.value = true
                    _sellersLoadError.value = false
                    _sellerPreviewPosts.value = emptyMap()
                    val result = withContext(Dispatchers.IO) {
                        userRepository.searchUsers(q, limit = 50, publicBrowse = isGuestBrowse())
                    }
                    result.fold(
                        onSuccess = { users ->
                            if (gen != sellersBrowseGeneration.get()) return@fold
                            _sellerBrowseResults.value = users
                            _sellersLoadError.value = false
                            _committedSellerSearchQuery.value = q
                        },
                        onFailure = {
                            if (gen != sellersBrowseGeneration.get()) return@fold
                            _sellerBrowseResults.value = emptyList()
                            _sellerPreviewPosts.value = emptyMap()
                            _committedSellerSearchQuery.value = ""
                            _sellersLoadError.value = true
                            _events.tryEmit(
                                it.message?.takeIf { m -> m.isNotBlank() }
                                    ?: getApplication<Application>().getString(R.string.feed_load_error),
                            )
                        },
                    )
                    _sellersLoading.value = false
                    if (gen == sellersBrowseGeneration.get() && _sellerBrowseResults.value.isNotEmpty()) {
                        loadSellerListingPreviews(gen)
                    }
                    setSearchBarExpanded(false)
                }
            }
        }
    }

    /**
     * Loads the next page when the user scrolls near the end of the grid.
     * Uses `offset = listings.size` and the same filters as the first page.
     */
    fun loadMore() {
        if (!_hasMore.value || _isLoadingMore.value || _isLoading.value) return
        if (_loadError.value && _listings.value.isEmpty()) return
        if (FeedLoadMoreThrottle.isBlocked(exploreLoadMoreBlockedUntilMs)) return
        if (!FeedLoadMoreThrottle.canLoadNow(lastExploreLoadMoreAtMs)) return
        lastExploreLoadMoreAtMs = System.currentTimeMillis()
        val offset = _listings.value.size
        val stableGen = listingsFetchGeneration.get()
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val isSearch = _isSearchMode.value
                val result = withContext(Dispatchers.IO) {
                    searchListingsPage(offset = offset, isSearch = isSearch)
                }
                if (stableGen != listingsFetchGeneration.get()) return@launch
                result.fold(
                    onSuccess = { page ->
                        _listings.update { existing -> dedupeAppend(existing, page) }
                        syncSellerFollowingFromListings(page)
                        _hasMore.value = page.size >= ExploreFeedPageSize
                        _loadError.value = false
                        prefetchExploreImages(page)
                    },
                    onFailure = { err ->
                        val blocked = FeedLoadMoreThrottle.blockedUntilAfter(err)
                        if (blocked != null) {
                            exploreLoadMoreBlockedUntilMs = blocked
                        } else {
                            _events.tryEmit(
                                err.message?.takeIf { m -> m.isNotBlank() }
                                    ?: getApplication<Application>().getString(R.string.feed_load_error),
                            )
                        }
                    },
                )
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private suspend fun runSearchWithCurrentFilters() {
        fetchListingsFirstPageInternal(isSearch = true)
    }

    fun loadAll() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = false
            withContext(Dispatchers.IO) {
                loadTags()
                loadFeaturedSellers()
                loadCategories()
                loadQuickInterestChips()
                refreshProfileSizingState()
                refreshShoppingContext()
            }
            fetchListingsFirstPage()
            _isLoading.value = false
        }
    }

    /**
     * Reads the viewer's profile to classify how rich their sizing data is. Cheap (one HTTP),
     * non-fatal — guests degrade to `Unknown`, reset match_profile to `all`, and the UI routes
     * sign-in when they try to enable the filter.
     */
    /** Call after the viewer saves profile sizing so the Explore toggle badge updates immediately. */
    fun refreshProfileSizingStateAfterSave() {
        viewModelScope.launch(Dispatchers.IO) { refreshProfileSizingState() }
    }

    private suspend fun refreshShoppingContext() {
        fashApp.recommendationRepository.shoppingContext(isGuestBrowse()).fold(
            onSuccess = { _shoppingContext.value = it },
            onFailure = { _shoppingContext.value = null },
        )
    }

    /** Refresh default-address snapshot after shipping address book changes. */
    fun refreshBrowseLocationAfterAddressSave() {
        viewModelScope.launch(Dispatchers.IO) {
            refreshDefaultAddressLocation()
        }
    }

    private suspend fun refreshProfileSizingState() {
        if (isGuestBrowse()) {
            _profileSizingState.value = ProfileSizingState.Unknown
            // Guests must not keep match_profile active — sizing filter requires a signed-in profile.
            if (_sizingMode.value.equals("match_profile", ignoreCase = true)) {
                _sizingMode.value = "all"
            }
            return
        }
        val uid = fashApp.authManager.sessionStore.read()?.userId
        if (uid.isNullOrBlank()) {
            _profileSizingState.value = ProfileSizingState.Unknown
            return
        }
        userRepository.getMeProfile().fold(
            onSuccess = { info ->
                val hasSize = !info.referenceSize.isNullOrBlank()
                val hasMeasurement = listOf(
                    info.referenceMeasurementChest,
                    info.referenceMeasurementHem,
                    info.referenceMeasurementLength,
                    info.referenceMeasurementShoulders,
                    info.referenceMeasurementSleeveLength,
                ).any { it != null && it > 0.0 }
                _profileSizingState.value = when {
                    hasSize -> ProfileSizingState.Full
                    hasMeasurement -> ProfileSizingState.EstimateOnly
                    else -> ProfileSizingState.Missing
                }
                _manualBrowseLocation.value = BrowseLocationFilter(
                    provinceId = info.browseProvinceId,
                    provinceName = info.browseProvinceName,
                    districtId = info.browseDistrictId,
                    districtName = info.browseDistrictName,
                )
            },
            onFailure = { _profileSizingState.value = ProfileSizingState.Unknown },
        )
        refreshDefaultAddressLocation()
    }

    private suspend fun refreshDefaultAddressLocation() {
        if (isGuestBrowse()) {
            _defaultAddressLocation.value = BrowseLocationFilter()
            return
        }
        userShippingAddressRepository.listShippingAddresses().fold(
            onSuccess = { list ->
                val def = list.firstOrNull { it.isDefault } ?: list.firstOrNull()
                _defaultAddressLocation.value = if (def == null) {
                    BrowseLocationFilter()
                } else {
                    BrowseLocationFilter(
                        provinceId = def.provinceId?.takeIf { it.isNotBlank() },
                        provinceName = def.city.trim().ifBlank { def.region.trim() },
                        districtId = def.districtId?.takeIf { it.isNotBlank() },
                        districtName = def.district.trim(),
                        wardId = def.wardId?.takeIf { it.isNotBlank() },
                        wardName = def.ward.trim(),
                    )
                }
            },
            onFailure = { _defaultAddressLocation.value = BrowseLocationFilter() },
        )
    }

    fun isBrowseLocationFilterActive(): Boolean = _browseLocationMode.value != BrowseLocationMode.Off

    fun activeBrowseLocationLabel(): String {
        return when (_browseLocationMode.value) {
            BrowseLocationMode.Off -> ""
            BrowseLocationMode.NearbyDefault -> _defaultAddressLocation.value.chipLabel
            BrowseLocationMode.Manual -> _manualBrowseLocation.value.chipLabel
        }
    }

    private fun effectiveSellerLocationQuery(): SellerLocationQuery {
        val source = when (_browseLocationMode.value) {
            BrowseLocationMode.Off -> return SellerLocationQuery()
            BrowseLocationMode.NearbyDefault -> _defaultAddressLocation.value
            BrowseLocationMode.Manual -> _manualBrowseLocation.value
        }
        if (!source.hasSelection) return SellerLocationQuery()
        return when {
            !source.wardId.isNullOrBlank() -> SellerLocationQuery(wardId = source.wardId)
            !source.districtId.isNullOrBlank() -> SellerLocationQuery(districtId = source.districtId)
            !source.provinceId.isNullOrBlank() -> SellerLocationQuery(provinceId = source.provinceId)
            else -> SellerLocationQuery()
        }
    }

    private fun persistBrowseLocationMode(mode: BrowseLocationMode) {
        _browseLocationMode.value = mode
        ExploreBrowseLocationPreference.write(getApplication(), mode)
    }

    /** Quick-toggle ON/OFF from the Explore card. */
    fun setBrowseLocationEnabled(enabled: Boolean) {
        if (!enabled) {
            if (_browseLocationMode.value == BrowseLocationMode.Off) return
            persistBrowseLocationMode(BrowseLocationMode.Off)
            viewModelScope.launch { fetchListingsFirstPage() }
            return
        }
        when {
            _manualBrowseLocation.value.hasSelection -> {
                persistBrowseLocationMode(BrowseLocationMode.Manual)
                viewModelScope.launch { fetchListingsFirstPage() }
            }
            _defaultAddressLocation.value.hasSelection -> {
                persistBrowseLocationMode(BrowseLocationMode.NearbyDefault)
                viewModelScope.launch { fetchListingsFirstPage() }
            }
            else -> {
                _showBrowseLocationSetupNudge.tryEmit(Unit)
            }
        }
    }

    fun setBrowseLocationModeFilter(mode: BrowseLocationMode) {
        when (mode) {
            BrowseLocationMode.Off -> {
                if (_browseLocationMode.value == BrowseLocationMode.Off) return
                persistBrowseLocationMode(BrowseLocationMode.Off)
            }
            BrowseLocationMode.NearbyDefault -> {
                if (!_defaultAddressLocation.value.hasSelection) {
                    _showBrowseLocationSetupNudge.tryEmit(Unit)
                    return
                }
                persistBrowseLocationMode(BrowseLocationMode.NearbyDefault)
            }
            BrowseLocationMode.Manual -> {
                if (!_manualBrowseLocation.value.hasSelection) {
                    _showBrowseLocationSetupNudge.tryEmit(Unit)
                    return
                }
                persistBrowseLocationMode(BrowseLocationMode.Manual)
            }
        }
        viewModelScope.launch { fetchListingsFirstPage() }
    }

    private suspend fun loadQuickInterestChips() {
        val tags = searchRepository.getTrendingTags(limit = 8).getOrElse { emptyList() }
        if (tags.isNotEmpty()) _quickInterestChips.value = tags
    }

    /**
     * Tapping a quick-interest chip by tag name: finds the matching aesthetic tag id in the loaded
     * catalog and toggles it as a filter. Falls back to a text search when not found in catalog.
     */
    fun toggleInterestChip(tagName: String) {
        val cleaned = tagName.trim()
        if (cleaned.isBlank()) return
        val matchedId = _aestheticTagsCatalog.value.firstOrNull {
            it.name.equals(cleaned, ignoreCase = true) || it.displayName.equals(cleaned, ignoreCase = true) ||
                it.displayNameVi.equals(cleaned, ignoreCase = true)
        }?.id
        if (matchedId != null) {
            toggleAestheticTagFilter(matchedId)
        } else {
            _searchQuery.value = cleaned
            submitSearch()
        }
    }

    /**
     * Tapping a trending chip that carries its own UUID (from `/search/trending-tags?include_ids=true`).
     * Uses the ID directly — no catalog lookup required (Bug C fix).
     * Falls back to name-based [toggleInterestChip] when [tagId] is blank.
     */
    fun toggleInterestChipWithId(tagId: String, tagName: String) {
        val id = tagId.trim()
        if (id.isNotBlank()) {
            toggleAestheticTagFilter(id)
        } else {
            toggleInterestChip(tagName)
        }
    }


    private suspend fun loadCategories() {
        commonServiceRepository.getCategoryTree().fold(
            onSuccess = { tree ->
                val leaves = flattenCategoryLeaves(tree)
                    .filter { it.id.isNotBlank() }
                    .distinctBy { it.id }
                    .sortedBy { it.name.lowercase() }
                _categories.value = leaves.map { Category(id = it.id, name = it.name, slug = it.slug) }
            },
            onFailure = {
                listingRepository.getCategories().fold(
                    onSuccess = { list ->
                        _categories.value = list
                            .filter { it.id.isNotBlank() }
                            .distinctBy { it.id }
                            .sortedBy { it.name.lowercase() }
                    },
                    onFailure = { _categories.value = emptyList() },
                )
            },
        )
    }

    private suspend fun loadTags() {
        commonServiceRepository.getAestheticTags(all = true).fold(
            onSuccess = { catalog -> _aestheticTagsCatalog.value = catalog },
            onFailure = { _aestheticTagsCatalog.value = emptyList() },
        )
        commonServiceRepository.getBrands(limit = 80, offset = 0).fold(
            onSuccess = { page -> _brands.value = page.items.sortedBy { it.name.lowercase() } },
            onFailure = { _brands.value = emptyList() },
        )
        commonServiceRepository.getCountries(all = true).fold(
            onSuccess = { countries ->
                _countriesCatalog.value = countries
                    .filter { c -> c.id.isNotBlank() }
                    .distinctBy { c -> c.id }
                    .sortedBy { c -> c.name.lowercase() }
            },
            onFailure = { _countriesCatalog.value = emptyList() },
        )
    }

    private fun flattenCategoryLeaves(nodes: List<CategoryTreeNode>): List<CategoryTreeNode> {
        val out = mutableListOf<CategoryTreeNode>()
        for (n in nodes) {
            if (n.children.isEmpty()) {
                out.add(n)
            } else {
                out.addAll(flattenCategoryLeaves(n.children))
            }
        }
        return out
    }

    private suspend fun loadFeaturedSellers() {
        suspend fun fetchOnce(): Result<List<FeaturedSellerItem>> {
            return if (isGuestBrowse()) {
                searchRepository.browseFeaturedSellersPage(limit = 10, offset = 0).map { it.items }
            } else {
                searchRepository.getFeaturedSellers(limit = 10)
            }
        }
        var result = fetchOnce()
        if (result.isFailure) {
            delay(400)
            result = fetchOnce()
        }
        result.fold(
            onSuccess = { _featuredSellers.value = it },
            onFailure = { /* keep stale rail */ },
        )
    }

    fun setBrowseLocationFilter(
        provinceId: String,
        provinceName: String,
        districtId: String = "",
        districtName: String = "",
        wardId: String = "",
        wardName: String = "",
    ) {
        viewModelScope.launch {
            if (!isGuestBrowse() && districtId.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    userRepository.putBrowseLocation(
                        provinceId = provinceId,
                        provinceName = provinceName,
                        districtId = districtId,
                        districtName = districtName,
                    )
                }
            }
            _manualBrowseLocation.value = BrowseLocationFilter(
                provinceId = provinceId,
                provinceName = provinceName,
                districtId = districtId.takeIf { it.isNotBlank() },
                districtName = districtName,
                wardId = wardId.takeIf { it.isNotBlank() },
                wardName = wardName,
            )
            persistBrowseLocationMode(BrowseLocationMode.Manual)
            fetchListingsFirstPage()
        }
    }

    fun clearBrowseLocationFilter() {
        persistBrowseLocationMode(BrowseLocationMode.Off)
        viewModelScope.launch { fetchListingsFirstPage() }
    }

    private suspend fun fetchListingsFirstPage() {
        fetchListingsFirstPageInternal(isSearch = _isSearchMode.value)
    }

    private suspend fun fetchListingsFirstPageInternal(isSearch: Boolean) {
        val gen = listingsFetchGeneration.incrementAndGet()
        val result = withContext(Dispatchers.IO) {
            searchListingsWithRetry(offset = 0, isSearch = isSearch)
        }
        if (gen != listingsFetchGeneration.get()) return
        result.fold(
            onSuccess = { page ->
                _listings.value = page
                syncSellerFollowingFromListings(page)
                _hasMore.value = page.size >= ExploreFeedPageSize
                _loadError.value = false
                lastSuccessfulExploreRefreshAtMs = System.currentTimeMillis()
                prefetchExploreImages(page)
            },
            onFailure = {
                _loadError.value = true
                _hasMore.value = false
                _events.tryEmit(
                    it.message?.takeIf { m -> m.isNotBlank() }
                        ?: getApplication<Application>().getString(R.string.feed_load_error),
                )
            },
        )
    }

    /**
     * `GET /api/v1/search/listings` — browse uses empty `q` (per API); search mode uses trimmed query.
     * Sort: `popular` for browse (heat-like), `recent` when the user is searching text.
     */
    private suspend fun searchListingsWithRetry(offset: Int, isSearch: Boolean): Result<List<ListingFeedItem>> {
        suspend fun once(): Result<List<ListingFeedItem>> = searchListingsPage(offset = offset, isSearch = isSearch)
        var result = once()
        if (result.isFailure) {
            delay(400)
            result = once()
        }
        return result
    }

    private fun listingSearchQueryForApi(): String =
        _committedListingSearchQuery.value.trim().ifBlank { _searchQuery.value.trim() }

    private fun countryIso2ForApi(): String? {
        normalizeCountryIso2(_selectedCountryIso2.value)?.let { return it }
        val id = _selectedCountryId.value?.takeIf { it.isNotBlank() } ?: return null
        return _countriesCatalog.value
            .find { it.id == id }
            ?.iso2
            ?.let { normalizeCountryIso2(it) }
    }

    private fun searchListingsPage(offset: Int, isSearch: Boolean): Result<List<ListingFeedItem>> {
        val q = if (isSearch) listingSearchQueryForApi() else ""
        val categoryId = _selectedCategoryId.value
        val sort = if (isSearch && q.isNotEmpty()) _sortOption.value else "popular"
        val (minP, maxP) = normalizedPriceBounds()
        val tagIds = _selectedAestheticTagIds.value.toList()
        val countryIso2 = countryIso2ForApi()
        val guest = isGuestBrowse()
        val usePersonalizedBrowse = q.isEmpty()
        val location = effectiveSellerLocationQuery()
        return if (usePersonalizedBrowse) {
            fashApp.recommendationRepository.exploreListings(
                publicBrowse = guest,
                categoryId = categoryId,
                aestheticTagIds = tagIds.takeIf { it.isNotEmpty() },
                brandId = _selectedBrandId.value,
                minPrice = minP,
                maxPrice = maxP,
                condition = _selectedConditionFilter.value,
                countryIso2 = countryIso2,
                limit = ExploreFeedPageSize,
                offset = offset,
                sizingMode = _sizingMode.value.takeIf { it.equals("match_profile", ignoreCase = true) },
                sellerProvinceId = location.provinceId,
                sellerDistrictId = location.districtId,
                sellerWardId = location.wardId,
            )
        } else if (guest) {
            searchRepository.browseListings(
                q = q,
                categoryId = categoryId,
                aestheticTagIds = tagIds.takeIf { it.isNotEmpty() },
                brandId = _selectedBrandId.value,
                countryIso2 = countryIso2,
                minPrice = minP,
                maxPrice = maxP,
                condition = _selectedConditionFilter.value,
                limit = ExploreFeedPageSize,
                offset = offset,
                sort = sort,
                sellerProvinceId = location.provinceId,
                sellerDistrictId = location.districtId,
                sellerWardId = location.wardId,
            )
        } else {
            searchRepository.searchListings(
                q = q,
                categoryId = categoryId,
                aestheticTagIds = tagIds.takeIf { it.isNotEmpty() },
                sizingMode = _sizingMode.value.takeIf { it.equals("match_profile", ignoreCase = true) },
                brandId = _selectedBrandId.value,
                countryIso2 = countryIso2,
                minPrice = minP,
                maxPrice = maxP,
                condition = _selectedConditionFilter.value,
                limit = ExploreFeedPageSize,
                offset = offset,
                sort = sort,
                sellerProvinceId = location.provinceId,
                sellerDistrictId = location.districtId,
                sellerWardId = location.wardId,
            )
        }
    }

    private fun normalizeCountryIso2(raw: String?): String? =
        raw?.trim()?.uppercase(Locale.US)?.takeIf { it.length == 2 && it.all { c -> c in 'A'..'Z' } }

    private fun parsePriceDigits(raw: String): Long? {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        return digits.toLongOrNull()?.coerceAtLeast(0L)
    }

    /** Ensures min ≤ max when both set. */
    private fun normalizedPriceBounds(): Pair<Long?, Long?> {
        val min = parsePriceDigits(_minPriceText.value)
        val max = parsePriceDigits(_maxPriceText.value)
        return when {
            min != null && max != null && min > max -> max to min
            else -> min to max
        }
    }

    fun setMinPriceText(text: String) {
        val filtered = text.filter { it.isDigit() }.take(12)
        if (filtered == _minPriceText.value) return
        _minPriceText.value = filtered
        scheduleDebouncedPriceFilterReload()
    }

    fun setMaxPriceText(text: String) {
        val filtered = text.filter { it.isDigit() }.take(12)
        if (filtered == _maxPriceText.value) return
        _maxPriceText.value = filtered
        scheduleDebouncedPriceFilterReload()
    }

    private fun scheduleDebouncedPriceFilterReload() {
        priceFilterDebounceJob?.cancel()
        priceFilterDebounceJob = viewModelScope.launch {
            delay(450)
            reloadAfterFilterChange()
        }
    }

    private fun reloadAfterFilterChange() {
        priceFilterDebounceJob?.cancel()
        priceFilterDebounceJob = null
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = false
            if (_isSearchMode.value) {
                runSearchWithCurrentFilters()
            } else {
                fetchListingsFirstPage()
            }
            _isLoading.value = false
        }
    }

    fun selectConditionFilter(conditionApiValue: String?) {
        val next = conditionApiValue?.takeIf { it.isNotBlank() }
        if (next == _selectedConditionFilter.value) return
        _selectedConditionFilter.value = next
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = false
            if (_isSearchMode.value) {
                runSearchWithCurrentFilters()
            } else {
                fetchListingsFirstPage()
            }
            _isLoading.value = false
        }
    }

    fun selectSortOption(sort: String) {
        val valid = sort in setOf("recent", "popular", "price_asc", "price_desc")
        if (!valid || sort == _sortOption.value) return
        _sortOption.value = sort
        if (!_isSearchMode.value) return
        val q = listingSearchQueryForApi()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = false
            runSearchWithCurrentFilters()
            _isLoading.value = false
        }
    }

    fun clearPriceFilters() {
        priceFilterDebounceJob?.cancel()
        if (_minPriceText.value.isEmpty() && _maxPriceText.value.isEmpty()) return
        _minPriceText.value = ""
        _maxPriceText.value = ""
        reloadAfterFilterChange()
    }

    private fun dedupeAppend(
        existing: List<ListingFeedItem>,
        new: List<ListingFeedItem>,
    ): List<ListingFeedItem> {
        if (new.isEmpty()) return existing
        val seen = existing.map { it.id }.toMutableSet()
        val appended = new.filter { item ->
            if (item.id in seen) false else {
                seen.add(item.id)
                true
            }
        }
        return existing + appended
    }

    fun toggleAestheticTagFilter(tagId: String) {
        val id = tagId.trim()
        if (id.isEmpty()) return
        _selectedAestheticTagIds.update { cur -> if (id in cur) cur - id else cur + id }
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = false
            if (_isSearchMode.value) {
                runSearchWithCurrentFilters()
            } else {
                fetchListingsFirstPage()
            }
            _isLoading.value = false
        }
    }

    fun clearAestheticTagFilters() {
        if (_selectedAestheticTagIds.value.isEmpty()) return
        _selectedAestheticTagIds.value = emptySet()
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = false
            if (_isSearchMode.value) {
                runSearchWithCurrentFilters()
            } else {
                fetchListingsFirstPage()
            }
            _isLoading.value = false
        }
    }

    fun selectBrandFilter(brandId: String?) {
        val next = brandId?.takeIf { it.isNotBlank() }
        if (next == _selectedBrandId.value) return
        _selectedBrandId.value = next
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = false
            if (_isSearchMode.value) {
                runSearchWithCurrentFilters()
            } else {
                fetchListingsFirstPage()
            }
            _isLoading.value = false
        }
    }

    /** Single-select country (made in); pass both id and iso2 from catalog, or nulls to clear. */
    fun selectCountryFilter(countryId: String?, countryIso2: String?) {
        val nextId = countryId?.takeIf { it.isNotBlank() }
        val nextIso = normalizeCountryIso2(countryIso2)
        if (nextId == _selectedCountryId.value && nextIso == _selectedCountryIso2.value) return
        _selectedCountryId.value = nextId
        _selectedCountryIso2.value = nextIso
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = false
            if (_isSearchMode.value) {
                runSearchWithCurrentFilters()
            } else {
                fetchListingsFirstPage()
            }
            _isLoading.value = false
        }
    }

    fun setSizingModeFilter(mode: String) {
        val m = if (mode.equals("match_profile", ignoreCase = true)) "match_profile" else "all"
        if (isGuestBrowse() && m == "match_profile") return
        if (m == _sizingMode.value) return
        _sizingMode.value = m
        ExploreSizingPreference.write(getApplication(), m)
        // When the user activates Match my size but has nothing on file, route them to the setup sheet
        // BEFORE we fire a network request that would otherwise silently degrade to "all". Guests fall
        // through (`Unknown`) — they will be prompted to sign in via the standard guest-gate elsewhere.
        if (m == "match_profile" && _profileSizingState.value == ProfileSizingState.Missing) {
            _showSizingSetupNudge.tryEmit(Unit)
        }
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = false
            if (_isSearchMode.value) {
                runSearchWithCurrentFilters()
            } else {
                fetchListingsFirstPage()
            }
            _isLoading.value = false
        }
    }

    fun selectCategory(categoryId: String?) {
        val next = categoryId?.takeIf { it.isNotBlank() }
        if (next == _selectedCategoryId.value) return
        _selectedCategoryId.value = next
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = false
            if (_isSearchMode.value) {
                runSearchWithCurrentFilters()
            } else {
                fetchListingsFirstPage()
            }
            _isLoading.value = false
        }
    }

    /** Clears marketplace filters only; keeps active listing search if any. */
    fun clearMarketplaceFilters() {
        if (!hasActiveMarketplaceFilters()) return
        priceFilterDebounceJob?.cancel()
        priceFilterDebounceJob = null
        _minPriceText.value = ""
        _maxPriceText.value = ""
        _selectedCategoryId.value = null
        _selectedBrandId.value = null
        _selectedAestheticTagIds.value = emptySet()
        _selectedCountryId.value = null
        _selectedCountryIso2.value = null
        _selectedConditionFilter.value = null
        if (_sizingMode.value != "all") {
            _sizingMode.value = "all"
            ExploreSizingPreference.write(getApplication(), "all")
        }
        if (_browseLocationMode.value != BrowseLocationMode.Off) {
            persistBrowseLocationMode(BrowseLocationMode.Off)
        }
        _hasMore.value = true
        reloadAfterFilterChange()
        viewModelScope.launch { requestScrollExploreToTop() }
    }

    private fun hasActiveMarketplaceFilters(): Boolean =
        _selectedCategoryId.value != null ||
            _selectedAestheticTagIds.value.isNotEmpty() ||
            _minPriceText.value.isNotEmpty() ||
            _maxPriceText.value.isNotEmpty() ||
            _selectedConditionFilter.value != null ||
            _selectedBrandId.value != null ||
            _selectedCountryId.value != null ||
            !_selectedCountryIso2.value.isNullOrBlank() ||
            _sizingMode.value != "all" ||
            _browseLocationMode.value != BrowseLocationMode.Off

    /**
     * Resets marketplace filters, search text, and reloads the listings browse feed.
     * Used from the Explore empty state when filters/search yield no results.
     */
    fun clearExploreConstraints() {
        viewModelScope.launch {
            priceFilterDebounceJob?.cancel()
            _minPriceText.value = ""
            _maxPriceText.value = ""
            _selectedCategoryId.value = null
            _selectedBrandId.value = null
            _selectedAestheticTagIds.value = emptySet()
            _selectedCountryId.value = null
            _selectedCountryIso2.value = null
            _selectedConditionFilter.value = null
            if (_sizingMode.value != "all") {
                _sizingMode.value = "all"
                ExploreSizingPreference.write(getApplication(), "all")
            }
            if (_browseLocationMode.value != BrowseLocationMode.Off) {
                persistBrowseLocationMode(BrowseLocationMode.Off)
            }
            _isSearchMode.value = false
            _committedListingSearchQuery.value = ""
            _searchQuery.value = ""
            _isLoading.value = true
            _loadError.value = false
            _listings.value = emptyList()
            _hasMore.value = true
            fetchListingsFirstPage()
            _isLoading.value = false
        }
    }

    /** Refreshes listings/sellers only when data is older than [ExploreStaleThresholdMs]. */
    fun onExploreOpened() {
        refreshIfStale()
    }

    fun refreshIfStale() {
        val now = System.currentTimeMillis()
        if (now - lastSuccessfulExploreRefreshAtMs < ExploreStaleThresholdMs) return
        reloadExploreContent()
    }

    fun onExploreTabSelected() {
        reloadExploreContent()
    }

    private fun reloadExploreContent() {
        viewModelScope.launch {
            when (_primarySection.value) {
                ExplorePrimarySection.Listings -> {
                    if (_isSearchMode.value) {
                        runSearchWithCurrentFilters()
                    } else {
                        fetchListingsFirstPage()
                    }
                }
                ExplorePrimarySection.Sellers -> {
                    refreshSellerBrowse()
                }
            }
        }
    }

    fun requestScrollExploreToTop() {
        viewModelScope.launch {
            if (_searchBarExpanded.value && !_isSearchMode.value) {
                setSearchBarExpanded(false)
                delay(48)
            }
            _scrollExploreToTop.emit(Unit)
        }
    }

    fun retryLoad() {
        loadAll()
    }

    /** Clears listing/seller results and follow chips when the user signs out. */
    fun clearCachesForSignedOutUser() {
        _listings.value = emptyList()
        _sellerBrowseResults.value = emptyList()
        _sellerPreviewPosts.value = emptyMap()
        _followingIds.value = emptySet()
        _hasMore.value = true
        _loadError.value = false
        _sellersLoadError.value = false
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _loadError.value = false
            withContext(Dispatchers.IO) {
                loadTags()
                loadFeaturedSellers()
                loadCategories()
            }
            when (_primarySection.value) {
                ExplorePrimarySection.Listings -> {
                    if (_isSearchMode.value) {
                        runSearchWithCurrentFilters()
                    } else {
                        fetchListingsFirstPage()
                    }
                }
                ExplorePrimarySection.Sellers -> {
                    refreshSellerBrowse()
                }
            }
            _isRefreshing.value = false
        }
    }

    fun follow(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            userRepository.follow(userId).fold(
                onSuccess = { resolvedId ->
                    _followingIds.update { set ->
                        var s = set + resolvedId
                        if (userId != resolvedId) s = s + userId
                        s
                    }
                    _events.tryEmit(getApplication<Application>().getString(R.string.follow_success))
                },
                onFailure = {
                    _events.tryEmit(
                        it.message?.takeIf { m -> m.isNotBlank() }
                            ?: getApplication<Application>().getString(R.string.feed_action_error),
                    )
                },
            )
        }
    }

    fun isFollowing(userId: String): Boolean = _followingIds.value.contains(userId)

    fun recordView(item: ListingFeedItem, position: Int = 0) {
        feedEventReporter.impression(item.id, surface = "explore", position = position)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                listingRepository.recordView(item.id)
            }
        }
    }

    fun reportListingClick(item: ListingFeedItem, position: Int = 0) {
        feedEventReporter.click(item.id, surface = "explore", position = position)
    }

    fun recordListingDwell(item: ListingFeedItem, surface: String, position: Int, dwellMs: Int) {
        if (dwellMs < 800) return
        feedEventReporter.dwell(item.id, surface = surface, position = position, dwellMs = dwellMs)
    }

    private fun prefetchExploreImages(items: List<ListingFeedItem>) {
        if (items.isEmpty()) return
        val ctx = getApplication<Application>()
        val columnWidthDp = (ctx.resources.configuration.screenWidthDp - 24) / 2f
        FeedListingImagePrefetch.prefetch(ctx, items, columnWidthDp)
    }

    /** Applies `seller.is_following` from listing payloads to [followingIds] (viewer batched flags). */
    private fun syncSellerFollowingFromListings(items: List<ListingFeedItem>) {
        if (items.isEmpty()) return
        _followingIds.update { cur ->
            val m = cur.toMutableSet()
            for (item in items) {
                val sid = item.sellerId?.takeIf { it.isNotBlank() } ?: continue
                if (item.sellerIsFollowing) m.add(sid) else m.remove(sid)
            }
            m
        }
    }

    /** Updates the same listing in the main grid and in seller preview strips (IDs may overlap). */
    private fun patchListingEverywhere(listingId: String, transform: (ListingFeedItem) -> ListingFeedItem) {
        _listings.update { list -> list.map { if (it.id == listingId) transform(it) else it } }
        _sellerPreviewPosts.update { map ->
            map.mapValues { (_, items) ->
                items.map { if (it.id == listingId) transform(it) else it }
            }
        }
        _listingPreview.update { preview ->
            preview?.takeIf { it.feedItem.id == listingId }?.copy(feedItem = transform(preview.feedItem))
        }
    }

    /** Opens the Explore half-sheet preview instead of navigating straight to PDP. */
    fun openListingPreview(item: ListingFeedItem, position: Int = 0) {
        openListingPreview(item, surface = "explore", position = position)
    }

    fun openListingPreview(item: ListingFeedItem, surface: String, position: Int = 0) {
        feedEventReporter.previewOpen(item.id, surface = surface, position = position)
        listingPreviewDetailJob?.cancel()
        _listingPreview.value = ExploreListingPreviewState(
            feedItem = item,
            gridPosition = position,
            surface = surface,
            detail = null,
            isDetailLoading = true,
        )
        listingPreviewDetailJob = viewModelScope.launch {
            val guest = isGuestBrowse()
            withContext(Dispatchers.IO) {
                listingRepository.getListingDetail(item.id, publicBrowse = guest)
            }.fold(
                onSuccess = { detail ->
                    _listingPreview.update { cur ->
                        if (cur?.feedItem?.id != item.id) return@update cur
                        cur.copy(detail = detail, isDetailLoading = false)
                    }
                },
                onFailure = {
                    _listingPreview.update { cur ->
                        if (cur?.feedItem?.id != item.id) return@update cur
                        cur.copy(isDetailLoading = false)
                    }
                },
            )
        }
    }

    fun closeListingPreview() {
        val cur = _listingPreview.value
        if (cur != null && !cur.outcomeRecorded) {
            val dwellMs = (System.currentTimeMillis() - cur.openedAtMs).toInt().coerceAtLeast(0)
            feedEventReporter.previewDismiss(
                cur.feedItem.id,
                surface = cur.surface,
                position = cur.gridPosition,
                dwellMs = dwellMs,
            )
        }
        listingPreviewDetailJob?.cancel()
        _listingPreview.value = null
    }

    /** User tapped “View detail” from quick look — stronger signal than dismiss. */
    fun openListingDetailFromPreview(): Pair<String, String?>? {
        val cur = _listingPreview.value ?: return null
        feedEventReporter.previewDetail(cur.feedItem.id, surface = cur.surface, position = cur.gridPosition)
        val id = cur.feedItem.id
        val sellerId = cur.feedItem.sellerId
        listingPreviewDetailJob?.cancel()
        _listingPreview.value = null
        return id to sellerId
    }

    /** User tapped “Message seller” from quick look. */
    fun openChatFromPreview(): Pair<String, String?>? {
        val cur = _listingPreview.value ?: return null
        feedEventReporter.chatInitiate(cur.feedItem.id, surface = cur.surface, position = cur.gridPosition)
        val id = cur.feedItem.id
        val sellerId = cur.feedItem.sellerId
        listingPreviewDetailJob?.cancel()
        _listingPreview.value = null
        return id to sellerId
    }

    fun toggleLike(item: ListingFeedItem) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { listingRepository.toggleLike(item.id) }
            result.fold(
                onSuccess = { liked ->
                    patchListingEverywhere(item.id) { cur ->
                        val delta = when {
                            liked && !cur.isLiked -> 1
                            !liked && cur.isLiked -> -1
                            else -> 0
                        }
                        cur.copy(
                            isLiked = liked,
                            likeCount = (cur.likeCount + delta).coerceAtLeast(0),
                        )
                    }
                    if (liked) feedEventReporter.like(item.id, surface = "explore")
                    emitSnackbarMessage(_events, FeedEngagementFeedback.likeMessageRes(liked))
                },
                onFailure = {
                    emitSnackbarMessage(
                        _events,
                        it.message?.takeIf { m -> m.isNotBlank() }
                            ?: getApplication<Application>().getString(R.string.feed_action_error),
                    )
                },
            )
        }
    }

    fun toggleSave(item: ListingFeedItem) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { listingRepository.toggleSave(item.id, item.isSaved) }
            result.fold(
                onSuccess = { saved ->
                    patchListingEverywhere(item.id) { cur ->
                        val delta = when {
                            saved && !cur.isSaved -> 1
                            !saved && cur.isSaved -> -1
                            else -> 0
                        }
                        cur.copy(
                            isSaved = saved,
                            saveCount = (cur.saveCount + delta).coerceAtLeast(0),
                        )
                    }
                    if (saved) feedEventReporter.save(item.id, surface = "explore")
                    emitSnackbarMessage(_events, FeedEngagementFeedback.saveMessageRes(saved))
                },
                onFailure = {
                    emitSnackbarMessage(
                        _events,
                        it.message?.takeIf { m -> m.isNotBlank() }
                            ?: getApplication<Application>().getString(R.string.feed_action_error),
                    )
                },
            )
        }
    }
}
