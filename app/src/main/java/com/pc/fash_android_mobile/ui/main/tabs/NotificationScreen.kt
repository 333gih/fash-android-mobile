package com.pc.fash_android_mobile.ui.main.tabs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.user.InboxNotificationItem
import com.pc.fash_android_mobile.ui.common.stableLazyKey
import com.pc.fash_android_mobile.ui.components.FashBottomPromoAdStrip
import com.pc.fash_android_mobile.ui.components.FashEmptyBulletTipLine
import com.pc.fash_android_mobile.ui.components.FashEmptyState
import com.pc.fash_android_mobile.ui.components.FashPromoSlideDef
import com.pc.fash_android_mobile.ui.components.FashPromoSliderBlock
import com.pc.fash_android_mobile.ui.notifications.NotificationDetailScreen
import com.pc.fash_android_mobile.ui.notifications.NotificationsViewModel
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val NotificationAdHeightFraction = 0.18f
private val NotificationAdMinHeight = 72.dp

/**
 * In-app notification inbox: `GET …/users/me/notifications` with keyset paging and read via PATCH.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel,
    onBack: () -> Unit,
    onExploreClick: () -> Unit = {},
    onPromoSlideClick: (slideId: String, pageIndex: Int) -> Unit = { _, _ -> },
    promoSlides: List<FashPromoSlideDef>? = null,
    onOpenOrder: (String) -> Unit = {},
    onOpenListing: (String, String?) -> Unit = { _, _ -> },
) {
    val scheme = MaterialTheme.colorScheme
    val items by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val loadMoreBusy by viewModel.loadMoreBusy.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val inboxUnavailable by viewModel.inboxUnavailable.collectAsState()
    val selectedDetailId by viewModel.selectedDetailId.collectAsState()
    val pullState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    DisposableEffect(Unit) {
        onDispose { viewModel.closeDetail() }
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(listState, items.size, hasMore, loadMoreBusy, isLoading) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to items.size
        }
            .distinctUntilChanged()
            .collect { (lastVisible, size) ->
                if (size > 0 && hasMore && !loadMoreBusy && !isLoading && lastVisible >= size - 3) {
                    viewModel.loadMore()
                }
            }
    }

    val detailItem = selectedDetailId?.let { id -> items.find { it.id == id } }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = scheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.notifications),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = scheme.onSurface,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back),
                                    tint = FashColors.Primary,
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = scheme.surface,
                            titleContentColor = scheme.onSurface,
                        ),
                    )
                },
            ) { paddingValues ->
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    val adHeight = (maxHeight * NotificationAdHeightFraction).coerceAtLeast(NotificationAdMinHeight)
                    Column(Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            when {
                                isLoading && items.isEmpty() -> {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = FashColors.Primary)
                                    }
                                }
                                loadError != null && items.isEmpty() -> {
                                    if (inboxUnavailable) {
                                        FashEmptyState(
                                            icon = Icons.Outlined.Notifications,
                                            title = stringResource(R.string.notification_inbox_unavailable_title),
                                            subtitle = stringResource(R.string.notification_inbox_unavailable_subtitle),
                                            modifier = Modifier.fillMaxSize(),
                                            contentDescription = null,
                                        )
                                    } else {
                                        FashEmptyState(
                                            icon = Icons.Outlined.ErrorOutline,
                                            title = stringResource(R.string.notification_load_error_title),
                                            subtitle = loadError?.takeIf { it.isNotBlank() }
                                                ?: stringResource(R.string.notification_load_error_subtitle),
                                            modifier = Modifier.fillMaxSize(),
                                            contentDescription = null,
                                            footer = {
                                                Spacer(Modifier.height(4.dp))
                                                OutlinedButton(
                                                    onClick = { viewModel.retryAfterError() },
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = FashColors.Primary,
                                                    ),
                                                    shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
                                                ) {
                                                    Text(stringResource(R.string.feed_retry))
                                                }
                                            },
                                        )
                                    }
                                }
                                items.isEmpty() -> {
                                    FashEmptyState(
                                        icon = Icons.Outlined.Notifications,
                                        title = stringResource(R.string.notification_empty_title),
                                        subtitle = stringResource(R.string.notification_empty_subtitle),
                                        modifier = Modifier.fillMaxSize(),
                                        contentDescription = null,
                                        footer = {
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                text = stringResource(R.string.notification_empty_hints_title),
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                                color = scheme.onSurface,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                            ) {
                                                FashEmptyBulletTipLine(text = stringResource(R.string.notification_empty_tip_1))
                                                FashEmptyBulletTipLine(text = stringResource(R.string.notification_empty_tip_2))
                                                FashEmptyBulletTipLine(text = stringResource(R.string.notification_empty_tip_3))
                                            }
                                        },
                                    )
                                }
                                else -> {
                                    PullToRefreshBox(
                                        isRefreshing = isRefreshing,
                                        onRefresh = { viewModel.refresh() },
                                        modifier = Modifier.fillMaxSize(),
                                        state = pullState,
                                        indicator = {
                                            PullToRefreshDefaults.Indicator(
                                                state = pullState,
                                                isRefreshing = isRefreshing,
                                                color = FashColors.Primary,
                                                containerColor = scheme.surface,
                                                modifier = Modifier.align(Alignment.TopCenter),
                                            )
                                        },
                                    ) {
                                        LazyColumn(
                                            state = listState,
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(
                                                horizontal = FashTheme.spacing.editorialStart,
                                                vertical = 16.dp,
                                            ),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            items(
                                                items = items,
                                                key = { stableLazyKey(it.id, 0, "n") },
                                            ) { row ->
                                                NotificationInboxRow(
                                                    item = row,
                                                    onClick = {
                                                        viewModel.openDetail(row.id)
                                                        viewModel.markReadIfNeeded(row)
                                                    },
                                                )
                                            }
                                            if (loadMoreBusy) {
                                                item(key = "notif_load_more") {
                                                    Box(
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .padding(16.dp),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.height(28.dp),
                                                            color = FashColors.Primary,
                                                            strokeWidth = 2.dp,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = scheme.outlineVariant.copy(alpha = 0.35f),
                        )
                        FashPromoSliderBlock(
                            slides = promoSlides,
                            onSlideClick = onPromoSlideClick,
                        )
                        FashBottomPromoAdStrip(
                            modifier = Modifier
                                .height(adHeight)
                                .fillMaxWidth(),
                            onExploreClick = onExploreClick,
                        )
                    }
                }
            }

            if (detailItem != null) {
                BackHandler { viewModel.closeDetail() }
            }
            detailItem?.let { item ->
                NotificationDetailScreen(
                    item = item,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scheme.surface),
                    onBack = { viewModel.closeDetail() },
                    onOpenOrder = onOpenOrder,
                    onOpenListing = onOpenListing,
                )
            }
        }
    }
}

@Composable
private fun NotificationInboxRow(
    item: InboxNotificationItem,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FashTheme.spacing.radiusCard))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isUnread) {
                scheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                scheme.surfaceContainerLow
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            RowTitleUnread(item)
            Text(
                text = item.body,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatNotificationListTime(item.createdAtIso),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RowTitleUnread(item: InboxNotificationItem) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = item.title.ifBlank { stringResource(R.string.notification_row_no_title) },
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.isUnread) {
            Text(
                text = stringResource(R.string.notification_unread_badge),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = FashColors.Primary,
            )
        }
    }
}

private fun formatNotificationListTime(iso: String): String =
    runCatching {
        val instant = Instant.parse(iso)
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .format(instant.atZone(ZoneId.systemDefault()))
    }.getOrDefault(iso)
