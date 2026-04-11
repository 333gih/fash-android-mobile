package com.pc.fash_android_mobile.data.auth

import com.pc.fash_android_mobile.network.SecuredApiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central auth state: single source of truth for session validity.
 *
 * [sessionExpiredMessage] emits a non-null human-readable string whenever the session
 * is force-cleared by the server (e.g. expired refresh token).  It is null for
 * intentional logouts.  Consumers should call [clearSessionExpiredMessage] after
 * displaying it.
 */
class AppAuthManager(
    val sessionStore: AuthSessionStore,
    val authRepository: AuthRepository,
) {

    /**
     * Starts false; [hydrateInitialAuthFromStore] runs on a background thread shortly after
     * process start (non-blocking `Application.onCreate`) so EncryptedSharedPreferences never
     * opens on the main thread.
     */
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    /** Cold start: set from background after [AuthSessionStore.read] on IO. */
    fun hydrateInitialAuthFromStore(hasSession: Boolean) {
        _isAuthenticated.value = hasSession
    }

    private val _sessionExpiredMessage = MutableStateFlow<String?>(null)
    val sessionExpiredMessage: StateFlow<String?> = _sessionExpiredMessage.asStateFlow()

    /**
     * Returns a secured OkHttpClient factory.
     * [onSessionInvalidated] receives an optional reason string and is called from
     * a background (OkHttp) thread when the refresh token is expired/invalid.
     */
    fun createSecuringClient(
        onSessionInvalidated: ((String?) -> Unit)? = null,
    ): SecuredApiClient = SecuredApiClient(sessionStore, authRepository, onSessionInvalidated)

    /** Call after a successful login or token refresh. */
    fun onSessionSaved() {
        _isAuthenticated.value = true
    }

    /**
     * Call after logout or when the server invalidates the session.
     * [reason] is shown to the user only when the session was force-expired
     * (i.e. not an intentional logout).
     */
    fun onSessionCleared(reason: String? = null) {
        if (reason != null) {
            _sessionExpiredMessage.value = reason
        }
        _isAuthenticated.value = false
    }

    /** Call from the UI after the session-expired message has been shown. */
    fun clearSessionExpiredMessage() {
        _sessionExpiredMessage.value = null
    }

    /**
     * Cold-start validation: refresh tokens if the access token is likely expired.
     * When the access token is still within [AuthSession.expiresInSeconds] of [AuthSessionStore]
     * issue time, skips the network call so a flaky refresh or misclassified 4xx cannot log
     * the user out. On transient refresh failures, keeps the session (retries a few times).
     * Clears the session on definitive OAuth-style failures (400/401/403) or refresh
     * [java.net.SocketTimeoutException] (OkHttp read/connect timeout, e.g. 20s).
     */
    suspend fun validateOrClearSession(): Boolean {
        val session = sessionStore.read() ?: return false
        if (isAccessTokenLikelyValid(session)) {
            _isAuthenticated.value = true
            return true
        }
        for (attempt in 0 until REFRESH_ATTEMPTS) {
            val snapshot = sessionStore.read() ?: return false
            val result = AuthTokenRefreshCoordinator.refreshIfStillCurrent(
                sessionStore,
                authRepository,
                snapshot.accessToken,
            )
            result.getOrNull()?.let {
                onSessionSaved()
                return true
            }
            val err = result.exceptionOrNull()!!
            if (err.isTransientRefreshFailure()) {
                if (attempt < REFRESH_ATTEMPTS - 1) {
                    delay(400L * (attempt + 1))
                    continue
                }
                _isAuthenticated.value = sessionStore.read() != null
                return true
            }
            sessionStore.clear()
            _isAuthenticated.value = false
            return false
        }
        return true
    }

    /** True when stored issue time + expiry suggests the access JWT is still valid. */
    private fun isAccessTokenLikelyValid(session: AuthSession): Boolean {
        if (session.expiresInSeconds <= 0L) return false
        val issued = sessionStore.getIssuedAtMillis()
        if (issued <= 0L) return false
        val expMs = issued + session.expiresInSeconds * 1000L
        return System.currentTimeMillis() < expMs - ACCESS_TOKEN_REFRESH_SKEW_MS
    }

    private companion object {
        private const val REFRESH_ATTEMPTS = 3
        /** Refresh slightly before real expiry so API calls are unlikely to see 401 first. */
        private const val ACCESS_TOKEN_REFRESH_SKEW_MS = 60_000L
    }

    /**
     * Calls the auth service to revoke the session, then **always** clears local tokens and sets
     * [isAuthenticated] to false so the UI returns to login even if the HTTP call fails (offline,
     * 5xx, or misconfigured path).
     */
    fun logout(accessToken: String): Result<Unit> {
        val result = authRepository.logout(accessToken)
        sessionStore.clear()
        _isAuthenticated.value = false
        return result
    }

    fun logoutAll(accessToken: String): Result<Unit> {
        val result = authRepository.logoutAll(accessToken)
        sessionStore.clear()
        _isAuthenticated.value = false
        return result
    }
}
