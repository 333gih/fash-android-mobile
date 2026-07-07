package com.pc.fash_android_mobile.ui.feed

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fires when a tab's first-page load stays empty past [FeedLoadStallPolicy.TIMEOUT_MS] (iOS parity).
 */
class FeedLoadStallWatch {
    private val epochs = mutableMapOf<String, Long>()
    private val jobs = mutableMapOf<String, Job>()

    fun schedule(
        scope: CoroutineScope,
        key: String,
        isStillPending: () -> Boolean,
        onStalled: () -> Unit,
    ) {
        jobs[key]?.cancel()
        val epoch = (epochs[key] ?: 0L) + 1L
        epochs[key] = epoch
        jobs[key] = scope.launch {
            delay(FeedLoadStallPolicy.TIMEOUT_MS)
            if (epochs[key] != epoch) return@launch
            if (!isStillPending()) return@launch
            onStalled()
        }
    }

    fun cancel(key: String) {
        jobs[key]?.cancel()
        jobs.remove(key)
    }

    fun cancelAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        epochs.clear()
    }
}
