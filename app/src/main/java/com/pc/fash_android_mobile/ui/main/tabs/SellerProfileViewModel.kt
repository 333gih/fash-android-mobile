package com.pc.fash_android_mobile.ui.main.tabs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.data.user.SellerFocusForbiddenException
import com.pc.fash_android_mobile.data.user.SellerFocusUnauthorizedException
import com.pc.fash_android_mobile.data.user.SellerListingFocus
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Loads another user's storefront via [UserRepository.getProfile] and seller listings;
 * follow/unfollow via [UserRepository.follow] / [UserRepository.unfollow].
 */
class SellerProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository =
        (application as FashApplication).userRepository
    private val listingRepository: ListingRepository =
        (application as FashApplication).listingRepository
    private val sessionStore =
        (application as FashApplication).authManager.sessionStore

    private val fashApp = application as FashApplication

    private fun isGuestBrowse(): Boolean = fashApp.isGuestBrowseActive

    private val _profile = MutableStateFlow<ProfileInfo?>(null)
    val profile: StateFlow<ProfileInfo?> = _profile.asStateFlow()

    private val _sellingListings = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val sellingListings: StateFlow<List<ListingFeedItem>> = _sellingListings.asStateFlow()

    private val _soldListings = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val soldListings: StateFlow<List<ListingFeedItem>> = _soldListings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow(false)
    val loadError: StateFlow<Boolean> = _loadError.asStateFlow()

    /** Mirrors server + local toggles after follow/unfollow. */
    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    private val _followInFlight = MutableStateFlow(false)
    val followInFlight: StateFlow<Boolean> = _followInFlight.asStateFlow()

    /** From `GET …/users/{id}/seller-focus`; null until loaded or skipped (guest). */
    private val _sellerFocus = MutableStateFlow<SellerListingFocus?>(null)
    val sellerFocus: StateFlow<SellerListingFocus?> = _sellerFocus.asStateFlow()

    private val _sellerFocusForbidden = MutableStateFlow(false)
    val sellerFocusForbidden: StateFlow<Boolean> = _sellerFocusForbidden.asStateFlow()

    private val _sellerFocusLoading = MutableStateFlow(false)
    val sellerFocusLoading: StateFlow<Boolean> = _sellerFocusLoading.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var activeKey: String? = null

    private var loadForSellerJob: Job? = null

    fun loadForSeller(username: String) {
        val key = username.trim().removePrefix("@")
        if (key.isBlank()) return
        // Same seller already shown — skip duplicate network when the screen recomposes (e.g. after navigation).
        if (key == activeKey && _profile.value != null) return
        loadForSellerJob?.cancel()
        loadForSellerJob = viewModelScope.launch {
            if (activeKey != key) {
                activeKey = key
                _profile.value = null
                _sellingListings.value = emptyList()
                _soldListings.value = emptyList()
                _isFollowing.value = false
                _sellerFocus.value = null
                _sellerFocusForbidden.value = false
                _sellerFocusLoading.value = false
            }
            // Same seller revisit: keep showing content; only first load / seller change uses blocking UI.
            val showBlockingUi = _profile.value == null
            if (showBlockingUi) _isLoading.value = true
            try {
                _loadError.value = false
                withContext(Dispatchers.IO) {
                    val profileResult = if (isGuestBrowse()) {
                        userRepository.getProfilePublic(key)
                    } else {
                        userRepository.getProfile(key)
                    }
                    profileResult.fold(
                        onSuccess = { prof ->
                            _profile.value = prof
                            _isFollowing.value = if (isGuestBrowse()) false else prof.isFollowing ?: false
                        },
                        onFailure = { _loadError.value = true },
                    )
                }
                _profile.value?.userId?.takeIf { it.isNotBlank() }?.let { loadListings(it) }
                loadSellerFocus(key)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadSellerFocus(key: String) {
        viewModelScope.launch {
            if (sessionStore.read() == null) {
                _sellerFocus.value = null
                _sellerFocusForbidden.value = false
                _sellerFocusLoading.value = false
                return@launch
            }
            _sellerFocusLoading.value = true
            _sellerFocusForbidden.value = false
            withContext(Dispatchers.IO) {
                userRepository.getSellerListingFocus(key)
            }.fold(
                onSuccess = { focus ->
                    _sellerFocus.value = focus
                    _sellerFocusForbidden.value = false
                },
                onFailure = { e ->
                    when (e) {
                        is SellerFocusForbiddenException -> {
                            _sellerFocus.value = null
                            _sellerFocusForbidden.value = true
                        }
                        is SellerFocusUnauthorizedException -> {
                            _sellerFocus.value = null
                            _sellerFocusForbidden.value = false
                        }
                        else -> {
                            _sellerFocus.value = null
                            _sellerFocusForbidden.value = false
                        }
                    }
                },
            )
            _sellerFocusLoading.value = false
        }
    }

    fun retryLoad(username: String) = loadForSeller(username)

    /** True when logged in and the opened profile is not the current user. */
    fun canFollowSeller(): Boolean {
        if (isGuestBrowse()) return false
        val my = sessionStore.read()?.userId?.trim().orEmpty()
        if (my.isBlank()) return false
        val seller = _profile.value?.userId?.trim().orEmpty()
        if (seller.isBlank()) return false
        return !my.equals(seller, ignoreCase = true)
    }

    /** Show follow CTA on seller shop (guest sees login prompt on tap). */
    fun canShowFollowUi(): Boolean {
        val seller = _profile.value?.userId?.trim().orEmpty()
        if (seller.isBlank()) return false
        if (isGuestBrowse()) return true
        return canFollowSeller()
    }

    fun toggleFollow() {
        val p = _profile.value ?: return
        if (isGuestBrowse() || !canFollowSeller()) return
        val target = p.username.trim().ifBlank { p.userId.trim() }
        if (target.isBlank()) return
        viewModelScope.launch {
            _followInFlight.value = true
            try {
                val unfollow = _isFollowing.value
                val result = withContext(Dispatchers.IO) {
                    if (unfollow) userRepository.unfollow(target) else userRepository.follow(target)
                }
                result.fold(
                    onSuccess = {
                        val nowFollowing = !unfollow
                        _isFollowing.value = nowFollowing
                        val fc = p.followerCount
                        _profile.value = p.copy(
                            isFollowing = nowFollowing,
                            followerCount = (fc + if (nowFollowing) 1 else -1).coerceAtLeast(0),
                        )
                        if (nowFollowing) {
                            _events.emit(
                                getApplication<Application>().getString(R.string.follow_success),
                            )
                        }
                    },
                    onFailure = { e ->
                        _events.emit(
                            e.message?.takeIf { m -> m.isNotBlank() }
                                ?: getApplication<Application>().getString(R.string.feed_action_error),
                        )
                    },
                )
            } finally {
                _followInFlight.value = false
            }
        }
    }

    private suspend fun loadListings(sellerId: String) {
        withContext(Dispatchers.IO) {
            if (isGuestBrowse()) {
                listingRepository.getListingsBySellerPublic(sellerId = sellerId, status = null, limit = 50).fold(
                    onSuccess = { _sellingListings.value = it },
                    onFailure = { _sellingListings.value = emptyList() },
                )
                listingRepository.getListingsBySellerPublic(sellerId = sellerId, status = "sold", limit = 50).fold(
                    onSuccess = { _soldListings.value = it },
                    onFailure = { _soldListings.value = emptyList() },
                )
            } else {
                listingRepository.getListingsBySeller(
                    sellerId = sellerId,
                    status = null,
                    limit = 50,
                ).fold(
                    onSuccess = { _sellingListings.value = it },
                    onFailure = { _sellingListings.value = emptyList() },
                )
                listingRepository.getListingsBySeller(
                    sellerId = sellerId,
                    status = "sold",
                    limit = 50,
                ).fold(
                    onSuccess = { _soldListings.value = it },
                    onFailure = { _soldListings.value = emptyList() },
                )
            }
        }
    }

    fun toggleLike(item: ListingFeedItem) {
        if (isGuestBrowse()) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                listingRepository.toggleLike(item.id)
            }
            result.fold(
                onSuccess = { liked ->
                    fun patch(cur: ListingFeedItem): ListingFeedItem {
                        val delta = when {
                            liked && !cur.isLiked -> 1
                            !liked && cur.isLiked -> -1
                            else -> 0
                        }
                        return cur.copy(
                            isLiked = liked,
                            likeCount = (cur.likeCount + delta).coerceAtLeast(0),
                        )
                    }
                    _sellingListings.update { list -> list.map { if (it.id == item.id) patch(it) else it } }
                    _soldListings.update { list -> list.map { if (it.id == item.id) patch(it) else it } }
                    _events.emit(
                        getApplication<Application>().getString(
                            if (liked) R.string.listing_like_added_snackbar else R.string.listing_like_removed_snackbar,
                        ),
                    )
                },
                onFailure = { e ->
                    _events.emit(
                        e.message?.takeIf { m -> m.isNotBlank() }
                            ?: getApplication<Application>().getString(R.string.feed_action_error),
                    )
                },
            )
        }
    }

    fun toggleSave(item: ListingFeedItem) {
        if (isGuestBrowse()) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                listingRepository.toggleSave(item.id, item.isSaved)
            }
            result.fold(
                onSuccess = { saved ->
                    fun patch(cur: ListingFeedItem): ListingFeedItem {
                        val delta = when {
                            saved && !cur.isSaved -> 1
                            !saved && cur.isSaved -> -1
                            else -> 0
                        }
                        return cur.copy(
                            isSaved = saved,
                            saveCount = (cur.saveCount + delta).coerceAtLeast(0),
                        )
                    }
                    _sellingListings.update { list -> list.map { if (it.id == item.id) patch(it) else it } }
                    _soldListings.update { list -> list.map { if (it.id == item.id) patch(it) else it } }
                    _events.emit(
                        getApplication<Application>().getString(
                            if (saved) R.string.listing_save_added_snackbar else R.string.listing_save_removed_snackbar,
                        ),
                    )
                },
                onFailure = { e ->
                    _events.emit(
                        e.message?.takeIf { m -> m.isNotBlank() }
                            ?: getApplication<Application>().getString(R.string.feed_action_error),
                    )
                },
            )
        }
    }
}
