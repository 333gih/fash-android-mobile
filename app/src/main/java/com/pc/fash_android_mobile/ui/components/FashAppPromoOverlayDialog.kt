package com.pc.fash_android_mobile.ui.components

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
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.promo.AppPromoCampaign
import com.pc.fash_android_mobile.data.promo.AppPromoCampaignKind
import com.pc.fash_android_mobile.data.promo.sanitizePromoDisplayString
import com.pc.fash_android_mobile.ui.feed.resolveListingImageUrl
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

private val PromoCardMaxWidth = 380.dp
private val PromoCardWidthFraction = 0.88f
private val PromoCardMaxHeightFraction = 0.82f

/**
 * Blocking app-open promo: content-adaptive card (hero only when image/icon warranted).
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
        ?: ""
    val primaryLabel = campaign.remotePrimaryLabel
        ?: campaign.primaryActionRes?.let { stringResource(it) }
        ?: return
    val secondaryLabel = sanitizePromoDisplayString(campaign.remoteSecondaryLabel)
        ?: campaign.secondaryActionRes?.let { stringResource(it) }
    val badgeText = sanitizePromoDisplayString(campaign.remoteBadge)
        ?: campaign.badgeRes?.let { stringResource(it) }

    val layout = AppPromoDialogLayout.from(
        campaign = campaign,
        title = titleText,
        message = messageText,
        primaryLabel = primaryLabel,
        secondaryLabel = secondaryLabel,
        badge = badgeText,
    )

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
                .background(Color.Black.copy(alpha = 0.52f)),
        ) {
            val fractionWidth = maxWidth * PromoCardWidthFraction
            val cardWidth = if (fractionWidth < PromoCardMaxWidth) fractionWidth else PromoCardMaxWidth
            val cardMaxHeight = maxHeight * PromoCardMaxHeightFraction
            val heroHeight = layout.heroHeight(cardWidth)
            val bodyTopPadding = if (heroHeight != null) 18.dp else 14.dp
            val ctaTopPadding = when {
                heroHeight != null -> 18.dp
                layout.showMessage -> 16.dp
                else -> 14.dp
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = cardWidth)
                    .fillMaxWidth(PromoCardWidthFraction)
                    .heightIn(max = cardMaxHeight)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                color = scheme.surface,
                shadowElevation = 20.dp,
                tonalElevation = 2.dp,
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        if (heroHeight != null) {
                            PromoHeroSection(
                                kind = campaign.kind,
                                layout = layout,
                                heroHeight = heroHeight,
                                iconSize = if (layout.showIconHero) 28.dp else 32.dp,
                                iconBoxSize = if (layout.showIconHero) 52.dp else 60.dp,
                            )
                        }
                        if (layout.showBadgeInline || layout.showTitle || layout.showMessage) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(top = bodyTopPadding),
                            ) {
                                if (layout.showBadgeInline) {
                                    PromoInlineBadge(label = layout.badge!!, scheme = scheme)
                                    Spacer(Modifier.height(10.dp))
                                }
                                if (layout.showTitle) {
                                    Text(
                                        text = layout.title,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = scheme.onSurface,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                if (layout.showMessage) {
                                    if (layout.showTitle) {
                                        Spacer(Modifier.height(6.dp))
                                    }
                                    Text(
                                        text = layout.message!!,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = scheme.onSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = ctaTopPadding, bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Button(
                                onClick = { onPrimaryClick(campaign) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(FashTheme.spacing.radiusCard))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                FashColors.Primary,
                                                FashColors.Primary.copy(alpha = 0.82f),
                                            ),
                                        ),
                                    ),
                                shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = scheme.onPrimary,
                                ),
                            ) {
                                Text(
                                    text = layout.primaryLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (layout.showSecondary) {
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
                                        text = layout.secondaryLabel!!,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = scheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(36.dp)
                            .background(
                                if (heroHeight != null) {
                                    Color.Black.copy(alpha = 0.42f)
                                } else {
                                    scheme.surfaceContainerHigh
                                },
                                CircleShape,
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.app_promo_cd_close),
                            tint = if (heroHeight != null) Color.White else scheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PromoInlineBadge(label: String, scheme: androidx.compose.material3.ColorScheme) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = scheme.primary.copy(alpha = 0.12f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = scheme.primary,
        )
    }
}

@Composable
private fun PromoHeroSection(
    kind: AppPromoCampaignKind,
    layout: AppPromoDialogLayout,
    heroHeight: Dp,
    iconSize: Dp,
    iconBoxSize: Dp,
) {
    val scheme = MaterialTheme.colorScheme
    if (layout.showImageHero) {
        val pagerState = rememberPagerState(pageCount = { layout.imageUrls.size })
        val multiImage = layout.imageUrls.size > 1
        val currentPage = pagerState.currentPage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (multiImage) {
                            Modifier.semantics {
                                contentDescription = "Image ${currentPage + 1} of ${layout.imageUrls.size}"
                            }
                        } else {
                            Modifier
                        },
                    ),
            ) { page ->
                FashAsyncImage(
                    model = resolveListingImageUrl(layout.imageUrls[page]),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.16f),
                            ),
                            startY = heroHeight.value * 0.5f,
                        ),
                    ),
            )
            if (multiImage) {
                FashPromoPageIndicator(
                    pageCount = layout.imageUrls.size,
                    currentPage = currentPage,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp),
                )
            }
            if (layout.showBadgeOnHero) {
                PromoHeroBadge(
                    label = layout.badge!!,
                    background = scheme.primary,
                    content = scheme.onPrimary,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }
        }
        return
    }

    val (icon, tint, gradient) = when (kind) {
        AppPromoCampaignKind.Welcome -> Triple(
            Icons.Outlined.WavingHand,
            FashColors.Success,
            listOf(FashColors.Success.copy(alpha = 0.22f), scheme.surfaceContainerHigh),
        )
        AppPromoCampaignKind.AppRating -> Triple(
            Icons.Default.Star,
            FashColors.Primary,
            listOf(FashColors.Primary.copy(alpha = 0.2f), scheme.surfaceContainerHigh),
        )
        AppPromoCampaignKind.SellerPackage -> Triple(
            Icons.Outlined.Storefront,
            FashColors.Primary,
            listOf(FashColors.Primary.copy(alpha = 0.18f), scheme.surfaceContainerHigh),
        )
        AppPromoCampaignKind.KycVerification -> Triple(
            Icons.Outlined.VerifiedUser,
            FashColors.Primary,
            listOf(FashColors.Primary.copy(alpha = 0.16f), scheme.surfaceContainerHigh),
        )
        AppPromoCampaignKind.Remote -> Triple(
            Icons.Outlined.WavingHand,
            FashColors.Primary,
            listOf(FashColors.Primary.copy(alpha = 0.16f), scheme.surfaceContainerHigh),
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
        if (layout.showBadgeOnHero) {
            PromoHeroBadge(
                label = layout.badge!!,
                background = tint,
                content = Color.White,
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
    }
}

@Composable
private fun PromoHeroBadge(
    label: String,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(12.dp),
        shape = RoundedCornerShape(percent = 50),
        color = background,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = content,
        )
    }
}
