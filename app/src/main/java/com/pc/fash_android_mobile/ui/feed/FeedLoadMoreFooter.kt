package com.pc.fash_android_mobile.ui.feed

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.ui.theme.FashColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val LoadMoreSlotHeight = 48.dp

/**
 * Pinterest-style bottom sentinel — fixed height so pagination does not thrash scroll position.
 * Port of iOS [FeedLoadMoreFooter.swift].
 *
 * Pagination fires only when this footer overlaps the lazy grid viewport (not on prefetch compose),
 * so the spinner stays on-screen and load-more does not feel like a freeze behind the promo dock.
 */
@Composable
fun FeedLoadMoreFooter(
    enabled: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    anchorItemCount: Int = 0,
) {
    var visitArmed by remember { mutableStateOf(true) }
    var lastTriggeredAtCount by remember { mutableIntStateOf(-1) }
    var isVisibleInViewport by remember { mutableStateOf(false) }
    var rearmJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val latestEnabled by rememberUpdatedState(enabled)
    val latestLoading by rememberUpdatedState(isLoadingMore)
    val latestAnchor by rememberUpdatedState(anchorItemCount)
    val latestOnLoadMore by rememberUpdatedState(onLoadMore)
    val latestVisible by rememberUpdatedState(isVisibleInViewport)

    fun tryLoadOnVisit() {
        if (!latestEnabled || latestLoading || !visitArmed || !latestVisible) return
        if (latestAnchor <= lastTriggeredAtCount) return
        visitArmed = false
        lastTriggeredAtCount = latestAnchor
        latestOnLoadMore()
    }

    fun scheduleRearmAfterLeavingViewport() {
        rearmJob?.cancel()
        rearmJob = scope.launch {
            delay(280)
            lastTriggeredAtCount = -1
            visitArmed = true
            if (latestVisible) tryLoadOnVisit()
        }
    }

    fun updateViewportVisibility(visible: Boolean) {
        if (visible == isVisibleInViewport) return
        val wasVisible = isVisibleInViewport
        isVisibleInViewport = visible
        when {
            visible && !wasVisible -> {
                rearmJob?.cancel()
                tryLoadOnVisit()
            }
            !visible && wasVisible -> scheduleRearmAfterLeavingViewport()
        }
    }

    LaunchedEffect(anchorItemCount, enabled, isLoadingMore, isVisibleInViewport) {
        if (!isVisibleInViewport || !enabled || isLoadingMore) return@LaunchedEffect
        if (anchorItemCount > lastTriggeredAtCount) {
            visitArmed = true
            tryLoadOnVisit()
        }
    }

    LaunchedEffect(isLoadingMore, enabled, isVisibleInViewport) {
        if (isLoadingMore || !enabled || !isVisibleInViewport) return@LaunchedEffect
        delay(280)
        visitArmed = true
        tryLoadOnVisit()
    }

    DisposableEffect(Unit) {
        onDispose {
            rearmJob?.cancel()
            visitArmed = true
            lastTriggeredAtCount = -1
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(LoadMoreSlotHeight)
            .padding(vertical = 4.dp)
            .onPlaced { coordinates ->
                val parent = coordinates.parentLayoutCoordinates
                if (parent == null) {
                    updateViewportVisibility(false)
                    return@onPlaced
                }
                val bounds = coordinates.boundsInParent()
                val itemTop = bounds.top
                val itemBottom = bounds.bottom
                val viewportBottom = parent.size.height.toFloat()
                val visible = itemBottom > 0f && itemTop < viewportBottom
                updateViewportVisibility(visible)
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isLoadingMore) {
            val pulseTransition = rememberInfiniteTransition(label = "feed_load_more_pulse")
            val pulse by pulseTransition.animateFloat(
                initialValue = 0.92f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "feed_load_more_pulse_scale",
            )
            CircularProgressIndicator(
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                        alpha = 0.72f + ((pulse - 0.92f) / 0.16f) * 0.28f
                    },
                color = FashColors.Primary,
                strokeWidth = 2.dp,
            )
        }
    }
}
