package com.pc.fash_android_mobile.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.pc.fash_android_mobile.ui.theme.FashBrandTypography
import com.pc.fash_android_mobile.ui.theme.FashColors

/**
 * Displays brand copy in the bold-italic “FASH MARKETPLACE” editorial style.
 * Pass already-localised strings; use [uppercase] for all-caps lockups when needed.
 */
@Composable
fun FashBrandMarkText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = FashBrandTypography.markBoldItalic,
    color: Color = FashColors.Primary,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        style = style,
        color = color,
        textAlign = textAlign ?: TextAlign.Unspecified,
        modifier = modifier,
    )
}
