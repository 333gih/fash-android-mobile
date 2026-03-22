package com.pc.fash_android_mobile

import com.pc.fash_android_mobile.data.auth.AppAuthManager
import com.pc.fash_android_mobile.data.auth.AuthRepository
import com.pc.fash_android_mobile.data.auth.AuthSessionStore
import com.pc.fash_android_mobile.data.chat.ChatRepository
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.order.OrderRepository
import com.pc.fash_android_mobile.data.payment.MockPaymentService
import com.pc.fash_android_mobile.data.payment.PaymentService
import com.pc.fash_android_mobile.data.search.SearchRepository
import com.pc.fash_android_mobile.data.user.UserRepository

/**
 * Application-scoped auth and network dependencies.
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
            securedClient = authManager.createSecuringClient { authManager.onSessionCleared() }.createClient(),
        )
    }

    val listingRepository: ListingRepository by lazy {
        ListingRepository(
            securedClient = authManager.createSecuringClient { authManager.onSessionCleared() }.createClient(),
        )
    }

    val searchRepository: SearchRepository by lazy {
        SearchRepository(
            securedClient = authManager.createSecuringClient { authManager.onSessionCleared() }.createClient(),
        )
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(
            securedClient = authManager.createSecuringClient { authManager.onSessionCleared() }.createClient(),
        )
    }

    val orderRepository: OrderRepository by lazy {
        OrderRepository(
            securedClient = authManager.createSecuringClient { authManager.onSessionCleared() }.createClient(),
        )
    }

    /** Replace with real implementation when payment API is available. */
    val paymentService: PaymentService by lazy { MockPaymentService() }
}
