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
import com.pc.fash_android_mobile.data.listing.ListingEngagementCoordinator
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.order.OrderRepository
import com.pc.fash_android_mobile.data.recommendation.FeedEventReporter
import com.pc.fash_android_mobile.data.recommendation.HomeExploreShortcut
import com.pc.fash_android_mobile.data.recommendation.HomeUxPersonalization
import com.pc.fash_android_mobile.data.recommendation.UxPersonalizationLocalStore
import com.pc.fash_android_mobile.data.recommendation.UxTabTracker
import com.pc.fash_android_mobile.data.recommendation.homeFeedTabFromKey
import com.pc.fash_android_mobile.data.recommendation.orderedHomeFeedTabs
import com.pc.fash_android_mobile.data.recommendation.toUxTabKey
import com.pc.fash_android_mobile.data.search.SearchRepository
import com.pc.fash_android_mobile.data.realtime.RealtimeEvent
import com.pc.fash_android_mobile.data.realtime.RealtimeManager
import com.pc.fash_android_mobile.data.user.UserRepository
import com.pc.fash_android_mobile.ui.explore.ExploreListingPreviewState
import com.pc.fash_android_mobile.ui.feed.FeedListingImagePrefetch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Buyer/seller dashboard counts for the home journey row (orders in delivery, wishlist, listings pending review). */
data class BuyerHomeStats(
    val activeDeliveryOrders: Int = 0,
    val savedListingsCount: Int = 0,
    val listingsInReviewCount: Int = 0,
) {
    /** Show journey row only when at least one stat is non-zero. */
    fun hasJourneyActivity(): Boolean =
        activeDeliveryOrders > 0 || savedListingsCount > 0 || listingsInReviewCount > 0
}

private val BuyerDeliveringStatuses = setOf(
    "payment_held",
    "in_transit",
    "delivering",
    "shipped",
    "shipping",
)

/** Skip automatic Home refresh when data was loaded within this window (ms). */
private const val HomeFeedStaleThresholdMs = 60_000L

/** Aggregated Home feed UI state — one collect reduces broad recomposition. */
data class HomeFeedUiState(
    val items: List<ListingFeedItem> = emptyList(),
    val discovery: HomeDiscoveryBundle = HomeDiscoveryBundle(),
    val selectedFeedTab: HomeFeedTab = HomeFeedTab.HuntToday,
    val followingIds: Set<String> = emptySet(),
    val buyerStats: BuyerHomeStats = BuyerHomeStats(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val tabsLoading: Set<HomeFeedTab> = emptySet(),
    val tabsLoadError: Set<HomeFeedTab> = emptySet(),
    val hasMoreItems: Boolean = true,
    val showSizingBanner: Boolean = false,
    val listingPreview: ExploreListingPreviewState? = null,
    val orderedFeedTabs: List<HomeFeedTab> = HomeFeedTab.signedInTabs(),
    val exploreShortcut: HomeExploreShortcut? = null,
)

/** Size of one follow-feed page (`GET /api/v1/listings/home`). */
internal const val HomeFollowFeedPageSize = 20

private const val HomeHuntTodayLimit = 12

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

    private val uxTabTracker = UxTabTracker(
        repository = fashApp.recommendationRepository,
        userIdProvider = { fashApp.authManager.sessionStore.read()?.userId },
        guestBrowse = { isGuestBrowse() },
        scope = viewModelScope,
    )

    private val _homeUxPersonalization = MutableStateFlow(HomeUxPersonalization())
    val homeUxPersonalization: StateFlow<HomeUxPersonalization> = _homeUxPersonalization.asStateFlow()

    private var homeUxApplied = false

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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** Per-tab feed loading (lazy fetch when tab is opened / prefetched). */
    private val _tabsLoading = MutableStateFlow<Set<HomeFeedTab>>(emptySet())
    val tabsLoading: StateFlow<Set<HomeFeedTab>> = _tabsLoading.asStateFlow()

    /** Per-tab feed load failure — drives retry on the active tab only. */
    private val _tabsLoadError = MutableStateFlow<Set<HomeFeedTab>>(emptySet())
    val tabsLoadError: StateFlow<Set<HomeFeedTab>> = _tabsLoadError.asStateFlow()

    private val loadedTabs = mutableSetOf<HomeFeedTab>()
    private var recommendationSectionsFetched = false
    private val tabLoadJobs = mutableMapOf<HomeFeedTab, Job>()

    /** Follow-feed pagination — guards duplicate in-flight requests. */
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    /** Follow-feed pagination — false when last fetch returned < HomeFollowFeedPageSize. */
    private val _hasMoreItems = MutableStateFlow(true)
    val hasMoreItems: StateFlow<Boolean> = _hasMoreItems.asStateFlow()

    private val _scrollHomeToTop = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scrollHomeToTop: SharedFlow<Unit> = _scrollHomeToTop.asSharedFlow()

    private val _selectedFeedTab = MutableStateFlow(HomeFeedTab.HuntToday)
    val selectedFeedTab: StateFlow<HomeFeedTab> = _selectedFeedTab.asStateFlow()

    fun setSelectedFeedTab(tab: HomeFeedTab) {
        if (_selectedFeedTab.value == tab) return
        uxTabTracker.onTabOpened("home", tab.toUxTabKey())
        _selectedFeedTab.value = tab
        ensureTabLoaded(tab)
        prefetchTabImages(tab)
        prefetchFromPersonalization(around = tab)
    }

    private fun prefetchFeedImages(items: List<ListingFeedItem>) {
        if (items.isEmpty()) return
        val ctx = getApplication<Application>()
        val columnWidthDp = (ctx.resources.configuration.screenWidthDp - 24) / 2f
        FeedListingImagePrefetch.prefetch(ctx, items, columnWidthDp)
    }

    private fun prefetchTabImages(tab: HomeFeedTab) {
        val bundle = _discoveryBundle.value
        val items = when (tab) {
            HomeFeedTab.HuntToday -> bundle.huntToday
            HomeFeedTab.ForYou -> bundle.forYou
            HomeFeedTab.StylePicks -> bundle.stylePicks
            HomeFeedTab.SimilarSaved -> bundle.similarToSaved
            HomeFeedTab.SeasonalNearYou -> bundle.seasonalNearYou
            HomeFeedTab.Following -> _items.value
        }
        prefetchFeedImages(items)
    }

    /** Coerce selection when guest mode hides personalized tabs (e.g. after sign-out). */
    fun normalizeSelectedFeedTab(isGuestBrowse: Boolean) {
        if (isGuestBrowse) {
            resetToHuntTodayTab(forceReload = false)
            return
        }
        val allowed = HomeFeedTab.tabsFor(false)
        if (_selectedFeedTab.value !in allowed) {
            resetToHuntTodayTab(forceReload = true)
        }
    }

    /**
     * Guest browse shell: public Hunt Today feed only — drop signed-in tab/personalization state.
     * Call when entering [FashApplication.isGuestBrowseActive] (cold start guest or after logout → continue browsing).
     */
    fun onGuestBrowseEntered() {
        uxTabTracker.closeActiveTab()
        feedEventReporter.flush()
        feedEventReporter.clearPending()
        homeUxApplied = false
        _homeUxPersonalization.value = HomeUxPersonalization()
        invalidateAllTabFeeds()
        _buyerStats.value = BuyerHomeStats()
        _showSizingBanner.value = false
        resetToHuntTodayTab(forceReload = true)
        loadFeed()
    }

    private fun resetToHuntTodayTab(forceReload: Boolean) {
        val switched = _selectedFeedTab.value != HomeFeedTab.HuntToday
        if (switched) {
            uxTabTracker.closeActiveTab()
            _selectedFeedTab.value = HomeFeedTab.HuntToday
        }
        if (forceReload || switched) {
            ensureTabLoaded(HomeFeedTab.HuntToday, force = true)
        }
    }

    private var lastSuccessfulRefreshAtMs = 0L

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

    private val _listingPreview = MutableStateFlow<ExploreListingPreviewState?>(null)
    val listingPreview: StateFlow<ExploreListingPreviewState?> = _listingPreview.asStateFlow()
    private var listingPreviewDetailJob: Job? = null

    val feedUiState: StateFlow<HomeFeedUiState> = combine(
        _items,
        _discoveryBundle,
        _selectedFeedTab,
        _followingIds,
        _buyerStats,
        _isLoading,
        _isRefreshing,
        _isLoadingMore,
        _tabsLoading,
        _tabsLoadError,
        _hasMoreItems,
        _showSizingBanner,
        _listingPreview,
        _homeUxPersonalization,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val ux = values[13] as HomeUxPersonalization
        HomeFeedUiState(
            items = values[0] as List<ListingFeedItem>,
            discovery = values[1] as HomeDiscoveryBundle,
            selectedFeedTab = values[2] as HomeFeedTab,
            followingIds = values[3] as Set<String>,
            buyerStats = values[4] as BuyerHomeStats,
            isLoading = values[5] as Boolean,
            isRefreshing = values[6] as Boolean,
            isLoadingMore = values[7] as Boolean,
            tabsLoading = values[8] as Set<HomeFeedTab>,
            tabsLoadError = values[9] as Set<HomeFeedTab>,
            hasMoreItems = values[10] as Boolean,
            showSizingBanner = values[11] as Boolean,
            listingPreview = values[12] as ExploreListingPreviewState?,
            orderedFeedTabs = orderedHomeFeedTabs(isGuestBrowse(), ux.tabOrder),
            exploreShortcut = ux.exploreShortcut,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeFeedUiState(),
    )

    init {
        viewModelScope.launch {
            AppLocale.localeRevisionFlow.collect {
                invalidateAllTabFeeds()
                withContext(Dispatchers.IO) { reloadHomeShell() }
                ensureTabLoaded(_selectedFeedTab.value, force = true)
            }
        }
        loadFeed()
        if (!isGuestBrowse()) {
            viewModelScope.launch(Dispatchers.IO) {
                coroutineScope {
                    launch { loadUxPersonalization() }
                    launch { loadRecommendationSections(force = false) }
                }
            }
        }
        viewModelScope.launch {
            if (isGuestBrowse()) return@launch
            realtimeManager.events.collect { event ->
                if (event is RealtimeEvent.FeedRefresh) {
                    withContext(Dispatchers.IO) { loadBuyerHomeStats() }
                    if (HomeFeedTab.Following in loadedTabs) {
                        ensureTabLoaded(HomeFeedTab.Following, force = true)
                    }
                }
            }
        }
    }

    private suspend fun reloadHomeShell() {
        homeDiscoveryRepository.loadShell().fold(
            onSuccess = { shell ->
                _discoveryBundle.update { cur ->
                    cur.copy(
                        editorialPosts = shell.editorialPosts,
                        recommendedSellers = shell.recommendedSellers,
                        trendingStyleTagChips = shell.trendingStyleTagChips,
                        trendingStyleTags = shell.trendingStyleTags,
                    )
                }
            },
            onFailure = { /* shell sections degrade to empty */ },
        )
    }

    private fun setTabLoading(tab: HomeFeedTab, loading: Boolean) {
        _tabsLoading.update { cur ->
            if (loading) cur + tab else cur - tab
        }
    }

    private fun setTabError(tab: HomeFeedTab, errored: Boolean) {
        _tabsLoadError.update { cur ->
            if (errored) cur + tab else cur - tab
        }
    }

    private fun invalidateAllTabFeeds() {
        loadedTabs.clear()
        recommendationSectionsFetched = false
        tabLoadJobs.values.forEach { it.cancel() }
        tabLoadJobs.clear()
        _tabsLoading.value = emptySet()
        _tabsLoadError.value = emptySet()
        _items.value = emptyList()
        _hasMoreItems.value = true
        _discoveryBundle.update {
            it.copy(
                huntToday = emptyList(),
                forYou = emptyList(),
                stylePicks = emptyList(),
                similarToSaved = emptyList(),
                seasonalNearYou = emptyList(),
                shoppingContext = null,
                recentlyViewed = emptyList(),
            )
        }
    }

    private fun ensureTabLoaded(tab: HomeFeedTab, force: Boolean = false) {
        if (isGuestBrowse() && tab.requiresAuth) return
        if (!force && tab in loadedTabs) return
        if (tab in _tabsLoading.value) return
        if (!force && tab == HomeFeedTab.HuntToday && recommendationSectionsFetched) {
            val cached = _discoveryBundle.value.huntToday
            if (cached.isNotEmpty()) {
                loadedTabs.add(tab)
                return
            }
        }
        if (!force && tab in HomeFeedTab.recommendationSectionTabs() && recommendationSectionsFetched) {
            loadedTabs.add(tab)
            return
        }

        tabLoadJobs[tab]?.cancel()
        tabLoadJobs[tab] = viewModelScope.launch {
            setTabLoading(tab, true)
            setTabError(tab, false)
            val ok = withContext(Dispatchers.IO) {
                when (tab) {
                    HomeFeedTab.HuntToday -> loadHuntTodayTab(force)
                    HomeFeedTab.Following -> loadFollowingTab(force)
                    HomeFeedTab.ForYou, HomeFeedTab.StylePicks, HomeFeedTab.SimilarSaved, HomeFeedTab.SeasonalNearYou ->
                        loadRecommendationSections(force)
                }
            }
            if (ok) {
                loadedTabs.add(tab)
                if (tab in HomeFeedTab.recommendationSectionTabs()) {
                    HomeFeedTab.recommendationSectionTabs().forEach { loadedTabs.add(it) }
                }
                prefetchTabImages(tab)
            } else {
                setTabError(tab, true)
            }
            setTabLoading(tab, false)
        }
    }

    private fun prefetchAdjacentTabs(tab: HomeFeedTab) {
        prefetchFromPersonalization(around = tab)
    }

    private fun prefetchFromPersonalization(around: HomeFeedTab) {
        val ux = _homeUxPersonalization.value
        val tabs = orderedHomeFeedTabs(isGuestBrowse(), ux.tabOrder)
        val prefetchKeys = ux.prefetchTabs.mapNotNull { homeFeedTabFromKey(it) }.filter { it in tabs }
        val targets = if (prefetchKeys.isNotEmpty()) {
            prefetchKeys.filter { it != around }.take(2)
        } else {
            val idx = tabs.indexOf(around)
            if (idx < 0) emptyList() else listOfNotNull(tabs.getOrNull(idx - 1), tabs.getOrNull(idx + 1))
        }
        targets.forEach { ensureTabLoaded(it) }
    }

    private suspend fun loadUxPersonalization() {
        if (isGuestBrowse()) return
        val ctx = getApplication<Application>().applicationContext
        val uid = fashApp.authManager.sessionStore.read()?.userId
        val localDefault = UxPersonalizationLocalStore.readHomeDefaultTab(ctx, uid)
        localDefault?.let { key ->
            homeFeedTabFromKey(key)?.let { tab ->
                if (!homeUxApplied) applyPreferredHomeTab(tab)
            }
        }
        fashApp.recommendationRepository.uxPersonalization(
            clientHour = UxPersonalizationLocalStore.currentClientHour(),
        ).onSuccess { bundle ->
            _homeUxPersonalization.value = bundle.home
            UxPersonalizationLocalStore.writeHomeDefaultTab(ctx, uid, bundle.home.defaultTabKey)
            homeFeedTabFromKey(bundle.home.defaultTabKey)?.let { applyPreferredHomeTab(it) }
            bundle.home.prefetchTabs.mapNotNull { homeFeedTabFromKey(it) }.forEach { ensureTabLoaded(it) }
        }
    }

    private fun applyPreferredHomeTab(tab: HomeFeedTab) {
        if (isGuestBrowse() && tab.requiresAuth) return
        if (tab !in HomeFeedTab.tabsFor(isGuestBrowse())) return
        if (homeUxApplied && _selectedFeedTab.value == tab) return
        homeUxApplied = true
        _selectedFeedTab.value = tab
        uxTabTracker.onTabOpened("home", tab.toUxTabKey())
        ensureTabLoaded(tab, force = !loadedTabs.contains(tab))
    }

    private fun sectionLimitFor(tab: HomeFeedTab, fallback: Int): Int {
        val key = tab.toUxTabKey()
        return _homeUxPersonalization.value.sectionLimits[key] ?: fallback
    }

    private fun huntTodaySizingMode(): String? =
        com.pc.fash_android_mobile.data.explore.ExploreSizingPreference
            .read(getApplication())
            .takeIf { it.equals("match_profile", ignoreCase = true) }

    private suspend fun loadHuntTodayTab(force: Boolean): Boolean {
        if (!force && HomeFeedTab.HuntToday in loadedTabs) return true
        if (!isGuestBrowse() && recommendationSectionsFetched) {
            val cached = _discoveryBundle.value.huntToday
            if (cached.isNotEmpty()) return true
        }
        return fashApp.recommendationRepository.exploreListings(
            publicBrowse = isGuestBrowse(),
            limit = sectionLimitFor(HomeFeedTab.HuntToday, HomeHuntTodayLimit),
            offset = 0,
            surface = HomeFeedTab.HuntToday.analyticsSurface,
            sizingMode = huntTodaySizingMode(),
        ).fold(
            onSuccess = { items ->
                _discoveryBundle.update { it.copy(huntToday = items) }
                syncSellerFollowingFromListings(items)
                true
            },
            onFailure = { false },
        )
    }

    private suspend fun loadFollowingTab(force: Boolean): Boolean {
        if (!force && HomeFeedTab.Following in loadedTabs && _items.value.isNotEmpty()) return true
        return fetchHomeFeedWithRetry().fold(
            onSuccess = { feed ->
                _items.value = feed
                syncSellerFollowingFromListings(feed)
                prefetchFeedImages(feed)
                true
            },
            onFailure = { false },
        )
    }

    private suspend fun loadRecommendationSections(force: Boolean): Boolean {
        if (!force && recommendationSectionsFetched) return true
        val styleLimit = sectionLimitFor(HomeFeedTab.StylePicks, 12)
        val similarLimit = sectionLimitFor(HomeFeedTab.SimilarSaved, 12)
        return fashApp.recommendationRepository.homeSections(
            publicBrowse = isGuestBrowse(),
            huntTodayLimit = sectionLimitFor(HomeFeedTab.HuntToday, 12),
            forYouLimit = sectionLimitFor(HomeFeedTab.ForYou, 16),
            sectionLimit = maxOf(styleLimit, similarLimit),
            sizingMode = huntTodaySizingMode(),
        ).fold(
            onSuccess = { sections ->
                _discoveryBundle.update { cur ->
                    cur.copy(
                        huntToday = sections.huntToday.ifEmpty { cur.huntToday },
                        forYou = sections.forYou,
                        stylePicks = sections.stylePicks,
                        similarToSaved = sections.similarToSaved,
                        seasonalNearYou = sections.seasonalNearYou,
                        shoppingContext = sections.shoppingContext ?: cur.shoppingContext,
                    )
                }
                syncSellerFollowingFromListings(
                    sections.huntToday + sections.forYou + sections.stylePicks +
                        sections.similarToSaved + sections.seasonalNearYou,
                )
                if (sections.huntToday.isNotEmpty()) {
                    loadedTabs.add(HomeFeedTab.HuntToday)
                }
                recommendationSectionsFetched = true
                prefetchTabImages(_selectedFeedTab.value)
                true
            },
            onFailure = { false },
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
        val inReview = listingRepository.getMyListings(status = "in_review", limit = 50, offset = 0)
            .getOrElse { emptyList() }
            .size
        _buyerStats.value = BuyerHomeStats(
            activeDeliveryOrders = delivering,
            savedListingsCount = saved,
            listingsInReviewCount = inReview,
        )
    }

    fun loadFeed() {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                coroutineScope {
                    val stats = async { loadBuyerHomeStats() }
                    val shell = async { reloadHomeShell() }
                    val sizing = async { refreshSizingBannerState() }
                    val sections = async {
                        if (!isGuestBrowse()) loadRecommendationSections(force = false)
                    }
                    stats.await()
                    shell.await()
                    sizing.await()
                    sections.await()
                }
            }
            _isLoading.value = false
            ensureTabLoaded(_selectedFeedTab.value, force = true)
            prefetchAdjacentTabs(_selectedFeedTab.value)
            lastSuccessfulRefreshAtMs = System.currentTimeMillis()
        }
    }

    /**
     * `GET /api/v1/listings/home` only — from followed sellers (core-service).
     * Empty list is valid when the user follows nobody; use Explore for discovery.
     * One automatic retry on failure to smooth transient network errors.
     */
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
                        prefetchFeedImages(page)
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
        retryTab(HomeFeedTab.Following)
    }

    fun retryDiscovery() {
        retryTab(_selectedFeedTab.value)
    }

    fun retryTab(tab: HomeFeedTab) {
        requestScrollHomeToTop()
        loadedTabs.remove(tab)
        if (tab in HomeFeedTab.recommendationSectionTabs()) {
            recommendationSectionsFetched = false
            HomeFeedTab.recommendationSectionTabs().forEach { loadedTabs.remove(it) }
        }
        ensureTabLoaded(tab, force = true)
    }

    /**
     * Clears user-specific feed state when the session ends so the shell never briefly shows
     * the previous account’s home after logout / account switch.
     */
    fun clearCachesForSignedOutUser() {
        uxTabTracker.closeActiveTab()
        uxTabTracker.flush()
        feedEventReporter.flush()
        feedEventReporter.clearPending()
        homeUxApplied = false
        _homeUxPersonalization.value = HomeUxPersonalization()
        _selectedFeedTab.value = HomeFeedTab.HuntToday
        invalidateAllTabFeeds()
        _likedIds.value = emptySet()
        _savedIds.value = emptySet()
        _followingIds.value = emptySet()
        _buyerStats.value = BuyerHomeStats()
        _discoveryBundle.value = HomeDiscoveryBundle()
        _showSizingBanner.value = false
        _isLoading.value = false
        _isRefreshing.value = false
        _isLoadingMore.value = false
        lastSuccessfulRefreshAtMs = 0L
    }

    /** Bottom nav re-tap on Home — scroll feed to top (pairs with [refresh]). */
    fun requestScrollHomeToTop() {
        viewModelScope.launch { _scrollHomeToTop.emit(Unit) }
    }

    /** Refreshes only when data is older than [HomeFeedStaleThresholdMs]. */
    fun refreshIfStale() {
        val now = System.currentTimeMillis()
        if (now - lastSuccessfulRefreshAtMs < HomeFeedStaleThresholdMs) return
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val selected = _selectedFeedTab.value
            invalidateAllTabFeeds()
            withContext(Dispatchers.IO) {
                coroutineScope {
                    val stats = async { loadBuyerHomeStats() }
                    val shell = async { reloadHomeShell() }
                    val sizing = async { refreshSizingBannerState() }
                    val ux = async { if (!isGuestBrowse()) loadUxPersonalization() }
                    val sections = async {
                        if (!isGuestBrowse()) loadRecommendationSections(force = false)
                    }
                    stats.await()
                    shell.await()
                    sizing.await()
                    ux.await()
                    sections.await()
                }
            }
            ensureTabLoaded(selected, force = true)
            prefetchFromPersonalization(selected)
            _isRefreshing.value = false
            lastSuccessfulRefreshAtMs = System.currentTimeMillis()
            requestScrollHomeToTop()
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
        if (!ListingEngagementCoordinator.beginLikeToggle(item.id)) return
        viewModelScope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    listingRepository.toggleLike(item.id)
                }
            } finally {
                ListingEngagementCoordinator.endLikeToggle(item.id)
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
        if (!ListingEngagementCoordinator.beginSaveToggle(item.id)) return
        viewModelScope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    listingRepository.toggleSave(item.id, item.isSaved)
                }
            } finally {
                ListingEngagementCoordinator.endSaveToggle(item.id)
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

    /** Opens the half-sheet quick look (same UX as Explore) instead of navigating straight to PDP. */
    fun openListingPreview(item: ListingFeedItem, surface: String, position: Int = 0) {
        feedEventReporter.previewOpen(item.id, surface = surface, position = position)
        recordView(item, position, surface)
        listingPreviewDetailJob?.cancel()
        _listingPreview.value = ExploreListingPreviewState(
            feedItem = item,
            gridPosition = position,
            surface = surface,
            detail = null,
            isDetailLoading = true,
        )
        listingPreviewDetailJob = viewModelScope.launch {
            val guest = isGuestBrowse()
            withContext(Dispatchers.IO) {
                listingRepository.getListingDetail(item.id, publicBrowse = guest)
            }.fold(
                onSuccess = { detail ->
                    _listingPreview.update { cur ->
                        if (cur?.feedItem?.id != item.id) return@update cur
                        cur.copy(detail = detail, isDetailLoading = false)
                    }
                },
                onFailure = {
                    _listingPreview.update { cur ->
                        if (cur?.feedItem?.id != item.id) return@update cur
                        cur.copy(isDetailLoading = false)
                    }
                },
            )
        }
    }

    fun closeListingPreview() {
        val cur = _listingPreview.value
        if (cur != null && !cur.outcomeRecorded) {
            val dwellMs = (System.currentTimeMillis() - cur.openedAtMs).toInt().coerceAtLeast(0)
            feedEventReporter.previewDismiss(
                cur.feedItem.id,
                surface = cur.surface,
                position = cur.gridPosition,
                dwellMs = dwellMs,
            )
        }
        listingPreviewDetailJob?.cancel()
        _listingPreview.value = null
    }

    fun openListingDetailFromPreview(): Pair<String, String?>? {
        val cur = _listingPreview.value ?: return null
        feedEventReporter.previewDetail(cur.feedItem.id, surface = cur.surface, position = cur.gridPosition)
        val id = cur.feedItem.id
        val sellerId = cur.feedItem.sellerId
        listingPreviewDetailJob?.cancel()
        _listingPreview.value = null
        return id to sellerId
    }

    fun openChatFromPreview(): Pair<String, String?>? {
        val cur = _listingPreview.value ?: return null
        feedEventReporter.chatInitiate(cur.feedItem.id, surface = cur.surface, position = cur.gridPosition)
        val id = cur.feedItem.id
        val sellerId = cur.feedItem.sellerId
        listingPreviewDetailJob?.cancel()
        _listingPreview.value = null
        return id to sellerId
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

    override fun onCleared() {
        uxTabTracker.closeActiveTab()
        uxTabTracker.flush()
        feedEventReporter.flush()
        super.onCleared()
    }

    fun flushUxTabTracker() {
        uxTabTracker.closeActiveTab()
        uxTabTracker.flush()
    }

    private fun updateListingInFeeds(listingId: String, transform: (ListingFeedItem) -> ListingFeedItem) {
        fun List<ListingFeedItem>.patch() = map { if (it.id == listingId) transform(it) else it }
        _items.update { it.patch() }
        _discoveryBundle.update { bundle ->
            bundle.copy(
                huntToday = bundle.huntToday.patch(),
                recentlyViewed = bundle.recentlyViewed.patch(),
                stylePicks = bundle.stylePicks.patch(),
                similarToSaved = bundle.similarToSaved.patch(),
                forYou = bundle.forYou.patch(),
            )
        }
        _listingPreview.update { preview ->
            preview?.takeIf { it.feedItem.id == listingId }?.copy(feedItem = transform(preview.feedItem))
        }
    }
}
