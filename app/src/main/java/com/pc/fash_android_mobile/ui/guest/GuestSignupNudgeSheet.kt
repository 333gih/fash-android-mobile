package com.pc.fash_android_mobile.ui.guest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pc.fash_android_mobile.R

/** Optional sign-up nudge for guests — at most once per 7 days (see guest-local-reminder.md). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestSignupNudgeSheet(
    title: String?,
    body: String?,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val displayTitle = title?.trim().orEmpty().ifEmpty {
        stringResource(R.string.guest_signup_nudge_title)
    }
    val displayBody = body?.trim().orEmpty().ifEmpty {
        stringResource(R.string.guest_signup_nudge_body)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = displayBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.guest_login_sheet_sign_in))
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.guest_login_sheet_continue_browsing),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
