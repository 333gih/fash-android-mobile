package com.pc.fash_android_mobile.data.auth

import com.pc.fash_android_mobile.BuildConfig
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.http.CoreServiceErrors
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Auth-service HTTP client (paths align with core-service/auth-service).
 * Unauthenticated: OTP, social, login, refresh. Authenticated: logout, logoutAll, FCM.
 */
class AuthRepository(
    private val client: OkHttpClient = defaultClient,
) {

    fun login(email: String, password: String): Result<AuthSession> = runCatching {
        val path = AppEnvironment.authLoginPath.trim().trimStart('/')
        val url = AppEnvironment.authServicePath(path)
        val json = JSONObject()
            .put("email", email.trim())
            .put("password", password)
            .put("application_id", AppEnvironment.authApplicationId.trim())
            .toString()
        val body = postJsonBody(url, json)
        parseLoginResponse(body)
    }

    fun requestEmailOtp(email: String): Result<Unit> = runCatching {
        val path = AppEnvironment.authOtpRequestPath.trim().trimStart('/')
        val url = AppEnvironment.authServicePath(path)
        val json = JSONObject()
            .put("email", email.trim())
            .put("application_id", AppEnvironment.authApplicationId.trim())
            .toString()
        postExpectSuccess(url, json)
    }

    fun verifyEmailOtp(email: String, otp: String): Result<AuthSession> = runCatching {
        val path = AppEnvironment.authOtpVerifyPath.trim().trimStart('/')
        val url = AppEnvironment.authServicePath(path)
        val json = JSONObject()
            .put("email", email.trim())
            .put("otp", otp.trim())
            .put("application_id", AppEnvironment.authApplicationId.trim())
            .toString()
        val body = postJsonBody(url, json)
        parseLoginResponse(body)
    }

    /** [provider] e.g. `google`, `facebook`. [providerToken] is ID token (Google) or access token (Facebook). */
    fun socialLogin(provider: String, providerToken: String): Result<AuthSession> = runCatching {
        val path = AppEnvironment.authSocialLoginPath.trim().trimStart('/')
        val url = AppEnvironment.authServicePath(path)
        val json = JSONObject()
            .put("provider", provider.trim().lowercase())
            .put("provider_token", providerToken.trim())
            .put("application_id", AppEnvironment.authApplicationId.trim())
            .toString()
        val body = postJsonBody(url, json)
        parseLoginResponse(body)
    }

    fun refresh(refreshToken: String): Result<AuthSession> = runCatching {
        val path = AppEnvironment.authRefreshPath.trim().trimStart('/')
        val url = AppEnvironment.authServicePath(path)
        val json = JSONObject()
            .put("refresh_token", refreshToken.trim())
            .put("application_id", AppEnvironment.authApplicationId.trim())
            .toString()
        val body = postJsonBody(url, json)
        parseLoginResponse(body)
    }

    /** Revokes the current session; [accessToken] is a Bearer access JWT. */
    fun logout(accessToken: String): Result<Unit> = runCatching {
        val path = AppEnvironment.authLogoutPath.trim().trimStart('/')
        val url = AppEnvironment.authServicePath(path)
        postWithBearer(url, accessToken)
    }

    /** Revokes all sessions for the user; [accessToken] is a Bearer access JWT. */
    fun logoutAll(accessToken: String): Result<Unit> = runCatching {
        val path = AppEnvironment.authLogoutAllPath.trim().trimStart('/')
        val url = AppEnvironment.authServicePath(path)
        postWithBearer(url, accessToken)
    }

    /** Registers FCM token for push notifications. Requires Bearer token. Ignores empty fcmToken. */
    fun registerFcm(accessToken: String, fcmToken: String, devicePlatform: String = "android"): Result<Unit> = runCatching {
        if (fcmToken.isBlank()) return@runCatching
        val path = AppEnvironment.authFcmRegisterPath.trim().trimStart('/')
        val url = AppEnvironment.authServicePath(path)
        val json = JSONObject()
            .put("fcm_token", fcmToken.trim())
            .put("device_platform", devicePlatform)
            .toString()
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(JSON_MEDIA))
            .header("Accept", "application/json")
            .header("Authorization", "Bearer ${accessToken.trim()}")
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AuthHttpException(response.code, CoreServiceErrors.parseErrorMessage(response.code, body))
            }
        }
    }

    private fun postWithBearer(url: String, accessToken: String) {
        val request = Request.Builder()
            .url(url)
            .post(ByteArray(0).toRequestBody(null))
            .header("Accept", "application/json")
            .header("Authorization", "Bearer ${accessToken.trim()}")
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AuthHttpException(response.code, CoreServiceErrors.parseErrorMessage(response.code, body))
            }
        }
    }

    private fun postExpectSuccess(url: String, json: String) {
        client.newCall(
            Request.Builder()
                .url(url)
                .post(json.toRequestBody(JSON_MEDIA))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json; charset=utf-8")
                .header("User-Agent", "FashAndroid/1.0")
                .build(),
        ).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AuthHttpException(response.code, CoreServiceErrors.parseErrorMessage(response.code, body))
            }
        }
    }

    private fun postJsonBody(url: String, json: String): String {
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(JSON_MEDIA))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json; charset=utf-8")
            .header("User-Agent", "FashAndroid/1.0")
            .build()
        return client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AuthHttpException(response.code, CoreServiceErrors.parseErrorMessage(response.code, body))
            }
            body
        }
    }

    private fun parseLoginResponse(json: String): AuthSession {
        val o = JSONObject(json)
        return AuthSession(
            accessToken = o.getString("access_token"),
            refreshToken = o.getString("refresh_token"),
            tokenType = o.optString("token_type", "Bearer").ifBlank { "Bearer" },
            expiresInSeconds = o.optLong("expires_in", 0L),
            isNewUser = o.optBoolean("is_new_user", false),
            userId = o.optString("user_id", "").takeIf { it.isNotBlank() },
            unreadCount = o.optLong("unread_count", 0L),
        )
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        private val defaultClient: OkHttpClient = run {
            val builder = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
            if (BuildConfig.DEBUG) {
                builder.addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
                )
            }
            builder.build()
        }
    }
}
