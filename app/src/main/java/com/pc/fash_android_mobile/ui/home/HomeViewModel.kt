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
import com.pc.fash_android_mobile.data.recommendation.FeedEventReporter
import com.pc.fash_android_mobile.data.search.SearchRepository
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
) {
    /** Show journey row only when at least one stat is non-zero. */
    fun hasJourneyActivity(): Boolean =
        activeDeliveryOrders > 0 || savedListingsCount > 0 || unreadMessages > 0
}

private val BuyerDeliveringStatuses = setOf(
    "payment_held",
    "in_transit",
    "delivering",
    "shipped",
    "shipping",
)

private const val HomeHuntTodayPreviewLimit = 8

/** Size of one follow-feed page (`GET /api/v1/listings/home`). */
internal const val HomeFollowFeedPageSize = 20

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val fashApp: FashApplication = application as FashApplication
    private val listingRepository: ListingRepository = fashApp.listingRepository
    private val searchRepository: SearchRepository = fashApp.searchRepository
    private fun isGuestBrowse(): Boolean = fashApp.isGuestBrowseActive
    private val userRepository: UserRepository =
        (application as FashApplication).userRepository
    private val orderRepository: OrderRepository =
        (application as FashApplication).orderRepository
    private val chatRepository: ChatRepository =
        (application as FashApplication).chatRepository
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

    private val homeDiscoveryRepository: HomeDiscoveryRepository = HttpHomeDiscoveryRepository(
        editorialGuideRepository = fashApp.editorialGuideRepository,
        searchRepository = fashApp.searchRepository,
        listingRepository = fashApp.listingRepository,
        recommendationRepository = fashApp.recommendationRepository,
        guestBrowseProvider = { isGuestBrowse() },
        // Home rails honor the same "Match my size" toggle as Explore.
        sizingModeProvider = { com.pc.fash_android_mobile.data.explore.ExploreSizingPreference.read(application) },
    )

    private val _items = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val items: StateFlow<List<ListingFeedItem>> = _items.asStateFlow()

    /** Heat-ranked marketplace preview (`GET /search/listings`, browse mode) — same source as Explore. */
    private val _huntTodayItems = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val huntTodayItems: StateFlow<List<ListingFeedItem>> = _huntTodayItems.asStateFlow()

    private val _huntTodayLoading = MutableStateFlow(false)
    val huntTodayLoading: StateFlow<Boolean> = _huntTodayLoading.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** Follow-feed pagination — guards duplicate in-flight requests (no UI spinner). */
    private val _isLoadingMore = MutableStateFlow(false)

    /** Follow-feed pagination — false when last fetch returned < HomeFollowFeedPageSize. */
    private val _hasMoreItems = MutableStateFlow(true)
    val hasMoreItems: StateFlow<Boolean> = _hasMoreItems.asStateFlow()

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

    /**
     * True when (a) the viewer is signed in, (b) their profile has no reference_size or measurements,
     * and (c) they haven't dismissed the banner. Drives the small "Add my size" prompt at the top of
     * the home feed — mirrors the Explore quick toggle nudge so users hit the same CTA wherever they
     * are in the app.
     */
    private val _showSizingBanner = MutableStateFlow(false)
    val showSizingBanner: StateFlow<Boolean> = _showSizingBanner.asStateFlow()

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { reloadDiscoveryBundle() }
        }
        viewModelScope.launch {
            AppLocale.localeRevisionFlow.collect {
                withContext(Dispatchers.IO) { reloadDiscoveryBundle() }
            }
        }
        viewModelScope.launch(Dispatchers.IO) { refreshSizingBannerState() }
        loadFeed()
        // INTEGRATION.md §5 feed.refresh: server hints that new listings are available
        viewModelScope.launch {
            if (isGuestBrowse()) return@launch
            realtimeManager.events.collect { event ->
                if (event is RealtimeEvent.FeedRefresh) {
                    withContext(Dispatchers.IO) {
                        loadBuyerHomeStats()
                        fetchHuntTodayPreview()
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

    /**
     * Re-evaluates whether the Home "Add your size" banner should be shown. Reads the viewer's
     * profile from network — same call the Explore VM makes — but is best-effort and silently fails
     * (banner stays hidden on failure).
     */
    private suspend fun refreshSizingBannerState() {
        if (isGuestBrowse()) {
            _showSizingBanner.value = false
            return
        }
        val ctx = getApplication<Application>().applicationContext
        if (com.pc.fash_android_mobile.data.home.HomeSizingBannerPreference.isDismissed(ctx)) {
            _showSizingBanner.value = false
            return
        }
        val uid = fashApp.authManager.sessionStore.read()?.userId
        if (uid.isNullOrBlank()) {
            _showSizingBanner.value = false
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
                _showSizingBanner.value = !hasSize && !hasMeasurement
            },
            onFailure = { _showSizingBanner.value = false },
        )
    }

    /** Call after the viewer saves profile sizing so the Home banner hides without restarting. */
    fun refreshSizingBannerAfterProfileSave() {
        viewModelScope.launch(Dispatchers.IO) { refreshSizingBannerState() }
    }

    /** Permanently hides the Home sizing banner until SharedPreferences are cleared (sign-out wipes them). */
    fun dismissSizingBanner() {
        val ctx = getApplication<Application>().applicationContext
        com.pc.fash_android_mobile.data.home.HomeSizingBannerPreference.markDismissed(ctx)
        _showSizingBanner.value = false
    }

    private suspend fun loadBuyerHomeStats() {
        if (isGuestBrowse()) {
            _buyerStats.value = BuyerHomeStats()
            return
        }
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
                fetchHuntTodayPreview()
                fetchHomeFeedWithRetry()
            }
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    _items.value = it
                    syncSellerFollowingFromListings(it)
                    syncSellerFollowingFromListings(_huntTodayItems.value)
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
    private suspend fun fetchHuntTodayPreview() {
        _huntTodayLoading.value = true
        val result = fashApp.recommendationRepository.exploreListings(
            publicBrowse = isGuestBrowse(),
            limit = HomeHuntTodayPreviewLimit,
            offset = 0,
        )
        _huntTodayLoading.value = false
        result.fold(
            onSuccess = { _huntTodayItems.value = it },
            onFailure = { /* keep last preview; home still usable */ },
        )
    }

    private suspend fun fetchHomeFeedWithRetry(): Result<List<ListingFeedItem>> {
        if (isGuestBrowse()) {
            return Result.success(emptyList())
        }
        suspend fun once(): Result<List<ListingFeedItem>> =
            listingRepository.getHomeFeed(limit = HomeFollowFeedPageSize, offset = 0)
        var result = once()
        if (result.isFailure) {
            delay(400)
            result = once()
        }
        // Initial page resets "has more": only continue paginating if we got a full page.
        result.onSuccess { page -> _hasMoreItems.value = page.size >= HomeFollowFeedPageSize }
        return result
    }

    /**
     * Fetch the next page of follow-feed listings (offset = current size). Idempotent: the call is
     * a no-op when a previous load is still in flight, when there are no more items, when we are in
     * guest browse mode, or when nothing has loaded yet. Throttled so fast scroll at the feed/style
     * boundary does not spam the API or recompose the list.
     */
    private var lastFollowFeedLoadMoreAtMs = 0L

    fun loadMoreFollowFeed() {
        if (isGuestBrowse()) return
        if (_isLoadingMore.value || !_hasMoreItems.value) return
        if (_isLoading.value || _isRefreshing.value) return
        val offset = _items.value.size
        if (offset == 0) return
        val now = System.currentTimeMillis()
        if (now - lastFollowFeedLoadMoreAtMs < 900) return
        lastFollowFeedLoadMoreAtMs = now
        viewModelScope.launch {
            _isLoadingMore.value = true
            val result = withContext(Dispatchers.IO) {
                listingRepository.getHomeFeed(limit = HomeFollowFeedPageSize, offset = offset)
            }
            _isLoadingMore.value = false
            result.fold(
                onSuccess = { page ->
                    if (page.isEmpty()) {
                        _hasMoreItems.value = false
                    } else {
                        _items.update { current ->
                            // De-dupe by id in case of overlap between pages (server hint changes).
                            val existingIds = current.mapTo(HashSet(current.size)) { it.id }
                            current + page.filter { existingIds.add(it.id) }
                        }
                        syncSellerFollowingFromListings(page)
                        _hasMoreItems.value = page.size >= HomeFollowFeedPageSize
                    }
                },
                onFailure = {
                    // Soft fail: surface a snackbar, don't disable further attempts.
                    _events.tryEmit(
                        it.message?.takeIf { m -> m.isNotBlank() }
                            ?: getApplication<Application>().getString(R.string.feed_load_error),
                    )
                },
            )
        }
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
        _huntTodayItems.value = emptyList()
        _huntTodayLoading.value = false
        _likedIds.value = emptySet()
        _savedIds.value = emptySet()
        _followingIds.value = emptySet()
        _buyerStats.value = BuyerHomeStats()
        _discoveryBundle.value = HomeDiscoveryBundle()
        _loadError.value = false
        _isLoading.value = false
        _isRefreshing.value = false
        _isLoadingMore.value = false
        _hasMoreItems.value = true
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
                fetchHuntTodayPreview()
                fetchHomeFeedWithRetry()
            }
            _isRefreshing.value = false
            result.fold(
                onSuccess = {
                    _items.value = it
                    syncSellerFollowingFromListings(it)
                    syncSellerFollowingFromListings(_huntTodayItems.value)
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
                    updateListingInFeeds(item.id) {
                        it.copy(
                            likeCount = if (liked) it.likeCount + 1 else (it.likeCount - 1).coerceAtLeast(0),
                            isLiked = liked,
                        )
                    }
                    if (liked) feedEventReporter.like(item.id, surface = "home")
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
                    updateListingInFeeds(item.id) {
                        val delta = when {
                            saved && !it.isSaved -> 1
                            !saved && it.isSaved -> -1
                            else -> 0
                        }
                        it.copy(
                            saveCount = (it.saveCount + delta).coerceAtLeast(0),
                            isSaved = saved,
                        )
                    }
                    viewModelScope.launch {
                        withContext(Dispatchers.IO) { loadBuyerHomeStats() }
                    }
                    if (saved) feedEventReporter.save(item.id, surface = "home")
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

    fun recordView(item: ListingFeedItem, position: Int = 0, surface: String = "home") {
        feedEventReporter.impression(item.id, surface = surface, position = position)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                listingRepository.recordView(item.id)
            }
        }
    }

    fun recordDwell(item: ListingFeedItem, surface: String, position: Int, dwellMs: Int) {
        if (dwellMs < 800) return
        feedEventReporter.dwell(item.id, surface = surface, position = position, dwellMs = dwellMs)
    }

    fun reportListingClick(item: ListingFeedItem, surface: String, position: Int = 0) {
        feedEventReporter.click(item.id, surface = surface, position = position)
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

    /**
     * Returns the aesthetic tag ID for the given [tagName] by searching the cached
     * [trendingStyleTagChips] in the discovery bundle.
     * Returns an empty string when no matching chip is found (caller falls back to name-based lookup).
     */
    fun trendingStyleTagChipIdForName(tagName: String): String {
        return discoveryBundle.value.trendingStyleTagChips
            .firstOrNull { it.name.equals(tagName.trim(), ignoreCase = true) }
            ?.id
            .orEmpty()
    }

    private fun updateListingInFeeds(listingId: String, transform: (ListingFeedItem) -> ListingFeedItem) {
        fun List<ListingFeedItem>.patch() = map { if (it.id == listingId) transform(it) else it }
        _items.update { it.patch() }
        _huntTodayItems.update { it.patch() }
        _discoveryBundle.update { bundle ->
            bundle.copy(
                recentlyViewed = bundle.recentlyViewed.patch(),
                stylePicks = bundle.stylePicks.patch(),
                similarToSaved = bundle.similarToSaved.patch(),
                forYou = bundle.forYou.patch(),
            )
        }
    }
}
