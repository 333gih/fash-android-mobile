package com.pc.fash_android_mobile.ui.orders

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.components.FashEmptyState
import com.pc.fash_android_mobile.ui.components.FashPillFilterChip
import com.pc.fash_android_mobile.ui.components.FashPromoSlideDef
import com.pc.fash_android_mobile.ui.components.FashPromoSliderAdFooter
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.order.OrderItem
import com.pc.fash_android_mobile.ui.common.stableLazyKey
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

private data class OrderStatusFilterChipDef(
    val filter: OrderStatusFilter,
    val labelRes: Int,
)

private val orderStatusFilterChips: List<OrderStatusFilterChipDef> = listOf(
    OrderStatusFilterChipDef(OrderStatusFilter.ALL, R.string.orders_chip_all),
    OrderStatusFilterChipDef(OrderStatusFilter.PAYMENT_PENDING, R.string.orders_chip_payment_pending),
    OrderStatusFilterChipDef(OrderStatusFilter.PAYMENT_HELD, R.string.orders_chip_payment_held),
    OrderStatusFilterChipDef(OrderStatusFilter.IN_TRANSIT, R.string.orders_chip_in_transit),
    OrderStatusFilterChipDef(OrderStatusFilter.DELIVERED_CONFIRMED, R.string.orders_chip_delivered),
    OrderStatusFilterChipDef(OrderStatusFilter.CANCELLED, R.string.orders_chip_cancelled),
    OrderStatusFilterChipDef(OrderStatusFilter.DISPUTED, R.string.orders_chip_disputed),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    modifier: Modifier = Modifier,
    viewModel: OrdersViewModel,
    onBack: () -> Unit,
    /** When true, omits the screen [TopAppBar] — host provides main navigation chrome. */
    embeddedInMainNav: Boolean = false,
    /** Bottom promo strip — same role as chat inbox (e.g. open Explore). */
    onPromoSlideClick: (FashPromoSlideDef, Int) -> Unit = { _, _ -> },
    /** When non-null, replaces default promo slides (e.g. remote config / admin CMS). */
    promoSlides: List<FashPromoSlideDef> = emptyList(),
    onOrderClick: (OrderItem) -> Unit = {},
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val buyingOrders by viewModel.buyingOrders.collectAsState()
    val sellingOrders by viewModel.sellingOrders.collectAsState()
    val buyingFilter by viewModel.buyingStatusFilter.collectAsState()
    val sellingFilter by viewModel.sellingStatusFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val confirmingOrderId by viewModel.confirmingOrderId.collectAsState()
    val pullState = rememberPullToRefreshState()

    val currentFilter = if (selectedTab == 0) buyingFilter else sellingFilter
    val sourceOrders = if (selectedTab == 0) buyingOrders else sellingOrders
    val filteredOrders = remember(sourceOrders, currentFilter) {
        sourceOrders.filter { currentFilter.matches(it) }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.scrollOrdersToTop.collect {
            listState.animateScrollToItem(0)
        }
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        if (!embeddedInMainNav) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.orders_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.orders_back),
                        tint = FashColors.Primary,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp,
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = FashColors.Primary,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = {
                        Text(
                            text = stringResource(R.string.orders_tab_buying),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Text(
                            text = stringResource(R.string.orders_tab_selling),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }

        OrderStatusFilterBar(
            orders = sourceOrders,
            selected = currentFilter,
            onSelect = viewModel::selectStatusFilter,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                    when {
                    isLoading && buyingOrders.isEmpty() && sellingOrders.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = FashColors.Primary)
                        }
                    }
                    loadError != null && buyingOrders.isEmpty() && sellingOrders.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            FashEmptyState(
                                icon = Icons.Outlined.ErrorOutline,
                                title = stringResource(R.string.orders_error_title),
                                subtitle = loadError?.takeIf { it.isNotBlank() }
                                    ?: stringResource(R.string.orders_error_sub),
                                modifier = Modifier.fillMaxSize(),
                                scrollable = false,
                                footer = {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.retryLoad() },
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
                    else -> {
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = { viewModel.refreshOrders() },
                            modifier = Modifier.fillMaxSize(),
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
                        ) {
                            AnimatedContent(
                                targetState = selectedTab to currentFilter,
                                transitionSpec = {
                                    (fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                                        slideInVertically { it / 28 }) togetherWith
                                        (fadeOut(tween(160)) + slideOutVertically { -it / 36 })
                                },
                                label = "ordersList",
                            ) { (_, _) ->
                                when {
                                    sourceOrders.isEmpty() -> {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            OrdersEmptyHint(
                                                isBuying = selectedTab == 0,
                                            )
                                        }
                                    }
                                    filteredOrders.isEmpty() -> {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            OrdersFilteredEmptyHint(
                                                onClearFilter = {
                                                    viewModel.selectStatusFilter(OrderStatusFilter.ALL)
                                                },
                                            )
                                        }
                                    }
                                    else -> {
                                        LazyColumn(
                                            state = listState,
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(
                                                horizontal = FashTheme.spacing.editorialStart,
                                                vertical = 16.dp,
                                            ),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            itemsIndexed(
                                                items = filteredOrders,
                                                key = { index, order ->
                                                    stableLazyKey(order.orderId, index, "ord")
                                                },
                                            ) { _, order ->
                                                OrderCard(
                                                    order = order,
                                                    showReviewButton = selectedTab == 0,
                                                    isConfirming = confirmingOrderId == order.orderId,
                                                    onConfirmReceipt = { viewModel.confirmReceipt(order.orderId) },
                                                    onReview = { onOrderClick(order) },
                                                    onClick = { onOrderClick(order) },
                                                )
                                            }
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
                slides = promoSlides,
                onSlideClick = onPromoSlideClick,
            )
        }
    }
}

@Composable
private fun OrderStatusFilterBar(
    orders: List<OrderItem>,
    selected: OrderStatusFilter,
    onSelect: (OrderStatusFilter) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 1.dp,
            color = scheme.outlineVariant.copy(alpha = 0.35f),
        )
        Surface(
            color = scheme.surface,
            tonalElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scroll)
                    .padding(
                        horizontal = FashTheme.spacing.editorialStart,
                        vertical = 10.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                orderStatusFilterChips.forEach { chip ->
                    val count = countOrdersForFilter(orders, chip.filter)
                    val label = buildString {
                        append(stringResource(chip.labelRes))
                        if (count > 0) append(" ($count)")
                    }
                    FashPillFilterChip(
                        selected = chip.filter == selected,
                        onClick = { onSelect(chip.filter) },
                        label = label,
                    )
                }
            }
        }
    }
}

@Composable
private fun OrdersFilteredEmptyHint(onClearFilter: () -> Unit) {
    FashEmptyState(
        icon = Icons.Outlined.FilterAlt,
        title = stringResource(R.string.orders_empty_filtered_title),
        subtitle = stringResource(R.string.orders_empty_filtered_sub),
        modifier = Modifier.fillMaxSize(),
        scrollable = false,
        footer = {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = onClearFilter,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FashColors.Primary),
                shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
            ) {
                Text(stringResource(R.string.orders_filter_clear))
            }
        },
    )
}

@Composable
private fun OrdersEmptyHint(
    isBuying: Boolean,
) {
    FashEmptyState(
        icon = if (isBuying) Icons.Outlined.ShoppingBag else Icons.Outlined.Storefront,
        title = stringResource(
            if (isBuying) R.string.orders_empty_buying else R.string.orders_empty_selling,
        ),
        subtitle = stringResource(
            if (isBuying) R.string.orders_empty_buying_sub else R.string.orders_empty_selling_sub,
        ),
        modifier = Modifier.fillMaxSize(),
        scrollable = false,
    )
}

@Composable
private fun OrderCard(
    order: OrderItem,
    showReviewButton: Boolean,
    isConfirming: Boolean,
    onConfirmReceipt: () -> Unit,
    onReview: () -> Unit,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val imageUrl = order.imageUrl.takeIf { it.isNotBlank() }?.let { resolveImageUrl(it) }.orEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.surfaceContainerHigh),
            ) {
                if (imageUrl.isNotEmpty()) {
                    FashAsyncImage(
                        model = imageUrl,
                        contentDescription = order.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = order.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    OrderStatusBadge(status = order.status)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "@${order.sellerUsername.ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (order.canConfirm) {
                    OutlinedButton(
                        onClick = onConfirmReceipt,
                        enabled = !isConfirming,
                        modifier = Modifier.align(Alignment.End),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = FashColors.Primary,
                        ),
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp,
                        ),
                    ) {
                        if (isConfirming) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = FashColors.Primary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = stringResource(R.string.orders_confirm_received),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                } else if (showReviewButton && order.canReview) {
                    OutlinedButton(
                        onClick = onReview,
                        modifier = Modifier.align(Alignment.End),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = FashColors.Primary,
                        ),
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.orders_review),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderStatusBadge(status: String) {
    val scheme = MaterialTheme.colorScheme
    val norm = normalizeOrderStatus(status)
    val (bg, fg) = when (norm) {
        "payment_pending" -> FashColors.Primary.copy(alpha = 0.12f) to FashColors.Primary
        "payment_held" -> scheme.secondaryContainer to scheme.onSecondaryContainer
        "in_transit" -> androidx.compose.ui.graphics.Color(0xFFB3E5FC) to androidx.compose.ui.graphics.Color(0xFF1976D2)
        "delivered_confirmed" -> scheme.surfaceContainerHigh to scheme.onSurfaceVariant
        "cancelled" -> scheme.errorContainer to scheme.onErrorContainer
        "disputed" -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        "cash_meetup_open" -> androidx.compose.ui.graphics.Color(0xFFE8EAF6) to androidx.compose.ui.graphics.Color(0xFF3949AB)
        else -> scheme.surfaceContainerHigh to scheme.onSurfaceVariant
    }
    Text(
        text = orderStatusLabelForList(status),
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

private fun resolveImageUrl(path: String): String {
    if (path.isBlank()) return ""
    if (path.startsWith("http")) return path
    val base = AppEnvironment.apiBaseUrl.trimEnd('/')
    return if (path.startsWith("/")) "$base$path" else "$base/$path"
}
