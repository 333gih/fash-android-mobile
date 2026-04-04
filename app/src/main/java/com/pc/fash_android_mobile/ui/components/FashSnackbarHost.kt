package com.pc.fash_android_mobile.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pc.fash_android_mobile.ui.theme.FashShapes

/**
 * App-wide snackbar: [FashShapes.large] corners and Material 3 inverse surface (readable on any screen).
 * Use for transient feedback (feed actions, login, checkout) so copy matches [MaterialTheme] typography.
 */
@Composable
fun FashSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { data: SnackbarData ->
            val scheme = MaterialTheme.colorScheme
            Snackbar(
                snackbarData = data,
                shape = FashShapes.large,
                containerColor = scheme.inverseSurface,
                contentColor = scheme.inverseOnSurface,
                actionContentColor = scheme.primary,
                dismissActionContentColor = scheme.inverseOnSurface,
            )
        },
    )
}
