package com.pc.fash_android_mobile.data.recommendation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Batches feed impressions/clicks and flushes to core recommendation API (Phase 2).
 */
class FeedEventReporter(
    private val repository: RecommendationRepository,
    private val sessionIdProvider: () -> String,
    private val publicBrowse: () -> Boolean,
    private val scope: CoroutineScope,
) {
    private val pending = mutableListOf<FeedEventPayload>()
    private val lock = Any()

    fun impression(listingId: String, surface: String, position: Int = 0, dwellMs: Int? = null) {
        enqueue(
            FeedEventPayload(
                listingId = listingId,
                surface = surface,
                eventType = "impression",
                position = position,
                dwellMs = dwellMs,
            ),
        )
    }

    fun click(listingId: String, surface: String, position: Int = 0) {
        enqueue(
            FeedEventPayload(
                listingId = listingId,
                surface = surface,
                eventType = "click",
                position = position,
            ),
        )
        flush()
    }

    /** User opened the quick-look bottom sheet (lower intent than a direct PDP click). */
    fun previewOpen(listingId: String, surface: String, position: Int = 0) {
        enqueue(
            FeedEventPayload(
                listingId = listingId,
                surface = surface,
                eventType = "preview_open",
                position = position,
            ),
        )
        flush()
    }

    /** User closed quick look without opening PDP/chat — [dwellMs] drives taste weight server-side. */
    fun previewDismiss(listingId: String, surface: String, position: Int = 0, dwellMs: Int) {
        if (dwellMs <= 0) return
        enqueue(
            FeedEventPayload(
                listingId = listingId,
                surface = surface,
                eventType = "preview_dismiss",
                position = position,
                dwellMs = dwellMs,
            ),
        )
        flush()
    }

    /** User continued from quick look to full product detail. */
    fun previewDetail(listingId: String, surface: String, position: Int = 0) {
        enqueue(
            FeedEventPayload(
                listingId = listingId,
                surface = surface,
                eventType = "preview_detail",
                position = position,
            ),
        )
        flush()
    }

    /**
     * Records a true dwell signal (item visible for [dwellMs]). Distinct from `impression` so backend
     * taste aggregation can score watch-time correctly (see AggregateFeedEventWeights).
     */
    fun dwell(listingId: String, surface: String, position: Int = 0, dwellMs: Int) {
        if (dwellMs <= 0) return
        enqueue(
            FeedEventPayload(
                listingId = listingId,
                surface = surface,
                eventType = "dwell",
                position = position,
                dwellMs = dwellMs,
            ),
        )
    }

    /**
     * High-intent buyer signal — listing was saved to wishlist. Server weights saves > clicks > impressions
     * (see core-service feed_events normalization). Flushed immediately so taste refresh sees it.
     */
    fun save(listingId: String, surface: String, position: Int = 0) {
        enqueue(
            FeedEventPayload(
                listingId = listingId,
                surface = surface,
                eventType = "save",
                position = position,
            ),
        )
        flush()
    }

    /** Lightweight affection signal. Less weight than `save` but stronger than `click`. */
    fun like(listingId: String, surface: String, position: Int = 0) {
        enqueue(
            FeedEventPayload(
                listingId = listingId,
                surface = surface,
                eventType = "like",
                position = position,
            ),
        )
        flush()
    }

    /**
     * Outbound share (deeplink generated, share sheet shown). Surface is whatever the user shared
     * FROM (e.g. "pdp", "explore_grid", "home_for_you").
     */
    fun share(listingId: String, surface: String, position: Int = 0) {
        enqueue(
            FeedEventPayload(
                listingId = listingId,
                surface = surface,
                eventType = "share",
                position = position,
            ),
        )
        flush()
    }

    /**
     * User opened/created a chat about the listing — closest signal to "ready to buy" we currently
     * collect. Flushed eagerly.
     */
    fun chatInitiate(listingId: String, surface: String, position: Int = 0) {
        enqueue(
            FeedEventPayload(
                listingId = listingId,
                surface = surface,
                eventType = "chat_initiate",
                position = position,
            ),
        )
        flush()
    }

    /**
     * Viewer followed the seller from a listing context. Distinct from a plain profile follow so the
     * backend can attribute the signal to a listing surface for content-based taste building.
     */
    fun followSeller(listingId: String, surface: String, position: Int = 0) {
        enqueue(
            FeedEventPayload(
                listingId = listingId,
                surface = surface,
                eventType = "follow_seller",
                position = position,
            ),
        )
        flush()
    }

    private fun enqueue(event: FeedEventPayload) {
        synchronized(lock) {
            pending.add(event)
            if (pending.size >= 20) {
                flushLocked()
            }
        }
    }

    fun flush() {
        synchronized(lock) {
            flushLocked()
        }
    }

    /** Drops queued events without sending — use on sign-out before session id changes. */
    fun clearPending() {
        synchronized(lock) {
            pending.clear()
        }
    }

    private fun flushLocked() {
        if (pending.isEmpty()) return
        val batch = pending.toList()
        pending.clear()
        val session = sessionIdProvider()
        scope.launch(Dispatchers.IO) {
            repository.recordFeedEvents(publicBrowse(), session, batch)
        }
    }
}
