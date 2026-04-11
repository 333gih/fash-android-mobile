package com.pc.fash_android_mobile.data.chat

/**
 * Lenient checks aligned with core-service for meetup / deal map links (Google Maps share URLs).
 */
object ChatMapsUrlRules {
    private val hints = listOf(
        "maps.app.goo.gl",
        "goo.gl/maps",
        "google.com/maps",
        "maps.google.com",
        "share.google/",
    )

    fun isLenientMeetingMapsUrl(url: String): Boolean {
        val u = url.trim().lowercase()
        if (u.isEmpty()) return false
        if (u.startsWith("http://") || u.startsWith("https://")) {
            return hints.any { u.contains(it) }
        }
        return hints.any { u.contains(it) }
    }
}
