package com.pc.fash_android_mobile.data.chat

import com.pc.fash_android_mobile.config.AppEnvironment
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

/**
 * Chat/conversations API client.
 */
class ChatRepository(
    private val securedClient: OkHttpClient,
) {

    /**
     * Fetches conversation list.
     * @param limit Max items per page
     * @param offset Pagination offset
     * @param status Optional: "unread" to filter unread only
     * @param role Optional: "seller" or "buyer" to filter by role
     */
    fun getConversations(
        limit: Int = 20,
        offset: Int = 0,
        status: String? = null,
        role: String? = null,
    ): Result<List<ConversationItem>> = runCatching {
        val base = AppEnvironment.apiPath("api/v1/chat/conversations")
        val query = mutableListOf<String>()
        query.add("limit=$limit")
        query.add("offset=$offset")
        status?.let { query.add("status=$it") }
        role?.let { query.add("role=$it") }
        val url = "$base?${query.joinToString("&")}"
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

    /**
     * Starts or gets a conversation for a listing (buyer with seller). Returns conversation ID.
     * Uses api/v1/chat/conversations (core-service). Fallback: api/v1/conversations.
     */
    fun startConversation(listingId: String): Result<String> = runCatching {
        if (listingId.isBlank()) error("listing_id is required")
        val json = JSONObject().put("listing_id", listingId).toString()
        val requestBody = json.toRequestBody(JSON_MEDIA)
        fun doPost(url: String): String {
            val response = securedClient.newCall(
                Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "FashAndroid/1.0")
                    .build(),
            ).execute()
            val b = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val msg = try { JSONObject(b).optString("error", b).ifBlank { b } } catch (_: Exception) { b }
                error("HTTP ${response.code}: $msg")
            }
            return b
        }
        val body = try {
            doPost(AppEnvironment.apiPath("api/v1/chat/conversations"))
        } catch (e: Exception) {
            if (e.message?.contains("404") == true) {
                doPost(AppEnvironment.apiPath("api/v1/conversations"))
            } else {
                throw e
            }
        }
        val obj = JSONObject(body.trim())
        val data = if (obj.has("data")) obj.getJSONObject("data") else obj
        data.optString("conversation_id", data.optString("id", data.optString("ID", ""))).takeIf { it.isNotBlank() }
            ?: error("No conversation_id in response")
    }

    /**
     * Fetches conversation detail (other user, product, messages, pending offer).
     * Uses api/v1/chat/conversations/:id (core-service). Fallback: api/v1/conversations/:id.
     */
    fun getConversationDetail(conversationId: String): Result<ConversationDetail> = runCatching {
        fun doGet(url: String): String {
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
            return body
        }
        val body = try {
            doGet(AppEnvironment.apiPath("api/v1/chat/conversations/$conversationId"))
        } catch (e: Exception) {
            if (e.message?.contains("404") == true) {
                doGet(AppEnvironment.apiPath("api/v1/conversations/$conversationId"))
            } else {
                throw e
            }
        }
        parseConversationDetail(body)
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

    /**
     * `POST /chat/offers` — tries doc body (`conversation_id` + `amount_vnd`), then legacy with `listing_id`.
     */
    fun createOffer(conversationId: String, listingId: String?, amountVnd: Long): Result<PriceOffer> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/chat/offers")
        val minimal = JSONObject()
            .put("conversation_id", conversationId)
            .put("amount_vnd", amountVnd)
            .toString()
        val body = try {
            postJson(url, minimal)
        } catch (_: Exception) {
            if (listingId.isNullOrBlank()) throw IllegalStateException("listing_id required for offer on this server")
            val full = JSONObject()
                .put("conversation_id", conversationId)
                .put("listing_id", listingId)
                .put("amount_vnd", amountVnd)
                .toString()
            postJson(url, full)
        }
        parseOffer(body)
    }

    /**
     * Accept/decline offer: `POST /chat/offers/accept|decline` (doc), fallback to `POST /offers/{id}/respond`.
     */
    fun respondToOffer(
        conversationId: String,
        offerMessageId: String,
        accept: Boolean,
    ): Result<Unit> = runCatching {
        val docPath = if (accept) "api/v1/chat/offers/accept" else "api/v1/chat/offers/decline"
        val docBody = JSONObject()
            .put("conversation_id", conversationId)
            .put("offer_message_id", offerMessageId)
            .toString()
        try {
            postJson(AppEnvironment.apiPath(docPath), docBody)
        } catch (_: Exception) {
            val url = AppEnvironment.apiPath("api/v1/offers/$offerMessageId/respond")
            val legacy = JSONObject().put("accept", accept).toString()
            postJson(url, legacy)
        }
    }

    /** `GET /chat/conversations/{id}/messages` — newest first. */
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

    /** `POST /chat/conversations/{id}/read` */
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

    /** `GET /chat/unread` */
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
        root.optInt("unread_count", root.optInt("unread", 0))
    }

    private fun postJson(url: String, json: String): String {
        return securedClient.newCall(
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
    }

    private fun parseMessagesArray(json: String): List<ChatMessage> {
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
        return (0 until arr.length()).map { i -> parseMessageObj(arr.getJSONObject(i)) }
    }

    private fun parseConversationDetail(json: String): ConversationDetail {
        val obj = JSONObject(json.trim())
        val data = if (obj.has("data")) obj.getJSONObject("data") else obj
        val other = data.optJSONObject("other_user") ?: data
        val product = data.optJSONObject("product") ?: data.optJSONObject("listing")
        val messagesArr = data.optJSONArray("messages") ?: data.optJSONArray("message_list") ?: JSONArray("[]")
        val messages = (0 until messagesArr.length()).map { i -> parseMessageObj(messagesArr.getJSONObject(i)) }
        val pendingOfferJson = data.optJSONObject("pending_offer")
        val pendingOffer = pendingOfferJson?.let { parseOfferObj(it) }
        return ConversationDetail(
            conversationId = data.optString("conversation_id", data.optString("id", "")),
            otherUser = parseOtherUser(other),
            product = product?.let { parseProductCard(it) },
            messages = messages,
            pendingOffer = pendingOffer,
        )
    }

    private fun parseOtherUser(o: JSONObject) = OtherUser(
        userId = o.optString("user_id", o.optString("id", "")),
        displayName = o.optString("display_name", o.optString("DisplayName", "")),
        username = o.optString("username", o.optString("Username", "")),
        avatarUrl = o.optString("avatar_url", o.optString("AvatarURL", "")),
        isOnline = o.optBoolean("is_online", o.optBoolean("isOnline", false)),
    )

    private fun parseProductCard(p: JSONObject) = ProductCard(
        listingId = p.optString("listing_id", p.optString("id", "")),
        title = p.optString("title", p.optString("Title", "")),
        priceVnd = p.optLong("price", p.optLong("price_vnd", 0L)),
        imageUrl = p.optString("image_url", p.optString("thumbnail_url", p.optString("first_image_url", ""))),
    )

    private fun parseMessageObj(m: JSONObject) = ChatMessage(
        messageId = m.optString("message_id", m.optString("id", "")),
        text = m.optString("text", m.optString("content", "")),
        isFromMe = m.optBoolean("is_from_me", m.optBoolean("from_me", false)),
        timestamp = m.optString("created_at", m.optString("timestamp", m.optString("sent_at", ""))),
        isRead = m.optBoolean("is_read", m.optBoolean("read", false)),
    )

    private fun parseMessage(json: String): ChatMessage {
        val obj = JSONObject(json.trim())
        val data = if (obj.has("data")) obj.getJSONObject("data") else obj
        return parseMessageObj(data)
    }

    private fun parseOfferObj(o: JSONObject) = PriceOffer(
        offerId = o.optString("offer_id", o.optString("id", "")),
        amountVnd = o.optLong("amount_vnd", o.optLong("amount", 0L)),
        proposedByMe = o.optBoolean("proposed_by_me", o.optBoolean("from_me", false)),
        status = o.optString("status", "pending"),
    )

    private fun parseOffer(json: String): PriceOffer {
        val obj = JSONObject(json.trim())
        val data = if (obj.has("data")) obj.getJSONObject("data") else obj
        return parseOfferObj(data)
    }

    private fun parseConversations(json: String): List<ConversationItem> {
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
            val other = o.optJSONObject("other_user") ?: o
            val product = o.optJSONObject("product") ?: o.optJSONObject("listing")
            val lastMsg = o.optJSONObject("last_message") ?: o
            ConversationItem(
                conversationId = o.optString("conversation_id", o.optString("id", "")),
                otherUserId = other.optString("user_id", other.optString("id", "")),
                username = other.optString("username", o.optString("username", "")),
                avatarUrl = other.optString("avatar_url", ""),
                lastMessageText = lastMsg.optString("text", lastMsg.optString("content", o.optString("last_message_text", ""))),
                timestamp = o.optString("updated_at", o.optString("timestamp", o.optString("last_message_at", ""))),
                productThumbnailUrl = product?.optString("image_url", product.optString("thumbnail_url", product.optString("first_image_url", ""))) ?: "",
                productId = product?.optString("listing_id", product.optString("id", "")) ?: "",
                isUnread = o.optBoolean("is_unread", o.optBoolean("unread", false)),
            )
        }
    }
}

data class ConversationDetail(
    val conversationId: String,
    val otherUser: OtherUser,
    val product: ProductCard?,
    val messages: List<ChatMessage>,
    val pendingOffer: PriceOffer?,
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
)

data class ChatMessage(
    val messageId: String,
    val text: String,
    val isFromMe: Boolean,
    val timestamp: String,
    val isRead: Boolean = false,
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
    val avatarUrl: String,
    val lastMessageText: String,
    val timestamp: String,
    val productThumbnailUrl: String,
    val productId: String,
    val isUnread: Boolean,
)
