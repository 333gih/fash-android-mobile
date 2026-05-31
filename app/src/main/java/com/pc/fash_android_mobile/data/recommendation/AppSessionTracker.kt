package com.pc.fash_android_mobile.data.recommendation

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Emits one `app_open` feed event per foreground session when the process reaches STARTED.
 */
class AppSessionTracker(
    private val feedEventReporter: FeedEventReporter,
) : DefaultLifecycleObserver {

    @Volatile
    private var reportedThisForegroundSession = false

    override fun onStart(owner: LifecycleOwner) {
        if (reportedThisForegroundSession) return
        reportedThisForegroundSession = true
        feedEventReporter.appOpen()
    }

    override fun onStop(owner: LifecycleOwner) {
        reportedThisForegroundSession = false
    }

    fun install() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
}
