package com.pc.fash_android_mobile.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.pc.fash_android_mobile.BuildConfig
import com.pc.fash_android_mobile.data.auth.AuthHttpException
import com.pc.fash_android_mobile.data.auth.AuthRepository
import com.pc.fash_android_mobile.data.auth.AuthSession
import com.pc.fash_android_mobile.data.auth.AuthSessionStore
import com.pc.fash_android_mobile.data.auth.AuthTokenRefreshCoordinator
import com.pc.fash_android_mobile.data.locale.AppLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Registers the device FCM token with [AuthRepository.registerFcm] after login and on token refresh.
 * On **401** (expired access JWT), refreshes via [AuthRepository.refresh] and retries once.
 */
class FcmTokenRegistrar(
    private val appContext: Context,
    private val authRepository: AuthRepository,
    private val sessionStore: AuthSessionStore,
    private val clientLocaleProvider: () -> String = { AppLocale.TAG_VI },
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val registerMutex = Mutex()

    /** Fetches the current token and registers with the backend if a session exists. */
    suspend fun registerCurrentTokenIfSession() = withContext(Dispatchers.IO) {
        val session = sessionStore.read()
        if (session == null) {
            logD("registerCurrentTokenIfSession: no session, skip")
            return@withContext
        }
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        if (token.isNullOrBlank() || token.length < MIN_FCM_TOKEN_LEN) {
            logW("registerCurrentTokenIfSession: FCM token missing or too short (Firebase init / google-services?)")
            registerPendingToken()
            return@withContext
        }
        logD("registerCurrentTokenIfSession: got FCM token len=${token.length}, calling API…")
        registerFcmWithOptionalRefresh(session, token)
        registerPendingToken()
    }

    /** Called from [FashFirebaseMessagingService.onNewToken] (non-suspend). */
    fun registerTokenAsync(fcmToken: String) {
        if (!isPlausibleFcmToken(fcmToken)) return
        scope.launch {
            val session = sessionStore.read()
            if (session == null) {
                stashPendingToken(fcmToken.trim())
                logD("registerTokenAsync: no session, stashed pending FCM token")
                return@launch
            }
            registerFcmWithOptionalRefresh(session, fcmToken.trim())
        }
    }

    suspend fun registerPendingToken() = withContext(Dispatchers.IO) {
        val pending = prefs().getString(PENDING_TOKEN_KEY, null)?.trim().orEmpty()
        if (pending.isEmpty()) return@withContext
        val session = sessionStore.read() ?: return@withContext
        logD("registerPendingToken: attempting stashed FCM token")
        registerFcmWithOptionalRefresh(session, pending)
    }

    fun clearOnLogout() {
        prefs().edit().remove(PENDING_TOKEN_KEY).apply()
        scope.launch {
            runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
                .onSuccess { logD("clearOnLogout: FCM token deleted") }
                .onFailure { logW("clearOnLogout: deleteToken failed — ${it.message}") }
        }
    }

    /**
     * Registers FCM; on **401** refreshes session and retries once (same pattern as [com.pc.fash_android_mobile.data.auth.AppAuthManager.validateOrClearSession]).
     */
    private suspend fun registerFcmWithOptionalRefresh(session: AuthSession, fcmToken: String) {
        if (!isPlausibleFcmToken(fcmToken)) {
            logW("registerFcm: rejected token (blank or too short)")
            return
        }
        registerMutex.withLock {
            val locale = clientLocaleProvider()
            val first = authRepository.registerFcm(session.accessToken, fcmToken, clientLocale = locale)
            if (first.isSuccess) {
                clearPendingToken()
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
            val second = authRepository.registerFcm(newSession.accessToken, fcmToken, clientLocale = locale)
            second.fold(
                onSuccess = {
                    clearPendingToken()
                    logD("registerFcm: backend OK after token refresh")
                },
                onFailure = { e ->
                    if (isUnauthorized(e)) {
                        logW("registerFcm: still 401 after refresh — ${e.message}")
                    } else {
                        logE("registerFcm: failed after refresh — ${e.message}", e)
                    }
                },
            )
        }
    }

    private fun stashPendingToken(token: String) {
        prefs().edit().putString(PENDING_TOKEN_KEY, token).apply()
    }

    private fun clearPendingToken() {
        prefs().edit().remove(PENDING_TOKEN_KEY).apply()
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun isUnauthorized(e: Throwable): Boolean =
        e is AuthHttpException && e.httpCode == 401

    private fun logD(msg: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, msg)
    }

    private fun logW(msg: String) {
        Log.w(TAG, msg)
    }

    private fun logE(msg: String, t: Throwable? = null) {
        Log.e(TAG, msg, t)
    }

    companion object {
        private const val TAG = "FcmTokenRegistrar"
        private const val PREFS = "fash.fcm"
        private const val PENDING_TOKEN_KEY = "pending_token"
        const val MIN_FCM_TOKEN_LEN = 100

        fun isPlausibleFcmToken(token: String): Boolean {
            val t = token.trim()
            if (t.length < MIN_FCM_TOKEN_LEN) return false
            if (t.length == 64 && t.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) return false
            return true
        }
    }
}
