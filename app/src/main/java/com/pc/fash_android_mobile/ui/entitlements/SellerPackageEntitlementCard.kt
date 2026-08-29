package com.pc.fash_android_mobile.ui.entitlements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.entitlements.FeatureUsageSummary
import com.pc.fash_android_mobile.data.entitlements.UserEntitlementSummary
import com.pc.fash_android_mobile.ui.components.FashPrimaryButton
import com.pc.fash_android_mobile.ui.components.FashSecondaryButton
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

private val PROFILE_FEATURE_GROUP_ORDER = listOf("verification", "visibility", "social_promo")

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
    val name = summary?.packageName?.ifBlank { summary.packageCode }.orEmpty()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(FashColors.Primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WorkspacePremium,
                        contentDescription = null,
                        tint = FashColors.Primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.seller_packages_entitlement_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    when {
                        loading && summary == null -> Text(
                            text = stringResource(R.string.explore_filter_loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                        name.isBlank() -> Text(
                            text = stringResource(R.string.seller_packages_entitlement_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                        else -> Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = FashColors.Primary,
                        )
                    }
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.seller_packages_entitlement_refresh),
                        tint = FashColors.Primary,
                    )
                }
            }
            summary?.let { ent ->
                profileFeatureRows(ent).forEach { (key, feature) ->
                    FeatureQuotaRow(
                        icon = profileFeatureIcon(key, feature.executionKind),
                        label = profileFeatureLabel(key, feature),
                        feature = feature,
                    )
                }
            }
            FashPrimaryButton(
                onClick = onOpenTools,
                label = stringResource(R.string.seller_packages_tools_open),
            )
            FashSecondaryButton(
                onClick = onUpgrade,
                label = stringResource(R.string.seller_packages_entitlement_upgrade),
            )
        }
    }
}

private fun profileFeatureRows(summary: UserEntitlementSummary): List<Pair<String, FeatureUsageSummary>> {
    val grouped = summary.features.entries
        .filter { it.value.featureGroup.isNotBlank() || it.value.name.isNotBlank() }
        .groupBy { it.value.featureGroup.ifBlank { "other" } }
    val order = PROFILE_FEATURE_GROUP_ORDER.filter { grouped.containsKey(it) } +
        grouped.keys.filter { it !in PROFILE_FEATURE_GROUP_ORDER }.sorted()
    return order.flatMap { group ->
        grouped[group].orEmpty().sortedBy { it.value.name.ifBlank { it.key } }
            .map { it.key to it.value }
    }
}

private fun profileFeatureIcon(key: String, kind: String): ImageVector = when {
    kind == "boost" || key == "explore_boost" -> Icons.Outlined.AutoAwesome
    key.contains("social") -> Icons.Outlined.Share
    key.contains("fanpage") -> Icons.Outlined.Campaign
    key.contains("authenticity") || key.contains("verify") || key == "seller_real_badge" -> Icons.Outlined.VerifiedUser
    else -> Icons.Outlined.WorkspacePremium
}

@Composable
private fun profileFeatureLabel(key: String, feature: FeatureUsageSummary): String {
    if (feature.name.isNotBlank()) return feature.name
    return when (key) {
        "authenticity_verify" -> stringResource(R.string.seller_packages_feature_authenticity)
        "explore_boost" -> stringResource(R.string.seller_packages_feature_explore_boost)
        "fanpage_spotlight" -> stringResource(R.string.seller_packages_feature_fanpage)
        "social_tiktok_instagram" -> stringResource(R.string.seller_packages_feature_social)
        "seller_real_badge" -> stringResource(R.string.seller_packages_feature_real_badge)
        else -> key.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}

@Composable
private fun FeatureQuotaRow(
    icon: ImageVector,
    label: String,
    feature: FeatureUsageSummary?,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (feature.canUse()) FashColors.Primary else scheme.onSurfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        QuotaChip(feature = feature)
    }
}
