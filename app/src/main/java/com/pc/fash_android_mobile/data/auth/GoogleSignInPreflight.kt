package com.pc.fash_android_mobile.data.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.pc.fash_android_mobile.R

/**
 * Fail fast before opening [com.google.android.gms.auth.api.signin.GoogleSignIn] UI.
 *
 * Play services waits up to ~30s (often twice) on [NETWORK_ERROR] when the device cannot
 * reach Google OAuth endpoints — preflight avoids that hang when the problem is obvious locally.
 */
object GoogleSignInPreflight {

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
        // iOS parity: no HTTP probe before opening the account picker. Some carriers / Private DNS
        // block app OkHttp to accounts.google.com while Google Play services sign-in still works.
        return null
    }

    private fun hasActiveInternet(context: Context): Boolean {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
