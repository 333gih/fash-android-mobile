package com.pc.fash_android_mobile.ui.orders

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

/** Sliders with top below this fraction of root height are bottom sticky docks — not used for anchor placement. */
private const val AnchorSliderMaxTopFraction = 0.5f

/**
 * Root [LayoutCoordinates] for the main content [androidx.compose.foundation.layout.Box] that wraps
 * [com.pc.fash_android_mobile.ui.main.MainNavScreen] (sibling to the pending-payment overlay).
 */
val LocalPendingPaymentRootCoordinates = compositionLocalOf<LayoutCoordinates?> { null }

/**
 * Registry of on-screen promo slider bounds (top/bottom in root coordinates). Used to place the
 * pending-payment banner above the topmost visible slider when one exists.
 */
class PendingPaymentSliderRegistry {
    internal val sliders = mutableStateMapOf<Any, SliderBoundsInRoot>()

    fun report(key: Any, top: Float, bottom: Float) {
        sliders[key] = SliderBoundsInRoot(top = top, bottom = bottom)
    }

    fun remove(key: Any) {
        sliders.remove(key)
    }

    /**
     * Top Y for the topmost **inline** promo slider (upper portion of the screen). Bottom sticky docks
     * are excluded so the banner uses bottom placement instead of floating mid-screen.
     */
    fun topmostAnchorEligibleSliderTop(rootHeightPx: Float): Float? {
        if (rootHeightPx <= 0f) return null
        val maxTop = rootHeightPx * AnchorSliderMaxTopFraction
        val visible = sliders.values.filter { b ->
            b.top < rootHeightPx && b.bottom > 0f && b.top < maxTop
        }
        if (visible.isEmpty()) return null
        return visible.minOf { it.top }
    }
}

data class SliderBoundsInRoot(val top: Float, val bottom: Float)

/** Default instance is only used outside [MainActivity] (e.g. previews); main app provides a shared registry. */
val LocalPendingPaymentSliderRegistry = compositionLocalOf { PendingPaymentSliderRegistry() }

/**
 * Registers this composable's bounds as a promo slider region for pending-payment banner placement.
 * No-ops when [LocalPendingPaymentRootCoordinates] is null (before first layout).
 */
fun Modifier.pendingPaymentSliderAnchor(enabled: Boolean = true): Modifier = composed {
    if (!enabled) return@composed this
    val root = LocalPendingPaymentRootCoordinates.current
    val registry = LocalPendingPaymentSliderRegistry.current
    val key = remember { Any() }
    DisposableEffect(key) {
        onDispose { registry.remove(key) }
    }
    this.then(
        Modifier.onGloballyPositioned { coords ->
            val r = root ?: return@onGloballyPositioned
            if (!coords.isAttached || !r.isAttached) return@onGloballyPositioned
            val c = coords.boundsInWindow()
            val rootWin = r.boundsInWindow()
            val top = c.top - rootWin.top
            val bottom = c.bottom - rootWin.top
            registry.report(key, top, bottom)
        },
    )
}
