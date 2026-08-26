package com.pc.fash_android_mobile

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.pc.fash_android_mobile.data.auth.AppAuthManager
import com.pc.fash_android_mobile.data.auth.AuthTokenRefreshCoordinator
import com.pc.fash_android_mobile.data.auth.AuthRepository
import com.pc.fash_android_mobile.data.auth.AuthSessionStore
import com.pc.fash_android_mobile.data.chat.ChatRepository
import com.pc.fash_android_mobile.data.deal.DealRepository
import com.pc.fash_android_mobile.data.appstatus.AppMaintenanceController
import com.pc.fash_android_mobile.data.appstatus.AppStatusRepository
import com.pc.fash_android_mobile.data.advertising.AdvertisingRepository
import com.pc.fash_android_mobile.data.promo.AppPromoCampaign
import com.pc.fash_android_mobile.data.promo.AppPromoInterstitialRepository
import com.pc.fash_android_mobile.data.common.CommonAestheticTagDto
import com.pc.fash_android_mobile.data.common.CommonServiceRepository
import com.pc.fash_android_mobile.data.common.PublicCommonCatalogRepository
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.address.AddressLocalStore
import com.pc.fash_android_mobile.data.onboarding.OnboardingLocalStore
import com.pc.fash_android_mobile.data.address.UserShippingAddressRepository
import com.pc.fash_android_mobile.data.order.OrderCancelCoordinator
import com.pc.fash_android_mobile.data.order.OrderRepository
import com.pc.fash_android_mobile.data.payment.CorePaymentRepository
import com.pc.fash_android_mobile.data.payment.MockPaymentService
import com.pc.fash_android_mobile.data.payment.PaymentService
import com.pc.fash_android_mobile.data.realtime.RealtimeManager
import com.pc.fash_android_mobile.data.recommendation.BrowseSessionStore
import com.pc.fash_android_mobile.data.recommendation.FeedEventReporter
import com.pc.fash_android_mobile.data.recommendation.AppSessionTracker
import com.pc.fash_android_mobile.data.recommendation.RecommendationRepository
import com.pc.fash_android_mobile.data.search.SearchRepository
import com.pc.fash_android_mobile.network.PublicBrowseHttp
import com.pc.fash_android_mobile.data.ui.UiDialogController
import com.pc.fash_android_mobile.data.locale.AppLocale
import com.pc.fash_android_mobile.data.locale.PreferredLocaleSync
import com.pc.fash_android_mobile.data.user.NotificationPreferencesRepository
import com.pc.fash_android_mobile.ui.notifications.ExploreNavigationFilter
import com.pc.fash_android_mobile.data.user.UserRepository
import com.pc.fash_android_mobile.deeplink.AccountSwitchPrompt
import com.pc.fash_android_mobile.notifications.FashNotificationChannels
import com.pc.fash_android_mobile.notifications.FcmTokenRegistrar
import com.pc.fash_android_mobile.notifications.InAppNotificationPresentation
import com.pc.fash_android_mobile.ui.chat.ChatInAppNotificationPolicy
import com.pc.fash_android_mobile.ui.chat.ChatNotificationPresence
import com.pc.fash_android_mobile.ui.chat.ChatUnreadRefreshHub
import com.pc.fash_android_mobile.ui.chat.ChatViewModel
import com.pc.fash_android_mobile.ui.chat.InboxNotificationSync
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

/**
 * WebSocket base for [RealtimeManager]: prefer `REALTIME_BASE_URL` in env ([BuildConfig.REALTIME_BASE_URL]),
 * else derive `api-core` → `api-realtime`, else legacy path swap / dev IP.
 */
private fun resolveRealtimeBaseUrl(): String {
    val explicit = BuildConfig.REALTIME_BASE_URL.trim()
    if (explicit.isNotEmpty()) return explicit

    val api = BuildConfig.API_BASE_URL.trim()
    val fromPath = api.replace("core-service", "realtime-service", ignoreCase = true)
    if (fromPath.contains("realtime-service", ignoreCase = true)) {
        return fromPath
    }
    val fromHost = api.replace("api-core.", "api-realtime.", ignoreCase = true)
    if (fromHost != api) return fromHost

    return "http://76.13.211.193/realtime-service/"
}

/**
 * Application-scoped auth and network dependencies.
 * Every secured OkHttpClient passes [AppAuthManager.onSessionCleared] with the
 * server-supplied reason so the UI can show an explanation when the session is
 * force-expired (e.g. refresh token expired).
 *
 * Implements [ImageLoaderFactory] so Coil uses one app-wide [ImageLoader] (caching + consistent config).
 * [java.io.InterruptedIOException] during decode often means the image request was cancelled (e.g. scrolling
 * away); that is normal. Framework/HWUI may still log decode interruptions; `logger(null)` disables Coil logs.
 */
class FashApplication : Application(), ImageLoaderFactory {

    private fun initSentryIfConfigured() {
        val dsn = BuildConfig.SENTRY_DSN.trim()
        if (dsn.isEmpty()) return
        SentryAndroid.init(this) { options ->
            options.dsn = dsn
            options.environment = BuildConfig.ENVIRONMENT_NAME
            options.tracesSampleRate = 0.2
            options.release = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .logger(null)
            .build()

    /** Global success/error/info dialogs; observe from [com.pc.fash_android_mobile.MainActivity]. */
    val uiDialog: UiDialogController by lazy { UiDialogController() }

    /**
     * Listing id from VIEW intent (share / deep link). Consumed when main shell opens product detail.
     * Held on the app so Compose under [com.pc.fash_android_mobile.ui.locale.ProvideAppLocale] can
     * read it via [applicationContext] without [androidx.activity.compose.LocalActivity] (null in that subtree).
     */
    val pendingDeepLinkListingId = MutableStateFlow<String?>(null)

    /** Seller @username from profile shop share / deep link. Opens seller shop overlay when consumed. */
    val pendingDeepLinkSellerUsername = MutableStateFlow<String?>(null)

    /**
     * Ledger row id from FCM / `fash://inbox/{id}`. Consumed when main shell opens the inbox detail sheet.
     */
    val pendingInboxNotificationId = MutableStateFlow<String?>(null)

    /**
     * When set, invite HTTPS / fash deep links included `?r=...` for referral attribution on first onboard.
     * Cleared after successful username onboard.
     */
    val pendingReferralToken = MutableStateFlow<String?>(null)

    /**
     * Optional `?ref=` handle from invite links — display-only hint until the user signs up.
     * Cleared after successful username onboard.
     */
    val pendingReferrerUsername = MutableStateFlow<String?>(null)

    /**
     * When true, authenticated main shell should open the invite-friends screen once (from `fash://invite`).
     * Cleared after consumption.
     */
    val pendingOpenInviteFriends = MutableStateFlow(false)

    /** In-app banner tap → order detail ([MainActivity] consumes). */
    val pendingOpenOrderId = MutableStateFlow<String?>(null)

    /** Profile-completion push → onboarding shell ([MainActivity] consumes). */
    val pendingOpenOnboarding = MutableStateFlow(false)

    /** Push/inbox → Explore overlay with recommendation filters ([MainNavScreen] consumes). */
    val pendingExploreNavigationFilter = MutableStateFlow<ExploreNavigationFilter?>(null)

    /** Guest local reminder → signup nudge sheet ([GuestMainShell] consumes). */
    val pendingGuestSignupNudge = MutableStateFlow(false)

    /** FCM tray / banner tap → chat thread ([MainActivity] consumes). */
    val pendingOpenChatConversationId = MutableStateFlow<String?>(null)

    /** Multi-account FCM: user B is active but account A has new inbox rows. */
    val pendingAccountSwitchPrompt = MutableStateFlow<AccountSwitchPrompt?>(null)

    fun requestAccountSwitchPrompt(prompt: AccountSwitchPrompt) {
        applicationScope.launch {
            pendingAccountSwitchPrompt.value = prompt
        }
    }

    fun clearAccountSwitchPrompt() {
        pendingAccountSwitchPrompt.value = null
    }

    /**
     * Incremented when the user should land on the inbox list (e.g. snackbar “Open” after unread increased).
     * [com.pc.fash_android_mobile.ui.main.MainNavScreen] opens the notification overlay when this changes past 0.
     */
    private val _inboxOpenRequestGeneration = MutableStateFlow(0L)
    val inboxOpenRequestGeneration = _inboxOpenRequestGeneration.asStateFlow()

    /** True while resolving a tray-tap deep link to a specific inbox row ([openInboxDetailFromPush]). */
    private val _inboxOpenFromTrayTap = MutableStateFlow(false)

    fun markInboxOpenFromTrayTap() {
        _inboxOpenFromTrayTap.value = true
    }

    /** Consumes and returns whether the current inbox open was initiated by a system notification tap. */
    fun consumeInboxOpenFromTrayTap(): Boolean {
        val fromTray = _inboxOpenFromTrayTap.value
        _inboxOpenFromTrayTap.value = false
        return fromTray
    }

    /** Tray tap / push payload with a concrete inbox notification id. */
    fun requestOpenInboxNotificationFromPush(notificationId: String) {
        val id = notificationId.trim()
        if (id.isEmpty()) return
        pendingInboxNotificationId.value = id
        markInboxOpenFromTrayTap()
        requestOpenNotificationInbox()
    }

    fun requestOpenNotificationInbox() {
        _inboxOpenRequestGeneration.update { it + 1L }
    }

    /**
     * Incremented when profile load fails but the account is (likely) not fully set up — [MainActivity] should
     * re-fetch setup-status and return the user to onboarding instead of leaving them on a broken home shell.
     */
    private val _setupGateRecheckGeneration = MutableStateFlow(0L)
    val setupGateRecheckGeneration = _setupGateRecheckGeneration.asStateFlow()

    fun requestSetupGateRecheckFromIncompleteProfile() {
        _setupGateRecheckGeneration.update { it + 1L }
    }

    fun resetSetupGateRecheckGeneration() {
        _setupGateRecheckGeneration.value = 0L
    }

    private val _aestheticTagCatalog = MutableStateFlow<List<CommonAestheticTagDto>>(emptyList())
    val aestheticTagCatalog: StateFlow<List<CommonAestheticTagDto>> = _aestheticTagCatalog.asStateFlow()

    /** Loads/refreshes common-service aesthetic tag catalog (locale via Accept-Language). */
    fun refreshAestheticTagCatalog() {
        applicationScope.launch(Dispatchers.IO) {
            commonServiceRepository.getAestheticTags(all = true)
                .onSuccess { _aestheticTagCatalog.value = it }
        }
    }

    /**
     * Must not use [kotlinx.coroutines.runBlocking] in [onCreate]: it blocks the main thread until
     * the coroutine finishes, which defeats IO dispatchers and causes "failed to complete startup"
     * ANRs when EncryptedSharedPreferences / keystore is slow under memory pressure.
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * FCM or realtime signaled that inbox unread count may have changed.
     * [com.pc.fash_android_mobile.MainActivity] collects this and calls [com.pc.fash_android_mobile.ui.notifications.NotificationsViewModel.refreshUnreadSummary].
     */
    private val _inboxUnreadRefreshSignals = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val inboxUnreadRefreshSignals: SharedFlow<Unit> = _inboxUnreadRefreshSignals.asSharedFlow()

    /** Admin promo interstitial from FCM data payload (backup when WS missed). */
    private val _appPromoShowSignals = MutableSharedFlow<AppPromoCampaign>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val appPromoShowSignals: SharedFlow<AppPromoCampaign> = _appPromoShowSignals.asSharedFlow()

    private val _inAppNotification = MutableStateFlow<FashInAppNotificationSession?>(null)
    /** WebSocket-driven toast when user is online (mirrors FCM payload shape). */
    val inAppNotification: StateFlow<FashInAppNotificationSession?> = _inAppNotification.asStateFlow()

    @Volatile
    private var inboxUnreadRefreshJob: Job? = null

    /**
     * True while the main shell runs without a user session (browse-only). Repositories use
     * [publicBrowseSearchRepository] / public listing paths when this is set.
     */
    @Volatile
    var isGuestBrowseActive: Boolean = false

    /** Conversation id while [ChatDetailScreen] is composed — suppresses duplicate in-app chat toasts. */
    @Volatile
    var activeChatConversationId: String? = null

    val publicBrowseHttpClient: OkHttpClient? by lazy {
        if (PublicBrowseHttp.isConfigured()) PublicBrowseHttp.createClient() else null
    }

    fun requestShowAppPromo(campaign: AppPromoCampaign) {
        applicationScope.launch {
            _appPromoShowSignals.emit(campaign)
        }
    }

    fun showInAppNotificationFromRealtime(
        title: String,
        body: String,
        data: Map<String, String>?,
        userNotificationId: String?,
        openConversationId: String? = null,
        chatViewModel: ChatViewModel? = null,
    ) {
        val openId = ChatNotificationPresence.openConversationId(
            openConversationId,
            activeChatConversationId,
        )
        if (ChatInAppNotificationPolicy.shouldSuppressInApp(data, openId)) {
            runSuppressedChatNotificationSideEffects(data)
            return
        }
        val session = InAppNotificationPresentation.enrich(
            context = this,
            session = FashInAppNotificationSession(
                title = title.trim(),
                body = body.trim(),
                data = data,
                userNotificationId = userNotificationId?.trim()?.takeIf { it.isNotEmpty() },
            ),
            chatViewModel = chatViewModel,
        )
        applicationScope.launch {
            _inAppNotification.value = session
        }
    }

    fun runSuppressedChatNotificationSideEffects(data: Map<String, String>?) {
        applicationScope.launch(Dispatchers.IO) {
            ChatInAppNotificationPolicy.conversationId(data)?.let { cid ->
                InboxNotificationSync.markChatNotificationsRead(cid, userRepository)
            }
            requestInboxUnreadRefreshDebounced()
            if (ChatInAppNotificationPolicy.isChatRelated(data)) {
                ChatUnreadRefreshHub.notifyMarkedRead()
            }
        }
    }

    fun dismissInAppNotification() {
        applicationScope.launch {
            _inAppNotification.value = null
        }
    }

    /** Debounced so burst FCM / WS frames do not hammer the API. */
    fun requestInboxUnreadRefreshDebounced() {
        inboxUnreadRefreshJob?.cancel()
        inboxUnreadRefreshJob = applicationScope.launch {
            delay(400)
            _inboxUnreadRefreshSignals.emit(Unit)
        }
    }

    override fun onCreate() {
        super.onCreate()
        initSentryIfConfigured()
        AppLocale.installApplicationContext(this)
        AppLocale.applyPersistedOrDefault(this)
        FashNotificationChannels.ensureChannels(this)
        appSessionTracker.install()
        applicationScope.launch(Dispatchers.IO) {
            refreshAestheticTagCatalog()
            val hasSession = runCatching { authManager.sessionStore.read() != null }.getOrDefault(false)
            authManager.hydrateInitialAuthFromStore(hasSession)
            fcmTokenRegistrar.subscribeAppStatusTopic()
            appMaintenanceController.refresh()
        }
        registerAppStatusNetworkCallback()
    }

    private fun registerAppStatusNetworkCallback() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching {
            cm.registerNetworkCallback(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        applicationScope.launch(Dispatchers.IO) {
                            appMaintenanceController.refresh()
                        }
                    }
                },
            )
        }
    }

    val authManager: AppAuthManager by lazy {
        AppAuthManager(
            sessionStore = AuthSessionStore(this),
            authRepository = AuthRepository(),
        )
    }

    val userRepository: UserRepository by lazy {
        UserRepository(
            publicClient = UserRepository.defaultClient(),
            securedClient = authManager
                .createSecuringClient { reason -> authManager.onSessionCleared(reason) }
                .createClient(),
            publicBrowseClient = publicBrowseHttpClient,
        )
    }

    val listingRepository: ListingRepository by lazy {
        ListingRepository(
            securedClient = authManager
                .createSecuringClient { reason -> authManager.onSessionCleared(reason) }
                .createClient(),
            publicBrowseClient = publicBrowseHttpClient,
        )
    }

    /** Core-service promo / advertising CMS — secured + public browse for guest shell. */
    val advertisingRepository: AdvertisingRepository by lazy {
        AdvertisingRepository(
            securedClient = authManager
                .createSecuringClient { reason -> authManager.onSessionCleared(reason) }
                .createClient(),
            publicBrowseClient = publicBrowseHttpClient,
            localeTagProvider = {
                AppLocale.currentTag(this@FashApplication)
            },
        )
    }

    /** Seller utility packages (`GET /app/advertising/product-packages`) — Bearer via [SecuredApiClient]. */
    val sellerProductPackageRepository: com.pc.fash_android_mobile.data.sellerpackages.SellerProductPackageRepository by lazy {
        com.pc.fash_android_mobile.data.sellerpackages.SellerProductPackageRepository(
            securedClient = authManager
                .createSecuringClient { reason -> authManager.onSessionCleared(reason) }
                .createClient(),
            localeTagProvider = {
                AppLocale.currentTag(this@FashApplication)
            },
        )
    }

    /** Admin promo interstitials pull backup (`GET /app/promo-interstitials/active`). */
    val appPromoInterstitialRepository: AppPromoInterstitialRepository by lazy {
        AppPromoInterstitialRepository(
            securedClient = authManager
                .createSecuringClient { reason -> authManager.onSessionCleared(reason) }
                .createClient(),
        )
    }

    val appStatusRepository: AppStatusRepository by lazy {
        AppStatusRepository(
            localeTagProvider = { AppLocale.currentTag(this@FashApplication) },
        )
    }

    val appMaintenanceController: AppMaintenanceController by lazy {
        AppMaintenanceController(
            repository = appStatusRepository,
            prefs = getSharedPreferences("fash_app_status", android.content.Context.MODE_PRIVATE),
        )
    }

    /** Public editorial guides (common-service `GET /api/v1/public/editorial-guides`). */
    val editorialGuideRepository: com.pc.fash_android_mobile.data.editorial.EditorialGuideRepository by lazy {
        com.pc.fash_android_mobile.data.editorial.EditorialGuideRepository {
            com.pc.fash_android_mobile.data.locale.AppLocale.currentTag(this@FashApplication)
        }
    }

    /** In-app UX surveys — secured users + guest public browse with device guest_key. */
    val uxSurveyRepository: com.pc.fash_android_mobile.data.uxsurvey.UxSurveyRepository by lazy {
        com.pc.fash_android_mobile.data.uxsurvey.UxSurveyRepository(
            securedClient = authManager
                .createSecuringClient { reason -> authManager.onSessionCleared(reason) }
                .createClient(),
            publicBrowseClient = publicBrowseHttpClient,
            guestSurveyProvider = {
                isGuestBrowseActive ||
                    authManager.sessionStore.read()?.accessToken.isNullOrBlank() != false
            },
            guestKeyProvider = { browseSessionStore.sessionId() },
            localeTagProvider = {
                com.pc.fash_android_mobile.data.locale.AppLocale.currentTag(this@FashApplication)
            },
        )
    }

    /** common-service catalog GETs (addresses, brands, categories, aesthetic-tags, countries). */
    val publicCommonCatalogRepository: PublicCommonCatalogRepository by lazy {
        PublicCommonCatalogRepository {
            com.pc.fash_android_mobile.data.locale.AppLocale.currentTag(this@FashApplication)
        }
    }

    val commonServiceRepository: CommonServiceRepository by lazy {
        CommonServiceRepository(
            securedClient = authManager
                .createSecuringClient { reason -> authManager.onSessionCleared(reason) }
                .createClient(),
            publicCatalogRepository = publicCommonCatalogRepository,
        )
    }

    val searchRepository: SearchRepository by lazy {
        SearchRepository(
            securedClient = authManager
                .createSecuringClient { reason -> authManager.onSessionCleared(reason) }
                .createClient(),
            publicBrowseClient = publicBrowseHttpClient,
        )
    }

    val recommendationRepository: RecommendationRepository by lazy {
        RecommendationRepository(
            securedClient = authManager
                .createSecuringClient { reason -> authManager.onSessionCleared(reason) }
                .createClient(),
            publicBrowseClient = publicBrowseHttpClient,
        )
    }

    val browseSessionStore: BrowseSessionStore by lazy {
        BrowseSessionStore(applicationContext)
    }

    val feedEventReporter: FeedEventReporter by lazy {
        FeedEventReporter(
            repository = recommendationRepository,
            sessionIdProvider = {
                if (isGuestBrowseActive) {
                    browseSessionStore.sessionId()
                } else {
                    val uid = authManager.sessionStore.read()?.userId
                    if (!uid.isNullOrBlank()) browseSessionStore.sessionIdForUser(uid)
                    else browseSessionStore.sessionId()
                }
            },
            publicBrowse = { isGuestBrowseActive },
            scope = applicationScope,
        )
    }

    private val appSessionTracker: AppSessionTracker by lazy {
        AppSessionTracker(
            feedEventReporter = feedEventReporter,
            onForeground = {
                applicationScope.launch(Dispatchers.IO) {
                    fcmTokenRegistrar.subscribeAppStatusTopic()
                }
                if (authManager.sessionStore.read() != null) {
                    realtimeManager.connect()
                    realtimeManager.sendPresenceActive()
                    applicationScope.launch(Dispatchers.IO) {
                        fcmTokenRegistrar.registerCurrentTokenIfSession()
                    }
                }
            },
            onBackground = {
                realtimeManager.pauseForBackground()
            },
        )
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(
            securedClient = authManager
                .createSecuringClient { reason -> authManager.onSessionCleared(reason) }
                .createClient(),
            sessionStore = authManager.sessionStore,
        )
    }

    val orderRepository: OrderRepository by lazy {
        OrderRepository(
            securedClient = authManager
                .createSecuringClient { reason -> authManager.onSessionCleared(reason) }
                .createClient(),
        )
    }

    /** In-person / offline deals (`POST /deals`, complete, cancel, review). */
    val dealRepository: DealRepository by lazy {
        DealRepository(
            securedClient = authManager
                .createSecuringClient { reason -> authManager.onSessionCleared(reason) }
                .createClient(),
        )
    }

    val orderCancelCoordinator: OrderCancelCoordinator by lazy {
        OrderCancelCoordinator(orderRepository, chatRepository)
    }

    /** Local shipping address book + per-order selection (sync with core when API is available). */
    val addressLocalStore: AddressLocalStore by lazy { AddressLocalStore(this) }

    /** Optional onboarding skips (aesthetic / sizing) per user; cleared on logout from [LoginViewModel]. */
    val onboardingLocalStore: OnboardingLocalStore by lazy { OnboardingLocalStore(this) }

    /** Core `GET/POST /users/me/shipping-addresses` + set default. */
    val userShippingAddressRepository: UserShippingAddressRepository by lazy {
        UserShippingAddressRepository(
            securedClient = authManager
                .createSecuringClient { reason -> authManager.onSessionCleared(reason) }
                .createClient(),
        )
    }

    /** Core `GET/PUT /users/me/notification-preferences`. */
    val notificationPreferencesRepository: NotificationPreferencesRepository by lazy {
        NotificationPreferencesRepository(
            securedClient = authManager
                .createSecuringClient { reason -> authManager.onSessionCleared(reason) }
                .createClient(),
        )
    }

    /**
     * Core-proxied payment initiation (returns gateway URL). Never calls payment-service internal APIs.
     */
    val corePaymentRepository: CorePaymentRepository by lazy {
        CorePaymentRepository(
            securedClient = authManager
                .createSecuringClient { reason -> authManager.onSessionCleared(reason) }
                .createClient(),
        )
    }

    /** Legacy mock — reserved for tests / offline demos. */
    val paymentService: PaymentService by lazy { MockPaymentService() }

    /**
     * Singleton WebSocket manager for the Fash realtime service.
     * Call [RealtimeManager.connect] once after a successful login and
     * [RealtimeManager.disconnect] on sign-out.
     *
     * Base URL: [BuildConfig.REALTIME_BASE_URL] when set (e.g. `https://api-realtime.fashandcurious.com/`);
     * otherwise [resolveRealtimeBaseUrl].
     */
    val realtimeManager: RealtimeManager by lazy {
        val realtimeBaseUrl = resolveRealtimeBaseUrl()
        RealtimeManager(
            sessionStore = authManager.sessionStore,
            realtimeBaseUrl = realtimeBaseUrl,
            // INTEGRATION.md §3.1: when the server returns 401 on the WS handshake, refresh
            // the access token before retrying so we never loop with a stale token.
            tokenRefresher = {
                val session = authManager.sessionStore.read() ?: return@RealtimeManager null
                AuthTokenRefreshCoordinator.refreshIfStillCurrent(
                    authManager.sessionStore,
                    authManager.authRepository,
                    session.accessToken,
                ).getOrNull()?.also { authManager.onSessionSaved() }?.accessToken
            },
        )
    }

    /** Registers FCM device token with core-service after login ([AuthRepository.registerFcm]). */
    val fcmTokenRegistrar: FcmTokenRegistrar by lazy {
        FcmTokenRegistrar(
            appContext = this,
            authRepository = authManager.authRepository,
            sessionStore = authManager.sessionStore,
            clientLocaleProvider = { AppLocale.coreApiPathSegment() },
        )
    }

    /** Syncs in-app language to `profiles.preferred_locale` when signed in. */
    val preferredLocaleSync: PreferredLocaleSync by lazy {
        PreferredLocaleSync(
            userRepository = userRepository,
            sessionStore = authManager.sessionStore,
        )
    }
}

/** Payload for transient in-app notification banner ([FashApplication.inAppNotification]). */
data class FashInAppNotificationSession(
    val title: String,
    val body: String,
    val data: Map<String, String>?,
    val userNotificationId: String?,
    val shownAt: Long = System.nanoTime(),
)
