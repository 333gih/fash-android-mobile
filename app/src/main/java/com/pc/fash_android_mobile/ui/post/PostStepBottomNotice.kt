package com.pc.fash_android_mobile.ui.post

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Scrollable step body with an optional hint at the bottom. The hint is shown only when
 * the main content does not overflow vertically ([scrollState.maxValue] == 0), i.e. there is
 * free space and no scrolling is required.
 */
@Composable
fun PostStepScrollWithBottomNotice(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp,
    bottomNotice: String?,
    scrollState: ScrollState,
    bottomExtra: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val hasBottomText = !bottomNotice.isNullOrBlank()
    val hasExtra = bottomExtra != null
    val showBottomSlot = scrollState.maxValue == 0 && (hasBottomText || hasExtra)
    // Do not use fillMaxSize() on the same node as weight(1f) — it breaks max-height for verticalScroll.
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            // Scroll body must not use fillMaxSize — it prevents verticalScroll from getting a bounded viewport.
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = horizontalPadding),
            ) {
                content()
            }
        }
        if (showBottomSlot) {
            if (hasBottomText) {
                PostStepNoticeText(text = bottomNotice!!, horizontalPadding = horizontalPadding)
            }
            bottomExtra?.invoke()
        }
    }
}

/**
 * Lazy list step body with an optional hint at the bottom. The hint is shown only when the
 * list does not scroll in either direction (all items fit in the viewport).
 */
@Composable
fun PostStepLazyListWithBottomNotice(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp,
    bottomNotice: String?,
    listState: LazyListState,
    header: @Composable ColumnScope.() -> Unit,
    lazyContent: LazyListScope.() -> Unit,
) {
    val showNotice = !bottomNotice.isNullOrBlank() &&
        !listState.canScrollForward &&
        !listState.canScrollBackward
    Column(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
        ) {
            header()
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = horizontalPadding),
            ) {
                lazyContent()
            }
        }
        if (showNotice) {
            PostStepNoticeText(text = bottomNotice!!, horizontalPadding = horizontalPadding)
        }
    }
}

@Composable
fun PostStepNoticeText(text: String, horizontalPadding: Dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(top = 8.dp, bottom = 12.dp),
    )
}
