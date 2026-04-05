package com.pc.fash_android_mobile.ui.orders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.feed.formatListingPriceVnd
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.fashAmbientShadow
import kotlinx.coroutines.delay

private val BannerThumbDp = 32.dp
private val BannerFallbackHeightDp = 72.dp

/**
 * Compact pending-payment chip: listing → amount + time → CTA. [anchorTopInRootPx] when non-null
 * places the banner above an inline promo; otherwise [modifier] handles bottom placement.
 */
@Composable
fun PendingPaymentBanner(
    data: PendingPaymentBannerData?,
    expanded: Boolean,
    anchorTopInRootPx: Float?,
    onPayClick: (PendingPaymentOrderRow) -> Unit,
    onCancelOrder: (PendingPaymentOrderRow) -> Unit = {},
    onDeadlineElapsed: (orderId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    // 0dp gap so the banner sits flush against the top edge of the inline promo slider
    val marginPx = with(density) { 0.dp.toPx() }
    var heightPx by remember { mutableIntStateOf(0) }
    val fallbackHeightPx = with(density) { BannerFallbackHeightDp.toPx().toInt() }

    val offsetY = if (anchorTopInRootPx != null && data != null) {
        val h = if (heightPx > 0) heightPx else fallbackHeightPx
        (anchorTopInRootPx - h - marginPx).coerceAtLeast(0f)
    } else {
        null
    }

    val placementModifier = if (offsetY != null) {
        Modifier.offset { IntOffset(0, offsetY.toInt()) }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(placementModifier)
            .onSizeChanged { heightPx = it.height },
    ) {
        PendingPaymentBannerContent(
            data = data,
            expanded = expanded,
            onPayClick = onPayClick,
            onCancelOrder = onCancelOrder,
            onDeadlineElapsed = onDeadlineElapsed,
        )
    }
}

@Composable
private fun PendingPaymentBannerContent(
    data: PendingPaymentBannerData?,
    expanded: Boolean,
    onPayClick: (PendingPaymentOrderRow) -> Unit,
    onCancelOrder: (PendingPaymentOrderRow) -> Unit,
    onDeadlineElapsed: (orderId: String) -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }

    val visible = expanded && data != null && data.orders.isNotEmpty()
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { full -> full },
            animationSpec = tween(260, easing = FastOutSlowInEasing),
        ) + fadeIn(tween(220)),
        exit = slideOutVertically(
            targetOffsetY = { full -> full },
            animationSpec = tween(240, easing = FastOutSlowInEasing),
        ) + fadeOut(tween(200)),
    ) {
        val d = data ?: return@AnimatedVisibility
        if (d.orders.size == 1) {
            PendingPaymentOrderCard(
                fetchedAtEpochMs = d.fetchedAtEpochMs,
                order = d.orders.first(),
                now = now,
                onPayClick = { onPayClick(d.orders.first()) },
                onCancelOrder = { onCancelOrder(d.orders.first()) },
                onDeadlineElapsed = onDeadlineElapsed,
                outerHorizontalPadding = true,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                d.orders.forEach { order ->
                    PendingPaymentOrderCard(
                        fetchedAtEpochMs = d.fetchedAtEpochMs,
                        order = order,
                        now = now,
                        onPayClick = { onPayClick(order) },
                        onCancelOrder = { onCancelOrder(order) },
                        onDeadlineElapsed = onDeadlineElapsed,
                        outerHorizontalPadding = false,
                        modifier = Modifier.widthIn(max = 220.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingPaymentOrderCard(
    fetchedAtEpochMs: Long,
    order: PendingPaymentOrderRow,
    now: Long,
    onPayClick: () -> Unit,
    onCancelOrder: () -> Unit,
    onDeadlineElapsed: (orderId: String) -> Unit,
    outerHorizontalPadding: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val elapsedSec = ((now - fetchedAtEpochMs) / 1000L).coerceAtLeast(0L)
    val remaining = (order.remainingSecondsAtFetch - elapsedSec).toInt().coerceAtLeast(0)
    val expired = order.expiredFromApi || remaining <= 0

    LaunchedEffect(order.orderId, remaining, order.expiredFromApi) {
        if (remaining == 0 && !order.expiredFromApi) {
            delay(340)
            onDeadlineElapsed(order.orderId)
        }
    }

    val pendingLabel = stringResource(R.string.pending_payment_banner_title)
    val titleText = order.title.ifBlank { "—" }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (outerHorizontalPadding) Modifier.padding(horizontal = 10.dp) else Modifier,
            )
            .padding(bottom = 4.dp)
            .widthIn(max = 480.dp)
            .fashAmbientShadow(elevation = 4.dp, shape = RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = scheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, FashColors.Primary.copy(alpha = 0.22f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(BannerThumbDp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(scheme.surfaceContainerHighest),
            ) {
                FashAsyncImage(
                    model = order.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                // Line 1: status + listing (what to pay for)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = pendingLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = FashColors.Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.outline,
                    )
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                // Line 2: amount + time (action info)
                Text(
                    text = if (expired) {
                        "${formatListingPriceVnd(order.amountVnd)} · ${stringResource(R.string.pending_payment_expired_label)}"
                    } else {
                        "${formatListingPriceVnd(order.amountVnd)} · ${formatCountdown(remaining)} · " +
                            stringResource(R.string.pending_payment_banner_countdown_hint)
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = if (expired) FontFamily.SansSerif else FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = if (expired) scheme.error else scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                TextButton(
                    onClick = onPayClick,
                    enabled = !expired,
                    modifier = Modifier.heightIn(min = 28.dp, max = 32.dp),
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = FashColors.Primary,
                        disabledContentColor = scheme.onSurfaceVariant,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.pending_payment_banner_cta),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                }
                if (!expired) {
                    TextButton(
                        onClick = onCancelOrder,
                        modifier = Modifier.heightIn(min = 24.dp, max = 30.dp),
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = scheme.error,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.order_cancel_order),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

private fun formatCountdown(remainingSeconds: Int): String {
    val totalSec = remainingSeconds.coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
