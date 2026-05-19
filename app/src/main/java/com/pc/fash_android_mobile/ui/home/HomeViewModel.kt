package com.pc.fash_android_mobile.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.chat.ChatRepository
import com.pc.fash_android_mobile.data.home.HomeDiscoveryBundle
import com.pc.fash_android_mobile.data.home.HomeDiscoveryRepository
import com.pc.fash_android_mobile.data.home.HttpHomeDiscoveryRepository
import com.pc.fash_android_mobile.data.locale.AppLocale
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.order.OrderRepository
import com.pc.fash_android_mobile.data.realtime.RealtimeEvent
import com.pc.fash_android_mobile.data.realtime.RealtimeManager
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
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

/** Buyer dashboard counts for the home journey row (orders in delivery, wishlist size, chat unread). */
data class BuyerHomeStats(
    val activeDeliveryOrders: Int = 0,
    val savedListingsCount: Int = 0,
    val unreadMessages: Int = 0,
)

private val BuyerDeliveringStatuses = setOf(
    "payment_held",
    "in_transit",
    "delivering",
    "shipped",
    "shipping",
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val listingRepository: ListingRepository =
        (application as FashApplication).listingRepository
    private val userRepository: UserRepository =
        (application as FashApplication).userRepository
    private val orderRepository: OrderRepository =
        (application as FashApplication).orderRepository
    private val chatRepository: ChatRepository =
        (application as FashApplication).chatRepository
    private val realtimeManager: RealtimeManager =
        (application as FashApplication).realtimeManager

    private val homeDiscoveryRepository: HomeDiscoveryRepository = run {
        val app = application as FashApplication
        HttpHomeDiscoveryRepository(
            editorialGuideRepository = app.editorialGuideRepository,
            searchRepository = app.searchRepository,
            listingRepository = app.listingRepository,
        )
    }

    private val _items = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val items: StateFlow<List<ListingFeedItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _scrollHomeToTop = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scrollHomeToTop: SharedFlow<Unit> = _scrollHomeToTop.asSharedFlow()

    /** True when last load failed (network/server error). User can retry. */
    private val _loadError = MutableStateFlow(false)
    val loadError: StateFlow<Boolean> = _loadError.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    private val _likedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _savedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _followingIds = MutableStateFlow<Set<String>>(emptySet())
    val followingIds: kotlinx.coroutines.flow.StateFlow<Set<String>> = _followingIds.asStateFlow()

    private val _buyerStats = MutableStateFlow(BuyerHomeStats())
    val buyerStats: StateFlow<BuyerHomeStats> = _buyerStats.asStateFlow()

    private val _discoveryBundle = MutableStateFlow(HomeDiscoveryBundle())
    val discoveryBundle: StateFlow<HomeDiscoveryBundle> = _discoveryBundle.asStateFlow()

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { reloadDiscoveryBundle() }
        }
        viewModelScope.launch {
            AppLocale.localeRevisionFlow.collect {
                withContext(Dispatchers.IO) { reloadDiscoveryBundle() }
            }
        }
        loadFeed()
        // INTEGRATION.md §5 feed.refresh: server hints that new listings are available
        viewModelScope.launch {
            realtimeManager.events.collect { event ->
                if (event is RealtimeEvent.FeedRefresh) {
                    withContext(Dispatchers.IO) {
                        loadBuyerHomeStats()
                        fetchHomeFeedWithRetry()
                    }.getOrNull()?.let { feed ->
                        _items.value = feed
                        syncSellerFollowingFromListings(feed)
                    }
                }
            }
        }
    }

    private suspend fun reloadDiscoveryBundle() {
        homeDiscoveryRepository.loadDiscoveryBundle().fold(
            onSuccess = { _discoveryBundle.value = it },
            onFailure = { /* keep last good payload; stub should not fail */ },
        )
    }

    private suspend fun loadBuyerHomeStats() {
        val orders = orderRepository.getBuyingOrders(limit = 50, offset = 0).getOrElse { emptyList() }
        val delivering = orders.count { it.status in BuyerDeliveringStatuses }
        val saved = listingRepository.getWishlistSavedCount(limit = 100, offset = 0).getOrElse { 0 }
        val unread = chatRepository.getUnreadCount().getOrElse { 0 }
        _buyerStats.value = BuyerHomeStats(
            activeDeliveryOrders = delivering,
            savedListingsCount = saved,
            unreadMessages = unread,
        )
    }

    fun loadFeed() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = false
            val result = withContext(Dispatchers.IO) {
                loadBuyerHomeStats()
                reloadDiscoveryBundle()
                fetchHomeFeedWithRetry()
            }
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    _items.value = it
                    syncSellerFollowingFromListings(it)
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
     * `GET /api/v1/listings/home` only — from followed sellers (core-service).
     * Empty list is valid when the user follows nobody; use Explore for discovery.
     * One automatic retry on failure to smooth transient network errors.
     */
    private suspend fun fetchHomeFeedWithRetry(): Result<List<ListingFeedItem>> {
        suspend fun once(): Result<List<ListingFeedItem>> =
            listingRepository.getHomeFeed(limit = 20, offset = 0)
        var result = once()
        if (result.isFailure) {
            delay(400)
            result = once()
        }
        return result
    }

    fun retryLoad() {
        loadFeed()
    }

    /**
     * Clears user-specific feed state when the session ends so the shell never briefly shows
     * the previous account’s home after logout / account switch.
     */
    fun clearCachesForSignedOutUser() {
        _items.value = emptyList()
        _likedIds.value = emptySet()
        _savedIds.value = emptySet()
        _followingIds.value = emptySet()
        _buyerStats.value = BuyerHomeStats()
        _discoveryBundle.value = HomeDiscoveryBundle()
        _loadError.value = false
        _isLoading.value = false
        _isRefreshing.value = false
    }

    /** Bottom nav re-tap on Home — scroll feed to top (pairs with [refresh]). */
    fun requestScrollHomeToTop() {
        viewModelScope.launch { _scrollHomeToTop.emit(Unit) }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _loadError.value = false
            val result = withContext(Dispatchers.IO) {
                loadBuyerHomeStats()
                reloadDiscoveryBundle()
                fetchHomeFeedWithRetry()
            }
            _isRefreshing.value = false
            result.fold(
                onSuccess = {
                    _items.value = it
                    syncSellerFollowingFromListings(it)
                },
                onFailure = { _loadError.value = true },
            )
        }
    }

    /**
     * Aligns local follow chip state with `seller.is_following` from listing payloads (viewer batched flags).
     */
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
                    _events.tryEmit(
                        getApplication<Application>().getString(
                            if (liked) R.string.listing_like_added_snackbar else R.string.listing_like_removed_snackbar,
                        ),
                    )
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
                listingRepository.toggleSave(item.id, item.isSaved)
            }
            result.fold(
                onSuccess = { saved ->
                    _savedIds.update { if (saved) it + item.id else it - item.id }
                    _items.update { list ->
                        list.map {
                            if (it.id == item.id) {
                                val delta = when {
                                    saved && !it.isSaved -> 1
                                    !saved && it.isSaved -> -1
                                    else -> 0
                                }
                                it.copy(
                                    saveCount = (it.saveCount + delta).coerceAtLeast(0),
                                    isSaved = saved,
                                )
                            } else it
                        }
                    }
                    viewModelScope.launch {
                        withContext(Dispatchers.IO) { loadBuyerHomeStats() }
                    }
                    _events.tryEmit(
                        getApplication<Application>().getString(
                            if (saved) R.string.listing_save_added_snackbar else R.string.listing_save_removed_snackbar,
                        ),
                    )
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
