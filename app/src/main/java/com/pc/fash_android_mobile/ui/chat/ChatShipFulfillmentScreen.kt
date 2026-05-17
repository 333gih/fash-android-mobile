package com.pc.fash_android_mobile.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.address.ShippingAddress
import com.pc.fash_android_mobile.data.order.OrderBuyerCancelPolicy
import com.pc.fash_android_mobile.data.order.effectiveBuyerTotal
import com.pc.fash_android_mobile.ui.address.AddressBookViewModel
import com.pc.fash_android_mobile.ui.commerce.DealAgreedPriceBanner
import com.pc.fash_android_mobile.ui.feed.formatListingPriceVnd
import com.pc.fash_android_mobile.ui.orders.OrderDetailViewModel
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.fashReadableOn

enum class ShipFlowSource {
    Chat,
    BuyNow,
}

/** Args for [ChatShipFulfillmentScreen] — chat ship path or listing Buy now. */
data class ChatShipFlowArgs(
    val orderId: String,
    val listingId: String,
    val agreedAmountVnd: Long,
    val source: ShipFlowSource = ShipFlowSource.Chat,
)

private enum class ShipFlowStep {
    Shipping,
    Payment,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatShipFulfillmentScreen(
    args: ChatShipFlowArgs,
    onBack: () -> Unit,
    orderDetailViewModel: OrderDetailViewModel,
    addressBookViewModel: AddressBookViewModel,
    onOpenShippingAddressList: () -> Unit,
    onOpenAddShippingAddress: () -> Unit,
    /** Opens in-app checkout for this order (same contract as order overlay → checkout). */
    onContinueToCheckout: (listingId: String, amountVnd: Long, orderId: String) -> Unit,
    shipOnlinePaymentEnabled: Boolean,
    onCancelOrder: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableIntStateOf(0) }
    val currentStep = if (step == 0) ShipFlowStep.Shipping else ShipFlowStep.Payment

    val detail by orderDetailViewModel.detail.collectAsState()
    val isLoading by orderDetailViewModel.isLoading.collectAsState()
    val savedAddresses by addressBookViewModel.addresses.collectAsState()

    LaunchedEffect(args.orderId) {
        addressBookViewModel.refresh()
        orderDetailViewModel.load(args.orderId)
    }

    val isBuyer = orderDetailViewModel.isCurrentUserBuyer()
    val isSeller = orderDetailViewModel.isCurrentUserSeller()
    val st = detail?.status?.trim()?.lowercase().orEmpty()
    val localAddress: ShippingAddress? = remember(args.orderId, savedAddresses) {
        addressBookViewModel.getSelectionForOrder(args.orderId)
    }
    val serverHasAddress = !detail?.shippingAddressFormatted.isNullOrBlank() ||
        (!detail?.recipientName.isNullOrBlank() && !detail?.recipientPhone.isNullOrBlank())
    val hasAddress = serverHasAddress || localAddress != null
    val displayName = detail?.recipientName?.trim()?.takeIf { it.isNotEmpty() }
        ?: localAddress?.recipientName?.trim().orEmpty()
    val displayPhone = detail?.recipientPhone?.trim()?.takeIf { it.isNotEmpty() }
        ?: localAddress?.phone?.trim().orEmpty()
    val displayAddressLine = detail?.shippingAddressFormatted?.trim()?.takeIf { it.isNotEmpty() }
        ?: localAddress?.formattedAddressLine().orEmpty()
    val agreedAmount = when {
        args.agreedAmountVnd > 0L -> args.agreedAmountVnd
        detail != null && detail!!.amountVnd > 0L -> detail!!.amountVnd
        else -> 0L
    }
    val titleRes = when (args.source) {
        ShipFlowSource.BuyNow -> R.string.chat_ship_flow_title_buy_now
        ShipFlowSource.Chat -> R.string.chat_ship_flow_title
    }
    val showCancel = isBuyer && OrderBuyerCancelPolicy.buyerCanCancel(st) && onCancelOrder != null

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.chat_detail_back_cd),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = currentStep == ShipFlowStep.Shipping,
                    onClick = { step = 0 },
                    label = { Text(stringResource(R.string.chat_ship_flow_step_shipping)) },
                )
                FilterChip(
                    selected = currentStep == ShipFlowStep.Payment,
                    onClick = { step = 1 },
                    label = { Text(stringResource(R.string.chat_ship_flow_step_payment)) },
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))

            if (isLoading && detail == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = FashColors.Primary)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.chat_ship_flow_loading_order),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OrderSummaryCard(
                        title = detail?.listingTitle?.trim().orEmpty().ifBlank {
                            stringResource(R.string.chat_ship_flow_order_fallback_title)
                        },
                        orderId = args.orderId,
                        amountLabel = formatListingPriceVnd(agreedAmount),
                        statusLabel = st.ifBlank { "—" },
                    )

                    if (agreedAmount >= 1000L) {
                        DealAgreedPriceBanner(
                            amountVnd = agreedAmount,
                            fromBuyNow = args.source == ShipFlowSource.BuyNow,
                        )
                    }

                    when (currentStep) {
                        ShipFlowStep.Shipping -> {
                            Icon(
                                imageVector = Icons.Outlined.LocalShipping,
                                contentDescription = null,
                                tint = FashColors.Primary,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Text(
                                text = stringResource(R.string.chat_ship_flow_shipping_heading),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            )
                            when {
                                isBuyer -> {
                                    Text(
                                        text = stringResource(R.string.chat_ship_flow_shipping_buyer_body),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (hasAddress) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                            tonalElevation = 0.dp,
                                            shadowElevation = 0.dp,
                                        ) {
                                            Column(Modifier.padding(14.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.CheckCircle,
                                                        contentDescription = null,
                                                        tint = FashColors.Primary,
                                                    )
                                                    Text(
                                                        text = stringResource(R.string.chat_ship_flow_address_saved),
                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                                    )
                                                }
                                                Spacer(Modifier.height(8.dp))
                                                displayName.takeIf { it.isNotEmpty() }?.let {
                                                    Text(
                                                        text = it,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                    )
                                                }
                                                displayPhone.takeIf { it.isNotEmpty() }?.let {
                                                    Text(
                                                        text = it,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                                displayAddressLine.takeIf { it.isNotEmpty() }?.let {
                                                    Spacer(Modifier.height(6.dp))
                                                    Text(
                                                        text = it,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = stringResource(R.string.chat_ship_flow_address_missing_hint),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = onOpenShippingAddressList,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(stringResource(R.string.chat_ship_flow_choose_saved_address))
                                    }
                                    OutlinedButton(
                                        onClick = onOpenAddShippingAddress,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(stringResource(R.string.chat_ship_flow_add_address))
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = { step = 1 },
                                        enabled = hasAddress,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = FashColors.Primary),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.chat_ship_flow_continue_to_payment),
                                            color = FashColors.Primary.fashReadableOn(),
                                        )
                                    }
                                    if (showCancel) {
                                        OutlinedButton(
                                            onClick = onCancelOrder,
                                            modifier = Modifier.fillMaxWidth(),
                                            border = BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.error.copy(alpha = 0.45f),
                                            ),
                                        ) {
                                            Text(
                                                text = stringResource(R.string.order_cancel_order),
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    }
                                }
                                isSeller -> {
                                    Text(
                                        text = stringResource(R.string.chat_ship_flow_shipping_seller_body),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = stringResource(R.string.chat_ship_flow_seller_after_paid_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                else -> {
                                    Text(
                                        text = stringResource(R.string.chat_ship_flow_shipping_unknown_role),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        ShipFlowStep.Payment -> {
                            Icon(
                                imageVector = Icons.Outlined.Payments,
                                contentDescription = null,
                                tint = FashColors.Primary,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Text(
                                text = stringResource(R.string.chat_ship_flow_payment_heading),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            )
                            if (!shipOnlinePaymentEnabled) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                ) {
                                    Text(
                                        text = stringResource(R.string.chat_ship_flow_payment_disabled_env),
                                        modifier = Modifier.padding(14.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            } else {
                                when {
                                    isBuyer && st == "payment_pending" -> {
                                        Text(
                                            text = stringResource(R.string.chat_ship_flow_payment_buyer_pending_body),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        val fee = detail?.shippingFeeVnd ?: 0L
                                        val total = detail?.effectiveBuyerTotal() ?: args.agreedAmountVnd
                                        if (fee > 0L) {
                                            Text(
                                                text = stringResource(
                                                    R.string.chat_ship_flow_estimated_shipping_fee,
                                                    formatListingPriceVnd(fee),
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                        Text(
                                            text = stringResource(
                                                R.string.chat_ship_flow_total_hint,
                                                formatListingPriceVnd(total),
                                            ),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.CreditCard,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                            Text(
                                                text = stringResource(R.string.chat_ship_flow_secure_checkout_note),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                val lid = args.listingId.trim().ifEmpty { detail?.listingId.orEmpty() }
                                                val amt = when {
                                                    args.agreedAmountVnd > 0L -> args.agreedAmountVnd
                                                    detail != null && detail!!.amountVnd > 0L -> detail!!.amountVnd
                                                    else -> 0L
                                                }
                                                onContinueToCheckout(lid, amt, args.orderId)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = args.listingId.isNotBlank() || !detail?.listingId.isNullOrBlank(),
                                            colors = ButtonDefaults.buttonColors(containerColor = FashColors.Primary),
                                        ) {
                                            Text(
                                                text = stringResource(R.string.chat_ship_flow_pay_online_cta),
                                                color = FashColors.Primary.fashReadableOn(),
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }
                                    isBuyer && st != "payment_pending" && st != "cancelled" -> {
                                        Text(
                                            text = stringResource(R.string.chat_ship_flow_payment_buyer_paid_or_progress),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    isSeller -> {
                                        Text(
                                            text = stringResource(R.string.chat_ship_flow_payment_seller_body),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = stringResource(R.string.chat_ship_flow_payment_unknown_role),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
}

@Composable
private fun OrderSummaryCard(
    title: String,
    orderId: String,
    amountLabel: String,
    statusLabel: String,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = stringResource(R.string.chat_ship_flow_order_id_line, orderId),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.chat_ship_flow_amount_line, amountLabel),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.chat_ship_flow_status_line, statusLabel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
