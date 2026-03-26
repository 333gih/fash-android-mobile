package com.pc.fash_android_mobile.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted storage for auth tokens. Uses Android Keystore-backed AES-256-GCM encryption.
 *
 * ### Keystore invalidation recovery
 * Android can invalidate the Keystore key after:
 * - Device PIN / pattern / password change or removal
 * - New biometric enrollment or removal
 * - OS upgrade or factory reset
 * - App reinstall on some devices
 *
 * When this happens, any read or write throws `KeyStoreException: Signature/MAC
 * verification failed`. We recover by:
 * 1. Deleting the corrupted encrypted prefs file
 * 2. Recreating a fresh encrypted store with a new key
 * 3. Returning `null` from [read] so callers (e.g. [AppAuthManager]) treat the
 *    session as missing and redirect the user to the login screen
 *
 * Never log or expose stored token values.
 */
class AuthSessionStore(private val context: Context) {

    // `var` because recovery may replace the prefs instance
    private var prefs: SharedPreferences = createPrefsOrRecover()

    // ── Public API ────────────────────────────────────────────────────────────

    fun save(session: AuthSession) {
        try {
            // Use commit() (synchronous) so tokens are on disk before the process can be killed
            // (e.g. user swipes the app away immediately after login). apply() is async and can lose data.
            val ok = prefs.edit()
                .putString(KEY_ACCESS, session.accessToken)
                .putString(KEY_REFRESH, session.refreshToken)
                .putString(KEY_TYPE, session.tokenType)
                .putLong(KEY_EXPIRES, session.expiresInSeconds)
                .putBoolean(KEY_IS_NEW_USER, session.isNewUser)
                .putString(KEY_USER_ID, session.userId)
                .putLong(KEY_UNREAD_COUNT, session.unreadCount)
                .putLong(KEY_ISSUED_AT, System.currentTimeMillis())
                .commit()
            if (!ok) {
                Log.w(TAG, "save: SharedPreferences.commit() returned false")
            }
        } catch (e: Exception) {
            Log.w(TAG, "save failed — Keystore key likely invalidated; wiping store", e)
            recoverAndReset()
        }
    }

    /**
     * Returns the stored session, or `null` if no session is present or the
     * Keystore key has been invalidated. In the latter case the corrupted store
     * is wiped automatically so the next save() can succeed.
     */
    fun read(): AuthSession? {
        return try {
            val access = prefs.getString(KEY_ACCESS, null)?.trim().orEmpty()
            val refresh = prefs.getString(KEY_REFRESH, null)?.trim().orEmpty()
            if (access.isEmpty() || refresh.isEmpty()) return null
            AuthSession(
                accessToken = access,
                refreshToken = refresh,
                tokenType = prefs.getString(KEY_TYPE, "Bearer") ?: "Bearer",
                expiresInSeconds = prefs.getLong(KEY_EXPIRES, 0L),
                isNewUser = prefs.getBoolean(KEY_IS_NEW_USER, false),
                userId = prefs.getString(KEY_USER_ID, null)?.takeIf { it.isNotBlank() },
                unreadCount = prefs.getLong(KEY_UNREAD_COUNT, 0L),
            )
        } catch (e: Exception) {
            // KeyStoreException: Signature/MAC verification failed
            // → key was invalidated; wipe corrupted data, user must re-login
            Log.w(TAG, "read failed — Keystore key likely invalidated; wiping store", e)
            recoverAndReset()
            null
        }
    }

    fun getIssuedAtMillis(): Long {
        return try {
            prefs.getLong(KEY_ISSUED_AT, 0L)
        } catch (e: Exception) {
            Log.w(TAG, "getIssuedAtMillis failed", e)
            recoverAndReset()
            0L
        }
    }

    fun clear() {
        try {
            if (!prefs.edit().clear().commit()) {
                Log.w(TAG, "clear: SharedPreferences.commit() returned false")
            }
        } catch (e: Exception) {
            Log.w(TAG, "clear failed — forcing full wipe", e)
            recoverAndReset()
        }
    }

    // ── Recovery ──────────────────────────────────────────────────────────────

    /**
     * Wipes the corrupted encrypted prefs file and recreates a fresh store.
     * After this call [prefs] points to an empty, freshly-keyed store.
     */
    private fun recoverAndReset() {
        // Best-effort commit clear before deleting the file
        runCatching { prefs.edit().clear().commit() }
        deletePrefsFile(context, PREFS_NAME)
        prefs = createPrefsOrRecover(afterReset = true)
    }

    // ── Creation helpers ──────────────────────────────────────────────────────

    /**
     * Tries to open (or create) the encrypted prefs.
     * On failure deletes the file (first attempt only) and retries once.
     * If it still fails, falls back to an empty plain prefs so the app stays
     * launchable — tokens won't be persisted but no crash occurs.
     */
    private fun createPrefsOrRecover(afterReset: Boolean = false): SharedPreferences {
        return try {
            createEncryptedPrefs(context)
        } catch (e: Exception) {
            Log.w(TAG, "EncryptedSharedPreferences init failed (afterReset=$afterReset)", e)
            if (!afterReset) {
                deletePrefsFile(context, PREFS_NAME)
                try {
                    createEncryptedPrefs(context)
                } catch (e2: Exception) {
                    Log.e(TAG, "EncryptedSharedPreferences still failing after reset — using plain empty prefs", e2)
                    emptyPlainPrefs(context)
                }
            } else {
                emptyPlainPrefs(context)
            }
        }
    }

    // ── Companion ─────────────────────────────────────────────────────────────

    private companion object {
        private const val TAG = "AuthSessionStore"
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

        /**
         * Returns an empty, cleared plain SharedPreferences used as a last resort
         * when Keystore is completely unavailable (e.g. corrupted hardware).
         * No tokens are stored — the user will be redirected to login.
         */
        private fun emptyPlainPrefs(context: Context): SharedPreferences =
            context.applicationContext
                .getSharedPreferences("${PREFS_NAME}_plain_fallback", Context.MODE_PRIVATE)
                .also { it.edit().clear().apply() }

        /**
         * Deletes the encrypted prefs XML file from the filesystem.
         * [Context.deleteSharedPreferences] is available on API 24+ (minSdk = 24).
         */
        private fun deletePrefsFile(context: Context, name: String) {
            try {
                context.applicationContext.deleteSharedPreferences(name)
            } catch (e: Exception) {
                // Manual fallback for edge cases
                runCatching {
                    val dir = context.applicationContext.filesDir.parentFile
                    java.io.File(dir, "shared_prefs/$name.xml").delete()
                }
            }
        }
    }
}
