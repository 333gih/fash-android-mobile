package com.pc.fash_android_mobile.ui.checkout

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.order.OrderDetail
import com.pc.fash_android_mobile.data.listing.ListingDetail
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.fashReadableOn
import com.pc.fash_android_mobile.ui.theme.FashTheme

private val InputCorner = RoundedCornerShape(12.dp)
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
    val awaitingGateway by viewModel.awaitingGatewayReturn.collectAsState()
    val scheme = MaterialTheme.colorScheme

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
            Scaffold(
                modifier = modifier.fillMaxSize(),
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
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 120.dp),
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
                        AddressSection(
                            fullName = fullName,
                            onFullNameChange = viewModel::onFullNameChange,
                            phone = phone,
                            onPhoneChange = viewModel::onPhoneChange,
                            address = address,
                            onAddressChange = viewModel::onAddressChange,
                            district = district,
                            onDistrictChange = viewModel::onDistrictChange,
                            city = city,
                            onCityChange = viewModel::onCityChange,
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
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                    ) {
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
                            grandTotalVnd = viewModel.grandTotalVnd,
                            isSubmitting = isSubmitting,
                            enabled = viewModel.canSubmit() && !awaitingGateway,
                            onClick = { viewModel.submitPayment(onSuccess) },
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
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
        }
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
private fun AddressSection(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    district: String,
    onDistrictChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
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
        Spacer(modifier = Modifier.height(12.dp))
        AddressInput(
            value = fullName,
            onValueChange = onFullNameChange,
            placeholder = stringResource(R.string.checkout_full_name),
        )
        Spacer(modifier = Modifier.height(8.dp))
        AddressInput(
            value = phone,
            onValueChange = onPhoneChange,
            placeholder = stringResource(R.string.checkout_phone),
        )
        Spacer(modifier = Modifier.height(8.dp))
        AddressInput(
            value = address,
            onValueChange = onAddressChange,
            placeholder = stringResource(R.string.checkout_address),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AddressInput(
                value = district,
                onValueChange = onDistrictChange,
                placeholder = stringResource(R.string.checkout_district),
                modifier = Modifier.weight(1f),
            )
            AddressInput(
                value = city,
                onValueChange = onCityChange,
                placeholder = stringResource(R.string.checkout_city),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AddressInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = InputCorner,
        color = scheme.surfaceContainerHighest,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = scheme.onSurface),
            singleLine = true,
            cursorBrush = SolidColor(FashColors.Primary),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    inner()
                }
            },
        )
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
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(index) },
                    shape = SectionCorner,
                    color = scheme.surface,
                    border = BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) FashColors.Primary else scheme.outlineVariant.copy(alpha = 0.72f),
                    ),
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
                            Surface(
                                shape = CircleShape,
                                color = paymentMethodChipColor(method.id).copy(alpha = 0.2f),
                                modifier = Modifier.size(40.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = paymentMethodInitial(method.id),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = paymentMethodChipColor(method.id),
                                    )
                                }
                            }
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

private fun paymentMethodInitial(id: String): String = when (id.lowercase()) {
    "momo" -> "M"
    "zalopay" -> "Z"
    "shopeepay" -> "S"
    "tpbank" -> "B"
    else -> id.take(1).uppercase()
}

@Composable
private fun paymentMethodChipColor(id: String): Color = when (id.lowercase()) {
    "momo" -> Color(0xFFE91E8C)
    "zalopay" -> Color(0xFF0068FF)
    "shopeepay" -> Color(0xFFEE4D2D)
    "tpbank" -> Color(0xFF757575)
    else -> FashColors.Primary
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
        color = Color.White,
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        androidx.compose.material3.Button(
            onClick = onClick,
            enabled = enabled && !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FashTheme.spacing.editorialStart, vertical = 16.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = FashColors.Primary,
                contentColor = FashColors.Primary.fashReadableOn(),
            ),
            shape = RoundedCornerShape(12.dp),
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
}

private fun resolveImageUrl(path: String): String {
    if (path.startsWith("http")) return path
    val base = com.pc.fash_android_mobile.config.AppEnvironment.apiBaseUrl.trimEnd('/')
    return if (path.startsWith("/")) "$base$path" else "$base/$path"
}

private fun formatPrice(vnd: Long): String =
    "₫ ${"%,d".format(vnd).replace(',', '.')}"
