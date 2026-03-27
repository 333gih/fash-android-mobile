package com.pc.fash_android_mobile.data.ui

/**
 * App-wide dialog payload for [UiDialogController] / [com.pc.fash_android_mobile.ui.components.FashGlobalDialogHost].
 */
sealed class UiDialogMessage {
    abstract val title: String?
    abstract val message: String

    data class Success(
        override val title: String? = null,
        override val message: String,
    ) : UiDialogMessage()

    data class Error(
        override val title: String? = null,
        override val message: String,
    ) : UiDialogMessage()

    data class Info(
        override val title: String? = null,
        override val message: String,
    ) : UiDialogMessage()
}
