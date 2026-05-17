package com.pc.fash_android_mobile.ui.orders

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.order.OrderItem

/**
 * Sub-tab filter for the orders list (buying / selling), aligned with core order statuses.
 */
enum class OrderStatusFilter {
    ALL,
    PAYMENT_PENDING,
    PAYMENT_HELD,
    IN_TRANSIT,
    DELIVERED_CONFIRMED,
    CANCELLED,
    DISPUTED,
}

/** Maps API variants to canonical status strings used in the app. */
fun normalizeOrderStatus(raw: String): String = when (val s = raw.lowercase().trim()) {
    "delivering", "shipped", "shipping" -> "in_transit"
    "completed" -> "delivered_confirmed"
    "pending" -> "payment_pending"
    "cash_meetup_open" -> "cash_meetup_open"
    "fulfillment_pending" -> "fulfillment_pending"
    else -> s
}

/** Server-side status value for GET /orders list filtering. */
fun OrderStatusFilter.toApiQuery(): String? = when (this) {
    OrderStatusFilter.ALL -> null
    OrderStatusFilter.PAYMENT_PENDING -> "payment_pending"
    OrderStatusFilter.PAYMENT_HELD -> "payment_held"
    OrderStatusFilter.IN_TRANSIT -> "in_transit"
    OrderStatusFilter.DELIVERED_CONFIRMED -> "delivered_confirmed"
    OrderStatusFilter.CANCELLED -> "cancelled"
    OrderStatusFilter.DISPUTED -> "disputed"
}

fun OrderStatusFilter.matches(order: OrderItem): Boolean {
    if (this == OrderStatusFilter.ALL) return true
    val norm = normalizeOrderStatus(order.status)
    return when (this) {
        OrderStatusFilter.ALL -> true
        OrderStatusFilter.PAYMENT_PENDING -> norm == "payment_pending"
        OrderStatusFilter.PAYMENT_HELD -> norm == "payment_held"
        OrderStatusFilter.IN_TRANSIT -> norm == "in_transit"
        OrderStatusFilter.DELIVERED_CONFIRMED -> norm == "delivered_confirmed"
        OrderStatusFilter.CANCELLED -> norm == "cancelled"
        OrderStatusFilter.DISPUTED -> norm == "disputed"
    }
}

fun countOrdersForFilter(orders: List<OrderItem>, filter: OrderStatusFilter): Int =
    if (filter == OrderStatusFilter.ALL) orders.size else orders.count { filter.matches(it) }

@Composable
fun orderStatusLabelForList(status: String): String {
    val norm = normalizeOrderStatus(status)
    return when (norm) {
        "payment_pending" -> stringResource(R.string.order_status_payment_pending)
        "payment_held" -> stringResource(R.string.order_status_payment_held)
        "in_transit" -> stringResource(R.string.order_status_in_transit)
        "delivered_confirmed" -> stringResource(R.string.order_status_delivered_confirmed)
        "cancelled" -> stringResource(R.string.order_status_cancelled)
        "disputed" -> stringResource(R.string.order_status_disputed)
        "cash_meetup_open" -> stringResource(R.string.order_status_cash_meetup_open)
        "fulfillment_pending" -> stringResource(R.string.order_status_fulfillment_pending)
        else -> status.ifBlank { stringResource(R.string.order_status_unknown) }
    }
}
