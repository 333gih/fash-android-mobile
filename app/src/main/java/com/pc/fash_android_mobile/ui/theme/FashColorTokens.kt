package com.pc.fash_android_mobile.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Surfaces + outlines shared by [LightEditorial] (System default / warm paper) and [LightPureWhite]
 * (explicit Light in Settings — full white canvas).
 */
interface LightSurfacePalette {
    val screen: Color
    val surfaceVariant: Color
    val surfaceContainerLow: Color
    val surfaceContainer: Color
    val surfaceContainerHigh: Color
    val surfaceContainerHighest: Color
    val outlineStrong: Color
    val outlineMuted: Color
}

/**
 * **Single source of truth for app colors.** Change branding, backgrounds, text, borders, and state
 * colors here — [FashColors] and Material 3 [fashLightColorScheme] / [fashDarkColorScheme] delegate
 * to these tokens, so the rest of the app keeps working without per-screen edits.
 *
 * **Related (edit alongside when tuning the whole UI):**
 * - Layout spacing & radii: [FashSpacing] in `Spacing.kt`
 * - Typography scale & fonts: `Type.kt`, [FashBrandTypography]
 * - Compose theme wiring: [FashTheme] in `Theme.kt`
 *
 * Prefer **semantic names** below (`textPrimary`, `screen`, `brandPrimary`, …) when adding new UI;
 * use [MaterialTheme.colorScheme] in composables so light/dark follow automatically.
 */
object FashColorTokens {

    /**
     * Light theme — editorial canvas, warm neutrals, coral brand.
     * Used when appearance is **System default** (device light) or any path using [FashLightAppearance.Editorial].
     */
    object LightEditorial : LightSurfacePalette {
        // —— Brand —— //
        val brandPrimary: Color = Color(0xFFFF4B64)
        val brandPrimaryDeep: Color = Color(0xFFE03E56)
        val brandPrimaryContainer: Color = Color(0xFFFF7A8C)
        val onBrandPrimary: Color = Color(0xFFFFEFEC)
        val onBrandPrimaryContainer: Color = Color(0xFF400014)

        // —— Surfaces (app canvas & elevation steps) —— //
        // Use a warm paper canvas + white “lifted” steps so cards, rows, and borders stay visible
        // instead of everything blending into one white plane.
        override val screen: Color = Color(0xFFF3F0EB)
        override val surfaceVariant: Color = Color(0xFFE8E3DD)
        override val surfaceContainerLow: Color = Color(0xFFF6F3EF)
        override val surfaceContainer: Color = Color(0xFFF9F7F4)
        override val surfaceContainerHigh: Color = Color(0xFFFBFAF8)
        override val surfaceContainerHighest: Color = Color(0xFFFFFFFF)

        /** Settings → Display → “System default” row when selected (do not use as full-screen bg). */
        val settingsSystemDefaultRow: Color = Color(0xFFF5F2EA)

        // —— Text —— //
        val textPrimary: Color = Color(0xFF1C1917)
        val textSecondary: Color = Color(0xFF52443F)

        // —— Borders & dividers —— //
        override val outlineStrong: Color = Color(0xFF5C534E)
        override val outlineMuted: Color = Color(0xFFA89890)

        // —— Secondary / tertiary —— //
        val secondary: Color = Color(0xFF6B5349)
        val onSecondary: Color = Color(0xFFFFFFFF)
        val secondaryContainer: Color = Color(0xFFFFDCC6)
        val onSecondaryContainer: Color = Color(0xFF331200)
        val tertiary: Color = Color(0xFF8C4A3F)
        val onTertiary: Color = Color(0xFFFFFFFF)

        // —— State —— //
        val error: Color = Color(0xFFBA1A1A)
        val onError: Color = Color(0xFFFFFFFF)
        val errorContainer: Color = Color(0xFFFFDAD6)
        val onErrorContainer: Color = Color(0xFF410002)
        val success: Color = Color(0xFF2E7D32)
    }

    /**
     * Explicit **Light** in Settings → Display: full white canvas; subtle neutrals for secondary
     * surfaces and visible borders so components don’t disappear.
     */
    object LightPureWhite : LightSurfacePalette {
        override val screen: Color = Color.White
        override val surfaceVariant: Color = Color(0xFFF0F0EE)
        override val surfaceContainerLow: Color = Color(0xFFFAFAFA)
        override val surfaceContainer: Color = Color(0xFFF5F5F5)
        override val surfaceContainerHigh: Color = Color.White
        override val surfaceContainerHighest: Color = Color.White
        override val outlineStrong: Color = Color(0xFF5C534E)
        override val outlineMuted: Color = Color(0xFFA09088)
    }

    /** Dark theme — warm charcoal surfaces, adjusted brand. */
    object Dark {
        val brandPrimary: Color = Color(0xFFFF6B7D)
        val brandPrimaryContainer: Color = Color(0xFFB02140)
        val onBrandPrimary: Color = Color(0xFF5C0016)
        val onBrandPrimaryContainer: Color = Color(0xFFFFDADA)

        val screen: Color = Color(0xFF141210)
        val surfaceContainerLow: Color = Color(0xFF1C1916)
        val surfaceContainer: Color = Color(0xFF231F1C)
        val surfaceContainerHigh: Color = Color(0xFF2B2623)
        val surfaceContainerHighest: Color = Color(0xFF342E2A)
        val surfaceVariant: Color = Color(0xFF3D3834)

        val textPrimary: Color = Color(0xFFF5EFEA)
        val textSecondary: Color = Color(0xFFD0C4BC)

        val outlineStrong: Color = Color(0xFF9D8B83)
        val outlineMuted: Color = Color(0xFF4A4340)

        val secondary: Color = Color(0xFFD7C2B6)
        val onSecondary: Color = Color(0xFF3B2D27)
        val secondaryContainer: Color = Color(0xFF524239)
        val onSecondaryContainer: Color = Color(0xFFF5DCCF)
        val tertiary: Color = Color(0xFFFFB4A8)
        val onTertiary: Color = Color(0xFF561E16)

        val error: Color = Color(0xFFFFB4AB)
        val onError: Color = Color(0xFF690005)
        val errorContainer: Color = Color(0xFF93000A)
        val onErrorContainer: Color = Color(0xFFFFDAD6)
    }
}
