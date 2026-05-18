package com.pc.fash_android_mobile.deeplink

import android.content.Intent

/** Parses FCM / notification-tap extras for multi-account switch prompts. */
object AccountSwitchDeepLinks {
    const val EXTRA_PENDING_USER_ID = "pending_user_id"
    const val EXTRA_PENDING_EMAIL_MASKED = "pending_email_masked"
    const val EXTRA_UNREAD_COUNT = "unread_count"
    const val FCM_TYPE = "account.notification_pending"

    fun parseFromIntent(intent: Intent?): AccountSwitchPrompt? {
        if (intent == null) return null
        val userId = intent.getStringExtra(EXTRA_PENDING_USER_ID)?.trim().orEmpty()
        if (userId.isEmpty()) return null
        val email = intent.getStringExtra(EXTRA_PENDING_EMAIL_MASKED)?.trim().orEmpty()
        val unread = intent.getStringExtra(EXTRA_UNREAD_COUNT)?.toIntOrNull()
            ?: intent.getIntExtra(EXTRA_UNREAD_COUNT, 0).takeIf { it > 0 }
            ?: 1
        return AccountSwitchPrompt(
            pendingUserId = userId,
            emailMasked = email.ifEmpty { null },
            unreadCount = unread.coerceAtLeast(1),
        )
    }

    fun parseFromFcmData(data: Map<String, String>): AccountSwitchPrompt? {
        if (data["type"] != FCM_TYPE) return null
        val userId = data["pending_user_id"]?.trim().orEmpty()
        if (userId.isEmpty()) return null
        val email = data["pending_email_masked"]?.trim().orEmpty()
        val unread = data["unread_count"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        return AccountSwitchPrompt(
            pendingUserId = userId,
            emailMasked = email.ifEmpty { null },
            unreadCount = unread,
        )
    }
}

data class AccountSwitchPrompt(
    val pendingUserId: String,
    val emailMasked: String?,
    val unreadCount: Int,
)
