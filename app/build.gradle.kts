import com.android.build.api.dsl.ApplicationProductFlavor
import java.io.File
import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

fun loadEnvFile(envFile: File): Map<String, String> {
    if (!envFile.exists()) {
        error(
            "Missing env file: ${envFile.absolutePath}\n" +
                "Add env/dev.env and env/prod.env at the project root.",
        )
    }
    return envFile.readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .associate { line ->
            val eq = line.indexOf('=')
            require(eq > 0) { "Invalid env line (expected KEY=value): $line" }
            val key = line.substring(0, eq).trim()
            val value = line.substring(eq + 1).trim()
            key to value
        }
}

fun buildConfigStringLiteral(raw: String): String =
    "\"${raw.replace("\\", "\\\\").replace("\"", "\\\"")}\""

/** Reads `gradle.properties`, `~/.gradle/gradle.properties`, or `local.properties` (same keys). */
fun org.gradle.api.Project.prop(key: String): String? {
    (findProperty(key) as String?)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    val lp = rootProject.file("local.properties")
    if (!lp.exists()) return null
    val p = Properties()
    lp.inputStream().use { p.load(it) }
    return p.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
}

/** Release keystore file if [FASH_RELEASE_STORE_FILE] points to an existing file (path relative to project root). */
val releaseKeystoreFile: File? =
    project.prop("FASH_RELEASE_STORE_FILE")
        ?.let { rootProject.file(it) }
        ?.takeIf { it.isFile }

fun ApplicationProductFlavor.injectFromEnv(env: Map<String, String>, flavorName: String) {
    fun envVal(key: String): String? = env[key]?.trim()?.takeIf { it.isNotEmpty() }
    fun envOrEmpty(key: String): String = env[key]?.trim() ?: ""

    val authBase = envVal("AUTH_SERVICE_BASE_URL")
        ?: error("AUTH_SERVICE_BASE_URL is required in env for flavor '$flavorName'")
    val apiBase = envVal("API_BASE_URL") ?: authBase
    val envName = envVal("ENVIRONMENT_NAME") ?: flavorName

    buildConfigField("String", "ENVIRONMENT_NAME", buildConfigStringLiteral(envName))
    buildConfigField("String", "AUTH_SERVICE_BASE_URL", buildConfigStringLiteral(authBase))
    buildConfigField("String", "API_BASE_URL", buildConfigStringLiteral(apiBase))
    val realtimeBaseFromEnv = envVal("REALTIME_BASE_URL") ?: ""
    buildConfigField("String", "REALTIME_BASE_URL", buildConfigStringLiteral(realtimeBaseFromEnv))
    buildConfigField(
        "String",
        "AUTH_CLIENT_ID",
        buildConfigStringLiteral(envVal("AUTH_CLIENT_ID") ?: error("AUTH_CLIENT_ID required for '$flavorName'")),
    )
    buildConfigField("String", "AUTH_CLIENT_SECRET", buildConfigStringLiteral(envOrEmpty("AUTH_CLIENT_SECRET")))
    val otpPath = envVal("AUTH_OTP_REQUEST_PATH") ?: "api/v1/auth/otp/request"
    buildConfigField("String", "AUTH_OTP_REQUEST_PATH", buildConfigStringLiteral(otpPath))
    val otpVerifyPath = envVal("AUTH_OTP_VERIFY_PATH") ?: "api/v1/auth/otp/verify"
    buildConfigField("String", "AUTH_OTP_VERIFY_PATH", buildConfigStringLiteral(otpVerifyPath))
    val applicationId = envVal("AUTH_APPLICATION_ID")
        ?: error("AUTH_APPLICATION_ID (UUID of the row in applications table) is required for flavor '$flavorName'")
    buildConfigField("String", "AUTH_APPLICATION_ID", buildConfigStringLiteral(applicationId))
    val socialPath = envVal("AUTH_SOCIAL_LOGIN_PATH") ?: "api/v1/auth/social-login"
    buildConfigField("String", "AUTH_SOCIAL_LOGIN_PATH", buildConfigStringLiteral(socialPath))
    val refreshPath = envVal("AUTH_REFRESH_PATH") ?: "api/v1/auth/refresh"
    buildConfigField("String", "AUTH_REFRESH_PATH", buildConfigStringLiteral(refreshPath))
    val logoutPath = envVal("AUTH_LOGOUT_PATH") ?: "api/v1/auth/logout"
    buildConfigField("String", "AUTH_LOGOUT_PATH", buildConfigStringLiteral(logoutPath))
    val logoutAllPath = envVal("AUTH_LOGOUT_ALL_PATH") ?: "api/v1/auth/logout-all"
    buildConfigField("String", "AUTH_LOGOUT_ALL_PATH", buildConfigStringLiteral(logoutAllPath))
    val loginPath = envVal("AUTH_LOGIN_PATH") ?: "api/v1/auth/login"
    buildConfigField("String", "AUTH_LOGIN_PATH", buildConfigStringLiteral(loginPath))
    val fcmRegisterPath = envVal("AUTH_FCM_REGISTER_PATH") ?: "api/v1/auth/fcm/register"
    buildConfigField("String", "AUTH_FCM_REGISTER_PATH", buildConfigStringLiteral(fcmRegisterPath))
    val changePasswordPath = envVal("AUTH_CHANGE_PASSWORD_PATH") ?: "api/v1/auth/change-password"
    buildConfigField("String", "AUTH_CHANGE_PASSWORD_PATH", buildConfigStringLiteral(changePasswordPath))
    val authMePath = envVal("AUTH_ME_PATH") ?: "api/v1/auth/me"
    buildConfigField("String", "AUTH_ME_PATH", buildConfigStringLiteral(authMePath))

    /**
     * When true, [com.pc.fash_android_mobile.config.AppEnvironment.authServicePath] becomes
     * `{AUTH_SERVICE_BASE_URL}/{vi|en}/api/v1/auth/...` (same locale segment as core).
     * When unset, falls back to `CORE_API_USE_LANGUAGE_PREFIX` so auth tracks core unless overridden.
     */
    val authApiUseLanguagePrefix = when {
        envVal("AUTH_API_USE_LANGUAGE_PREFIX") != null ->
            envVal("AUTH_API_USE_LANGUAGE_PREFIX")!!.equals("true", ignoreCase = true)
        else ->
            envVal("CORE_API_USE_LANGUAGE_PREFIX")?.equals("true", ignoreCase = true) == true
    }
    buildConfigField("boolean", "AUTH_API_USE_LANGUAGE_PREFIX", authApiUseLanguagePrefix.toString())
    val fbAppId = envOrEmpty("FACEBOOK_APP_ID")
    val fbClientToken = envOrEmpty("FACEBOOK_CLIENT_TOKEN")
    val facebookLoginEnabled =
        envVal("FACEBOOK_LOGIN_ENABLED")?.equals("true", ignoreCase = true) ?: true
    buildConfigField("boolean", "FACEBOOK_LOGIN_ENABLED", facebookLoginEnabled.toString())
    buildConfigField("String", "FACEBOOK_APP_ID", buildConfigStringLiteral(fbAppId))
    buildConfigField("String", "FACEBOOK_CLIENT_TOKEN", buildConfigStringLiteral(fbClientToken))
    resValue("string", "facebook_app_id", fbAppId.ifEmpty { "0" })
    resValue("string", "facebook_client_token", fbClientToken.ifEmpty { "unset" })
    val googleWebClientId = envOrEmpty("GOOGLE_WEB_CLIENT_ID")
    buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", buildConfigStringLiteral(googleWebClientId))

    val maxOffersPerConversation = envVal("CHAT_MAX_OFFERS_PER_CONVERSATION")?.toIntOrNull()
        ?: error(
            "CHAT_MAX_OFFERS_PER_CONVERSATION (positive integer) is required in env for flavor '$flavorName'",
        )
    require(maxOffersPerConversation >= 1) {
        "CHAT_MAX_OFFERS_PER_CONVERSATION must be >= 1 for flavor '$flavorName'"
    }
    buildConfigField("int", "CHAT_MAX_OFFERS_PER_CONVERSATION", maxOffersPerConversation.toString())

    /**
     * When true, create-listing step "Photos" requires at least one image (Next + submit validation).
     * When false, Next is enabled without photos (e.g. dev); API may still reject empty `image_urls`.
     */
    val postRequireListingImages =
        envVal("POST_REQUIRE_LISTING_IMAGES")?.equals("true", ignoreCase = true) ?: true
    buildConfigField("boolean", "POST_REQUIRE_LISTING_IMAGES", postRequireListingImages.toString())

    /**
     * Return URL for wallet apps after payment (must be HTTPS for most gateways).
     * Core-service should proxy [CorePaymentRepository] initiate and pass this to payment-service.
     */
    val paymentRedirectUrl = envVal("PAYMENT_REDIRECT_URL")
        ?: "https://fash.app/payment/callback"
    buildConfigField("String", "PAYMENT_REDIRECT_URL", buildConfigStringLiteral(paymentRedirectUrl))

    /**
     * Public HTTPS base for listing share links (no trailing slash); final URL is `{base}/{listingId}`.
     * Use your **marketing / website host** (same host users see in a browser), not `api-*` subdomains:
     * those URLs must serve an HTML bridge page (e.g. `fash-admin-portal-fe` `/p/l/[id]`) + Android App Links.
     * Host here must match [AndroidManifest.xml] `android:host`; path prefix must stay `/p/l` unless you change the manifest.
     */
    val listingShareBaseUrl = envVal("LISTING_SHARE_BASE_URL") ?: "https://fash.app/p/l"
    buildConfigField("String", "LISTING_SHARE_BASE_URL", buildConfigStringLiteral(listingShareBaseUrl))
    val listingShareHost = listingShareBaseUrl
        .removePrefix("https://")
        .removePrefix("http://")
        .substringBefore('/')
        .trim()
        .ifBlank { "fash.app" }
    if (listingShareHost.startsWith("api-", ignoreCase = true)) {
        logger.lifecycle(
            "[$flavorName] LISTING_SHARE_BASE_URL uses host '$listingShareHost'. Prefer your public site " +
                "(e.g. https://fashandcurious.com/p/l) so shared links open in a browser and App Links stay on-brand.",
        )
    }
    val listingSharePath =
        try {
            val uriString =
                if (listingShareBaseUrl.contains("://")) listingShareBaseUrl else "https://$listingShareBaseUrl"
            val uri = URI(uriString)
            (uri.path ?: "").trimEnd('/').ifBlank { "" }
        } catch (_: Exception) {
            ""
        }
    if (listingSharePath != "/p/l") {
        logger.lifecycle(
            "[$flavorName] LISTING_SHARE_BASE_URL path is '${listingSharePath.ifBlank { "/" }}' — " +
                "AndroidManifest intent-filters use pathPrefix /p/l/ only; mismatch breaks HTTPS deep links.",
        )
    }
    manifestPlaceholders["listingShareHost"] = listingShareHost
    /**
     * Core API path template for initiating gateway payment (single %s = order_id).
     * Example: api/v1/orders/%s/payments/initiate
     */
    val corePaymentInitiatePath = envOrEmpty("CORE_PAYMENT_INITIATE_PATH").ifBlank {
        "api/v1/orders/%s/payments/initiate"
    }
    buildConfigField("String", "CORE_PAYMENT_INITIATE_PATH", buildConfigStringLiteral(corePaymentInitiatePath))

    /**
     * When true, [com.pc.fash_android_mobile.config.AppEnvironment.apiPath] becomes
     * `{API_BASE_URL}/{vi|en}/api/...` (current app language).
     * Auth URLs under [AUTH_SERVICE_BASE_URL] use [com.pc.fash_android_mobile.config.AppEnvironment.authServicePath]
     * (`AUTH_API_USE_LANGUAGE_PREFIX`, defaults to matching this flag).
     */
    val coreApiUseLanguagePrefix =
        envVal("CORE_API_USE_LANGUAGE_PREFIX")?.equals("true", ignoreCase = true) == true
    buildConfigField("boolean", "CORE_API_USE_LANGUAGE_PREFIX", coreApiUseLanguagePrefix.toString())

    /** common-service base (GET catalog: addresses, brands, categories, tags, countries). Trailing slash optional. */
    val commonServiceBase =
        envVal("COMMON_SERVICE_BASE_URL")
            ?: "http://76.13.211.193/common-service/"
    buildConfigField("String", "COMMON_SERVICE_BASE_URL", buildConfigStringLiteral(commonServiceBase))

    /**
     * Secured GET under [API_BASE_URL] (via [AppEnvironment.apiPath]) returning onboarding/home gate flags.
     * Prefer core-service `GET .../users/me/setup-status` (same JSON as legacy `access-status` when present).
     */
    val userAccessStatusPath =
        envVal("CORE_USER_ACCESS_STATUS_PATH") ?: "api/v1/users/me/setup-status"
    buildConfigField("String", "CORE_USER_ACCESS_STATUS_PATH", buildConfigStringLiteral(userAccessStatusPath))

    /**
     * When true, do not show sizing-reference onboarding even if `sizing_reference_completed` is false
     * (dev / markets where sizing step is disabled).
     */
    val skipSizingReferenceCompleted =
        envVal("SKIP_SIZING_REFERENCE_COMPLETED")?.equals("true", ignoreCase = true) == true
    buildConfigField("boolean", "SKIP_SIZING_REFERENCE_COMPLETED", skipSizingReferenceCompleted.toString())

    // Guest browse attestation for GET /api/v1/public/* (core-service). Pair with PUBLIC_BROWSE_CLIENT_SECRETS env.
    buildConfigField(
        "String",
        "PUBLIC_BROWSE_CLIENT_ID",
        buildConfigStringLiteral(envVal("PUBLIC_BROWSE_CLIENT_ID") ?: "fash-android"),
    )
    buildConfigField(
        "String",
        "PUBLIC_BROWSE_CLIENT_TOKEN",
        buildConfigStringLiteral(envOrEmpty("PUBLIC_BROWSE_CLIENT_TOKEN")),
    )

    buildConfigField("String", "INTERNAL_SECRET", buildConfigStringLiteral(envOrEmpty("INTERNAL_SECRET")))
    /**
     * Optional long-lived Bearer for service calls when no user session; user JWT wins when logged in.
     */
    buildConfigField(
        "String",
        "INTERNAL_SERVICE_BEARER_TOKEN",
        buildConfigStringLiteral(envOrEmpty("INTERNAL_SERVICE_BEARER_TOKEN")),
    )

    /**
     * Public marketing / legal site root (no trailing slash). App opens `{base}/{vi|en}/terms` and `.../privacy`.
     * Production: `https://fashandcurious.com/portal` (see traefik PathPrefix `/portal`).
     */
    val legalPortalBaseUrl = envVal("LEGAL_PORTAL_BASE_URL") ?: "https://fashandcurious.com/portal"
    buildConfigField("String", "LEGAL_PORTAL_BASE_URL", buildConfigStringLiteral(legalPortalBaseUrl))

    /**
     * HTTPS URL for out-of-app identity / KYC re-verification (meetup trust). Empty = no in-app “open” button;
     * user can still complete verification elsewhere and tap “I've finished” to POST ack.
     */
    buildConfigField("String", "IDENTITY_REVERIFY_URL", buildConfigStringLiteral(envOrEmpty("IDENTITY_REVERIFY_URL")))

    /**
     * C2C chat: after a deal exists, offer **ship + online payment** path from the fulfillment chooser.
     * When false, the Ship option shows as coming soon / disabled (copy from strings).
     */
    val c2cShipFulfillmentEnabled =
        envVal("C2C_SHIP_FULFILLMENT_ENABLED")?.equals("true", ignoreCase = true) ?: true
    buildConfigField("boolean", "C2C_SHIP_FULFILLMENT_ENABLED", c2cShipFulfillmentEnabled.toString())
    /**
     * When false, ship flow still opens for address / copy but checkout / PSP is gated as in development.
     */
    val c2cShipOnlinePaymentEnabled =
        envVal("C2C_SHIP_ONLINE_PAYMENT_ENABLED")?.equals("true", ignoreCase = true) ?: true
    buildConfigField("boolean", "C2C_SHIP_ONLINE_PAYMENT_ENABLED", c2cShipOnlinePaymentEnabled.toString())

    /**
     * From env `SHIPPING` — when true, Home “Đang giao” opens a screen with live in-transit buying orders.
     * When false or unset as `false`, that screen shows a “coming soon” state (feature not enabled for this build).
     * Omit or set `true` to show real order data (default true preserves existing behaviour).
     */
    val shippingEnabled =
        envVal("SHIPPING")?.equals("true", ignoreCase = true) ?: true
    buildConfigField("boolean", "SHIPPING_ENABLED", shippingEnabled.toString())
}

android {
    namespace = "com.pc.fash_android_mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pc.fash_android_mobile"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Overridden per flavor by [injectFromEnv] (LISTING_SHARE_BASE_URL host).
        manifestPlaceholders["listingShareHost"] = "fash.app"
    }

    flavorDimensions += "environment"
    val devEnv = loadEnvFile(rootProject.file("env/dev.env"))
    val prodEnv = loadEnvFile(rootProject.file("env/prod.env"))

    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            injectFromEnv(devEnv, "dev")
        }
        create("prod") {
            dimension = "environment"
            injectFromEnv(prodEnv, "prod")
        }
    }

    signingConfigs {
        if (releaseKeystoreFile != null) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = project.prop("FASH_RELEASE_STORE_PASSWORD").orEmpty()
                keyAlias = project.prop("FASH_RELEASE_KEY_ALIAS") ?: "upload"
                keyPassword = project.prop("FASH_RELEASE_KEY_PASSWORD").orEmpty()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Without a release keystore, sign with the debug key so the APK is installable (not *-unsigned).
            // For Play Store / real distribution, set FASH_RELEASE_* in local.properties (see SIGNING.md).
            signingConfig = if (releaseKeystoreFile != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation("androidx.browser:browser:1.8.0")
    // Explicit coordinates — ensures IDE/Kotlin resolve `com.facebook.*` (Catalog `libs.fb.login` can fail indexing in some setups).
    implementation("com.facebook.android:facebook-login:17.0.2")
    implementation(libs.play.services.auth)
    implementation(libs.androidx.security.crypto)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.lifecycle.process)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
