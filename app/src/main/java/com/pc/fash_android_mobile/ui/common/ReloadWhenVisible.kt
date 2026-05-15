package com.pc.fash_android_mobile.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Invokes [onReload] whenever [visible] becomes true or any [keys] change while visible.
 * Use for overlay screens that stay composed under another route (e.g. orders list under order detail).
 */
@Composable
fun ReloadWhenVisible(
    visible: Boolean,
    vararg keys: Any?,
    onReload: () -> Unit,
) {
    LaunchedEffect(visible, *keys) {
        if (visible) onReload()
    }
}
