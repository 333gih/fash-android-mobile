package com.pc.fash_android_mobile.data.auth

/**
 * Auth session from login/OTP/social/refresh. Tokens must never be logged or exposed.
 */
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresInSeconds: Long,
    val isNewUser: Boolean = false,
    val userId: String? = null,
    val unreadCount: Long = 0L,
)
