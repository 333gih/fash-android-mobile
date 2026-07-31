package com.pc.fash_android_mobile.ui.feed

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

/**
 * Restores scroll anchor after pagination append — [androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid]
 * can snap toward the top while the user holds near the bottom before the next page finishes loading.
 * Port of iOS [FeedScrollTrimCompensator] / clamp-only scroll preservation for feed append.
 */
@Composable
fun FeedStaggeredGridScrollPreserveEffect(
    state: LazyStaggeredGridState,
    itemCount: Int,
    enabled: Boolean = true,
) {
    if (!enabled) return

    var lastItemCount by remember { mutableIntStateOf(itemCount) }
    var anchorIndex by remember { mutableIntStateOf(0) }
    var anchorOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(state) {
        snapshotFlow {
            state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset
        }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                anchorIndex = index
                anchorOffset = offset
            }
    }

    LaunchedEffect(itemCount) {
        val previous = lastItemCount
        lastItemCount = itemCount
        if (itemCount <= previous || previous <= 0 || anchorIndex <= 0) return@LaunchedEffect

        val restoreIndex = anchorIndex
        val restoreOffset = anchorOffset
        snapshotFlow { state.layoutInfo.totalItemsCount }.first { it > restoreIndex }
        if (state.firstVisibleItemIndex < restoreIndex - 1) {
            state.scrollToItem(restoreIndex, restoreOffset)
        }
    }
}

/** Same append guard for profile / notification [androidx.compose.foundation.lazy.LazyColumn] feeds. */
@Composable
fun FeedLazyListScrollPreserveEffect(
    state: LazyListState,
    itemCount: Int,
    enabled: Boolean = true,
) {
    if (!enabled) return

    var lastItemCount by remember { mutableIntStateOf(itemCount) }
    var anchorIndex by remember { mutableIntStateOf(0) }
    var anchorOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(state) {
        snapshotFlow {
            state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset
        }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                anchorIndex = index
                anchorOffset = offset
            }
    }

    LaunchedEffect(itemCount) {
        val previous = lastItemCount
        lastItemCount = itemCount
        if (itemCount <= previous || previous <= 0 || anchorIndex <= 0) return@LaunchedEffect

        val restoreIndex = anchorIndex
        val restoreOffset = anchorOffset
        snapshotFlow { state.layoutInfo.totalItemsCount }.first { it > restoreIndex }
        if (state.firstVisibleItemIndex < restoreIndex - 1) {
            state.scrollToItem(restoreIndex, restoreOffset)
        }
    }
}
