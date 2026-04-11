package com.pc.fash_android_mobile.network

import com.pc.fash_android_mobile.BuildConfig
import com.pc.fash_android_mobile.data.auth.AUTH_TOKEN_REFRESH_SYNC
import com.pc.fash_android_mobile.data.auth.AuthRepository
import com.pc.fash_android_mobile.data.auth.AuthSession
import com.pc.fash_android_mobile.data.auth.AuthSessionStore
import com.pc.fash_android_mobile.data.auth.AuthTokenRefreshCoordinator
import com.pc.fash_android_mobile.data.auth.isTransientRefreshFailure
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/** OkHttp request tag: this call already retried once after a token refresh (prevents refresh loops). */
private enum class AuthRetryAfterRefresh { Marker }

/**
 * Provides a secured OkHttpClient that:
 * - Injects `Accept`, `User-Agent`, optional [BuildConfig.INTERNAL_SECRET] as `X-Internal-Secret`,
 *   user `Authorization` Bearer when logged in, or optional [BuildConfig.INTERNAL_SERVICE_BEARER_TOKEN]
 *   when not logged in (see ANDROID_API_INTEGRATION.md — prefer one auth method server-side).
 * - On 401: refreshes the access token (synchronized — only one refresh at a time)
 *   and retries the original request with the new token
 * - On refresh failure (expired/invalid refresh token, or [java.net.SocketTimeoutException]):
 *   clears session, calls [onSessionInvalidated] with a reason string, then throws [IOException]
 *   so callers get a clean error instead of a pointless unauthenticated retry
 * - Retries the original request **once** after a successful refresh; a second 401 clears the session
 *   (avoids infinite refresh loops when the resource truly rejects the caller).
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
                .header("User-Agent", FASH_HTTP_USER_AGENT)
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
            val request = chain.request()
            val response = chain.proceed(request)
            if (response.code != 401) return response

            // Already retried once with a post-refresh token — do not loop (refresh again / hammer API).
            if (request.tag(AuthRetryAfterRefresh::class.java) != null) {
                response.close()
                synchronized(AUTH_TOKEN_REFRESH_SYNC) {
                    sessionStore.clear()
                    onSessionInvalidated?.invoke(SESSION_EXPIRED_MSG)
                }
                throw IOException(SESSION_EXPIRED_MSG)
            }

            val staleSession = sessionStore.read()
            response.close()

            // No session at all → can't refresh
            if (staleSession == null) {
                throw IOException(SESSION_EXPIRED_MSG)
            }

            val refreshResult = AuthTokenRefreshCoordinator.refreshIfStillCurrent(
                sessionStore,
                authRepository,
                staleSession.accessToken,
            )
            val refreshed: AuthSession? = refreshResult.fold(
                onSuccess = { it },
                onFailure = { t ->
                    if (t.isTransientRefreshFailure()) {
                        throw IOException("Transient token refresh failure", t)
                    }
                    sessionStore.clear()
                    onSessionInvalidated?.invoke(SESSION_EXPIRED_MSG)
                    null
                },
            )

            if (refreshed == null) {
                // Refresh token was expired/invalid — throw clean error (no useless retry)
                throw IOException(SESSION_EXPIRED_MSG)
            }

            // Retry original request with the new access token
            val retryRequest = request.newBuilder()
                .removeHeader("Authorization")
                .tag(AuthRetryAfterRefresh::class.java, AuthRetryAfterRefresh.Marker)
                .header(
                    "Authorization",
                    "${refreshed.tokenType.ifBlank { "Bearer" }} ${refreshed.accessToken}",
                )
                .build()
            return chain.proceed(retryRequest)
        }

        companion object {
            const val SESSION_EXPIRED_MSG =
                "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
        }
    }

}
