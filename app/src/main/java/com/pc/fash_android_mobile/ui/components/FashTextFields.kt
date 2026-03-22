package com.pc.fash_android_mobile.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.pc.fash_android_mobile.R

/**
 * Soft filled field — minimal chrome, tinted container; floating-style label via Material3 [TextField].
 */
@Composable
fun FashFilledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = label,
        placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyLarge) } },
        singleLine = singleLine,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = scheme.surfaceVariant.copy(alpha = 0.45f),
            unfocusedContainerColor = scheme.surfaceVariant.copy(alpha = 0.3f),
            disabledContainerColor = scheme.surfaceVariant.copy(alpha = 0.2f),
            focusedIndicatorColor = scheme.primary,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = scheme.onSurface,
            unfocusedTextColor = scheme.onSurface,
            cursorColor = scheme.primary,
            focusedLabelColor = scheme.primary,
            unfocusedLabelColor = scheme.onSurfaceVariant,
        ),
    )
}

/**
 * Search-style prompt from the spec — label uses [androidx.compose.material3.Typography.labelSmall].
 */
@Composable
fun FashSearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FashFilledTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(stringResource(R.string.search_label), style = MaterialTheme.typography.labelSmall) },
        placeholder = stringResource(R.string.search_placeholder),
    )
}
