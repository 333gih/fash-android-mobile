package com.pc.fash_android_mobile.ui.sellerpackages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.sellerpackages.SellerProductPackage
import com.pc.fash_android_mobile.ui.components.FashEmptyState
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerProductPackagesScreen(
    modifier: Modifier = Modifier,
    viewModel: SellerProductPackagesViewModel,
    onBack: () -> Unit,
    onBuyPackage: (SellerProductPackage) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val packages by viewModel.packages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadError by viewModel.loadError.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.seller_packages_screen_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = FashColors.Primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surface),
            )
        },
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = scheme.surfaceContainerLowest,
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(color = FashColors.Primary)
                    }
                }
                loadError != null -> {
                    FashEmptyState(
                        icon = Icons.Outlined.ErrorOutline,
                        title = stringResource(R.string.seller_packages_load_error),
                        subtitle = loadError.orEmpty(),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                packages.isEmpty() -> {
                    FashEmptyState(
                        icon = Icons.Outlined.ShoppingBag,
                        title = stringResource(R.string.seller_packages_empty),
                        subtitle = stringResource(R.string.seller_packages_screen_subtitle),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = FashTheme.spacing.editorialStart,
                            vertical = 12.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.seller_packages_screen_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        items(packages, key = { it.code }) { pkg ->
                            SellerPackageCard(
                                pkg = pkg,
                                onBuy = { onBuyPackage(pkg) },
                            )
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SellerPackageCard(
    pkg: SellerProductPackage,
    onBuy: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val border = if (pkg.isBestSeller) {
        BorderStroke(2.dp, FashColors.Primary)
    } else {
        BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f))
    }
    val container = if (pkg.isBestSeller) {
        scheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        scheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        colors = CardDefaults.cardColors(containerColor = container),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = if (pkg.isBestSeller) 4.dp else 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pkg.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = pkg.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                pkg.badgeLabel?.takeIf { it.isNotBlank() }?.let { badge ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (pkg.isBestSeller) FashColors.Primary else scheme.secondaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (pkg.isBestSeller) {
                                Icon(
                                    imageVector = Icons.Outlined.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = scheme.onPrimary,
                                )
                            }
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (pkg.isBestSeller) scheme.onPrimary else scheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(
                    R.string.seller_packages_price_per_month,
                    formatVnd(pkg.priceVnd),
                    pkg.durationDays,
                ),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = FashColors.Primary,
                ),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            pkg.features.forEach { feature ->
                val title = featureTitle(feature.id, feature.name)
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
            Spacer(modifier = Modifier.height(16.dp))
            val comingSoon = !pkg.isReleased
            if (comingSoon) {
                OutlinedButton(
                    onClick = onBuy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.seller_packages_coming_soon_cta))
                }
            } else {
                Button(
                    onClick = onBuy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pkg.isBestSeller) FashColors.Primary else scheme.primary,
                    ),
                ) {
                    Text(stringResource(R.string.seller_packages_buy_now))
                }
            }
        }
    }
}

@Composable
private fun featureTitle(featureId: String, apiName: String? = null): String {
    apiName?.takeIf { it.isNotBlank() }?.let { return it }
    return when (featureId) {
    "authenticity_verify" -> stringResource(R.string.seller_packages_feature_authenticity)
    "explore_boost" -> stringResource(R.string.seller_packages_feature_explore_boost)
    "fanpage_spotlight" -> stringResource(R.string.seller_packages_feature_fanpage)
    "social_tiktok_instagram" -> stringResource(R.string.seller_packages_feature_social)
    else -> featureId
    }
}
