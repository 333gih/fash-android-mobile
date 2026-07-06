package com.pc.fash_android_mobile.data.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.pc.fash_android_mobile.BuildConfig

/**
 * Shared Google Sign-In launch path — mirrors iOS [GoogleSignInClients.prepareForAccountPicker] +
 * preflight before opening the account picker.
 */
object GoogleSignInFlow {

    private fun isConfigured(): Boolean {
        val id = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        return id.isNotEmpty() && !id.equals("YOUR_GOOGLE_WEB_CLIENT_ID", ignoreCase = true)
    }

    /** Clears cached Google account so the next sign-in shows the account picker. */
    suspend fun prepareForAccountPicker(context: Context) {
        if (!isConfigured()) return
        withContext(Dispatchers.IO) {
            runCatching { buildGoogleSignInClient(context).signOut().await() }
        }
    }

    suspend fun blockReasonOrNull(context: Context): String? =
        GoogleSignInPreflight.blockReason(context)

    suspend fun launchSignIn(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
    ): LaunchResult {
        blockReasonOrNull(context)?.let { return LaunchResult.Blocked(it) }
        prepareForAccountPicker(context)
        return runCatching {
            val client = buildGoogleSignInClient(context)
            launcher.launch(client.signInIntent)
            LaunchResult.Launched
        }.getOrElse { LaunchResult.NotConfigured }
    }

    sealed interface LaunchResult {
        data object Launched : LaunchResult
        data object NotConfigured : LaunchResult
        data class Blocked(val message: String) : LaunchResult
    }
}
