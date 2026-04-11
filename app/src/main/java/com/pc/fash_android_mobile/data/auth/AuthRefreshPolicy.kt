package com.pc.fash_android_mobile.data.auth

import org.json.JSONException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Whether a failed token refresh should **keep** the stored session (transient)
 * or allow clearing it (permanent / invalid credentials).
 *
 * - **Transient:** I/O and network errors (except below), JSON parse failures, 5xx, rate limits,
 *   and other ambiguous 4xx (e.g. 404 mis-route) — keep tokens so a later retry works.
 * - **Permanent:** [AuthHttpException] with **400** (invalid_grant), **401**, or **403** —
 *   rejected refresh token per typical OAuth-style semantics.
 * - **[SocketTimeoutException]:** connect/read exceeded OkHttp timeouts (e.g. 20s) — treat like
 *   logout so the user is not stuck with a stale session when the refresh endpoint never responds.
 */
internal fun Throwable.isTransientRefreshFailure(): Boolean {
    var t: Throwable? = this
    while (t != null) {
        when (t) {
            is JSONException -> return true
            is SocketTimeoutException -> return false
            is IOException -> return true
            is AuthHttpException -> {
                val c = t.httpCode
                return c !in DEFINITIVE_AUTH_FAILURE_CODES
            }
        }
        t = t.cause
    }
    return true
}

private val DEFINITIVE_AUTH_FAILURE_CODES = setOf(400, 401, 403)
