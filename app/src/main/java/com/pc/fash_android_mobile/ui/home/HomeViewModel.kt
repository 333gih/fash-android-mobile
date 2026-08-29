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
import com.pc.fash_android_mobile.data.search.FeaturedSellerItem
import com.pc.fash_android_mobile.data.search.SearchRepository
import com.pc.fash_android_mobile.data.search.shopReadyOnly
import com.pc.fash_android_mobile.data.realtime.RealtimeEvent
import com.pc.fash_android_mobile.data.realtime.RealtimeManager
import com.pc.fash_android_mobile.data.user.UserRepository
import com.pc.fash_android_mobile.ui.components.FeedEngagementFeedback
import com.pc.fash_android_mobile.ui.components.emitSnackbarMessage
import com.pc.fash_android_mobile.ui.explore.ExploreListingPreviewState
import com.pc.fash_android_mobile.ui.feed.FeedListingImagePrefetch
import com.pc.fash_android_mobile.ui.feed.FeedLoadMoreThrottle
import com.pc.fash_android_mobile.ui.feed.FeedLoadStallWatch
import kotlinx.coroutines.channels.BufferOverflow
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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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

/** Section-tab pagination page size — matches iOS `HomeFeedConstants.tabLoadMorePageSize`. */
private const val HomeTabLoadMorePageSize = 20

private data class HomeTabFeedState(
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
)

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
    val tabsLoadStalled: Set<HomeFeedTab> = emptySet(),
    val hasMoreItems: Boolean = true,
    val selectedTabHasMore: Boolean = false,
    val selectedTabLoadingMore: Boolean = false,
    val showBrandFooter: Boolean = false,
    val showSizingBanner: Boolean = false,
    val listingPreview: ExploreListingPreviewState? = null,
    val featuredSellers: List<FeaturedSellerItem> = emptyList(),
    val featuredSellersLoading: Boolean = false,
    val orderedFeedTabs: List<HomeFeedTab> = HomeFeedTab.signedInTabs(),
    val exploreShortcut: HomeExploreShortcut? = null,
)

/** Size of one follow-feed page (`GET /api/v1/listings/home`). */
internal const val HomeFollowFeedPageSize = 20

private const val HomeHuntTodayLimit = 12

/** Home “Shop nên ghé” rail — matches iOS [HomeViewModel.loadFeaturedSellers]. */
private const val HomeFeaturedSellersLimit = 12
private const val TAB_PREFETCH_DEFER_MS = 180L
/** Drop hung first-page loads so Home can show retry instead of an infinite skeleton. */
private const val TAB_LOAD_TIMEOUT_MS = 12_000L

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
    private val sectionLoadMoreJobs = mutableMapOf<HomeFeedTab, Job>()
    private val tabStallWatch = FeedLoadStallWatch()
    private val _tabsLoadStalled = MutableStateFlow<Set<HomeFeedTab>>(emptySet())
    private val _tabFeedState = MutableStateFlow<Map<HomeFeedTab, HomeTabFeedState>>(emptyMap())

    /** Follow-feed pagination — guards duplicate in-flight requests. */
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    /** Follow-feed pagination — false when last fetch returned < HomeFollowFeedPageSize. */
    private val _hasMoreItems = MutableStateFlow(true)
    val hasMoreItems: StateFlow<Boolean> = _hasMoreItems.asStateFlow()

    private val _scrollHomeToTop = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val scrollHomeToTop: SharedFlow<Unit> = _scrollHomeToTop.asSharedFlow()

    private val _scrollHomeFeedToTop = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scrollHomeFeedToTop: SharedFlow<Unit> = _scrollHomeFeedToTop.asSharedFlow()

    private val _selectedFeedTab = MutableStateFlow(HomeFeedTab.HuntToday)
    val selectedFeedTab: StateFlow<HomeFeedTab> = _selectedFeedTab.asStateFlow()

    fun setSelectedFeedTab(tab: HomeFeedTab) {
        if (_selectedFeedTab.value == tab) {
            requestScrollHomeToTop()
            return
        }
        uxTabTracker.onTabOpened("home", tab.toUxTabKey())
        _selectedFeedTab.value = tab
        if (tabLoadJobs[tab]?.isActive == true && itemsForTab(tab).isEmpty()) {
            beginTabLoad(tab)
        }
        if (isGuestBrowse()) {
            requestScrollHomeToTop()
        } else {
            requestScrollHomeFeedToTop()
        }
        ensureTabLoaded(tab)
        prefetchTabImages(tab)
        viewModelScope.launch {
            delay(TAB_PREFETCH_DEFER_MS)
            if (_selectedFeedTab.value == tab) {
                prefetchFromPersonalization(around = tab)
            }
        }
    }

    fun hasCachedItems(tab: HomeFeedTab): Boolean = itemsForTab(tab).isNotEmpty()

    fun isTabLoadStalled(tab: HomeFeedTab): Boolean = tab in _tabsLoadStalled.value

    fun loadMoreActiveTab() {
        val tab = _selectedFeedTab.value
        if (tab == HomeFeedTab.Following) {
            loadMoreFollowFeed()
        } else {
            loadMoreSectionTab(tab)
        }
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
            requestScrollHomeToTop()
            return
        }
        val allowed = HomeFeedTab.tabsFor(false)
        if (_selectedFeedTab.value !in allowed) {
            resetToHuntTodayTab(forceReload = true)
        }
    }

    /**
     * Launch gate — load the active Home tab before revealing the main shell (iOS parity).
     * Shell chrome and other tabs continue loading in the background.
     */
    suspend fun awaitLaunchReady(isGuestBrowse: Boolean) {
        normalizeSelectedFeedTab(isGuestBrowse)
        val tab = _selectedFeedTab.value
        withContext(Dispatchers.IO) {
            coroutineScope {
                val shell = async { reloadHomeShell() }
                val sellers = async { loadFeaturedSellers() }
                shell.await()
                sellers.await()
                awaitTabLoadedSync(tab, force = true)
            }
        }
        lastSuccessfulRefreshAtMs = System.currentTimeMillis()
        scheduleLaunchShellEnrichment()
        if (isGuestBrowse) {
            requestScrollHomeToTop()
        }
    }

    private suspend fun awaitTabLoadedSync(tab: HomeFeedTab, force: Boolean) {
        if (isGuestBrowse() && tab.requiresAuth) return
        tabLoadJobs[tab]?.cancel()
        beginTabLoad(tab)
        var ok = false
        try {
            ok = withTimeoutOrNull(TAB_LOAD_TIMEOUT_MS) {
                when (tab) {
                    HomeFeedTab.HuntToday -> loadHuntTodayTab(force)
                    HomeFeedTab.Following -> loadFollowingTab(force)
                    HomeFeedTab.ForYou,
                    HomeFeedTab.StylePicks,
                    HomeFeedTab.SimilarSaved,
                    HomeFeedTab.SeasonalNearYou,
                    -> loadRecommendationSections(force)
                }
            } == true
        } finally {
            if (currentCoroutineContext().isActive) {
                finishTabLoad(tab, ok)
            } else {
                clearTabLoadingWithoutMarkingLoaded(tab)
            }
        }
    }

    private fun scheduleLaunchShellEnrichment() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                coroutineScope {
                    if (!isGuestBrowse()) {
                        async { loadBuyerHomeStats() }
                        async { refreshSizingBannerState() }
                        async { loadUxPersonalization() }
                        async { loadRecommendationSections(force = false) }
                    }
                }
            }
            prefetchAdjacentTabs(_selectedFeedTab.value)
        }
    }

    /**
     * Guest browse shell: public Hunt Today feed only — drop signed-in tab/personalization state.
     * Call when entering [FashApplication.isGuestBrowseActive] (cold start guest or after logout → continue browsing).
     */
    fun onGuestBrowseEntered(forceReset: Boolean = true) {
        if (
            !forceReset &&
            HomeFeedTab.HuntToday in loadedTabs &&
            _discoveryBundle.value.huntToday.isNotEmpty()
        ) {
            return
        }
        if (_featuredSellers.value.isEmpty()) {
            _featuredSellersLoading.value = true
        }
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

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
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

    private val _featuredSellers = MutableStateFlow<List<FeaturedSellerItem>>(emptyList())
    private val _featuredSellersLoading = MutableStateFlow(false)

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
        _tabsLoadStalled,
        _tabFeedState,
        _hasMoreItems,
        _showSizingBanner,
        _listingPreview,
        _homeUxPersonalization,
        _featuredSellers,
        _featuredSellersLoading,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val ux = values[15] as HomeUxPersonalization
        val selectedTab = values[2] as HomeFeedTab
        val guest = isGuestBrowse()
        val tabState = (values[11] as Map<HomeFeedTab, HomeTabFeedState>)[selectedTab]
            ?: HomeTabFeedState()
        val selectedTabHasMore = when (selectedTab) {
            HomeFeedTab.Following -> values[12] as Boolean
            else -> tabState.hasMore
        }
        val selectedTabLoadingMore = when (selectedTab) {
            HomeFeedTab.Following -> values[7] as Boolean
            else -> tabState.isLoadingMore
        }
        HomeFeedUiState(
            items = values[0] as List<ListingFeedItem>,
            discovery = values[1] as HomeDiscoveryBundle,
            selectedFeedTab = selectedTab,
            followingIds = values[3] as Set<String>,
            buyerStats = values[4] as BuyerHomeStats,
            isLoading = values[5] as Boolean,
            isRefreshing = values[6] as Boolean,
            isLoadingMore = values[7] as Boolean,
            tabsLoading = values[8] as Set<HomeFeedTab>,
            tabsLoadError = values[9] as Set<HomeFeedTab>,
            tabsLoadStalled = values[10] as Set<HomeFeedTab>,
            hasMoreItems = values[12] as Boolean,
            selectedTabHasMore = selectedTabHasMore,
            selectedTabLoadingMore = selectedTabLoadingMore,
            showBrandFooter = computeShowBrandFooter(
                tab = selectedTab,
                isGuestBrowse = guest,
                items = itemsForTab(selectedTab, values[0] as List<ListingFeedItem>, values[1] as HomeDiscoveryBundle),
                isShellLoading = values[5] as Boolean,
                isRefreshing = values[6] as Boolean,
                tabsLoading = values[8] as Set<HomeFeedTab>,
                followingHasMore = values[12] as Boolean,
                followingLoadingMore = values[7] as Boolean,
                tabHasMore = tabState.hasMore,
                tabLoadingMore = tabState.isLoadingMore,
            ),
            showSizingBanner = values[13] as Boolean,
            listingPreview = values[14] as ExploreListingPreviewState?,
            orderedFeedTabs = orderedHomeFeedTabs(guest, ux.tabOrder),
            exploreShortcut = ux.exploreShortcut,
            featuredSellers = values[16] as List<FeaturedSellerItem>,
            featuredSellersLoading = values[17] as Boolean,
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
                    launch { refreshShoppingContext() }
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
                        trendingStyleTagChips = shell.trendingStyleTagChips,
                        trendingStyleTags = shell.trendingStyleTags,
                    )
                }
            },
            onFailure = { /* shell sections degrade to empty */ },
        )
    }

    /**
     * Curated seller rail — same path as iOS [loadFeaturedSellers]: direct search API,
     * stale-while-revalidate, one automatic retry on transient failures.
     */
    private suspend fun loadFeaturedSellers() {
        val showLoadingShell = _featuredSellers.value.isEmpty()
        if (showLoadingShell) {
            _featuredSellersLoading.value = true
        }
        try {
            suspend fun fetchOnce(): Result<List<FeaturedSellerItem>> {
                return if (isGuestBrowse()) {
                    searchRepository.browseFeaturedSellersPage(
                        limit = HomeFeaturedSellersLimit,
                        offset = 0,
                    ).map { it.items }
                } else {
                    searchRepository.getFeaturedSellers(
                        limit = HomeFeaturedSellersLimit,
                        offset = 0,
                    )
                }
            }
            var result = fetchOnce()
            if (result.isFailure) {
                delay(400)
                result = fetchOnce()
            }
            result.onSuccess { sellers ->
                _featuredSellers.value = sellers.shopReadyOnly()
            }
        } finally {
            if (showLoadingShell) {
                _featuredSellersLoading.value = false
            }
        }
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

    private fun itemsForTab(
        tab: HomeFeedTab,
        followingItems: List<ListingFeedItem> = _items.value,
        discovery: HomeDiscoveryBundle = _discoveryBundle.value,
    ): List<ListingFeedItem> = when (tab) {
        HomeFeedTab.HuntToday -> discovery.huntToday
        HomeFeedTab.ForYou -> discovery.forYou
        HomeFeedTab.Following -> followingItems
        HomeFeedTab.StylePicks -> discovery.stylePicks
        HomeFeedTab.SimilarSaved -> discovery.similarToSaved
        HomeFeedTab.SeasonalNearYou -> discovery.seasonalNearYou
    }

    private fun computeShowBrandFooter(
        tab: HomeFeedTab,
        isGuestBrowse: Boolean,
        items: List<ListingFeedItem>,
        isShellLoading: Boolean,
        isRefreshing: Boolean,
        tabsLoading: Set<HomeFeedTab>,
        followingHasMore: Boolean,
        followingLoadingMore: Boolean,
        tabHasMore: Boolean,
        tabLoadingMore: Boolean,
    ): Boolean {
        if (isGuestBrowse && tab.requiresAuth) return false
        if (items.isEmpty()) return false
        if (isShellLoading || isRefreshing || tab in tabsLoading) return false
        if (tab == HomeFeedTab.Following) {
            return !followingHasMore && !followingLoadingMore
        }
        return !tabHasMore && !tabLoadingMore
    }

    private fun setTabHasMore(tab: HomeFeedTab, hasMore: Boolean) {
        _tabFeedState.update { cur ->
            cur + (tab to (cur[tab] ?: HomeTabFeedState()).copy(hasMore = hasMore))
        }
    }

    private fun setTabLoadingMore(tab: HomeFeedTab, loading: Boolean) {
        _tabFeedState.update { cur ->
            cur + (tab to (cur[tab] ?: HomeTabFeedState()).copy(isLoadingMore = loading))
        }
    }

    private fun scheduleTabStallWatch(tab: HomeFeedTab) {
        tabStallWatch.schedule(
            scope = viewModelScope,
            key = tab.name,
            isStillPending = {
                _selectedFeedTab.value == tab &&
                    tab !in loadedTabs &&
                    itemsForTab(tab).isEmpty()
            },
            onStalled = {
                // Drop the skeleton so the retry CTA can show even if the job is still hung.
                setTabLoading(tab, false)
                _tabsLoadStalled.update { it + tab }
            },
        )
    }

    private fun clearTabLoadingWithoutMarkingLoaded(tab: HomeFeedTab) {
        finishTabStallWatch(tab)
        setTabLoading(tab, false)
    }

    private fun finishTabStallWatch(tab: HomeFeedTab) {
        tabStallWatch.cancel(tab.name)
        _tabsLoadStalled.update { it - tab }
    }

    private fun beginTabLoad(tab: HomeFeedTab) {
        finishTabStallWatch(tab)
        if (_selectedFeedTab.value == tab && itemsForTab(tab).isEmpty()) {
            setTabLoading(tab, true)
            scheduleTabStallWatch(tab)
        }
        setTabError(tab, false)
    }

    private fun finishTabLoad(tab: HomeFeedTab, ok: Boolean) {
        finishTabStallWatch(tab)
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

    private fun appendUniqueItems(page: List<ListingFeedItem>, tab: HomeFeedTab): Int {
        val existing = itemsForTab(tab).mapTo(HashSet()) { it.id }
        val fresh = page.filter { existing.add(it.id) }
        if (fresh.isEmpty()) return 0
        when (tab) {
            HomeFeedTab.HuntToday ->
                _discoveryBundle.update { it.copy(huntToday = it.huntToday + fresh) }
            HomeFeedTab.ForYou ->
                _discoveryBundle.update { it.copy(forYou = it.forYou + fresh) }
            HomeFeedTab.StylePicks ->
                _discoveryBundle.update { it.copy(stylePicks = it.stylePicks + fresh) }
            HomeFeedTab.SimilarSaved ->
                _discoveryBundle.update { it.copy(similarToSaved = it.similarToSaved + fresh) }
            HomeFeedTab.SeasonalNearYou ->
                _discoveryBundle.update { it.copy(seasonalNearYou = it.seasonalNearYou + fresh) }
            HomeFeedTab.Following -> Unit
        }
        syncSellerFollowingFromListings(fresh)
        prefetchFeedImages(fresh)
        return fresh.size
    }

    private fun loadMoreSectionTab(tab: HomeFeedTab) {
        if (isGuestBrowse() && tab.requiresAuth) return
        val state = _tabFeedState.value[tab] ?: HomeTabFeedState()
        if (!state.hasMore || state.isLoadingMore) return
        if (_isLoading.value || _isRefreshing.value || tab in _tabsLoading.value) return
        if (sectionLoadMoreJobs[tab]?.isActive == true) return
        val blockedUntil = sectionTabLoadMoreBlockedUntilMs[tab] ?: 0L
        if (FeedLoadMoreThrottle.isBlocked(blockedUntil)) return
        val lastAt = sectionTabLoadMoreAtMs[tab] ?: 0L
        if (!FeedLoadMoreThrottle.canLoadNow(lastAt)) return
        sectionTabLoadMoreAtMs[tab] = System.currentTimeMillis()
        sectionLoadMoreJobs[tab] = viewModelScope.launch {
            setTabLoadingMore(tab, true)
            try {
                val offset = itemsForTab(tab).size
                val result = withContext(Dispatchers.IO) {
                    fashApp.recommendationRepository.exploreListings(
                        publicBrowse = isGuestBrowse(),
                        limit = HomeTabLoadMorePageSize,
                        offset = offset,
                        surface = tab.analyticsSurface,
                        sizingMode = huntTodaySizingMode(),
                    )
                }
                if (_selectedFeedTab.value != tab) return@launch
                result.fold(
                    onSuccess = { page ->
                        if (page.isEmpty()) {
                            setTabHasMore(tab, false)
                        } else {
                            val added = appendUniqueItems(page, tab)
                            setTabHasMore(
                                tab,
                                page.size >= HomeTabLoadMorePageSize && added > 0,
                            )
                        }
                    },
                    onFailure = { err ->
                        val blocked = FeedLoadMoreThrottle.blockedUntilAfter(err)
                        if (blocked != null) {
                            sectionTabLoadMoreBlockedUntilMs[tab] = blocked
                        } else {
                            _events.tryEmit(
                                err.message?.takeIf { msg -> msg.isNotBlank() }
                                    ?: getApplication<Application>().getString(R.string.feed_load_error),
                            )
                        }
                    },
                )
            } finally {
                setTabLoadingMore(tab, false)
                sectionLoadMoreJobs.remove(tab)
            }
        }
    }

    private fun invalidateAllTabFeeds() {
        loadedTabs.clear()
        recommendationSectionsFetched = false
        tabLoadJobs.values.forEach { it.cancel() }
        tabLoadJobs.clear()
        sectionLoadMoreJobs.values.forEach { it.cancel() }
        sectionLoadMoreJobs.clear()
        tabStallWatch.cancelAll()
        _tabsLoading.value = emptySet()
        _tabsLoadError.value = emptySet()
        _tabsLoadStalled.value = emptySet()
        _tabFeedState.value = emptyMap()
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
        if (!force && tabLoadJobs[tab]?.isActive == true) return
        if (!force && itemsForTab(tab).isNotEmpty()) {
            loadedTabs.add(tab)
            if (tab in HomeFeedTab.recommendationSectionTabs()) {
                HomeFeedTab.recommendationSectionTabs().forEach { loadedTabs.add(it) }
            }
            return
        }
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
            beginTabLoad(tab)
            var ok = false
            try {
                ok = withTimeoutOrNull(TAB_LOAD_TIMEOUT_MS) {
                    withContext(Dispatchers.IO) {
                        when (tab) {
                            HomeFeedTab.HuntToday -> loadHuntTodayTab(force)
                            HomeFeedTab.Following -> loadFollowingTab(force)
                            HomeFeedTab.ForYou, HomeFeedTab.StylePicks, HomeFeedTab.SimilarSaved, HomeFeedTab.SeasonalNearYou ->
                                loadRecommendationSections(force)
                        }
                    }
                } == true
            } finally {
                if (isActive) {
                    finishTabLoad(tab, ok)
                } else {
                    clearTabLoadingWithoutMarkingLoaded(tab)
                }
            }
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
                setTabHasMore(
                    HomeFeedTab.HuntToday,
                    items.size >= sectionLimitFor(HomeFeedTab.HuntToday, HomeHuntTodayLimit),
                )
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
        val huntLimit = sectionLimitFor(HomeFeedTab.HuntToday, 12)
        val forYouLimit = sectionLimitFor(HomeFeedTab.ForYou, 16)
        val sectionLimit = maxOf(styleLimit, similarLimit)
        return fashApp.recommendationRepository.homeSections(
            publicBrowse = isGuestBrowse(),
            huntTodayLimit = huntLimit,
            forYouLimit = forYouLimit,
            sectionLimit = sectionLimit,
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
                        dailyOutfitDrop = sections.dailyOutfitDrop,
                        shoppingContext = sections.shoppingContext ?: cur.shoppingContext,
                    )
                }
                syncSellerFollowingFromListings(
                    sections.huntToday + sections.forYou + sections.stylePicks +
                        sections.similarToSaved + sections.seasonalNearYou,
                )
                if (sections.huntToday.isNotEmpty()) {
                    loadedTabs.add(HomeFeedTab.HuntToday)
                    setTabHasMore(HomeFeedTab.HuntToday, sections.huntToday.size >= huntLimit)
                }
                setTabHasMore(HomeFeedTab.ForYou, sections.forYou.size >= forYouLimit)
                setTabHasMore(HomeFeedTab.StylePicks, sections.stylePicks.size >= styleLimit)
                setTabHasMore(HomeFeedTab.SimilarSaved, sections.similarToSaved.size >= similarLimit)
                setTabHasMore(HomeFeedTab.SeasonalNearYou, sections.seasonalNearYou.size >= sectionLimit)
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

    /** Refresh metro + season chip after default address / profile location changes. */
    fun refreshShoppingContextAfterProfileSave() {
        viewModelScope.launch(Dispatchers.IO) { refreshShoppingContext() }
    }

    private suspend fun refreshShoppingContext() {
        if (isGuestBrowse()) {
            _discoveryBundle.update { it.copy(shoppingContext = null) }
            return
        }
        fashApp.recommendationRepository.shoppingContext(publicBrowse = false).fold(
            onSuccess = { ctx ->
                if (ctx.chipLabel() != null) {
                    _discoveryBundle.update { cur -> cur.copy(shoppingContext = ctx) }
                }
            },
            onFailure = { /* keep cached value from home-sections */ },
        )
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
            try {
                withContext(Dispatchers.IO) {
                    coroutineScope {
                        val stats = async { loadBuyerHomeStats() }
                        val shell = async { reloadHomeShell() }
                        val sellers = async { loadFeaturedSellers() }
                        val sizing = async { refreshSizingBannerState() }
                        val context = async { if (!isGuestBrowse()) refreshShoppingContext() }
                        val sections = async {
                            if (!isGuestBrowse()) loadRecommendationSections(force = false)
                        }
                        stats.await()
                        shell.await()
                        sellers.await()
                        sizing.await()
                        context.await()
                        sections.await()
                    }
                }
                if (!isActive) return@launch
                ensureTabLoaded(_selectedFeedTab.value, force = true)
                prefetchAdjacentTabs(_selectedFeedTab.value)
                lastSuccessfulRefreshAtMs = System.currentTimeMillis()
            } finally {
                _isLoading.value = false
            }
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
    private var followFeedLoadMoreBlockedUntilMs = 0L
    private val sectionTabLoadMoreAtMs = mutableMapOf<HomeFeedTab, Long>()
    private val sectionTabLoadMoreBlockedUntilMs = mutableMapOf<HomeFeedTab, Long>()

    fun loadMoreFollowFeed() {
        if (isGuestBrowse()) return
        if (_isLoadingMore.value || !_hasMoreItems.value) return
        if (_isLoading.value || _isRefreshing.value) return
        val offset = _items.value.size
        if (offset == 0) return
        if (FeedLoadMoreThrottle.isBlocked(followFeedLoadMoreBlockedUntilMs)) return
        if (!FeedLoadMoreThrottle.canLoadNow(lastFollowFeedLoadMoreAtMs, FeedLoadMoreThrottle.FOLLOWING_INTERVAL_MS)) return
        lastFollowFeedLoadMoreAtMs = System.currentTimeMillis()
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
                onFailure = { err ->
                    val blocked = FeedLoadMoreThrottle.blockedUntilAfter(err)
                    if (blocked != null) {
                        followFeedLoadMoreBlockedUntilMs = blocked
                    } else {
                        _events.tryEmit(
                            err.message?.takeIf { m -> m.isNotBlank() }
                                ?: getApplication<Application>().getString(R.string.feed_load_error),
                        )
                    }
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
        finishTabStallWatch(tab)
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
        _featuredSellers.value = emptyList()
        _featuredSellersLoading.value = false
        _showSizingBanner.value = false
        _isLoading.value = false
        _isRefreshing.value = false
        _isLoadingMore.value = false
        _tabsLoadStalled.value = emptySet()
        _tabFeedState.value = emptyMap()
        tabStallWatch.cancelAll()
        lastSuccessfulRefreshAtMs = 0L
    }

    /** Bottom nav re-tap on Home — scroll feed to top (pairs with [refresh]). */
    fun requestScrollHomeToTop() {
        viewModelScope.launch { _scrollHomeToTop.emit(Unit) }
    }

    /**
     * Guest shell reveal after [FashWaitingScreen] — re-emit scroll like bottom-nav re-tap so
     * [HomeFeedTabs] receives the event once the grid is composed (cold start parity with reload).
     */
    fun scheduleGuestHomeScrollToTopAfterReveal() {
        if (!isGuestBrowse()) return
        viewModelScope.launch {
            repeat(3) { attempt ->
                _scrollHomeToTop.emit(Unit)
                if (attempt < 2) delay(if (attempt == 0) 50L else 200L)
            }
        }
    }

    /** Tab swipe / tab change — scroll to pinned tab row (iOS `requestScrollHomeFeedToTop`). */
    fun requestScrollHomeFeedToTop() {
        viewModelScope.launch { _scrollHomeFeedToTop.emit(Unit) }
    }

    /** Refreshes only when data is older than [HomeFeedStaleThresholdMs]. */
    fun refreshIfStale() {
        val now = System.currentTimeMillis()
        if (now - lastSuccessfulRefreshAtMs < HomeFeedStaleThresholdMs) {
            if (_featuredSellers.value.isEmpty() && !_featuredSellersLoading.value) {
                viewModelScope.launch(Dispatchers.IO) { loadFeaturedSellers() }
            }
            return
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val selected = _selectedFeedTab.value
            try {
                invalidateAllTabFeeds()
                withContext(Dispatchers.IO) {
                    coroutineScope {
                        val stats = async { loadBuyerHomeStats() }
                        val shell = async { reloadHomeShell() }
                        val sellers = async { loadFeaturedSellers() }
                        val sizing = async { refreshSizingBannerState() }
                        val ux = async { if (!isGuestBrowse()) loadUxPersonalization() }
                        val context = async { if (!isGuestBrowse()) refreshShoppingContext() }
                        val sections = async {
                            if (!isGuestBrowse()) loadRecommendationSections(force = false)
                        }
                        stats.await()
                        shell.await()
                        sellers.await()
                        sizing.await()
                        ux.await()
                        context.await()
                        sections.await()
                    }
                }
                if (!isActive) return@launch
                ensureTabLoaded(selected, force = true)
                prefetchFromPersonalization(selected)
                lastSuccessfulRefreshAtMs = System.currentTimeMillis()
                requestScrollHomeToTop()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * After the splash home-gate times out, keep loading the visible tab in the background
     * so the grid does not stay on an infinite skeleton.
     */
    fun continueLaunchLoadIfNeeded() {
        val tab = _selectedFeedTab.value
        if (tab in loadedTabs && itemsForTab(tab).isNotEmpty()) return
        _isLoading.value = false
        ensureTabLoaded(tab, force = true)
    }

    /** End of maintenance — drop hung in-flight flags and fetch a fresh Home. */
    fun reloadAfterMaintenance() {
        lastSuccessfulRefreshAtMs = 0L
        _isLoading.value = false
        _isRefreshing.value = false
        invalidateAllTabFeeds()
        loadFeed()
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
                    emitSnackbarMessage(_events, FeedEngagementFeedback.likeMessageRes(liked))
                },
                onFailure = {
                    emitSnackbarMessage(
                        _events,
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
                    emitSnackbarMessage(_events, FeedEngagementFeedback.saveMessageRes(saved))
                },
                onFailure = {
                    emitSnackbarMessage(
                        _events,
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
