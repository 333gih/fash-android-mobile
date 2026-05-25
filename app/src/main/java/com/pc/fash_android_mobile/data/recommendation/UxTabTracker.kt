package com.pc.fash_android_mobile.data.recommendation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Tracks home/profile tab opens with dwell time and syncs batches to core-service.
 */
class UxTabTracker(
    private val repository: RecommendationRepository,
    private val userIdProvider: () -> String?,
    private val guestBrowse: () -> Boolean,
    private val scope: CoroutineScope,
) {
    private val pending = mutableListOf<UxEventPayload>()
    private val lock = Any()
    private var activeScope: String? = null
    private var activeTabKey: String? = null
    private var openedAtMs: Long = 0L

    fun onTabOpened(scope: String, tabKey: String) {
        if (guestBrowse()) return
        closeActiveTab()
        activeScope = scope
        activeTabKey = tabKey
        openedAtMs = System.currentTimeMillis()
        enqueue(
            UxEventPayload(
                scope = scope,
                tabKey = tabKey,
                clientHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            ),
        )
    }

    fun closeActiveTab() {
        val scope = activeScope ?: return
        val tabKey = activeTabKey ?: return
        val dwellMs = (System.currentTimeMillis() - openedAtMs).toInt().coerceAtLeast(0)
        if (dwellMs >= 800) {
            enqueue(
                UxEventPayload(
                    scope = scope,
                    tabKey = tabKey,
                    clientHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                    dwellMs = dwellMs,
                ),
            )
        }
        activeScope = null
        activeTabKey = null
        openedAtMs = 0L
    }

    fun flush() {
        synchronized(lock) {
            flushLocked()
        }
    }

    private fun enqueue(event: UxEventPayload) {
        synchronized(lock) {
            pending.add(event)
            if (pending.size >= 20) {
                flushLocked()
            }
        }
    }

    private fun flushLocked() {
        if (pending.isEmpty()) return
        if (guestBrowse()) {
            pending.clear()
            return
        }
        val batch = pending.toList()
        pending.clear()
        scope.launch(Dispatchers.IO) {
            repository.recordUxEvents(batch)
        }
    }
}
