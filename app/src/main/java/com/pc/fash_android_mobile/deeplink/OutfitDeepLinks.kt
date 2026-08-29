package com.pc.fash_android_mobile.deeplink

import android.net.Uri

object OutfitDeepLinks {
    fun fashOutfitSetUri(setId: String): Uri {
        val id = setId.trim()
        return Uri.Builder()
            .scheme("fash")
            .authority("outfit-set")
            .appendPath(id)
            .build()
    }

    fun parseOutfitSetId(uri: Uri): String? {
        if (!uri.scheme.equals("fash", ignoreCase = true)) return null
        if (!uri.host.equals("outfit-set", ignoreCase = true)) return null
        return uri.pathSegments.firstOrNull { it.isNotBlank() }
    }

    fun parseOutfitSetIdFromDeepLink(deepLink: String): String? {
        val trimmed = deepLink.trim()
        if (trimmed.isEmpty()) return null
        return runCatching { parseOutfitSetId(Uri.parse(trimmed)) }.getOrNull()
    }

    fun isOutfitDailyDropListDeepLink(deepLink: String): Boolean {
        val uri = runCatching { Uri.parse(deepLink.trim()) }.getOrNull() ?: return false
        return uri.scheme.equals("fash", ignoreCase = true) &&
            uri.host.equals("outfit-daily-drop", ignoreCase = true)
    }
}
