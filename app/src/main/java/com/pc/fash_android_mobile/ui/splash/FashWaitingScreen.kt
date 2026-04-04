package com.pc.fash_android_mobile.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.BeVietnamProFamily
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

private val SplashAccent = Color(0xFFF04D63)

private val ThumbCorner = RoundedCornerShape(28.dp)
private val GrayscaleFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** After this delay, show progress + label so long cold starts / session checks feel responsive. */
private const val LONG_WAIT_FEEDBACK_MS = 2_500L

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
    val centerBreathScale by infiniteTransition.animateFloat(
        initialValue = 0.988f,
        targetValue = 1.012f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "centerBreathScale",
    )

    var showLongWaitFeedback by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(LONG_WAIT_FEEDBACK_MS)
        showLongWaitFeedback = true
    }

    val stillLoadingCd = stringResource(R.string.waiting_screen_still_loading_cd)

    val cornerAngle = cornerFloat * 2f * PI.toFloat()
    val topCornerX = sin(cornerAngle) * 5f
    val topCornerY = cos(cornerAngle * 0.65f) * 6f
    val bottomCornerX = cos(cornerAngle * 1.1f) * 6f
    val bottomCornerY = sin(cornerAngle * 0.85f) * 5f

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

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .scale(centerBreathScale),
        ) {
            WaitingScreenCenterEditorial()
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(
                visible = showLongWaitFeedback,
                enter = fadeIn(animationSpec = tween(420)) +
                    slideInVertically(initialOffsetY = { it / 5 }),
                exit = fadeOut(animationSpec = tween(200)),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .width(168.dp)
                            .height(3.dp)
                            .semantics { contentDescription = stillLoadingCd },
                        color = SplashAccent,
                        trackColor = dividerColor.copy(alpha = 0.35f),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.waiting_screen_still_loading),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = onSurfaceMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
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
private fun WaitingScreenCenterEditorial(
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val onSurfaceMuted = scheme.onSurfaceVariant

    var showEyebrow by remember { mutableStateOf(false) }
    var showHeadline by remember { mutableStateOf(false) }
    var showAccentTrack by remember { mutableStateOf(false) }
    var showBody by remember { mutableStateOf(false) }
    var showMantra by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(120)
        showEyebrow = true
        delay(180)
        showHeadline = true
        delay(120)
        showAccentTrack = true
        delay(160)
        showBody = true
        delay(140)
        showMantra = true
    }

    val accentBarWidth by animateDpAsState(
        targetValue = if (showAccentTrack) 72.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "accentBarWidth",
    )

    val accentPulse = rememberInfiniteTransition(label = "accentPulse")
    val accentPulseAlpha by accentPulse.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "accentPulseAlpha",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
    ) {
        AnimatedVisibility(
            visible = showEyebrow,
            enter = fadeIn(animationSpec = tween(420)) +
                slideInVertically { it / 4 },
        ) {
            Text(
                text = stringResource(R.string.waiting_screen_eyebrow),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.Center,
                ),
                color = onSurfaceMuted.copy(alpha = 0.85f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        AnimatedVisibility(
            visible = showHeadline,
            enter = fadeIn(animationSpec = tween(480)) +
                slideInVertically { it / 4 },
        ) {
            Text(
                text = stringResource(R.string.waiting_screen_headline),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                ),
                color = scheme.onSurface,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .height(4.dp)
                .width(accentBarWidth)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            FashColors.Primary.copy(alpha = accentPulseAlpha),
                            SplashAccent.copy(alpha = 0.85f * accentPulseAlpha),
                        ),
                    ),
                ),
        )
        Spacer(modifier = Modifier.height(14.dp))
        AnimatedVisibility(
            visible = showBody,
            enter = fadeIn(animationSpec = tween(420)) +
                slideInVertically { it / 6 },
        ) {
            Text(
                text = stringResource(R.string.waiting_screen_line_2),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Normal,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                ),
                color = onSurfaceMuted,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        AnimatedVisibility(
            visible = showMantra,
            enter = fadeIn(animationSpec = tween(400)) +
                slideInVertically { it / 6 },
        ) {
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
