package com.pc.fash_android_mobile.data.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.pc.fash_android_mobile.BuildConfig

/**
 * Builds [GoogleSignInClient] using the **Web application** OAuth client id in [BuildConfig.GOOGLE_WEB_CLIENT_ID].
 * You must also register an **Android** OAuth client (package + SHA-1) in Google Cloud Console.
 */
fun buildGoogleSignInClient(context: Context): GoogleSignInClient {
    val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
    require(webClientId.isNotEmpty()) { "GOOGLE_WEB_CLIENT_ID is empty" }
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestProfile()
        .requestIdToken(webClientId)
        .build()
    return GoogleSignIn.getClient(context, gso)
}
