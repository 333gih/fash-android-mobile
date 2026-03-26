package com.pc.fash_android_mobile.ui.orders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.order.OrderDetail
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    viewModel: OrderDetailViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onNavigateToPayment: (listingId: String, amountVnd: Long, orderId: String) -> Unit,
) {
    val detail by viewModel.detail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val isWorking by viewModel.isWorking.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showReviewDialog by remember { mutableStateOf(false) }
    var reviewRating by remember { mutableFloatStateOf(5f) }
    val scrollState = rememberScrollState()

    LaunchedEffect(orderId) {
        viewModel.load(orderId)
        scrollState.scrollTo(0)
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.order_detail_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.orders_back),
                                tint = FashColors.Primary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
                if (isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = FashColors.Primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->
        when {
            isLoading && detail == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = FashColors.Primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.order_detail_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            loadError != null && detail == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = loadError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { viewModel.load(orderId) }) {
                        Text(stringResource(R.string.feed_retry))
                    }
                }
            }
            detail != null && detail!!.orderId.equals(orderId.trim(), ignoreCase = true) -> {
                val d = detail!!
                val isBuyer = viewModel.isCurrentUserBuyer()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = FashTheme.spacing.editorialStart)
                            .padding(bottom = 120.dp),
                    ) {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 4 },
                        ) {
                            StatusHeader(status = d.status)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        ProductBlock(d = d)
                        Spacer(modifier = Modifier.height(16.dp))
                        AmountBlock(d = d)
                        Spacer(modifier = Modifier.height(16.dp))
                        PartyBlock(
                            title = stringResource(R.string.order_detail_party_buyer),
                            username = d.buyerUsername,
                            displayName = d.buyerDisplayName,
                            avatarUrl = d.buyerAvatarUrl,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        PartyBlock(
                            title = stringResource(R.string.order_detail_party_seller),
                            username = d.sellerUsername,
                            displayName = d.sellerDisplayName,
                            avatarUrl = d.sellerAvatarUrl,
                        )
                        if (d.trackingNumber.isNotBlank() || d.carrier.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            TrackingBlock(d = d)
                        }
                    }
                    AnimatedVisibility(
                        visible = true,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 2 },
                        exit = fadeOut() + slideOutVertically { it / 2 },
                    ) {
                        ActionBar(
                            d = d,
                            isBuyer = isBuyer,
                            isWorking = isWorking,
                            onPay = {
                                onNavigateToPayment(d.listingId, d.amountVnd, d.orderId)
                            },
                            onConfirm = { viewModel.confirmReceipt(d.orderId) },
                            onReview = { showReviewDialog = true },
                        )
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = FashColors.Primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.order_detail_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (
        showReviewDialog &&
        detail != null &&
        detail!!.orderId.equals(orderId.trim(), ignoreCase = true)
    ) {
        val d = detail!!
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = { Text(stringResource(R.string.order_detail_review_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.order_detail_review_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Slider(
                        value = reviewRating,
                        onValueChange = { reviewRating = it },
                        valueRange = 1f..5f,
                        steps = 3,
                    )
                    Text(
                        text = "${reviewRating.roundToInt()} / 5",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FashColors.Primary,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReviewDialog = false
                        viewModel.submitReview(d.orderId, reviewRating.roundToInt(), null)
                    },
                ) {
                    Text(stringResource(R.string.order_detail_review_submit))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) {
                    Text(stringResource(R.string.create_listing_cancel))
                }
            },
        )
    }
}

@Composable
private fun StatusHeader(status: String) {
    val scheme = MaterialTheme.colorScheme
    val label = orderStatusLabel(status)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FashTheme.spacing.radiusCard))
            .background(scheme.surface)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.order_detail_status_caption),
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = FashColors.Primary,
        )
    }
}

@Composable
private fun orderStatusLabel(status: String): String = when (status.lowercase()) {
    "payment_pending" -> stringResource(R.string.order_status_payment_pending)
    "payment_held" -> stringResource(R.string.order_status_payment_held)
    "in_transit" -> stringResource(R.string.order_status_in_transit)
    "delivered_confirmed" -> stringResource(R.string.order_status_delivered_confirmed)
    "cancelled" -> stringResource(R.string.order_status_cancelled)
    "disputed" -> stringResource(R.string.order_status_disputed)
    else -> if (status.isBlank()) stringResource(R.string.order_status_unknown) else status
}

@Composable
private fun ProductBlock(d: OrderDetail) {
    val scheme = MaterialTheme.colorScheme
    val imageUrl = d.listingImageUrl.takeIf { it.isNotBlank() }?.let { resolveImageUrl(it) }.orEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FashTheme.spacing.radiusCard))
            .background(scheme.surface)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.order_detail_product),
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(scheme.surfaceContainerHigh),
            ) {
                if (imageUrl.isNotEmpty()) {
                    FashAsyncImage(
                        model = imageUrl,
                        contentDescription = d.listingTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = d.listingTitle.ifBlank { "—" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = d.listingStatus,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AmountBlock(d: OrderDetail) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FashTheme.spacing.radiusCard))
            .background(scheme.surface)
            .padding(16.dp),
    ) {
        MoneyRow(stringResource(R.string.order_detail_amount), d.amountVnd)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = scheme.outlineVariant.copy(alpha = 0.3f))
        MoneyRow(stringResource(R.string.order_detail_platform_fee), d.platformFeeVnd)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = scheme.outlineVariant.copy(alpha = 0.3f))
        MoneyRow(stringResource(R.string.order_detail_seller_payout), d.sellerPayoutVnd)
    }
}

@Composable
private fun MoneyRow(label: String, vnd: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatOrderPrice(vnd),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PartyBlock(
    title: String,
    username: String,
    displayName: String,
    avatarUrl: String,
) {
    val scheme = MaterialTheme.colorScheme
    val url = avatarUrl.takeIf { it.isNotBlank() }?.let { resolveImageUrl(it) }.orEmpty()
    val name = displayName.ifBlank { "@$username" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FashTheme.spacing.radiusCard))
            .background(scheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.width(88.dp),
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(scheme.surfaceContainerHigh),
        ) {
            if (url.isNotEmpty()) {
                FashAsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (username.isNotBlank()) {
                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TrackingBlock(d: OrderDetail) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FashTheme.spacing.radiusCard))
            .background(scheme.surface)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.order_detail_tracking),
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = d.carrier.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = d.trackingNumber.ifBlank { stringResource(R.string.order_detail_tracking_empty) },
            style = MaterialTheme.typography.bodySmall,
            color = FashColors.Primary,
        )
    }
}

@Composable
private fun ActionBar(
    d: OrderDetail,
    isBuyer: Boolean,
    isWorking: Boolean,
    onPay: () -> Unit,
    onConfirm: () -> Unit,
    onReview: () -> Unit,
) {
    val statusNorm = d.status.trim().lowercase()
    val showPay = isBuyer && statusNorm == "payment_pending"
    val showConfirm = isBuyer && d.canConfirm
    val showReview = isBuyer && d.canReview
    if (!showPay && !showConfirm && !showReview) {
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedVisibility(visible = showPay) {
            ScalePressButton(
                text = stringResource(R.string.order_detail_pay),
                enabled = !isWorking,
                onClick = onPay,
            )
        }
        AnimatedVisibility(visible = showConfirm) {
            OutlinedButton(
                onClick = onConfirm,
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FashColors.Primary),
            ) {
                if (isWorking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = FashColors.Primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.orders_confirm_received))
            }
        }
        AnimatedVisibility(visible = showReview) {
            OutlinedButton(
                onClick = onReview,
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FashColors.Primary),
            ) {
                Text(stringResource(R.string.orders_review))
            }
        }
    }
}

@Composable
private fun ScalePressButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(90),
        label = "orderPayScale",
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = FashColors.Primary,
            contentColor = FashColors.OnPrimary,
        ),
        shape = RoundedCornerShape(12.dp),
        interactionSource = interaction,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
    }
}

private fun resolveImageUrl(path: String): String {
    if (path.startsWith("http")) return path
    val base = AppEnvironment.apiBaseUrl.trimEnd('/')
    return if (path.startsWith("/")) "$base$path" else "$base/$path"
}

private fun formatOrderPrice(vnd: Long): String =
    "đ ${"%,d".format(vnd).replace(',', '.')}"
