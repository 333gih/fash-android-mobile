package com.pc.fash_android_mobile.ui.entitlements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.entitlements.FeatureUsageSummary
import com.pc.fash_android_mobile.data.entitlements.UserEntitlementSummary
import com.pc.fash_android_mobile.ui.theme.FashColors

@Composable
fun SellerPackageEntitlementCard(
    summary: UserEntitlementSummary?,
    loading: Boolean,
    onRefresh: () -> Unit,
    onUpgrade: () -> Unit,
    onOpenTools: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.seller_packages_entitlement_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
            when {
                loading && summary == null -> Text(stringResource(R.string.loading))
                summary == null -> Text(stringResource(R.string.seller_packages_entitlement_empty))
                else -> {
                    Text(
                        text = summary.packageName.ifBlank { summary.packageCode },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = FashColors.Primary,
                    )
                    featureLine(
                        label = stringResource(R.string.seller_packages_feature_authenticity),
                        feature = summary.features["authenticity_verify"],
                    )
                    featureLine(
                        label = stringResource(R.string.seller_packages_feature_explore),
                        feature = summary.features["explore_boost"],
                    )
                    featureLine(
                        label = stringResource(R.string.seller_packages_feature_fanpage),
                        feature = summary.features["fanpage_spotlight"],
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.feed_retry))
                }
                Button(onClick = onUpgrade, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.seller_packages_entitlement_upgrade))
                }
            }
            OutlinedButton(onClick = onOpenTools, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.seller_packages_tools_title))
            }
        }
    }
}

@Composable
private fun featureLine(label: String, feature: FeatureUsageSummary?) {
    if (feature == null || !feature.enabled) {
        Text(
            text = "$label: —",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val quota = when {
        feature.unlimited -> "∞"
        feature.remaining != null -> "${feature.remaining} left"
        else -> "✓"
    }
    Text(
        text = "$label: $quota",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
