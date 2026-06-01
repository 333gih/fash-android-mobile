package com.pc.fash_android_mobile.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Left-edge horizontal swipe to go back — mirrors iOS edge back. */
fun Modifier.fashEdgeBackSwipe(
    enabled: Boolean = true,
    edgeWidthDp: Float = 28f,
    triggerDragDp: Float = 72f,
    onBack: () -> Boolean,
): Modifier = composed {
    if (!enabled) return@composed this
    val density = LocalDensity.current
    val edgePx = with(density) { edgeWidthDp.dp.toPx() }
    val triggerPx = with(density) { triggerDragDp.dp.toPx() }
    val latestOnBack = rememberUpdatedState(onBack)
    pointerInput(enabled, edgePx, triggerPx) {
        var totalDrag = 0f
        var tracking = false
        detectHorizontalDragGestures(
            onDragStart = { offset ->
                tracking = offset.x <= edgePx
                totalDrag = 0f
            },
            onHorizontalDrag = { change, dragAmount ->
                if (tracking && dragAmount > 0f) {
                    totalDrag += dragAmount
                }
                change.consume()
            },
            onDragEnd = {
                if (tracking && totalDrag >= triggerPx) {
                    latestOnBack.value()
                }
                tracking = false
                totalDrag = 0f
            },
            onDragCancel = {
                tracking = false
                totalDrag = 0f
            },
        )
    }
}
