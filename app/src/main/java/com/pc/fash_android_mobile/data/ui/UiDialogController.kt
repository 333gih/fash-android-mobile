package com.pc.fash_android_mobile.data.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single global dialog queue (last show wins). Use from ViewModels via
 * [com.pc.fash_android_mobile.ui.common.showUiDialog] or [FashApplication.uiDialog].
 */
class UiDialogController {

    private val _current = MutableStateFlow<UiDialogMessage?>(null)
    val current: StateFlow<UiDialogMessage?> = _current.asStateFlow()

    fun showSuccess(message: String, title: String? = null) {
        _current.value = UiDialogMessage.Success(title = title, message = message)
    }

    fun showError(message: String, title: String? = null) {
        _current.value = UiDialogMessage.Error(title = title, message = message)
    }

    fun showInfo(message: String, title: String? = null) {
        _current.value = UiDialogMessage.Info(title = title, message = message)
    }

    fun dismiss() {
        _current.value = null
    }
}
