package com.pc.fash_android_mobile.ui.post

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

/**
 * Pill selector for post flow: tonal surface when unselected; [FashColors.Primary] fill when selected (no box frame).
 */
@Composable
fun PostSelectablePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(FashTheme.spacing.radiusPill)
    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (selected) FashColors.Primary else scheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) scheme.onPrimary else scheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/**
 * Full-width selectable row for searchable lists (brand, country, category search).
 * Matches checkout payment rows: tonal [Surface], primary border only when selected; leading avatar (emoji or initial),
 * and trailing radio indicator — consistent with the app design system.
 */
@Composable
fun PostSelectableListRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingEmoji: String? = null,
    subtitle: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(FashTheme.spacing.radiusCard)
    val chipFill =
        if (selected) FashColors.Primary.copy(alpha = 0.12f) else scheme.surfaceContainerLow

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = chipFill,
        border = if (selected) {
            BorderStroke(1.5.dp, FashColors.Primary.copy(alpha = 0.85f))
        } else {
            null
        },
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PostSelectableListLeading(
                text = text,
                leadingEmoji = leadingEmoji,
                selected = selected,
                chipFill = chipFill,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurface,
                )
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
                imageVector = if (selected) Icons.Default.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) FashColors.Primary else scheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun PostSelectableListLeading(
    text: String,
    leadingEmoji: String?,
    selected: Boolean,
    chipFill: Color,
) {
    val scheme = MaterialTheme.colorScheme
    val initial = listLeadingInitial(text)
    Surface(
        shape = CircleShape,
        color = chipFill,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!leadingEmoji.isNullOrBlank()) {
                Text(
                    text = leadingEmoji,
                    style = MaterialTheme.typography.titleMedium,
                )
            } else {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (selected) FashColors.Primary else scheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun listLeadingInitial(text: String): String {
    val t = text.trim()
    if (t.isEmpty()) return "?"
    val c = t.first()
    return if (c.isLetterOrDigit()) c.uppercaseChar().toString() else t.take(1)
}

@Composable
fun PostListingSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = label,
        singleLine = true,
        enabled = enabled,
        shape = shape,
        colors = postListingOutlinedFieldColors(),
    )
}

@Composable
fun PostListingOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 12,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    suffix: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        enabled = enabled,
        shape = shape,
        keyboardOptions = keyboardOptions,
        suffix = suffix,
        colors = postListingOutlinedFieldColors(),
    )
}
