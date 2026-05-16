package com.pc.fash_android_mobile.data.auth

import android.content.Context
import com.facebook.login.LoginManager
import com.pc.fash_android_mobile.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Clears Google Sign-In and Facebook Login SDK caches so the next sign-in can show the account
 * picker / consent again. App JWT logout alone does not revoke the Google account session.
 */
suspend fun clearCachedSocialSignInForLogout(applicationContext: Context) {
    withContext(Dispatchers.IO) {
        runCatching {
            val webId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
            if (webId.isNotEmpty() && !webId.equals("YOUR_GOOGLE_WEB_CLIENT_ID", ignoreCase = true)) {
                buildGoogleSignInClient(applicationContext).signOut().await()
            }
        }
        runCatching {
            LoginManager.getInstance().logOut()
        }
    }
}
