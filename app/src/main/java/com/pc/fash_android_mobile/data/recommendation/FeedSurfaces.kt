package com.pc.fash_android_mobile.data.recommendation

/**
 * Feed event surfaces — must match core-service `FeedSurface*` constants
 * (`feed_event.entity.go`).
 */
object FeedSurfaces {
    const val APP_OPEN = "app_open"
    const val NOTIFICATION_OPEN = "notification_open"

    /**
     * Placeholder listing id for session-level events (no listing context).
     * Server ingest expects a UUID-shaped value; core-service maps engagement from surface.
     */
    const val SESSION_SENTINEL_LISTING_ID = "00000000-0000-4000-8090-000000000001"
}
