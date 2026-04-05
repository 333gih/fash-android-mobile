package com.pc.fash_android_mobile.ui.chat

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Emits when a conversation was marked read in [ChatDetailViewModel] so [ChatViewModel] can refresh
 * the global unread badge and inbox rows without waiting for realtime or back navigation.
 */
object ChatUnreadRefreshHub {
    private val _signals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val signals: SharedFlow<Unit> = _signals.asSharedFlow()

    fun notifyMarkedRead() {
        _signals.tryEmit(Unit)
    }
}
