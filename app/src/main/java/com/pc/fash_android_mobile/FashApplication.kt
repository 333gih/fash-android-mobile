package com.pc.fash_android_mobile

import com.pc.fash_android_mobile.data.auth.AppAuthManager
import com.pc.fash_android_mobile.data.auth.AuthRepository
import com.pc.fash_android_mobile.data.auth.AuthSessionStore
import com.pc.fash_android_mobile.data.chat.ChatRepository
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.address.AddressLocalStore
import com.pc.fash_android_mobile.data.order.OrderRepository
import com.pc.fash_android_mobile.data.payment.CorePaymentRepository
import com.pc.fash_android_mobile.data.payment.MockPaymentService
import com.pc.fash_android_mobile.data.payment.PaymentService
import com.pc.fash_android_mobile.data.realtime.RealtimeManager
import com.pc.fash_android_mobile.data.search.SearchRepository
import com.pc.fash_android_mobile.data.ui.UiDialogController
import com.pc.fash_android_mobile.data.locale.AppLocale
import com.pc.fash_android_mobile.data.user.UserRepository
import com.pc.fash_android_mobile.notifications.FashNotificationChannels
import com.pc.fash_android_mobile.notifications.FcmTokenRegistrar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application-scoped auth and network dependencies.
 * Every secured OkHttpClient passes [AppAuthManager.onSessionCleared] with the
 * server-supplied reason so the UI can show an explanation when the session is
 * force-expired (e.g. refresh token expired).
 */
class FashApplication : android.app.Application() {

    /** Global success/error/info dialogs; observe from [com.pc.fash_android_mobile.MainActivity]. */
    val uiDialog: UiDialogController by lazy { UiDialogController() }

    /**
     * Must not use [kotlinx.coroutines.runBlocking] in [onCreate]: it blocks the main thread until
     * the coroutine finishes, which defeats IO dispatchers and causes "failed to complete startup"
     * ANRs when EncryptedSharedPreferences / keystore is slow under memory pressure.
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

    /** Local shipping address book + per-order selection (sync with core when API is available). */
    val addressLocalStore: AddressLocalStore by lazy { AddressLocalStore(this) }

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
     * URL is derived from the existing API_BASE_URL (same host, realtime-service path)
     * so no extra BuildConfig field is required — the app boots safely even without a
     * full Gradle re-sync after adding REALTIME_BASE_URL to the env files.
     */
    val realtimeManager: RealtimeManager by lazy {
        val realtimeBaseUrl = BuildConfig.API_BASE_URL
            .replace("core-service", "realtime-service")
            .takeIf { it.contains("realtime-service") }
            ?: "http://76.13.211.193/realtime-service/"
        RealtimeManager(
            sessionStore = authManager.sessionStore,
            realtimeBaseUrl = realtimeBaseUrl,
            // INTEGRATION.md §3.1: when the server returns 401 on the WS handshake, refresh
            // the access token before retrying so we never loop with a stale token.
            tokenRefresher = {
                val session = authManager.sessionStore.read() ?: return@RealtimeManager null
                authManager.authRepository.refresh(session.refreshToken)
                    .getOrNull()
                    ?.also { newSession ->
                        authManager.sessionStore.save(newSession)
                        authManager.onSessionSaved()
                    }
                    ?.accessToken
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
