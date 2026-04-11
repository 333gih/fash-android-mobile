package com.pc.fash_android_mobile.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.pc.fash_android_mobile.BuildConfig
import com.pc.fash_android_mobile.data.auth.AuthHttpException
import com.pc.fash_android_mobile.data.auth.AuthRepository
import com.pc.fash_android_mobile.data.auth.AuthSession
import com.pc.fash_android_mobile.data.auth.AuthSessionStore
import com.pc.fash_android_mobile.data.auth.AuthTokenRefreshCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Registers the device FCM token with [AuthRepository.registerFcm] after login and on token refresh.
 * On **401** (expired access JWT), refreshes via [AuthRepository.refresh] and retries once.
 */
class FcmTokenRegistrar(
    private val authRepository: AuthRepository,
    private val sessionStore: AuthSessionStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Fetches the current token and registers with the backend if a session exists. */
    suspend fun registerCurrentTokenIfSession() = withContext(Dispatchers.IO) {
        val session = sessionStore.read() ?: run {
            logD("registerCurrentTokenIfSession: no session, skip")
            return@withContext
        }
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        if (token.isNullOrBlank()) {
            logW("registerCurrentTokenIfSession: FCM token is null (Firebase init / google-services?)")
            return@withContext
        }
        logD("registerCurrentTokenIfSession: got FCM token, calling API…")
        registerFcmWithOptionalRefresh(session, token)
    }

    /** Called from [FashFirebaseMessagingService.onNewToken] (non-suspend). */
    fun registerTokenAsync(fcmToken: String) {
        if (fcmToken.isBlank()) return
        scope.launch {
            val session = sessionStore.read() ?: return@launch
            registerFcmWithOptionalRefresh(session, fcmToken)
        }
    }

    /**
     * Registers FCM; on **401** refreshes session and retries once (same pattern as [com.pc.fash_android_mobile.data.auth.AppAuthManager.validateOrClearSession]).
     */
    private suspend fun registerFcmWithOptionalRefresh(session: AuthSession, fcmToken: String) {
        val first = authRepository.registerFcm(session.accessToken, fcmToken)
        if (first.isSuccess) {
            logD("registerFcm: backend OK")
            return
        }
        val err = first.exceptionOrNull() ?: run {
            logW("registerFcm: unknown failure")
            return
        }
        if (!isUnauthorized(err)) {
            logE("registerFcm: backend failed — ${err.message}", err)
            return
        }
        val newSession = AuthTokenRefreshCoordinator.refreshIfStillCurrent(
            sessionStore,
            authRepository,
            session.accessToken,
        ).getOrElse {
            logW("registerFcm: access token expired; refresh failed — ${it.message}")
            return
        }
        val second = authRepository.registerFcm(newSession.accessToken, fcmToken)
        second.fold(
            onSuccess = { logD("registerFcm: backend OK after token refresh") },
            onFailure = { e ->
                if (isUnauthorized(e)) {
                    logW("registerFcm: still 401 after refresh — ${e.message}")
                } else {
                    logE("registerFcm: failed after refresh — ${e.message}", e)
                }
            },
        )
    }

    private fun isUnauthorized(e: Throwable): Boolean =
        e is AuthHttpException && e.httpCode == 401

    private fun logD(msg: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, msg)
    }

    private fun logW(msg: String) {
        if (BuildConfig.DEBUG) Log.w(TAG, msg)
    }

    private fun logE(msg: String, t: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.e(TAG, msg, t)
    }

    companion object {
        private const val TAG = "FcmTokenRegistrar"
    }
}
