package com.pc.fash_android_mobile.ui.post

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun CreateListingFlowScreen(
    viewModel: PostViewModel,
    onClose: () -> Unit,
) {
    val step by viewModel.step.collectAsState()
    when (step) {
        1 -> CreateListingStep1Screen(
            viewModel = viewModel,
            onClose = onClose,
            onNext = { /* advances via viewModel.nextStep() */ },
        )
        2 -> CreateListingStep2Screen(
            viewModel = viewModel,
            onClose = onClose,
            onNext = { viewModel.nextStep() },
        )
        3 -> CreateListingStep3Screen(
            viewModel = viewModel,
            onClose = onClose,
            onSubmitSuccess = onClose,
        )
        else -> CreateListingStep1Screen(
            viewModel = viewModel,
            onClose = onClose,
            onNext = { },
        )
    }
}
