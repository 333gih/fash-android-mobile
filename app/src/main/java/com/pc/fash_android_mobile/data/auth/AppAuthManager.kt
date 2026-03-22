package com.pc.fash_android_mobile.data.auth

import com.pc.fash_android_mobile.network.SecuredApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central auth state: single source of truth for session validity.
 * Use [securingClient] for all API calls that require authentication.
 */
class AppAuthManager(
    val sessionStore: AuthSessionStore,
    val authRepository: AuthRepository,
) {

    private val _isAuthenticated = MutableStateFlow(sessionStore.read() != null)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    /** Use [createSecuringClient] for API calls requiring auth. Client injects Bearer token and refreshes on 401. */
    fun createSecuringClient(onSessionInvalidated: (() -> Unit)? = null): SecuredApiClient =
        SecuredApiClient(sessionStore, authRepository, onSessionInvalidated)

    /** Call after login/refresh. */
    fun onSessionSaved() {
        _isAuthenticated.value = true
    }

    /** Call after logout or session invalidated. */
    fun onSessionCleared() {
        _isAuthenticated.value = false
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
                onSessionCleared()
                false
            },
        )
    }

    fun logout(accessToken: String): Result<Unit> {
        return authRepository.logout(accessToken).also { result ->
            result.onSuccess {
                sessionStore.clear()
                onSessionCleared()
            }
        }
    }

    fun logoutAll(accessToken: String): Result<Unit> {
        return authRepository.logoutAll(accessToken).also { result ->
            result.onSuccess {
                sessionStore.clear()
                onSessionCleared()
            }
        }
    }
}
