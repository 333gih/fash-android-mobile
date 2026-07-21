package com.pc.fash_android_mobile.data.http

import android.content.Context
import com.pc.fash_android_mobile.R
import org.json.JSONObject

/**
 * Parsed error from FASH HTTP APIs (core-service, auth-service, Kong gateway).
 */
data class ServiceError(
    val httpCode: Int,
    val code: String?,
    val message: String,
    val retryAfterSeconds: Int? = null,
) {
    val isRateLimited: Boolean
        get() = httpCode == 429 || code in RATE_LIMIT_CODES

    companion object {
        val RATE_LIMIT_CODES = setOf(
            "RATE_LIMIT_EXCEEDED",
            "OTP_REQUEST_LIMIT",
            "OTP_LOCKED",
            "TOO_MANY_PAYMENT_ATTEMPTS",
        )
    }
}

/**
 * Thrown by core-service HTTP clients when `!isSuccessful`; [httpCode] is the response status.
 * [message] is from JSON `error` / `message` when present ([CoreServiceErrors.parseErrorMessage]).
 */
class CoreServiceHttpException(
    val httpCode: Int,
    message: String,
    val serviceError: ServiceError? = null,
) : Exception(message) {
    val errorCode: String? get() = serviceError?.code
    val isRateLimited: Boolean get() = serviceError?.isRateLimited == true
    val retryAfterSeconds: Int? get() = serviceError?.retryAfterSeconds
}

/**
 * core-service listing (and shared handler) error JSON:
 * `{ "code": <HTTP status int>, "error": "<message>" }` or auth `{ "code": "<string>", "message" }`.
 */
object CoreServiceErrors {

    fun parse(httpCode: Int, body: String?, retryAfterHeader: String? = null): ServiceError {
        val retryAfter = parseRetryAfter(retryAfterHeader)
        val raw = body?.trim().orEmpty()
        if (raw.isEmpty()) {
            return ServiceError(httpCode, normalizedCode(null, httpCode), fallbackMessage(httpCode), retryAfter)
        }
        if (raw.startsWith("{")) {
            try {
                val o = JSONObject(raw)
                val code = parseCodeField(o, httpCode)
                val err = o.optString("error", "").trim()
                if (err.isNotBlank()) {
                    return ServiceError(httpCode, normalizedCode(code, httpCode), err, retryAfter)
                }
                val msg = o.optString("message", "").trim()
                if (msg.isNotBlank()) {
                    return ServiceError(httpCode, normalizedCode(code, httpCode), msg, retryAfter)
                }
            } catch (_: Exception) {
                // fall through
            }
        }
        return ServiceError(httpCode, normalizedCode(null, httpCode), raw, retryAfter)
    }

    fun toHttpException(httpCode: Int, body: String?, retryAfterHeader: String? = null): CoreServiceHttpException {
        val parsed = parse(httpCode, body, retryAfterHeader)
        return CoreServiceHttpException(parsed.httpCode, parsed.message, parsed)
    }

    /**
     * Returns the user-facing message: prefers JSON [error], then non-JSON body, then a short default per HTTP code.
     */
    fun parseErrorMessage(httpCode: Int, body: String?): String = parse(httpCode, body).message

    /**
     * Maps a [ServiceError] to a localized string when the client should override generic English/Kong text.
     */
    fun localizedMessage(context: Context, error: ServiceError, otpContext: Boolean = false): String {
        if (error.isRateLimited) {
            val retry = error.retryAfterSeconds
            return when {
                otpContext && retry != null && retry > 0 ->
                    context.getString(R.string.error_rate_limit_otp_wait, retry)
                otpContext ->
                    context.getString(R.string.error_rate_limit_otp)
                retry != null && retry > 0 ->
                    context.getString(R.string.error_rate_limit_wait, retry)
                else ->
                    context.getString(R.string.error_rate_limit_generic)
            }
        }
        return when (error.code?.uppercase()) {
            "MISSING_TOKEN" -> context.getString(R.string.error_auth_missing_token)
            "SOCIAL_AUTH_DISABLED" -> context.getString(R.string.error_social_auth_disabled)
            "SOCIAL_AUTH_FAILED" -> context.getString(R.string.error_social_auth_failed)
            "INVALID_APPLICATION_ID" -> context.getString(R.string.error_invalid_application_id)
            else -> error.message.takeIf { it.isNotBlank() } ?: localizedHttpFallback(context, error.httpCode)
        }
    }

    private fun localizedHttpFallback(context: Context, httpCode: Int): String = when (httpCode) {
        400 -> context.getString(R.string.error_http_bad_request)
        401 -> context.getString(R.string.error_http_unauthorized)
        403 -> context.getString(R.string.error_http_forbidden)
        404 -> context.getString(R.string.error_http_not_found)
        409 -> context.getString(R.string.error_http_conflict)
        429 -> context.getString(R.string.error_rate_limit_generic)
        in 500..599 -> context.getString(R.string.error_http_server)
        else -> context.getString(R.string.error_http_status, httpCode.toString())
    }

    private fun parseCodeField(o: JSONObject, httpCode: Int): String? {
        if (!o.has("code")) return null
        return when (val v = o.get("code")) {
            is String -> v.trim().takeIf { it.isNotBlank() }
            is Number -> if (v.toInt() == 429 || httpCode == 429) "RATE_LIMIT_EXCEEDED" else null
            else -> null
        }
    }

    private fun normalizedCode(parsed: String?, httpCode: Int): String? {
        if (parsed != null) return parsed
        if (httpCode == 429) return "RATE_LIMIT_EXCEEDED"
        return null
    }

    private fun parseRetryAfter(header: String?): Int? {
        val raw = header?.trim().orEmpty()
        if (raw.isEmpty()) return null
        return raw.toIntOrNull()?.takeIf { it > 0 }
            ?: raw.toLongOrNull()?.takeIf { it > 0 }?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt()
    }

    private fun fallbackMessage(httpCode: Int): String = when (httpCode) {
        400 -> "Bad request"
        401 -> "Authentication required"
        403 -> "Permission denied"
        404 -> "Not found"
        409 -> "Not available"
        429 -> "Too many requests. Please try again later."
        500 -> "Server error"
        else -> "Request failed (HTTP $httpCode)"
    }
}
