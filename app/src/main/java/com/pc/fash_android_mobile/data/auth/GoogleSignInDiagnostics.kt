package com.pc.fash_android_mobile.data.auth

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.pc.fash_android_mobile.BuildConfig
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/** Runtime signing cert info — used when Google returns DEVELOPER_ERROR (package + SHA-1 mismatch). */
object GoogleSignInDiagnostics {
    private const val TAG = "GoogleSignIn"

    data class Snapshot(
        val packageName: String,
        val flavor: String,
        val buildType: String,
        val signingSha1: String?,
        val webClientIdConfigured: Boolean,
    )

    fun snapshot(context: Context): Snapshot {
        val webId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        return Snapshot(
            packageName = context.packageName,
            flavor = BuildConfig.FLAVOR,
            buildType = BuildConfig.BUILD_TYPE,
            signingSha1 = signingSha1ColonSeparated(context),
            webClientIdConfigured = webId.isNotEmpty() &&
                !webId.equals("YOUR_GOOGLE_WEB_CLIENT_ID", ignoreCase = true),
        )
    }

    fun logSnapshot(context: Context) {
        val s = snapshot(context)
        Log.i(
            TAG,
            "package=${s.packageName} flavor=${s.flavor} build=${s.buildType} " +
                "sha1=${s.signingSha1 ?: "?"} webClientConfigured=${s.webClientIdConfigured}",
        )
    }

    /** SHA-1 of the certificate that signed this installed APK/AAB (Play App Signing on store builds). */
    fun signingSha1ColonSeparated(context: Context): String? {
        return runCatching {
            val pm = context.packageManager
            val pkg = context.packageName
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            @Suppress("DEPRECATION")
            val info = pm.getPackageInfo(pkg, flags)
            val sigBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                info.signatures?.firstOrNull()?.toByteArray()
            } ?: return@runCatching null
            val cert = CertificateFactory.getInstance("X.509")
                .generateCertificate(ByteArrayInputStream(sigBytes)) as X509Certificate
            val digest = MessageDigest.getInstance("SHA-1").digest(cert.encoded)
            digest.joinToString(":") { byte -> "%02X".format(byte) }
        }.getOrNull()
    }
}
