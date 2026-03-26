package com.pc.fash_android_mobile.data.auth

import java.io.IOException

/**
 * Whether a failed token refresh should **keep** the stored session (transient)
 * or allow clearing it (permanent / invalid credentials).
 *
 * - **Transient:** no TCP/TLS response, timeouts, 5xx, 429, 408 — user stays logged in;
 *   the next API call may succeed or trigger another refresh.
 * - **Permanent:** 401/403/400/404 on refresh — invalid or rejected refresh token.
 */
internal fun Throwable.isTransientRefreshFailure(): Boolean {
    var t: Throwable? = this
    while (t != null) {
        when (t) {
            is IOException -> return true
            is AuthHttpException -> {
                val c = t.httpCode
                return c in 500..599 || c == 429 || c == 408
            }
        }
        t = t.cause
    }
    return false
}
