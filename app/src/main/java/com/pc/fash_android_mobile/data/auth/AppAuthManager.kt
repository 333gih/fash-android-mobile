package com.pc.fash_android_mobile.data.auth

import com.pc.fash_android_mobile.network.SecuredApiClient
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

    private val _isAuthenticated = MutableStateFlow(sessionStore.read() != null)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

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

    /** Validate session on app start: refresh if needed, clear if invalid. Returns true if valid. */
    suspend fun validateOrClearSession(): Boolean {
        val session = sessionStore.read() ?: return false
        val refreshResult = authRepository.refresh(session.refreshToken)
        return refreshResult.fold(
            onSuccess = { newSession ->
                sessionStore.save(newSession)
                onSessionSaved()
                true
            },
            onFailure = {
                sessionStore.clear()
                // Silent clear on app start — no reason message needed
                _isAuthenticated.value = false
                false
            },
        )
    }

    fun logout(accessToken: String): Result<Unit> =
        authRepository.logout(accessToken).also { result ->
            result.onSuccess {
                sessionStore.clear()
                // Intentional logout — no reason message
                _isAuthenticated.value = false
            }
        }

    fun logoutAll(accessToken: String): Result<Unit> =
        authRepository.logoutAll(accessToken).also { result ->
            result.onSuccess {
                sessionStore.clear()
                _isAuthenticated.value = false
            }
        }
}
