package com.pc.fash_android_mobile.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** When true, listing cards should ignore taps (horizontal tab swipe in progress or just committed). */
val LocalFashTabSwipeConsuming = compositionLocalOf { false }

/**
 * Horizontal chip rows (category / brand / aesthetic) — consume horizontal drags so parent
 * [fashTabSwipe] on the profile list does not switch listing tabs.
 */
fun Modifier.fashConsumeHorizontalPointerForTabSwipe(): Modifier = pointerInput(Unit) {
    val touchSlopPx = with(density) { 12.dp.toPx() }
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var totalX = 0f
        var totalY = 0f
        var lockedHorizontal = false
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            val delta = change.positionChange()
            totalX += delta.x
            totalY += delta.y
            if (!lockedHorizontal && (abs(totalX) > touchSlopPx || abs(totalY) > touchSlopPx)) {
                lockedHorizontal = abs(totalX) > abs(totalY) * HORIZONTAL_DOMINANCE_RATIO
            }
            if (lockedHorizontal) {
                event.changes.forEach { if (it.pressed) it.consume() }
            }
        }
    }
}

private const val HORIZONTAL_DOMINANCE_RATIO = 1.15f
private const val VELOCITY_FLING_PX_PER_S = 900f

/**
 * TikTok-style horizontal tab swipe on vertical feeds: lock axis, consume touches, velocity-aware snap.
 */
fun Modifier.fashTabSwipe(
    enabled: Boolean = true,
    tabCount: Int,
    currentVisualIndex: Int,
    onVisualIndexChanged: (Int) -> Unit,
    onConsumingChanged: (Boolean) -> Unit = {},
    onTabSwipeCommitted: () -> Unit = {},
): Modifier = pointerInput(enabled, tabCount, currentVisualIndex) {
        if (!enabled || tabCount <= 1) return@pointerInput

        val touchSlopPx = with(density) { 12.dp.toPx() }
        val distanceThresholdPx = with(density) { 72.dp.toPx() }
        val flingDistancePx = with(density) { 36.dp.toPx() }

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var totalX = 0f
            var totalY = 0f
            var horizontalLocked = false
            var lastEventTime = down.uptimeMillis
            var velocityX = 0f

            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break

                val delta = change.positionChange()
                val dt = (change.uptimeMillis - lastEventTime).coerceAtLeast(1L)
                velocityX = (delta.x / dt) * 1000f
                lastEventTime = change.uptimeMillis

                if (!horizontalLocked) {
                    totalX += delta.x
                    totalY += delta.y
                    if (abs(totalX) > touchSlopPx || abs(totalY) > touchSlopPx) {
                        horizontalLocked = abs(totalX) > abs(totalY) * HORIZONTAL_DOMINANCE_RATIO
                        if (horizontalLocked) onConsumingChanged(true)
                    }
                } else {
                    totalX += delta.x
                    event.changes.forEach { it.consume() }
                }
            }

            onConsumingChanged(false)

            if (!horizontalLocked) return@awaitEachGesture

            val flingNext = velocityX <= -VELOCITY_FLING_PX_PER_S
            val flingPrev = velocityX >= VELOCITY_FLING_PX_PER_S
            val commitNext =
                (totalX <= -distanceThresholdPx || (flingNext && abs(totalX) > flingDistancePx)) &&
                    currentVisualIndex < tabCount - 1
            val commitPrev =
                (totalX >= distanceThresholdPx || (flingPrev && abs(totalX) > flingDistancePx)) &&
                    currentVisualIndex > 0

            when {
                commitNext -> {
                    onTabSwipeCommitted()
                    onVisualIndexChanged(currentVisualIndex + 1)
                }
                commitPrev -> {
                    onTabSwipeCommitted()
                    onVisualIndexChanged(currentVisualIndex - 1)
                }
            }
        }
}
