package com.pc.fash_android_mobile.data.locale

import android.util.Log
import com.pc.fash_android_mobile.BuildConfig
import com.pc.fash_android_mobile.data.auth.AuthSessionStore
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Keeps [profiles.preferred_locale] in sync with the in-app language when the user is signed in.
 * Also invoked on cold start so persisted locale matches the server (idempotent).
 */
class PreferredLocaleSync(
    private val userRepository: UserRepository,
    private val sessionStore: AuthSessionStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun syncIfSession(locale: String) {
        scope.launch { syncIfSessionSuspend(locale) }
    }

    suspend fun syncIfSessionSuspend(locale: String) {
        if (sessionStore.read() == null) return
        val tag = if (locale.startsWith(AppLocale.TAG_EN)) AppLocale.TAG_EN else AppLocale.TAG_VI
        userRepository.syncPreferredLocale(tag).onFailure { e ->
            if (BuildConfig.DEBUG) Log.w(TAG, "syncPreferredLocale failed: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "PreferredLocaleSync"
    }
}
