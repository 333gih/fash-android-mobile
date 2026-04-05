package com.pc.fash_android_mobile.ui.checkout

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.components.FashSnackbarHost
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.address.ShippingAddress
import com.pc.fash_android_mobile.data.order.OrderDetail
import com.pc.fash_android_mobile.data.listing.ListingDetail
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.fashReadableOn
import com.pc.fash_android_mobile.ui.theme.FashTheme

private val SectionCorner = RoundedCornerShape(16.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    modifier: Modifier = Modifier,
    listingId: String,
    overridePriceVnd: Long = 0L,
    /** When non-null, the order row already exists (navigated from order detail). */
    existingOrderId: String? = null,
    viewModel: CheckoutViewModel,
    onBack: () -> Unit,
    /** Invoked with the paid order id after core reports **payment_held** (or equivalent). */
    onSuccess: (orderId: String) -> Unit,
) {
    val context = LocalContext.current
    val detail by viewModel.detail.collectAsState()
    val fullName by viewModel.fullName.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val address by viewModel.address.collectAsState()
    val district by viewModel.district.collectAsState()
    val city by viewModel.city.collectAsState()
    val selectedPaymentIndex by viewModel.selectedPaymentIndex.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val orderDetail by viewModel.orderDetail.collectAsState()
    val shippingAddressForDisplay by viewModel.shippingAddressForDisplay.collectAsState()
    val awaitingGateway by viewModel.awaitingGatewayReturn.collectAsState()
    val isCancelling by viewModel.isCancelling.collectAsState()
    val scheme = MaterialTheme.colorScheme
    var showCancelConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val canCancelPendingOrder =
        existingOrderId != null &&
            orderDetail?.status?.trim()?.equals("payment_pending", ignoreCase = true) == true

    LaunchedEffect(listingId, overridePriceVnd, existingOrderId) {
        viewModel.loadListing(listingId, overridePriceVnd, existingOrderId)
    }

    LaunchedEffect(Unit) {
        viewModel.paymentUiEvents.collect { event ->
            when (event) {
                is PaymentUiEvent.OpenPaymentUrl -> runCatching {
                    CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .build()
                        .launchUrl(context, Uri.parse(event.url))
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }

    BackHandler { onBack() }

    when {
        isLoading && detail == null -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = FashColors.Primary)
        }
        loadError != null && detail == null -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(loadError!!, color = scheme.onSurfaceVariant)
                OutlinedButton(onClick = { viewModel.loadListing(listingId, overridePriceVnd, existingOrderId) }) {
                    Text(stringResource(R.string.chat_retry))
                }
            }
        }
        detail != null -> {
            val d = detail!!
            val canPay = viewModel.canSubmit() && !awaitingGateway
            Box(modifier = modifier.fillMaxSize()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { FashSnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = stringResource(R.string.checkout_title),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.onSurface,
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
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
                    bottomBar = {
                        CheckoutBottomBar(
                            grandTotalVnd = viewModel.grandTotalVnd,
                            isSubmitting = isSubmitting,
                            awaitingGateway = awaitingGateway,
                            enabled = canPay,
                            onClick = { viewModel.submitPayment(onSuccess) },
                            showCancelPendingOrder = canCancelPendingOrder,
                            onCancelPendingOrder = { showCancelConfirm = true },
                            isCancelling = isCancelling,
                        )
                    },
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                    ) {
                        val negotiated =
                            overridePriceVnd > 0L || orderDetail != null
                        ProductCheckoutCard(
                            detail = d,
                            order = orderDetail,
                            displayPriceVnd = viewModel.productPriceVnd,
                            negotiatedPrice = negotiated,
                        )
                        orderDetail?.let { od ->
                            Spacer(modifier = Modifier.height(12.dp))
                            OrderSnapshotCard(order = od)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        ShippingReadOnlySection(
                            fullName = fullName,
                            phone = phone,
                            addressLine = address,
                            district = district,
                            city = city,
                            canSubmit = viewModel.canSubmit(),
                            savedAddress = shippingAddressForDisplay,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        PaymentMethodSection(
                            methods = viewModel.paymentMethods,
                            selectedIndex = selectedPaymentIndex,
                            onSelect = viewModel::selectPaymentMethod,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        OrderSummarySection(
                            productPriceVnd = viewModel.productPriceVnd,
                            shippingFeeVnd = viewModel.shippingFeeVnd,
                            discountVnd = viewModel.discountVnd,
                            grandTotalVnd = viewModel.grandTotalVnd,
                            sellerPayoutVnd = viewModel.sellerPayoutVnd,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                CheckoutProcessingOverlay(visible = isSubmitting || isCancelling)
            }
        }
    }

    if (showCancelConfirm && detail != null && existingOrderId != null) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text(stringResource(R.string.order_cancel_confirm_title)) },
            text = { Text(stringResource(R.string.order_cancel_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelConfirm = false
                        viewModel.cancelPendingOrder(onBack)
                    },
                    enabled = !isCancelling,
                ) {
                    Text(stringResource(R.string.order_cancel_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) {
                    Text(stringResource(R.string.order_cancel_confirm_dismiss))
                }
            },
        )
    }
}

@Composable
private fun ProductCheckoutCard(
    detail: ListingDetail,
    order: OrderDetail?,
    displayPriceVnd: Long,
    negotiatedPrice: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val imageUrl = order?.listingImageUrl?.takeIf { it.isNotBlank() }?.let { resolveImageUrl(it) }
        ?: detail.imageUrls.firstOrNull()?.takeIf { it.isNotBlank() }?.let { resolveImageUrl(it) }
    val title = order?.listingTitle?.takeIf { it.isNotBlank() } ?: detail.title.ifBlank { "—" }
    val listed = detail.priceVnd
    val showListedCompare = listed > 0L && listed != displayPriceVnd

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart),
        shape = SectionCorner,
        color = scheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.checkout_order_overview),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(scheme.surfaceContainerHigh),
                ) {
                    if (imageUrl != null) {
                        FashAsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "@${detail.sellerUsername ?: order?.sellerUsername ?: "—"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = formatPrice(displayPriceVnd),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = FashColors.Primary,
                        )
                        if (negotiatedPrice) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = FashColors.Primary.copy(alpha = 0.12f),
                            ) {
                                Text(
                                    text = stringResource(R.string.checkout_price_deal),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FashColors.Primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    if (showListedCompare) {
                        Text(
                            text = "${stringResource(R.string.checkout_price_listed)}: ${formatPrice(listed)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough,
                        )
                    }
                }
            }
            ListingMetaLines(detail = detail)
        }
    }
}

@Composable
private fun ListingMetaLines(detail: ListingDetail) {
    val scheme = MaterialTheme.colorScheme
    val lines = listOfNotNull(
        detail.condition.takeIf { it.isNotBlank() }?.let {
            stringResource(R.string.create_listing_condition_label) to it
        },
        detail.size?.takeIf { it.isNotBlank() }?.let {
            stringResource(R.string.product_size) to it
        },
        detail.brand?.takeIf { it.isNotBlank() }?.let {
            stringResource(R.string.product_brand) to it
        },
    )
    if (lines.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.58f))
        lines.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun OrderSnapshotCard(order: OrderDetail) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart),
        shape = SectionCorner,
        color = scheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.checkout_section_order),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.checkout_order_id_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = shortOrderId(order.orderId),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = scheme.onSurface,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.order_detail_status_caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = checkoutOrderStatusLabel(order.status),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = FashColors.Primary,
                )
            }
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.58f))
            Text(
                text = stringResource(R.string.checkout_section_parties),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurfaceVariant,
            )
            PartyRow(
                label = stringResource(R.string.order_detail_party_seller),
                username = order.sellerUsername,
                display = order.sellerDisplayName,
            )
            PartyRow(
                label = stringResource(R.string.order_detail_party_buyer),
                username = order.buyerUsername,
                display = order.buyerDisplayName,
            )
        }
    }
}

@Composable
private fun PartyRow(label: String, username: String, display: String) {
    val scheme = MaterialTheme.colorScheme
    val handle = username.takeIf { it.isNotBlank() }?.let { "@$it" }.orEmpty()
    val name = display.takeIf { it.isNotBlank() } ?: handle.ifBlank { "—" }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
        )
        Text(
            text = if (handle.isNotBlank() && name != handle) "$name · $handle" else name,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = scheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun checkoutOrderStatusLabel(status: String): String = when (status.lowercase()) {
    "payment_pending" -> stringResource(R.string.order_status_payment_pending)
    "payment_held" -> stringResource(R.string.order_status_payment_held)
    "in_transit" -> stringResource(R.string.order_status_in_transit)
    "delivered_confirmed" -> stringResource(R.string.order_status_delivered_confirmed)
    "cancelled" -> stringResource(R.string.order_status_cancelled)
    "disputed" -> stringResource(R.string.order_status_disputed)
    else -> status.ifBlank { stringResource(R.string.order_status_unknown) }
}

private fun shortOrderId(id: String): String {
    val t = id.trim()
    if (t.length <= 14) return t
    return "${t.take(6)}…${t.takeLast(4)}"
}

@Composable
private fun ShippingReadOnlySection(
    fullName: String,
    phone: String,
    addressLine: String,
    district: String,
    city: String,
    canSubmit: Boolean,
    savedAddress: ShippingAddress?,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart),
    ) {
        Text(
            text = stringResource(R.string.checkout_address_label),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = scheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.checkout_shipping_readonly_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        AnimatedVisibility(
            visible = !canSubmit,
            enter = fadeIn(tween(280)),
            exit = fadeOut(tween(180)),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = scheme.errorContainer.copy(alpha = 0.35f),
            ) {
                Text(
                    text = stringResource(R.string.checkout_missing_address_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = SectionCorner,
            color = scheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ShippingReadOnlyRow(
                        label = stringResource(R.string.checkout_full_name),
                        value = fullName.ifBlank { "—" },
                        modifier = Modifier.weight(1f),
                    )
                    if (savedAddress?.isDefault == true) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = FashColors.Primary.copy(alpha = 0.12f),
                        ) {
                            Text(
                                text = stringResource(R.string.address_badge_default),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = FashColors.Primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                if (savedAddress?.label?.isNotBlank() == true) {
                    ShippingReadOnlyRow(
                        label = stringResource(R.string.address_field_label_optional),
                        value = savedAddress.label,
                    )
                }
                ShippingReadOnlyRow(
                    label = stringResource(R.string.checkout_phone),
                    value = phone.ifBlank { "—" },
                )
                ShippingReadOnlyRow(
                    label = stringResource(R.string.checkout_address),
                    value = addressLine.ifBlank { "—" },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ShippingReadOnlyRow(
                        label = stringResource(R.string.checkout_district),
                        value = district.ifBlank { "—" },
                        modifier = Modifier.weight(1f),
                    )
                    ShippingReadOnlyRow(
                        label = stringResource(R.string.checkout_city),
                        value = city.ifBlank { "—" },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShippingReadOnlyRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = scheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CheckoutBottomBar(
    grandTotalVnd: Long,
    isSubmitting: Boolean,
    awaitingGateway: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    showCancelPendingOrder: Boolean,
    onCancelPendingOrder: () -> Unit,
    isCancelling: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = scheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            if (showCancelPendingOrder && !awaitingGateway) {
                OutlinedButton(
                    onClick = onCancelPendingOrder,
                    enabled = !isCancelling && !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, scheme.error.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.error),
                ) {
                    Text(
                        stringResource(R.string.order_cancel_order),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (awaitingGateway) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = scheme.primaryContainer.copy(alpha = 0.35f),
                ) {
                    Text(
                        text = stringResource(R.string.checkout_awaiting_gateway),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = FashTheme.spacing.editorialStart, vertical = 10.dp),
                    )
                }
            }
            CheckoutButton(
                grandTotalVnd = grandTotalVnd,
                isSubmitting = isSubmitting,
                enabled = enabled,
                onClick = onClick,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 0.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.checkout_secured_fash_pay),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CheckoutProcessingOverlay(visible: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val infinite = rememberInfiniteTransition(label = "checkoutPulse")
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(180)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.48f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                color = scheme.surfaceContainerHigh,
                modifier = Modifier.padding(horizontal = 28.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CircularProgressIndicator(
                        color = FashColors.Primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(44.dp),
                    )
                    Text(
                        text = stringResource(R.string.checkout_processing_overlay),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer { alpha = pulseAlpha },
                    )
                    Text(
                        text = stringResource(R.string.checkout_processing_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodSection(
    methods: List<PaymentMethodOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart),
    ) {
        Text(
            text = stringResource(R.string.checkout_payment_method_label),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = scheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = scheme.surfaceContainerHighest.copy(alpha = 0.6f),
        ) {
            Text(
                text = stringResource(R.string.checkout_editorial_badge),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            methods.forEachIndexed { index, method ->
                val selected = index == selectedIndex
                val rowScale by animateFloatAsState(
                    targetValue = if (selected) 1.02f else 1f,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
                    label = "paymentRowScale",
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = rowScale
                            scaleY = rowScale
                        }
                        .clickable { onSelect(index) },
                    shape = SectionCorner,
                    color = if (selected) {
                        FashColors.Primary.copy(alpha = 0.08f)
                    } else {
                        scheme.surfaceContainerLow
                    },
                    border = if (selected) {
                        BorderStroke(1.5.dp, FashColors.Primary.copy(alpha = 0.85f))
                    } else {
                        null
                    },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Image(
                                painter = painterResource(paymentMethodIconRes(method.id)),
                                contentDescription = method.name,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                            Text(
                                text = method.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onSurface,
                            )
                        }
                        Icon(
                            imageVector = if (selected) Icons.Default.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (selected) FashColors.Primary else scheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun paymentMethodIconRes(id: String): Int = when (id.lowercase()) {
    "momo" -> R.drawable.ic_payment_momo
    "vnpay" -> R.drawable.ic_payment_vnpay
    "tpbank" -> R.drawable.ic_payment_tpbank
    else -> R.drawable.ic_payment_generic
}

@Composable
private fun OrderSummarySection(
    productPriceVnd: Long,
    shippingFeeVnd: Long,
    discountVnd: Long,
    grandTotalVnd: Long,
    sellerPayoutVnd: Long,
) {
    val scheme = MaterialTheme.colorScheme
    val discountColor = Color(0xFF2E7D32)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart),
        shape = SectionCorner,
        color = scheme.surfaceContainerHighest,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.checkout_order_summary),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.checkout_product_price),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = formatPrice(productPriceVnd),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.checkout_shipping_fee),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = formatPrice(shippingFeeVnd),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                )
            }
            if (discountVnd > 0L) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.checkout_discount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    Text(
                        text = "-${formatPrice(discountVnd)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = discountColor,
                    )
                }
            }
            if (sellerPayoutVnd > 0L) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.checkout_seller_receives),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatPrice(sellerPayoutVnd),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = scheme.onSurface,
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.checkout_total_payment),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                )
                Text(
                    text = formatPrice(grandTotalVnd),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = FashColors.Primary,
                )
            }
        }
    }
}

@Composable
private fun CheckoutButton(
    grandTotalVnd: Long,
    isSubmitting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled && !isSubmitting,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FashTheme.spacing.editorialStart)
            .padding(top = 12.dp, bottom = 10.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = FashColors.Primary,
            contentColor = FashColors.Primary.fashReadableOn(),
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = FashColors.Primary.fashReadableOn(),
                strokeWidth = 2.dp,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.checkout_confirm_pay),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = FashColors.Primary.fashReadableOn(),
                    )
                }
                Text(
                    text = formatPrice(grandTotalVnd),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = FashColors.Primary.fashReadableOn().copy(alpha = 0.95f),
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

private fun formatPrice(vnd: Long): String =
    "₫ ${"%,d".format(vnd).replace(',', '.')}"
