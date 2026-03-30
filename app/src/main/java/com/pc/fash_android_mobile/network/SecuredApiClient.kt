package com.pc.fash_android_mobile.network

import com.pc.fash_android_mobile.BuildConfig
import com.pc.fash_android_mobile.data.auth.AuthRepository
import com.pc.fash_android_mobile.data.auth.AuthSession
import com.pc.fash_android_mobile.data.auth.AuthSessionStore
import com.pc.fash_android_mobile.data.auth.isTransientRefreshFailure
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Provides a secured OkHttpClient that:
 * - Injects `Accept`, `User-Agent`, optional [BuildConfig.INTERNAL_SECRET] as `X-Internal-Secret`,
 *   user `Authorization` Bearer when logged in, or optional [BuildConfig.INTERNAL_SERVICE_BEARER_TOKEN]
 *   when not logged in (see ANDROID_API_INTEGRATION.md — prefer one auth method server-side).
 * - On 401: refreshes the access token (synchronized — only one refresh at a time)
 *   and retries the original request with the new token
 * - On refresh failure (expired/invalid refresh token): clears session,
 *   calls [onSessionInvalidated] with a reason string, then throws [IOException]
 *   so callers get a clean error instead of a pointless unauthenticated retry
 */
class SecuredApiClient(
    private val sessionStore: AuthSessionStore,
    private val authRepository: AuthRepository,
    private val onSessionInvalidated: ((String?) -> Unit)? = null,
) {

    fun createClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor(sessionStore))
        .addInterceptor(
            RefreshTokenInterceptor(sessionStore, authRepository, onSessionInvalidated),
        )
        .build()

    // ── Interceptor 1: injects the current Bearer token ─────────────────────
    private class AuthInterceptor(
        private val sessionStore: AuthSessionStore,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val session = sessionStore.read()
            val base = chain.request().newBuilder()
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
            val internalSecret = BuildConfig.INTERNAL_SECRET.trim()
            if (internalSecret.isNotEmpty()) {
                base.header("X-Internal-Secret", internalSecret)
            }
            if (session != null && session.accessToken.isNotBlank()) {
                base.header(
                    "Authorization",
                    "${session.tokenType.ifBlank { "Bearer" }} ${session.accessToken}",
                )
            } else {
                val serviceBearer = BuildConfig.INTERNAL_SERVICE_BEARER_TOKEN.trim()
                if (serviceBearer.isNotEmpty()) {
                    base.header("Authorization", "Bearer $serviceBearer")
                }
            }
            return chain.proceed(base.build())
        }
    }

    // ── Interceptor 2: handles 401 → refresh → retry ─────────────────────────
    private class RefreshTokenInterceptor(
        private val sessionStore: AuthSessionStore,
        private val authRepository: AuthRepository,
        private val onSessionInvalidated: ((String?) -> Unit)?,
    ) : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val response = chain.proceed(chain.request())
            if (response.code != 401) return response

            val staleSession = sessionStore.read()
            response.close()

            // No session at all → can't refresh
            if (staleSession == null) {
                throw IOException(SESSION_EXPIRED_MSG)
            }

            // Synchronize so concurrent 401s don't trigger multiple refresh calls.
            // chain.proceed() is intentionally called OUTSIDE the lock.
            val refreshed: AuthSession? = synchronized(REFRESH_LOCK) {
                val current = sessionStore.read()
                if (current != null && current.accessToken != staleSession.accessToken) {
                    // Another thread already refreshed while we were waiting for the lock.
                    return@synchronized current
                }
                authRepository.refresh(staleSession.refreshToken).fold(
                    onSuccess = { newSession ->
                        sessionStore.save(newSession)
                        newSession
                    },
                    onFailure = { t ->
                        if (t.isTransientRefreshFailure()) {
                            throw IOException("Transient token refresh failure", t)
                        }
                        sessionStore.clear()
                        onSessionInvalidated?.invoke(SESSION_EXPIRED_MSG)
                        null
                    },
                )
            }

            if (refreshed == null) {
                // Refresh token was expired/invalid — throw clean error (no useless retry)
                throw IOException(SESSION_EXPIRED_MSG)
            }

            // Retry original request with the new access token
            val retryRequest = chain.request().newBuilder()
                .removeHeader("Authorization")
                .header(
                    "Authorization",
                    "${refreshed.tokenType.ifBlank { "Bearer" }} ${refreshed.accessToken}",
                )
                .build()
            return chain.proceed(retryRequest)
        }

        companion object {
            private val REFRESH_LOCK = Any()
            const val SESSION_EXPIRED_MSG =
                "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
        }
    }

    companion object {
        private const val USER_AGENT = "FashAndroid/1.0"
    }
}
