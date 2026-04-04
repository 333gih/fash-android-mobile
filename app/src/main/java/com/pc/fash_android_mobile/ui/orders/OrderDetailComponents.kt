package com.pc.fash_android_mobile.ui.orders

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.address.ShippingAddress
import com.pc.fash_android_mobile.data.order.OrderDetail
import com.pc.fash_android_mobile.data.order.effectiveBuyerTotal
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.components.FashProfileAvatarImage
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.fashReadableOn
import com.pc.fash_android_mobile.ui.theme.FashTheme

internal enum class OrderViewerRole {
    Buyer,
    Seller,
    /** Neither buyer nor seller matched session — read-only. */
    Viewer,
}

@Composable
internal fun OrderHeroCard(
    d: OrderDetail,
    role: OrderViewerRole,
    formatDate: (String) -> String,
) {
    val scheme = MaterialTheme.colorScheme
    val st = d.status.trim().lowercase()
    val (title, subtitle, accent) = heroContent(d, role, st, formatDate)
    val (container, onContainer) = when (st) {
        "cancelled" -> scheme.errorContainer to scheme.onErrorContainer
        "disputed" -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        else -> scheme.surface to scheme.onSurface
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = container,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(FashColors.Primary),
                )
                Text(
                    text = stringResource(R.string.order_detail_status_caption),
                    style = MaterialTheme.typography.labelSmall,
                    color = onContainer.copy(alpha = 0.75f),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = if (st == "cancelled" || st == "disputed") onContainer else accent,
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun heroContent(
    d: OrderDetail,
    role: OrderViewerRole,
    st: String,
    formatDate: (String) -> String,
): Triple<String, String, Color> {
    val primary = FashColors.Primary
    return when (st) {
        "payment_pending" -> when (role) {
            OrderViewerRole.Seller -> Triple(
                stringResource(R.string.order_hero_seller_wait_payment_title),
                stringResource(R.string.order_hero_seller_wait_payment_sub),
                primary,
            )
            else -> Triple(
                stringResource(R.string.order_hero_buyer_payment_pending_title),
                stringResource(R.string.order_hero_buyer_payment_pending_sub),
                primary,
            )
        }
        "payment_held" -> when (role) {
            OrderViewerRole.Seller -> {
                val shipBy = d.shipByAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty()
                val sub = if (shipBy.isNotBlank()) {
                    stringResource(R.string.order_hero_seller_prepare_sub_deadline, shipBy)
                } else {
                    stringResource(R.string.order_hero_seller_prepare_sub)
                }
                Triple(stringResource(R.string.order_hero_seller_prepare_title), sub, primary)
            }
            else -> Triple(
                stringResource(R.string.order_hero_buyer_payment_held_title),
                d.escrowReleaseAt.takeIf { it.isNotBlank() }?.let {
                    stringResource(R.string.order_hero_buyer_escrow_sub, formatDate(it))
                } ?: stringResource(R.string.order_hero_buyer_payment_held_sub),
                primary,
            )
        }
        "in_transit" -> {
            val eta = d.expectedDeliveryAt.takeIf { it.isNotBlank() }?.let {
                stringResource(R.string.order_hero_eta_line, formatDate(it))
            }.orEmpty()
            val track = d.trackingStatusSummary.takeIf { it.isNotBlank() }.orEmpty()
            val sub = listOf(eta, track).filter { it.isNotBlank() }.joinToString("\n")
            Triple(
                stringResource(R.string.order_status_in_transit),
                sub.ifBlank { stringResource(R.string.order_hero_in_transit_sub) },
                primary,
            )
        }
        "delivered_confirmed" -> {
            val sub = d.deliveredAt.takeIf { it.isNotBlank() }?.let { at ->
                stringResource(R.string.order_hero_completed_sub, formatDate(at))
            }.orEmpty()
            Triple(stringResource(R.string.order_status_delivered_confirmed), sub, primary)
        }
        "cancelled" -> Triple(
            stringResource(R.string.order_status_cancelled),
            d.cancelledAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }
                ?.let { stringResource(R.string.order_hero_cancelled_sub, it) }
                ?: stringResource(R.string.order_hero_cancelled_sub_generic),
            primary,
        )
        "disputed" -> Triple(
            stringResource(R.string.order_status_disputed),
            d.disputeSummary.ifBlank { stringResource(R.string.order_hero_disputed_sub) },
            primary,
        )
        else -> Triple(
            d.status.ifBlank { stringResource(R.string.order_status_unknown) },
            "",
            primary,
        )
    }
}

@Composable
internal fun OrderTimelineSection(
    d: OrderDetail,
    formatDate: (String) -> String,
) {
    val st = d.status.trim().lowercase()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.order_timeline_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            when (st) {
                "cancelled" -> {
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_placed),
                            subtitle = d.createdAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                            state = TimelineStepState.Done,
                        ),
                        isLast = false,
                    )
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_cancelled),
                            subtitle = d.cancelledAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                            state = TimelineStepState.Current,
                        ),
                        isLast = true,
                    )
                }
                "disputed" -> {
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_placed),
                            subtitle = d.createdAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                            state = TimelineStepState.Done,
                        ),
                        isLast = false,
                    )
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_dispute_open),
                            subtitle = d.disputeSummary,
                            state = TimelineStepState.Current,
                        ),
                        isLast = true,
                    )
                }
                "payment_pending" -> {
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_placed),
                            subtitle = d.createdAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                            state = TimelineStepState.Done,
                        ),
                        isLast = false,
                    )
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_waiting_payment),
                            subtitle = "",
                            state = TimelineStepState.Current,
                        ),
                        isLast = true,
                    )
                }
                "payment_held" -> {
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_placed),
                            subtitle = d.createdAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                            state = TimelineStepState.Done,
                        ),
                        isLast = false,
                    )
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_paid),
                            subtitle = d.paidAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                            state = TimelineStepState.Done,
                        ),
                        isLast = false,
                    )
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_preparing),
                            subtitle = stringResource(R.string.order_timeline_preparing_sub),
                            state = TimelineStepState.Current,
                        ),
                        isLast = true,
                    )
                }
                "in_transit" -> {
                    val shipSub = d.shippedAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty()
                    val transitSub = d.trackingStatusSummary.ifBlank {
                        d.expectedDeliveryAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty()
                    }
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_placed),
                            subtitle = d.createdAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                            state = TimelineStepState.Done,
                        ),
                        isLast = false,
                    )
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_paid),
                            subtitle = d.paidAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                            state = TimelineStepState.Done,
                        ),
                        isLast = false,
                    )
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_shipped),
                            subtitle = shipSub,
                            state = TimelineStepState.Done,
                        ),
                        isLast = false,
                    )
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_in_transit),
                            subtitle = transitSub,
                            state = TimelineStepState.Current,
                        ),
                        isLast = true,
                    )
                }
                "delivered_confirmed" -> {
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_placed),
                            subtitle = d.createdAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                            state = TimelineStepState.Done,
                        ),
                        isLast = false,
                    )
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_paid),
                            subtitle = d.paidAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                            state = TimelineStepState.Done,
                        ),
                        isLast = false,
                    )
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_shipped),
                            subtitle = d.shippedAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                            state = TimelineStepState.Done,
                        ),
                        isLast = false,
                    )
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_delivered),
                            subtitle = d.deliveredAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                            state = TimelineStepState.Done,
                        ),
                        isLast = true,
                    )
                }
                else -> {
                    TimelineRow(
                        TimelineStepUi(
                            title = stringResource(R.string.order_timeline_placed),
                            subtitle = d.createdAt.takeIf { it.isNotBlank() }?.let { formatDate(it) }.orEmpty(),
                            state = TimelineStepState.Done,
                        ),
                        isLast = false,
                    )
                    TimelineRow(
                        TimelineStepUi(
                            title = orderStatusLabelString(st),
                            subtitle = "",
                            state = TimelineStepState.Current,
                        ),
                        isLast = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun orderStatusLabelString(status: String): String = when (status.lowercase()) {
    "payment_pending" -> stringResource(R.string.order_status_payment_pending)
    "payment_held" -> stringResource(R.string.order_status_payment_held)
    "in_transit" -> stringResource(R.string.order_status_in_transit)
    "delivered_confirmed" -> stringResource(R.string.order_status_delivered_confirmed)
    "cancelled" -> stringResource(R.string.order_status_cancelled)
    "disputed" -> stringResource(R.string.order_status_disputed)
    else -> status.ifBlank { stringResource(R.string.order_status_unknown) }
}

internal data class TimelineStepUi(
    val title: String,
    val subtitle: String,
    val state: TimelineStepState,
)

internal enum class TimelineStepState { Done, Current, Pending }

@Composable
private fun TimelineRow(
    step: TimelineStepUi,
    isLast: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val (icon, tint) = when (step.state) {
        TimelineStepState.Done -> Icons.Default.CheckCircle to FashColors.Primary
        TimelineStepState.Current -> Icons.Default.LocalShipping to FashColors.Primary
        TimelineStepState.Pending -> Icons.Default.RadioButtonUnchecked to scheme.outline.copy(alpha = 0.5f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .padding(top = 4.dp)
                        .background(scheme.outlineVariant.copy(alpha = 0.5f)),
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (step.state == TimelineStepState.Current) FontWeight.Bold else FontWeight.Normal,
                ),
                color = when (step.state) {
                    TimelineStepState.Current -> FashColors.Primary
                    else -> scheme.onSurface
                },
            )
            if (step.subtitle.isNotBlank()) {
                Text(
                    text = step.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun BuyerShippingAddressCard(
    d: OrderDetail,
    selectedLocal: ShippingAddress?,
    onChangeClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.order_detail_shipping_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onChangeClick) {
                    Text(
                        text = if (
                            selectedLocal != null ||
                            d.recipientName.isNotBlank() ||
                            d.shippingAddressFormatted.isNotBlank()
                        ) {
                            stringResource(R.string.order_detail_shipping_change)
                        } else {
                            stringResource(R.string.order_detail_shipping_choose)
                        },
                        color = FashColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            when {
                selectedLocal != null -> {
                    Text(
                        text = selectedLocal.recipientName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    if (selectedLocal.phone.isNotBlank()) {
                        Text(
                            text = selectedLocal.phone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedLocal.formattedSingleLine(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                d.recipientName.isNotBlank() || d.recipientPhone.isNotBlank() ||
                    d.shippingAddressFormatted.isNotBlank() -> {
                    val name = d.recipientName.ifBlank { "—" }
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    if (d.recipientPhone.isNotBlank()) {
                        Text(
                            text = d.recipientPhone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val line = d.shippingAddressFormatted.trim()
                    if (line.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                else -> {
                    Text(
                        text = stringResource(R.string.order_detail_shipping_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun OrderProductCard(d: OrderDetail, showSellerHandle: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val imageUrl = d.listingImageUrl.takeIf { it.isNotBlank() }?.let { orderDetailResolveImageUrl(it) }.orEmpty()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = scheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.order_detail_product),
                style = MaterialTheme.typography.labelSmall,
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
                    if (showSellerHandle && d.sellerUsername.isNotBlank()) {
                        Text(
                            text = "@${d.sellerUsername}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = d.listingTitle.ifBlank { "—" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (d.listingVariantLabel.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.order_detail_variant_line, d.listingVariantLabel),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formatOrderPrice(d.amountVnd),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = FashColors.Primary,
                    )
                }
            }
        }
    }
}

@Composable
internal fun OrderBuyerPaymentCard(d: OrderDetail) {
    val scheme = MaterialTheme.colorScheme
    val total = d.effectiveBuyerTotal()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = scheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.order_detail_payment_breakdown_title),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            MoneyLine(stringResource(R.string.order_detail_buyer_product_price), d.amountVnd, emphasis = false)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = scheme.outlineVariant.copy(alpha = 0.55f),
            )
            MoneyLine(stringResource(R.string.order_detail_buyer_shipping_fee), d.shippingFeeVnd, emphasis = false)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = scheme.outlineVariant.copy(alpha = 0.55f),
            )
            MoneyLine(stringResource(R.string.order_detail_buyer_total), total, emphasis = true)
        }
    }
}

@Composable
internal fun OrderSellerRevenueCard(d: OrderDetail) {
    val scheme = MaterialTheme.colorScheme
    val pct = if (d.amountVnd > 0L) {
        ((d.platformFeeVnd * 100L) / d.amountVnd).toInt().coerceIn(0, 100)
    } else {
        null
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = scheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.order_detail_seller_revenue_title),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            MoneyLine(stringResource(R.string.order_detail_seller_listing_price), d.amountVnd, emphasis = false)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = scheme.outlineVariant.copy(alpha = 0.55f),
            )
            val feeLabel = if (pct != null) {
                stringResource(R.string.order_detail_commission_percent, pct)
            } else {
                stringResource(R.string.order_detail_platform_fee)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = feeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = "−${formatOrderPrice(d.platformFeeVnd)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = scheme.outlineVariant.copy(alpha = 0.55f),
            )
            MoneyLine(stringResource(R.string.order_detail_seller_net), d.sellerPayoutVnd, emphasis = true)
        }
    }
}

@Composable
private fun MoneyLine(label: String, vnd: Long, emphasis: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = if (emphasis) MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium,
            color = if (emphasis) scheme.onSurface else scheme.onSurfaceVariant,
        )
        Text(
            text = formatOrderPrice(vnd),
            style = if (emphasis) {
                MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            },
            color = if (emphasis) FashColors.Primary else scheme.onSurface,
        )
    }
}

@Composable
internal fun BuyerProtectionBanner() {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFE8F5E9),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Default.Payments,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(22.dp),
            )
            Column {
                Text(
                    text = stringResource(R.string.order_detail_buyer_protection_title),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF1B5E20),
                )
                Text(
                    text = stringResource(R.string.order_detail_buyer_protection_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1B5E20).copy(alpha = 0.9f),
                )
            }
        }
    }
}

@Composable
internal fun CounterpartyCard(
    title: String,
    displayName: String,
    username: String,
    avatarUrl: String,
) {
    val scheme = MaterialTheme.colorScheme
    val url = avatarUrl.takeIf { it.isNotBlank() }?.let { orderDetailResolveImageUrl(it) }.orEmpty()
    val avatarForUi = url.takeIf { it.isNotEmpty() }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = scheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(scheme.surfaceContainerHigh),
                    ) {
                        FashProfileAvatarImage(
                            imageUrl = avatarForUi,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = displayName.ifBlank { "@$username" },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
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
        }
    }
}

@Composable
internal fun OrderTrackingCard(d: OrderDetail) {
    if (d.trackingNumber.isBlank() && d.carrier.isBlank()) return
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        color = scheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.order_detail_tracking),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = d.carrier.ifBlank { "—" },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = d.trackingNumber.ifBlank { stringResource(R.string.order_detail_tracking_empty) },
                style = MaterialTheme.typography.bodySmall,
                color = FashColors.Primary,
            )
            if (d.trackingStatusSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = d.trackingStatusSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun OrderStickyBottomBar(
    d: OrderDetail,
    role: OrderViewerRole,
    isWorking: Boolean,
    onPay: () -> Unit,
    onConfirmReceipt: () -> Unit,
    onReview: () -> Unit,
    onShip: () -> Unit,
    onChat: () -> Unit,
    onOpenDispute: () -> Unit = {},
    onSubmitDisputeEvidence: () -> Unit = {},
) {
    val st = d.status.trim().lowercase()
    val showPay = role == OrderViewerRole.Buyer && st == "payment_pending"
    val showConfirm = role == OrderViewerRole.Buyer && d.canConfirm
    val showReview = role == OrderViewerRole.Buyer && d.canReview
    val showShip = role == OrderViewerRole.Seller && st == "payment_held" && d.canShip
    val showChat =
        d.conversationId.isNotBlank() &&
            (role == OrderViewerRole.Buyer || role == OrderViewerRole.Seller) &&
            st !in listOf("cancelled")
    val showOpenDispute =
        role != OrderViewerRole.Viewer &&
            st in listOf("in_transit", "delivered_confirmed")
    val showDisputeEvidence =
        role != OrderViewerRole.Viewer && st == "disputed"

    if (!showPay && !showConfirm && !showReview && !showShip && !showChat &&
        !showOpenDispute && !showDisputeEvidence
    ) {
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
        if (showShip) {
            Button(
                onClick = onShip,
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FashColors.Primary,
                    contentColor = FashColors.Primary.fashReadableOn(),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.order_detail_action_ship),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (showOpenDispute) {
            OutlinedButton(
                onClick = onOpenDispute,
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.order_detail_dispute_open),
                    fontWeight = FontWeight.SemiBold,
                    color = FashColors.Primary,
                )
            }
        }
        if (showDisputeEvidence) {
            OutlinedButton(
                onClick = onSubmitDisputeEvidence,
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.order_detail_dispute_evidence),
                    fontWeight = FontWeight.SemiBold,
                    color = FashColors.Primary,
                )
            }
        }
        if (showPay) {
            Button(
                onClick = onPay,
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FashColors.Primary,
                    contentColor = FashColors.Primary.fashReadableOn(),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.order_detail_pay), fontWeight = FontWeight.SemiBold)
            }
        }
        if (showConfirm) {
            Button(
                onClick = onConfirmReceipt,
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FashColors.Primary,
                    contentColor = FashColors.Primary.fashReadableOn(),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.orders_confirm_received), fontWeight = FontWeight.SemiBold)
            }
        }
        if (showReview) {
            OutlinedButton(
                onClick = onReview,
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.orders_review))
            }
        }
        if (showChat) {
            OutlinedButton(
                onClick = onChat,
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = FashColors.Primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (role) {
                        OrderViewerRole.Seller -> stringResource(R.string.order_detail_chat_buyer)
                        else -> stringResource(R.string.order_detail_chat_seller)
                    },
                    color = FashColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

internal fun formatOrderPrice(vnd: Long): String =
    "đ ${"%,d".format(vnd).replace(',', '.')}"

internal fun orderDetailResolveImageUrl(path: String): String {
    if (path.startsWith("http")) return path
    val base = AppEnvironment.apiBaseUrl.trimEnd('/')
    return if (path.startsWith("/")) "$base$path" else "$base/$path"
}

internal fun formatOrderDateTime(iso: String): String {
    if (iso.isBlank()) return ""
    return try {
        val s = iso.trim().let { t ->
            when {
                t.contains('T') -> t
                t.contains(' ') -> t.replaceFirst(" ", "T")
                else -> "${t}T00:00:00Z"
            }
        }
        val instant = java.time.Instant.parse(
            if (s.endsWith("Z") || s.contains('+') || s.length > 25) s else "${s}Z",
        )
        val z = java.time.ZoneId.systemDefault()
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(instant.atZone(z))
    } catch (_: Exception) {
        iso
    }
}
