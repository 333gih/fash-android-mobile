package com.pc.fash_android_mobile.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.BeVietnamProFamily
import com.pc.fash_android_mobile.ui.theme.FashTheme

private val SplashBg = Color(0xFFFAF9F9)
private val SplashAccent = Color(0xFFF04D63)
private val SplashBodyText = Color(0xFF4A4A4A)
private val SplashWatermark = Color(0xFFF2F2F2)
private val SplashDotInactive = Color(0xFFFFD8DE)
private val SplashDivider = Color(0xFFDEDDDD)

private val ThumbCorner = RoundedCornerShape(28.dp)
private val GrayscaleFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * Full-screen waiting / splash / transition surface matching the Fash onboarding art:
 * centered wordmark + tagline, faint rotated watermark, corner photo thumbs, GEN Z footer, step dots.
 */
@Composable
fun FashWaitingScreen(
    modifier: Modifier = Modifier,
    /** Third dot active (final onboarding step), matching the reference. */
    activeDotIndex: Int = 2,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFDFC),
                            SplashBg,
                            Color(0xFFF7F2F4),
                        ),
                        center = Offset(wPx * 0.35f, hPx * 0.25f),
                        radius = maxOf(wPx, hPx) * 0.95f,
                    ),
                ),
        )
        WatermarkFash(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(0.72f)
                .offset(x = maxWidth * 0.12f, y = maxHeight * 0.08f),
        )

        SplashCornerImage(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 22.dp, top = 20.dp)
                .width(96.dp)
                .height(148.dp),
            cropAlignment = Alignment.TopStart,
        )

        SplashCornerImage(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 108.dp)
                .width(152.dp)
                .height(104.dp),
            cropAlignment = Alignment.BottomEnd,
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.splash_wordmark),
                style = TextStyle(
                    fontFamily = BeVietnamProFamily,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontSize = 52.sp,
                    lineHeight = 56.sp,
                    letterSpacing = (-0.5).sp,
                ),
                color = SplashAccent,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.login_tagline),
                style = TextStyle(
                    fontFamily = BeVietnamProFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                ),
                color = SplashBodyText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GenZFooterLine(
                label = stringResource(R.string.splash_footer_gen_z),
                background = SplashBg,
            )
            Spacer(modifier = Modifier.height(20.dp))
            StepDots(activeIndex = activeDotIndex.coerceIn(0, 2))
        }
    }
}

@Composable
private fun WatermarkFash(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.splash_wordmark),
        style = TextStyle(
            fontFamily = BeVietnamProFamily,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Normal,
            fontSize = 132.sp,
            lineHeight = 132.sp,
            letterSpacing = (-2).sp,
        ),
        color = SplashWatermark,
        modifier = modifier.rotate(-90f),
    )
}

@Composable
private fun SplashCornerImage(
    modifier: Modifier,
    cropAlignment: Alignment,
) {
    Image(
        // Vector drawable only — layer-list XML is not supported by painterResource() (crashes at runtime).
        painter = painterResource(R.drawable.login_hero_trench),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = cropAlignment,
        colorFilter = GrayscaleFilter,
        modifier = modifier.clip(ThumbCorner),
    )
}

@Composable
private fun GenZFooterLine(
    label: String,
    background: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(
            modifier = Modifier.width(48.dp),
            color = SplashDivider,
            thickness = 1.dp,
        )
        Text(
            text = label,
            style = TextStyle(
                fontFamily = BeVietnamProFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 1.6.sp,
            ),
            color = SplashBodyText,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .background(background),
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = SplashDivider,
            thickness = 1.dp,
        )
    }
}

@Composable
private fun StepDots(activeIndex: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { i ->
            val active = i == activeIndex
            val size = if (active) 10.dp else 8.dp
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(if (active) SplashAccent else SplashDotInactive),
            )
        }
    }
}

/**
 * Fades the same full-screen treatment over [content]. Use when changing routes or loading
 * so the wait feels intentional (toggle [visible] and pair with [runFashWaitWithMinDuration]).
 */
@Composable
fun FashWaitingOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxSize(),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        FashWaitingScreen()
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun FashWaitingScreenPreview() {
    FashTheme {
        FashWaitingScreen()
    }
}
