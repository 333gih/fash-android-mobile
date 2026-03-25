package com.pc.fash_android_mobile.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.realtime.RealtimeEvent
import com.pc.fash_android_mobile.data.realtime.RealtimeManager
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val listingRepository: ListingRepository =
        (application as FashApplication).listingRepository
    private val userRepository: UserRepository =
        (application as FashApplication).userRepository
    private val realtimeManager: RealtimeManager =
        (application as FashApplication).realtimeManager

    private val _items = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val items: StateFlow<List<ListingFeedItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** True when last load failed (network/server error). User can retry. */
    private val _loadError = MutableStateFlow(false)
    val loadError: StateFlow<Boolean> = _loadError.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    private val _likedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _savedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _followingIds = MutableStateFlow<Set<String>>(emptySet())
    val followingIds: kotlinx.coroutines.flow.StateFlow<Set<String>> = _followingIds.asStateFlow()

    init {
        loadFeed()
        // INTEGRATION.md §5 feed.refresh: server hints that new listings are available
        viewModelScope.launch {
            realtimeManager.events.collect { event ->
                if (event is RealtimeEvent.FeedRefresh) {
                    withContext(Dispatchers.IO) { fetchFeedWithFallback() }
                        .getOrNull()?.let { _items.value = it }
                }
            }
        }
    }

    fun loadFeed() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = false
            val result = withContext(Dispatchers.IO) {
                fetchFeedWithFallback()
            }
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    _items.value = it
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
    }

    /**
     * Home feed from followed sellers; if empty, fallback to Explore feed.
     * Covers: no follows, no listings from follows, no listings in explore.
     */
    private fun fetchFeedWithFallback(): Result<List<ListingFeedItem>> {
        val home = listingRepository.getHomeFeed(limit = 20, offset = 0)
        return if (home.isSuccess && home.getOrNull()?.isNotEmpty() == true) {
            home
        } else {
            listingRepository.getExploreFeed(limit = 20, offset = 0)
        }
    }

    fun retryLoad() {
        loadFeed()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _loadError.value = false
            val result = withContext(Dispatchers.IO) {
                fetchFeedWithFallback()
            }
            _isRefreshing.value = false
            result.fold(
                onSuccess = { _items.value = it },
                onFailure = { _loadError.value = true },
            )
        }
    }

    fun toggleLike(item: ListingFeedItem) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                listingRepository.toggleLike(item.id)
            }
            result.fold(
                onSuccess = { liked ->
                    _likedIds.update { if (liked) it + item.id else it - item.id }
                    _items.update { list ->
                        list.map {
                            if (it.id == item.id) {
                                it.copy(
                                    likeCount = if (liked) it.likeCount + 1 else (it.likeCount - 1).coerceAtLeast(0),
                                    isLiked = liked,
                                )
                            } else it
                        }
                    }
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

    fun toggleSave(item: ListingFeedItem) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                listingRepository.toggleSave(item.id)
            }
            result.fold(
                onSuccess = { saved ->
                    _savedIds.update { if (saved) it + item.id else it - item.id }
                    _items.update { list ->
                        list.map {
                            if (it.id == item.id) {
                                it.copy(
                                    saveCount = if (saved) it.saveCount + 1 else (it.saveCount - 1).coerceAtLeast(0),
                                    isSaved = saved,
                                )
                            } else it
                        }
                    }
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

    fun recordView(item: ListingFeedItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                listingRepository.recordView(item.id)
            }
        }
    }

    fun follow(sellerId: String?) {
        if (sellerId.isNullOrBlank()) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                userRepository.follow(sellerId)
            }
            result.fold(
                onSuccess = { resolvedUserId ->
                    _followingIds.update { set ->
                        var s = set + resolvedUserId
                        if (sellerId != resolvedUserId) s = s + sellerId
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

    fun isFollowing(sellerId: String?): Boolean {
        if (sellerId.isNullOrBlank()) return false
        val set = _followingIds.value
        return set.contains(sellerId)
    }
}
