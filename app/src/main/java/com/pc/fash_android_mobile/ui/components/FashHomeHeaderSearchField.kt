package com.pc.fash_android_mobile.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.lerp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.FashColors

/**
 * Home header faux search field — width do parent điều khiển (expand/collapse).
 * Tap mở Explore search.
 */
@Composable
fun FashHomeHeaderSearchField(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp,
    animateHint: Boolean = true,
    contentReveal: Float = 1f,
    placeholder: String = stringResource(R.string.home_header_search_placeholder),
) {
    if (width <= 0.dp) return

    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)
    val searchLabel = stringResource(R.string.search_label)
    val reveal = contentReveal.coerceIn(0f, 1f)
    val showPlaceholder = reveal > 0.32f

    val transition = rememberInfiniteTransition(label = "homeSearchHint")
    val borderPulse by transition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.52f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "borderPulse",
    )
    val shimmer by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 5600
                -0.35f at 0
                -0.35f at 3800
                1.35f at 5200 using FastOutSlowInEasing
                1.35f at 5600
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer",
    )

    val borderAlpha = if (animateHint) borderPulse else 0.32f
    val shimmerActive = animateHint && reveal > 0.85f

    Surface(
        onClick = onClick,
        modifier = modifier
            .width(width)
            .height(40.dp)
            .semantics { contentDescription = searchLabel },
        shape = shape,
        color = scheme.surfaceVariant.copy(alpha = 0.38f + 0.12f * reveal),
        shadowElevation = if (animateHint && reveal > 0.9f) 1.dp else 0.dp,
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = FashColors.Primary.copy(alpha = borderAlpha * reveal.coerceAtLeast(0.35f)),
                    shape = shape,
                )
                .clip(shape)
                .then(
                    if (shimmerActive) {
                        Modifier.drawWithContent {
                            drawContent()
                            val w = size.width
                            val x = w * shimmer
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        FashColors.Primary.copy(alpha = 0.07f),
                                        Color.Transparent,
                                    ),
                                    start = Offset(x - w * 0.25f, 0f),
                                    end = Offset(x + w * 0.25f, size.height),
                                ),
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (showPlaceholder) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = FashColors.Primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = placeholder,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(start = 8.dp)
                            .graphicsLayer { alpha = ((reveal - 0.32f) / 0.68f).coerceIn(0f, 1f) },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = scheme.onSurfaceVariant.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = FashColors.Primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Chu kỳ thu/giãn search Home header (~11s). */
internal const val HomeSearchExpandCycleMs = 11_000

/** Progress 0 = thu (icon), 1 = giãn (full field). Nghỉ lâu ở thu; pulse icon trước khi giãn. */
@Composable
fun rememberHomeSearchExpandProgress(animate: Boolean): Float {
    if (!animate) return 0f

    val transition = rememberInfiniteTransition(label = "homeSearchExpand")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = HomeSearchExpandCycleMs
                0f at 0
                0f at 3_600 using FastOutSlowInEasing
                1f at 4_400 using FastOutSlowInEasing
                1f at 8_000 using FastOutSlowInEasing
                0f at 8_700 using FastOutSlowInEasing
                0f at HomeSearchExpandCycleMs
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "expandProgress",
    ).value
}

/**
 * Icon search thu gọn — một nhịp pulse ngắn (~400ms) ngay trước khi field giãn ra.
 */
@Composable
fun FashHomeCollapsedSearchIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    animateHint: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val attention = if (animateHint) {
        val transition = rememberInfiniteTransition(label = "homeSearchIconPulse")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = HomeSearchExpandCycleMs
                    0f at 0
                    0f at 3_200
                    1f at 3_550 using FastOutSlowInEasing
                    0f at 3_950 using FastOutSlowInEasing
                    0f at HomeSearchExpandCycleMs
                },
                repeatMode = RepeatMode.Restart,
            ),
            label = "iconAttention",
        ).value
    } else {
        0f
    }

    IconButton(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            val s = 1f + attention * 0.16f
            scaleX = s
            scaleY = s
        },
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = stringResource(R.string.search_label),
            tint = lerp(
                scheme.onSurface,
                FashColors.Primary,
                attention * 0.9f,
            ),
            modifier = Modifier.size(24.dp),
        )
    }
}
