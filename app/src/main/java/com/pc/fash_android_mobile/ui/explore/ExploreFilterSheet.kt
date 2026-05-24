package com.pc.fash_android_mobile.ui.explore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme
import kotlinx.coroutines.delay

private val exploreFilterTeaserChipResIds = listOf(
    R.string.explore_filter_teaser_sizing,
    R.string.explore_filter_teaser_area,
    R.string.explore_filter_teaser_category,
    R.string.explore_filter_teaser_style,
    R.string.explore_filter_teaser_brand,
    R.string.explore_filter_teaser_price,
)

private val exploreFilterTeaserLineResIds = listOf(
    R.string.explore_filter_teaser_line_sizing,
    R.string.explore_filter_teaser_line_area,
    R.string.explore_filter_teaser_line_style,
    R.string.explore_filter_teaser_line_price,
    R.string.explore_filter_teaser_line_category,
)

private val exploreFilterHintResIds = listOf(
    R.string.explore_filter_hint_sizing,
    R.string.explore_filter_hint_area,
    R.string.explore_filter_hint_category,
    R.string.explore_filter_hint_style,
    R.string.explore_filter_hint_price,
)

private fun Modifier.exploreFilterMarquee(): Modifier = basicMarquee(
    initialDelayMillis = 600,
    repeatDelayMillis = 800,
    velocity = 32.dp,
)

/** Subtle pulse on the filter icon when no filters are active — draws the eye without being loud. */
@Composable
internal fun ExploreFilterIconPulse(
    active: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (active) {
        Box(modifier = modifier) { content() }
        return
    }
    val transition = rememberInfiniteTransition(label = "filter_icon_pulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_100),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "filter_icon_scale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_100),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "filter_icon_alpha",
    )
    Box(
        modifier = modifier
            .scale(scale)
            .graphicsLayer { this.alpha = alpha },
    ) {
        content()
    }
}

/** Horizontally scrolling filter-type chips — “running text” teaser. */
@Composable
internal fun ExploreFilterTeaserMarquee(
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val chips = exploreFilterTeaserChipResIds.map { stringResource(it) }
    val separator = stringResource(R.string.explore_filter_teaser_separator)
    val marqueeText = chips.joinToString(separator)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FashTheme.spacing.radiusPill))
            .background(FashColors.Primary.copy(alpha = 0.07f))
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = marqueeText,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = FashColors.Primary,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .exploreFilterMarquee()
                .padding(horizontal = 10.dp),
        )
    }
}

/** Vertical flip between short CTAs — encourages opening the filter sheet. */
@Composable
internal fun ExploreFilterTeaserRotatingLine(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    var lineIndex by remember { mutableIntStateOf(0) }
    val lines = exploreFilterTeaserLineResIds.map { stringResource(it) }

    LaunchedEffect(lines.size) {
        if (lines.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(if (compact) 2_800 else 3_200)
            lineIndex = (lineIndex + 1) % lines.size
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 20.dp else 24.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        AnimatedContent(
            targetState = lineIndex,
            transitionSpec = {
                (slideInVertically { fullHeight -> fullHeight } + fadeIn(tween(340)))
                    .togetherWith(slideOutVertically { fullHeight -> -fullHeight } + fadeOut(tween(260)))
            },
            label = "explore_filter_teaser_line",
        ) { index ->
            Text(
                text = lines.getOrElse(index) { "" },
                style = if (compact) {
                    MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                } else {
                    MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                },
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Idle-state teaser on the Explore filter bar: rotating CTA + scrolling filter-type chips.
 * Shown when the user has not applied filters yet.
 */
@Composable
internal fun ExploreFilterIdleTeaser(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
    ) {
        ExploreFilterTeaserRotatingLine(
            compact = compact,
            modifier = Modifier.padding(top = if (compact) 0.dp else 2.dp),
        )
        if (!compact) {
            ExploreFilterTeaserMarquee()
        }
        Text(
            text = stringResource(R.string.explore_filter_teaser_tap),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun ExploreFilterHintCarousel(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val edge = FashTheme.spacing.editorialStart
    var hintIndex by remember { mutableIntStateOf(0) }
    val hints = exploreFilterHintResIds.map { stringResource(it) }

    LaunchedEffect(hints.size) {
        if (hints.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(3_800)
            hintIndex = (hintIndex + 1) % hints.size
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = edge, vertical = 8.dp),
        shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin),
        color = FashColors.Primary.copy(alpha = 0.08f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ExploreFilterTeaserMarquee(
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.explore_filter_hint_label),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = FashColors.Primary,
                    modifier = Modifier.padding(end = 10.dp),
                )
                AnimatedContent(
                    targetState = hintIndex,
                    transitionSpec = {
                        (slideInVertically { it } + fadeIn(tween(320)))
                            .togetherWith(slideOutVertically { -it } + fadeOut(tween(240)))
                    },
                    label = "explore_filter_hint",
                    modifier = Modifier.weight(1f),
                ) { index ->
                    Text(
                        text = hints.getOrElse(index) { "" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ExploreFilterExpandableGroup(
    title: String,
    subtitle: String? = null,
    expandedInitially: Boolean = true,
    activeCount: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val edge = FashTheme.spacing.editorialStart
    var expanded by rememberSaveable(title) { mutableStateOf(expandedInitially) }
    val shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = edge, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .clickable { expanded = !expanded }
                .background(scheme.surfaceContainerLow)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                    )
                    if (activeCount > 0) {
                        Text(
                            text = activeCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onPrimary,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(FashColors.Primary)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                content()
            }
        }
    }
}
