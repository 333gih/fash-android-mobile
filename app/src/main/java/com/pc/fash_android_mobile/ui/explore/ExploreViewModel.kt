package com.pc.fash_android_mobile.ui.explore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.search.SearchRepository
import com.pc.fash_android_mobile.data.user.UserRepository
import com.pc.fash_android_mobile.data.user.UserSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExploreViewModel(application: Application) : AndroidViewModel(application) {

    private val listingRepository: ListingRepository =
        (application as FashApplication).listingRepository
    private val searchRepository: SearchRepository =
        (application as FashApplication).searchRepository
    private val userRepository: UserRepository =
        (application as FashApplication).userRepository

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _selectedTagIndex = MutableStateFlow(0) // 0 = All
    val selectedTagIndex: StateFlow<Int> = _selectedTagIndex.asStateFlow()

    private val _featuredSellers = MutableStateFlow<List<UserSearchResult>>(emptyList())
    val featuredSellers: StateFlow<List<UserSearchResult>> = _featuredSellers.asStateFlow()

    private val _listings = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val listings: StateFlow<List<ListingFeedItem>> = _listings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _loadError = MutableStateFlow(false)
    val loadError: StateFlow<Boolean> = _loadError.asStateFlow()

    private val _followingIds = MutableStateFlow<Set<String>>(emptySet())
    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = false
            withContext(Dispatchers.IO) {
                loadTags()
                loadFeaturedSellers()
                loadListings()
            }
            _isLoading.value = false
        }
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

    private suspend fun loadListings() {
        val selectedTag = if (_selectedTagIndex.value <= 0) null else _tags.value.getOrNull(_selectedTagIndex.value - 1)
        val result = listingRepository.getExploreFeed(
            limit = 20,
            offset = 0,
            tags = selectedTag,
        )
        result.fold(
            onSuccess = { _listings.value = it },
            onFailure = {
                _loadError.value = true
                _events.tryEmit(
                    it.message?.takeIf { m -> m.isNotBlank() }
                        ?: getApplication<Application>().getString(R.string.feed_load_error),
                )
            },
        )
    }

    fun selectTag(index: Int) {
        if (index == _selectedTagIndex.value) return
        _selectedTagIndex.value = index
        viewModelScope.launch {
            _isLoading.value = true
            val tag = if (index <= 0) null else _tags.value.getOrNull(index - 1)
            listingRepository.getExploreFeed(limit = 20, offset = 0, tags = tag).fold(
                onSuccess = { _listings.value = it },
                onFailure = { _loadError.value = true },
            )
            _isLoading.value = false
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
                loadListings()
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
}
