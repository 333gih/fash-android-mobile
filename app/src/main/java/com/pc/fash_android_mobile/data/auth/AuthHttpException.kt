package com.pc.fash_android_mobile.data.auth

/**
 * Non-success HTTP response from the auth service (login, refresh, etc.).
 * Used to distinguish **definitive** auth failures (4xx) from **transient** server/network issues.
 */
class AuthHttpException(
    val httpCode: Int,
    message: String,
) : Exception(message)
