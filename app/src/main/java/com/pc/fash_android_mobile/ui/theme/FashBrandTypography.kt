package com.pc.fash_android_mobile.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Bold italic uppercase brand treatment (reference: “FASH MARKETPLACE” editorial lockup).
 * Uses [BeVietnamProFamily]; italic may be synthesized if no italic font file is present.
 */
object FashBrandTypography {
    /** Top bar / hero wordmark */
    val markBoldItalic = TextStyle(
        fontFamily = BeVietnamProFamily,
        fontWeight = FontWeight.Bold,
        fontStyle = FontStyle.Italic,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.8.sp,
    )

    /** Explore title pairing — slightly smaller */
    val markBoldItalicMedium = markBoldItalic.copy(
        fontSize = 18.sp,
        lineHeight = 22.sp,
    )

    /** Login / splash hero */
    val markBoldItalicLarge = markBoldItalic.copy(
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = 1.sp,
    )

    /** Home footer strip */
    val markBoldItalicSmall = markBoldItalic.copy(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.2.sp,
    )

    /** Splash / waiting screen — large centered wordmark */
    val markSplashCenter = markBoldItalic.copy(
        fontSize = 52.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.5).sp,
    )

    /** Secondary line under mark (e.g. MARKETPLACE) */
    val marketplaceSubtitle = TextStyle(
        fontFamily = BeVietnamProFamily,
        fontWeight = FontWeight.Bold,
        fontStyle = FontStyle.Italic,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 2.sp,
    )
}
