package com.pc.fash_android_mobile.ui.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R

/**
 * Show the global Fash dialog from any [AndroidViewModel] (e.g. after API [Result] failure).
 *
 * ```
 * result.onFailure { e ->
 *   showUiDialogError(e)
 * }
 * ```
 */
fun AndroidViewModel.showUiDialogSuccess(message: String, title: String? = null) {
    (getApplication<Application>() as FashApplication).uiDialog.showSuccess(message, title)
}

fun AndroidViewModel.showUiDialogError(message: String, title: String? = null) {
    (getApplication<Application>() as FashApplication).uiDialog.showError(message, title)
}

fun AndroidViewModel.showUiDialogError(throwable: Throwable, title: String? = null) {
    val msg = throwable.message?.takeIf { it.isNotBlank() }
        ?: getApplication<Application>().getString(R.string.feed_action_error)
    showUiDialogError(msg, title)
}

fun AndroidViewModel.showUiDialogInfo(message: String, title: String? = null) {
    (getApplication<Application>() as FashApplication).uiDialog.showInfo(message, title)
}
