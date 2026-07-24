package com.pc.fash_android_mobile.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Re-arms the guest local reminder after device reboot (AlarmManager alarms do not survive reboot).
 */
class GuestLocalReengagementBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val action = intent?.action ?: return
        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        Log.d(TAG, "reschedule after $action")
        GuestLocalReengagementScheduler.scheduleAfterBackground(context.applicationContext)
    }

    companion object {
        private const val TAG = "GuestLocalReminder"
    }
}
