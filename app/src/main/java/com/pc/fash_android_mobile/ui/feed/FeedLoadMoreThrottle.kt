package com.pc.fash_android_mobile.ui.feed

import com.pc.fash_android_mobile.data.http.CoreServiceHttpException

/**
 * Shared scroll-pagination pacing — avoids hammering core-service when the feed footer
 * retriggers or the user flings quickly (iOS: loadMoreCooldownUntil parity).
 */
object FeedLoadMoreThrottle {
    const val DEFAULT_INTERVAL_MS = 400L
    const val FOLLOWING_INTERVAL_MS = 900L
    private const val RATE_LIMIT_FLOOR_MS = 2_000L

    fun canLoadNow(lastAtMs: Long, minIntervalMs: Long = DEFAULT_INTERVAL_MS): Boolean {
        if (lastAtMs <= 0L) return true
        return System.currentTimeMillis() - lastAtMs >= minIntervalMs
    }

    fun isBlocked(untilMs: Long): Boolean = untilMs > 0L && System.currentTimeMillis() < untilMs

    fun blockedUntilAfterRateLimit(retryAfterSeconds: Int?): Long {
        val waitMs = when {
            retryAfterSeconds == null || retryAfterSeconds <= 0 -> RATE_LIMIT_FLOOR_MS
            else -> maxOf(RATE_LIMIT_FLOOR_MS, retryAfterSeconds * 1000L)
        }
        return System.currentTimeMillis() + waitMs
    }

    fun blockedUntilAfter(error: Throwable): Long? {
        val http = error as? CoreServiceHttpException ?: return null
        if (!http.isRateLimited) return null
        return blockedUntilAfterRateLimit(http.retryAfterSeconds)
    }
}
