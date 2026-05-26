package com.pc.fash_android_mobile.ui.main.tabs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.recommendation.ProfileUxPersonalization
import com.pc.fash_android_mobile.data.recommendation.UxPersonalizationLocalStore
import com.pc.fash_android_mobile.data.recommendation.UxTabTracker
import com.pc.fash_android_mobile.data.recommendation.orderedProfileTabIndices
import com.pc.fash_android_mobile.data.recommendation.profileTabIndexFromKey
import com.pc.fash_android_mobile.data.recommendation.profileTabKeyFromIndex
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.data.user.UserAccessStatus
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
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

private const val ProfileStaleThresholdMs = 60_000L

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository =
        (application as FashApplication).userRepository
    private val listingRepository: ListingRepository =
        (application as FashApplication).listingRepository
    private val fashApp: FashApplication = application as FashApplication

    private val uxTabTracker = UxTabTracker(
        repository = fashApp.recommendationRepository,
        userIdProvider = { fashApp.authManager.sessionStore.read()?.userId },
        guestBrowse = { false },
        scope = viewModelScope,
    )

    private val _profileUxPersonalization = MutableStateFlow(ProfileUxPersonalization())
    val profileUxPersonalization: StateFlow<ProfileUxPersonalization> = _profileUxPersonalization.asStateFlow()

    private val _orderedProfileTabIndices = MutableStateFlow(orderedProfileTabIndices(emptyList()))
    val profileTabOrder: StateFlow<List<Int>> = _orderedProfileTabIndices.asStateFlow()

    private var profileUxDefaultApplied = false

    private val _profile = MutableStateFlow<ProfileInfo?>(null)
    val profile: StateFlow<ProfileInfo?> = _profile.asStateFlow()

    private val _sellingListings = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val sellingListings: StateFlow<List<ListingFeedItem>> = _sellingListings.asStateFlow()

    private val _inReviewListings = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val inReviewListings: StateFlow<List<ListingFeedItem>> = _inReviewListings.asStateFlow()

    private val _rejectedListings = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val rejectedListings: StateFlow<List<ListingFeedItem>> = _rejectedListings.asStateFlow()

    private val _soldListings = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val soldListings: StateFlow<List<ListingFeedItem>> = _soldListings.asStateFlow()

    private val _wishlistListings = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val wishlistListings: StateFlow<List<ListingFeedItem>> = _wishlistListings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

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

    /**
     * One-shot open-profile-tab request (e.g. Home journey “Đã lưu” → Saved tab with grid visible).
     */
    data class ProfileTabOpenRequest(
        val tabIndex: Int,
        val scrollToGrid: Boolean = true,
    )

    private val _profileTabOpenRequest = MutableStateFlow<ProfileTabOpenRequest?>(null)
    private val _profileTabOpenGeneration = MutableStateFlow(0L)
    val profileTabOpenGeneration: StateFlow<Long> = _profileTabOpenGeneration.asStateFlow()

    private val _scrollProfileToTop = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scrollProfileToTop: SharedFlow<Unit> = _scrollProfileToTop.asSharedFlow()

    fun requestOpenProfileTab(tabIndex: Int, scrollToGrid: Boolean = true) {
        _profileTabOpenRequest.value = ProfileTabOpenRequest(
            tabIndex = tabIndex.coerceIn(0, ProfileListingTab.LAST),
            scrollToGrid = scrollToGrid,
        )
        _profileTabOpenGeneration.update { it + 1L }
        refresh(force = true)
    }

    fun onProfileTabSelected(tabIndex: Int) {
        uxTabTracker.onTabOpened("profile", profileTabKeyFromIndex(tabIndex))
    }

    private suspend fun loadProfileUxPersonalization() {
        val ctx = getApplication<Application>().applicationContext
        val uid = fashApp.authManager.sessionStore.read()?.userId
        UxPersonalizationLocalStore.readProfileDefaultTab(ctx, uid)?.let { key ->
            profileTabIndexFromKey(key)?.let { idx ->
                _profileUxPersonalization.value = _profileUxPersonalization.value.copy(defaultTabKey = key)
                if (!profileUxDefaultApplied) pendingDefaultProfileTab = idx
            }
        }
        fashApp.recommendationRepository.uxPersonalization(
            clientHour = UxPersonalizationLocalStore.currentClientHour(),
        ).onSuccess { bundle ->
            _profileUxPersonalization.value = bundle.profile
            _orderedProfileTabIndices.value = orderedProfileTabIndices(bundle.profile.tabOrderKeys)
            UxPersonalizationLocalStore.writeProfileDefaultTab(ctx, uid, bundle.profile.defaultTabKey)
            profileTabIndexFromKey(bundle.profile.defaultTabKey)?.let { idx ->
                if (!profileUxDefaultApplied) pendingDefaultProfileTab = idx
            }
        }
    }

    private var pendingDefaultProfileTab: Int? = null

    fun consumePendingDefaultProfileTab(): Int? {
        if (profileUxDefaultApplied) return null
        if (_profileTabOpenGeneration.value != 0L) return null
        val tab = pendingDefaultProfileTab ?: profileTabIndexFromKey(_profileUxPersonalization.value.defaultTabKey)
        pendingDefaultProfileTab = null
        if (tab == null) return null
        profileUxDefaultApplied = true
        return tab
    }

    /** Home journey row → Profile Saved tab, scrolled to pinned grid. */
    fun requestWishlistTabFromHome() = requestOpenProfileTab(ProfileListingTab.WISHLIST, scrollToGrid = true)

    /** Home journey “Đang duyệt” → Profile in-review tab. */
    fun requestInReviewTabFromHome() = requestOpenProfileTab(ProfileListingTab.IN_REVIEW, scrollToGrid = true)

    fun consumeProfileTabOpenRequest(): ProfileTabOpenRequest? {
        if (_profileTabOpenGeneration.value == 0L) return null
        val req = _profileTabOpenRequest.value
        _profileTabOpenRequest.value = null
        _profileTabOpenGeneration.value = 0L
        return req
    }

    /** Bottom nav re-tap on Profile — scroll list to top (pairs with [refresh]). */
    fun requestScrollProfileToTop() {
        viewModelScope.launch { _scrollProfileToTop.emit(Unit) }
    }

    private var loadProfileJob: Job? = null
    private var lastSuccessfulRefreshAtMs = 0L

    /** Session user id we last reconciled [profile] against; used to detect account switch without a full process restart. */
    private var lastLoadedProfileForUserId: String? = null

    private fun clearProfileCachesOnly() {
        loadProfileJob?.cancel()
        _profile.value = null
        _sellingListings.value = emptyList()
        _inReviewListings.value = emptyList()
        _rejectedListings.value = emptyList()
        _soldListings.value = emptyList()
        _wishlistListings.value = emptyList()
        _loadError.value = false
        _meetingSchedulingReverifyRequired.value = false
        _meetingSchedulingSuspendedUntil.value = null
    }

    /**
     * Clears cached profile and listings when the session ends.
     * Call from the same [LaunchedEffect] that observes [AppAuthManager.isAuthenticated] going false.
     */
    fun clearCachedProfile() {
        lastLoadedProfileForUserId = null
        profileUxDefaultApplied = false
        _profileUxPersonalization.value = ProfileUxPersonalization()
        _orderedProfileTabIndices.value = orderedProfileTabIndices(emptyList())
        uxTabTracker.closeActiveTab()
        uxTabTracker.flush()
        _profileTabOpenRequest.value = null
        _profileTabOpenGeneration.value = 0L
        clearProfileCachesOnly()
    }

    /**
     * After cold-start validation or a fresh login: if the in-memory profile belongs to another user,
     * clear it and refetch. Avoids showing the previous account on the Profile tab when [ProfileViewModel]
     * is scoped to [MainActivity] and survives logout/login.
     */
    fun onAuthenticatedSessionReady() {
        viewModelScope.launch(Dispatchers.IO) {
            val sid = (getApplication<FashApplication>().authManager.sessionStore.read()?.userId ?: "")
                .trim()
                .lowercase()
            if (sid.isEmpty()) return@launch
            withContext(Dispatchers.Main.immediate) {
                val pid = _profile.value?.userId?.trim()?.lowercase()
                val staleProfile = !pid.isNullOrBlank() && pid != sid
                val switchedAccount = lastLoadedProfileForUserId != null && lastLoadedProfileForUserId != sid
                if (staleProfile || switchedAccount) {
                    lastLoadedProfileForUserId = null
                    clearProfileCachesOnly()
                }
                lastLoadedProfileForUserId = sid
                if (_profile.value == null) {
                    loadProfile()
                }
            }
        }
    }

    /**
     * First load when profile is missing; also refetches if cached profile user id does not match the session
     * (e.g. account switch edge cases).
     */
    fun ensureProfileLoaded() {
        viewModelScope.launch(Dispatchers.IO) {
            val sid = (getApplication<FashApplication>().authManager.sessionStore.read()?.userId ?: "")
                .trim()
                .lowercase()
            withContext(Dispatchers.Main.immediate) {
                val pid = _profile.value?.userId?.trim()?.lowercase()
                if (sid.isNotEmpty() && !pid.isNullOrBlank() && sid != pid) {
                    lastLoadedProfileForUserId = null
                    clearProfileCachesOnly()
                    lastLoadedProfileForUserId = sid
                    loadProfile()
                    return@withContext
                }
                if (_profile.value == null && sid.isNotEmpty()) {
                    loadProfile()
                }
            }
        }
    }

    fun loadProfile() {
        loadProfileJob?.cancel()
        loadProfileJob = viewModelScope.launch {
            // Only block the whole screen when we have no profile yet; refreshes stay silent (no flicker).
            val showBlockingUi = _profile.value == null
            if (showBlockingUi) _isLoading.value = true
            try {
                _loadError.value = false
                fetchProfileAndListings()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Pull-to-refresh — same payload as [loadProfile], with Material indicator (no full-screen blocking). */
    fun refreshIfStale() {
        val now = System.currentTimeMillis()
        if (now - lastSuccessfulRefreshAtMs < ProfileStaleThresholdMs) return
        refresh(force = false)
    }

    fun refresh(force: Boolean = true) {
        if (!force) {
            val now = System.currentTimeMillis()
            if (now - lastSuccessfulRefreshAtMs < ProfileStaleThresholdMs) return
        }
        loadProfileJob?.cancel()
        _isRefreshing.value = true
        loadProfileJob = viewModelScope.launch {
            try {
                _loadError.value = false
                fetchProfileAndListings()
                lastSuccessfulRefreshAtMs = System.currentTimeMillis()
            } finally {
                _isRefreshing.value = false
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchProfileAndListings() {
        withContext(Dispatchers.IO) {
            userRepository.getMeProfile().fold(
                onSuccess = { _profile.value = it },
                onFailure = {
                    _loadError.value = true
                    val gate = userRepository.getUserAccessStatus().getOrNull()
                    if (gate == null || !gate.canAccessHome) {
                        (getApplication<FashApplication>()).requestSetupGateRecheckFromIncompleteProfile()
                    }
                },
            )
            userRepository.getUserAccessStatus().getOrNull()?.let { applyMeetingTrustFromStatus(it) }
            loadProfileUxPersonalization()
        }
        _profile.value?.userId?.let {
            withContext(Dispatchers.IO) {
                coroutineScope {
                    val mine = async {
                        listingRepository.getMyListings(limit = 50, offset = 0).getOrElse { emptyList() }
                    }
                    val wish = async {
                        listingRepository.getWishlistListings(limit = 50, offset = 0).getOrElse { emptyList() }
                    }
                    val allMine = mine.await()
                    _sellingListings.value = allMine.filter { it.isActiveListing() }
                    _inReviewListings.value = allMine.filter { it.isInReviewListing() }
                    _rejectedListings.value = allMine.filter { it.isRejectedListing() }
                    _soldListings.value = allMine.filter { it.isSoldListingStatus() }
                    _wishlistListings.value = wish.await()
                }
            }
        } ?: run {
            _sellingListings.value = emptyList()
            _inReviewListings.value = emptyList()
            _rejectedListings.value = emptyList()
            _soldListings.value = emptyList()
            _wishlistListings.value = emptyList()
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
                    _inReviewListings.update { list -> list.map { if (it.id == item.id) patch(it) else it } }
                    _rejectedListings.update { list -> list.map { if (it.id == item.id) patch(it) else it } }
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
                    _inReviewListings.update { list -> list.map { if (it.id == item.id) basePatch(it) else it } }
                    _rejectedListings.update { list -> list.map { if (it.id == item.id) basePatch(it) else it } }
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

    override fun onCleared() {
        uxTabTracker.closeActiveTab()
        uxTabTracker.flush()
        super.onCleared()
    }
}
