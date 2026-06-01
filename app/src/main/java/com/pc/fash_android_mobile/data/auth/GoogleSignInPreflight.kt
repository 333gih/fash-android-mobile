package com.pc.fash_android_mobile.data.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.pc.fash_android_mobile.R
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Fail fast before opening [com.google.android.gms.auth.api.signin.GoogleSignIn] UI.
 *
 * Play services waits up to ~30s (often twice) on [NETWORK_ERROR] when the device cannot
 * reach Google OAuth endpoints — preflight avoids that hang when the problem is obvious locally.
 */
object GoogleSignInPreflight {

    private val reachabilityClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    /** User-visible reason to show instead of launching Sign-In; null = OK to proceed. */
    fun blockReason(context: Context): String? {
        if (!hasActiveInternet(context)) {
            return context.getString(R.string.login_google_network_error)
        }
        val gms = GoogleApiAvailability.getInstance()
        val code = gms.isGooglePlayServicesAvailable(context)
        if (code != ConnectionResult.SUCCESS) {
            val detail = gms.getErrorString(code)?.trim().orEmpty()
            return if (detail.isEmpty()) {
                context.getString(R.string.login_google_play_services_error)
            } else {
                context.getString(R.string.login_google_play_services_error_detail, detail)
            }
        }
        if (!canReachGoogleAccounts()) {
            return context.getString(R.string.login_google_network_error)
        }
        return null
    }

    private fun hasActiveInternet(context: Context): Boolean {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Best-effort probe — same host GMS uses for OAuth token exchange. */
    private fun canReachGoogleAccounts(): Boolean = runCatching {
        val request = Request.Builder()
            .url("https://accounts.google.com/")
            .head()
            .header("User-Agent", "FashAndroid/GoogleSignInPreflight")
            .build()
        reachabilityClient.newCall(request).execute().use { response ->
            response.code in 200..399 || response.code == 405
        }
    }.getOrDefault(false)
}
