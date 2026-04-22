package com.pc.fash_android_mobile.data.chat

import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.auth.AuthSessionStore
import com.pc.fash_android_mobile.data.listing.ListingImageUrlsWire
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

/**
 * Builds a throwable message that includes JSON `code` when present so UI can map
 * `CONVERSATION_ORDER_EXISTS`, `CONVERSATION_CLOSED`, `PENDING_OFFER_EXISTS`, `OFFER_LIMIT_REACHED`, etc.
 */
private fun buildHttpErrorMessage(responseCode: Int, body: String): String {
    val trimmed = body.trim()
    val code = try {
        JSONObject(trimmed).optString("code", "").trim()
    } catch (_: Exception) {
        ""
    }
    val msg = try {
        val o = JSONObject(trimmed)
        o.optString("error", o.optString("message", trimmed)).ifBlank { trimmed }
    } catch (_: Exception) {
        trimmed
    }
    return if (code.isNotBlank()) {
        "HTTP $responseCode: $msg code=$code"
    } else {
        "HTTP $responseCode: $msg"
    }
}

/**
 * Chat/conversations API client.
 *
 * Endpoints match core-service under [com.pc.fash_android_mobile.config.AppEnvironment.apiPath]:
 * `POST api/v1/chat/conversations` (listing_id), `GET api/v1/chat/conversations/{id}`,
 * `POST api/v1/chat/offers`, `POST api/v1/chat/offers/accept|accept-in-chat|decline`,
 * `POST api/v1/chat/meetings/propose|.../confirm|.../cancel|.../check-in`, etc.
 * Optional locale segment (`{vi|en}`) is applied when `CORE_API_USE_LANGUAGE_PREFIX` is true in env.
 *
 * Parses PascalCase responses from the backend:
 *   Conversation: ID, BuyerID, SellerID, LastMessage (string), LastMessageAt, Listing, Buyer, Seller
 *   Profile:      UserID, Username, DisplayName, AvatarURL
 *   Listing:      ID, Title, CoverImageURL, Price
 *   Message:      ID, Content, SenderID, CreatedAt, IsRead
 */
class ChatRepository(
    private val securedClient: OkHttpClient,
    private val sessionStore: AuthSessionStore,
) {

    private val currentUserId: String
        get() = sessionStore.read()?.userId.orEmpty()

    // ── Conversations list ────────────────────────────────────────────────

    /** API: GET /chat/conversations?limit=&offset= (& group_by=listing for seller grouped inbox). */
    fun getConversations(
        limit: Int = 50,
        offset: Int = 0,
    ): Result<List<ConversationItem>> = runCatching {
        val url = "${AppEnvironment.apiPath("api/v1/chat/conversations")}?limit=$limit&offset=$offset"
        val body = executeConversationsGet(url)
        parseConversations(body)
    }

    /** GET /chat/conversations?group_by=listing — grouped rows for seller inbox. */
    fun getConversationsGroupedByListing(
        limit: Int = 50,
        offset: Int = 0,
    ): Result<List<ConversationListingGroup>> = runCatching {
        val base = AppEnvironment.apiPath("api/v1/chat/conversations")
        val url = "$base?limit=$limit&offset=$offset&group_by=listing"
        val body = executeConversationsGet(url)
        parseConversationGroups(body)
    }

    private fun executeConversationsGet(url: String): String {
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
                error(buildHttpErrorMessage(response.code, b))
            }
            response.body?.string().orEmpty()
        }
    }

    // ── Start / create conversation ───────────────────────────────────────

    /** API: POST /chat/conversations — body: { listing_id }. Returns 200 (existing) or 201 (new). */
    fun startConversation(listingId: String): Result<String> = runCatching {
        if (listingId.isBlank()) error("listing_id is required")
        val json = JSONObject().put("listing_id", listingId).toString()
        val body = postJson(AppEnvironment.apiPath("api/v1/chat/conversations"), json)
        extractConversationIdFromStartResponse(body)
    }

    /**
     * Backend may return the conversation id at `id`, under `data`, or nested as `data.conversation.id`.
     */
    private fun extractConversationIdFromStartResponse(body: String): String {
        val raw = body.trim()
        if (raw.isEmpty()) error("Empty response body")
        val root = JSONObject(raw)
        val payload: JSONObject = when {
            !root.has("data") -> root
            else -> when (val d = root.get("data")) {
                is JSONObject -> d
                is String -> return d.trim().takeIf { it.isNotBlank() }
                    ?: error("No conversation id in response")
                else -> error("Unexpected data in response")
            }
        }
        fun idFrom(o: JSONObject): String {
            val direct = o.optString("ID", o.optString("id", o.optString("conversation_id", "")))
            if (direct.isNotBlank()) return direct
            val nested = o.optJSONObject("conversation") ?: o.optJSONObject("Conversation")
            if (nested != null) {
                val inner = nested.optString(
                    "ID",
                    nested.optString("id", nested.optString("conversation_id", "")),
                )
                if (inner.isNotBlank()) return inner
            }
            return ""
        }
        val id = idFrom(payload)
        if (id.isNotBlank()) return id
        error("No conversation id in response")
    }

    // ── Conversation detail ───────────────────────────────────────────────

    fun getConversationDetail(conversationId: String): Result<ConversationDetail> = runCatching {
        fun doGet(url: String): String {
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
                    error(buildHttpErrorMessage(response.code, b))
                }
                response.body?.string().orEmpty()
            }
        }
        val body = doGet(AppEnvironment.apiPath("api/v1/chat/conversations/$conversationId"))
        parseConversationDetail(body)
    }

    // ── Messages ──────────────────────────────────────────────────────────

    fun getMessages(
        conversationId: String,
        limit: Int = 50,
        offset: Int = 0,
    ): Result<List<ChatMessage>> = runCatching {
        val url = "${AppEnvironment.apiPath("api/v1/chat/conversations/$conversationId/messages")}?limit=$limit&offset=$offset"
        val body = securedClient.newCall(
            Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .header("User-Agent", "FashAndroid/1.0")
                .build(),
        ).execute().use { response ->
            if (!response.isSuccessful) {
                val b = response.body?.string().orEmpty()
                error(buildHttpErrorMessage(response.code, b))
            }
            response.body?.string().orEmpty()
        }
        parseMessagesArray(body)
    }

    fun sendMessage(conversationId: String, text: String): Result<ChatMessage> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/chat/messages")
        val json = JSONObject()
            .put("conversation_id", conversationId)
            .put("content", text)
            .toString()
        val body = securedClient.newCall(
            Request.Builder()
                .url(url)
                .post(json.toRequestBody(JSON_MEDIA))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "FashAndroid/1.0")
                .build(),
        ).execute().use { response ->
            if (!response.isSuccessful) {
                val b = response.body?.string().orEmpty()
                error(buildHttpErrorMessage(response.code, b))
            }
            response.body?.string().orEmpty()
        }
        parseMessage(body)
    }

    // ── Messages ──────────────────────────────────────────────────────────

    /** API: DELETE /chat/messages/{message_id} — only within 5 minutes of sending. */
    fun deleteMessage(messageId: String): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/chat/messages/$messageId")
        securedClient.newCall(
            Request.Builder()
                .url(url)
                .delete()
                .header("Accept", "application/json")
                .header("User-Agent", "FashAndroid/1.0")
                .build(),
        ).execute().use { response ->
            if (!response.isSuccessful) {
                val b = response.body?.string().orEmpty()
                error(buildHttpErrorMessage(response.code, b))
            }
        }
    }

    // ── Offers ────────────────────────────────────────────────────────────

    /** API: POST /chat/offers — body: { conversation_id, amount_vnd }. No listing_id needed. */
    fun createOffer(conversationId: String, amountVnd: Long): Result<PriceOffer> = runCatching {
        val json = JSONObject()
            .put("conversation_id", conversationId)
            .put("amount_vnd", amountVnd)
            .toString()
        parseOffer(postJson(AppEnvironment.apiPath("api/v1/chat/offers"), json))
    }

    /**
     * API: POST /chat/offers/counter — seller counters a buyer offer.
     * Body: `conversation_id`, `buyer_offer_message_id`, `amount_vnd` (min 1000 on server).
     */
    fun createCounterOffer(
        conversationId: String,
        buyerOfferMessageId: String,
        amountVnd: Long,
    ): Result<ChatMessage> = runCatching {
        val json = JSONObject()
            .put("conversation_id", conversationId.trim())
            .put("buyer_offer_message_id", buyerOfferMessageId.trim())
            .put("amount_vnd", amountVnd)
            .toString()
        val body = postJson(AppEnvironment.apiPath("api/v1/chat/offers/counter"), json)
        parseMessage(body)
    }

    /** API: POST /chat/offers/accept or /decline — body: { conversation_id, offer_message_id }. */
    /**
     * Decline returns a JSON body without an order. Accept may return a created order id under
     * `order_id` / `OrderID` or nested `order.id` — used to open order detail immediately.
     */
    fun respondToOffer(
        conversationId: String,
        offerMessageId: String,
        accept: Boolean,
    ): Result<String?> = runCatching {
        val path = if (accept) "api/v1/chat/offers/accept" else "api/v1/chat/offers/decline"
        val json = JSONObject()
            .put("conversation_id", conversationId)
            .put("offer_message_id", offerMessageId)
            .toString()
        val body = postJson(AppEnvironment.apiPath(path), json)
        if (accept) parseOrderIdFromOfferAcceptResponse(body) else null
    }

    private fun parseOrderIdFromOfferAcceptResponse(body: String): String? {
        val raw = body.trim()
        if (raw.isEmpty()) return null
        return runCatching {
            val root = JSONObject(raw)
            val o: JSONObject = when {
                root.has("data") && !root.isNull("data") -> {
                    val d = root.get("data")
                    if (d is JSONObject) d else root
                }
                else -> root
            }
            val orderObj = o.optJSONObject("order") ?: o.optJSONObject("Order")
            val nestedId = orderObj?.let { ord ->
                ord.optString("id", ord.optString("ID", "")).trim().takeIf { it.isNotEmpty() }
            }
            if (nestedId != null) return@runCatching nestedId
            sequenceOf(
                o.optString("order_id", ""),
                o.optString("OrderID", ""),
                o.optString("orderId", ""),
            ).map { it.trim() }.firstOrNull { it.isNotEmpty() }
        }.getOrNull()
    }

    /**
     * `POST /chat/offers/accept-in-chat` — same body as [respondToOffer] accept; creates order
     * (e.g. cash meetup) per current core-service rules.
     */
    fun acceptOfferInChat(conversationId: String, offerMessageId: String): Result<String?> = runCatching {
        val json = JSONObject()
            .put("conversation_id", conversationId.trim())
            .put("offer_message_id", offerMessageId.trim())
            .toString()
        val body = postJson(AppEnvironment.apiPath("api/v1/chat/offers/accept-in-chat"), json)
        parseOrderIdFromOfferAcceptResponse(body)
    }

    /**
     * Prefer accept-in-chat; on 404 (older servers) fall back to `POST /chat/offers/accept`.
     */
    fun acceptOfferUnified(conversationId: String, offerMessageId: String): Result<String?> {
        val first = acceptOfferInChat(conversationId, offerMessageId)
        if (first.isSuccess) return first
        val err = first.exceptionOrNull()?.message.orEmpty()
        val notFound = err.contains("HTTP 404") || err.contains(" 404 ") ||
            err.contains("code=404", ignoreCase = true) ||
            err.contains("NOT_FOUND", ignoreCase = true)
        return if (notFound) {
            respondToOffer(conversationId, offerMessageId, true)
        } else {
            first
        }
    }

    /** POST /chat/meetings/propose — creates [meeting_proposal] message + appointment. */
    fun proposeMeeting(
        conversationId: String,
        locationUrl: String,
        scheduledAtRfc3339: String,
        reminderEnabled: Boolean,
        reminderOffsetMinutes: Int,
    ): Result<Unit> = runCatching {
        val json = JSONObject()
            .put("conversation_id", conversationId)
            .put("location_url", locationUrl.trim())
            .put("scheduled_at", scheduledAtRfc3339.trim())
            .put("reminder_enabled", reminderEnabled)
            .put("reminder_offset_minutes", reminderOffsetMinutes)
            .toString()
        postJson(AppEnvironment.apiPath("api/v1/chat/meetings/propose"), json)
    }

    fun confirmMeeting(appointmentId: String): Result<Unit> = runCatching {
        val id = appointmentId.trim()
        if (id.isEmpty()) error("appointment id required")
        postJson(AppEnvironment.apiPath("api/v1/chat/meetings/$id/confirm"), "{}")
    }

    fun cancelMeeting(appointmentId: String): Result<MeetingCancelResult> = runCatching {
        val id = appointmentId.trim()
        if (id.isEmpty()) error("appointment id required")
        val body = postJson(AppEnvironment.apiPath("api/v1/chat/meetings/$id/cancel"), "{}")
        val o = JSONObject(body.trim())
        val root = when {
            o.has("data") && o.get("data") is JSONObject -> o.getJSONObject("data")
            else -> o
        }
        MeetingCancelResult(
            suggestSellerReopenListing = root.optBoolean(
                "suggest_seller_reopen_listing",
                root.optBoolean("SuggestSellerReopenListing", false),
            ),
        )
    }

    /**
     * `POST /chat/meetings/:appointment_id/check-in` — optional GPS in `scheduled_at ± 30m`.
     * Request body uses role-specific keys when coordinates are sent: `buyer_check_in_lat` /
     * `buyer_check_in_lng` or `seller_check_in_lat` / `seller_check_in_lng`.
     *
     * Response (typical): `meeting_appointment`, `already_checked_in`, `role`, optional `your_check_in_at`.
     * Appointment [MeetingAppointmentPayload.status] is unchanged by this call (still confirmed, etc.).
     *
     * If the route is missing (404/405/501), returns [MeetingCheckInResult.endpointAvailable] `false`.
     */
    fun checkInMeeting(
        appointmentId: String,
        isBuyer: Boolean,
        lat: Double? = null,
        lng: Double? = null,
    ): Result<MeetingCheckInResult> {
        val id = appointmentId.trim()
        if (id.isEmpty()) return Result.failure(IllegalArgumentException("appointment id required"))
        val json = JSONObject()
        if (lat != null && lng != null) {
            if (isBuyer) {
                json.put("buyer_check_in_lat", lat)
                json.put("buyer_check_in_lng", lng)
            } else {
                json.put("seller_check_in_lat", lat)
                json.put("seller_check_in_lng", lng)
            }
        }
        val payload = if (json.length() == 0) "{}" else json.toString()
        return try {
            val body = postJson(AppEnvironment.apiPath("api/v1/chat/meetings/$id/check-in"), payload)
            Result.success(parseMeetingCheckInResponse(body))
        } catch (e: Exception) {
            if (isMeetupCheckInEndpointMissing(e)) {
                Result.success(
                    MeetingCheckInResult(endpointAvailable = false),
                )
            } else {
                Result.failure(e)
            }
        }
    }

    // Add this inside class ChatRepository

    /**
     * POST /v1/chat/conversations/:conversation_id/report
     * Body: reported_user_id, category, description (max 2000)
     * Only buyer/seller in the thread can call; reported_user_id must be the other party.
     * Duplicate pending report returns 409 CONVERSATION_REPORT_DUPLICATE.
     */
    fun reportConversation(
        conversationId: String,
        reportedUserId: String,
        category: String,
        description: String?,
    ): Result<Unit> = runCatching {
        if (conversationId.isBlank()) error("conversationId required")
        if (reportedUserId.isBlank()) error("reportedUserId required")
        val allowedCategories = setOf("spam", "harassment", "scam", "inappropriate", "other")
        if (category !in allowedCategories) error("Invalid category: $category")
        val desc = description?.trim()?.takeIf { it.isNotEmpty() }
        if (desc != null && desc.length > 2000) error("Description exceeds 2000 characters")

        val json = JSONObject().apply {
            put("reported_user_id", reportedUserId)
            put("category", category)
            desc?.let { put("description", it) }
        }.toString()

        postJson(
            AppEnvironment.apiPath("api/v1/chat/conversations/$conversationId/report"),
            json,
        )
    }

    private fun parseMeetingCheckInResponse(body: String): MeetingCheckInResult {
        val o = JSONObject(body.trim())
        val root: JSONObject = when {
            o.has("data") && o.get("data") is JSONObject -> o.getJSONObject("data")
            else -> o
        }
        val already = root.optBoolean("already_checked_in", root.optBoolean("AlreadyCheckedIn", false))
        val yourAt = root.optString("your_check_in_at", root.optString("YourCheckInAt", ""))
            .trim()
            .takeIf { it.isNotEmpty() }
        val role = root.optString("role", root.optString("Role", ""))
            .trim()
            .takeIf { it.isNotEmpty() }
        val apptObj = root.optJSONObject("meeting_appointment")
            ?: root.optJSONObject("MeetingAppointment")
        val appt = apptObj?.let { meetingAppointmentJsonToPayload(it) }
        return MeetingCheckInResult(
            endpointAvailable = true,
            alreadyCheckedIn = already,
            yourCheckInAt = yourAt,
            role = role,
            meetingAppointment = appt,
        )
    }

    private fun isMeetupCheckInEndpointMissing(e: Exception): Boolean {
        val msg = e.message.orEmpty()
        return msg.contains("HTTP 404") ||
            msg.contains("HTTP 405") ||
            msg.contains("HTTP 501") ||
            msg.contains(" 404 ") ||
            msg.contains(" 405 ") ||
            msg.contains(" 501 ") ||
            msg.contains("NOT_FOUND", ignoreCase = true) ||
            msg.contains("unknown route", ignoreCase = true) ||
            msg.contains("no such", ignoreCase = true)
    }

    // ── Read / unread ─────────────────────────────────────────────────────

    fun markConversationRead(conversationId: String): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/chat/conversations/$conversationId/read")
        securedClient.newCall(
            Request.Builder()
                .url(url)
                .post("{}".toRequestBody(JSON_MEDIA))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
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

    fun getUnreadCount(): Result<Int> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/chat/unread")
        val body = securedClient.newCall(
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
        val obj = JSONObject(body.trim())
        val root = if (obj.has("data")) obj.getJSONObject("data") else obj
        root.optInt("unread_count", root.optInt("unread", root.optInt("UnreadCount", 0)))
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private fun postJson(url: String, json: String): String =
        securedClient.newCall(
            Request.Builder()
                .url(url)
                .post(json.toRequestBody(JSON_MEDIA))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "FashAndroid/1.0")
                .build(),
        ).execute().use { response ->
            val b = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error(buildHttpErrorMessage(response.code, b))
            }
            b
        }

    // ── Parsers ───────────────────────────────────────────────────────────

    /**
     * Parses the conversations list.
     * API response fields (PascalCase):
     *   ID, BuyerID, SellerID, LastMessage (string), LastMessageAt, UpdatedAt,
     *   Listing { ID, Title, CoverImageURL, Price },
     *   Buyer { UserID, Username, DisplayName, AvatarURL },
     *   Seller { UserID, Username, DisplayName, AvatarURL }
     */
    private fun parseConversations(json: String): List<ConversationItem> {
        val raw = json.trim()
        val arr = when {
            raw.startsWith("[") -> JSONArray(raw)
            else -> try {
                val obj = JSONObject(raw)
                if (obj.has("data")) obj.getJSONArray("data") else JSONArray("[]")
            } catch (_: Exception) { JSONArray("[]") }
        }
        return (0 until arr.length()).map { i -> parseConversationItem(arr.getJSONObject(i)) }
    }

    /**
     * Parses `group_by=listing` payloads: array of { listing, conversations[] } or falls back to
     * client-side grouping of a flat conversation list.
     */
    private fun parseConversationGroups(json: String): List<ConversationListingGroup> {
        val raw = json.trim()
        val arr = when {
            raw.startsWith("[") -> JSONArray(raw)
            else -> try {
                val obj = JSONObject(raw)
                when {
                    obj.has("data") -> obj.getJSONArray("data")
                    obj.has("groups") -> obj.getJSONArray("groups")
                    else -> JSONArray("[]")
                }
            } catch (_: Exception) { JSONArray("[]") }
        }
        if (arr.length() == 0) return emptyList()
        val sample = arr.optJSONObject(0) ?: return emptyList()
        val groupedShape = sample.has("conversations") || sample.has("Conversations") ||
            sample.has("listing") || sample.has("Listing")
        if (groupedShape) {
            return (0 until arr.length()).mapNotNull { i ->
                val g = arr.optJSONObject(i) ?: return@mapNotNull null
                val listingObj = g.optJSONObject("listing") ?: g.optJSONObject("Listing")
                val convArr = g.optJSONArray("conversations")
                    ?: g.optJSONArray("Conversations")
                    ?: JSONArray()
                val listingId = listingObj?.optString("ID", listingObj.optString("id", ""))?.takeIf { it.isNotBlank() }
                    ?: g.optString("listing_id", g.optString("ListingID", ""))
                if (listingId.isBlank()) return@mapNotNull null
                val card = listingObj?.let { parseProductCard(it) }
                val conversations = (0 until convArr.length()).map { j ->
                    parseConversationItem(convArr.getJSONObject(j))
                }
                val count = g.optInt("conversation_count", g.optInt("ConversationCount", conversations.size))
                ConversationListingGroup(
                    listingId = listingId,
                    coverImageUrl = card?.imageUrl ?: conversations.firstOrNull()?.productThumbnailUrl.orEmpty(),
                    title = card?.title ?: conversations.firstOrNull()?.productTitle.orEmpty(),
                    priceVnd = card?.priceVnd ?: conversations.firstOrNull()?.productPrice ?: 0L,
                    conversations = conversations,
                    conversationCountBadge = if (count > 0) count else conversations.size,
                )
            }
        }
        val flat = parseConversations(json)
        return flat.filter { it.productId.isNotBlank() }
            .groupBy { it.productId }
            .map { (pid, rows) ->
                val first = rows.first()
                ConversationListingGroup(
                    listingId = pid,
                    coverImageUrl = first.productThumbnailUrl,
                    title = first.productTitle,
                    priceVnd = first.productPrice,
                    conversations = rows.sortedByDescending { it.timestamp },
                    conversationCountBadge = rows.size,
                )
            }
            .sortedByDescending { g -> g.conversations.maxOfOrNull { it.timestamp }.orEmpty() }
    }

    private fun parseConversationItem(o: JSONObject): ConversationItem {
        val myId = currentUserId
        val convId = o.optString("ID", o.optString("id", o.optString("conversation_id", "")))
        val buyerId = o.optString("BuyerID", o.optString("buyer_id", ""))
        val sellerId = o.optString("SellerID", o.optString("seller_id", ""))
        val buyerObj = o.optJSONObject("Buyer") ?: o.optJSONObject("buyer")
        val sellerObj = o.optJSONObject("Seller") ?: o.optJSONObject("seller")
        val otherProfile: JSONObject? = when {
            myId.isNotBlank() && myId == buyerId -> sellerObj
            myId.isNotBlank() && myId == sellerId -> buyerObj
            sellerObj != null -> sellerObj
            else -> buyerObj
        }
        val listingObj = o.optJSONObject("Listing") ?: o.optJSONObject("listing") ?: o.optJSONObject("product")
        var lastMsgText = ""
        var lastMsgType = "text"
        var lastOfferAmountVnd = 0L
        var lastOfferStatus = ""
        var lastOfferFromBuyer = true
        when {
            o.has("LastMessage") && o.get("LastMessage") is org.json.JSONObject -> {
                val lm = o.getJSONObject("LastMessage")
                lastMsgText = lm.optString("Content", lm.optString("content", lm.optString("Text", "")))
                lastMsgType = lm.optString("MessageType", lm.optString("message_type", "text")).ifBlank { "text" }
                lastOfferAmountVnd = lm.optLong("OfferAmountVND", lm.optLong("offer_amount_vnd", 0L))
                lastOfferStatus = lm.optString("OfferStatus", lm.optString("offer_status", ""))
                val senderId = lm.optString("SenderID", lm.optString("sender_id", ""))
                lastOfferFromBuyer = when {
                    senderId.isNotBlank() && buyerId.isNotBlank() -> senderId == buyerId
                    else -> true
                }
            }
            o.has("last_message") && o.get("last_message") is org.json.JSONObject -> {
                val lm = o.getJSONObject("last_message")
                lastMsgText = lm.optString("content", lm.optString("Content", ""))
                lastMsgType = lm.optString("message_type", lm.optString("MessageType", "text")).ifBlank { "text" }
                lastOfferAmountVnd = lm.optLong("offer_amount_vnd", lm.optLong("OfferAmountVND", 0L))
                lastOfferStatus = lm.optString("offer_status", lm.optString("OfferStatus", ""))
                val senderId = lm.optString("sender_id", lm.optString("SenderID", ""))
                lastOfferFromBuyer = when {
                    senderId.isNotBlank() && buyerId.isNotBlank() -> senderId == buyerId
                    else -> true
                }
            }
            else -> {
                lastMsgText = o.optString("LastMessage", o.optString("last_message", ""))
            }
        }
        lastMsgType = o.optString("LastMessageType", o.optString("last_message_type", lastMsgType)).ifBlank { lastMsgType }
        if (lastOfferAmountVnd == 0L) {
            lastOfferAmountVnd = o.optLong("LastOfferAmountVND", o.optLong("last_offer_amount_vnd", 0L))
        }
        if (lastOfferAmountVnd > 0L) lastMsgType = "offer"
        val pendingOfferObj = o.optJSONObject("pending_offer") ?: o.optJSONObject("PendingOffer")
        val pendingOfferAmountVnd = pendingOfferObj?.let { po ->
            po.optLong("AmountVND", po.optLong("amount_vnd", po.optLong("Amount", 0L)))
        } ?: o.optLong("PendingOfferAmountVND", o.optLong("pending_offer_amount_vnd", 0L))
        val lastMsgAt = o.optString("LastMessageAt", "").takeIf { it.isNotBlank() && it != "null" }
        val timestamp = lastMsgAt ?: o.optString("UpdatedAt", o.optString("updated_at", o.optString("CreatedAt", "")))
        val productThumb: String = listingObj?.let { listing ->
            val arr = listing.optJSONArray("ImageURLs") ?: listing.optJSONArray("image_urls")
            ListingImageUrlsWire.resolveCoverUrl(
                listing.optString("CoverImageURL", "")
                    .ifBlank { listing.optString("cover_image_url", "") },
                arr,
            )
        }.orEmpty()
        val productId: String = listingObj?.optString("ID", listingObj.optString("id", ""))
            ?.takeIf { it.isNotBlank() }
            ?: o.optString("ListingID", o.optString("listing_id", ""))
        val unreadCount = o.optInt("unread_count", o.optInt("UnreadCount", o.optInt("unreadCount", 0)))
            .coerceAtLeast(0)
        val hasUnreadFlag = o.optBoolean("has_unread", o.optBoolean("HasUnread", o.optBoolean("hasUnread", false)))
        val legacyUnread = o.optBoolean("IsUnread", o.optBoolean("is_unread", o.optBoolean("unread", false)))
        val hasUnread = hasUnreadFlag || unreadCount > 0 || legacyUnread
        return ConversationItem(
            conversationId = convId,
            otherUserId = otherProfile?.optString("UserID", otherProfile.optString("user_id", otherProfile.optString("ID", ""))) ?: "",
            username = otherProfile?.optString("Username", otherProfile.optString("username", "")) ?: "",
            displayName = otherProfile?.optString("DisplayName", otherProfile.optString("display_name", "")) ?: "",
            avatarUrl = otherProfile?.optString("AvatarURL", otherProfile.optString("avatar_url", "")) ?: "",
            lastMessageText = lastMsgText,
            lastMessageType = lastMsgType,
            lastOfferAmountVnd = lastOfferAmountVnd,
            lastOfferStatus = lastOfferStatus,
            lastOfferFromBuyer = lastOfferFromBuyer,
            pendingOfferAmountVnd = pendingOfferAmountVnd,
            timestamp = timestamp,
            productThumbnailUrl = productThumb,
            productTitle = listingObj?.optString("Title", listingObj.optString("title", "")) ?: "",
            productId = productId,
            productPrice = listingObj?.optLong("Price", listingObj.optLong("price", 0L)) ?: 0L,
            hasUnread = hasUnread,
            unreadCount = unreadCount,
            buyerUserId = buyerId,
            sellerUserId = sellerId,
        )
    }

    /**
     * Parses single conversation detail.
     * Same PascalCase field rules as [parseConversations].
     */
    private fun parseConversationDetail(json: String): ConversationDetail {
        val raw = json.trim()
        val obj = if (raw.startsWith("{")) JSONObject(raw) else JSONObject("{}")
        val data = if (obj.has("data")) obj.getJSONObject("data") else obj
        // Some gateways wrap the resource as data.conversation { ... }
        val root = data.optJSONObject("conversation")
            ?: data.optJSONObject("Conversation")
            ?: data

        val convId = root.optString("ID", root.optString("id", root.optString("conversation_id", "")))

        val myId = currentUserId
        val buyerId = root.optString("BuyerID", root.optString("buyer_id", ""))
        val sellerId = root.optString("SellerID", root.optString("seller_id", ""))
        val buyerObj = root.optJSONObject("Buyer") ?: root.optJSONObject("buyer")
        val sellerObj = root.optJSONObject("Seller") ?: root.optJSONObject("seller")
        val otherProfile: JSONObject? = when {
            myId.isNotBlank() && myId == buyerId -> sellerObj
            myId.isNotBlank() && myId == sellerId -> buyerObj
            sellerObj != null -> sellerObj
            else -> buyerObj
        }
        // Also try legacy "other_user" key from older API versions
        val otherUser = parseOtherUser(otherProfile ?: root.optJSONObject("other_user") ?: root)

        val listingObj = root.optJSONObject("Listing")
            ?: root.optJSONObject("listing")
            ?: root.optJSONObject("product")
        val product = listingObj?.let { parseProductCard(it) }

        // Messages embedded in detail response (optional — separate getMessages() call preferred)
        val messagesArr = root.optJSONArray("Messages")
            ?: root.optJSONArray("messages")
            ?: root.optJSONArray("message_list")
            ?: JSONArray("[]")
        val messages = (0 until messagesArr.length()).map { i -> parseMessageObj(messagesArr.getJSONObject(i)) }

        val pendingOffer = root.optJSONObject("pending_offer")?.let { parseOfferObj(it) }

        val isBuyer = when {
            myId.isNotBlank() && buyerId.isNotBlank() -> myId == buyerId
            else -> true  // unknown — default to showing the offer button
        }

        // Extract order_id — non-null means the seller accepted an offer and an order was created
        val orderId = root.optString("order_id", root.optString("OrderID", ""))
            .takeIf { it.isNotBlank() && it != "null" }

        val offerCount = root.optInt("offer_count", root.optInt("OfferCount", 0))
        val isClosed = root.optBoolean("is_closed", root.optBoolean("IsClosed", false))
        val myReport = parseMyConversationReport(root)

        return ConversationDetail(
            conversationId = convId,
            otherUser = otherUser,
            product = product,
            messages = messages,
            pendingOffer = pendingOffer,
            isBuyer = isBuyer,
            orderId = orderId,
            offerCount = offerCount,
            isClosed = isClosed,
            myReport = myReport,
        )
    }

    /** Latest report by the current user on this conversation, if present (`my_report` on GET detail). */
    private fun parseMyConversationReport(root: JSONObject): MyConversationReport? {
        val o = root.optJSONObject("my_report")
            ?: root.optJSONObject("MyReport")
            ?: return null
        if (o.length() == 0) return null
        val reportId = o.optString("report_id", o.optString("ReportID", o.optString("id", ""))).trim()
        if (reportId.isEmpty()) return null
        val status = o.optString("status", o.optString("Status", ""))
            .trim()
            .lowercase()
            .ifBlank { "pending" }
        val reportedUserId = o.optString("reported_user_id", o.optString("ReportedUserID", "")).trim()
        val category = o.optString("category", o.optString("Category", "")).trim().lowercase()
        val createdAt = o.optString("created_at", o.optString("CreatedAt", "")).trim()
        return MyConversationReport(
            reportId = reportId,
            reportedUserId = reportedUserId,
            category = category,
            createdAt = createdAt,
            status = status,
        )
    }

    /**
     * Parses a profile object into [OtherUser].
     * Handles both PascalCase (UserID, Username, AvatarURL) and snake_case.
     */
    private fun parseOtherUser(o: JSONObject) = OtherUser(
        userId = o.optString("UserID", o.optString("user_id", o.optString("id", ""))),
        displayName = o.optString("DisplayName", o.optString("display_name", "")),
        username = o.optString("Username", o.optString("username", "")),
        avatarUrl = o.optString("AvatarURL", o.optString("avatar_url", "")),
        isOnline = o.optBoolean("is_online", o.optBoolean("IsOnline", false)),
    )

    /**
     * Parses a Listing object into [ProductCard].
     * Handles PascalCase: ID, Title, CoverImageURL, Price.
     */
    private fun parseProductCard(p: JSONObject): ProductCard {
        val urlsArr = p.optJSONArray("ImageURLs") ?: p.optJSONArray("image_urls")
        val imageUrl = ListingImageUrlsWire.resolveCoverUrl(
            p.optString("CoverImageURL", "")
                .ifBlank { p.optString("cover_image_url", "") }
                .ifBlank { p.optString("image_url", "") }
                .ifBlank { p.optString("thumbnail_url", "") },
            urlsArr,
        )
        return ProductCard(
            listingId = p.optString("ID", p.optString("id", p.optString("listing_id", ""))),
            title = p.optString("Title", p.optString("title", "")),
            priceVnd = p.optLong("Price", p.optLong("price", p.optLong("price_vnd", 0L))),
            imageUrl = imageUrl,
            listingStatus = p.optString("Status", p.optString("status", "active")).lowercase().ifBlank { "active" },
        )
    }

    /**
     * Parses a message object.
     * Handles PascalCase: ID, Content, SenderID, CreatedAt, IsRead.
     * Determines [ChatMessage.isFromMe] by comparing SenderID with current user's ID.
     */
    private fun parseMessageObj(m: JSONObject): ChatMessage {
        val myId = currentUserId
        val senderId = m.optString("SenderID", m.optString("sender_id", ""))
        val isFromMe = when {
            m.has("is_from_me") -> m.getBoolean("is_from_me")
            m.has("from_me") -> m.getBoolean("from_me")
            myId.isNotBlank() && senderId.isNotBlank() -> senderId == myId
            else -> false
        }
        // API field is "MessageType" (PascalCase), not "Type"
        val rawType = m.optString("MessageType", m.optString("message_type", m.optString("Type", m.optString("type", "text"))))
            .ifBlank { "text" }
        val systemSubtype = m.optString("system_subtype", m.optString("system_type", m.optString("SystemSubtype", "")))
            .ifBlank { null }

        val meetingAppointment = parseMeetingAppointmentPayload(m)

        // Offer data is flat on the message object: OfferAmountVND, OfferStatus (NOT nested)
        val offerAmount = m.optLong("OfferAmountVND", m.optLong("offer_amount_vnd", 0L))
        val offerStatus = m.optString("OfferStatus", m.optString("offer_status", "")).ifBlank { "pending" }

        // isRead is derived from ReadAt (null = unread), with boolean fallbacks for other backends
        val isRead = when {
            m.has("ReadAt") -> !m.isNull("ReadAt")
            m.has("read_at") -> !m.isNull("read_at")
            m.has("IsRead") -> m.getBoolean("IsRead")
            m.has("is_read") -> m.getBoolean("is_read")
            else -> false
        }

        return ChatMessage(
            messageId = m.optString("ID", m.optString("id", m.optString("message_id", ""))),
            text = m.optString("Content", m.optString("content", m.optString("text", ""))),
            isFromMe = isFromMe,
            senderId = senderId,
            timestamp = m.optString("CreatedAt", m.optString("created_at", m.optString("timestamp", m.optString("sent_at", "")))),
            isRead = isRead,
            messageType = rawType,
            offerAmountVnd = offerAmount,
            offerStatus = offerStatus,
            outboundState = OutboundSendState.NONE,
            systemSubtype = systemSubtype,
            meetingAppointment = meetingAppointment,
        )
    }

    private fun parseMeetingAppointmentPayload(m: JSONObject): MeetingAppointmentPayload? {
        val o = m.optJSONObject("meeting_appointment")
            ?: m.optJSONObject("MeetingAppointment")
            ?: return null
        return meetingAppointmentJsonToPayload(o)
    }

    private fun meetingAppointmentJsonToPayload(o: JSONObject): MeetingAppointmentPayload? {
        val proposerId = o.optString("proposer_id", o.optString("ProposerID", ""))
        val myId = currentUserId
        val id = o.optString("id", o.optString("ID", "")).trim()
        if (id.isEmpty()) return null
        return MeetingAppointmentPayload(
            id = id,
            status = o.optString("status", o.optString("Status", "")).lowercase().ifBlank { "pending" },
            locationUrl = o.optString("location_url", o.optString("LocationURL", "")),
            scheduledAt = o.optString("scheduled_at", o.optString("ScheduledAt", "")),
            reminderOffsetMinutes = o.optInt("reminder_offset_minutes", o.optInt("ReminderOffsetMinutes", 60)),
            reminderEnabled = o.optBoolean("reminder_enabled", o.optBoolean("ReminderEnabled", true)),
            proposerId = proposerId,
            isProposerMe = myId.isNotBlank() && proposerId.isNotBlank() && proposerId == myId,
            buyerCheckInAt = appointmentOptIso(o, "buyer_check_in_at", "BuyerCheckInAt"),
            sellerCheckInAt = appointmentOptIso(o, "seller_check_in_at", "SellerCheckInAt"),
        )
    }

    private fun appointmentOptIso(o: JSONObject, vararg keys: String): String {
        for (k in keys) {
            val v = o.optString(k, "").trim()
            if (v.isNotEmpty()) return v
        }
        return ""
    }

    private fun parseMessagesArray(json: String): List<ChatMessage> {
        val raw = json.trim()
        val arr = when {
            raw.startsWith("[") -> JSONArray(raw)
            else -> try {
                val obj = JSONObject(raw)
                when {
                    obj.has("data") -> obj.getJSONArray("data")
                    obj.has("Messages") -> obj.getJSONArray("Messages")
                    obj.has("messages") -> obj.getJSONArray("messages")
                    else -> JSONArray("[]")
                }
            } catch (_: Exception) { JSONArray("[]") }
        }
        return (0 until arr.length())
            .map { i -> arr.getJSONObject(i) }
            .filterNot { it.optBoolean("IsDeleted", it.optBoolean("is_deleted", false)) }
            .map { parseMessageObj(it) }
            .groupBy { it.messageId }
            .map { (_, rows) -> rows.last() }
            .sortedBy { it.timestamp }   // Ascending (oldest first); dedupe by id in case API repeats a row
    }

    private fun parseMessage(json: String): ChatMessage {
        val obj = JSONObject(json.trim())
        val data: JSONObject = when {
            obj.has("data") && !obj.isNull("data") -> obj.getJSONObject("data")
            obj.has("message") && !obj.isNull("message") -> obj.getJSONObject("message")
            obj.has("Message") && !obj.isNull("Message") -> obj.getJSONObject("Message")
            else -> obj
        }
        return parseMessageObj(data)
    }

    private fun parseOfferObj(o: JSONObject) = PriceOffer(
        offerId = o.optString("ID", o.optString("id", o.optString("offer_id", ""))),
        amountVnd = o.optLong("AmountVND", o.optLong("amount_vnd", o.optLong("amount", 0L))),
        proposedByMe = run {
            // Try SenderID comparison first (most reliable)
            val senderId = o.optString("SenderID", o.optString("sender_id", ""))
            val myId = currentUserId
            when {
                senderId.isNotBlank() && myId.isNotBlank() -> senderId == myId
                else -> o.optBoolean("proposed_by_me", o.optBoolean("from_me", false))
            }
        },
        status = o.optString("Status", o.optString("status", "pending")),
    )

    private fun parseOffer(json: String): PriceOffer {
        val obj = JSONObject(json.trim())
        val data = if (obj.has("data")) obj.getJSONObject("data") else obj
        return parseOfferObj(data)
    }
}

// ── Data models ───────────────────────────────────────────────────────────────

/** Response hints from `POST /chat/meetings/:id/cancel`. */
data class MeetingCancelResult(
    val suggestSellerReopenListing: Boolean = false,
)

/**
 * Parsed `POST /chat/meetings/:appointment_id/check-in` body.
 * Does not change `meeting_appointment.status` (stays pending / confirmed / cancelled).
 */
data class MeetingCheckInResult(
    /** False when the route is missing (404/405/501) — caller should refresh chat/order only. */
    val endpointAvailable: Boolean,
    /** Server did not write again — this party had already checked in. */
    val alreadyCheckedIn: Boolean = false,
    val yourCheckInAt: String? = null,
    val role: String? = null,
    val meetingAppointment: MeetingAppointmentPayload? = null,
)

/** Embedded on [ChatMessage] when [ChatMessage.messageType] is `meeting_proposal` and API sends `meeting_appointment`. */
data class MeetingAppointmentPayload(
    val id: String,
    val status: String,
    val locationUrl: String,
    val scheduledAt: String,
    val reminderOffsetMinutes: Int,
    val reminderEnabled: Boolean,
    val proposerId: String,
    val isProposerMe: Boolean,
    val buyerCheckInAt: String = "",
    val sellerCheckInAt: String = "",
)

data class ConversationDetail(
    val conversationId: String,
    val otherUser: OtherUser,
    val product: ProductCard?,
    val messages: List<ChatMessage>,
    val pendingOffer: PriceOffer?,
    /** True when the current user is the buyer in this conversation (not the seller). */
    val isBuyer: Boolean = true,
    /** Non-null when the seller has accepted an offer and the backend created an order. */
    val orderId: String? = null,
    /** Server offer counter; buyer offer button disabled when >= env max per conversation (see CHAT_MAX_OFFERS_PER_CONVERSATION). */
    val offerCount: Int = 0,
    /** Listing reserved / chat read-only. */
    val isClosed: Boolean = false,
    /** Present when the current user has a report on this thread (GET conversation detail). */
    val myReport: MyConversationReport? = null,
)

/** Subset of the server `my_report` object on conversation detail. */
data class MyConversationReport(
    val reportId: String,
    val reportedUserId: String,
    val category: String,
    val createdAt: String,
    /** `pending` | `dismissed` | `warned` | `suspended` — moderation progress. */
    val status: String,
)

/** Seller inbox: conversations grouped under one listing. */
data class ConversationListingGroup(
    val listingId: String,
    val coverImageUrl: String,
    val title: String,
    val priceVnd: Long,
    val conversations: List<ConversationItem>,
    val conversationCountBadge: Int,
)

data class OtherUser(
    val userId: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String,
    val isOnline: Boolean = false,
)

data class ProductCard(
    val listingId: String,
    val title: String,
    val priceVnd: Long,
    val imageUrl: String,
    /** "active" | "reserved" | "sold" — used to show SOLD badge overlay. */
    val listingStatus: String = "active",
)

/**
 * Local-only state for outbound text messages (optimistic UI).
 * Server-backed rows use [NONE]; [SENDING]/[FAILED] are for pending client sends.
 */
enum class OutboundSendState {
    NONE,
    SENDING,
    FAILED,
}

data class ChatMessage(
    val messageId: String,
    val text: String,
    val isFromMe: Boolean,
    val timestamp: String,
    val isRead: Boolean = false,
    val senderId: String = "",
    /** "text" | "offer" | "system" | "meeting_proposal" */
    val messageType: String = "text",
    /** Populated when messageType == "offer" */
    val offerAmountVnd: Long = 0L,
    val offerStatus: String = "",
    /** Only for optimistic sends; cleared when the server row is merged in. */
    val outboundState: OutboundSendState = OutboundSendState.NONE,
    /** For system rows: e.g. `conversation.closed`, `conversation.reopened`. */
    val systemSubtype: String? = null,
    /** When [messageType] is `meeting_proposal`, filled from API `meeting_appointment`. */
    val meetingAppointment: MeetingAppointmentPayload? = null,
)

data class PriceOffer(
    val offerId: String,
    val amountVnd: Long,
    val proposedByMe: Boolean,
    val status: String,
)

data class ConversationItem(
    val conversationId: String,
    val otherUserId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val lastMessageText: String,
    /** "text" | "offer" | "system" — from API or parsed [LastMessage] object. */
    val lastMessageType: String = "text",
    val lastOfferAmountVnd: Long = 0L,
    val lastOfferStatus: String = "",
    /** When last row is an offer, true if the buyer proposed (typical). */
    val lastOfferFromBuyer: Boolean = true,
    /** Buyer’s pending offer amount (seller inbox preview). */
    val pendingOfferAmountVnd: Long = 0L,
    val timestamp: String,
    val productThumbnailUrl: String,
    val productTitle: String,
    val productId: String,
    val productPrice: Long = 0L,
    /** At least one unread inbound message (API: has_unread). */
    val hasUnread: Boolean,
    /** Count of unread inbound messages (API: unread_count). */
    val unreadCount: Int = 0,
    val buyerUserId: String = "",
    val sellerUserId: String = "",
)
