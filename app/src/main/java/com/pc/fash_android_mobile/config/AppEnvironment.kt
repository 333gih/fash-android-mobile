package com.pc.fash_android_mobile.config

import com.pc.fash_android_mobile.BuildConfig
import com.pc.fash_android_mobile.data.locale.AppLocale

/**
 * Typed access to values loaded from [env/dev.env] or [env/prod.env] at build time (via [BuildConfig]).
 * Build a variant: `./gradlew installDevDebug` or select the **dev** / **prod** product flavor in Android Studio.
 */
object AppEnvironment {

    /** Logical name from env file (e.g. `dev`, `prod`). */
    val environmentName: String
            get() = BuildConfig.ENVIRONMENT_NAME

    /** Product flavor of this build (`dev` or `prod`). */
    val flavor: String
            get() = BuildConfig.FLAVOR

    val isDev: Boolean get() = flavor == "dev"
    val isProd: Boolean get() = flavor == "prod"

    /** Final application id for this variant (includes `applicationIdSuffix` on dev). */
    val applicationId: String
            get() = BuildConfig.APPLICATION_ID

    /** Auth service base URL (trailing slash recommended). */
    val authServiceBaseUrl: String
            get() = BuildConfig.AUTH_SERVICE_BASE_URL

    /** General API base URL; defaults to auth URL in Gradle if omitted in env. */
    val apiBaseUrl: String
            get() = BuildConfig.API_BASE_URL

    /** Public OAuth / client id for this environment. */
    val authClientId: String
            get() = BuildConfig.AUTH_CLIENT_ID

    /**
     * Optional client secret — avoid non-empty values in VCS; prefer runtime secrets for production.
     */
    val authClientSecret: String
            get() = BuildConfig.AUTH_CLIENT_SECRET

    /** Relative path for email OTP POST (JSON `{ "email": "…" }`). */
    val authOtpRequestPath: String
            get() = BuildConfig.AUTH_OTP_REQUEST_PATH

    /** Relative path for OTP verify POST (JSON `{ "email": "…", "otp": "…" }`). */
    val authOtpVerifyPath: String
            get() = BuildConfig.AUTH_OTP_VERIFY_PATH

    /** Registered application UUID in auth-service `applications` table. */
    val authApplicationId: String
            get() = BuildConfig.AUTH_APPLICATION_ID

    val authSocialLoginPath: String
            get() = BuildConfig.AUTH_SOCIAL_LOGIN_PATH

    val authRefreshPath: String
            get() = BuildConfig.AUTH_REFRESH_PATH

    val authLogoutPath: String
            get() = BuildConfig.AUTH_LOGOUT_PATH

    val authLogoutAllPath: String
            get() = BuildConfig.AUTH_LOGOUT_ALL_PATH

    val authLoginPath: String
            get() = BuildConfig.AUTH_LOGIN_PATH

    val authFcmRegisterPath: String
            get() = BuildConfig.AUTH_FCM_REGISTER_PATH

    /**
     * OAuth **Web client** id (ends with `.apps.googleusercontent.com`) for [requestIdToken][com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder.requestIdToken].
     */
    val googleWebClientId: String
            get() = BuildConfig.GOOGLE_WEB_CLIENT_ID

    /**
     * From `POST_STEPS_RELAX_VALIDATION=true` in env — when true, listing flow **Next** ignores
     * step 1/2 draft validation; when false (default), behavior matches strict checks in [com.pc.fash_android_mobile.ui.post.CreateListingDraft].
     */
    val postStepsRelaxValidation: Boolean
        get() = BuildConfig.POST_STEPS_RELAX_VALIDATION

    /** common-service root (see ANDROID_API_INTEGRATION.md); no language prefix. */
    val commonServiceBaseUrl: String
        get() = BuildConfig.COMMON_SERVICE_BASE_URL.trimEnd('/')

    /**
     * Path under common-service: [relativeApiPath] is e.g. `api/v1/addresses/tree`.
     */
    fun commonServicePath(relativeApiPath: String): String {
        val base = commonServiceBaseUrl
        val rel = relativeApiPath.trimStart('/')
        return "$base/$rel"
    }

    /** `GET /health` — unauthenticated; path is service root, not under `/api/v1`. */
    fun commonServiceHealthUrl(): String = "$commonServiceBaseUrl/health"

    fun authServicePath(relativePath: String): String {
        val base = authServiceBaseUrl.trimEnd('/')
        val rel = relativePath.trimStart('/')
        return "$base/$rel"
    }

    /**
     * Core-service API (same host as [apiBaseUrl]). When [BuildConfig.CORE_API_USE_LANGUAGE_PREFIX] is true
     * (from `CORE_API_USE_LANGUAGE_PREFIX=true` in env), paths are `{base}/{vi|en}/{relativePath}` e.g. `.../vi/api/v1/...`.
     * Auth endpoints use [authServicePath] and are not prefixed.
     */
    fun apiPath(relativePath: String): String {
        val base = apiBaseUrl.trimEnd('/')
        val rel = relativePath.trimStart('/')
        if (!BuildConfig.CORE_API_USE_LANGUAGE_PREFIX) {
            return "$base/$rel"
        }
        val lang = AppLocale.coreApiPathSegment()
        return "$base/$lang/$rel"
    }
}
