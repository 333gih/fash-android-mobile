package com.pc.fash_android_mobile.data.auth

/** Refresh ran while [AuthSessionStore.read] returned null (e.g. race with logout). Not a transient network error. */
internal class AuthSessionMissingException : Exception("No stored session for token refresh")

/**
 * Single global lock for access/refresh token rotation.
 *
 * All paths that call [AuthRepository.refresh] must go through [refreshIfStillCurrent] so that:
 * - Concurrent 401s (OkHttp, FCM, WebSocket) do not run parallel refresh calls with the same
 *   refresh token (many backends invalidate the old refresh on rotation).
 * - After one caller succeeds, others observe the new session and skip a redundant refresh.
 */
internal val AUTH_TOKEN_REFRESH_SYNC = Any()

object AuthTokenRefreshCoordinator {

    /**
     * Refreshes the session when the caller hit 401 (or cold start with expired access JWT).
     *
     * @param accessTokenWhenUnauthorized access token that was rejected (empty = always call refresh API)
     */
    fun refreshIfStillCurrent(
        sessionStore: AuthSessionStore,
        authRepository: AuthRepository,
        accessTokenWhenUnauthorized: String,
    ): Result<AuthSession> = synchronized(AUTH_TOKEN_REFRESH_SYNC) {
        val current = sessionStore.read()
            ?: return Result.failure(AuthSessionMissingException())
        val stale = accessTokenWhenUnauthorized.trim()
        if (stale.isNotEmpty() &&
            current.accessToken.isNotBlank() &&
            current.accessToken != stale
        ) {
            return Result.success(current)
        }
        val result = authRepository.refresh(current.refreshToken)
        result.onSuccess { sessionStore.save(it) }
        result
    }
}
