package com.pc.fash_android_mobile.deeplink

import android.content.Intent
import android.net.Uri
import com.pc.fash_android_mobile.BuildConfig

/** Parses listing deep links; matches [AppEnvironment.listingShareUrl] and `fash://listing/{id}`. */
object ListingDeepLinks {

    /** `fash://listing/{listingId}` — always opens in app when installed. */
    fun fashListingUri(listingId: String): Uri {
        val id = listingId.trim()
        return Uri.Builder()
            .scheme("fash")
            .authority("listing")
            .appendPath(id)
            .build()
    }

    fun parseListingIdFromIntent(intent: Intent?): String? {
        val data = intent?.data ?: return null
        return parseListingId(data)
    }

    fun parseListingId(uri: Uri): String? {
        if (uri.scheme.equals("fash", ignoreCase = true) &&
            uri.host.equals("listing", ignoreCase = true)
        ) {
            val id = uri.pathSegments.firstOrNull { it.isNotBlank() } ?: return null
            return id.takeIf { it.length >= 8 }
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
        if (segs.size <= baseSegments.size) return null
        if (baseSegments.isNotEmpty()) {
            val ok = baseSegments.indices.all { i -> segs[i].equals(baseSegments[i], ignoreCase = true) }
            if (!ok) return null
        }
        return segs[baseSegments.size].takeIf { it.isNotBlank() && it.length >= 8 }
    }
}
