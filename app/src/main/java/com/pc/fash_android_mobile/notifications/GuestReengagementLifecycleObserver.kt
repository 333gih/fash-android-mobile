package com.pc.fash_android_mobile.notifications

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/** Reschedules guest local reminder when the app leaves foreground. */
class GuestReengagementLifecycleObserver(
    private val appContext: Context,
) : DefaultLifecycleObserver {

    fun register() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun unregister() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        GuestLocalReengagementScheduler.cancel(appContext)
    }

    override fun onStop(owner: LifecycleOwner) {
        GuestLocalReengagementScheduler.scheduleAfterBackground(appContext)
    }
}
