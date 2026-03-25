package com.pc.fash_android_mobile

import com.pc.fash_android_mobile.BuildConfig
import com.pc.fash_android_mobile.data.auth.AppAuthManager
import com.pc.fash_android_mobile.data.auth.AuthRepository
import com.pc.fash_android_mobile.data.auth.AuthSessionStore
import com.pc.fash_android_mobile.data.chat.ChatRepository
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.order.OrderRepository
import com.pc.fash_android_mobile.data.payment.MockPaymentService
import com.pc.fash_android_mobile.data.payment.PaymentService
import com.pc.fash_android_mobile.data.realtime.RealtimeManager
import com.pc.fash_android_mobile.data.search.SearchRepository
import com.pc.fash_android_mobile.data.user.UserRepository

/**
 * Application-scoped auth and network dependencies.
 * Every secured OkHttpClient passes [AppAuthManager.onSessionCleared] with the
 * server-supplied reason so the UI can show an explanation when the session is
 * force-expired (e.g. refresh token expired).
 */
class FashApplication : android.app.Application() {

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

    /** Replace with real implementation when payment API is available. */
    val paymentService: PaymentService by lazy { MockPaymentService() }

    /**
     * Singleton WebSocket manager for the Fash realtime service.
     * Call [RealtimeManager.connect] once after a successful login and
     * [RealtimeManager.disconnect] on sign-out.
     */
    val realtimeManager: RealtimeManager by lazy {
        RealtimeManager(
            sessionStore = authManager.sessionStore,
            realtimeBaseUrl = BuildConfig.REALTIME_BASE_URL,
        )
    }
}
