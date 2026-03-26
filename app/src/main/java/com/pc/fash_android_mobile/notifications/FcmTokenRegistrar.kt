package com.pc.fash_android_mobile.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.pc.fash_android_mobile.BuildConfig
import com.pc.fash_android_mobile.data.auth.AuthRepository
import com.pc.fash_android_mobile.data.auth.AuthSessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Registers the device FCM token with [AuthRepository.registerFcm] after login and on token refresh.
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
        authRepository.registerFcm(session.accessToken, token).fold(
            onSuccess = { logD("registerCurrentTokenIfSession: backend OK") },
            onFailure = { e ->
                logE("registerCurrentTokenIfSession: backend failed — ${e.message}", e)
            },
        )
    }

    /** Called from [FashFirebaseMessagingService.onNewToken] (non-suspend). */
    fun registerTokenAsync(fcmToken: String) {
        if (fcmToken.isBlank()) return
        scope.launch {
            val session = sessionStore.read() ?: return@launch
            authRepository.registerFcm(session.accessToken, fcmToken).fold(
                onSuccess = { logD("registerTokenAsync: backend OK") },
                onFailure = { e -> logE("registerTokenAsync: backend failed — ${e.message}", e) },
            )
        }
    }

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
