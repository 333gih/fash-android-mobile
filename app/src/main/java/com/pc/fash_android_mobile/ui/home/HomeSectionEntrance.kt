package com.pc.fash_android_mobile.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Staggered fade + slide-up for home dashboard rows (Gen Z–friendly motion, not distracting).
 *
 * Use [StaggeredEntrance] when several siblings should appear in sequence (e.g. journey stat cards
 * that share a parent composition). For LazyColumn rails, prefer [HomeSectionReveal] which animates
 * each rail individually as it scrolls into view and persists the "seen" flag across the session so
 * re-entering a rail does not re-trigger the motion.
 */
@Composable
internal fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    baseDelayMs: Long = 0L,
    staggerMs: Long = 72L,
    initialOffsetY: Dp = 14.dp,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(baseDelayMs + staggerMs * index)
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "homeStaggerAlpha",
    )
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else initialOffsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "homeStaggerY",
    )
    Box(
        modifier = modifier
            .offset { IntOffset(0, offsetY.roundToPx()) }
            .graphicsLayer { this.alpha = alpha },
    ) {
        content()
    }
}

/**
 * One-shot fade + slide-up reveal for a single rail/section in a LazyColumn.
 *
 * Each call uses [rememberSaveable] keyed by [sectionKey] so:
 * - the rail animates the FIRST time it scrolls into view, and
 * - re-scrolling past it later does not retrigger (no jitter during fast scroll).
 *
 * [initialOffsetY] = 16dp + 240ms is intentionally subtler than the journey stagger (14dp + 360ms +
 * cross-stagger) so individual rails feel light rather than dramatic.
 */
@Composable
internal fun HomeSectionReveal(
    sectionKey: String,
    modifier: Modifier = Modifier,
    initialOffsetY: Dp = 16.dp,
    durationMs: Int = 240,
    content: @Composable () -> Unit,
) {
    var seen by rememberSaveable(sectionKey) { mutableStateOf(false) }
    if (seen) {
        Box(modifier = modifier) { content() }
        return
    }
    var visible by remember(sectionKey) { mutableStateOf(false) }
    LaunchedEffect(sectionKey) {
        visible = true
        seen = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        label = "homeRevealAlpha-$sectionKey",
    )
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else initialOffsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "homeRevealOffsetY-$sectionKey",
    )
    Box(
        modifier = modifier
            .offset { IntOffset(0, offsetY.roundToPx()) }
            .graphicsLayer { this.alpha = alpha },
    ) {
        content()
    }
}
