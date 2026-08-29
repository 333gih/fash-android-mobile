package com.pc.fash_android_mobile.ui.onboarding

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CapsuleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.components.FashMascotGuideImage
import com.pc.fash_android_mobile.ui.theme.FashColors
import kotlinx.coroutines.delay

private enum class MascotPointDirection(val resId: Int) {
    Up(R.drawable.fash_mascot_point_up),
    Down(R.drawable.fash_mascot_point_down),
    Left(R.drawable.fash_mascot_point_left),
}

private data class MascotCoachPlacement(
    val mascotCenter: Offset,
    val direction: MascotPointDirection,
    val flipHorizontal: Boolean,
    val captionCenter: Offset,
)

@Composable
fun MascotSpotlightOverlay(
    title: String,
    bodyText: String,
    anchor: FeatureTourAnchor?,
    anchors: Map<FeatureTourAnchor, LayoutCoordinates>,
    stepIndex: Int,
    stepCount: Int,
    showBack: Boolean,
    isLast: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var overlayCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val density = LocalDensity.current
    val holePadding = 10.dp
    val corner = 18.dp
    val scrim = MaterialTheme.colorScheme.scrim.copy(alpha = 0.62f)
    val mascotSizePx = with(density) { 72.dp.toPx() }
    val mascotGapPx = with(density) { 14.dp.toPx() }

    val holeRect = remember(anchor, anchors, overlayCoords, density) {
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

    var holeReady by remember(anchor) { mutableStateOf(anchor == null) }
    LaunchedEffect(anchor) {
        holeReady = anchor == null
        if (anchor != null) {
            delay(48)
            holeReady = true
        }
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

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val maxWpx = with(density) { maxWidth.toPx() }
            val maxHpx = with(density) { maxHeight.toPx() }
            val placement = hr?.let {
                coachPlacement(
                    hole = it,
                    overlayWidth = maxWpx,
                    overlayHeight = maxHpx,
                    mascotSize = mascotSizePx,
                    mascotGap = mascotGapPx,
                )
            }

            if (placement != null) {
                val mascotOffsetX = with(density) { (placement.mascotCenter.x - mascotSizePx / 2f).toDp() }
                val mascotOffsetY = with(density) { (placement.mascotCenter.y - mascotSizePx / 2f).toDp() }
                val captionOffsetX = with(density) { (placement.captionCenter.x).toDp() }
                val captionOffsetY = with(density) { (placement.captionCenter.y).toDp() }
                FashMascotGuideImage(
                    resId = placement.direction.resId,
                    sizeDp = 72,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = mascotOffsetX, y = mascotOffsetY)
                        .scale(scaleX = if (placement.flipHorizontal) -1f else 1f, scaleY = 1f),
                )
                CaptionBubble(
                    title = title,
                    bodyText = bodyText,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = (captionOffsetX - minOf(maxWidth, 300.dp) / 2).coerceAtLeast(8.dp),
                            y = (captionOffsetY - 48.dp).coerceAtLeast(72.dp),
                        )
                        .widthIn(max = minOf(maxWidth - 40.dp, 300.dp)),
                )
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = maxHeight * 0.28f)
                        .widthIn(max = minOf(maxWidth - 48.dp, 320.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    FashMascotGuideImage(resId = MascotPointDirection.Up.resId, sizeDp = 88)
                    CaptionBubble(title = title, bodyText = bodyText)
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            androidx.compose.ui.graphics.Color.Transparent,
                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f),
                        ),
                    ),
                )
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onSkip) {
                    Text(
                        text = stringResource(R.string.app_tour_skip),
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.88f),
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(stepCount.coerceAtLeast(1)) { page ->
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (page == stepIndex) 16.dp else 6.dp)
                                .clip(CapsuleShape)
                                .background(
                                    if (page == stepIndex) {
                                        FashColors.Primary
                                    } else {
                                        androidx.compose.ui.graphics.Color.White.copy(alpha = 0.35f)
                                    },
                                ),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showBack) {
                        TextButton(onClick = onBack) {
                            Text(
                                text = stringResource(R.string.app_tour_back),
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.92f),
                            )
                        }
                    }
                    TextButton(
                        onClick = onNext,
                        modifier = Modifier
                            .clip(CapsuleShape)
                            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.96f)),
                    ) {
                        Text(
                            text = stringResource(
                                if (isLast) R.string.app_tour_done else R.string.app_tour_next,
                            ),
                            color = FashColors.Primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptionBubble(
    title: String,
    bodyText: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = bodyText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun coachPlacement(
    hole: Rect,
    overlayWidth: Float,
    overlayHeight: Float,
    mascotSize: Float,
    mascotGap: Float,
): MascotCoachPlacement {
    val spaceAbove = hole.top
    val spaceBelow = overlayHeight - hole.bottom
    val spaceLeft = hole.left
    val spaceRight = overlayWidth - hole.right
    val captionHeight = 96f
    val halfMascot = mascotSize / 2f

    if (spaceBelow >= mascotSize + mascotGap + 40f && spaceBelow >= spaceAbove) {
        val mascotY = hole.bottom + mascotGap + halfMascot
        val captionY = minOf(mascotY + halfMascot + captionHeight / 2f + 8f, overlayHeight - 120f)
        return MascotCoachPlacement(
            mascotCenter = Offset(hole.center.x, mascotY),
            direction = MascotPointDirection.Up,
            flipHorizontal = false,
            captionCenter = Offset(hole.center.x, captionY),
        )
    }

    if (spaceAbove >= mascotSize + mascotGap + 40f) {
        val mascotY = hole.top - mascotGap - halfMascot
        val captionY = maxOf(mascotY - halfMascot - captionHeight / 2f - 8f, 80f)
        return MascotCoachPlacement(
            mascotCenter = Offset(hole.center.x, mascotY),
            direction = MascotPointDirection.Down,
            flipHorizontal = false,
            captionCenter = Offset(hole.center.x, captionY),
        )
    }

    if (spaceRight >= mascotSize + mascotGap + 40f && spaceRight >= spaceLeft) {
        val mascotX = hole.right + mascotGap + halfMascot
        return MascotCoachPlacement(
            mascotCenter = Offset(mascotX, hole.center.y),
            direction = MascotPointDirection.Left,
            flipHorizontal = true,
            captionCenter = Offset(minOf(mascotX + halfMascot + 130f, overlayWidth - 20f), hole.center.y),
        )
    }

    val mascotX = hole.left - mascotGap - halfMascot
    return MascotCoachPlacement(
        mascotCenter = Offset(maxOf(halfMascot + 12f, mascotX), hole.center.y),
        direction = MascotPointDirection.Left,
        flipHorizontal = false,
        captionCenter = Offset(maxOf(130f, mascotX - halfMascot - 10f), hole.center.y),
    )
}

fun Modifier.guideSpotlightAnchor(
    anchor: FeatureTourAnchor,
    enabled: Boolean,
    onPositioned: (FeatureTourAnchor, LayoutCoordinates?) -> Unit,
): Modifier {
    if (!enabled) return this
    return onGloballyPositioned { coords ->
        onPositioned(anchor, coords.takeIf { it.isAttached })
    }
}
