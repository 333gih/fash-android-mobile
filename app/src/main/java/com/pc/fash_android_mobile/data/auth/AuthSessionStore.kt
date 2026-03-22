package com.pc.fash_android_mobile.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted storage for auth tokens. Uses Android Keystore-backed encryption.
 * Never log or expose stored values.
 */
class AuthSessionStore(context: Context) {

    private val prefs: SharedPreferences = createEncryptedPrefs(context)

    fun save(session: AuthSession) {
        prefs.edit()
            .putString(KEY_ACCESS, session.accessToken)
            .putString(KEY_REFRESH, session.refreshToken)
            .putString(KEY_TYPE, session.tokenType)
            .putLong(KEY_EXPIRES, session.expiresInSeconds)
            .putBoolean(KEY_IS_NEW_USER, session.isNewUser)
            .putString(KEY_USER_ID, session.userId)
            .putLong(KEY_UNREAD_COUNT, session.unreadCount)
            .putLong(KEY_ISSUED_AT, System.currentTimeMillis())
            .apply()
    }

    fun read(): AuthSession? {
        val access = prefs.getString(KEY_ACCESS, null)?.trim().orEmpty()
        val refresh = prefs.getString(KEY_REFRESH, null)?.trim().orEmpty()
        if (access.isEmpty() || refresh.isEmpty()) return null
        return AuthSession(
            accessToken = access,
            refreshToken = refresh,
            tokenType = prefs.getString(KEY_TYPE, "Bearer") ?: "Bearer",
            expiresInSeconds = prefs.getLong(KEY_EXPIRES, 0L),
            isNewUser = prefs.getBoolean(KEY_IS_NEW_USER, false),
            userId = prefs.getString(KEY_USER_ID, null)?.takeIf { it.isNotBlank() },
            unreadCount = prefs.getLong(KEY_UNREAD_COUNT, 0L),
        )
    }

    fun getIssuedAtMillis(): Long = prefs.getLong(KEY_ISSUED_AT, 0L)

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        private const val PREFS_NAME = "fash_auth_session"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_TYPE = "token_type"
        private const val KEY_EXPIRES = "expires_in"
        private const val KEY_IS_NEW_USER = "is_new_user"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_UNREAD_COUNT = "unread_count"
        private const val KEY_ISSUED_AT = "issued_at"

        private fun createEncryptedPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context.applicationContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }
}
