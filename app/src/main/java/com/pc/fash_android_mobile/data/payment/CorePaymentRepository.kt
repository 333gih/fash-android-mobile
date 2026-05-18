package com.pc.fash_android_mobile.data.payment

import com.pc.fash_android_mobile.BuildConfig
import com.pc.fash_android_mobile.config.AppEnvironment
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

/**
 * **core-service only** — initiates payment for an existing order so the backend can call
 * payment-service `internal/payments/initiate` with secrets. The app never calls payment-service
 * internal routes directly (see ANDROID_CORE_REALTIME_INTEGRATION.md, payment section).
 *
 * Expected: `POST {API_BASE}/{CORE_PAYMENT_INITIATE_PATH}` with `%s` → order id.
 * Response: `payment_url`, optional `transaction_id`, `expires_at`.
 */
class CorePaymentRepository(
    private val securedClient: OkHttpClient,
) {

    fun initiatePayment(
        orderId: String,
        paymentMethod: String,
        redirectUrl: String,
        shipping: CheckoutAddress?,
    ): Result<PaymentInitiateResult> = runCatching {
        val oid = orderId.trim()
        if (oid.isBlank()) error("order_id required")
        val pathTemplate = BuildConfig.CORE_PAYMENT_INITIATE_PATH.trim()
        val path = String.format(pathTemplate, oid)
        val url = AppEnvironment.apiPath(path.trimStart('/'))
        val json = JSONObject()
            .put("payment_method", paymentMethod.trim().lowercase())
            .put("redirect_url", redirectUrl.trim())
        shipping?.let { s ->
            json.put(
                "shipping_address",
                JSONObject()
                    .put("recipient_name", s.fullName.trim())
                    .put("phone", s.phone.trim())
                    .put("line1", s.address.trim())
                    .put("district", s.district.trim())
                    .put("city", s.city.trim()),
            )
        }
        val idem = UUID.randomUUID().toString()
        val body = executePostJson(url, json.toString(), idempotencyKey = idem)
        parseInitiateResponse(body)
    }

    /** Enabled gateways from core → payment-service catalog. */
    fun listPaymentMethods(): Result<List<PaymentGatewayOption>> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/payment-methods")
        val raw = executeGet(url)
        parsePaymentMethodsResponse(raw)
    }

    /**
     * Optional: core may expose payment status for polling. If 404, returns failure.
     */
    fun getPaymentStatus(orderId: String): Result<PaymentStatusResult> = runCatching {
        val oid = orderId.trim()
        val url = AppEnvironment.apiPath("api/v1/orders/$oid/payments/status")
        val body = executeGet(url)
        parseStatusResponse(body)
    }

    private fun executePostJson(url: String, json: String, idempotencyKey: String? = null): String {
        return securedClient.newCall(
            Request.Builder()
                .url(url)
                .post(json.toRequestBody(JSON_MEDIA))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "FashAndroid/1.0")
                .apply {
                    idempotencyKey?.takeIf { it.isNotBlank() }?.let {
                        header("X-Idempotency-Key", it)
                        header("Idempotency-Key", it)
                    }
                }
                .build(),
        ).execute().use { response ->
            val respBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("HTTP ${response.code}: ${parseApiErrorMessage(respBody)}")
            }
            respBody.ifBlank { "{}" }
        }
    }

    private fun executeGet(url: String): String {
        return securedClient.newCall(
            Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .header("User-Agent", "FashAndroid/1.0")
                .build(),
        ).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("HTTP ${response.code}: ${parseApiErrorMessage(body)}")
            }
            body
        }
    }

    private fun parseApiErrorMessage(raw: String): String {
        return try {
            val o = JSONObject(raw.trim().ifBlank { "{}" })
            when (o.optString("error_code", "").uppercase()) {
                "PAYMENT_METHOD_UNAVAILABLE" ->
                    "Phương thức thanh toán chưa được bật trên máy chủ. Thử lại sau hoặc chọn phương thức khác."
                "ORDER_NOT_PAYABLE" ->
                    "Đơn hàng không ở trạng thái chờ thanh toán."
                "SELLER_PAYOUT_NOT_CONFIGURED" ->
                    "Người bán chưa cấu hình tài khoản nhận tiền."
                "FEE_CALCULATION_ERROR", "ORDER_FEE_MISMATCH" ->
                    "Lỗi tính phí đơn hàng. Hủy đơn và tạo lại."
                else -> {
                    val err = o.optString("error", raw).ifBlank { raw }
                    if (err.contains("not awaiting payment", ignoreCase = true)) {
                        "Đơn không còn ở bước thanh toán. Mở Đơn hàng để xem trạng thái mới nhất."
                    } else {
                        err
                    }
                }
            }
        } catch (_: Exception) {
            raw
        }
    }

    private fun parseInitiateResponse(raw: String): PaymentInitiateResult {
        val o = when {
            raw.trim().startsWith("{") -> try {
                val obj = JSONObject(raw)
                if (obj.has("data")) obj.getJSONObject("data") else obj
            } catch (_: Exception) {
                JSONObject(raw)
            }
            else -> JSONObject("{}")
        }
        val paymentUrl = o.optString("payment_url", o.optString("paymentUrl", o.optString("PaymentURL", "")))
            .ifBlank { o.optString("checkout_url", "") }
        if (paymentUrl.isBlank()) error("No payment_url in response")
        return PaymentInitiateResult(
            paymentUrl = paymentUrl,
            transactionId = o.optString("transaction_id", o.optString("transactionId", "")).takeIf { it.isNotBlank() },
            expiresAt = o.optString("expires_at", o.optString("expiresAt", "")).takeIf { it.isNotBlank() },
        )
    }

    private fun parsePaymentMethodsResponse(raw: String): List<PaymentGatewayOption> {
        val root = JSONObject(raw.trim().ifBlank { "{}" })
        val arr = when {
            root.has("payment_methods") -> root.getJSONArray("payment_methods")
            root.has("data") && root.getJSONObject("data").has("payment_methods") ->
                root.getJSONObject("data").getJSONArray("payment_methods")
            else -> JSONArray()
        }
        val out = mutableListOf<PaymentGatewayOption>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (!o.optBoolean("enabled", true)) continue
            val id = o.optString("id", o.optString("ID", "")).trim().lowercase()
            if (id.isEmpty()) continue
            val name = o.optString("name", o.optString("Name", id)).trim().ifBlank { id }
            out.add(PaymentGatewayOption(id, name))
        }
        return out
    }

    private fun parseStatusResponse(raw: String): PaymentStatusResult {
        val o = when {
            raw.trim().startsWith("{") -> try {
                val obj = JSONObject(raw)
                if (obj.has("data")) obj.getJSONObject("data") else obj
            } catch (_: Exception) {
                JSONObject(raw)
            }
            else -> JSONObject("{}")
        }
        return PaymentStatusResult(
            escrowStatus = o.optString("escrow_status", o.optString("status", "")),
            paidAt = o.optString("paid_at", o.optString("paidAt", "")),
            amountVnd = o.optLong("amount_vnd", o.optLong("AmountVND", 0L)),
        )
    }
}

data class PaymentGatewayOption(
    val id: String,
    val name: String,
)

data class PaymentInitiateResult(
    val paymentUrl: String,
    val transactionId: String?,
    val expiresAt: String?,
)

data class PaymentStatusResult(
    val escrowStatus: String,
    val paidAt: String,
    val amountVnd: Long,
)
