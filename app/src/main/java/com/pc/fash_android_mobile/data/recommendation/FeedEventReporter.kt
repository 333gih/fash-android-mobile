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
