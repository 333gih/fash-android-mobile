package com.pc.fash_android_mobile.ui.components

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import com.pc.fash_android_mobile.R
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Snackbar copy for like / save — iOS [FeedEngagementFeedback] + Android `listing_*_snackbar`.
 */
object FeedEngagementFeedback {
    @StringRes
    fun likeMessageRes(liked: Boolean): Int =
        if (liked) R.string.listing_like_added_snackbar else R.string.listing_like_removed_snackbar

    @StringRes
    fun saveMessageRes(saved: Boolean): Int =
        if (saved) R.string.listing_save_added_snackbar else R.string.listing_save_removed_snackbar
}

/** Reliable delivery for transient UI feedback (avoid dropping [MutableSharedFlow.tryEmit]). */
suspend fun AndroidViewModel.emitSnackbarMessage(
    events: MutableSharedFlow<String>,
    message: String,
) {
    val trimmed = message.trim()
    if (trimmed.isNotEmpty()) {
        events.emit(trimmed)
    }
}

suspend fun AndroidViewModel.emitSnackbarMessage(
    events: MutableSharedFlow<String>,
    @StringRes messageRes: Int,
) {
    emitSnackbarMessage(events, getApplication<Application>().getString(messageRes))
}
