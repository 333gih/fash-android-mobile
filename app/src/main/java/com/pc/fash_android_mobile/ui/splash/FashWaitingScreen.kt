package com.pc.fash_android_mobile.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.BeVietnamProFamily
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

private val SplashAccent = Color(0xFFF04D63)

private val ThumbCorner = RoundedCornerShape(28.dp)
private val GrayscaleFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * Full-screen waiting / splash / transition surface: editorial watermark, corner thumbs, GEN Z footer, step dots.
 * Center copy is typography-only (pre-loved / reuse messaging) — no placeholder image.
 */
@Composable
fun FashWaitingScreen(
    modifier: Modifier = Modifier,
    /** Third dot active (final onboarding step), matching the reference. */
    activeDotIndex: Int = 2,
) {
    val scheme = MaterialTheme.colorScheme
    val onSurfaceMuted = scheme.onSurfaceVariant
    val watermarkColor = scheme.onSurface.copy(alpha = 0.08f)
    val dividerColor = scheme.outlineVariant.copy(alpha = 0.55f)
    val dotInactive = scheme.outlineVariant.copy(alpha = 0.45f)

    val infiniteTransition = rememberInfiniteTransition(label = "fash_waiting")
    val watermarkDrift by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "watermarkDrift",
    )
    val cornerFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cornerFloat",
    )
    val dotWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dotWave",
    )
    val activeDotPulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "activeDotPulse",
    )

    val cornerAngle = cornerFloat * 2f * PI.toFloat()
    val topCornerX = sin(cornerAngle) * 5f
    val topCornerY = cos(cornerAngle * 0.65f) * 6f
    val bottomCornerX = cos(cornerAngle * 1.1f) * 6f
    val bottomCornerY = sin(cornerAngle * 0.85f) * 5f

    var centerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { centerVisible = true }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(scheme.surface),
        )
        WatermarkFash(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(0.72f)
                .offset(
                    x = maxWidth * 0.12f,
                    y = maxHeight * 0.08f + maxHeight * 0.05f * watermarkDrift,
                ),
            color = watermarkColor,
        )

        SplashCornerImage(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 22.dp, top = 20.dp)
                .offset(topCornerX.dp, topCornerY.dp)
                .width(96.dp)
                .height(148.dp),
            cropAlignment = Alignment.TopStart,
        )

        SplashCornerImage(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 108.dp)
                .offset(bottomCornerX.dp, bottomCornerY.dp)
                .width(152.dp)
                .height(104.dp),
            cropAlignment = Alignment.BottomEnd,
        )

        AnimatedVisibility(
            visible = centerVisible,
            enter = fadeIn(animationSpec = tween(520)) +
                scaleIn(initialScale = 0.94f, animationSpec = tween(520)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
            ) {
                Text(
                    text = stringResource(R.string.waiting_screen_headline),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    ),
                    color = scheme.onSurface,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.waiting_screen_line_2),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Normal,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center,
                    ),
                    color = onSurfaceMuted,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.waiting_screen_line_3),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.3.sp,
                    ),
                    color = FashColors.Primary,
                )
            }
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
                background = scheme.surface,
                dividerColor = dividerColor,
                labelColor = onSurfaceMuted,
            )
            Spacer(modifier = Modifier.height(20.dp))
            StepDots(
                activeIndex = activeDotIndex.coerceIn(0, 2),
                inactiveColor = dotInactive,
                dotWavePhase = dotWave,
                activePulseScale = activeDotPulse,
            )
        }
    }
}

@Composable
private fun WatermarkFash(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
) {
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
        color = color,
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
    dividerColor: Color,
    labelColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(
            modifier = Modifier.width(48.dp),
            color = dividerColor,
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
            color = labelColor,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .background(background),
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = dividerColor,
            thickness = 1.dp,
        )
    }
}

@Composable
private fun StepDots(
    activeIndex: Int,
    inactiveColor: Color,
    dotWavePhase: Float,
    activePulseScale: Float,
) {
    val waveAngle = dotWavePhase * 2f * PI.toFloat()
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { i ->
            val active = i == activeIndex
            val size = if (active) 10.dp else 8.dp
            val stagger = i * (2f * PI.toFloat() / 3f)
            val bounce = sin(waveAngle + stagger)
            val yOffset = (bounce * 5f).dp
            Box(
                modifier = Modifier
                    .offset(y = yOffset)
                    .then(
                        if (active) Modifier.scale(activePulseScale) else Modifier,
                    )
                    .size(size)
                    .clip(CircleShape)
                    .background(if (active) SplashAccent else inactiveColor),
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
