package com.pc.fash_android_mobile.deeplink

import android.content.Intent
import android.net.Uri
import com.pc.fash_android_mobile.BuildConfig

/** Parses seller shop deep links; matches [com.pc.fash_android_mobile.config.AppEnvironment.profileShareUrl]. */
object ProfileDeepLinks {

    private val USERNAME_RE = Regex("^[a-zA-Z0-9_]{2,32}$")

    fun normalizeUsername(raw: String): String = raw.trim().removePrefix("@")

    /** `fash://profile/{username}` — opens seller shop when app is installed. */
    fun fashProfileUri(username: String): Uri {
        val handle = normalizeUsername(username)
        return Uri.Builder()
            .scheme("fash")
            .authority("profile")
            .appendPath(handle)
            .build()
    }

    fun parseUsernameFromIntent(intent: Intent?): String? {
        val data = intent?.data ?: return null
        return parseUsername(data)
    }

    fun parseUsername(uri: Uri): String? {
        if (uri.scheme.equals("fash", ignoreCase = true) &&
            uri.host.equals("profile", ignoreCase = true)
        ) {
            val handle = uri.pathSegments.firstOrNull { it.isNotBlank() } ?: return null
            return handle.takeIf { USERNAME_RE.matches(it) }
        }
        if (!uri.scheme.equals("http", ignoreCase = true) &&
            !uri.scheme.equals("https", ignoreCase = true)
        ) {
            return null
        }
        val expectedHost = try {
            Uri.parse(BuildConfig.LISTING_SHARE_BASE_URL).host
        } catch (_: Exception) {
            null
        } ?: return null
        if (!uri.host.equals(expectedHost, ignoreCase = true)) return null
        val baseUri = try {
            Uri.parse(BuildConfig.LISTING_SHARE_BASE_URL)
        } catch (_: Exception) {
            return null
        }
        val basePath = baseUri.path?.trim('/') ?: ""
        val baseSegments = if (basePath.isEmpty()) emptyList() else basePath.split('/').filter { it.isNotEmpty() }
        val segs = uri.pathSegments
        // Expected: …/p/u/{username} (listing base is typically …/p/l)
        val uIndex = baseSegments.indexOfLast { it.equals("p", ignoreCase = true) }
        val expectedPrefix = if (uIndex >= 0) {
            baseSegments.take(uIndex + 1) + "u"
        } else {
            baseSegments + "u"
        }
        if (segs.size != expectedPrefix.size + 1) return null
        if (!expectedPrefix.indices.all { i -> segs[i].equals(expectedPrefix[i], ignoreCase = true) }) {
            return null
        }
        val handle = segs[expectedPrefix.size].trim()
        return handle.takeIf { USERNAME_RE.matches(it) }
    }
}
