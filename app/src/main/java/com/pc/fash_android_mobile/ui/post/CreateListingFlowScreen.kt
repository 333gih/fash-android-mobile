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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
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
        when {
            step <= 1 -> onClose()
            else -> viewModel.prevStep()
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

    Box(modifier = Modifier.fillMaxSize()) {
        when (step) {
            1 -> CreateListingPostStep1(viewModel = viewModel, onCloseRequest = { handleCloseAttempt() })
            2 -> CreateListingPostStep2(viewModel = viewModel, onCloseRequest = { handleCloseAttempt() })
            3 -> CreateListingPostStep3(viewModel = viewModel, onCloseRequest = { handleCloseAttempt() })
            4 -> CreateListingPostStep4(viewModel = viewModel, onCloseRequest = { handleCloseAttempt() })
            5 -> CreateListingPostStep5(viewModel = viewModel, onCloseRequest = { handleCloseAttempt() })
            6 -> CreateListingPostStep6(viewModel = viewModel, onCloseRequest = { handleCloseAttempt() })
            7 -> CreateListingPostStep7(viewModel = viewModel, onCloseRequest = { handleCloseAttempt() })
            8 -> CreateListingPostStep8(viewModel = viewModel, onCloseRequest = { handleCloseAttempt() })
            9 -> CreateListingPostStep9(viewModel = viewModel, onCloseRequest = { handleCloseAttempt() })
            10 -> CreateListingPostStep10(
                viewModel = viewModel,
                onCloseRequest = { handleCloseAttempt() },
                onSubmitSuccess = onClose,
            )
            else -> CreateListingPostStep1(viewModel = viewModel, onCloseRequest = { handleCloseAttempt() })
        }
    }
}
