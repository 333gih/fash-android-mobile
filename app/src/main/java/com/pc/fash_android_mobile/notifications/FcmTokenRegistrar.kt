package com.pc.fash_android_mobile.notifications

import com.google.firebase.messaging.FirebaseMessaging
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
        val session = sessionStore.read() ?: return@withContext
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
            ?: return@withContext
        authRepository.registerFcm(session.accessToken, token)
    }

    /** Called from [FashFirebaseMessagingService.onNewToken] (non-suspend). */
    fun registerTokenAsync(fcmToken: String) {
        if (fcmToken.isBlank()) return
        scope.launch {
            val session = sessionStore.read() ?: return@launch
            authRepository.registerFcm(session.accessToken, fcmToken)
        }
    }
}
