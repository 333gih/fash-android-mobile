package com.pc.fash_android_mobile.ui.orders

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.order.OrderItem
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    modifier: Modifier = Modifier,
    viewModel: OrdersViewModel,
    onBack: () -> Unit,
    onSearchClick: () -> Unit = {},
    onOrderClick: (OrderItem) -> Unit = {},
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val buyingOrders by viewModel.buyingOrders.collectAsState()
    val sellingOrders by viewModel.sellingOrders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val confirmingOrderId by viewModel.confirmingOrderId.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadOrders()
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
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
            actions = {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_label),
                        tint = FashColors.Primary,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )

        OrdersTabs(
            selectedTab = selectedTab,
            onTabSelected = viewModel::selectTab,
        )

        when {
            isLoading && buyingOrders.isEmpty() && sellingOrders.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = FashColors.Primary)
                }
            }
            loadError != null && buyingOrders.isEmpty() && sellingOrders.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = loadError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { viewModel.retryLoad() },
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = FashColors.Primary,
                        ),
                    ) {
                        Text(stringResource(R.string.feed_retry))
                    }
                }
            }
            else -> {
                val items = if (selectedTab == 0) buyingOrders else sellingOrders
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = FashTheme.spacing.editorialStart,
                        vertical = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items, key = { it.orderId }) { order ->
                        OrderCard(
                            order = order,
                            isConfirming = confirmingOrderId == order.orderId,
                            onConfirmReceipt = { viewModel.confirmReceipt(order.orderId) },
                            onReview = { /* TODO: open review screen */ },
                            onClick = { onOrderClick(order) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surface)
            .padding(horizontal = FashTheme.spacing.editorialStart),
    ) {
        listOf(
            R.string.orders_tab_buying,
            R.string.orders_tab_selling,
        ).forEachIndexed { index, resId ->
            val selected = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 12.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(resId),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) FashColors.Primary else scheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(FashColors.Primary),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: OrderItem,
    isConfirming: Boolean,
    onConfirmReceipt: () -> Unit,
    onReview: () -> Unit,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val imageUrl = order.imageUrl.takeIf { it.isNotBlank() }?.let { resolveImageUrl(it) }.orEmpty()
    val isDelivering = order.status in listOf("delivering", "shipped", "shipping")
    val isCompleted = order.status == "completed"

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
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
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
                    StatusBadge(
                        isDelivering = isDelivering,
                        isCompleted = isCompleted,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "@${order.sellerUsername.ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatPrice(order.priceVnd),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = FashColors.Primary,
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
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
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
                } else if (order.canReview) {
                    OutlinedButton(
                        onClick = onReview,
                        modifier = Modifier.align(Alignment.End),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = FashColors.Primary,
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
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
private fun StatusBadge(
    isDelivering: Boolean,
    isCompleted: Boolean,
) {
    val (textRes, bgColor, textColor) = when {
        isDelivering -> Triple(R.string.orders_status_delivering, androidx.compose.ui.graphics.Color(0xFFB3E5FC), androidx.compose.ui.graphics.Color(0xFF1976D2))
        isCompleted -> Triple(R.string.orders_status_completed, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
        else -> Triple(R.string.orders_status_pending, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

private fun resolveImageUrl(path: String): String {
    if (path.isBlank()) return ""
    if (path.startsWith("http")) return path
    val base = AppEnvironment.apiBaseUrl.trimEnd('/')
    return if (path.startsWith("/")) "$base$path" else "$base/$path"
}

private fun formatPrice(vnd: Long): String =
    "đ ${"%,d".format(vnd).replace(',', '.')}"
