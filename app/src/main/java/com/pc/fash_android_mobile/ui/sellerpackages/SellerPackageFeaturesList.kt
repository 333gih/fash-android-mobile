package com.pc.fash_android_mobile.ui.sellerpackages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.sellerpackages.SellerPackageFeature
import com.pc.fash_android_mobile.ui.theme.FashColors

@Composable
fun packageFeatureTitle(featureId: String, apiName: String? = null): String {
    apiName?.takeIf { it.isNotBlank() }?.let { return it }
    return when (featureId) {
        "authenticity_verify" -> stringResource(R.string.seller_packages_feature_authenticity)
        "explore_boost" -> stringResource(R.string.seller_packages_feature_explore_boost)
        "fanpage_spotlight" -> stringResource(R.string.seller_packages_feature_fanpage)
        "social_tiktok_instagram" -> stringResource(R.string.seller_packages_feature_social)
        else -> featureId
    }
}

@Composable
fun SellerPackageFeaturesList(
    features: List<SellerPackageFeature>,
    modifier: Modifier = Modifier,
    sectionTitle: String? = null,
) {
    if (features.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        sectionTitle?.takeIf { it.isNotBlank() }?.let { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        features.forEach { feature ->
            val title = packageFeatureTitle(feature.id, feature.name)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (feature.included) Icons.Outlined.Check else Icons.Outlined.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (feature.included) FashColors.Primary else scheme.outline,
                )
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (feature.included) scheme.onSurface else scheme.onSurfaceVariant,
                    )
                    feature.highlight?.takeIf { it.isNotBlank() && feature.included }?.let { h ->
                        Text(
                            text = h,
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
