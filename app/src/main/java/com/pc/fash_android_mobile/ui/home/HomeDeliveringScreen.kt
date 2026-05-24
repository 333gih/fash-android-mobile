package com.pc.fash_android_mobile.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.PrecisionManufacturing
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.order.OrderItem
import com.pc.fash_android_mobile.ui.common.stableLazyKey
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.components.FashEmptyState
import com.pc.fash_android_mobile.ui.components.FashPromoSlideDef
import com.pc.fash_android_mobile.ui.components.FashPromoSliderAdFooter
import com.pc.fash_android_mobile.ui.orders.normalizeOrderStatus
import com.pc.fash_android_mobile.ui.orders.orderStatusLabelForList
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

/**
 * Full-screen hub from Home “Đang giao”. When [AppEnvironment.shippingEnabled] is false, shows a
 * professional “coming soon” state; when true, lists the buyer’s in-transit orders (same bucket as the Home counter).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDeliveringScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeDeliveringViewModel,
    onBack: () -> Unit,
    onOrderClick: (OrderItem) -> Unit,
    /** Opens the full Orders screen (buying tab). */
    onOpenAllOrders: () -> Unit,
    /** After confirm receipt or refresh — keep Home journey counts in sync. */
    onDataMutated: () -> Unit = {},
    /** When true, keeps bottom nav visible and shows promo slider above it. */
    embeddedInMainNav: Boolean = true,
    promoSlides: List<FashPromoSlideDef> = emptyList(),
    onPromoSlideClick: (FashPromoSlideDef, Int) -> Unit = { _, _ -> },
) {
    val scheme = MaterialTheme.colorScheme
    val shippingEnabled = AppEnvironment.shippingEnabled
    val orders by viewModel.orders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val confirmingOrderId by viewModel.confirmingOrderId.collectAsState()
    val pullState = rememberPullToRefreshState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surfaceContainerLow),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.home_delivering_screen_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
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
                containerColor = scheme.surface,
                titleContentColor = scheme.onSurface,
            ),
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (!shippingEnabled) {
                ComingSoonPanel(onOpenAllOrders = onOpenAllOrders)
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        viewModel.refresh(true)
                        onDataMutated()
                    },
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
                    when {
                        isLoading && orders.isEmpty() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = FashColors.Primary)
                            }
                        }
                        loadError != null && orders.isEmpty() -> {
                            FashEmptyState(
                                icon = Icons.Outlined.ErrorOutline,
                                title = stringResource(R.string.home_delivering_error_title),
                                subtitle = loadError?.takeIf { it.isNotBlank() }
                                    ?: stringResource(R.string.home_delivering_error_subtitle),
                                modifier = Modifier.fillMaxSize(),
                                scrollable = false,
                                footer = {
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.refresh(true)
                                            onDataMutated()
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FashColors.Primary),
                                        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
                                    ) {
                                        Text(stringResource(R.string.feed_retry))
                                    }
                                },
                            )
                        }
                        orders.isEmpty() -> {
                            FashEmptyState(
                                icon = Icons.Outlined.LocalShipping,
                                title = stringResource(R.string.home_delivering_empty_title),
                                subtitle = stringResource(R.string.home_delivering_empty_subtitle),
                                modifier = Modifier.fillMaxSize(),
                                scrollable = false,
                                footer = {
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = onOpenAllOrders,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FashColors.Primary),
                                        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
                                    ) {
                                        Text(stringResource(R.string.home_delivering_cta_all_orders))
                                    }
                                },
                            )
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    horizontal = FashTheme.spacing.editorialStart,
                                    vertical = 16.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                item(key = "hint") {
                                    Text(
                                        text = stringResource(R.string.home_delivering_list_intro),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = scheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 4.dp),
                                    )
                                }
                                items(
                                    items = orders,
                                    key = { stableLazyKey(it.orderId, 0, "del") },
                                ) { order ->
                                    DeliveringOrderCard(
                                        order = order,
                                        isConfirming = confirmingOrderId == order.orderId,
                                        onConfirmReceipt = {
                                            viewModel.confirmReceipt(order.orderId)
                                            onDataMutated()
                                        },
                                        onClick = { onOrderClick(order) },
                                    )
                                }
                                item(key = "footer_cta") {
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = onOpenAllOrders,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FashColors.Primary),
                                        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
                                    ) {
                                        Text(stringResource(R.string.home_delivering_cta_all_orders))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (embeddedInMainNav) {
            FashPromoSliderAdFooter(
                modifier = Modifier.fillMaxWidth(),
                slides = promoSlides,
                onSlideClick = onPromoSlideClick,
            )
        }
    }
}

@Composable
private fun ComingSoonPanel(onOpenAllOrders: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
            color = scheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 0.dp,
        ) {
            Column(Modifier.padding(20.dp)) {
                Icon(
                    imageVector = Icons.Outlined.PrecisionManufacturing,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = FashColors.Primary,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.home_delivering_coming_soon_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_delivering_coming_soon_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = stringResource(R.string.home_delivering_coming_soon_hint),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onOpenAllOrders,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = FashColors.Primary),
            shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        ) {
            Text(stringResource(R.string.home_delivering_cta_all_orders))
        }
    }
}

@Composable
private fun DeliveringOrderCard(
    order: OrderItem,
    isConfirming: Boolean,
    onConfirmReceipt: () -> Unit,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val imageUrl = order.imageUrl.takeIf { it.isNotBlank() }?.let { resolveDeliveringImageUrl(it) }.orEmpty()

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
                    DeliveringStatusChip(status = order.status)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "@${order.sellerUsername.ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                if (order.canConfirm) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.35f))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onConfirmReceipt,
                        enabled = !isConfirming,
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FashColors.Primary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
                }
            }
        }
    }
}

@Composable
private fun DeliveringStatusChip(status: String) {
    val scheme = MaterialTheme.colorScheme
    val norm = normalizeOrderStatus(status)
    val (bg, fg) = when (norm) {
        "in_transit" -> androidx.compose.ui.graphics.Color(0xFFB3E5FC) to androidx.compose.ui.graphics.Color(0xFF1976D2)
        "payment_held" -> scheme.secondaryContainer to scheme.onSecondaryContainer
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

private fun resolveDeliveringImageUrl(path: String): String {
    if (path.isBlank()) return ""
    if (path.startsWith("http")) return path
    val base = AppEnvironment.apiBaseUrl.trimEnd('/')
    return if (path.startsWith("/")) "$base$path" else "$base/$path"
}
