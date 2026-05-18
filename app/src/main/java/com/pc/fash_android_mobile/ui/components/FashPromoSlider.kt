package com.pc.fash_android_mobile.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.orders.pendingPaymentSliderAnchor
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import com.pc.fash_android_mobile.ui.theme.fashReadableOnGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Deep link / in-app target for a promo slide (from core-service CMS). */
data class FashPromoNav(
    val type: String,
    val payload: String = "",
)

/**
 * Admin-ready promo slide (orders, notifications, home, etc.).
 * [id] is stable for analytics. Use [titleRes]/[subtitleRes] for built-in copy or
 * [titleText]/[subtitleText] when loaded from [com.pc.fash_android_mobile.data.advertising.AdvertisingRepository].
 */
data class FashPromoSlideDef(
    val id: String,
    val titleRes: Int? = null,
    val subtitleRes: Int? = null,
    val titleText: String? = null,
    val subtitleText: String? = null,
    val gradient: List<Color>,
    val border: Color? = null,
    /** When null, UI uses [R.string.orders_promo_badge]. */
    val badgeText: String? = null,
    val bannerImageUrl: String? = null,
    val navigation: FashPromoNav? = null,
)

/** Promo carousel card height — shared with [FashBottomPromoAdStrip] on Orders / Notifications. */
val FashPromoCarouselCardHeight = 112.dp
private const val AutoAdvanceMs = 6_500L

/**
 * Horizontal promo pager (gradient cards, dots overlaid).
 *
 * @param slides CMS slides from core-service only; empty list hides the block.
 * @param reportPendingPaymentAnchor When true, registers bounds for global pending-payment banner placement (above slider).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FashPromoSlider(
    modifier: Modifier = Modifier,
    slides: List<FashPromoSlideDef> = emptyList(),
    reportPendingPaymentAnchor: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = FashTheme.spacing.editorialStart,
    ),
    onSlideClick: (FashPromoSlideDef, Int) -> Unit = { _, _ -> },
) {
    if (slides.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { slides.size })

    LaunchedEffect(pagerState, slides.size) {
        if (slides.size <= 1) return@LaunchedEffect
        while (isActive) {
            delay(AutoAdvanceMs)
            val next = (pagerState.currentPage + 1) % slides.size
            runCatching { pagerState.animateScrollToPage(next) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp)
            .then(if (reportPendingPaymentAnchor) Modifier.pendingPaymentSliderAnchor() else Modifier),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding,
            pageSpacing = 12.dp,
            verticalAlignment = Alignment.CenterVertically,
        ) { page ->
            val slide = slides[page]
            val cd = stringResource(
                R.string.orders_promo_pager_cd,
                page + 1,
                slides.size,
            )
            FashPromoCard(
                slide = slide,
                badge = slide.badgeText?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.orders_promo_badge),
                contentDescription = cd,
                onClick = { onSlideClick(slide, page) },
            )
        }

        if (slides.size > 1) {
            FashPromoPageIndicator(
                pageCount = slides.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
            )
        }
    }
}

/**
 * [Surface] (surfaceContainerLow) + [FashPromoSlider] — same chrome as Orders / Notifications
 * inline promo blocks.
 */
@Composable
fun FashPromoSliderBlock(
    modifier: Modifier = Modifier,
    slides: List<FashPromoSlideDef> = emptyList(),
    reportPendingPaymentAnchor: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = FashTheme.spacing.editorialStart,
    ),
    onSlideClick: (FashPromoSlideDef, Int) -> Unit = { _, _ -> },
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        FashPromoSlider(
            modifier = Modifier.fillMaxWidth(),
            slides = slides,
            reportPendingPaymentAnchor = reportPendingPaymentAnchor,
            contentPadding = contentPadding,
            onSlideClick = onSlideClick,
        )
    }
}

@Composable
private fun FashPromoCard(
    slide: FashPromoSlideDef,
    badge: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val banner = slide.bannerImageUrl?.takeIf { it.isNotBlank() }
    val titleColor = if (banner != null) Color.White else slide.gradient.fashReadableOnGradient()
    val subtitleColor = titleColor.copy(alpha = 0.92f)
    val shape = RoundedCornerShape(FashTheme.spacing.radiusCard)
    val titleStr = slide.titleText?.takeIf { it.isNotBlank() }
        ?: slide.titleRes?.let { stringResource(it) }.orEmpty()
    val subtitleStr = slide.subtitleText?.takeIf { it.isNotBlank() }
        ?: slide.subtitleRes?.let { stringResource(it) }.orEmpty()
    val useImageBackground = banner != null
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(FashPromoCarouselCardHeight)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
            }
            .clip(shape)
            .then(
                slide.border?.let { b -> Modifier.border(1.dp, b, shape) } ?: Modifier,
            )
            .clickable(onClick = onClick),
    ) {
        if (useImageBackground) {
            FashAsyncImage(
                model = banner,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.55f),
                            0.45f to Color.Black.copy(alpha = 0.28f),
                            1f to Color.Black.copy(alpha = 0.62f),
                        ),
                    ),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(slide.gradient)),
            )
        }
        Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = titleColor.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterStart)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(end = 48.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = titleStr,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitleStr,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun FashPromoPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (selected) 18.dp else 6.dp,
                animationSpec = tween(durationMillis = 220),
                label = "fashPromoDot",
            )
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (selected) FashColors.Primary else scheme.outlineVariant.copy(alpha = 0.7f),
                    ),
            )
        }
    }
}
