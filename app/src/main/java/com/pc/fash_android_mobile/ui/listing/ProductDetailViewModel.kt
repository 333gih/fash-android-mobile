package com.pc.fash_android_mobile.ui.listing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingDetail
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.listing.ProductDetailGuideStore
import com.pc.fash_android_mobile.data.order.OrderRepository
import com.pc.fash_android_mobile.data.realtime.RealtimeEvent
import com.pc.fash_android_mobile.data.realtime.RealtimeManager
import com.pc.fash_android_mobile.data.recommendation.FeedEventReporter
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Buyer's in-flight order on this listing (reserved for current user). */
data class BuyerActiveOrder(
    val orderId: String,
    val amountVnd: Long,
    val status: String,
)

/** Sticky bottom CTAs on product detail (realtime-aware). */
enum class ProductBottomBarMode {
    Normal,
    /** Another buyer reserved; amber copy. */
    ReservedOther,
    /** Current user has the active order; green copy. */
    ReservedBuyer,
    /** Terminal sold state. */
    Sold,
}

class ProductDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val fashApp: FashApplication = application as FashApplication
    private val listingRepository: ListingRepository = fashApp.listingRepository
    private fun isGuestBrowse(): Boolean = fashApp.isGuestBrowseActive
    private val userRepository: UserRepository =
        (application as FashApplication).userRepository
    private val orderRepository: OrderRepository =
        (application as FashApplication).orderRepository
    private val realtimeManager: RealtimeManager =
        (application as FashApplication).realtimeManager
    private val sessionStore =
        (application as FashApplication).authManager.sessionStore
    private val guideStore = ProductDetailGuideStore(application)

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

    private val _detail = MutableStateFlow<ListingDetail?>(null)
    val detail: StateFlow<ListingDetail?> = _detail.asStateFlow()

    private val _sellerProfile = MutableStateFlow<ProfileInfo?>(null)
    val sellerProfile: StateFlow<ProfileInfo?> = _sellerProfile.asStateFlow()

    private val _moreFromSeller = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val moreFromSeller: StateFlow<List<ListingFeedItem>> = _moreFromSeller.asStateFlow()

    private val _relatedByCategory = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val relatedByCategory: StateFlow<List<ListingFeedItem>> = _relatedByCategory.asStateFlow()

    private val _relatedByBrand = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val relatedByBrand: StateFlow<List<ListingFeedItem>> = _relatedByBrand.asStateFlow()

    private val _relatedByStyle = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val relatedByStyle: StateFlow<List<ListingFeedItem>> = _relatedByStyle.asStateFlow()

    private val _discoveryFeed = MutableStateFlow<List<ProductDiscoveryFeedEntry>>(emptyList())
    val discoveryFeed: StateFlow<List<ProductDiscoveryFeedEntry>> = _discoveryFeed.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    private val _bottomBarMode = MutableStateFlow(ProductBottomBarMode.Normal)
    val bottomBarMode: StateFlow<ProductBottomBarMode> = _bottomBarMode.asStateFlow()

    private val _buyerActiveOrder = MutableStateFlow<BuyerActiveOrder?>(null)
    val buyerActiveOrder: StateFlow<BuyerActiveOrder?> = _buyerActiveOrder.asStateFlow()

    private val _showPurchaseGuide = MutableStateFlow(false)
    val showPurchaseGuide: StateFlow<Boolean> = _showPurchaseGuide.asStateFlow()

    /** True while opening a conversation after Message — shows inline progress on the chat button. */
    private val _isOpeningChat = MutableStateFlow(false)
    val isOpeningChat: StateFlow<Boolean> = _isOpeningChat.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    private var activeListingId: String = ""
    private var listingEventsJob: Job? = null

    override fun onCleared() {
        super.onCleared()
        listingEventsJob?.cancel()
        if (activeListingId.isNotBlank()) {
            realtimeManager.unsubscribeFromListing(activeListingId)
        }
    }

    /** Clears PDP + listing realtime subscription after logout so the next user never sees stale CTAs. */
    fun clearCachesForSignedOutUser() {
        listingEventsJob?.cancel()
        listingEventsJob = null
        if (activeListingId.isNotBlank()) {
            runCatching { realtimeManager.unsubscribeFromListing(activeListingId) }
        }
        activeListingId = ""
        _detail.value = null
        _sellerProfile.value = null
        _moreFromSeller.value = emptyList()
        _relatedByCategory.value = emptyList()
        _relatedByBrand.value = emptyList()
        _relatedByStyle.value = emptyList()
        _discoveryFeed.value = emptyList()
        _isLoading.value = false
        _loadError.value = null
        _isFollowing.value = false
        _bottomBarMode.value = ProductBottomBarMode.Normal
        _buyerActiveOrder.value = null
        _showPurchaseGuide.value = false
        _isOpeningChat.value = false
    }

    fun loadDetail(listingId: String) {
        if (listingId.isBlank()) {
            _loadError.value = getApplication<Application>().getString(R.string.product_detail_error)
            _isLoading.value = false
            return
        }
        listingEventsJob?.cancel()
        if (activeListingId.isNotBlank() && !activeListingId.equals(listingId, ignoreCase = true)) {
            realtimeManager.unsubscribeFromListing(activeListingId)
        }
        viewModelScope.launch {
            _detail.value = null
            _sellerProfile.value = null
            _moreFromSeller.value = emptyList()
        _relatedByCategory.value = emptyList()
        _relatedByBrand.value = emptyList()
        _relatedByStyle.value = emptyList()
        _discoveryFeed.value = emptyList()
            _bottomBarMode.value = ProductBottomBarMode.Normal
            _buyerActiveOrder.value = null
            _showPurchaseGuide.value = false
            _isOpeningChat.value = false
            _isLoading.value = true
            _loadError.value = null
            val guestBrowse = isGuestBrowse()
            withContext(Dispatchers.IO) {
                val guest = guestBrowse
                val detailResult = listingRepository.getListingDetail(listingId, publicBrowse = guest)
                detailResult.fold(
                    onSuccess = { d ->
                        _detail.value = d
                        d.sellerIsFollowing?.let { _isFollowing.value = it }
                        val sid = d.sellerId?.takeIf { it.isNotBlank() }
                            ?: d.sellerUsername?.takeIf { it.isNotBlank() }
                        sid?.let { loadSellerAndMore(it, listingId, guest) }
                        if (!guest) {
                            listingRepository.recordView(listingId)
                        }
                    },
                    onFailure = {
                        _loadError.value = it.message?.takeIf { m -> m.isNotBlank() }
                            ?: getApplication<Application>().getString(R.string.product_detail_error)
                    },
                )
            }
            _isLoading.value = false
            _detail.value?.let { d ->
                applyBottomModeFromDetail(d, listingId)
                if (!guestBrowse) {
                    maybeShowPurchaseGuide(d)
                    startListingRealtime(listingId)
                }
            }
        }
    }

    private fun applyBottomModeFromDetail(d: ListingDetail, listingId: String) {
        when (d.status.lowercase()) {
            "sold" -> {
                _bottomBarMode.value = ProductBottomBarMode.Sold
                _buyerActiveOrder.value = null
            }
            "reserved" -> {
                _bottomBarMode.value = ProductBottomBarMode.ReservedOther
                _buyerActiveOrder.value = null
                viewModelScope.launch {
                    val active = loadBuyerActiveOrder(listingId)
                    if (active != null) {
                        _buyerActiveOrder.value = active
                        _bottomBarMode.value = ProductBottomBarMode.ReservedBuyer
                    }
                }
            }
            else -> {
                _bottomBarMode.value = ProductBottomBarMode.Normal
                _buyerActiveOrder.value = null
            }
        }
    }

    private fun maybeShowPurchaseGuide(d: ListingDetail) {
        val uid = sessionStore.read()?.userId?.trim().orEmpty()
        if (uid.isBlank() || guideStore.hasSeenPurchaseGuide(uid)) return
        if (d.status.equals("sold", ignoreCase = true)) return
        _showPurchaseGuide.value = true
    }

    fun dismissPurchaseGuide() {
        val uid = sessionStore.read()?.userId?.trim().orEmpty()
        if (uid.isNotBlank()) guideStore.markPurchaseGuideSeen(uid)
        _showPurchaseGuide.value = false
    }

    private fun startListingRealtime(listingId: String) {
        activeListingId = listingId
        realtimeManager.subscribeToListing(listingId)
        listingEventsJob?.cancel()
        listingEventsJob = viewModelScope.launch {
            realtimeManager.events.collect { event ->
                when (event) {
                    is RealtimeEvent.ListingReserved -> if (listingIdMatches(event.listingId)) {
                        _detail.update { it?.copy(status = "reserved") }
                        _bottomBarMode.value = ProductBottomBarMode.ReservedOther
                        _buyerActiveOrder.value = null
                        val lid = activeListingId
                        viewModelScope.launch {
                            val active = loadBuyerActiveOrder(lid)
                            if (active != null) {
                                _buyerActiveOrder.value = active
                                _bottomBarMode.value = ProductBottomBarMode.ReservedBuyer
                            }
                        }
                    }
                    is RealtimeEvent.ListingAvailable -> if (listingIdMatches(event.listingId)) {
                        _detail.update { it?.copy(status = "active") }
                        _bottomBarMode.value = ProductBottomBarMode.Normal
                        _buyerActiveOrder.value = null
                        _events.tryEmit(
                            getApplication<Application>().getString(R.string.product_listing_available_snackbar),
                        )
                    }
                    is RealtimeEvent.ListingSold -> if (listingIdMatches(event.listingId)) {
                        _detail.update { it?.copy(status = "sold") }
                        _bottomBarMode.value = ProductBottomBarMode.Sold
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun listingIdMatches(id: String): Boolean =
        id.isNotBlank() && activeListingId.equals(id.trim(), ignoreCase = true)

    private suspend fun loadBuyerActiveOrder(listingId: String): BuyerActiveOrder? {
        val myId = sessionStore.read()?.userId?.trim().orEmpty()
        if (myId.isBlank()) return null
        val orders = withContext(Dispatchers.IO) {
            orderRepository.getBuyingOrders(50, 0)
        }.getOrNull() ?: return null
        val active = setOf(
            "payment_pending", "payment_held", "in_transit", "pending",
            "cash_meetup_open", "fulfillment_pending",
        )
        val match = orders.firstOrNull { o ->
            o.listingId.equals(listingId, ignoreCase = true) &&
                o.status.lowercase() in active
        } ?: return null
        return BuyerActiveOrder(
            orderId = match.orderId,
            amountVnd = match.priceVnd,
            status = match.status,
        )
    }

    suspend fun findBuyerActiveOrderForListing(listingId: String): BuyerActiveOrder? =
        loadBuyerActiveOrder(listingId)

    private suspend fun loadSellerAndMore(sellerKey: String, excludeListingId: String, publicBrowse: Boolean = false) {
        val d = _detail.value ?: return
        val profileId = d.sellerUsername?.takeIf { it.isNotBlank() } ?: sellerKey
        val profileResult = if (publicBrowse) {
            userRepository.getProfilePublic(profileId)
        } else {
            userRepository.getProfile(profileId)
        }
        profileResult.fold(
            onSuccess = {
                _sellerProfile.value = it
                if (!publicBrowse && _detail.value?.sellerIsFollowing == null) {
                    it.isFollowing?.let { following -> _isFollowing.value = following }
                }
            },
            onFailure = { },
        )
        coroutineScope {
            val sellerRail = async {
                val moreResult = if (publicBrowse) {
                    listingRepository.getListingsBySellerPublic(
                        sellerKey,
                        limit = ProductDiscoveryFeedBuilder.SELLER_RAIL_LIMIT,
                    )
                } else {
                    listingRepository.getListingsBySeller(
                        sellerKey,
                        limit = ProductDiscoveryFeedBuilder.SELLER_RAIL_LIMIT,
                    )
                }
                moreResult.getOrNull()
                    ?.filter { it.id != excludeListingId }
                    ?.filter { (it.listingStatus ?: "").lowercase() != "sold" }
                    ?: emptyList()
            }
            val categoryRail = async {
                loadRelatedRail(
                    excludeListingId = excludeListingId,
                    publicBrowse = publicBrowse,
                    categoryId = d.categoryId?.trim()?.takeIf { it.isNotEmpty() },
                )
            }
            val brandRail = async {
                loadRelatedRail(
                    excludeListingId = excludeListingId,
                    publicBrowse = publicBrowse,
                    brandId = d.brandId?.trim()?.takeIf { it.isNotEmpty() },
                )
            }
            val tagIds = d.aestheticTagRefs.mapNotNull { it.id?.trim()?.takeIf { id -> id.isNotEmpty() } }
            val styleRail = async {
                loadRelatedRail(
                    excludeListingId = excludeListingId,
                    publicBrowse = publicBrowse,
                    aestheticTagIds = tagIds.takeIf { it.isNotEmpty() },
                )
            }
            _moreFromSeller.value = sellerRail.await()
            _relatedByCategory.value = categoryRail.await()
            _relatedByBrand.value = brandRail.await()
            _relatedByStyle.value = styleRail.await()
            val sellerBadge = d.sellerUsername?.trim()?.takeIf { it.isNotEmpty() }?.let { "@$it" }
                ?: d.sellerDisplayName?.trim()?.takeIf { it.isNotEmpty() }
                ?: getApplication<Application>().getString(R.string.product_relation_badge_seller)
            _discoveryFeed.value = ProductDiscoveryFeedBuilder.merge(
                detail = d,
                sellerLabel = sellerBadge,
                sellerItems = _moreFromSeller.value,
                categoryLabel = d.category?.trim()?.takeIf { it.isNotEmpty() }
                    ?: d.parentCategoryName?.trim()?.takeIf { it.isNotEmpty() },
                categoryItems = _relatedByCategory.value,
                brandLabel = d.brand?.trim()?.takeIf { it.isNotEmpty() },
                brandItems = _relatedByBrand.value,
                styleItems = _relatedByStyle.value,
                styleFallbackLabel = getApplication<Application>().getString(R.string.product_related_style),
            )
        }
    }

    private suspend fun loadRelatedRail(
        excludeListingId: String,
        publicBrowse: Boolean,
        categoryId: String? = null,
        brandId: String? = null,
        aestheticTagIds: List<String>? = null,
    ): List<ListingFeedItem> {
        if (categoryId == null && brandId == null && aestheticTagIds.isNullOrEmpty()) return emptyList()
        val searchRepository = fashApp.searchRepository
        val result = if (publicBrowse) {
            searchRepository.browseListings(
                categoryId = categoryId,
                brandId = brandId,
                aestheticTagIds = aestheticTagIds,
                limit = ProductDiscoveryFeedBuilder.RELATED_RAIL_LIMIT,
                offset = 0,
            )
        } else {
            searchRepository.searchListings(
                categoryId = categoryId,
                brandId = brandId,
                aestheticTagIds = aestheticTagIds,
                sort = "recent",
                limit = ProductDiscoveryFeedBuilder.RELATED_RAIL_LIMIT,
                offset = 0,
            )
        }
        return result.getOrNull()?.filter { it.id != excludeListingId } ?: emptyList()
    }

    private fun patchDiscoveryRails(
        itemId: String,
        transform: (ListingFeedItem) -> ListingFeedItem,
    ) {
        fun mapList(list: List<ListingFeedItem>) =
            list.map { if (it.id == itemId) transform(it) else it }
        _moreFromSeller.update { mapList(it) }
        _relatedByCategory.update { mapList(it) }
        _relatedByBrand.update { mapList(it) }
        _relatedByStyle.update { mapList(it) }
        _discoveryFeed.update { feed ->
            feed.map { entry ->
                if (entry.item.id == itemId) {
                    entry.copy(item = transform(entry.item))
                } else {
                    entry
                }
            }
        }
    }

    private fun followTargetOrNull(): String? {
        val d = _detail.value ?: return null
        return d.sellerId?.takeIf { it.isNotBlank() }
            ?: d.sellerUsername?.takeIf { it.isNotBlank() }
    }

    fun follow(sellerId: String?) {
        val target = sellerId?.takeIf { it.isNotBlank() } ?: followTargetOrNull()
        if (target.isNullOrBlank()) return
        viewModelScope.launch {
            userRepository.follow(target).fold(
                onSuccess = {
                    _isFollowing.update { true }
                    _detail.update { it?.copy(sellerIsFollowing = true) }
                    reportSellerFollowedFromPdp()
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

    fun unfollow(sellerId: String?) {
        val target = sellerId?.takeIf { it.isNotBlank() } ?: followTargetOrNull()
        if (target.isNullOrBlank()) return
        viewModelScope.launch {
            userRepository.unfollow(target).fold(
                onSuccess = {
                    _isFollowing.update { false }
                    _detail.update { it?.copy(sellerIsFollowing = false) }
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

    fun setFollowing(following: Boolean) {
        _isFollowing.value = following
    }

    /**
     * Records a share signal. Call from the PDP host right before invoking the Android share sheet
     * so the backend taste graph attributes the outbound link to this listing. Falls back to a no-op
     * when no listing is loaded.
     */
    fun reportShare() {
        val id = _detail.value?.id ?: return
        feedEventReporter.share(id, surface = "pdp")
    }

    /**
     * Records a high-intent chat-initiate signal — the user opened or created a chat thread about
     * this listing. Backend weights this above save (see core-service feed_events normalization).
     */
    fun reportChatInitiate() {
        val id = _detail.value?.id ?: return
        feedEventReporter.chatInitiate(id, surface = "pdp")
    }

    /** Viewer followed the seller from PDP — distinct surface from profile follow. */
    fun reportSellerFollowedFromPdp() {
        val id = _detail.value?.id ?: return
        feedEventReporter.followSeller(id, surface = "pdp")
    }

    fun toggleSave() {
        val d = _detail.value ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                listingRepository.toggleSave(d.id, d.isSaved)
            }
            result.fold(
                onSuccess = { saved ->
                    _detail.update { it?.copy(isSaved = saved) }
                    if (saved) feedEventReporter.save(d.id, surface = "pdp")
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

    fun toggleLike() {
        val d = _detail.value ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                listingRepository.toggleLike(d.id)
            }
            result.fold(
                onSuccess = { liked ->
                    _detail.update { cur ->
                        val c = cur ?: return@update null
                        val delta = when {
                            liked && !c.isLiked -> 1
                            !liked && c.isLiked -> -1
                            else -> 0
                        }
                        c.copy(
                            isLiked = liked,
                            likeCount = (c.likeCount + delta).coerceAtLeast(0),
                        )
                    }
                    if (liked) feedEventReporter.like(d.id, surface = "pdp")
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

    fun toggleLikeMoreFromSeller(item: ListingFeedItem) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                listingRepository.toggleLike(item.id)
            }
            result.fold(
                onSuccess = { liked ->
                    patchDiscoveryRails(item.id) {
                        val delta = when {
                            liked && !it.isLiked -> 1
                            !liked && it.isLiked -> -1
                            else -> 0
                        }
                        it.copy(
                            isLiked = liked,
                            likeCount = (it.likeCount + delta).coerceAtLeast(0),
                        )
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

    fun toggleSaveMoreFromSeller(item: ListingFeedItem) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                listingRepository.toggleSave(item.id, item.isSaved)
            }
            result.fold(
                onSuccess = { saved ->
                    patchDiscoveryRails(item.id) {
                        val delta = when {
                            saved && !it.isSaved -> 1
                            !saved && it.isSaved -> -1
                            else -> 0
                        }
                        it.copy(
                            isSaved = saved,
                            saveCount = (it.saveCount + delta).coerceAtLeast(0),
                        )
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

    fun retryLoad(listingId: String) {
        loadDetail(listingId)
    }

    fun clearError() {
        _loadError.value = null
    }

    fun setOpeningChat(opening: Boolean) {
        _isOpeningChat.value = opening
    }
}
