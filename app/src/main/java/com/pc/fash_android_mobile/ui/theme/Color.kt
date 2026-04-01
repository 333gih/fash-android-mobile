package com.pc.fash_android_mobile.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Brand and surface tokens for "The Digital Editorial" design system.
 * Prefer [fashLightColorScheme] / [fashDarkColorScheme] with Material 3; use these for gradients and one-off accents.
 */
object FashColors {
    /** Editorial coral — main actions and brand (matches latest login art) */
    val Primary = Color(0xFFFF4B64)

    /** Deeper tone for gradients / pressed */
    val PrimaryDeep = Color(0xFFE03E56)

    /** Softer primary tint — backgrounds and secondary emphasis */
    val PrimaryContainer = Color(0xFFFF7A8C)

    val OnPrimary = Color(0xFFFFEFEC)

    val OnPrimaryContainer = Color(0xFF400014)

    /** App-wide screen and Material surface base — white canvas */
    val SurfaceVariantCream = Color.White

    /** Base canvas (lowest surface) */
    val SurfaceLowest = Color.White

    /** Main feed / section bands */
    val SurfaceContainerLow = Color.White

    val SurfaceContainer = Color.White

    val SurfaceContainerHigh = Color.White

    /** High-priority cards (e.g. buy-now) */
    val SurfaceContainerHighest = Color.White

    val OnSurface = Color(0xFF1C1917)

    val OnSurfaceVariant = Color(0xFF52443F)

    /**
     * Focused borders, key outlines. Warm brown-gray — readable on white without cold “tech gray.”
     * (Avoid pure white/near-white borders: they disappear on [SurfaceLowest].)
     */
    val Outline = Color(0xFF6A5F59)

    /**
     * Default hairlines, cards, chips, dividers on white. **Warm stone** (#BEB6B1 family): visibly distinct
     * from the canvas, softer than flat #D9D9D9, and on-brand next to coral [Primary].
     */
    val OutlineVariant = Color(0xFFBEB6B1)

    val SecondaryWarm = Color(0xFF6B5349)

    val OnSecondaryWarm = Color(0xFFFFFFFF)

    val SecondaryContainer = Color(0xFFFFDCC6)

    val OnSecondaryContainer = Color(0xFF331200)

    val TertiaryAccent = Color(0xFF8C4A3F)

    val OnTertiary = Color(0xFFFFFFFF)

    /** FAB / floating — ambient tint base (#FF3B5C @ ~6% via modifiers) */
    val AmbientShadowBase = Primary

    val Error = Color(0xFFBA1A1A)

    val OnError = Color(0xFFFFFFFF)

    val ErrorContainer = Color(0xFFFFDAD6)

    val OnErrorContainer = Color(0xFF410002)

    /** Success / available indicator (e.g. username check) */
    val Success = Color(0xFF2E7D32)

    // —— Dark editorial —— //

    val PrimaryDark = Color(0xFFFF6B7D)

    val PrimaryContainerDark = Color(0xFFB02140)

    val OnPrimaryDark = Color(0xFF5C0016)

    val OnPrimaryContainerDark = Color(0xFFFFDADA)

    val SurfaceLowestDark = Color(0xFF141210)

    val SurfaceContainerLowDark = Color(0xFF1C1916)

    val SurfaceContainerDark = Color(0xFF231F1C)

    val SurfaceContainerHighDark = Color(0xFF2B2623)

    val SurfaceContainerHighestDark = Color(0xFF342E2A)

    val SurfaceVariantDark = Color(0xFF3D3834)

    val OnSurfaceDark = Color(0xFFF5EFEA)

    val OnSurfaceVariantDark = Color(0xFFD0C4BC)

    val OutlineDark = Color(0xFF9D8B83)

    val OutlineVariantDark = Color(0xFF4A4340)

    val SecondaryDark = Color(0xFFD7C2B6)

    val OnSecondaryDark = Color(0xFF3B2D27)

    val SecondaryContainerDark = Color(0xFF524239)

    val OnSecondaryContainerDark = Color(0xFFF5DCCF)

    val TertiaryDark = Color(0xFFFFB4A8)

    val OnTertiaryDark = Color(0xFF561E16)

    val ErrorDark = Color(0xFFFFB4AB)

    val OnErrorDark = Color(0xFF690005)

    val ErrorContainerDark = Color(0xFF93000A)

    val OnErrorContainerDark = Color(0xFFFFDAD6)
}

fun fashLightColorScheme() = lightColorScheme(
    primary = FashColors.Primary,
    onPrimary = FashColors.OnPrimary,
    primaryContainer = FashColors.PrimaryContainer,
    onPrimaryContainer = FashColors.OnPrimaryContainer,
    secondary = FashColors.SecondaryWarm,
    onSecondary = FashColors.OnSecondaryWarm,
    secondaryContainer = FashColors.SecondaryContainer,
    onSecondaryContainer = FashColors.OnSecondaryContainer,
    tertiary = FashColors.TertiaryAccent,
    onTertiary = FashColors.OnTertiary,
    background = FashColors.SurfaceLowest,
    onBackground = FashColors.OnSurface,
    surface = FashColors.SurfaceLowest,
    onSurface = FashColors.OnSurface,
    surfaceVariant = FashColors.SurfaceVariantCream,
    onSurfaceVariant = FashColors.OnSurfaceVariant,
    surfaceContainerLowest = FashColors.SurfaceLowest,
    surfaceContainerLow = FashColors.SurfaceContainerLow,
    surfaceContainer = FashColors.SurfaceContainer,
    surfaceContainerHigh = FashColors.SurfaceContainerHigh,
    surfaceContainerHighest = FashColors.SurfaceContainerHighest,
    outline = FashColors.Outline,
    outlineVariant = FashColors.OutlineVariant,
    error = FashColors.Error,
    onError = FashColors.OnError,
    errorContainer = FashColors.ErrorContainer,
    onErrorContainer = FashColors.OnErrorContainer,
)

fun fashDarkColorScheme() = darkColorScheme(
    primary = FashColors.PrimaryDark,
    onPrimary = FashColors.OnPrimaryDark,
    primaryContainer = FashColors.PrimaryContainerDark,
    onPrimaryContainer = FashColors.OnPrimaryContainerDark,
    secondary = FashColors.SecondaryDark,
    onSecondary = FashColors.OnSecondaryDark,
    secondaryContainer = FashColors.SecondaryContainerDark,
    onSecondaryContainer = FashColors.OnSecondaryContainerDark,
    tertiary = FashColors.TertiaryDark,
    onTertiary = FashColors.OnTertiaryDark,
    background = FashColors.SurfaceLowestDark,
    onBackground = FashColors.OnSurfaceDark,
    surface = FashColors.SurfaceLowestDark,
    onSurface = FashColors.OnSurfaceDark,
    surfaceVariant = FashColors.SurfaceVariantDark,
    onSurfaceVariant = FashColors.OnSurfaceVariantDark,
    surfaceContainerLowest = FashColors.SurfaceLowestDark,
    surfaceContainerLow = FashColors.SurfaceContainerLowDark,
    surfaceContainer = FashColors.SurfaceContainerDark,
    surfaceContainerHigh = FashColors.SurfaceContainerHighDark,
    surfaceContainerHighest = FashColors.SurfaceContainerHighestDark,
    outline = FashColors.OutlineDark,
    outlineVariant = FashColors.OutlineVariantDark,
    error = FashColors.ErrorDark,
    onError = FashColors.OnErrorDark,
    errorContainer = FashColors.ErrorContainerDark,
    onErrorContainer = FashColors.OnErrorContainerDark,
)
