package com.pc.fash_android_mobile.data.order

import com.pc.fash_android_mobile.config.AppEnvironment
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Orders API client.
 */
class OrderRepository(
    private val securedClient: OkHttpClient,
) {

    /**
     * Fetches orders where current user is the buyer.
     */
    fun getBuyingOrders(limit: Int = 30, offset: Int = 0): Result<List<OrderItem>> = runCatching {
        val url = "${AppEnvironment.apiPath("api/v1/orders/buying")}?limit=$limit&offset=$offset"
        parseOrders(executeGet(url))
    }

    /**
     * Fetches orders where current user is the seller.
     */
    fun getSellingOrders(limit: Int = 30, offset: Int = 0): Result<List<OrderItem>> = runCatching {
        val url = "${AppEnvironment.apiPath("api/v1/orders/selling")}?limit=$limit&offset=$offset"
        parseOrders(executeGet(url))
    }

    /**
     * Confirms that the buyer has received the order.
     */
    fun confirmReceipt(orderId: String): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/orders/$orderId/confirm")
        securedClient.newCall(
            Request.Builder()
                .url(url)
                .post(ByteArray(0).toRequestBody(null))
                .header("Accept", "application/json")
                .header("User-Agent", "FashAndroid/1.0")
                .build(),
        ).execute().use { response ->
            if (!response.isSuccessful) {
                val b = response.body?.string().orEmpty()
                val msg = try { JSONObject(b).optString("error", b).ifBlank { b } } catch (_: Exception) { b }
                error("HTTP ${response.code}: $msg")
            }
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
            if (!response.isSuccessful) {
                val b = response.body?.string().orEmpty()
                val msg = try { JSONObject(b).optString("error", b).ifBlank { b } } catch (_: Exception) { b }
                error("HTTP ${response.code}: $msg")
            }
            response.body?.string().orEmpty()
        }
    }

    private fun parseOrders(json: String): List<OrderItem> {
        val raw = json.trim()
        val arr = when {
            raw.startsWith("[") -> JSONArray(raw)
            else -> try {
                val obj = JSONObject(raw)
                if (obj.has("data")) obj.getJSONArray("data") else JSONArray("[]")
            } catch (_: Exception) { JSONArray("[]") }
        }
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val listing = o.optJSONObject("listing") ?: o.optJSONObject("product") ?: o
            val seller = o.optJSONObject("seller") ?: o
            OrderItem(
                orderId = o.optString("order_id", o.optString("id", "")),
                listingId = listing.optString("listing_id", listing.optString("id", "")),
                title = listing.optString("title", listing.optString("Title", o.optString("title", ""))),
                imageUrl = listing.optString("image_url", listing.optString("cover_image_url", listing.optJSONArray("image_urls")?.optString(0) ?: "")),
                sellerUsername = seller.optString("username", o.optString("seller_username", "")),
                priceVnd = o.optLong("price_vnd", listing.optLong("price", listing.optLong("priceVnd", 0L))),
                status = o.optString("status", "pending").lowercase(),
                canConfirm = o.optBoolean("can_confirm", o.optString("status", "").lowercase() == "delivering" || o.optString("status", "").lowercase() == "shipped"),
                canReview = o.optBoolean("can_review", o.optString("status", "").lowercase() == "completed"),
            )
        }
    }
}

data class OrderItem(
    val orderId: String,
    val listingId: String,
    val title: String,
    val imageUrl: String,
    val sellerUsername: String,
    val priceVnd: Long,
    val status: String,
    val canConfirm: Boolean,
    val canReview: Boolean,
)
