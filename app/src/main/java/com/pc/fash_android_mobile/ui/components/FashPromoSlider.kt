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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import com.pc.fash_android_mobile.ui.theme.fashReadableOnGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Admin-ready promo slide (orders, notifications, home, etc.).
 * [id] is stable for analytics and deep links when wired from CMS / remote config.
 */
data class FashPromoSlideDef(
    val id: String,
    val titleRes: Int,
    val subtitleRes: Int,
    val gradient: List<Color>,
    val border: Color? = null,
)

private val FashPromoCardHeight = 112.dp
private const val AutoAdvanceMs = 6_500L

fun defaultFashPromoSlides(scheme: ColorScheme): List<FashPromoSlideDef> = listOf(
    FashPromoSlideDef(
        id = "bundle_shipping",
        titleRes = R.string.orders_promo_slide1_title,
        subtitleRes = R.string.orders_promo_slide1_subtitle,
        gradient = listOf(FashColors.PrimaryDeep, FashColors.Primary),
    ),
    FashPromoSlideDef(
        id = "protected_payments",
        titleRes = R.string.orders_promo_slide2_title,
        subtitleRes = R.string.orders_promo_slide2_subtitle,
        gradient = listOf(FashColors.SecondaryWarm, FashColors.TertiaryAccent),
    ),
    FashPromoSlideDef(
        id = "seller_perks",
        titleRes = R.string.orders_promo_slide3_title,
        subtitleRes = R.string.orders_promo_slide3_subtitle,
        gradient = listOf(scheme.surfaceContainerLow, scheme.surfaceVariant),
        border = scheme.outlineVariant.copy(alpha = 0.65f),
    ),
)

/**
 * Horizontal promo pager (gradient cards, dots overlaid).
 *
 * **Default slides** ([defaultFashPromoSlides], `orders_promo_*` strings) are shared with
 * Orders, Notifications, Home, Explore, and Chat — pass [slides] only when overriding (e.g. CMS).
 *
 * @param slides When null, uses [defaultFashPromoSlides]. Pass a non-null list from ViewModel when admin API is ready.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FashPromoSlider(
    modifier: Modifier = Modifier,
    slides: List<FashPromoSlideDef>? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = FashTheme.spacing.editorialStart,
    ),
    onSlideClick: (slideId: String, pageIndex: Int) -> Unit = { _, _ -> },
) {
    val scheme = MaterialTheme.colorScheme
    val resolved = slides ?: remember(scheme) { defaultFashPromoSlides(scheme) }
    if (resolved.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { resolved.size })

    LaunchedEffect(pagerState, resolved.size) {
        if (resolved.size <= 1) return@LaunchedEffect
        while (isActive) {
            delay(AutoAdvanceMs)
            val next = (pagerState.currentPage + 1) % resolved.size
            runCatching { pagerState.animateScrollToPage(next) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding,
            pageSpacing = 12.dp,
            verticalAlignment = Alignment.CenterVertically,
        ) { page ->
            val slide = resolved[page]
            val cd = stringResource(
                R.string.orders_promo_pager_cd,
                page + 1,
                resolved.size,
            )
            FashPromoCard(
                slide = slide,
                badge = stringResource(R.string.orders_promo_badge),
                contentDescription = cd,
                onClick = { onSlideClick(slide.id, page) },
            )
        }

        if (resolved.size > 1) {
            FashPromoPageIndicator(
                pageCount = resolved.size,
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
 * inline promo blocks. Use [slides] = null for the shared default deck.
 */
@Composable
fun FashPromoSliderBlock(
    modifier: Modifier = Modifier,
    slides: List<FashPromoSlideDef>? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = FashTheme.spacing.editorialStart,
    ),
    onSlideClick: (slideId: String, pageIndex: Int) -> Unit = { _, _ -> },
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        FashPromoSlider(
            modifier = Modifier.fillMaxWidth(),
            slides = slides,
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
    val titleColor = slide.gradient.fashReadableOnGradient()
    val subtitleColor = titleColor.copy(alpha = 0.92f)
    val shape = RoundedCornerShape(FashTheme.spacing.radiusCard)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(FashPromoCardHeight)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
            }
            .clip(shape)
            .background(Brush.horizontalGradient(slide.gradient))
            .then(
                slide.border?.let { b -> Modifier.border(1.dp, b, shape) } ?: Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = titleColor.copy(alpha = 0.85f),
            modifier = Modifier.align(Alignment.TopEnd),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterStart)
                .padding(end = 48.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(slide.titleRes),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(slide.subtitleRes),
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FashPromoPageIndicator(
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
