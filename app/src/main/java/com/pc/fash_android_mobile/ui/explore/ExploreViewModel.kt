package com.pc.fash_android_mobile.ui.explore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.Category
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.realtime.RealtimeEvent
import com.pc.fash_android_mobile.data.realtime.RealtimeManager
import com.pc.fash_android_mobile.data.search.SearchRepository
import com.pc.fash_android_mobile.data.search.TrendingQueryItem
import com.pc.fash_android_mobile.data.user.UserRepository
import com.pc.fash_android_mobile.data.user.UserSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

private const val ExploreFeedPageSize = 20

class ExploreViewModel(application: Application) : AndroidViewModel(application) {

    private val listingRepository: ListingRepository =
        (application as FashApplication).listingRepository
    private val searchRepository: SearchRepository =
        (application as FashApplication).searchRepository
    private val userRepository: UserRepository =
        (application as FashApplication).userRepository
    private val realtimeManager: RealtimeManager =
        (application as FashApplication).realtimeManager

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _selectedTagIndex = MutableStateFlow(0) // 0 = All
    val selectedTagIndex: StateFlow<Int> = _selectedTagIndex.asStateFlow()

    private val _featuredSellers = MutableStateFlow<List<UserSearchResult>>(emptyList())
    val featuredSellers: StateFlow<List<UserSearchResult>> = _featuredSellers.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    /** `null` = all categories (no `category_id` filter). */
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _listings = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val listings: StateFlow<List<ListingFeedItem>> = _listings.asStateFlow()

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
    private val _events = MutableSharedFlow<String>()
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

    /** Search hub (expanded bar, no submitted query yet): recent / trending / tags from search endpoints. */
    private val _searchOverlayRecentQueries = MutableStateFlow<List<String>>(emptyList())
    val searchOverlayRecentQueries: StateFlow<List<String>> = _searchOverlayRecentQueries.asStateFlow()

    private val _searchOverlayTrendingQueries = MutableStateFlow<List<TrendingQueryItem>>(emptyList())
    val searchOverlayTrendingQueries: StateFlow<List<TrendingQueryItem>> = _searchOverlayTrendingQueries.asStateFlow()

    private val _searchOverlayTrendingTags = MutableStateFlow<List<String>>(emptyList())
    val searchOverlayTrendingTags: StateFlow<List<String>> = _searchOverlayTrendingTags.asStateFlow()

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

    /** Digits-only VND hints; empty = no bound. */
    private val _minPriceText = MutableStateFlow("")
    val minPriceText: StateFlow<String> = _minPriceText.asStateFlow()

    private val _maxPriceText = MutableStateFlow("")
    val maxPriceText: StateFlow<String> = _maxPriceText.asStateFlow()

    /** `null` = any condition. API values: `new`, `like_new`, `good`, `fair`. */
    private val _selectedConditionFilter = MutableStateFlow<String?>(null)
    val selectedConditionFilter: StateFlow<String?> = _selectedConditionFilter.asStateFlow()

    /** `recent`, `popular`, `price_asc`, `price_desc` — applied only when text search has non-empty `q`. */
    private val _sortOption = MutableStateFlow("recent")
    val sortOption: StateFlow<String> = _sortOption.asStateFlow()

    private var priceFilterDebounceJob: Job? = null

    init {
        loadAll()
        viewModelScope.launch {
            realtimeManager.events.collect { event ->
                if (event is RealtimeEvent.FeedRefresh && !_isSearchMode.value) {
                    fetchListingsFirstPage()
                }
            }
        }
    }

    fun requestSearchBarExpanded() {
        _searchBarExpanded.value = true
        loadSearchOverlayData()
    }

    fun setSearchBarExpanded(expanded: Boolean) {
        _searchBarExpanded.value = expanded
        if (!expanded) {
            autocompleteJob?.cancel()
            _autocompleteSuggestions.value = emptyList()
            _autocompleteLoading.value = false
            _searchQuery.value = ""
            if (_isSearchMode.value) {
                _isSearchMode.value = false
                viewModelScope.launch {
                    _isLoading.value = true
                    _loadError.value = false
                    fetchListingsFirstPage()
                    _isLoading.value = false
                }
            }
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
                val result = withContext(Dispatchers.IO) {
                    searchRepository.autocompleteListingTitles(snapshot)
                }
                if (_searchQuery.value.trim() != snapshot) return@launch
                _autocompleteSuggestions.value = result.getOrElse { emptyList() }
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

    /** Trending tag chip: match filter tags when possible, else search by text. */
    fun selectSearchOverlayTrendingTag(tag: String) {
        val cleaned = tag.trim()
        if (cleaned.isBlank()) return
        val idx = _tags.value.indexOfFirst { it.equals(cleaned, ignoreCase = true) }
        if (idx >= 0) {
            selectTag(idx + 1)
            setSearchBarExpanded(false)
        } else {
            selectSearchSuggestionAndSubmit(cleaned)
        }
    }

    /** Runs `GET /search/listings` with current query + category + aesthetic tag (first page). */
    fun submitSearch() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = false
            _isSearchMode.value = true
            runSearchWithCurrentFilters()
            _isLoading.value = false
        }
    }

    /**
     * Loads the next page when the user scrolls near the end of the grid.
     * Uses `offset = listings.size` and the same filters as the first page.
     */
    fun loadMore() {
        if (!_hasMore.value || _isLoadingMore.value || _isLoading.value) return
        if (_loadError.value && _listings.value.isEmpty()) return
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
                        _hasMore.value = page.size >= ExploreFeedPageSize
                        _loadError.value = false
                    },
                    onFailure = {
                        _events.tryEmit(
                            it.message?.takeIf { m -> m.isNotBlank() }
                                ?: getApplication<Application>().getString(R.string.feed_load_error),
                        )
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
            }
            fetchListingsFirstPage()
            _isLoading.value = false
        }
    }

    private suspend fun loadCategories() {
        listingRepository.getCategories().fold(
            onSuccess = { list ->
                _categories.value = list
                    .filter { it.id.isNotBlank() }
                    .distinctBy { it.id }
                    .sortedBy { it.name.lowercase() }
            },
            onFailure = { _categories.value = emptyList() },
        )
    }

    private suspend fun loadTags() {
        searchRepository.getTrendingTags().fold(
            onSuccess = { _tags.value = it },
            onFailure = {
                userRepository.getAestheticTags().fold(
                    onSuccess = { _tags.value = it.map { t -> t.name } },
                    onFailure = { },
                )
            },
        )
    }

    private suspend fun loadFeaturedSellers() {
        userRepository.searchUsers("a", limit = 10).fold(
            onSuccess = { _featuredSellers.value = it },
            onFailure = { _featuredSellers.value = emptyList() },
        )
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
                _hasMore.value = page.size >= ExploreFeedPageSize
                _loadError.value = false
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

    private fun searchListingsPage(offset: Int, isSearch: Boolean): Result<List<ListingFeedItem>> {
        val q = if (isSearch) _searchQuery.value.trim() else ""
        val idx = _selectedTagIndex.value
        val tag = if (idx <= 0) null else _tags.value.getOrNull(idx - 1)
        val categoryId = _selectedCategoryId.value
        val sort = if (isSearch && q.isNotEmpty()) _sortOption.value else "popular"
        val (minP, maxP) = normalizedPriceBounds()
        return searchRepository.searchListings(
            q = q,
            categoryId = categoryId,
            tags = tag,
            minPrice = minP,
            maxPrice = maxP,
            condition = _selectedConditionFilter.value,
            limit = ExploreFeedPageSize,
            offset = offset,
            sort = sort,
        )
    }

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
        val q = _searchQuery.value.trim()
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

    fun selectTag(index: Int) {
        if (index == _selectedTagIndex.value) return
        _selectedTagIndex.value = index
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

    fun onExploreTabSelected() {
        viewModelScope.launch {
            if (_isSearchMode.value) {
                runSearchWithCurrentFilters()
            } else {
                fetchListingsFirstPage()
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

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _loadError.value = false
            withContext(Dispatchers.IO) {
                loadTags()
                loadFeaturedSellers()
                loadCategories()
            }
            if (_isSearchMode.value) {
                runSearchWithCurrentFilters()
            } else {
                fetchListingsFirstPage()
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

    fun recordView(item: ListingFeedItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                listingRepository.recordView(item.id)
            }
        }
    }
}
