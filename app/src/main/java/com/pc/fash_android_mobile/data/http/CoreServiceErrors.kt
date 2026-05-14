package com.pc.fash_android_mobile.data.http

import org.json.JSONObject

/**
 * Thrown by core-service HTTP clients when `!isSuccessful`; [httpCode] is the response status.
 * [message] is from JSON `error` when present ([CoreServiceErrors.parseErrorMessage]).
 */
class CoreServiceHttpException(
    val httpCode: Int,
    message: String,
) : Exception(message)

/**
 * core-service listing (and shared handler) error JSON:
 * `{ "code": <HTTP status int>, "error": "<message>" }`.
 * There is no separate app-level code string in the body; branch on HTTP status and show [error].
 */
object CoreServiceErrors {

    /**
     * Returns the user-facing message: prefers JSON [error], then non-JSON body, then a short default per HTTP code.
     */
    fun parseErrorMessage(httpCode: Int, body: String?): String {
        val raw = body?.trim().orEmpty()
        if (raw.isEmpty()) return fallbackMessage(httpCode)
        if (raw.startsWith("{")) {
            try {
                val o = JSONObject(raw)
                val err = o.optString("error", "").trim()
                if (err.isNotBlank()) return err
                // fash-auth-service (and other services) use { "code", "message" }
                val msg = o.optString("message", "").trim()
                if (msg.isNotBlank()) return msg
            } catch (_: Exception) {
                // fall through
            }
        }
        return raw.ifBlank { fallbackMessage(httpCode) }
    }

    private fun fallbackMessage(httpCode: Int): String = when (httpCode) {
        400 -> "Bad request"
        401 -> "Authentication required"
        403 -> "Permission denied"
        404 -> "Not found"
        409 -> "Not available"
        500 -> "Server error"
        else -> "Request failed (HTTP $httpCode)"
    }
}
