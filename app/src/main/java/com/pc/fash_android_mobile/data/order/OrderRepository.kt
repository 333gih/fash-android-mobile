package com.pc.fash_android_mobile.data.order

import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.listing.ListingImageUrlsWire
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
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
    fun getBuyingOrders(limit: Int = 30, offset: Int = 0, status: String? = null): Result<List<OrderItem>> =
        getOrdersByRole("buyer", limit, offset, status)

    /**
     * Fetches orders where current user is the seller.
     * Tries `GET /orders?role=seller`, falls back to `/orders/selling`.
     */
    fun getSellingOrders(limit: Int = 30, offset: Int = 0, status: String? = null): Result<List<OrderItem>> =
        getOrdersByRole("seller", limit, offset, status)

    private fun getOrdersByRole(role: String, limit: Int, offset: Int, status: String? = null): Result<List<OrderItem>> = runCatching {
        val q = StringBuilder("role=$role&limit=$limit&offset=$offset")
        status?.trim()?.takeIf { it.isNotEmpty() }?.let {
            q.append("&status=").append(URLEncoder.encode(it, Charsets.UTF_8))
        }
        val primary = "${AppEnvironment.apiPath("api/v1/orders")}?$q"
        try {
            parseOrders(executeGet(primary))
        } catch (_: Exception) {
            val legacyPath = if (role == "buyer") "api/v1/orders/buying" else "api/v1/orders/selling"
            val legacy = "${AppEnvironment.apiPath(legacyPath)}?limit=$limit&offset=$offset"
            parseOrders(executeGet(legacy))
        }
    }

    /** `GET /orders/pending-payment` — buyer orders awaiting payment (auth). */
    fun getPendingPaymentOrders(): Result<PendingPaymentApiResponse> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/orders/pending-payment")
        parsePendingPaymentResponse(executeGet(url))
    }

    /** `GET /orders/{order_id}` — list card shape. */
    fun getOrder(orderId: String): Result<OrderItem> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/orders/${orderId.trim()}")
        parseOrder(executeGet(url))
    }

    /** `GET /orders/{order_id}` — full detail for order screen. */
    fun getOrderDetail(orderId: String): Result<OrderDetail> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/orders/${orderId.trim()}")
        parseOrderDetail(executeGet(url))
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
        val primary = root.optString("ID", root.optString("id", root.optString("order_id", "")))
        val fallback = o.optString("ID", o.optString("id", o.optString("order_id", "")))
        val id = primary.ifBlank { fallback }
        if (id.isBlank()) error("No order id in response")
        id
    }

    /**
     * Buyer cancels an unpaid order.
     * `POST /orders/{order_id}/cancel` — 403 if not buyer, 404 if missing, 409 + `ORDER_NOT_CANCELLABLE` if not `payment_pending`.
     */
    fun cancelOrder(orderId: String): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/orders/${orderId.trim()}/cancel")
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
                val code = try {
                    JSONObject(b).optString("code", "")
                } catch (_: Exception) {
                    ""
                }
                when (response.code) {
                    403 -> error("FORBIDDEN")
                    404 -> error("NOT_FOUND")
                    409 -> if (code == "ORDER_NOT_CANCELLABLE") {
                        error("ORDER_NOT_CANCELLABLE")
                    } else {
                        val msg = try {
                            JSONObject(b).optString("error", JSONObject(b).optString("message", b))
                        } catch (_: Exception) {
                            b
                        }
                        error(msg.ifBlank { "HTTP 409" })
                    }
                    else -> {
                        val msg = try {
                            JSONObject(b).optString("error", JSONObject(b).optString("message", b))
                        } catch (_: Exception) {
                            b
                        }
                        error(msg.ifBlank { "HTTP ${response.code}" })
                    }
                }
            }
        }
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

    /**
     * Seller confirms in-person / meetup handoff (`payment_held` → `in_transit` for MEETUP fulfilment).
     * `POST /orders/{order_id}/confirm-handoff`
     */
    fun confirmHandoff(orderId: String): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/orders/${orderId.trim()}/confirm-handoff")
        executePostEmptyBody(url)
    }

    /** Seller: optional timestamp for cash received (`POST /orders/:id/acknowledge-offline-cash`). */
    fun acknowledgeOfflineCash(orderId: String): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/orders/${orderId.trim()}/acknowledge-offline-cash")
        executePostEmptyBody(url)
    }

    /**
     * After meet time: report no-show / mutual cancel (`POST /orders/:id/report-meeting-no-show`).
     * [reason]: `other_absent`, `other_late`, or `mutual_cancel`.
     */
    fun reportMeetingNoShow(
        orderId: String,
        reason: String,
        photoUrls: List<String> = emptyList(),
        note: String = "",
    ): Result<Unit> = runCatching {
        val json = JSONObject().put("reason", reason.trim())
        val n = note.trim()
        if (n.isNotEmpty()) json.put("note", n)
        if (photoUrls.isNotEmpty()) {
            val arr = JSONArray()
            photoUrls.take(10).forEach { u ->
                val t = u.trim()
                if (t.isNotEmpty()) arr.put(t)
            }
            if (arr.length() > 0) json.put("photo_urls", arr)
        }
        executePostJson(
            AppEnvironment.apiPath("api/v1/orders/${orderId.trim()}/report-meeting-no-show"),
            json.toString(),
        )
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

    /**
     * `POST /orders/dispute` — buyer or seller opens a dispute (order must be `in_transit` or `delivered_confirmed`).
     */
    fun openDispute(orderId: String, description: String, photoUrls: List<String> = emptyList()): Result<Unit> =
        runCatching {
            val url = AppEnvironment.apiPath("api/v1/orders/dispute")
            val desc = description.trim().take(2000)
            if (desc.isEmpty()) error("description required")
            val arr = JSONArray()
            photoUrls.take(10).forEach { u ->
                val t = u.trim()
                if (t.isNotEmpty()) arr.put(t)
            }
            val json = JSONObject()
                .put("order_id", orderId.trim())
                .put("description", desc)
                .put("photo_urls", arr)
            executePostJson(url, json.toString())
        }

    /**
     * `POST /orders/dispute/evidence` — update buyer or seller evidence after dispute is open.
     */
    fun submitDisputeEvidence(
        orderId: String,
        description: String,
        photoUrls: List<String> = emptyList(),
    ): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/orders/dispute/evidence")
        val desc = description.trim().take(2000)
        if (desc.isEmpty()) error("description required")
        val arr = JSONArray()
        photoUrls.take(10).forEach { u ->
            val t = u.trim()
            if (t.isNotEmpty()) arr.put(t)
        }
        val json = JSONObject()
            .put("order_id", orderId.trim())
            .put("description", desc)
            .put("photo_urls", arr)
        executePostJson(url, json.toString())
    }

    private fun executePostEmptyBody(url: String) {
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
                val msg = try {
                    JSONObject(b).optString("error", JSONObject(b).optString("message", b)).ifBlank { b }
                } catch (_: Exception) {
                    b
                }
                error("HTTP ${response.code}: $msg")
            }
        }
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

    private fun parseOrderDetail(json: String): OrderDetail {
        val raw = json.trim()
        val top = when {
            raw.startsWith("{") -> try {
                JSONObject(raw)
            } catch (_: Exception) {
                JSONObject("{}")
            }
            else -> JSONObject("{}")
        }
        val o: JSONObject = unwrapOrderJsonObject(top)
        val listing = o.optJSONObject("Listing") ?: o.optJSONObject("listing") ?: JSONObject()
        val buyer = o.optJSONObject("Buyer") ?: o.optJSONObject("buyer") ?: JSONObject()
        val seller = o.optJSONObject("Seller") ?: o.optJSONObject("seller") ?: JSONObject()
        val imageUrlsArr = listing.optJSONArray("ImageURLs") ?: listing.optJSONArray("image_urls")
        val coverUrl = ListingImageUrlsWire.resolveCoverUrl(
            listing.optString("CoverImageURL", "")
                .ifBlank { listing.optString("cover_image_url", "") },
            imageUrlsArr,
        )
        val rawStatus = o.optString("status", o.optString("Status", "payment_pending")).lowercase()
        val canConfirm = o.optBoolean("can_confirm", rawStatus == "in_transit")
        val buyerReview = parseBuyerReview(o)
        val apiCanReviewExplicit = when {
            o.has("can_review") -> o.optBoolean("can_review", false)
            o.has("CanReview") -> o.optBoolean("CanReview", false)
            else -> null
        }
        val defaultReviewEligible = rawStatus == "delivered_confirmed"
        val canReview = when {
            buyerReview != null -> false
            apiCanReviewExplicit != null -> apiCanReviewExplicit
            else -> defaultReviewEligible
        }
        val trackingNumber = o.optString("tracking_number", o.optString("TrackingNumber", ""))
        val shippingFee = o.optLong("shipping_fee_vnd", o.optLong("ShippingFeeVND", o.optLong("shipping_fee", 0L)))
        val discountVnd = o.optLong("discount_vnd", o.optLong("DiscountVND", o.optLong("discount_amount_vnd", 0L)))
        val buyerTotal = o.optLong("buyer_total_vnd", o.optLong("BuyerTotalVND", o.optLong("total_vnd", 0L)))
        val shipAddr = o.optJSONObject("shipping_address")
            ?: o.optJSONObject("ShippingAddress")
            ?: o.optJSONObject("delivery_address")
            ?: JSONObject()
        val shippingFormatted = buildShippingAddressString(shipAddr, o)
        val recipientName = shipAddr.optString("recipient_name", shipAddr.optString("name", shipAddr.optString("RecipientName", "")))
            .ifBlank { o.optString("recipient_name", o.optString("RecipientName", "")) }
        val recipientPhone = shipAddr.optString("phone", shipAddr.optString("Phone", ""))
            .ifBlank { o.optString("recipient_phone", o.optString("RecipientPhone", "")) }
        val trackingEmpty = trackingNumber.isBlank()
        val canShip = o.optBoolean("can_ship", o.optBoolean("CanShip", false)) ||
            (rawStatus == "payment_held" && trackingEmpty)
        val variantLabel = buildListingVariantLabel(listing)
        val convId = o.optString("conversation_id", o.optString("conversationId", o.optString("ConversationID", "")))
        val trackingSummary = o.optString("tracking_status", o.optString("TrackingStatus", o.optString("last_tracking_event", "")))
        val meetingAppointment = parseOrderMeetingAppointment(o)
        val meetingGrace = parseOrderMeetingGrace(o)
        val meetupDeadlineAt = o.optIsoFirst("meetup_deadline_at", "MeetupDeadlineAt")
        val canConfirmHandoff = when {
            o.has("can_confirm_handoff") -> o.optBoolean("can_confirm_handoff", false)
            o.has("CanConfirmHandoff") -> o.optBoolean("CanConfirmHandoff", false)
            o.has("canConfirmHandoff") -> o.optBoolean("canConfirmHandoff", false)
            else -> false
        }
        val canAcknowledgeOfflineCash = o.optBoolean(
            "can_acknowledge_offline_cash",
            o.optBoolean("CanAcknowledgeOfflineCash", false),
        )
        return OrderDetail(
            orderId = o.optString("id", o.optString("ID", o.optString("order_id", ""))),
            listingId = o.optString("listing_id", o.optString("ListingID", listing.optString("ID", listing.optString("id", "")))),
            buyerUserId = o.optString("buyer_id", o.optString("BuyerID", "")).ifBlank {
                buyer.optString("UserID", buyer.optString("user_id", buyer.optString("ID", buyer.optString("id", ""))))
            },
            sellerUserId = o.optString("seller_id", o.optString("SellerID", "")).ifBlank {
                seller.optString("UserID", seller.optString("user_id", seller.optString("ID", seller.optString("id", ""))))
            },
            amountVnd = o.optLong("amount_vnd", o.optLong("AmountVND", 0L)),
            platformFeeVnd = o.optLong("platform_fee_vnd", o.optLong("PlatformFeeVND", 0L)),
            sellerPayoutVnd = o.optLong("seller_payout_vnd", o.optLong("SellerPayoutVND", 0L)),
            status = rawStatus,
            trackingNumber = trackingNumber,
            carrier = o.optString("carrier", o.optString("Carrier", "")),
            listingTitle = listing.optString("Title", listing.optString("title", "")),
            listingImageUrl = coverUrl,
            listingPriceVnd = listing.optLong("Price", listing.optLong("price", 0L)),
            listingStatus = listing.optString("Status", listing.optString("status", "active")).lowercase().ifBlank { "active" },
            buyerUsername = buyer.optString("Username", buyer.optString("username", "")),
            buyerDisplayName = buyer.optString("DisplayName", buyer.optString("display_name", "")),
            buyerAvatarUrl = buyer.optString("AvatarURL", buyer.optString("avatar_url", "")),
            sellerUsername = seller.optString("Username", seller.optString("username", "")),
            sellerDisplayName = seller.optString("DisplayName", seller.optString("display_name", "")),
            sellerAvatarUrl = seller.optString("AvatarURL", seller.optString("avatar_url", "")),
            canConfirm = canConfirm,
            canReview = canReview,
            buyerReview = buyerReview,
            shippingFeeVnd = shippingFee,
            discountVnd = discountVnd,
            buyerTotalVnd = buyerTotal,
            recipientName = recipientName,
            recipientPhone = recipientPhone,
            shippingAddressFormatted = shippingFormatted,
            createdAt = o.optIsoFirst("created_at", "CreatedAt", "createdAt"),
            paidAt = o.optIsoFirst("paid_at", "PaidAt", "payment_completed_at"),
            shippedAt = o.optIsoFirst("shipped_at", "ShippedAt", "ship_date"),
            deliveredAt = o.optIsoFirst("delivered_at", "DeliveredAt", "buyer_confirmed_at"),
            cancelledAt = o.optIsoFirst("cancelled_at", "CancelledAt", "canceled_at"),
            expectedDeliveryAt = o.optIsoFirst("expected_delivery_at", "ExpectedDelivery", "estimated_delivery"),
            shipByAt = o.optIsoFirst("ship_by", "ship_by_at", "ShipBy", "must_ship_before"),
            escrowReleaseAt = o.optIsoFirst("escrow_release_at", "EscrowReleaseAt", "auto_release_at"),
            disputeSummary = o.optString("dispute_summary", o.optString("dispute_reason", o.optString("DisputeSummary", ""))),
            listingVariantLabel = variantLabel,
            conversationId = convId,
            trackingStatusSummary = trackingSummary,
            canShip = canShip,
            meetingAppointment = meetingAppointment,
            meetingGrace = meetingGrace,
            meetupDeadlineAt = meetupDeadlineAt,
            canConfirmHandoff = canConfirmHandoff,
            canAcknowledgeOfflineCash = canAcknowledgeOfflineCash,
        )
    }

    private fun parseBuyerReview(order: JSONObject): OrderBuyerReview? {
        val br = order.optJSONObject("buyer_review")
            ?: order.optJSONObject("BuyerReview")
            ?: order.optJSONObject("buyerReview")
            ?: return null
        val id = br.optString("id", br.optString("ID", "")).trim()
        val rating = br.optInt("rating", br.optInt("Rating", 0))
        val comment = br.optString("comment", br.optString("Comment", "")).trim()
        val createdAt = br.optIsoFirst("created_at", "CreatedAt", "createdAt")
        if (id.isEmpty() && rating <= 0) return null
        return OrderBuyerReview(
            id = id,
            rating = rating.coerceIn(0, 5),
            comment = comment,
            createdAt = createdAt,
        )
    }

    /** `{ "ok": true, "order": { ... } }`, `{ "data": { "order": ... } }`, or flat order object. */
    private fun unwrapOrderJsonObject(top: JSONObject): JSONObject {
        val data = if (top.has("data") && top.get("data") is JSONObject) top.getJSONObject("data") else null
        fun orderFrom(obj: JSONObject?): JSONObject? {
            if (obj == null) return null
            when {
                obj.has("order") && obj.get("order") is JSONObject -> return obj.getJSONObject("order")
                obj.has("Order") && obj.get("Order") is JSONObject -> return obj.getJSONObject("Order")
            }
            return null
        }
        orderFrom(data)?.let { return it }
        orderFrom(top)?.let { return it }
        if (data != null) return data
        return top
    }

    private fun parseOrderMeetingGrace(order: JSONObject): OrderMeetingGrace? {
        val g = order.optJSONObject("meeting_grace") ?: order.optJSONObject("MeetingGrace") ?: return null
        // `{}` is valid — do not drop; otherwise UI removes the whole meetup grace card after check-in refresh.
        return OrderMeetingGrace(
            canCheckIn = g.optBoolean("can_check_in", g.optBoolean("CanCheckIn", false)),
            canReportNoShow = g.optBoolean("can_report_no_show", g.optBoolean("CanReportNoShow", false)),
            sosUnlocked = g.optBoolean("sos_unlocked", g.optBoolean("SosUnlocked", false)),
            checkInHint = g.optString("check_in_hint", g.optString("CheckInHint", "")).trim(),
            noShowHint = g.optString("no_show_hint", g.optString("NoShowHint", "")).trim(),
            buyerCheckedInAt = g.optIsoGrace("buyer_checked_in_at", "BuyerCheckedInAt"),
            sellerCheckedInAt = g.optIsoGrace("seller_checked_in_at", "SellerCheckedInAt"),
            phase = g.optString("phase", g.optString("Phase", "")).trim().lowercase(),
        )
    }

    private fun JSONObject.optIsoGrace(vararg keys: String): String {
        for (k in keys) {
            val v = optString(k, "").trim()
            if (v.isNotBlank()) return v
        }
        return ""
    }

    private fun parseOrderMeetingAppointment(order: JSONObject): OrderMeetingAppointment? {
        val m = order.optJSONObject("meeting_appointment")
            ?: order.optJSONObject("MeetingAppointment")
            ?: return null
        val id = m.optString("id", m.optString("ID", "")).trim()
        if (id.isEmpty()) return null
        return OrderMeetingAppointment(
            id = id,
            status = m.optString("status", m.optString("Status", "")).trim().lowercase().ifBlank { "pending" },
            locationUrl = m.optString("location_url", m.optString("LocationURL", "")),
            scheduledAt = m.optString("scheduled_at", m.optString("ScheduledAt", "")),
            reminderOffsetMinutes = m.optInt("reminder_offset_minutes", m.optInt("ReminderOffsetMinutes", 60)),
            reminderEnabled = m.optBoolean("reminder_enabled", m.optBoolean("ReminderEnabled", true)),
            reminderSentAt = meetingOptIso(m, "reminder_sent_at", "ReminderSentAt"),
            createdAt = meetingOptIso(m, "created_at", "CreatedAt"),
            updatedAt = meetingOptIso(m, "updated_at", "UpdatedAt"),
            buyerCheckInAt = meetingOptIso(m, "buyer_check_in_at", "BuyerCheckInAt"),
            sellerCheckInAt = meetingOptIso(m, "seller_check_in_at", "SellerCheckInAt"),
        )
    }

    private fun meetingOptIso(m: JSONObject, vararg keys: String): String {
        for (k in keys) {
            val v = m.optString(k, "").trim()
            if (v.isNotBlank()) return v
        }
        return ""
    }

    private fun JSONObject.optIsoFirst(vararg keys: String): String {
        for (k in keys) {
            val v = optString(k, "").trim()
            if (v.isNotBlank()) return v
        }
        return ""
    }

    private fun buildShippingAddressString(ship: JSONObject, order: JSONObject): String {
        val direct = ship.optString("formatted", ship.optString("full_address", ship.optString("address", ""))).trim()
        if (direct.isNotBlank()) return direct
        val line1 = ship.optString("line1", ship.optString("address_line_1", "")).trim()
        val line2 = ship.optString("line2", ship.optString("address_line_2", "")).trim()
        val ward = ship.optString("ward", "").trim()
        val district = ship.optString("district", ship.optString("county", "")).trim()
        val city = ship.optString("city", ship.optString("province", "")).trim()
        val parts = listOf(line1, line2, ward, district, city).filter { it.isNotBlank() }
        if (parts.isNotEmpty()) return parts.joinToString(", ")
        return order.optString("shipping_address_text", order.optString("delivery_address", "")).trim()
    }

    private fun buildListingVariantLabel(listing: JSONObject): String {
        val direct = listing.optString("variant_label", listing.optString("VariantLabel", listing.optString("sku", ""))).trim()
        if (direct.isNotBlank()) return direct
        val attrs = listing.optJSONArray("attributes") ?: listing.optJSONArray("Attributes") ?: return ""
        val bits = mutableListOf<String>()
        for (i in 0 until attrs.length()) {
            val a = attrs.optJSONObject(i) ?: continue
            val name = a.optString("name", a.optString("key", "")).trim()
            val value = a.optString("value", "").trim()
            if (value.isNotBlank()) bits.add(if (name.isNotBlank()) "$name: $value" else value)
        }
        return bits.joinToString(" · ").trim()
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

    private fun parsePendingPaymentResponse(json: String): PendingPaymentApiResponse {
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
        val minutes = o.optInt("payment_window_minutes", o.optInt("PaymentWindowMinutes", 15))
        val arr = o.optJSONArray("orders") ?: o.optJSONArray("Orders") ?: JSONArray()
        val orders = (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { parsePendingOrderRow(it) }
        }
        return PendingPaymentApiResponse(paymentWindowMinutes = minutes, orders = orders)
    }

    private fun parsePendingOrderRow(o: JSONObject): PendingPaymentOrderDto {
        fun s(vararg keys: String): String {
            for (k in keys) {
                val v = o.optString(k, "").trim()
                if (v.isNotBlank()) return v
            }
            return ""
        }
        return PendingPaymentOrderDto(
            orderId = s("order_id", "OrderID", "id", "ID"),
            listingId = s("listing_id", "ListingID"),
            amountVnd = o.optLong("amount_vnd", o.optLong("AmountVND", 0L)),
            status = s("status", "Status").ifBlank { "payment_pending" },
            createdAt = s("created_at", "CreatedAt"),
            paymentDeadlineAt = s("payment_deadline_at", "PaymentDeadlineAt"),
            remainingSeconds = o.optInt("remaining_seconds", o.optInt("RemainingSeconds", 0)),
            expired = o.optBoolean("expired", o.optBoolean("Expired", false)),
            listingTitle = s("listing_title", "ListingTitle", "title", "Title"),
            coverImageUrl = s("cover_image_url", "CoverImageURL", "image_url", "ImageURL"),
        )
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
        val coverUrl = ListingImageUrlsWire.resolveCoverUrl(
            listing.optString("cover_image_url", "")
                .ifBlank { listing.optString("CoverImageURL", "") },
            imageUrlsArr,
        )
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
            createdAt = o.optIsoFirst("created_at", "CreatedAt", "createdAt"),
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
    /** ISO-8601 from API when present — used for payment countdown. */
    val createdAt: String = "",
)
