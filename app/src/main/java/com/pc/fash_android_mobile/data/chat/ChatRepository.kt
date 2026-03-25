package com.pc.fash_android_mobile.data.chat

import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.auth.AuthSessionStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

/**
 * Chat/conversations API client.
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

    /** API: GET /chat/conversations?limit=&offset= — only these two params are supported. */
    fun getConversations(
        limit: Int = 50,
        offset: Int = 0,
    ): Result<List<ConversationItem>> = runCatching {
        val url = "${AppEnvironment.apiPath("api/v1/chat/conversations")}?limit=$limit&offset=$offset"
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
        parseConversations(body)
    }

    // ── Start / create conversation ───────────────────────────────────────

    /** API: POST /chat/conversations — body: { listing_id }. Returns 200 (existing) or 201 (new). */
    fun startConversation(listingId: String): Result<String> = runCatching {
        if (listingId.isBlank()) error("listing_id is required")
        val json = JSONObject().put("listing_id", listingId).toString()
        val body = postJson(AppEnvironment.apiPath("api/v1/chat/conversations"), json)
        val obj = JSONObject(body.trim())
        val data = if (obj.has("data")) obj.getJSONObject("data") else obj
        data.optString("ID", data.optString("id", data.optString("conversation_id", ""))).takeIf { it.isNotBlank() }
            ?: error("No conversation id in response")
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
                    val msg = try { JSONObject(b).optString("error", b).ifBlank { b } } catch (_: Exception) { b }
                    error("HTTP ${response.code}: $msg")
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
                val msg = try { JSONObject(b).optString("error", b).ifBlank { b } } catch (_: Exception) { b }
                error("HTTP ${response.code}: $msg")
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
                val msg = try { JSONObject(b).optString("error", b).ifBlank { b } } catch (_: Exception) { b }
                error("HTTP ${response.code}: $msg")
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
                val msg = try { JSONObject(b).optString("error", b).ifBlank { b } } catch (_: Exception) { b }
                error("HTTP ${response.code}: $msg")
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

    /** API: POST /chat/offers/accept or /decline — body: { conversation_id, offer_message_id }. */
    fun respondToOffer(
        conversationId: String,
        offerMessageId: String,
        accept: Boolean,
    ): Result<Unit> = runCatching {
        val path = if (accept) "api/v1/chat/offers/accept" else "api/v1/chat/offers/decline"
        val json = JSONObject()
            .put("conversation_id", conversationId)
            .put("offer_message_id", offerMessageId)
            .toString()
        postJson(AppEnvironment.apiPath(path), json)
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
                val msg = try { JSONObject(b).optString("error", b).ifBlank { b } } catch (_: Exception) { b }
                error("HTTP ${response.code}: $msg")
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
        val myId = currentUserId
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)

            // Conversation ID
            val convId = o.optString("ID", o.optString("id", o.optString("conversation_id", "")))

            // Determine "other user" by comparing BuyerID/SellerID with current user
            val buyerId = o.optString("BuyerID", o.optString("buyer_id", ""))
            val sellerId = o.optString("SellerID", o.optString("seller_id", ""))
            val buyerObj = o.optJSONObject("Buyer") ?: o.optJSONObject("buyer")
            val sellerObj = o.optJSONObject("Seller") ?: o.optJSONObject("seller")
            // If current user is the buyer → show seller as "other", and vice versa
            val otherProfile: JSONObject? = when {
                myId.isNotBlank() && myId == buyerId -> sellerObj
                myId.isNotBlank() && myId == sellerId -> buyerObj
                // Fallback: prefer seller profile (most common case: buyer browsing)
                sellerObj != null -> sellerObj
                else -> buyerObj
            }

            // Listing / product (Listing object may be null — fall back to root ListingID)
            val listingObj = o.optJSONObject("Listing") ?: o.optJSONObject("listing") ?: o.optJSONObject("product")

            // LastMessage is a plain string field on the root
            val lastMsgText = o.optString("LastMessage", o.optString("last_message", ""))

            // Timestamp: prefer LastMessageAt (null-safe), fall back to UpdatedAt
            val lastMsgAt = o.optString("LastMessageAt", "").takeIf { it.isNotBlank() && it != "null" }
            val timestamp = lastMsgAt ?: o.optString("UpdatedAt", o.optString("updated_at", o.optString("CreatedAt", "")))

            // Product thumbnail: CoverImageURL first, then first element of ImageURLs array
            val productThumb: String = listingObj?.let { listing ->
                listing.optString("CoverImageURL", listing.optString("cover_image_url", ""))
                    .takeIf { it.isNotBlank() }
                    ?: listing.optJSONArray("ImageURLs")?.optString(0, "")?.takeIf { it.isNotBlank() }
                    ?: listing.optJSONArray("image_urls")?.optString(0, "")?.takeIf { it.isNotBlank() }
                    ?: ""
            } ?: ""

            // Product ID: prefer Listing.ID, then root ListingID as fallback
            val productId: String = listingObj?.optString("ID", listingObj.optString("id", ""))
                ?.takeIf { it.isNotBlank() }
                ?: o.optString("ListingID", o.optString("listing_id", ""))

            // isUnread: the API does not expose this field directly.
            // We derive it: if there IS a last message and the current user is NOT the sender,
            // treat it as potentially unread. Since we don't have LastMessageSenderID here we
            // fall back to the explicit field if the backend ever adds it.
            val isUnread = o.optBoolean("IsUnread", o.optBoolean("is_unread", o.optBoolean("unread", false)))

            ConversationItem(
                conversationId = convId,
                otherUserId = otherProfile?.optString("UserID", otherProfile.optString("user_id", otherProfile.optString("ID", ""))) ?: "",
                username = otherProfile?.optString("Username", otherProfile.optString("username", "")) ?: "",
                displayName = otherProfile?.optString("DisplayName", otherProfile.optString("display_name", "")) ?: "",
                avatarUrl = otherProfile?.optString("AvatarURL", otherProfile.optString("avatar_url", "")) ?: "",
                lastMessageText = lastMsgText,
                timestamp = timestamp,
                productThumbnailUrl = productThumb,
                productTitle = listingObj?.optString("Title", listingObj.optString("title", "")) ?: "",
                productId = productId,
                productPrice = listingObj?.optLong("Price", listingObj.optLong("price", 0L)) ?: 0L,
                isUnread = isUnread,
                buyerUserId = buyerId,
                sellerUserId = sellerId,
            )
        }
    }

    /**
     * Parses single conversation detail.
     * Same PascalCase field rules as [parseConversations].
     */
    private fun parseConversationDetail(json: String): ConversationDetail {
        val raw = json.trim()
        val obj = if (raw.startsWith("{")) JSONObject(raw) else JSONObject("{}")
        val data = if (obj.has("data")) obj.getJSONObject("data") else obj

        val convId = data.optString("ID", data.optString("id", data.optString("conversation_id", "")))

        val myId = currentUserId
        val buyerId = data.optString("BuyerID", data.optString("buyer_id", ""))
        val sellerId = data.optString("SellerID", data.optString("seller_id", ""))
        val buyerObj = data.optJSONObject("Buyer") ?: data.optJSONObject("buyer")
        val sellerObj = data.optJSONObject("Seller") ?: data.optJSONObject("seller")
        val otherProfile: JSONObject? = when {
            myId.isNotBlank() && myId == buyerId -> sellerObj
            myId.isNotBlank() && myId == sellerId -> buyerObj
            sellerObj != null -> sellerObj
            else -> buyerObj
        }
        // Also try legacy "other_user" key from older API versions
        val otherUser = parseOtherUser(otherProfile ?: data.optJSONObject("other_user") ?: data)

        val listingObj = data.optJSONObject("Listing")
            ?: data.optJSONObject("listing")
            ?: data.optJSONObject("product")
        val product = listingObj?.let { parseProductCard(it) }

        // Messages embedded in detail response (optional — separate getMessages() call preferred)
        val messagesArr = data.optJSONArray("Messages")
            ?: data.optJSONArray("messages")
            ?: data.optJSONArray("message_list")
            ?: JSONArray("[]")
        val messages = (0 until messagesArr.length()).map { i -> parseMessageObj(messagesArr.getJSONObject(i)) }

        val pendingOffer = data.optJSONObject("pending_offer")?.let { parseOfferObj(it) }

        val isBuyer = when {
            myId.isNotBlank() && buyerId.isNotBlank() -> myId == buyerId
            else -> true  // unknown — default to showing the offer button
        }

        // Extract order_id — non-null means the seller accepted an offer and an order was created
        val orderId = data.optString("order_id", data.optString("OrderID", ""))
            .takeIf { it.isNotBlank() && it != "null" }

        return ConversationDetail(
            conversationId = convId,
            otherUser = otherUser,
            product = product,
            messages = messages,
            pendingOffer = pendingOffer,
            isBuyer = isBuyer,
            orderId = orderId,
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
        val imageUrl = p.optString("CoverImageURL", "").takeIf { it.isNotBlank() }
            ?: p.optString("cover_image_url", "").takeIf { it.isNotBlank() }
            ?: p.optString("image_url", "").takeIf { it.isNotBlank() }
            ?: p.optString("thumbnail_url", "").takeIf { it.isNotBlank() }
            ?: run {
                // Try first element of ImageURLs array
                val urlsArr = p.optJSONArray("ImageURLs") ?: p.optJSONArray("image_urls")
                urlsArr?.optString(0, "") ?: ""
            }
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
        )
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
            .sortedBy { it.timestamp }   // Ensure ascending order (oldest first) regardless of API response order
    }

    private fun parseMessage(json: String): ChatMessage {
        val obj = JSONObject(json.trim())
        val data = if (obj.has("data")) obj.getJSONObject("data") else obj
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

data class ChatMessage(
    val messageId: String,
    val text: String,
    val isFromMe: Boolean,
    val timestamp: String,
    val isRead: Boolean = false,
    val senderId: String = "",
    /** "text" | "offer" | "system" */
    val messageType: String = "text",
    /** Populated when messageType == "offer" */
    val offerAmountVnd: Long = 0L,
    val offerStatus: String = "",
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
    val timestamp: String,
    val productThumbnailUrl: String,
    val productTitle: String,
    val productId: String,
    val productPrice: Long = 0L,
    val isUnread: Boolean,
    val buyerUserId: String = "",
    val sellerUserId: String = "",
)
