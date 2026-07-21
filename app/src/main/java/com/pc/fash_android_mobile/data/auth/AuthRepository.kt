package com.pc.fash_android_mobile.data.auth

import android.util.Log
import com.pc.fash_android_mobile.BuildConfig
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.http.CoreServiceErrors
import com.pc.fash_android_mobile.network.FASH_HTTP_USER_AGENT
import com.pc.fash_android_mobile.util.ClientIpAddress
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Auth-service HTTP client — URLs built with [AppEnvironment.authServicePath] (base `AUTH_SERVICE_BASE_URL`
 * + optional `{vi|en}/` when `AUTH_API_USE_LANGUAGE_PREFIX` + paths from env).
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
            .put("client_channel", "fash_android_app")
            .toString()
        val body = postJsonBody(url, json)
        parseLoginResponse(body)
    }

    /**
     * Sends OTP to [email]. Returns [Result] of **is_new_user** from JSON when present:
     * `true` → show onboarding progress on the OTP screen; `false` → returning user (hide bar).
     * Empty body, parse errors, or missing key default to **false** so existing users are not
     * shown the onboarding strip unless the API explicitly sets `is_new_user: true`.
     */
    fun requestEmailOtp(email: String): Result<Boolean> = runCatching {
        val path = AppEnvironment.authOtpRequestPath.trim().trimStart('/')
        val url = AppEnvironment.authServicePath(path)
        val json = JSONObject()
            .put("email", email.trim())
            .put("application_id", AppEnvironment.authApplicationId.trim())
            .put("client_channel", "fash_android_app")
            .toString()
        val body = postJsonBody(url, json)
        if (body.isBlank()) return@runCatching false
        runCatching {
            val o = JSONObject(body)
            o.optBoolean("is_new_user", false)
        }.getOrElse { false }
    }

    fun verifyEmailOtp(email: String, otp: String): Result<AuthSession> = runCatching {
        val path = AppEnvironment.authOtpVerifyPath.trim().trimStart('/')
        val url = AppEnvironment.authServicePath(path)
        val json = JSONObject()
            .put("email", email.trim())
            .put("otp", otp.trim())
            .put("application_id", AppEnvironment.authApplicationId.trim())
            .put("client_channel", "fash_android_app")
            .toString()
        val body = postJsonBody(url, json)
        parseLoginResponse(body)
    }

    /** [provider] e.g. `google`, `facebook`. [providerToken] is ID token (Google) or access token (Facebook). */
    fun socialLogin(provider: String, providerToken: String): Result<AuthSession> = runCatching {
        val path = AppEnvironment.authSocialLoginPath.trim().trimStart('/')
        val json = JSONObject()
            .put("provider", provider.trim().lowercase())
            .put("provider_token", providerToken.trim())
            .put("application_id", AppEnvironment.authApplicationId.trim())
            .put("client_channel", "fash_android_app")
            .toString()
        var lastError: Exception? = null
        for (url in AppEnvironment.authServiceCandidateUrls(path)) {
            try {
                return@runCatching parseLoginResponse(postJsonBody(url, json))
            } catch (e: Exception) {
                lastError = e
                if (BuildConfig.DEBUG) {
                    val code = (e as? AuthHttpException)?.errorCode
                    Log.w(TAG, "social-login failed url=$url code=$code http=${(e as? AuthHttpException)?.httpCode}", e)
                }
                if (e is AuthHttpException && e.httpCode == 404) continue
                throw e
            }
        }
        throw lastError ?: IllegalStateException("social-login failed")
    }

    /**
     * `POST` refresh body per core-service contract. Call sites that may run concurrently (HTTP 401,
     * WebSocket, FCM) should use [AuthTokenRefreshCoordinator.refreshIfStillCurrent] so only one refresh
     * runs at a time when the backend rotates refresh tokens.
     */
    fun refresh(refreshToken: String): Result<AuthSession> = runCatching {
        val rel = AppEnvironment.authRefreshPath.trim().trimStart('/')
        val url = AppEnvironment.authServicePath(rel)
        val json = JSONObject()
            .put("application_id", AppEnvironment.authApplicationId.trim())
            .put("ip_address", ClientIpAddress.localIpv4OrEmpty())
            .put("refresh_token", refreshToken.trim())
            .put("user_agent", FASH_HTTP_USER_AGENT)
            .toString()
        val body = try {
            postJsonBody(url, json)
        } catch (e: Exception) {
            logRefreshTokenResponseFailure(url, e)
            throw e
        }
        logRefreshTokenResponseSuccess(body)
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
    fun registerFcm(
        accessToken: String,
        fcmToken: String,
        devicePlatform: String = "android",
        clientLocale: String? = null,
    ): Result<Unit> = runCatching {
        if (fcmToken.isBlank()) return@runCatching
        val path = AppEnvironment.authFcmRegisterPath.trim().trimStart('/')
        val url = AppEnvironment.authServicePath(path)
        val json = JSONObject()
            .put("fcm_token", fcmToken.trim())
            .put("device_platform", devicePlatform)
        clientLocale?.trim()?.takeIf { it.isNotEmpty() }?.let { json.put("client_locale", it) }
        val payload = json.toString()
        val request = Request.Builder()
            .url(url)
            .post(payload.toRequestBody(JSON_MEDIA))
            .header("Accept", "application/json")
            .header("Authorization", "Bearer ${accessToken.trim()}")
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw authHttpException(response, body)
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
                throw authHttpException(response, body)
            }
        }
    }

    private fun postJsonBody(url: String, json: String): String {
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(JSON_MEDIA))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json; charset=utf-8")
            .header("User-Agent", FASH_HTTP_USER_AGENT)
            .build()
        return client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw authHttpException(response, body)
            }
            body
        }
    }

    private fun authHttpException(response: okhttp3.Response, body: String): AuthHttpException {
        val parsed = CoreServiceErrors.parse(
            httpCode = response.code,
            body = body,
            retryAfterHeader = response.header("Retry-After"),
        )
        return AuthHttpException(parsed.httpCode, parsed.message, parsed)
    }

    private fun logRefreshTokenResponseSuccess(rawBody: String) {
        if (BuildConfig.DEBUG) {
            val hint = runCatching {
                val o = JSONObject(rawBody)
                val exp = o.optLong("expires_in", -1L)
                "expires_in=$exp"
            }.getOrElse { "parse skipped" }
            Log.d(TAG, "refresh token HTTP 200 ($hint)")
        }
    }

    private fun logRefreshTokenResponseFailure(url: String, e: Exception) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "refresh token request failed url=$url", e)
        }
    }

    private fun parseLoginResponse(json: String): AuthSession {
        val root = JSONObject(json.trim())
        val o = when {
            root.has("data") && root.get("data") is JSONObject -> root.getJSONObject("data")
            else -> root
        }
        return AuthSession(
            accessToken = o.getString("access_token"),
            refreshToken = o.getString("refresh_token"),
            tokenType = o.optString("token_type", "Bearer").ifBlank { "Bearer" },
            expiresInSeconds = parseExpiresIn(o),
            isNewUser = o.optBoolean("is_new_user", false),
            userId = o.optString("user_id", "").takeIf { it.isNotBlank() },
            unreadCount = o.optLong("unread_count", 0L),
        )
    }

    private fun parseExpiresIn(o: JSONObject): Long = runCatching {
        if (!o.has("expires_in")) return@runCatching 0L
        val v = o.get("expires_in")
        when (v) {
            is Number -> v.toLong()
            is String -> v.trim().toLongOrNull() ?: 0L
            else -> 0L
        }.coerceAtLeast(0L)
    }.getOrDefault(0L)

    companion object {
        private const val TAG = "AuthRepository"

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
