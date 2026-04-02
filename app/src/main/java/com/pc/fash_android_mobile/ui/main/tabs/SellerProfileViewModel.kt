package com.pc.fash_android_mobile.ui.main.tabs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var activeKey: String? = null

    fun loadForSeller(username: String) {
        val key = username.trim().removePrefix("@")
        if (key.isBlank()) return
        viewModelScope.launch {
            if (activeKey != key) {
                activeKey = key
                _profile.value = null
                _sellingListings.value = emptyList()
                _soldListings.value = emptyList()
                _isFollowing.value = false
            }
            _isLoading.value = true
            _loadError.value = false
            withContext(Dispatchers.IO) {
                userRepository.getProfile(key).fold(
                    onSuccess = { prof ->
                        _profile.value = prof
                        _isFollowing.value = prof.isFollowing ?: false
                    },
                    onFailure = { _loadError.value = true },
                )
            }
            _profile.value?.userId?.takeIf { it.isNotBlank() }?.let { loadListings(it) }
            _isLoading.value = false
        }
    }

    fun retryLoad(username: String) = loadForSeller(username)

    /** True when logged in and the opened profile is not the current user. */
    fun canFollowSeller(): Boolean {
        val my = sessionStore.read()?.userId?.trim().orEmpty()
        if (my.isBlank()) return false
        val seller = _profile.value?.userId?.trim().orEmpty()
        if (seller.isBlank()) return false
        return !my.equals(seller, ignoreCase = true)
    }

    fun toggleFollow() {
        val p = _profile.value ?: return
        if (!canFollowSeller()) return
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
