package com.pc.fash_android_mobile.ui.main.tabs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.data.user.UserAccessStatus
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository =
        (application as FashApplication).userRepository
    private val listingRepository: ListingRepository =
        (application as FashApplication).listingRepository

    private val _profile = MutableStateFlow<ProfileInfo?>(null)
    val profile: StateFlow<ProfileInfo?> = _profile.asStateFlow()

    private val _sellingListings = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val sellingListings: StateFlow<List<ListingFeedItem>> = _sellingListings.asStateFlow()

    private val _soldListings = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val soldListings: StateFlow<List<ListingFeedItem>> = _soldListings.asStateFlow()

    private val _wishlistListings = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val wishlistListings: StateFlow<List<ListingFeedItem>> = _wishlistListings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow(false)
    val loadError: StateFlow<Boolean> = _loadError.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _meetingSchedulingReverifyRequired = MutableStateFlow(false)
    val meetingSchedulingReverifyRequired: StateFlow<Boolean> = _meetingSchedulingReverifyRequired.asStateFlow()

    private val _meetingSchedulingSuspendedUntil = MutableStateFlow<String?>(null)
    val meetingSchedulingSuspendedUntil: StateFlow<String?> = _meetingSchedulingSuspendedUntil.asStateFlow()

    private val _ackMeetingReverifyInFlight = MutableStateFlow(false)
    val ackMeetingReverifyInFlight: StateFlow<Boolean> = _ackMeetingReverifyInFlight.asStateFlow()

    private var loadProfileJob: Job? = null

    /** First load when profile is missing. Skips refetch when the Profile tab recomposes but data is already in memory. */
    fun ensureProfileLoaded() {
        if (_profile.value != null) return
        loadProfile()
    }

    fun loadProfile() {
        loadProfileJob?.cancel()
        loadProfileJob = viewModelScope.launch {
            // Only block the whole screen when we have no profile yet; refreshes stay silent (no flicker).
            val showBlockingUi = _profile.value == null
            if (showBlockingUi) _isLoading.value = true
            try {
                _loadError.value = false
                withContext(Dispatchers.IO) {
                    userRepository.getMeProfile().fold(
                        onSuccess = { _profile.value = it },
                        onFailure = { _loadError.value = true },
                    )
                    userRepository.getUserAccessStatus().getOrNull()?.let { applyMeetingTrustFromStatus(it) }
                }
                _profile.value?.userId?.let { sellerId ->
                    withContext(Dispatchers.IO) {
                        coroutineScope {
                            val selling = async {
                                listingRepository.getListingsBySeller(
                                    sellerId = sellerId,
                                    status = null,
                                    limit = 50,
                                ).getOrElse { emptyList() }
                            }
                            val sold = async {
                                listingRepository.getListingsBySeller(
                                    sellerId = sellerId,
                                    status = "sold",
                                    limit = 50,
                                ).getOrElse { emptyList() }
                            }
                            val wish = async {
                                listingRepository.getWishlistListings(limit = 50, offset = 0).getOrElse { emptyList() }
                            }
                            _sellingListings.value = selling.await()
                            _soldListings.value = sold.await()
                            _wishlistListings.value = wish.await()
                        }
                    }
                } ?: run {
                    _sellingListings.value = emptyList()
                    _soldListings.value = emptyList()
                    _wishlistListings.value = emptyList()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retryLoad() = loadProfile()

    private fun applyMeetingTrustFromStatus(status: UserAccessStatus) {
        _meetingSchedulingReverifyRequired.value = status.meetingSchedulingReverifyRequired
        _meetingSchedulingSuspendedUntil.value = status.meetingSchedulingSuspendedUntil
    }

    fun refreshMeetingTrustFlags() {
        viewModelScope.launch {
            val status = withContext(Dispatchers.IO) {
                userRepository.getUserAccessStatus().getOrNull()
            }
            status?.let { applyMeetingTrustFromStatus(it) }
        }
    }

    fun ackMeetingIdentityReverify() {
        if (_ackMeetingReverifyInFlight.value) return
        viewModelScope.launch {
            _ackMeetingReverifyInFlight.value = true
            val result = withContext(Dispatchers.IO) {
                userRepository.ackMeetingIdentityReverify()
            }
            _ackMeetingReverifyInFlight.value = false
            val app = getApplication<Application>()
            result.fold(
                onSuccess = {
                    _meetingSchedulingReverifyRequired.value = false
                    _meetingSchedulingSuspendedUntil.value = null
                    refreshMeetingTrustFlags()
                    _events.tryEmit(app.getString(R.string.meeting_identity_reverify_ack_ok))
                },
                onFailure = { e ->
                    _events.tryEmit(
                        e.message?.takeIf { m -> m.isNotBlank() }
                            ?: app.getString(R.string.meeting_identity_reverify_ack_error),
                    )
                },
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
                    _wishlistListings.update { list -> list.map { if (it.id == item.id) patch(it) else it } }
                    _events.tryEmit(
                        getApplication<Application>().getString(
                            if (liked) R.string.listing_like_added_snackbar else R.string.listing_like_removed_snackbar,
                        ),
                    )
                },
                onFailure = { e ->
                    _events.tryEmit(
                        e.message?.takeIf { m -> m.isNotBlank() }
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
                    fun basePatch(cur: ListingFeedItem): ListingFeedItem {
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
                    _sellingListings.update { list -> list.map { if (it.id == item.id) basePatch(it) else it } }
                    _soldListings.update { list -> list.map { if (it.id == item.id) basePatch(it) else it } }
                    if (!saved && item.isSaved) {
                        _wishlistListings.update { list -> list.filter { it.id != item.id } }
                    } else {
                        _wishlistListings.update { list ->
                            val idx = list.indexOfFirst { it.id == item.id }
                            when {
                                idx >= 0 -> list.mapIndexed { i, e -> if (i == idx) basePatch(e) else e }
                                saved && !item.isSaved -> list + basePatch(item)
                                else -> list
                            }
                        }
                    }
                    _events.tryEmit(
                        getApplication<Application>().getString(
                            if (saved) R.string.listing_save_added_snackbar else R.string.listing_save_removed_snackbar,
                        ),
                    )
                },
                onFailure = { e ->
                    _events.tryEmit(
                        e.message?.takeIf { m -> m.isNotBlank() }
                            ?: getApplication<Application>().getString(R.string.feed_action_error),
                    )
                },
            )
        }
    }
}
