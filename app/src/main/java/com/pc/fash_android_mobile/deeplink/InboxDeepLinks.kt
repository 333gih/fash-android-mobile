package com.pc.fash_android_mobile.deeplink

import android.content.Intent
import android.net.Uri
import java.util.UUID

/** In-app inbox / FCM tray: `fash://inbox/{notificationId}` (ledger row UUID). */
object InboxDeepLinks {

    fun parseNotificationIdFromIntent(intent: Intent?): String? {
        intent ?: return null
        intent.getStringExtra("deep_link")?.let { parseNotificationIdFromDeepLinkString(it) }?.let { return it }
        intent.getStringExtra("notification_id")?.let { parseUuid(it) }?.let { return it }
        intent.data?.let { parseNotificationIdFromUri(it) }?.let { return it }
        return null
    }

    fun parseNotificationIdFromDeepLinkString(raw: String): String? {
        val s = raw.trim()
        if (s.isEmpty()) return null
        return runCatching { parseNotificationIdFromUri(Uri.parse(s)) }.getOrNull()
    }

    fun parseNotificationIdFromUri(uri: Uri): String? {
        if (!uri.scheme.equals("fash", ignoreCase = true)) return null
        if (!uri.host.equals("inbox", ignoreCase = true)) return null
        val seg = uri.pathSegments.firstOrNull { it.isNotBlank() } ?: return null
        return parseUuid(seg)
    }

    private fun parseUuid(s: String): String? {
        val t = s.trim()
        if (t.length < 32) return null
        return runCatching { UUID.fromString(t).toString() }.getOrNull()
    }
}
