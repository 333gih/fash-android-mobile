package com.pc.fash_android_mobile.data.auth

import org.json.JSONException
import java.io.IOException

/**
 * Whether a failed token refresh should **keep** the stored session (transient)
 * or allow clearing it (permanent / invalid credentials).
 *
 * - **Transient:** I/O and network errors, JSON parse failures, 5xx, rate limits,
 *   and other ambiguous 4xx (e.g. 404 mis-route) — keep tokens so a later retry works.
 * - **Permanent:** [AuthHttpException] with **400** (invalid_grant), **401**, or **403** —
 *   rejected refresh token per typical OAuth-style semantics.
 */
internal fun Throwable.isTransientRefreshFailure(): Boolean {
    var t: Throwable? = this
    while (t != null) {
        when (t) {
            is JSONException -> return true
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
