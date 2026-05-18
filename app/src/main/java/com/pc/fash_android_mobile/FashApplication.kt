package com.pc.fash_android_mobile

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.pc.fash_android_mobile.data.auth.AppAuthManager
import com.pc.fash_android_mobile.data.auth.AuthTokenRefreshCoordinator
import com.pc.fash_android_mobile.data.auth.AuthRepository
import com.pc.fash_android_mobile.data.auth.AuthSessionStore
import com.pc.fash_android_mobile.data.chat.ChatRepository
import com.pc.fash_android_mobile.data.deal.DealRepository
import com.pc.fash_android_mobile.data.advertising.AdvertisingRepository
import com.pc.fash_android_mobile.data.promo.AppPromoCampaign
import com.pc.fash_android_mobile.data.promo.AppPromoInterstitialRepository
import com.pc.fash_android_mobile.data.common.CommonServiceRepository
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
import com.pc.fash_android_mobile.data.search.SearchRepository
import com.pc.fash_android_mobile.data.ui.UiDialogController
import com.pc.fash_android_mobile.data.locale.AppLocale
import com.pc.fash_android_mobile.data.user.UserRepository
import com.pc.fash_android_mobile.deeplink.AccountSwitchPrompt
import com.pc.fash_android_mobile.notifications.FashNotificationChannels
import com.pc.fash_android_mobile.notifications.FcmTokenRegistrar
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

    /**
     * Ledger row id from FCM / `fash://inbox/{id}`. Consumed when main shell opens the inbox detail sheet.
     */
    val pendingInboxNotificationId = MutableStateFlow<String?>(null)

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
    ) {
        applicationScope.launch {
            _inAppNotification.value = FashInAppNotificationSession(
                title = title.trim(),
                body = body.trim(),
                data = data,
                userNotificationId = userNotificationId?.trim()?.takeIf { it.isNotEmpty() },
            )
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
        AppLocale.installApplicationContext(this)
        AppLocale.applyPersistedOrDefault(this)
        FashNotificationChannels.ensureChannels(this)
        applicationScope.launch(Dispatchers.IO) {
            val hasSession = runCatching { authManager.sessionStore.read() != null }.getOrDefault(false)
            authManager.hydrateInitialAuthFromStore(hasSession)
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
        )
    }

    val listingRepository: ListingRepository by lazy {
        ListingRepository(
            securedClient = authManager
                .createSecuringClient { reason -> authManager.onSessionCleared(reason) }
                .createClient(),
        )
    }

    /** Core-service promo / advertising CMS (`GET /app/advertising/slides`). */
    val advertisingRepository: AdvertisingRepository by lazy {
        AdvertisingRepository(
            securedClient = authManager
                .createSecuringClient { reason -> authManager.onSessionCleared(reason) }
                .createClient(),
            localeTagProvider = {
                com.pc.fash_android_mobile.data.locale.AppLocale.currentTag(this@FashApplication)
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

    /** common-service catalog GETs (addresses, brands, categories, aesthetic-tags, countries). */
    val commonServiceRepository: CommonServiceRepository by lazy {
        CommonServiceRepository(
            securedClient = authManager
                .createSecuringClient { reason -> authManager.onSessionCleared(reason) }
                .createClient(),
        )
    }

    val searchRepository: SearchRepository by lazy {
        SearchRepository(
            securedClient = authManager
                .createSecuringClient { reason -> authManager.onSessionCleared(reason) }
                .createClient(),
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
            authRepository = authManager.authRepository,
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
