package com.pc.fash_android_mobile.data.recommendation

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Emits one `app_open` feed event per foreground session when the process reaches STARTED.
 * Sends presence v2 frames to the realtime service on foreground/background transitions.
 */
class AppSessionTracker(
    private val feedEventReporter: FeedEventReporter,
    private val onForeground: () -> Unit = {},
    private val onBackground: () -> Unit = {},
) : DefaultLifecycleObserver {

    @Volatile
    private var reportedThisForegroundSession = false

    override fun onStart(owner: LifecycleOwner) {
        onForeground()
        if (reportedThisForegroundSession) return
        reportedThisForegroundSession = true
        feedEventReporter.appOpen()
    }

    override fun onStop(owner: LifecycleOwner) {
        onBackground()
        reportedThisForegroundSession = false
    }

    fun install() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
}
