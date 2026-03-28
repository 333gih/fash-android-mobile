package com.pc.fash_android_mobile.ui.explore

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
import androidx.compose.material3.MaterialTheme
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

private val PromoCardHeight = 128.dp
private val AutoAdvanceMs = 6_000L

private data class PromoSlide(
    val titleRes: Int,
    val subtitleRes: Int,
    val gradient: List<Color>,
    val border: Color? = null,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExplorePromoCarousel(
    modifier: Modifier = Modifier,
    /** Optional: analytics, deep link, or future in-app actions. */
    onSlideClick: (pageIndex: Int) -> Unit = {},
) {
    val slides = remember {
        listOf(
            PromoSlide(
                titleRes = R.string.explore_promo_slide1_title,
                subtitleRes = R.string.explore_promo_slide1_subtitle,
                gradient = listOf(FashColors.PrimaryDeep, FashColors.Primary),
            ),
            PromoSlide(
                titleRes = R.string.explore_promo_slide2_title,
                subtitleRes = R.string.explore_promo_slide2_subtitle,
                gradient = listOf(FashColors.SecondaryWarm, FashColors.TertiaryAccent),
            ),
            PromoSlide(
                titleRes = R.string.explore_promo_slide3_title,
                subtitleRes = R.string.explore_promo_slide3_subtitle,
                gradient = listOf(FashColors.SurfaceContainerLow, FashColors.SurfaceVariantCream),
                border = FashColors.OutlineVariant.copy(alpha = 0.65f),
            ),
        )
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })

    LaunchedEffect(pagerState) {
        while (isActive) {
            delay(AutoAdvanceMs)
            val next = (pagerState.currentPage + 1) % slides.size
            runCatching { pagerState.animateScrollToPage(next) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = FashTheme.spacing.editorialStart,
                end = FashTheme.spacing.editorialEnd,
            ),
            pageSpacing = 12.dp,
            verticalAlignment = Alignment.CenterVertically,
        ) { page ->
            val slide = slides[page]
            val cd = stringResource(
                R.string.explore_promo_pager_cd,
                page + 1,
                slides.size,
            )
            ExplorePromoCard(
                slide = slide,
                badge = stringResource(R.string.explore_promo_badge),
                contentDescription = cd,
                onClick = { onSlideClick(page) },
            )
        }

        PromoPageIndicator(
            pageCount = slides.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .padding(top = 10.dp)
                .align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun ExplorePromoCard(
    slide: PromoSlide,
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
            .height(PromoCardHeight)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
            }
            .clip(shape)
            .background(Brush.horizontalGradient(slide.gradient))
            .then(
                slide.border?.let { b -> Modifier.border(1.dp, b, shape) } ?: Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
                .padding(end = 56.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(slide.titleRes),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(slide.subtitleRes),
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PromoPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
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
                label = "promoDot",
            )
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (selected) FashColors.Primary else FashColors.OutlineVariant.copy(alpha = 0.7f),
                    ),
            )
        }
    }
}
