package com.pc.fash_android_mobile.ui.welcome

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.components.FashBrandMarkText
import com.pc.fash_android_mobile.ui.components.FashPrimaryButton
import com.pc.fash_android_mobile.ui.locale.LoginLanguageToggle
import com.pc.fash_android_mobile.ui.theme.FashBrandTypography
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import kotlinx.coroutines.launch

private data class WelcomeIntroSlideDef(
    val titleRes: Int,
    val bodyRes: Int,
    val icon: ImageVector,
    val gradient: List<Color>,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeIntroScreen(
    onSignIn: () -> Unit,
    onBrowseGuest: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val spacing = FashTheme.spacing
    val slides = listOf(
        WelcomeIntroSlideDef(
            R.string.welcome_intro_slide1_title,
            R.string.welcome_intro_slide1_body,
            Icons.Outlined.Storefront,
            listOf(Color(0x14FF4B64), Color(0x08FF4B64)),
        ),
        WelcomeIntroSlideDef(
            R.string.welcome_intro_slide2_title,
            R.string.welcome_intro_slide2_body,
            Icons.Outlined.Search,
            listOf(Color(0x14D9A066), Color(0x08FF4B64)),
        ),
        WelcomeIntroSlideDef(
            R.string.welcome_intro_slide3_title,
            R.string.welcome_intro_slide3_body,
            Icons.Outlined.Checkroom,
            listOf(Color(0x12000000), Color(0x18FF4B64)),
        ),
        WelcomeIntroSlideDef(
            R.string.welcome_intro_slide4_title,
            R.string.welcome_intro_slide4_body,
            Icons.Outlined.Forum,
            listOf(Color(0x14FF4B64), Color(0x10FF4B64)),
        ),
    )
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val isLastSlide = pagerState.currentPage >= slides.lastIndex

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = spacing.spacing4),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.editorialStart)
                .padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onSignIn) {
                Text(
                    text = stringResource(R.string.welcome_intro_skip),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            LoginLanguageToggle()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.editorialStart),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FashBrandMarkText(
                text = stringResource(R.string.brand_wordmark),
                style = FashBrandTypography.markBoldItalicLarge,
            )
            Text(
                text = stringResource(R.string.login_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = spacing.editorialStart),
        ) { page ->
            WelcomeIntroSlideCard(slide = slides[page])
        }

        WelcomeIntroPageIndicator(
            pageCount = slides.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 10.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.editorialStart)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isLastSlide) {
                FashPrimaryButton(
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.welcome_intro_sign_in),
                )
                onBrowseGuest?.let { browse ->
                    TextButton(
                        onClick = browse,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.welcome_intro_browse_guest),
                            style = MaterialTheme.typography.labelLarge,
                            color = FashColors.Primary,
                        )
                    }
                }
            } else {
                FashPrimaryButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                pagerState.currentPage + 1,
                                animationSpec = tween(280, easing = FastOutSlowInEasing),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.welcome_intro_next),
                )
            }
        }
    }
}

@Composable
private fun WelcomeIntroPageIndicator(
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
                label = "welcomeIntroDot",
            )
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (selected) FashColors.Primary else scheme.outlineVariant.copy(alpha = 0.55f),
                    ),
            )
        }
    }
}

@Composable
private fun WelcomeIntroSlideCard(slide: WelcomeIntroSlideDef) {
    val shape = RoundedCornerShape(28.dp)
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(slide.gradient),
                    shape,
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(28.dp))
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(FashColors.Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = slide.icon,
                        contentDescription = null,
                        tint = FashColors.Primary,
                        modifier = Modifier.size(40.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(slide.titleRes),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(slide.bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
