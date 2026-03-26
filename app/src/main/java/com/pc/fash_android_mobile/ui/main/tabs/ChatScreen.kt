package com.pc.fash_android_mobile.ui.main.tabs

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.common.stableLazyKey
import com.pc.fash_android_mobile.data.chat.ConversationItem
import com.pc.fash_android_mobile.data.chat.ConversationListingGroup
import com.pc.fash_android_mobile.ui.chat.ChatFilter
import com.pc.fash_android_mobile.ui.chat.ChatViewModel
import com.pc.fash_android_mobile.ui.chat.SellerInboxGroupMode
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

private val AvatarSize = 48.dp
private val ProductThumbSize = 56.dp
private val ChipCorner = RoundedCornerShape(20.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel,
    onConversationClick: (ConversationItem) -> Unit = {},
) {
    val conversations by viewModel.conversations.collectAsState()
    val displayGroups by viewModel.displayGroups.collectAsState()
    val expandedGroupIds by viewModel.expandedGroupListingIds.collectAsState()
    val sellerHasActiveListings by viewModel.sellerHasActiveListings.collectAsState()
    val sellerInboxGroupMode by viewModel.sellerInboxGroupMode.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val scheme = MaterialTheme.colorScheme
    val pullState = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        viewModel.loadConversations()
    }

    val showGroupedInbox =
        sellerHasActiveListings && sellerInboxGroupMode == SellerInboxGroupMode.ByProduct

    Column(modifier = modifier.fillMaxSize()) {
        if (sellerHasActiveListings) {
            SellerInboxSegmentRow(
                mode = sellerInboxGroupMode,
                onModeChange = viewModel::setSellerInboxGroupMode,
            )
        }
        FilterBar(
            selectedFilter = selectedFilter,
            onFilterClick = viewModel::setFilter,
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = pullState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = isRefreshing,
                    color = FashColors.Primary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
        ) { when {
            isLoading -> ConversationSkeletonList()
            loadError != null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(FashTheme.spacing.editorialStart),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = loadError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = { viewModel.loadConversations() }) {
                        Text(stringResource(R.string.chat_retry))
                    }
                }
            }
            showGroupedInbox && displayGroups.isEmpty() -> EmptyInboxHint()
            !showGroupedInbox && conversations.isEmpty() -> EmptyInboxHint()
            showGroupedInbox -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                displayGroups.forEach { group ->
                    item(key = "h-group-${group.listingId}") {
                        ListingGroupHeader(
                            group = group,
                            expanded = group.listingId in expandedGroupIds,
                            formatPrice = viewModel::formatPriceVnd,
                            onToggle = { viewModel.toggleListingGroupExpanded(group.listingId) },
                        )
                    }
                    if (group.listingId in expandedGroupIds) {
                        items(
                            count = group.conversations.size,
                            key = { idx -> "${group.listingId}-${group.conversations[idx].conversationId}" },
                        ) { idx ->
                            val item = group.conversations[idx]
                            ConversationRow(
                                item = item,
                                formatTimestamp = viewModel::formatTimestamp,
                                onClick = { onConversationClick(item) },
                            )
                        }
                    }
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                itemsIndexed(
                    conversations,
                    key = { index, item -> stableLazyKey(item.conversationId, index, "conv") },
                ) { _, item ->
                    ConversationRow(
                        item = item,
                        formatTimestamp = viewModel::formatTimestamp,
                        onClick = { onConversationClick(item) },
                    )
                }
            }
        } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    onMenuClick: () -> Unit = { },
    onSearchClick: () -> Unit = { },
) {
    androidx.compose.material3.TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.chat_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_label),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
private fun EmptyInboxHint() {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 40.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(FashColors.Primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = FashColors.Primary,
                )
            }
            Text(
                text = stringResource(R.string.chat_empty),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.chat_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SellerInboxSegmentRow(
    mode: SellerInboxGroupMode,
    onModeChange: (SellerInboxGroupMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SellerSegmentChip(
            selected = mode == SellerInboxGroupMode.AllConversations,
            label = stringResource(R.string.chat_filter_all),
            onClick = { onModeChange(SellerInboxGroupMode.AllConversations) },
        )
        SellerSegmentChip(
            selected = mode == SellerInboxGroupMode.ByProduct,
            label = stringResource(R.string.chat_inbox_by_product),
            onClick = { onModeChange(SellerInboxGroupMode.ByProduct) },
        )
    }
}

@Composable
private fun SellerSegmentChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = ChipCorner,
        color = if (selected) FashColors.Primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .clip(ChipCorner)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = if (selected) FashColors.OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun ListingGroupHeader(
    group: ConversationListingGroup,
    expanded: Boolean,
    formatPrice: (Long) -> String,
    onToggle: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val thumb = group.coverImageUrl.takeIf { it.isNotBlank() }?.let { resolveImageUrl(it) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(scheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (thumb != null) {
                FashAsyncImage(
                    model = thumb,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.title.ifBlank { "—" },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatPrice(group.priceVnd),
                style = MaterialTheme.typography.labelMedium,
                color = FashColors.Primary,
            )
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = FashColors.Primary.copy(alpha = 0.12f),
        ) {
            Text(
                text = group.conversationCountBadge.coerceAtLeast(0).toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = FashColors.Primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FilterBar(
    selectedFilter: ChatFilter,
    onFilterClick: (ChatFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            ChatFilter.All to R.string.chat_filter_all,
            ChatFilter.Unread to R.string.chat_filter_unread,
            ChatFilter.Seller to R.string.chat_filter_seller,
            ChatFilter.Buyer to R.string.chat_filter_buyer,
        ).forEach { (filter, labelRes) ->
            val selected = filter == selectedFilter
            Surface(
                shape = ChipCorner,
                color = if (selected) FashColors.Primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .clip(ChipCorner)
                    .clickable { onFilterClick(filter) },
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (selected) FashColors.OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun ConversationRow(
    item: ConversationItem,
    formatTimestamp: (String) -> String,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val avatarUrl = item.avatarUrl.takeIf { it.isNotBlank() }?.let { resolveImageUrl(it) }
    val thumbUrl = item.productThumbnailUrl.takeIf { it.isNotBlank() }?.let { resolveImageUrl(it) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(AvatarSize)) {
            Box(
                modifier = Modifier
                    .size(AvatarSize)
                    .clip(CircleShape)
                    .background(scheme.surfaceContainerHigh),
            ) {
                if (avatarUrl != null) {
                    FashAsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            if (item.isUnread) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(12.dp)
                        .background(FashColors.Primary, CircleShape)
                        .padding(2.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.displayName.ifBlank { "@${item.username.ifBlank { "user" }}" },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (item.isUnread) FontWeight.ExtraBold else FontWeight.Bold,
                    ),
                    color = if (item.isUnread) scheme.onSurface else scheme.onSurface.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = formatTimestamp(item.timestamp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (item.isUnread) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (item.isUnread) FashColors.Primary else scheme.onSurfaceVariant,
                )
            }
            Text(
                text = item.lastMessageText.ifBlank { " " },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (item.isUnread) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = if (item.isUnread) scheme.onSurface else scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(ProductThumbSize)
                .clip(RoundedCornerShape(8.dp))
                .background(scheme.surfaceContainerHigh),
        ) {
            if (thumbUrl != null) {
                FashAsyncImage(
                    model = thumbUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun ConversationSkeletonList() {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceContainerHigh,
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.surfaceContainerHigh,
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim),
    )

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(7) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(AvatarSize)
                        .clip(CircleShape)
                        .background(brush),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(ProductThumbSize)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush),
                )
            }
        }
    }
}

private fun resolveImageUrl(path: String): String {
    if (path.startsWith("http")) return path
    val base = com.pc.fash_android_mobile.config.AppEnvironment.apiBaseUrl.trimEnd('/')
    return if (path.startsWith("/")) "$base$path" else "$base/$path"
}
