package com.pc.fash_android_mobile.data.recommendation

import android.content.Context
import java.util.UUID

/** Stable anonymous session id for guest feed events and public browse analytics. */
class BrowseSessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun sessionId(): String {
        var id = prefs.getString(KEY, null)
        if (id.isNullOrBlank()) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY, id).apply()
        }
        return id
    }

    fun sessionIdForUser(userId: String): String = "u:$userId"

    companion object {
        private const val PREFS = "fash_browse_session"
        private const val KEY = "guest_session_id"
    }
}
