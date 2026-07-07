package com.pc.fash_android_mobile.data.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.pc.fash_android_mobile.BuildConfig

fun resolveGoogleWebClientId(context: Context): String {
    val fromEnv = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
    if (fromEnv.isNotEmpty() && !fromEnv.equals("YOUR_GOOGLE_WEB_CLIENT_ID", ignoreCase = true)) {
        return fromEnv
    }
    // google-services Gradle plugin → default_web_client_id from app/google-services.json
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    if (resId != 0) {
        val fromFirebase = context.getString(resId).trim()
        if (fromFirebase.isNotEmpty()) return fromFirebase
    }
    return fromEnv
}

fun isGoogleWebClientIdConfigured(context: Context): Boolean {
    val id = resolveGoogleWebClientId(context)
    return id.isNotEmpty() && !id.equals("YOUR_GOOGLE_WEB_CLIENT_ID", ignoreCase = true)
}

/**
 * Builds [GoogleSignInClient] using the **Web application** OAuth client id.
 *
 * Important: [BuildConfig.GOOGLE_WEB_CLIENT_ID] must be the **Web application** client ID from Google Cloud
 * Console (Credentials → OAuth 2.0 Client IDs → type *Web application*). A downloaded JSON whose root key is
 * `"installed"` is a *Desktop/other* client — that id is the wrong credential type for [requestIdToken] and
 * often causes [com.google.android.gms.common.api.ApiException] `DEVELOPER_ERROR` or invalid ID tokens.
 * Create a separate **Web application** client in the *same* GCP project, put its client id in env, and add
 * that same id to auth-service `GOOGLE_OAUTH_CLIENT_IDS` for server verification.
 *
 * You must also create **Android** OAuth clients (package + SHA-1) for `com.pc.fash_android_mobile` and, for
 * the `dev` flavor, `com.pc.fash_android_mobile.dev` (debug keystore SHA-1 + release/upload SHA-1 as needed).
 */
fun buildGoogleSignInClient(context: Context): GoogleSignInClient {
    val webClientId = resolveGoogleWebClientId(context)
    require(webClientId.isNotEmpty()) { "GOOGLE_WEB_CLIENT_ID is empty" }
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestProfile()
        .requestIdToken(webClientId)
        .build()
    return GoogleSignIn.getClient(context, gso)
}
