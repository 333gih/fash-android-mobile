package com.pc.fash_android_mobile.data.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque

/**
 * Global success / error / info dialogs in **FIFO order**: rapid [showSuccess]/[showError]/[showInfo] calls
 * enqueue; the UI shows one at a time and advances when the user dismisses ([dismiss]).
 *
 * Use from ViewModels via [com.pc.fash_android_mobile.ui.common.showUiDialog] or [FashApplication.uiDialog].
 */
class UiDialogController {

    private val lock = Any()
    private val queue = ArrayDeque<UiDialogMessage>()
    private val _current = MutableStateFlow<UiDialogMessage?>(null)
    val current: StateFlow<UiDialogMessage?> = _current.asStateFlow()

    private fun enqueue(message: UiDialogMessage) {
        synchronized(lock) {
            if (_current.value == null) {
                _current.value = message
            } else {
                queue.addLast(message)
            }
        }
    }

    fun showSuccess(message: String, title: String? = null) {
        enqueue(UiDialogMessage.Success(title = title, message = message))
    }

    fun showError(message: String, title: String? = null) {
        enqueue(UiDialogMessage.Error(title = title, message = message))
    }

    fun showInfo(message: String, title: String? = null) {
        enqueue(UiDialogMessage.Info(title = title, message = message))
    }

    /** Dismisses the visible dialog and shows the next queued message, if any. */
    fun dismiss() {
        synchronized(lock) {
            _current.value = queue.pollFirst()
        }
    }
}
