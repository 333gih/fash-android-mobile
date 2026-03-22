package com.pc.fash_android_mobile.network

import com.pc.fash_android_mobile.data.auth.AuthRepository
import com.pc.fash_android_mobile.data.auth.AuthSessionStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Provides a secured OkHttpClient that:
 * - Injects Bearer token into all requests
 * - Refreshes token on 401 and retries once
 * - Calls [onSessionInvalidated] when refresh fails (session cleared)
 * - Never logs tokens or sensitive data
 *
 * Usage: `authManager.createSecuringClient { authManager.onSessionCleared() }.createClient()`
 */
class SecuredApiClient(
    private val sessionStore: AuthSessionStore,
    private val authRepository: AuthRepository,
    private val onSessionInvalidated: (() -> Unit)? = null,
) {

    fun createClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor(sessionStore))
        .addInterceptor(RefreshTokenInterceptor(sessionStore, authRepository, onSessionInvalidated))
        .build()

    private class AuthInterceptor(
        private val sessionStore: AuthSessionStore,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val session = sessionStore.read()
            val request = chain.request()
            val newRequest = if (session != null && session.accessToken.isNotBlank()) {
                request.newBuilder()
                    .header("Authorization", "${session.tokenType.ifBlank { "Bearer" }} ${session.accessToken}")
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .build()
            } else {
                request.newBuilder()
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .build()
            }
            return chain.proceed(newRequest)
        }
    }

    private class RefreshTokenInterceptor(
        private val sessionStore: AuthSessionStore,
        private val authRepository: AuthRepository,
        private val onSessionInvalidated: (() -> Unit)?,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val response = chain.proceed(chain.request())
            if (response.code != 401) return response
            val session = sessionStore.read() ?: return response
            response.close()
            val refreshResult = authRepository.refresh(session.refreshToken)
            return refreshResult.fold(
                onSuccess = { newSession ->
                    sessionStore.save(newSession)
                    val newRequest = chain.request().newBuilder()
                        .removeHeader("Authorization")
                        .header(
                            "Authorization",
                            "${newSession.tokenType.ifBlank { "Bearer" }} ${newSession.accessToken}",
                        )
                        .build()
                    chain.proceed(newRequest)
                },
                onFailure = {
                    sessionStore.clear()
                    onSessionInvalidated?.invoke()
                    val unauthRequest = chain.request().newBuilder()
                        .removeHeader("Authorization")
                        .build()
                    chain.proceed(unauthRequest)
                },
            )
        }
    }

    companion object {
        private const val USER_AGENT = "FashAndroid/1.0"
    }
}
