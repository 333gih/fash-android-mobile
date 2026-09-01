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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.components.FashMascotGuideImage
import com.pc.fash_android_mobile.ui.theme.FashColors
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

private val PillShape = RoundedCornerShape(50)

private enum class MascotPointDirection(val resId: Int) {
    Up(R.drawable.fash_mascot_point_up),
    Down(R.drawable.fash_mascot_point_down),
    Left(R.drawable.fash_mascot_point_left),
}

private enum class CoachStackLayout {
    VerticalMascotFirst,
    VerticalCaptionFirst,
    HorizontalMascotFirst,
}

private data class MascotCoachPlacement(
    val stackCenter: Offset,
    val stackHalfWidth: Float,
    val stackHalfHeight: Float,
    val direction: MascotPointDirection,
    val flipHorizontal: Boolean,
    val layout: CoachStackLayout,
    val captionMaxWidth: Dp,
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
    val mascotSizeDp = 72.dp
    val mascotSizePx = with(density) { mascotSizeDp.toPx() }
    val mascotGapPx = with(density) { 14.dp.toPx() }
    val controlsReservePx = with(density) { 132.dp.toPx() }
    val topSafePx = with(density) { 56.dp.toPx() }
    val stackSpacingPx = with(density) { 8.dp.toPx() }
    val captionHeightEstimatePx = with(density) { 104.dp.toPx() }

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
            val captionMaxWidthDp = minOf(maxWidth - 40.dp, 300.dp)
            val captionMaxWidthPx = with(density) { captionMaxWidthDp.toPx() }
            val verticalStackHeightPx = mascotSizePx + stackSpacingPx + captionHeightEstimatePx
            val horizontalStackWidthPx = mascotSizePx + stackSpacingPx + captionMaxWidthPx

            val placement = hr?.let {
                coachPlacement(
                    hole = it,
                    overlayWidth = maxWpx,
                    overlayHeight = maxHpx,
                    mascotGap = mascotGapPx,
                    verticalStackHeight = verticalStackHeightPx,
                    horizontalStackWidth = horizontalStackWidthPx,
                    controlsReserve = controlsReservePx,
                    topSafe = topSafePx,
                    captionMaxWidthDp = captionMaxWidthDp,
                    captionMaxWidthPx = captionMaxWidthPx,
                    mascotSizePx = mascotSizePx,
                )
            }

            if (placement != null) {
                val stackOffsetX = with(density) {
                    (placement.stackCenter.x - placement.stackHalfWidth).toDp()
                }
                val stackOffsetY = with(density) {
                    (placement.stackCenter.y - placement.stackHalfHeight).toDp()
                }
                CoachStack(
                    direction = placement.direction,
                    flipHorizontal = placement.flipHorizontal,
                    layout = placement.layout,
                    title = title,
                    bodyText = bodyText,
                    mascotSizeDp = 72,
                    captionMaxWidth = placement.captionMaxWidth,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = stackOffsetX.coerceAtLeast(8.dp),
                            y = stackOffsetY.coerceAtLeast(with(density) { topSafePx.toDp() }),
                        ),
                )
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = maxHeight * 0.22f)
                        .widthIn(max = minOf(maxWidth - 48.dp, 320.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
                                .clip(PillShape)
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
                            .clip(PillShape)
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
private fun CoachStack(
    direction: MascotPointDirection,
    flipHorizontal: Boolean,
    layout: CoachStackLayout,
    title: String,
    bodyText: String,
    mascotSizeDp: Int,
    captionMaxWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val mascot = @Composable {
        FashMascotGuideImage(
            resId = direction.resId,
            sizeDp = mascotSizeDp,
            modifier = Modifier.scale(scaleX = if (flipHorizontal) -1f else 1f, scaleY = 1f),
        )
    }
    val caption = @Composable {
        CaptionBubble(
            title = title,
            bodyText = bodyText,
            modifier = Modifier.widthIn(max = captionMaxWidth),
        )
    }

    when (layout) {
        CoachStackLayout.VerticalMascotFirst -> {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                mascot()
                caption()
            }
        }
        CoachStackLayout.VerticalCaptionFirst -> {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                caption()
                mascot()
            }
        }
        CoachStackLayout.HorizontalMascotFirst -> {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                mascot()
                caption()
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
    mascotGap: Float,
    verticalStackHeight: Float,
    horizontalStackWidth: Float,
    controlsReserve: Float,
    topSafe: Float,
    captionMaxWidthDp: Dp,
    captionMaxWidthPx: Float,
    mascotSizePx: Float,
): MascotCoachPlacement {
    val contentBottom = overlayHeight - controlsReserve
    val spaceAbove = hole.top - topSafe
    val spaceBelow = contentBottom - hole.bottom
    val spaceLeft = hole.left
    val spaceRight = overlayWidth - hole.right
    val halfVertical = verticalStackHeight / 2f
    val halfHorizontal = horizontalStackWidth / 2f
    val verticalHalfWidth = max(captionMaxWidthPx / 2f, mascotSizePx / 2f)

    val canPlaceBelow = spaceBelow >= verticalStackHeight + mascotGap
    val canPlaceAbove = spaceAbove >= verticalStackHeight + mascotGap

    if (canPlaceBelow && spaceBelow >= spaceAbove) {
        val stackCenterY = min(hole.bottom + mascotGap + halfVertical, contentBottom - halfVertical)
        val stackCenterX = hole.center.x.coerceIn(verticalHalfWidth + 12f, overlayWidth - verticalHalfWidth - 12f)
        return MascotCoachPlacement(
            stackCenter = Offset(stackCenterX, stackCenterY),
            stackHalfWidth = verticalHalfWidth,
            stackHalfHeight = halfVertical,
            direction = MascotPointDirection.Up,
            flipHorizontal = false,
            layout = CoachStackLayout.VerticalMascotFirst,
            captionMaxWidth = captionMaxWidthDp,
        )
    }

    if (canPlaceAbove) {
        val stackCenterY = max(hole.top - mascotGap - halfVertical, topSafe + halfVertical)
        val stackCenterX = hole.center.x.coerceIn(verticalHalfWidth + 12f, overlayWidth - verticalHalfWidth - 12f)
        return MascotCoachPlacement(
            stackCenter = Offset(stackCenterX, stackCenterY),
            stackHalfWidth = verticalHalfWidth,
            stackHalfHeight = halfVertical,
            direction = MascotPointDirection.Down,
            flipHorizontal = false,
            layout = CoachStackLayout.VerticalCaptionFirst,
            captionMaxWidth = captionMaxWidthDp,
        )
    }

    val canPlaceRight = spaceRight >= horizontalStackWidth + mascotGap
    val canPlaceLeft = spaceLeft >= horizontalStackWidth + mascotGap

    if (canPlaceRight && spaceRight >= spaceLeft) {
        val stackCenterX = min(hole.right + mascotGap + halfHorizontal, overlayWidth - halfHorizontal - 12f)
        val stackCenterY = hole.center.y.coerceIn(
            halfVertical + topSafe,
            contentBottom - halfVertical,
        )
        return MascotCoachPlacement(
            stackCenter = Offset(stackCenterX, stackCenterY),
            stackHalfWidth = halfHorizontal,
            stackHalfHeight = mascotSizePx / 2f,
            direction = MascotPointDirection.Left,
            flipHorizontal = true,
            layout = CoachStackLayout.HorizontalMascotFirst,
            captionMaxWidth = captionMaxWidthDp,
        )
    }

    if (canPlaceLeft) {
        val stackCenterX = max(hole.left - mascotGap - halfHorizontal, halfHorizontal + 12f)
        val stackCenterY = hole.center.y.coerceIn(
            halfVertical + topSafe,
            contentBottom - halfVertical,
        )
        return MascotCoachPlacement(
            stackCenter = Offset(stackCenterX, stackCenterY),
            stackHalfWidth = halfHorizontal,
            stackHalfHeight = mascotSizePx / 2f,
            direction = MascotPointDirection.Left,
            flipHorizontal = false,
            layout = CoachStackLayout.HorizontalMascotFirst,
            captionMaxWidth = captionMaxWidthDp,
        )
    }

    // Fallback: center stack in safe area above controls
    val fallbackY = max(topSafe + halfVertical, contentBottom - halfVertical)
    val fallbackX = hole.center.x.coerceIn(verticalHalfWidth + 12f, overlayWidth - verticalHalfWidth - 12f)
    return MascotCoachPlacement(
        stackCenter = Offset(fallbackX, fallbackY),
        stackHalfWidth = verticalHalfWidth,
        stackHalfHeight = halfVertical,
        direction = MascotPointDirection.Up,
        flipHorizontal = false,
        layout = CoachStackLayout.VerticalMascotFirst,
        captionMaxWidth = captionMaxWidthDp,
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
