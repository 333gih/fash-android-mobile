package com.pc.fash_android_mobile.ui.guest

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.ui.theme.FashColors
import com.pc.fash_android_mobile.ui.theme.FashTheme

/**
 * Compact top-bar CTA for guest browse — replaces inbox/orders icons so sign-in is the only account action.
 */
@Composable
fun GuestTopBarSignInAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.guest_topbar_sign_in)
    val cd = stringResource(R.string.guest_topbar_sign_in_cd)
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 36.dp)
            .semantics { contentDescription = cd },
        contentPadding = PaddingValues(horizontal = FashTheme.spacing.spacing2, vertical = 4.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = FashColors.Primary.copy(alpha = 0.14f),
            contentColor = FashColors.Primary,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}
