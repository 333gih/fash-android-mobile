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

    /** `POST` — relative to [authServicePath], e.g. `api/v1/auth/change-password`. */
    val authChangePasswordPath: String
        get() = BuildConfig.AUTH_CHANGE_PASSWORD_PATH

    /** `GET` / `PATCH` — relative to [authServicePath], e.g. `api/v1/auth/me` (fash-auth-service identity). */
    val authMePath: String
        get() = BuildConfig.AUTH_ME_PATH

    /**
     * OAuth **Web client** id (ends with `.apps.googleusercontent.com`) for [requestIdToken][com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder.requestIdToken].
     */
    val googleWebClientId: String
            get() = BuildConfig.GOOGLE_WEB_CLIENT_ID

    /** From env `INTERNAL_SECRET` — sent as `X-Internal-Secret` when non-empty (dev or internal builds only). */
    val internalSecretConfigured: Boolean
        get() = BuildConfig.INTERNAL_SECRET.isNotBlank()

    /** From env `INTERNAL_SERVICE_BEARER_TOKEN` — used as Bearer when no user session. */
    val internalServiceBearerConfigured: Boolean
        get() = BuildConfig.INTERNAL_SERVICE_BEARER_TOKEN.isNotBlank()

    /**
     * Secured GET path (relative, passed to [apiPath]) for onboarding/home gate.
     * Core-service: `api/v1/users/me/setup-status` (override via env `CORE_USER_ACCESS_STATUS_PATH`).
     * Response JSON: `has_profile`, `aesthetic_tags_configured`, `onboarding_done`, `sizing_reference_completed`,
     * `can_access_home`, `next_step`, etc.
     */
    val userAccessStatusPath: String
        get() = BuildConfig.CORE_USER_ACCESS_STATUS_PATH

    /**
     * From env `SKIP_SIZING_REFERENCE_COMPLETED` — when true, sizing onboarding UI is hidden even if the
     * server reports `sizing_reference_completed: false`.
     */
    val skipSizingReferenceCompleted: Boolean
        get() = BuildConfig.SKIP_SIZING_REFERENCE_COMPLETED

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

    /**
     * Full URL under **auth-service** only (login, OTP, refresh, logout, FCM, change-password, …).
     *
     * Pattern when `AUTH_API_USE_LANGUAGE_PREFIX` is true (default follows `CORE_API_USE_LANGUAGE_PREFIX`):
     * `{AUTH_SERVICE_BASE_URL}/{vi|en}/api/v1/auth/...` — e.g. `http://host/auth-service/en/api/v1/auth/login`.
     *
     * [relativePath] is typically from env (`AUTH_LOGIN_PATH`, …) and starts with `api/v1/auth/`.
     */
    fun authServicePath(relativePath: String): String {
        val base = authServiceBaseUrl.trimEnd('/')
        val rel = relativePath.trimStart('/')
        if (!BuildConfig.AUTH_API_USE_LANGUAGE_PREFIX) {
            return "$base/$rel"
        }
        val lang = AppLocale.coreApiPathSegment()
        return "$base/$lang/$rel"
    }

    /** Auth URLs to try when locale-prefixed routing may be absent on the gateway (parity with [coreApiCandidateUrls]). */
    fun authServiceCandidateUrls(relativePath: String): List<String> {
        val path = relativePath.trim().trimStart('/')
        val withLocale = authServicePath(path)
        if (!BuildConfig.AUTH_API_USE_LANGUAGE_PREFIX) return listOf(withLocale)
        val withoutLocale = "${authServiceBaseUrl.trimEnd('/')}/$path"
        return if (withoutLocale == withLocale) listOf(withLocale) else listOf(withLocale, withoutLocale)
    }

    /**
     * Core-service API (same host as [apiBaseUrl]). When [BuildConfig.CORE_API_USE_LANGUAGE_PREFIX] is true
     * (from `CORE_API_USE_LANGUAGE_PREFIX=true` in env), paths are `{base}/{vi|en}/{relativePath}` e.g. `.../vi/api/v1/...`.
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

    /**
     * Core API without `{vi|en}/` — for stacks that only expose `/api/v1/...` directly under [apiBaseUrl].
     */
    fun apiPathWithoutLocale(relativePath: String): String {
        val base = apiBaseUrl.trimEnd('/')
        val rel = relativePath.trimStart('/')
        return "$base/$rel"
    }

    /**
     * Ordered URLs for a core relative path when the locale segment may be absent on the server (Traefik / routing mismatch).
     * Always tries [apiPath] first; when [BuildConfig.CORE_API_USE_LANGUAGE_PREFIX] is true, also tries [apiPathWithoutLocale] if different.
     */
    fun coreApiCandidateUrls(relativePath: String): List<String> {
        val path = relativePath.trim().trimStart('/')
        val withLocale = apiPath(path)
        if (!BuildConfig.CORE_API_USE_LANGUAGE_PREFIX) return listOf(withLocale)
        val withoutLocale = apiPathWithoutLocale(path)
        return if (withoutLocale == withLocale) listOf(withLocale) else listOf(withLocale, withoutLocale)
    }

    /**
     * Share / universal link for a listing ([BuildConfig.LISTING_SHARE_BASE_URL] + `/` + id).
     * This must be a **browser-resolvable** HTTPS URL on your marketing domain (not an API-only host), so chat apps
     * can show a preview page; the site should offer “Open in app” (see portal `app/p/l/[listingId]`).
     * Also use [com.pc.fash_android_mobile.deeplink.ListingDeepLinks.fashListingUri] for the `fash://` fallback.
     */
    fun listingShareUrl(listingId: String): String {
        val id = listingId.trim()
        if (id.isEmpty()) return ""
        val base = BuildConfig.LISTING_SHARE_BASE_URL.trimEnd('/')
        return "$base/$id"
    }

    /**
     * Share / universal link for a seller shop ([BuildConfig.LISTING_SHARE_BASE_URL] with `/p/l` → `/p/u` + username).
     * Pair with [com.pc.fash_android_mobile.deeplink.ProfileDeepLinks.fashProfileUri] in share text.
     */
    fun profileShareUrl(username: String): String {
        val handle = username.trim().removePrefix("@")
        if (handle.isEmpty()) return ""
        val base = BuildConfig.LISTING_SHARE_BASE_URL.trimEnd('/')
        val profileBase = if (base.endsWith("/p/l", ignoreCase = true)) {
            base.dropLast(1) + "u"
        } else {
            "$base/u"
        }
        return "$profileBase/$handle"
    }

    /**
     * Marketing / legal pages on the admin portal (see `fash-admin-portal-fe` publish routes).
     * [languageTag] should be [AppLocale.TAG_VI] or [AppLocale.TAG_EN] (e.g. from [AppLocale.currentTag]).
     */
    val legalPortalBaseUrl: String
        get() = BuildConfig.LEGAL_PORTAL_BASE_URL.trimEnd('/')

    fun legalTermsUrl(languageTag: String): String {
        val lang = if (languageTag.equals(AppLocale.TAG_EN, ignoreCase = true)) AppLocale.TAG_EN else AppLocale.TAG_VI
        return "${legalPortalBaseUrl}/$lang/terms"
    }

    fun legalPrivacyUrl(languageTag: String): String {
        val lang = if (languageTag.equals(AppLocale.TAG_EN, ignoreCase = true)) AppLocale.TAG_EN else AppLocale.TAG_VI
        return "${legalPortalBaseUrl}/$lang/privacy"
    }

    /** From env `IDENTITY_REVERIFY_URL` — optional Custom Tabs target for meetup identity re-verification. */
    val identityReverifyUrl: String
        get() = BuildConfig.IDENTITY_REVERIFY_URL.trim()

    /**
     * From env `SHIPPING` ([BuildConfig.SHIPPING_ENABLED]) — gates the Home “Đang giao” hub: live order list vs
     * “coming soon” placeholder until logistics tracking is rolled out for this environment.
     */
    val shippingEnabled: Boolean
        get() = BuildConfig.SHIPPING_ENABLED
}
