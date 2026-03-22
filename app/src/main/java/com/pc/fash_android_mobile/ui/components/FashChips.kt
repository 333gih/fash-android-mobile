package com.pc.fash_android_mobile.ui.components

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pc.fash_android_mobile.ui.theme.FashTheme

/**
 * Pill chip (20dp corners) — unselected [surfaceVariant], selected [primary] + [onPrimary].
 */
@Composable
fun FashPillFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = FashTheme.spacing.chipShape()
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, style = MaterialTheme.typography.labelLarge) },
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = scheme.surfaceVariant,
            labelColor = scheme.onSurfaceVariant,
            selectedContainerColor = scheme.primary,
            selectedLabelColor = scheme.onPrimary,
            disabledContainerColor = scheme.surfaceVariant.copy(alpha = 0.38f),
            disabledLabelColor = scheme.onSurfaceVariant.copy(alpha = 0.38f),
        ),
        border = null,
    )
}
