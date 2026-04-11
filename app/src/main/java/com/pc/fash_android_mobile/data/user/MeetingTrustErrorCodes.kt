package com.pc.fash_android_mobile.data.user

/**
 * Error codes from core-service meeting trust / propose flows.
 * Chat HTTP errors embed `code=…` via [com.pc.fash_android_mobile.data.chat] helpers.
 */
object MeetingTrustErrorCodes {

    const val MEETING_IDENTITY_REVERIFY_REQUIRED = "MEETING_IDENTITY_REVERIFY_REQUIRED"

    fun isIdentityReverifyRequired(message: String?): Boolean {
        val m = message.orEmpty()
        if (m.isEmpty()) return false
        return m.contains("code=$MEETING_IDENTITY_REVERIFY_REQUIRED", ignoreCase = true) ||
            m.contains(MEETING_IDENTITY_REVERIFY_REQUIRED, ignoreCase = true)
    }
}
