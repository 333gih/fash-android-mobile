package com.pc.fash_android_mobile.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

/**
 * Pill selector aligned with Explore filter chips: soft fill, no outline stroke,
 * editorial primary when selected ([FashColors.Primary] + onPrimary).
 */
@Composable
fun PostSelectablePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) scheme.onPrimary else scheme.onSurface,
        modifier = modifier
            .clip(RoundedCornerShape(FashTheme.spacing.radiusPill))
            .background(if (selected) FashColors.Primary else scheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
    )
}

/**
 * Full-width row for searchable lists (brand, country, category search): card radius, same fill logic as pills.
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FashTheme.spacing.radiusCard))
            .background(if (selected) FashColors.Primary else scheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        val labelColor = if (selected) scheme.onPrimary else scheme.onSurface
        if (!leadingEmoji.isNullOrBlank()) {
            Text(
                text = leadingEmoji,
                style = MaterialTheme.typography.titleMedium,
                color = labelColor,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = labelColor,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) scheme.onPrimary.copy(alpha = 0.88f) else scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
fun PostListingSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = label,
        singleLine = true,
        shape = shape,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = scheme.outlineVariant.copy(alpha = 0.45f),
            focusedBorderColor = scheme.primary.copy(alpha = 0.7f),
            cursorColor = scheme.primary,
            focusedLabelColor = scheme.primary,
        ),
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
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(FashTheme.spacing.radiusSoftMin)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        shape = shape,
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = scheme.outlineVariant.copy(alpha = 0.45f),
            focusedBorderColor = scheme.primary.copy(alpha = 0.7f),
            cursorColor = scheme.primary,
            focusedLabelColor = scheme.primary,
        ),
    )
}
