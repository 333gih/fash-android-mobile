package com.pc.fash_android_mobile.ui.post

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.pc.fash_android_mobile.R

@Composable
fun CreateListingFlowScreen(
    viewModel: PostViewModel,
    onClose: () -> Unit,
) {
    val step by viewModel.step.collectAsState()
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    fun handleCloseAttempt() {
        if (step >= 2) {
            showDiscardDialog = true
        } else {
            onClose()
        }
    }

    BackHandler {
        if (showDiscardDialog) {
            showDiscardDialog = false
            return@BackHandler
        }
        when (step) {
            1 -> onClose()
            2 -> viewModel.prevStep()
            3 -> viewModel.prevStep()
            else -> onClose()
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.create_listing_discard_title)) },
            text = { Text(stringResource(R.string.create_listing_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onClose()
                    },
                ) {
                    Text(stringResource(R.string.create_listing_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.create_listing_discard_cancel))
                }
            },
        )
    }

    when (step) {
        1 -> CreateListingStep1Screen(
            viewModel = viewModel,
            onClose = onClose,
        )
        2 -> CreateListingStep2Screen(
            viewModel = viewModel,
            onCloseRequest = { handleCloseAttempt() },
        )
        3 -> CreateListingStep3Screen(
            viewModel = viewModel,
            onCloseRequest = { handleCloseAttempt() },
            onSubmitSuccess = onClose,
        )
        else -> CreateListingStep1Screen(
            viewModel = viewModel,
            onClose = onClose,
        )
    }
}
