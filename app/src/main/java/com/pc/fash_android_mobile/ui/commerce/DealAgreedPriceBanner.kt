package com.pc.fash_android_mobile.ui.commerce

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.feed.formatListingPriceVnd

@Composable
fun DealAgreedPriceBanner(
    amountVnd: Long,
    fromBuyNow: Boolean,
    modifier: Modifier = Modifier,
) {
    if (amountVnd < 1000L) return
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = scheme.primaryContainer.copy(alpha = 0.55f),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = if (fromBuyNow) {
                    stringResource(R.string.deal_agreed_price_buy_now, formatListingPriceVnd(amountVnd))
                } else {
                    stringResource(R.string.deal_agreed_price_negotiated, formatListingPriceVnd(amountVnd))
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = scheme.onPrimaryContainer,
            )
        }
    }
}
