package com.pc.fash_android_mobile.ui.post

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.pc.fash_android_mobile.ui.theme.FashColors

/**
 * Post listing flow — surfaces follow [MaterialTheme.colorScheme] (editorial vs pure white light).
 */
object PostListingColors {
    @Composable
    fun fieldSurface(): Color {
        val scheme = MaterialTheme.colorScheme
        return if (isSystemInDarkTheme()) FashColors.SurfaceContainerHighestDark else scheme.surfaceContainerHighest
    }

    /** Full-bleed background for each post step. */
    @Composable
    fun stepCanvas(): Color {
        val scheme = MaterialTheme.colorScheme
        return if (isSystemInDarkTheme()) FashColors.SurfaceLowestDark else scheme.background
    }
}

@Composable
fun postListingOutlinedFieldColors() = outlinedTextFieldColorsFor(PostListingColors.fieldSurface())

@Composable
private fun outlinedTextFieldColorsFor(container: Color) =
    OutlinedTextFieldDefaults.colors(
        focusedContainerColor = container,
        unfocusedContainerColor = container,
        disabledContainerColor = container.copy(alpha = 0.72f),
        errorContainerColor = container,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        errorTextColor = MaterialTheme.colorScheme.error,
        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.78f),
        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        errorBorderColor = MaterialTheme.colorScheme.error,
        cursorColor = MaterialTheme.colorScheme.primary,
        errorCursorColor = MaterialTheme.colorScheme.error,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
        errorLabelColor = MaterialTheme.colorScheme.error,
    )
