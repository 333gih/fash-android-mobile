package com.pc.fash_android_mobile.ui.splash

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

/**
 * Runs [block] and keeps the UI in a waiting state until both work finishes and at least
 * [minDisplayMs] has passed — avoids jarring sub-second flashes when switching pages.
 */
suspend fun runFashWaitWithMinDuration(
    minDisplayMs: Long,
    onWaiting: (Boolean) -> Unit,
    block: suspend () -> Unit,
) {
    onWaiting(true)
    try {
        coroutineScope {
            val work = async { block() }
            val minHold = async { delay(minDisplayMs) }
            work.await()
            minHold.await()
        }
    } finally {
        onWaiting(false)
    }
}
