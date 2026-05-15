package com.pc.fash_android_mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.CloseFullscreen
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WavingHand
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.promo.AppPromoCampaign
import com.pc.fash_android_mobile.data.promo.AppPromoCampaignKind
import com.pc.fash_android_mobile.ui.components.FashAsyncImage
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

private val PromoCardCompactMaxWidth = 400.dp
private val PromoCardCompactWidthFraction = 0.86f
private val PromoCardCompactMaxHeightFraction = 0.72f
private val PromoHeroCompactHeight = 112.dp

private val PromoCardExpandedMaxWidth = 520.dp
private val PromoCardExpandedWidthFraction = 0.94f
private val PromoCardExpandedMaxHeightFraction = 0.88f
private val PromoHeroExpandedHeight = 160.dp

/**
 * Blocking app-open promo: full-screen scrim, compact center card with optional expand.
 * Only the close control and CTAs dismiss — no tap-outside.
 */
@Composable
fun FashAppPromoOverlayDialog(
    campaign: AppPromoCampaign?,
    onDismiss: () -> Unit,
    onPrimaryClick: (AppPromoCampaign) -> Unit,
    onSecondaryClick: ((AppPromoCampaign) -> Unit)? = null,
) {
    if (campaign == null) return
    val scheme = MaterialTheme.colorScheme
    val titleText = campaign.remoteTitle
        ?: campaign.titleRes?.let { stringResource(it) }
        ?: return
    val messageText = campaign.remoteMessage
        ?: campaign.messageRes?.let { stringResource(it) }
        ?: return
    val primaryLabel = campaign.remotePrimaryLabel
        ?: campaign.primaryActionRes?.let { stringResource(it) }
        ?: return
    val secondaryLabel = campaign.remoteSecondaryLabel
        ?: campaign.secondaryActionRes?.let { stringResource(it) }
    val badgeText = campaign.remoteBadge
        ?: campaign.badgeRes?.let { stringResource(it) }
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(campaign.id, campaign.version) {
        isExpanded = false
    }

    val cardMaxWidth = if (isExpanded) PromoCardExpandedMaxWidth else PromoCardCompactMaxWidth
    val cardWidthFraction = if (isExpanded) PromoCardExpandedWidthFraction else PromoCardCompactWidthFraction
    val cardMaxHeightFraction = if (isExpanded) PromoCardExpandedMaxHeightFraction else PromoCardCompactMaxHeightFraction
    val heroHeight = if (isExpanded) PromoHeroExpandedHeight else PromoHeroCompactHeight
    val contentPadding = if (isExpanded) 26.dp else 22.dp
    val titleStyle = if (isExpanded) {
        MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
    } else {
        MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
    }
    val messageStyle = if (isExpanded) {
        MaterialTheme.typography.bodyLarge
    } else {
        MaterialTheme.typography.bodyMedium
    }
    val heroIconSize = if (isExpanded) 36.dp else 28.dp
    val heroBadgeIconBox = if (isExpanded) 64.dp else 52.dp

    Dialog(
        onDismissRequest = { /* must use close / CTA */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.scrim.copy(alpha = 0.62f)),
        ) {
            val fractionWidth = maxWidth * cardWidthFraction
            val cardWidth = if (fractionWidth < cardMaxWidth) fractionWidth else cardMaxWidth
            val cardMaxHeight = maxHeight * cardMaxHeightFraction

            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = cardWidth)
                    .fillMaxWidth(cardWidthFraction)
                    .heightIn(max = cardMaxHeight),
                shape = RoundedCornerShape(if (isExpanded) 24.dp else 22.dp),
                color = scheme.surfaceContainerHigh,
                tonalElevation = 4.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.4f)),
            ) {
                Box(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        PromoHeroSection(
                            kind = campaign.kind,
                            badge = badgeText,
                            imageUrls = campaign.remoteImageUrls,
                            heroHeight = heroHeight,
                            iconSize = heroIconSize,
                            iconBoxSize = heroBadgeIconBox,
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = contentPadding),
                        ) {
                            Spacer(Modifier.height(if (isExpanded) 20.dp else 16.dp))
                            Text(
                                text = titleText,
                                style = titleStyle,
                                color = scheme.onSurface,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(if (isExpanded) 10.dp else 8.dp))
                            Text(
                                text = messageText,
                                style = messageStyle,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = contentPadding)
                                .padding(
                                    top = if (isExpanded) 22.dp else 18.dp,
                                    bottom = if (isExpanded) 22.dp else 18.dp,
                                ),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Button(
                                onClick = { onPrimaryClick(campaign) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = if (isExpanded) 52.dp else 48.dp),
                                shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = scheme.primary,
                                    contentColor = scheme.onPrimary,
                                ),
                            ) {
                                Text(
                                    text = primaryLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                )
                            }
                            secondaryLabel?.let { secondaryText ->
                                TextButton(
                                    onClick = {
                                        if (onSecondaryClick != null) {
                                            onSecondaryClick(campaign)
                                        } else {
                                            onDismiss()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = secondaryText,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = scheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = if (isExpanded) {
                                    Icons.Outlined.CloseFullscreen
                                } else {
                                    Icons.Outlined.OpenInFull
                                },
                                contentDescription = stringResource(
                                    if (isExpanded) R.string.app_promo_cd_collapse else R.string.app_promo_cd_expand,
                                ),
                                tint = scheme.onSurface,
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.app_promo_cd_close),
                                tint = scheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PromoHeroSection(
    kind: AppPromoCampaignKind,
    badge: String?,
    imageUrls: List<String>,
    heroHeight: Dp,
    iconSize: Dp,
    iconBoxSize: Dp,
) {
    val scheme = MaterialTheme.colorScheme
    if (kind == AppPromoCampaignKind.Remote && imageUrls.isNotEmpty()) {
        val pagerState = rememberPagerState(pageCount = { imageUrls.size })
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight),
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                FashAsyncImage(
                    model = imageUrls[page],
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            badge?.let { label ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(percent = 50),
                    color = scheme.primary.copy(alpha = 0.14f),
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.primary,
                    )
                }
            }
        }
        return
    }
    val (icon, tint, gradient) = when (kind) {
        AppPromoCampaignKind.Welcome -> Triple(
            Icons.Outlined.WavingHand,
            FashColors.Success,
            listOf(FashColors.Success.copy(alpha = 0.2f), scheme.surfaceContainerHigh),
        )
        AppPromoCampaignKind.AppRating -> Triple(
            Icons.Default.Star,
            FashColors.Primary,
            listOf(FashColors.Primary.copy(alpha = 0.18f), scheme.surfaceContainerHigh),
        )
        AppPromoCampaignKind.SellerPackage -> Triple(
            Icons.Outlined.Storefront,
            FashColors.Primary,
            listOf(FashColors.Primary.copy(alpha = 0.16f), scheme.surfaceContainerHigh),
        )
        AppPromoCampaignKind.KycVerification -> Triple(
            Icons.Outlined.VerifiedUser,
            FashColors.Primary,
            listOf(FashColors.Primary.copy(alpha = 0.14f), scheme.surfaceContainerHigh),
        )
        AppPromoCampaignKind.Remote -> Triple(
            Icons.Outlined.WavingHand,
            FashColors.Primary,
            listOf(FashColors.Primary.copy(alpha = 0.14f), scheme.surfaceContainerHigh),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .background(Brush.verticalGradient(gradient)),
        contentAlignment = Alignment.Center,
    ) {
        if (kind == AppPromoCampaignKind.AppRating) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(5) { index ->
                    Icon(
                        imageVector = if (index < 4) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(iconSize),
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(iconBoxSize)
                    .background(tint.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
        badge?.let { label ->
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                shape = RoundedCornerShape(percent = 50),
                color = tint.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = tint,
                )
            }
        }
    }
}
