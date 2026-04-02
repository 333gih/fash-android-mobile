package com.pc.fash_android_mobile.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LocalFashSpacing = staticCompositionLocalOf { FashSpacing() }

private val LocalFashTextStyles = staticCompositionLocalOf { FashTextStyles() }

object FashTheme {
    val spacing: FashSpacing
        @[Composable ReadOnlyComposable] get() = LocalFashSpacing.current

    val textStyles: FashTextStyles
        @[Composable ReadOnlyComposable] get() = LocalFashTextStyles.current
}

/**
 * Digital Editorial theme — Vina-Pink, cream surfaces, Be Vietnam Pro.
 * @param dynamicColor When true (and on API 31+), uses system dynamic palette instead of brand colors.
 * @param lightAppearance When not in dark mode and [dynamicColor] is false: editorial paper vs full white
 *   (explicit Light in Settings). Ignored when [darkTheme] is true.
 */
@Composable
fun FashTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    /** When [darkTheme] is false and [dynamicColor] is off: editorial paper vs full white (explicit Light). */
    lightAppearance: FashLightAppearance = FashLightAppearance.Editorial,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> fashDarkColorScheme()
        else -> fashLightColorScheme(lightAppearance)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalFashSpacing provides FashSpacing(),
        LocalFashTextStyles provides FashTextStyles(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FashTypography,
            shapes = FashShapes,
            content = content,
        )
    }
}

/** @deprecated Use [FashTheme] for clarity */
@Deprecated("Renamed to FashTheme", ReplaceWith("FashTheme(darkTheme, dynamicColor, content)"))
@Composable
fun FashandroidmobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) = FashTheme(darkTheme, dynamicColor, lightAppearance = FashLightAppearance.Editorial, content = content)
