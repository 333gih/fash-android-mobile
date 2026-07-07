package com.pc.fash_android_mobile.data.auth

import android.content.Context
import com.facebook.login.LoginManager
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
            if (isGoogleWebClientIdConfigured(applicationContext)) {
                buildGoogleSignInClient(applicationContext).signOut().await()
            }
        }
        runCatching {
            LoginManager.getInstance().logOut()
        }
    }
}
