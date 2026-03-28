package com.pc.fash_android_mobile.ui.explore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.explore.ExploreUiPreferences
import com.pc.fash_android_mobile.data.listing.Category
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.realtime.RealtimeEvent
import com.pc.fash_android_mobile.data.realtime.RealtimeManager
import com.pc.fash_android_mobile.data.search.SearchRepository
import com.pc.fash_android_mobile.data.user.UserRepository
import com.pc.fash_android_mobile.data.user.UserSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val exploreUiPreferences = ExploreUiPreferences(application)

    private val _filtersExpanded = MutableStateFlow(exploreUiPreferences.readFiltersExpanded())
    val filtersExpanded: StateFlow<Boolean> = _filtersExpanded.asStateFlow()

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

    /** Top bar: expanded search field (from any tab’s search icon or Explore’s search). */
    private val _searchBarExpanded = MutableStateFlow(false)
    val searchBarExpanded: StateFlow<Boolean> = _searchBarExpanded.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** True when the user ran a text search (non-browse). */
    private val _isSearchMode = MutableStateFlow(false)
    val isSearchMode: StateFlow<Boolean> = _isSearchMode.asStateFlow()

    /**
     * Bumps on each new first-page request so stale responses (filter change, tab refresh) are dropped.
     * [loadMore] snapshots this at start and aborts if it changed mid-flight.
     */
    private val listingsFetchGeneration = AtomicInteger(0)

    fun setFiltersExpanded(expanded: Boolean) {
        if (_filtersExpanded.value == expanded) return
        _filtersExpanded.value = expanded
        exploreUiPreferences.writeFiltersExpanded(expanded)
    }

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
    }

    fun setSearchBarExpanded(expanded: Boolean) {
        _searchBarExpanded.value = expanded
        if (!expanded) {
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
        val sort = if (isSearch && q.isNotEmpty()) "recent" else "popular"
        return searchRepository.searchListings(
            q = q,
            categoryId = categoryId,
            tags = tag,
            limit = ExploreFeedPageSize,
            offset = offset,
            sort = sort,
        )
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
