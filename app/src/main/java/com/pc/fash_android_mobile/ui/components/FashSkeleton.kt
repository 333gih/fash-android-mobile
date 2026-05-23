package com.pc.fash_android_mobile.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.ui.theme.FashTheme

/**
 * Shimmer placeholder block. Replaces CircularProgressIndicator everywhere the layout shape is
 * predictable (cards, rails, grid). Animation is 1.4s linear sweep — fast enough to feel alive,
 * slow enough not to compete with content motion.
 *
 * Use [FashSkeletonBox] for arbitrary rectangular regions and the higher-level
 * [FashSkeletonRail] / [FashSkeletonGrid] for the standard Home/Explore loading states.
 */
@Composable
fun FashSkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val highlightColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)

    val transition = rememberInfiniteTransition(label = "fashSkeletonShimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fashSkeletonTranslate",
    )

    val sweepWidth = 800f
    val xOffset = (translate * 2f - 1f) * sweepWidth

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(xOffset, 0f),
        end = Offset(xOffset + sweepWidth, 0f),
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush),
    )
}

/**
 * Listing card placeholder — image (4:5) + 2 lines of text + price. Matches the real
 * ListingGridCard geometry so swap-in is invisible.
 */
@Composable
fun FashSkeletonListingCard(
    modifier: Modifier = Modifier,
    imageAspectRatio: Float = 4f / 5f,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FashSkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(imageAspectRatio),
            shape = RoundedCornerShape(FashTheme.spacing.radiusCard),
        )
        FashSkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(12.dp),
        )
        FashSkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(12.dp),
        )
        FashSkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.35f)
                .height(14.dp),
        )
    }
}

/**
 * Horizontal rail of listing card skeletons — used while a discovery rail is loading.
 */
@Composable
fun FashSkeletonRail(
    modifier: Modifier = Modifier,
    cardWidth: Dp = 156.dp,
    cardCount: Int = 5,
    showHeader: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = FashTheme.spacing.spacing2),
    ) {
        if (showHeader) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = FashTheme.spacing.editorialStart,
                        end = FashTheme.spacing.editorialEnd,
                        bottom = FashTheme.spacing.spacing3,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FashSkeletonBox(
                        modifier = Modifier
                            .width(160.dp)
                            .height(18.dp),
                    )
                    FashSkeletonBox(
                        modifier = Modifier
                            .width(220.dp)
                            .height(12.dp),
                    )
                }
                FashSkeletonBox(
                    modifier = Modifier
                        .width(56.dp)
                        .height(20.dp),
                    shape = RoundedCornerShape(FashTheme.spacing.radiusPill),
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(
                start = FashTheme.spacing.editorialStart,
                end = FashTheme.spacing.editorialEnd,
            ),
            horizontalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing3),
        ) {
            items(count = cardCount) {
                FashSkeletonListingCard(
                    modifier = Modifier.width(cardWidth),
                )
            }
        }
    }
}

/**
 * 2-column grid skeleton for follow feed (Home) and main Explore grid.
 */
@Composable
fun FashSkeletonGrid(
    modifier: Modifier = Modifier,
    rows: Int = 4,
    imageAspectRatio: Float = 4f / 5f,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = FashTheme.spacing.editorialStart,
                end = FashTheme.spacing.editorialEnd,
            ),
        verticalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing3),
    ) {
        repeat(rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing2),
            ) {
                FashSkeletonListingCard(
                    modifier = Modifier.weight(1f),
                    imageAspectRatio = imageAspectRatio,
                )
                FashSkeletonListingCard(
                    modifier = Modifier.weight(1f),
                    imageAspectRatio = imageAspectRatio,
                )
            }
        }
    }
}

/**
 * Seller story row skeleton.
 */
@Composable
fun FashSkeletonSellerStrip(
    modifier: Modifier = Modifier,
    avatarSize: Dp = 60.dp,
    cellCount: Int = 6,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = FashTheme.spacing.spacing2),
        contentPadding = PaddingValues(
            start = FashTheme.spacing.editorialStart,
            end = FashTheme.spacing.editorialEnd,
        ),
        horizontalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing3),
    ) {
        items(count = cellCount) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FashSkeletonBox(
                    modifier = Modifier.size(avatarSize),
                    shape = CircleShape,
                )
                FashSkeletonBox(
                    modifier = Modifier
                        .width(avatarSize - 8.dp)
                        .height(10.dp),
                )
            }
        }
    }
}
