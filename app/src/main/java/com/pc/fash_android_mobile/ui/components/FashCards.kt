package com.pc.fash_android_mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.ui.theme.FashTheme
import com.pc.fash_android_mobile.ui.theme.ghostBorderColor

/** Portrait editorial ratio for product imagery */
const val FashEditorialImageAspect = 3f / 4f

/**
 * Feed / product card — tonal surface, no harsh dividers; optional ghost border on white backdrops.
 */
@Composable
fun FashEditorialCard(
    modifier: Modifier = Modifier,
    onGhostBorder: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(FashTheme.spacing.radiusCard)
    val borderMod = if (onGhostBorder) {
        Modifier.border(1.dp, ghostBorderColor(scheme.outlineVariant), shape)
    } else {
        Modifier
    }
    Card(
        modifier = modifier.then(borderMod),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(FashTheme.spacing.spacing4),
            verticalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing2),
            content = content,
        )
    }
}

/**
 * 3:4 frame for full-width product art — place [androidx.compose.foundation.Image], Coil, etc. inside [content].
 */
@Composable
fun FashEditorialImageFrame(
    modifier: Modifier = Modifier,
    placeholderColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(FashTheme.spacing.radiusCard)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(FashEditorialImageAspect)
            .clip(shape)
            .background(placeholderColor),
    ) {
        Box(Modifier.fillMaxSize()) {
            content()
        }
    }
}

/**
 * Same geometry as [FashEditorialImageFrame], for bitmaps / drawables with [ContentScale.Crop].
 */
@Composable
fun FashEditorialImageContainer(
    modifier: Modifier = Modifier,
    placeholderColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable () -> Unit,
) = FashEditorialImageFrame(modifier, placeholderColor, content)

/**
 * Product line: title + price in editorial type scale.
 */
@Composable
fun FashProductTextBlock(
    title: String,
    priceLabel: String,
    description: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(FashTheme.spacing.spacing2)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = priceLabel,
            style = FashTheme.textStyles.price,
            color = MaterialTheme.colorScheme.primary,
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
