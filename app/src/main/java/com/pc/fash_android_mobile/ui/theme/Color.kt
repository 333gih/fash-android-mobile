package com.pc.fash_android_mobile.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

/**
 * Which light palette [fashLightColorScheme] uses. **Editorial** = warm paper (System default when device
 * is light). **PureWhite** = full white background when user picks **Light** in Settings.
 */
enum class FashLightAppearance {
    Editorial,
    PureWhite,
}

/**
 * Legacy Material-style names for the Fash palette. Brand and semantic text come from [FashColorTokens.LightEditorial];
 * surface fields match the **editorial** baseline — prefer [MaterialTheme.colorScheme] in composables so
 * **PureWhite** light mode is applied correctly.
 *
 * Prefer [fashLightColorScheme] / [fashDarkColorScheme] with Material 3; use [FashColors] for gradients
 * and one-off accents when [MaterialTheme.colorScheme] is not enough.
 */
object FashColors {
    /** Editorial coral — main actions and brand (matches latest login art) */
    val Primary = FashColorTokens.LightEditorial.brandPrimary

    /** Deeper tone for gradients / pressed */
    val PrimaryDeep = FashColorTokens.LightEditorial.brandPrimaryDeep

    /** Softer primary tint — backgrounds and secondary emphasis */
    val PrimaryContainer = FashColorTokens.LightEditorial.brandPrimaryContainer

    val OnPrimary = FashColorTokens.LightEditorial.onBrandPrimary

    val OnPrimaryContainer = FashColorTokens.LightEditorial.onBrandPrimaryContainer

    /** Editorial secondary surfaces — use [MaterialTheme.colorScheme.surfaceVariant] for theme-aware UI. */
    val SurfaceVariantCream = FashColorTokens.LightEditorial.surfaceVariant

    /** Editorial paper canvas — use [MaterialTheme.colorScheme.background] for theme-aware UI. */
    val SurfaceLowest = FashColorTokens.LightEditorial.screen

    /** Main feed / section bands */
    val SurfaceContainerLow = FashColorTokens.LightEditorial.surfaceContainerLow

    val SurfaceContainer = FashColorTokens.LightEditorial.surfaceContainer

    val SurfaceContainerHigh = FashColorTokens.LightEditorial.surfaceContainerHigh

    /** High-priority cards (e.g. buy-now) */
    val SurfaceContainerHighest = FashColorTokens.LightEditorial.surfaceContainerHighest

    /**
     * Settings → Display → “System default” selected row (warm editorial cream).
     * Matches brand reference #F5F2EA (RGB 245, 242, 234).
     */
    val SystemDefaultThemeHighlight = FashColorTokens.LightEditorial.settingsSystemDefaultRow

    val OnSurface = FashColorTokens.LightEditorial.textPrimary

    val OnSurfaceVariant = FashColorTokens.LightEditorial.textSecondary

    /**
     * Focused borders, key outlines. Warm brown-gray — readable on white without cold “tech gray.”
     * (Avoid pure white/near-white borders: they disappear on [SurfaceLowest].)
     */
    val Outline = FashColorTokens.LightEditorial.outlineStrong

    /**
     * Default hairlines, cards, chips, dividers — use [MaterialTheme.colorScheme.outlineVariant] in UI.
     */
    val OutlineVariant = FashColorTokens.LightEditorial.outlineMuted

    val SecondaryWarm = FashColorTokens.LightEditorial.secondary

    val OnSecondaryWarm = FashColorTokens.LightEditorial.onSecondary

    val SecondaryContainer = FashColorTokens.LightEditorial.secondaryContainer

    val OnSecondaryContainer = FashColorTokens.LightEditorial.onSecondaryContainer

    val TertiaryAccent = FashColorTokens.LightEditorial.tertiary

    val OnTertiary = FashColorTokens.LightEditorial.onTertiary

    /** FAB / floating — ambient tint base (#FF3B5C @ ~6% via modifiers) */
    val AmbientShadowBase = Primary

    val Error = FashColorTokens.LightEditorial.error

    val OnError = FashColorTokens.LightEditorial.onError

    val ErrorContainer = FashColorTokens.LightEditorial.errorContainer

    val OnErrorContainer = FashColorTokens.LightEditorial.onErrorContainer

    /** Success / available indicator (e.g. username check) */
    val Success = FashColorTokens.LightEditorial.success

    // —— Dark editorial —— //

    val PrimaryDark = FashColorTokens.Dark.brandPrimary

    val PrimaryContainerDark = FashColorTokens.Dark.brandPrimaryContainer

    val OnPrimaryDark = FashColorTokens.Dark.onBrandPrimary

    val OnPrimaryContainerDark = FashColorTokens.Dark.onBrandPrimaryContainer

    val SurfaceLowestDark = FashColorTokens.Dark.screen

    val SurfaceContainerLowDark = FashColorTokens.Dark.surfaceContainerLow

    val SurfaceContainerDark = FashColorTokens.Dark.surfaceContainer

    val SurfaceContainerHighDark = FashColorTokens.Dark.surfaceContainerHigh

    val SurfaceContainerHighestDark = FashColorTokens.Dark.surfaceContainerHighest

    val SurfaceVariantDark = FashColorTokens.Dark.surfaceVariant

    val OnSurfaceDark = FashColorTokens.Dark.textPrimary

    val OnSurfaceVariantDark = FashColorTokens.Dark.textSecondary

    val OutlineDark = FashColorTokens.Dark.outlineStrong

    val OutlineVariantDark = FashColorTokens.Dark.outlineMuted

    val SecondaryDark = FashColorTokens.Dark.secondary

    val OnSecondaryDark = FashColorTokens.Dark.onSecondary

    val SecondaryContainerDark = FashColorTokens.Dark.secondaryContainer

    val OnSecondaryContainerDark = FashColorTokens.Dark.onSecondaryContainer

    val TertiaryDark = FashColorTokens.Dark.tertiary

    val OnTertiaryDark = FashColorTokens.Dark.onTertiary

    val ErrorDark = FashColorTokens.Dark.error

    val OnErrorDark = FashColorTokens.Dark.onError

    val ErrorContainerDark = FashColorTokens.Dark.errorContainer

    val OnErrorContainerDark = FashColorTokens.Dark.onErrorContainer
}

fun fashLightColorScheme(
    appearance: FashLightAppearance = FashLightAppearance.Editorial,
) = run {
    val L = lightSurfacePalette(appearance)
    val E = FashColorTokens.LightEditorial
    lightColorScheme(
        primary = E.brandPrimary,
        onPrimary = E.onBrandPrimary,
        primaryContainer = E.brandPrimaryContainer,
        onPrimaryContainer = E.onBrandPrimaryContainer,
        secondary = E.secondary,
        onSecondary = E.onSecondary,
        secondaryContainer = E.secondaryContainer,
        onSecondaryContainer = E.onSecondaryContainer,
        tertiary = E.tertiary,
        onTertiary = E.onTertiary,
        background = L.screen,
        onBackground = E.textPrimary,
        surface = L.screen,
        onSurface = E.textPrimary,
        surfaceVariant = L.surfaceVariant,
        onSurfaceVariant = E.textSecondary,
        surfaceContainerLowest = L.screen,
        surfaceContainerLow = L.surfaceContainerLow,
        surfaceContainer = L.surfaceContainer,
        surfaceContainerHigh = L.surfaceContainerHigh,
        surfaceContainerHighest = L.surfaceContainerHighest,
        outline = L.outlineStrong,
        outlineVariant = L.outlineMuted,
        error = E.error,
        onError = E.onError,
        errorContainer = E.errorContainer,
        onErrorContainer = E.onErrorContainer,
    )
}

private fun lightSurfacePalette(appearance: FashLightAppearance): LightSurfacePalette = when (appearance) {
    FashLightAppearance.Editorial -> FashColorTokens.LightEditorial
    FashLightAppearance.PureWhite -> FashColorTokens.LightPureWhite
}

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
