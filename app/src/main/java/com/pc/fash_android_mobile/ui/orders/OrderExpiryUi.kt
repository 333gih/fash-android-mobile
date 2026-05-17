package com.pc.fash_android_mobile.ui.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R

fun formatOrderExpiryCountdown(remainingSeconds: Long): String {
    val total = remainingSeconds.coerceAtLeast(0L)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return when {
        h > 0 -> "%d:%02d:%02d".format(h, m, s)
        else -> "%d:%02d".format(m, s)
    }
}

@Composable
fun OrderExpiryCountdownBanner(
    remainingSeconds: Long,
    expiryKind: String,
    isBuyer: Boolean,
    modifier: Modifier = Modifier,
) {
    var left by remember(remainingSeconds, expiryKind) { mutableLongStateOf(remainingSeconds) }
    LaunchedEffect(remainingSeconds, expiryKind) {
        left = remainingSeconds
        while (left > 0L) {
            delay(1000)
            left--
        }
    }
    if (left <= 0L || expiryKind.isBlank()) return
    val scheme = MaterialTheme.colorScheme
    val urgent = left < 5 * 60
    val container = if (urgent) scheme.errorContainer.copy(alpha = 0.55f) else scheme.tertiaryContainer.copy(alpha = 0.65f)
    val onContainer = if (urgent) scheme.onErrorContainer else scheme.onTertiaryContainer
    val countdown = formatOrderExpiryCountdown(left)
    val message = when (expiryKind) {
        "fulfillment_choice" -> if (isBuyer) {
            stringResource(R.string.order_expiry_fulfillment_buyer, countdown)
        } else {
            stringResource(R.string.order_expiry_fulfillment_seller, countdown)
        }
        "meetup_payment" -> if (isBuyer) {
            stringResource(R.string.order_expiry_meetup_payment_buyer, countdown)
        } else {
            stringResource(R.string.order_expiry_meetup_payment_seller, countdown)
        }
        else -> if (isBuyer) {
            stringResource(R.string.order_expiry_payment_buyer, countdown)
        } else {
            stringResource(R.string.order_expiry_payment_seller, countdown)
        }
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = container,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = null,
                tint = onContainer,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (urgent) FontWeight.SemiBold else FontWeight.Medium,
                ),
                color = onContainer,
            )
        }
    }
}
