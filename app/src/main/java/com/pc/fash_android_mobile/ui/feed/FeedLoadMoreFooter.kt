package com.pc.fash_android_mobile.ui.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.ui.theme.FashColors
import kotlinx.coroutines.delay

private val LoadMoreSlotHeight = 48.dp

/**
 * Pinterest-style bottom sentinel — fixed height so pagination does not thrash scroll position.
 * Port of iOS [FeedLoadMoreFooter.swift].
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
    val latestEnabled by rememberUpdatedState(enabled)
    val latestLoading by rememberUpdatedState(isLoadingMore)
    val latestAnchor by rememberUpdatedState(anchorItemCount)
    val latestOnLoadMore by rememberUpdatedState(onLoadMore)

    fun tryLoadOnVisit() {
        if (!latestEnabled || latestLoading || !visitArmed) return
        if (latestAnchor <= lastTriggeredAtCount) return
        visitArmed = false
        lastTriggeredAtCount = latestAnchor
        latestOnLoadMore()
    }

    DisposableEffect(enabled, isLoadingMore, anchorItemCount) {
        if (enabled && !isLoadingMore) {
            tryLoadOnVisit()
        }
        onDispose { }
    }

    DisposableEffect(Unit) {
        onDispose {
            visitArmed = true
            lastTriggeredAtCount = -1
        }
    }

    androidx.compose.runtime.LaunchedEffect(anchorItemCount) {
        if (anchorItemCount > lastTriggeredAtCount && enabled && !isLoadingMore) {
            visitArmed = true
            tryLoadOnVisit()
        }
    }

    androidx.compose.runtime.LaunchedEffect(isLoadingMore) {
        if (!isLoadingMore && enabled) {
            delay(280)
            visitArmed = true
            tryLoadOnVisit()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(LoadMoreSlotHeight)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoadingMore) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = FashColors.Primary,
                strokeWidth = 2.dp,
            )
        }
    }
}
