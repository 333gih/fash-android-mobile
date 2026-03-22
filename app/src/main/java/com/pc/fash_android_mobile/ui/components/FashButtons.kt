package com.pc.fash_android_mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashGradients
import com.pc.fash_android_mobile.ui.theme.FashTheme

private val DefaultPrimaryCorner = 12.dp

/**
 * Primary CTA — 48dp min height. Default: gradient. Pass [solidFill] for a flat fill (e.g. login pill + soft shadow).
 */
@Composable
fun FashPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Center,
    cornerRadius: Dp = DefaultPrimaryCorner,
    /** When set, skips gradient and uses this solid color (login / marketing). */
    solidFill: Color? = null,
    /** Primary-tinted ambient shadow (no black); only applied with [solidFill]. */
    softShadowElevation: Dp = 0.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val minH = FashTheme.spacing.buttonHeight
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minH),
    ) {
        val brush = remember(constraints.maxWidth, minH, density, cornerRadius) {
            val w = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val h = with(density) { minH.toPx() }.coerceAtLeast(1f)
            FashGradients.primaryCta(w, h)
        }
        val bgModifier = if (solidFill != null) {
            Modifier.background(solidFill, shape)
        } else {
            Modifier.background(brush, shape)
        }
        val shadowMod =
            if (solidFill != null && softShadowElevation > 0.dp) {
                Modifier.shadow(
                    elevation = softShadowElevation,
                    shape = shape,
                    spotColor = solidFill.copy(alpha = 0.16f),
                    ambientColor = solidFill.copy(alpha = 0.09f),
                    clip = false,
                )
            } else {
                Modifier
            }
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(minH)
                .then(shadowMod)
                .then(bgModifier),
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = FashColors.OnPrimary,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = FashColors.OnPrimary.copy(alpha = 0.38f),
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
            contentPadding = PaddingValues(horizontal = FashTheme.spacing.spacing4),
        ) {
            ProvideTextStyle(
                MaterialTheme.typography.labelLarge.copy(color = LocalContentColor.current),
            ) {
                Row(
                    horizontalArrangement = horizontalArrangement,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }
        }
    }
}

@Composable
fun FashPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.buy_now),
    enabled: Boolean = true,
    cornerRadius: Dp = DefaultPrimaryCorner,
    solidFill: Color? = null,
    softShadowElevation: Dp = 0.dp,
) {
    FashPrimaryButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        cornerRadius = cornerRadius,
        solidFill = solidFill,
        softShadowElevation = softShadowElevation,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Secondary — ghost fill, 10% primary border.
 */
@Composable
fun FashSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.add_to_cart),
    enabled: Boolean = true,
) {
    val primary = MaterialTheme.colorScheme.primary
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(minHeight = FashTheme.spacing.buttonHeight)
            .fillMaxWidth(),
        shape = RoundedCornerShape(DefaultPrimaryCorner),
        border = BorderStroke(1.dp, primary.copy(alpha = 0.10f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = primary,
        ),
        contentPadding = PaddingValues(horizontal = FashTheme.spacing.spacing4),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}
