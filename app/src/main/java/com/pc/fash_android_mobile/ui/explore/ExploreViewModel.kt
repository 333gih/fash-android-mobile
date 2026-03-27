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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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

    /** True when grid shows `/search/listings` results (not explore feed). */
    private val _isSearchMode = MutableStateFlow(false)
    val isSearchMode: StateFlow<Boolean> = _isSearchMode.asStateFlow()

    /**
     * Bumps on each new explore-listings request. Completed responses with a stale
     * generation are dropped so an in-flight [loadAll] cannot overwrite a newer tag filter.
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
                    fetchExploreListingsForCurrentTag()
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
                    fetchExploreListingsForCurrentTag()
                    _isLoading.value = false
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /** Runs `GET /search/listings` with current query + category + aesthetic tag. */
    fun submitSearch() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = false
            _isSearchMode.value = true
            runSearchWithCurrentFilters()
            _isLoading.value = false
        }
    }

    private suspend fun runSearchWithCurrentFilters() {
        val gen = listingsFetchGeneration.incrementAndGet()
        val q = _searchQuery.value.trim()
        val idx = _selectedTagIndex.value
        val tag = if (idx <= 0) null else _tags.value.getOrNull(idx - 1)
        val categoryId = _selectedCategoryId.value
        val result = withContext(Dispatchers.IO) {
            searchRepository.searchListings(
                q = q,
                categoryId = categoryId,
                tags = tag,
                limit = ExploreFeedPageSize,
                offset = 0,
            )
        }
        if (gen != listingsFetchGeneration.get()) return
        result.fold(
            onSuccess = {
                _listings.value = it
                _loadError.value = false
            },
            onFailure = {
                _loadError.value = true
                _events.tryEmit(
                    it.message?.takeIf { m -> m.isNotBlank() }
                        ?: getApplication<Application>().getString(R.string.feed_load_error),
                )
            },
        )
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
            fetchExploreListingsForCurrentTag()
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

    /**
     * Loads explore feed for the current [_selectedTagIndex] on [Dispatchers.IO] and
     * applies results only if this request is still the latest (see [listingsFetchGeneration]).
     */
    private suspend fun fetchExploreListingsForCurrentTag() {
        val gen = listingsFetchGeneration.incrementAndGet()
        val idx = _selectedTagIndex.value
        val tag = if (idx <= 0) null else _tags.value.getOrNull(idx - 1)
        val categoryId = _selectedCategoryId.value
        val result = withContext(Dispatchers.IO) {
            exploreFeedWithRetry(tags = tag, categoryId = categoryId)
        }
        if (gen != listingsFetchGeneration.get()) return
        result.fold(
            onSuccess = {
                _listings.value = it
                _loadError.value = false
            },
            onFailure = {
                _loadError.value = true
                _events.tryEmit(
                    it.message?.takeIf { m -> m.isNotBlank() }
                        ?: getApplication<Application>().getString(R.string.feed_load_error),
                )
            },
        )
    }

    private suspend fun exploreFeedWithRetry(
        tags: String?,
        categoryId: String? = null,
    ): Result<List<ListingFeedItem>> {
        suspend fun once(): Result<List<ListingFeedItem>> =
            listingRepository.getExploreFeed(
                limit = ExploreFeedPageSize,
                offset = 0,
                tags = tags,
                categoryId = categoryId?.takeIf { it.isNotBlank() },
            )
        var result = once()
        if (result.isFailure) {
            delay(400)
            result = once()
        }
        return result
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
                fetchExploreListingsForCurrentTag()
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
                fetchExploreListingsForCurrentTag()
            }
            _isLoading.value = false
        }
    }

    /**
     * Call when the user switches to the Explore tab so the grid refreshes from the API
     * (picks up realtime and other changes while they were on another tab).
     */
    fun onExploreTabSelected() {
        viewModelScope.launch {
            if (_isSearchMode.value) {
                runSearchWithCurrentFilters()
            } else {
                fetchExploreListingsForCurrentTag()
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
                fetchExploreListingsForCurrentTag()
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
