package com.pc.fash_android_mobile.ui.post

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.onboarding.OnboardingProgressBar
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.fashReadableOn

/**
 * Single top row: optional back, close (X), centered step label, primary action (NEXT / POST).
 * Matches full-screen posting flow — no main app bar.
 */
@Composable
fun CreateListingFlowHeader(
    step: Int,
    totalSteps: Int,
    onBackClick: (() -> Unit)?,
    onCloseClick: () -> Unit,
    primaryLabelRes: Int,
    onPrimaryClick: () -> Unit,
    primaryEnabled: Boolean,
    primaryLoading: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = FashColors.Primary,
                        )
                    }
                }
                IconButton(onClick = onCloseClick) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.create_listing_close_cd),
                        tint = scheme.onSurface,
                    )
                }
            }
            Text(
                text = stringResource(R.string.create_listing_step, step, totalSteps),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onPrimaryClick,
                    enabled = primaryEnabled && !primaryLoading,
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FashColors.Primary,
                        contentColor = FashColors.Primary.fashReadableOn(),
                        disabledContainerColor = scheme.surfaceContainerHighest,
                        disabledContentColor = scheme.onSurfaceVariant,
                    ),
                ) {
                    if (primaryLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = FashColors.Primary.fashReadableOn(),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = stringResource(primaryLabelRes),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }
        OnboardingProgressBar(currentStep = step, totalSteps = totalSteps)
        Spacer(modifier = Modifier.height(4.dp))
    }
}
