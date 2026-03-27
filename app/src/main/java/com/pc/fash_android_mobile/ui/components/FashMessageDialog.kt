package com.pc.fash_android_mobile.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.ui.UiDialogMessage
import com.pc.fash_android_mobile.ui.theme.FashColors

/**
 * Editorial-style alert for success / error / info; matches Material 3 + Fash tokens.
 */
@Composable
fun FashGlobalDialogHost(
    message: UiDialogMessage?,
    onDismiss: () -> Unit,
) {
    if (message == null) return
    val resolvedTitle = message.title ?: when (message) {
        is UiDialogMessage.Success -> stringResource(R.string.dialog_title_success)
        is UiDialogMessage.Error -> stringResource(R.string.dialog_title_error)
        is UiDialogMessage.Info -> stringResource(R.string.dialog_title_info)
    }
    val (iconVector, iconTint) = when (message) {
        is UiDialogMessage.Success -> Icons.Default.CheckCircle to FashColors.Success
        is UiDialogMessage.Error -> Icons.Default.ErrorOutline to FashColors.Error
        is UiDialogMessage.Info -> Icons.Default.Info to FashColors.Primary
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.dialog_ok),
                    color = FashColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        icon = {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconTint,
            )
        },
        title = {
            Text(
                text = resolvedTitle,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Text(
                text = message.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    )
}
