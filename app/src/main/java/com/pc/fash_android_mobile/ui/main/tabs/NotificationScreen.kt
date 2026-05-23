package com.pc.fash_android_mobile.ui.main.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.pc.fash_android_mobile.data.user.InboxNotificationItem
import com.pc.fash_android_mobile.data.user.NotificationGroupSummaryItem
import com.pc.fash_android_mobile.ui.notifications.notificationGroupHasActivity
import com.pc.fash_android_mobile.ui.notifications.notificationGroupIcon
import com.pc.fash_android_mobile.ui.notifications.notificationGroupSubtitleRes
import com.pc.fash_android_mobile.ui.notifications.notificationGroupTitleRes
import com.pc.fash_android_mobile.ui.notifications.notificationPayloadIcon
import com.pc.fash_android_mobile.ui.notifications.parseAppPromoCampaignFromInbox
import com.pc.fash_android_mobile.ui.notifications.parseNotificationDetailActions
import com.pc.fash_android_mobile.ui.common.stableLazyKey
import com.pc.fash_android_mobile.ui.components.FashEmptyBulletTipLine
import com.pc.fash_android_mobile.ui.components.FashEmptyState
import com.pc.fash_android_mobile.ui.components.FashPromoSlideDef
import com.pc.fash_android_mobile.ui.components.FashPromoSliderAdFooter
import com.pc.fash_android_mobile.ui.components.FashSnackbarHost
import com.pc.fash_android_mobile.ui.notifications.NotificationDetailScreen
import com.pc.fash_android_mobile.ui.notifications.NotificationsViewModel
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    onPromoSlideClick: (FashPromoSlideDef, Int) -> Unit = { _, _ -> },
    promoSlides: List<FashPromoSlideDef> = emptyList(),
    onOpenOrder: (String) -> Unit = {},
    onOpenListing: (String, String?) -> Unit = { _, _ -> },
    onOpenChat: (String) -> Unit = {},
    onOpenFollowConnections: (Int) -> Unit = {},
    onOpenExplore: () -> Unit = {},
    onOpenInviteFriends: () -> Unit = {},
    onPromoMainTab: (com.pc.fash_android_mobile.ui.main.MainTab) -> Unit = {},
    onPromoOpenOrders: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val items by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val loadMoreBusy by viewModel.loadMoreBusy.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val inboxUnavailable by viewModel.inboxUnavailable.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val markAllReadBusy by viewModel.markAllReadBusy.collectAsState()
    val selectedDetailId by viewModel.selectedDetailId.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val pullState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val canMarkAllRead = !inboxUnavailable &&
        !markAllReadBusy &&
        selectedGroup != null &&
        (items.any { it.isUnread } || (groups.find { it.group == selectedGroup }?.unreadCount ?: 0) > 0)

    DisposableEffect(Unit) {
        onDispose { viewModel.closeDetail() }
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
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
                // Match OrdersScreen: do not reserve bottom system bar in content padding — the promo
                // footer (FashBottomPromoAdStrip edgeToEdge) applies navigation-bar padding internally.
                contentWindowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
                ),
                snackbarHost = { FashSnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (selectedGroup != null) {
                                    stringResource(notificationGroupTitleRes(selectedGroup!!))
                                } else {
                                    stringResource(R.string.notifications)
                                },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = scheme.onSurface,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (selectedGroup != null) {
                                    viewModel.closeGroup()
                                } else {
                                    onBack()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back),
                                    tint = FashColors.Primary,
                                )
                            }
                        },
                        actions = {
                            if (selectedGroup != null) {
                                if (markAllReadBusy) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = FashTheme.spacing.editorialStart - 4.dp)
                                            .size(48.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = FashColors.Primary,
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                } else {
                                    TextButton(
                                        onClick = { viewModel.markAllRead() },
                                        enabled = canMarkAllRead,
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = FashColors.Primary,
                                            disabledContentColor = scheme.onSurface.copy(alpha = 0.38f),
                                        ),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.notification_mark_all_read),
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = scheme.surface,
                            titleContentColor = scheme.onSurface,
                        ),
                    )
                },
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                            when {
                                isLoading && (if (selectedGroup == null) groups.isEmpty() else items.isEmpty()) -> {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = FashColors.Primary)
                                    }
                                }
                                loadError != null && (if (selectedGroup == null) groups.isEmpty() else items.isEmpty()) -> {
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
                                selectedGroup == null && groups.all { it.latestId == null && it.unreadCount == 0 } -> {
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
                                selectedGroup != null && items.isEmpty() -> {
                                    FashEmptyState(
                                        icon = notificationGroupIcon(selectedGroup!!),
                                        title = stringResource(R.string.notification_group_empty_title),
                                        subtitle = stringResource(notificationGroupSubtitleRes(selectedGroup!!)),
                                        modifier = Modifier.fillMaxSize(),
                                        contentDescription = null,
                                    )
                                }
                                selectedGroup == null -> {
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
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(
                                                horizontal = FashTheme.spacing.editorialStart,
                                                vertical = 8.dp,
                                            ),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            items(
                                                items = groups,
                                                key = { stableLazyKey(it.group, 0, "ng") },
                                            ) { row ->
                                                NotificationGroupRow(
                                                    item = row,
                                                    onClick = { viewModel.openGroup(row.group) },
                                                )
                                            }
                                        }
                                    }
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

                    FashPromoSliderAdFooter(
                        modifier = Modifier.fillMaxWidth(),
                        onExploreClick = onExploreClick,
                        slides = promoSlides,
                        onSlideClick = onPromoSlideClick,
                        edgeToEdgeAdStrip = true,
                    )
                }
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
                    onOpenChat = onOpenChat,
                    onOpenFollowConnections = onOpenFollowConnections,
                    onOpenExplore = onOpenExplore,
                    onOpenInviteFriends = onOpenInviteFriends,
                    onPromoMainTab = onPromoMainTab,
                    onPromoOpenOrders = onPromoOpenOrders,
                )
            }
        }
    }
}

/** Compact uniform height so all 7 groups fit on one screen. */
private val NotificationGroupRowHeight = 64.dp

@Composable
private fun NotificationGroupRow(
    item: NotificationGroupSummaryItem,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val preview = item.latestBody?.takeIf { it.isNotBlank() }
        ?: item.latestTitle?.takeIf { it.isNotBlank() }
        ?: stringResource(notificationGroupSubtitleRes(item.group))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(NotificationGroupRowHeight)
            .clip(RoundedCornerShape(FashTheme.spacing.radiusCard))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = notificationGroupIcon(item.group),
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(20.dp),
                ) {
                    Text(
                        text = stringResource(notificationGroupTitleRes(item.group)),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (item.unreadCount > 0) {
                        Text(
                            text = item.unreadCount.coerceAtMost(99).toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onPrimary,
                            modifier = Modifier
                                .background(FashColors.Primary, RoundedCornerShape(999.dp))
                                .padding(horizontal = 7.dp, vertical = 1.dp),
                        )
                    }
                }
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
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
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = notificationPayloadIcon(item.payloadType),
                contentDescription = null,
                tint = FashColors.Primary,
                modifier = Modifier.size(24.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
            notificationRowImageUrl(item)?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

private fun notificationRowImageUrl(item: InboxNotificationItem): String? {
    parseAppPromoCampaignFromInbox(item)?.remoteImageUrls?.firstOrNull()?.let { return it }
    return parseNotificationDetailActions(item).imageUrl?.takeIf { it.isNotBlank() }
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
