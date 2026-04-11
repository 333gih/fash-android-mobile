package com.pc.fash_android_mobile.ui.orders

import android.content.Context
import android.location.LocationManager

/**
 * Best-effort last known coordinates for [com.pc.fash_android_mobile.data.chat.ChatRepository.checkInMeeting].
 * Requires [android.Manifest.permission.ACCESS_COARSE_LOCATION] (or finer) to be granted.
 */
object MeetupCheckInLocation {
    fun peekLastKnownLatLng(context: Context): Pair<Double, Double>? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return try {
            val candidates = lm.allProviders.orEmpty().mapNotNull { provider ->
                try {
                    lm.getLastKnownLocation(provider)
                } catch (_: SecurityException) {
                    null
                }
            }
            val best = candidates.maxByOrNull { it.time } ?: return null
            val lat = best.latitude
            val lng = best.longitude
            if (!lat.isFinite() || !lng.isFinite()) return null
            lat to lng
        } catch (_: SecurityException) {
            null
        }
    }
}
