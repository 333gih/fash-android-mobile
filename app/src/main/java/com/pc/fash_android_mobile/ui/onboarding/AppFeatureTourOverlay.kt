package com.pc.fash_android_mobile.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.components.FashMascotGuideImage
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import kotlinx.coroutines.delay

@Composable
fun AppFeatureTourOverlay(
    visible: Boolean,
    anchors: Map<FeatureTourAnchor, LayoutCoordinates>,
    currentStep: AppTourStep,
    onStepChange: (AppTourStep) -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    var overlayCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val density = LocalDensity.current
    val holePadding = 10.dp
    val corner = 18.dp
    val scrim = MaterialTheme.colorScheme.scrim.copy(alpha = 0.58f)

    val anchor = currentStep.anchor()
    val holeRect = remember(currentStep, anchor, anchors, overlayCoords, density) {
        val a = anchor?.let { anchors[it] } ?: return@remember null
        val overlay = overlayCoords ?: return@remember null
        if (!a.isAttached || !overlay.isAttached) return@remember null
        val pad = with(density) { holePadding.toPx() }
        val ax = a.positionInWindow().x
        val ay = a.positionInWindow().y
        val ox = overlay.positionInWindow().x
        val oy = overlay.positionInWindow().y
        val left = ax - ox - pad
        val top = ay - oy - pad
        val w = a.size.width + pad * 2
        val h = a.size.height + pad * 2
        if (w <= 0f || h <= 0f) return@remember null
        Rect(Offset(left, top), Size(w, h))
    }

    // Wait one frame after tab switch so anchors re-layout before we read hole (reduces flicker).
    var holeReady by remember(currentStep) { mutableStateOf(false) }
    LaunchedEffect(currentStep) {
        holeReady = false
        delay(48)
        holeReady = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayCoords = it },
    ) {
        val showHole = anchor != null && holeRect != null && holeReady
        val cornerPx = with(density) { corner.toPx() }
        val hr = if (showHole) holeRect else null

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
        ) {
            drawRect(color = scrim)
            if (hr != null) {
                drawRoundRect(
                    color = androidx.compose.ui.graphics.Color.Black,
                    topLeft = hr.topLeft,
                    size = hr.size,
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    blendMode = BlendMode.Clear,
                )
            }
        }

        if (hr != null) {
            Canvas(Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = FashColors.Primary,
                    topLeft = hr.topLeft,
                    size = hr.size,
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = 2.5.dp.toPx()),
                )
            }
        }

        val stepIndex = currentStep.ordinal
        val total = AppTourStep.entries.size
        val progress = (stepIndex + 1f) / total

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = FashTheme.spacing.spacing4),
        ) {
            val cardMax = 400.dp
            val maxHpx = with(density) { maxHeight.toPx() }
            val verticalBias = when {
                currentStep == AppTourStep.Intro -> 0.5f
                holeRect != null && holeReady && anchor != null -> {
                    val mid = holeRect.center.y
                    if (mid < maxHpx * 0.42f) 0.82f else 0.18f
                }
                else -> 0.5f
            }
            ElevatedCard(
                modifier = Modifier
                    .widthIn(max = cardMax)
                    .align(
                        when {
                            currentStep == AppTourStep.Intro -> Alignment.Center
                            verticalBias > 0.5f -> Alignment.BottomCenter
                            else -> Alignment.TopCenter
                        },
                    )
                    .padding(vertical = if (currentStep == AppTourStep.Intro) 0.dp else FashTheme.spacing.spacing5),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(FashTheme.spacing.spacing4),
                    verticalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing3),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.app_tour_chip),
                            style = MaterialTheme.typography.labelMedium,
                            color = FashColors.Primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(onClick = onSkip) {
                            Text(stringResource(R.string.app_tour_skip))
                        }
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = FashColors.Primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    )
                    val mascotRes = if (currentStep == AppTourStep.Intro) {
                        R.drawable.fash_mascot_point_up
                    } else {
                        R.drawable.fash_mascot_point_left
                    }
                    FashMascotGuideImage(resId = mascotRes, sizeDp = 56)
                    Text(
                        text = stepTitle(currentStep),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stepBody(currentStep),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                    )
                    Spacer(Modifier.height(FashTheme.spacing.spacing2))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing3),
                    ) {
                        if (currentStep != AppTourStep.Intro) {
                            TextButton(
                                onClick = {
                                    val prev = AppTourStep.entries.getOrNull(currentStep.ordinal - 1)
                                    if (prev != null) onStepChange(prev)
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.app_tour_back))
                            }
                        }
                        TextButton(
                            onClick = {
                                val next = AppTourStep.entries.getOrNull(currentStep.ordinal + 1)
                                if (next == null) onFinish() else onStepChange(next)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                if (currentStep == AppTourStep.entries.last()) {
                                    stringResource(R.string.app_tour_done)
                                } else {
                                    stringResource(R.string.app_tour_next)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun stepTitle(step: AppTourStep): String = stringResource(
    when (step) {
        AppTourStep.Intro -> R.string.app_tour_intro_title
        AppTourStep.NavHome -> R.string.app_tour_nav_home_title
        AppTourStep.NavOrders -> R.string.app_tour_nav_orders_title
        AppTourStep.NavPost -> R.string.app_tour_nav_post_title
        AppTourStep.NavChat -> R.string.app_tour_nav_chat_title
        AppTourStep.NavProfile -> R.string.app_tour_nav_profile_title
        AppTourStep.TopBarActions -> R.string.app_tour_top_bar_title
    },
)

@Composable
private fun stepBody(step: AppTourStep): String = stringResource(
    when (step) {
        AppTourStep.Intro -> R.string.app_tour_intro_body
        AppTourStep.NavHome -> R.string.app_tour_nav_home_body
        AppTourStep.NavOrders -> R.string.app_tour_nav_orders_body
        AppTourStep.NavPost -> R.string.app_tour_nav_post_body
        AppTourStep.NavChat -> R.string.app_tour_nav_chat_body
        AppTourStep.NavProfile -> R.string.app_tour_nav_profile_body
        AppTourStep.TopBarActions -> R.string.app_tour_top_bar_body
    },
)
