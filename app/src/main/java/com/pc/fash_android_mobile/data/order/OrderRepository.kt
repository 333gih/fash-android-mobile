package com.pc.fash_android_mobile.data.order

import com.pc.fash_android_mobile.config.AppEnvironment
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

/**
 * Orders API client (core-service: `GET /orders?role=`, `POST /orders`, etc.).
 */
class OrderRepository(
    private val securedClient: OkHttpClient,
) {

    /**
     * Fetches orders where current user is the buyer.
     * Tries `GET /orders?role=buyer`, falls back to `/orders/buying`.
     */
    fun getBuyingOrders(limit: Int = 30, offset: Int = 0): Result<List<OrderItem>> =
        getOrdersByRole("buyer", limit, offset)

    /**
     * Fetches orders where current user is the seller.
     * Tries `GET /orders?role=seller`, falls back to `/orders/selling`.
     */
    fun getSellingOrders(limit: Int = 30, offset: Int = 0): Result<List<OrderItem>> =
        getOrdersByRole("seller", limit, offset)

    private fun getOrdersByRole(role: String, limit: Int, offset: Int): Result<List<OrderItem>> = runCatching {
        val primary = "${AppEnvironment.apiPath("api/v1/orders")}?role=$role&limit=$limit&offset=$offset"
        try {
            parseOrders(executeGet(primary))
        } catch (_: Exception) {
            val legacyPath = if (role == "buyer") "api/v1/orders/buying" else "api/v1/orders/selling"
            val legacy = "${AppEnvironment.apiPath(legacyPath)}?limit=$limit&offset=$offset"
            parseOrders(executeGet(legacy))
        }
    }

    /** `GET /orders/{order_id}` */
    fun getOrder(orderId: String): Result<OrderItem> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/orders/${orderId.trim()}")
        parseOrder(executeGet(url))
    }

    /**
     * `POST /orders` — checkout. Returns new order id.
     */
    fun createOrder(listingId: String, amountVnd: Long): Result<String> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/orders")
        val json = JSONObject()
            .put("listing_id", listingId.trim())
            .put("amount_vnd", amountVnd)
            .toString()
        val body = executePostJson(url, json)
        val o = JSONObject(body.trim())
        val root = if (o.has("data")) o.getJSONObject("data") else o
        // Handle both snake_case and PascalCase responses from the backend
        root.optString("ID", root.optString("id", root.optString("order_id", ""))).ifBlank {
            o.optString("ID", o.optString("id", o.optString("order_id", "")))
        }.ifBlank { error("No order id in response") }
    }

    /**
     * Confirms that the buyer has received the order.
     * `POST /orders/{order_id}/confirm`
     */
    fun confirmReceipt(orderId: String): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/orders/${orderId.trim()}/confirm")
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

    /** `POST /orders/ship` */
    fun shipOrder(orderId: String, trackingNumber: String, carrier: String): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/orders/ship")
        val json = JSONObject()
            .put("order_id", orderId.trim())
            .put("tracking_number", trackingNumber.trim())
            .put("carrier", carrier.trim())
            .toString()
        executePostJson(url, json)
    }

    /** `POST /orders/review` */
    fun submitReview(orderId: String, rating: Int, comment: String? = null): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/orders/review")
        val json = JSONObject()
            .put("order_id", orderId.trim())
            .put("rating", rating)
        comment?.takeIf { it.isNotBlank() }?.let { json.put("comment", it) }
        executePostJson(url, json.toString())
    }

    private fun executePostJson(url: String, json: String): String {
        return securedClient.newCall(
            Request.Builder()
                .url(url)
                .post(json.toRequestBody(JSON_MEDIA))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "FashAndroid/1.0")
                .build(),
        ).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val msg = try { JSONObject(body).optString("error", body).ifBlank { body } } catch (_: Exception) { body }
                error("HTTP ${response.code}: $msg")
            }
            body.ifBlank { "{}" }
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

    private fun parseOrder(json: String): OrderItem {
        val raw = json.trim()
        val o = when {
            raw.startsWith("{") -> try {
                val obj = JSONObject(raw)
                if (obj.has("data")) obj.getJSONObject("data") else obj
            } catch (_: Exception) {
                JSONObject(raw)
            }
            else -> JSONObject("{}")
        }
        return mapOrderJson(o)
    }

    private fun parseOrders(json: String): List<OrderItem> {
        val raw = json.trim()
        val arr = when {
            raw.startsWith("[") -> JSONArray(raw)
            else -> try {
                val obj = JSONObject(raw)
                if (obj.has("data")) obj.getJSONArray("data") else JSONArray("[]")
            } catch (_: Exception) {
                JSONArray("[]")
            }
        }
        return (0 until arr.length()).map { i -> mapOrderJson(arr.getJSONObject(i)) }
    }

    private fun mapOrderJson(o: JSONObject): OrderItem {
        val listing = o.optJSONObject("listing") ?: o.optJSONObject("Listing")
            ?: o.optJSONObject("product") ?: o.optJSONObject("Product") ?: o
        val seller = o.optJSONObject("seller") ?: o.optJSONObject("Seller")
            ?: o.optJSONObject("buyer") ?: o.optJSONObject("Buyer") ?: o
        val rawStatus = o.optString("status", o.optString("Status", "payment_pending")).lowercase()
        val imageUrlsArr = listing.optJSONArray("image_urls") ?: listing.optJSONArray("ImageURLs")
        val coverUrl = listing.optString("cover_image_url", "")
            .ifBlank { listing.optString("CoverImageURL", "") }
            .ifBlank { imageUrlsArr?.optString(0) ?: "" }
        return OrderItem(
            orderId = o.optString("id", o.optString("ID", o.optString("order_id", ""))),
            listingId = o.optString("listing_id", o.optString("ListingID", listing.optString("id", listing.optString("ID", "")))),
            title = listing.optString("title", listing.optString("Title", o.optString("title", o.optString("Title", "")))),
            imageUrl = coverUrl,
            sellerUsername = seller.optString("username", seller.optString("Username", o.optString("seller_username", ""))),
            priceVnd = o.optLong("amount_vnd", o.optLong("AmountVND", listing.optLong("price", listing.optLong("Price", 0L)))),
            status = rawStatus,
            canConfirm = o.optBoolean("can_confirm", rawStatus == "in_transit"),
            canReview = o.optBoolean("can_review", rawStatus == "delivered_confirmed"),
        )
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
