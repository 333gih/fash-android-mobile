package com.pc.fash_android_mobile.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashShapes

/**
 * App-wide snackbar aligned with Fash surfaces: card radius, soft shadow, primary actions on inverse bar.
 *
 * Uses Material [SnackbarHost] so **only one snackbar is visible at a time** and the queue advances
 * without enter/exit animations stacking on top of each other (fixes overlapping toasts when many
 * events fire in a row).
 *
 * Always applies [navigationBarsPadding] and [imePadding]. Pass [additionalBottomInset] for main bottom
 * nav or chat composer.
 */
@Composable
fun FashSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    /** Space above system bars / IME for fixed bottom UI (e.g. main bottom nav, chat composer). */
    additionalBottomInset: Dp = 0.dp,
) {
    val scheme = MaterialTheme.colorScheme
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
            .padding(bottom = additionalBottomInset)
            .navigationBarsPadding()
            .imePadding(),
        snackbar = { data ->
            Snackbar(
                snackbarData = data,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 10.dp,
                        shape = FashShapes.large,
                        ambientColor = Color.Black.copy(alpha = 0.12f),
                        spotColor = Color.Black.copy(alpha = 0.18f),
                    ),
                shape = FashShapes.large,
                containerColor = scheme.inverseSurface,
                contentColor = scheme.inverseOnSurface,
                actionContentColor = FashColors.Primary,
                dismissActionContentColor = scheme.inverseOnSurface.copy(alpha = 0.88f),
            )
        },
    )
}
