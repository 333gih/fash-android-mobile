package com.pc.fash_android_mobile.ui.locale

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.data.locale.AppLocale
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.fashReadableOn

/**
 * Pill segmented control (VN | EN) for the login screen only.
 * Pink thumb slides between halves; labels invert with selection.
 */
@Composable
fun LoginLanguageToggle(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isEnglish = AppLocale.currentTag(context) == AppLocale.TAG_EN
    val trackBg = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f)
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val mutedLabel = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val horizontalInset = 2.dp

    BoxWithConstraints(
        modifier = modifier
            .width(120.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .border(1.dp, borderColor, RoundedCornerShape(17.dp))
            .background(trackBg),
    ) {
        val innerW = maxWidth - horizontalInset * 2
        val segmentW = innerW / 2
        val thumbOffset by animateDpAsState(
            targetValue = if (isEnglish) horizontalInset + segmentW else horizontalInset,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            label = "loginLocaleThumb",
        )

        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(segmentW)
                .fillMaxHeight()
                .padding(vertical = 2.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(FashColors.Primary),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics {
                        role = Role.Button
                        contentDescription = "VN"
                    }
                    .clickable { AppLocale.setLocale(context, AppLocale.TAG_VI) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "VN",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (!isEnglish) FashColors.Primary.fashReadableOn() else mutedLabel,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics {
                        role = Role.Button
                        contentDescription = "EN"
                    }
                    .clickable { AppLocale.setLocale(context, AppLocale.TAG_EN) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "EN",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isEnglish) FashColors.Primary.fashReadableOn() else mutedLabel,
                )
            }
        }
    }
}
