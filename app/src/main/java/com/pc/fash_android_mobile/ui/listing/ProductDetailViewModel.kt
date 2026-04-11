package com.pc.fash_android_mobile.ui.listing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingDetail
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.order.OrderRepository
import com.pc.fash_android_mobile.data.realtime.RealtimeEvent
import com.pc.fash_android_mobile.data.realtime.RealtimeManager
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private val listingRepository: ListingRepository =
        (application as FashApplication).listingRepository
    private val userRepository: UserRepository =
        (application as FashApplication).userRepository
    private val orderRepository: OrderRepository =
        (application as FashApplication).orderRepository
    private val realtimeManager: RealtimeManager =
        (application as FashApplication).realtimeManager
    private val sessionStore =
        (application as FashApplication).authManager.sessionStore

    private val _detail = MutableStateFlow<ListingDetail?>(null)
    val detail: StateFlow<ListingDetail?> = _detail.asStateFlow()

    private val _sellerProfile = MutableStateFlow<ProfileInfo?>(null)
    val sellerProfile: StateFlow<ProfileInfo?> = _sellerProfile.asStateFlow()

    private val _moreFromSeller = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val moreFromSeller: StateFlow<List<ListingFeedItem>> = _moreFromSeller.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    private val _bottomBarMode = MutableStateFlow(ProductBottomBarMode.Normal)
    val bottomBarMode: StateFlow<ProductBottomBarMode> = _bottomBarMode.asStateFlow()

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
            _bottomBarMode.value = ProductBottomBarMode.Normal
            _isOpeningChat.value = false
            _isLoading.value = true
            _loadError.value = null
            withContext(Dispatchers.IO) {
                val detailResult = listingRepository.getListingDetail(listingId)
                detailResult.fold(
                    onSuccess = { d ->
                        _detail.value = d
                        d.sellerIsFollowing?.let { _isFollowing.value = it }
                        val sid = d.sellerId?.takeIf { it.isNotBlank() }
                            ?: d.sellerUsername?.takeIf { it.isNotBlank() }
                        sid?.let { loadSellerAndMore(it, listingId) }
                        listingRepository.recordView(listingId)
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
                startListingRealtime(listingId)
            }
        }
    }

    private fun applyBottomModeFromDetail(d: ListingDetail, listingId: String) {
        when (d.status.lowercase()) {
            "sold" -> _bottomBarMode.value = ProductBottomBarMode.Sold
            "reserved" -> {
                _bottomBarMode.value = ProductBottomBarMode.ReservedOther
                viewModelScope.launch {
                    if (resolveBuyerForListing(listingId)) {
                        _bottomBarMode.value = ProductBottomBarMode.ReservedBuyer
                    }
                }
            }
            else -> _bottomBarMode.value = ProductBottomBarMode.Normal
        }
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
                        val lid = activeListingId
                        viewModelScope.launch {
                            if (resolveBuyerForListing(lid)) {
                                _bottomBarMode.value = ProductBottomBarMode.ReservedBuyer
                            }
                        }
                    }
                    is RealtimeEvent.ListingAvailable -> if (listingIdMatches(event.listingId)) {
                        _detail.update { it?.copy(status = "active") }
                        _bottomBarMode.value = ProductBottomBarMode.Normal
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

    private suspend fun resolveBuyerForListing(listingId: String): Boolean {
        val myId = sessionStore.read()?.userId?.trim().orEmpty()
        if (myId.isBlank()) return false
        val orders = withContext(Dispatchers.IO) {
            orderRepository.getBuyingOrders(50, 0)
        }.getOrNull() ?: return false
        val active = setOf(
            "payment_pending", "payment_held", "in_transit", "pending", "cash_meetup_open",
        )
        return orders.any { o ->
            o.listingId.equals(listingId, ignoreCase = true) &&
                o.status.lowercase() in active
        }
    }

    private suspend fun loadSellerAndMore(sellerKey: String, excludeListingId: String) {
        val d = _detail.value
        val profileId = d?.sellerUsername?.takeIf { it.isNotBlank() } ?: sellerKey
        val profileResult = userRepository.getProfile(profileId)
        profileResult.fold(
            onSuccess = {
                _sellerProfile.value = it
                if (_detail.value?.sellerIsFollowing == null) {
                    it.isFollowing?.let { following -> _isFollowing.value = following }
                }
            },
            onFailure = { },
        )
        val moreResult = listingRepository.getListingsBySeller(sellerKey, limit = 5)
        moreResult.fold(
            onSuccess = { list ->
                _moreFromSeller.value = list.filter { it.id != excludeListingId }.take(5)
            },
            onFailure = { _moreFromSeller.value = emptyList() },
        )
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

    fun toggleSave() {
        val d = _detail.value ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                listingRepository.toggleSave(d.id, d.isSaved)
            }
            result.fold(
                onSuccess = { saved ->
                    _detail.update { it?.copy(isSaved = saved) }
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
                    _moreFromSeller.update { list ->
                        list.map {
                            if (it.id != item.id) return@map it
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
                    _moreFromSeller.update { list ->
                        list.map {
                            if (it.id != item.id) return@map it
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
