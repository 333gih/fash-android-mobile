package com.pc.fash_android_mobile.data.auth

import com.pc.fash_android_mobile.data.http.ServiceError

/**
 * Non-success HTTP response from the auth service (login, refresh, etc.).
 * Used to distinguish **definitive** auth failures (4xx) from **transient** server/network issues.
 */
class AuthHttpException(
    val httpCode: Int,
    message: String,
    val serviceError: ServiceError? = null,
) : Exception(message) {
    val errorCode: String? get() = serviceError?.code
    val isRateLimited: Boolean get() = serviceError?.isRateLimited == true
    val retryAfterSeconds: Int? get() = serviceError?.retryAfterSeconds
}
