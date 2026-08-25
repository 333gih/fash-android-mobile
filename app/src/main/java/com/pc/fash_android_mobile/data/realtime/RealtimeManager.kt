package com.pc.fash_android_mobile.data.realtime

import com.pc.fash_android_mobile.data.auth.AuthSessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reads the first non-blank string among [keys]. Core-service / Redis often use PascalCase
 * (`ConversationID`) while the integration doc shows snake_case (`conversation_id`).
 */
private fun JSONObject.firstNonBlank(vararg keys: String): String {
    keys.forEach { key ->
        if (has(key) && !isNull(key)) {
            val s = optString(key, "").trim()
            if (s.isNotBlank()) return s
        }
    }
    return ""
}

/** Tries [payload] first, then envelope [root] (some gateways put fields next to `type`/`ts`). */
private fun firstNonBlankPayload(payload: JSONObject, root: JSONObject, vararg keys: String): String {
    val a = payload.firstNonBlank(*keys)
    if (a.isNotBlank()) return a
    return root.firstNonBlank(*keys)
}

/**
 * Manages the single long-lived WebSocket connection to the Fash realtime service.
 *
 * ### INTEGRATION.md compliance
 * - §2.1 Auth: the **auth-service access JWT** from [sessionStore] is sent **twice** on the
 *   handshake — URL-encoded `?token=…` (INTEGRATION.md) **and** `Authorization: Bearer …` so
 *   API gateways that only inspect headers still validate the same token.
 * - §2.1 Step 4 : OkHttp handles WS-level pings automatically via [pingInterval]
 * - §2.1 Step 5 : [subscribeToConversation] tracks rooms so they are **re-subscribed
 *                 automatically on every reconnect** (fixes the silent-drop + reconnect bugs)
 * - §3.1        : When the server returns 401, [tokenRefresher] is called before retrying
 *                 so we never loop with a stale token
 * - §3.3        : Conversation subscriptions survive disconnects via [subscribedConversations]
 *
 * ### Delivery model (from §2.2)
 * - `message.new` and `read.receipts` are delivered by **user-id** — no subscription needed
 * - `typing.*` events require the room subscription sent by [subscribeToConversation]
 */
class RealtimeManager(
    private val sessionStore: AuthSessionStore,
    /** Base URL of the realtime service, e.g. `http://76.13.211.193/realtime-service/` */
    private val realtimeBaseUrl: String,
    /**
     * Optional callback invoked when the server returns HTTP 401 on the WS handshake.
     * Per INTEGRATION.md §3.1: refresh the access token and return the new value, or
     * return null if refresh is impossible (user will be signed out by the auth layer).
     */
    private val tokenRefresher: (suspend () -> String?)? = null,
) {
    // ── State ─────────────────────────────────────────────────────────────────

    enum class State { DISCONNECTED, CONNECTING, CONNECTED }

    private val _state = MutableStateFlow(State.DISCONNECTED)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _events = MutableSharedFlow<RealtimeEvent>(
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()

    /** True while the local user is typing — toggled by [sendTypingStart]/[sendTypingStop]. */
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    // ── Internals ─────────────────────────────────────────────────────────────

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val intentionalDisconnect = AtomicBoolean(false)
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null

    /**
     * Tracks every conversation we subscribed to so they can be re-sent after each reconnect.
     * INTEGRATION.md §2.1 Step 5 + §3.3: subscriptions must survive disconnections.
     */
    private val subscribedConversations: MutableSet<String> =
        Collections.synchronizedSet(mutableSetOf())

    /** Tracked listing rooms (`subscribe.listing`) — re-sent after reconnect. */
    private val subscribedListings: MutableSet<String> =
        Collections.synchronizedSet(mutableSetOf())

    /** Outbound frames queued while the socket is still connecting (typing/subscribe). */
    private val pendingOutbound = Collections.synchronizedList(mutableListOf<JSONObject>())

    private val client: OkHttpClient = OkHttpClient.Builder()
        // Transport-level keep-alive (OkHttp answers server ping frames automatically)
        .pingInterval(25, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)   // no timeout — long-lived connection
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Opens the WebSocket using the current access token from [sessionStore].
     * No-op if already connecting or connected.
     */
    fun connect() {
        if (_state.value != State.DISCONNECTED) return
        intentionalDisconnect.set(false)
        openSocket()
    }

    /** Closes the socket when the app backgrounds; subscriptions are kept for reconnect. */
    fun pauseForBackground() {
        intentionalDisconnect.set(true)
        reconnectJob?.cancel()
        val ws = webSocket
        if (_state.value == State.CONNECTED && ws != null) {
            sendNow(
                JSONObject().apply {
                    put("type", "presence")
                    put("state", "background")
                },
            )
            scope.launch {
                delay(80)
                runCatching { ws.close(CLOSE_NORMAL, "App background") }
                if (webSocket === ws) {
                    webSocket = null
                    _state.value = State.DISCONNECTED
                }
            }
            return
        }
        webSocket?.close(CLOSE_NORMAL, "App background")
        webSocket = null
        _state.value = State.DISCONNECTED
    }

    /** Closes the socket permanently (on sign-out). Cancels any pending reconnect. */
    fun disconnect() {
        intentionalDisconnect.set(true)
        reconnectJob?.cancel()
        webSocket?.close(CLOSE_NORMAL, "User signed out")
        webSocket = null
        _state.value = State.DISCONNECTED
        subscribedConversations.clear()
        subscribedListings.clear()
        synchronized(pendingOutbound) { pendingOutbound.clear() }
    }

    /**
     * Sends `subscribe.conversation` and tracks the room so it is **automatically re-subscribed
     * after every reconnect**.
     *
     * BUG FIX (§2.1 Step 5): previously the message was silently dropped when the WS was still
     * in CONNECTING state and was never re-sent after a reconnect.
     */
    fun subscribeToConversation(conversationId: String) {
        val cid = normalizeConversationId(conversationId)
        if (cid.isEmpty()) return
        subscribedConversations.add(cid)
        send(
            JSONObject().apply {
                put("type", "subscribe.conversation")
                put("conversation_id", cid)
            },
        )
    }

    /** Sends `unsubscribe.conversation` and stops tracking the room. */
    fun unsubscribeFromConversation(conversationId: String) {
        val cid = normalizeConversationId(conversationId)
        if (cid.isEmpty()) return
        subscribedConversations.remove(cid)
        send(
            JSONObject().apply {
                put("type", "unsubscribe.conversation")
                put("conversation_id", cid)
            },
        )
    }

    /** Sends `subscribe.listing` and tracks the room for reconnect (INTEGRATION.md §2.5). */
    fun subscribeToListing(listingId: String) {
        if (listingId.isBlank()) return
        subscribedListings.add(listingId)
        if (_state.value == State.CONNECTED) {
            send(JSONObject().apply {
                put("type", "subscribe.listing")
                put("listing_id", listingId)
            })
        }
    }

    fun unsubscribeFromListing(listingId: String) {
        if (listingId.isBlank()) return
        subscribedListings.remove(listingId)
        send(JSONObject().apply {
            put("type", "unsubscribe.listing")
            put("listing_id", listingId)
        })
    }

    /** Sends a client `ping` to verify the connection is alive. */
    fun sendPing() {
        send(JSONObject().put("type", "ping"))
    }

    /** Notifies the server the app is foreground/active (presence v2). */
    fun sendPresenceActive() {
        send(
            JSONObject().apply {
                put("type", "presence")
                put("state", "active")
            },
        )
    }

    /** Notifies the server the app is backgrounded so FCM is not suppressed. */
    fun sendPresenceBackground() {
        send(
            JSONObject().apply {
                put("type", "presence")
                put("state", "background")
            },
        )
    }

    /** Sends `typing.start` for the given conversation. */
    fun sendTypingStart(conversationId: String) {
        val cid = normalizeConversationId(conversationId)
        if (cid.isEmpty()) return
        send(
            JSONObject().apply {
                put("type", "typing.start")
                put("conversation_id", cid)
            },
        )
        _isTyping.value = true
    }

    /** Sends `typing.stop` for the given conversation. */
    fun sendTypingStop(conversationId: String) {
        val cid = normalizeConversationId(conversationId)
        if (cid.isEmpty()) return
        send(
            JSONObject().apply {
                put("type", "typing.stop")
                put("conversation_id", cid)
            },
        )
        _isTyping.value = false
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun openSocket() {
        val token = sessionStore.read()?.accessToken ?: return
        _state.value = State.CONNECTING

        val wsUrl = buildWsUrl(token)
        val request = Request.Builder()
            .url(wsUrl)
            .header("Authorization", "Bearer $token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(ws: WebSocket, response: Response) {
                webSocket = ws
                _state.value = State.CONNECTED
                reconnectJob?.cancel()
                flushPendingOutbound()
                resubscribeAll()
            }

            override fun onMessage(ws: WebSocket, text: String) {
                parseAndEmit(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(CLOSE_NORMAL, null)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                _state.value = State.DISCONNECTED
                _events.tryEmit(RealtimeEvent.Disconnected(willReconnect = !intentionalDisconnect.get()))
                if (!intentionalDisconnect.get()) scheduleReconnect(isAuthFailure = false)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                _state.value = State.DISCONNECTED
                // BUG FIX (§3.1): detect 401 so we can refresh the token before retrying
                val is401 = response?.code == 401
                _events.tryEmit(RealtimeEvent.Disconnected(willReconnect = !intentionalDisconnect.get()))
                if (!intentionalDisconnect.get()) scheduleReconnect(isAuthFailure = is401)
            }
        })
    }

    private var reconnectDelay = BASE_RECONNECT_DELAY_MS

    /**
     * Schedules a reconnect with exponential back-off.
     *
     * BUG FIX (§3.1): when [isAuthFailure] is true (server returned 401), the [tokenRefresher]
     * is called first. Only if the refresh succeeds do we reconnect — using the new token that
     * was saved to [sessionStore] by the refresher. If refresh fails we stop, because the auth
     * layer will sign the user out.
     */
    private fun scheduleReconnect(isAuthFailure: Boolean = false) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(reconnectDelay)
            reconnectDelay = minOf(reconnectDelay * 2, MAX_RECONNECT_DELAY_MS)
            if (!isActive || intentionalDisconnect.get()) return@launch

            if (isAuthFailure && tokenRefresher != null) {
                val freshToken = tokenRefresher.invoke()
                if (freshToken == null) {
                    // Refresh failed → auth layer will sign out user; don't loop
                    return@launch
                }
                // freshToken is already persisted to sessionStore by the caller;
                // openSocket() will read it from there
            }

            openSocket()
        }
    }

    /**
     * Re-sends `subscribe.conversation` for every tracked room.
     * Called after each `connected` message (initial connect and every reconnect).
     * Fixes the re-subscribe-after-reconnect bug (INTEGRATION.md §3.3).
     */
    private fun resubscribeAll() {
        synchronized(subscribedConversations) {
            subscribedConversations.forEach { convId ->
                send(JSONObject().apply {
                    put("type", "subscribe.conversation")
                    put("conversation_id", convId)
                })
            }
        }
        synchronized(subscribedListings) {
            subscribedListings.forEach { listingId ->
                send(JSONObject().apply {
                    put("type", "subscribe.listing")
                    put("listing_id", listingId)
                })
            }
        }
    }

    private fun normalizeConversationId(conversationId: String): String =
        conversationId.trim().lowercase()

    private fun sendNow(json: JSONObject) {
        webSocket?.send(json.toString())
    }

    private fun send(json: JSONObject) {
        if (_state.value == State.CONNECTED && webSocket != null) {
            sendNow(json)
            return
        }
        if (intentionalDisconnect.get()) return
        synchronized(pendingOutbound) {
            pendingOutbound.add(json)
            while (pendingOutbound.size > 64) {
                pendingOutbound.removeAt(0)
            }
        }
    }

    private fun flushPendingOutbound() {
        val batch = synchronized(pendingOutbound) {
            pendingOutbound.toList().also { pendingOutbound.clear() }
        }
        batch.forEach { sendNow(it) }
    }

    /**
     * Builds the WebSocket URL with `?token=…&platform=android`.
     * [token] is always the auth-service access JWT read from [sessionStore] after login/refresh.
     * also sets `Authorization: Bearer` in [openSocket] — same value, for gateway parity.
     */
    private fun buildWsUrl(token: String): String {
        val normalised = realtimeBaseUrl.trimEnd('/')
        val wsBase = normalised
            .replace("https://", "wss://")
            .replace("http://", "ws://")
        val encoded = java.net.URLEncoder.encode(token, "UTF-8")
        return "$wsBase/ws?token=$encoded&platform=android"
    }

    private fun parseAndEmit(text: String) {
        val event = runCatching {
            val json = JSONObject(text)
            val type = json.optString("type", "")

            // First message after connect is NOT in the standard envelope (§1.3)
            if (type == "connected") {
                return@runCatching RealtimeEvent.Connected(
                    userId = json.firstNonBlank("user_id", "UserID", "userId"),
                    connId = json.firstNonBlank("conn_id", "ConnID", "connId"),
                )
            }

            // Envelope: { type, payload, ts } — payload may be empty; fields may live on root
            val payload = json.optJSONObject("payload") ?: JSONObject()

            when (type) {
                "message.new" -> {
                    val msgType = firstNonBlankPayload(
                        payload, json,
                        "message_type", "MessageType",
                    ).ifBlank { "text" }
                    val sysSub = firstNonBlankPayload(
                        payload, json,
                        "system_subtype", "system_type", "SystemSubtype", "subtype", "SubType",
                    ).takeIf { it.isNotBlank() }
                    RealtimeEvent.MessageNew(
                        conversationId = firstNonBlankPayload(
                            payload, json,
                            "conversation_id", "ConversationID", "conversationId",
                        ),
                        messageId = firstNonBlankPayload(
                            payload, json,
                            "message_id", "MessageID", "messageId", "ID", "Id",
                        ),
                        senderId = firstNonBlankPayload(
                            payload, json,
                            "sender_id", "SenderID", "senderId",
                        ),
                        recipientId = firstNonBlankPayload(
                            payload, json,
                            "recipient_id", "RecipientID", "recipientId",
                        ),
                        preview = firstNonBlankPayload(payload, json, "preview", "Preview"),
                        messageType = msgType,
                        systemSubtype = sysSub,
                    )
                }
                "read.receipts" -> RealtimeEvent.ReadReceipts(
                    conversationId = firstNonBlankPayload(
                        payload, json,
                        "conversation_id", "ConversationID", "conversationId",
                    ),
                    readerId = firstNonBlankPayload(
                        payload, json,
                        "read_by", "ReadBy", "recipient_id", "RecipientID", "reader_id", "ReaderID",
                    ),
                    notifyUserId = firstNonBlankPayload(
                        payload, json,
                        "notify_user_id", "NotifyUserID",
                    ),
                )
                "read.ack" -> RealtimeEvent.ReadReceipts(
                    conversationId = firstNonBlankPayload(
                        payload, json,
                        "conversation_id", "ConversationID", "conversationId",
                    ),
                    readerId = firstNonBlankPayload(payload, json, "reader_id", "ReaderID"),
                    notifyUserId = "",
                )
                "typing.start" -> RealtimeEvent.TypingStart(
                    conversationId = firstNonBlankPayload(
                        payload, json,
                        "conversation_id", "ConversationID", "conversationId",
                    ),
                    userId = firstNonBlankPayload(
                        payload, json,
                        "user_id", "UserID", "sender_id", "SenderID",
                    ),
                )
                "typing.stop" -> RealtimeEvent.TypingStop(
                    conversationId = firstNonBlankPayload(
                        payload, json,
                        "conversation_id", "ConversationID", "conversationId",
                    ),
                    userId = firstNonBlankPayload(
                        payload, json,
                        "user_id", "UserID", "sender_id", "SenderID",
                    ),
                )
                "order.status_changed" -> RealtimeEvent.OrderStatusChanged(
                    orderId = firstNonBlankPayload(payload, json, "order_id", "OrderID", "orderId"),
                    conversationId = firstNonBlankPayload(
                        payload, json,
                        "conversation_id", "ConversationID", "conversationId",
                    ),
                    newStatus = firstNonBlankPayload(
                        payload, json,
                        "status", "Status", "new_status", "NewStatus",
                    ),
                )
                "offer.limit_reset" -> RealtimeEvent.OfferLimitReset(
                    listingId = firstNonBlankPayload(
                        payload, json,
                        "listing_id", "ListingID", "listingId",
                    ),
                    conversationId = firstNonBlankPayload(
                        payload, json,
                        "conversation_id", "ConversationID", "conversationId",
                    ),
                    newPriceVnd = run {
                        val p = payload.optLong("new_price", payload.optLong("NewPrice", -1L))
                        if (p >= 0) p else json.optLong("new_price", json.optLong("NewPrice", 0L))
                    },
                )
                "listing.reserved" -> RealtimeEvent.ListingReserved(
                    firstNonBlankPayload(payload, json, "listing_id", "ListingID", "listingId"),
                )
                "listing.available" -> RealtimeEvent.ListingAvailable(
                    firstNonBlankPayload(payload, json, "listing_id", "ListingID", "listingId"),
                )
                "listing.sold" -> RealtimeEvent.ListingSold(
                    firstNonBlankPayload(payload, json, "listing_id", "ListingID", "listingId"),
                )
                "conversation.closed" -> RealtimeEvent.ConversationClosed(
                    firstNonBlankPayload(payload, json, "conversation_id", "ConversationID", "conversationId"),
                )
                "conversation.reopened" -> RealtimeEvent.ConversationReopened(
                    firstNonBlankPayload(payload, json, "conversation_id", "ConversationID", "conversationId"),
                )
                "feed.refresh" -> RealtimeEvent.FeedRefresh
                "inbox.refresh" -> RealtimeEvent.InboxRefresh
                "notification.show" -> {
                    val title = firstNonBlankPayload(payload, json, "title", "Title")
                    val body = firstNonBlankPayload(payload, json, "body", "Body")
                    val dataObj = payload.optJSONObject("data") ?: json.optJSONObject("data")
                    val dataMap = if (dataObj != null && dataObj.length() > 0) {
                        buildMap {
                            val it = dataObj.keys()
                            while (it.hasNext()) {
                                val k = it.next()
                                put(k, dataObj.optString(k, "").trim())
                            }
                        }.filterValues { it.isNotEmpty() }.ifEmpty { null }
                    } else {
                        null
                    }
                    val nid = firstNonBlankPayload(
                        payload, json,
                        "user_notification_id", "userNotificationId", "UserNotificationID",
                    ).ifBlank { null }
                    RealtimeEvent.NotificationShow(
                        title = title,
                        body = body,
                        data = dataMap,
                        userNotificationId = nid,
                    )
                }
                "app.promo.show" -> {
                    val campaign = payload.optJSONObject("campaign")
                        ?: json.optJSONObject("campaign")
                        ?: JSONObject()
                    RealtimeEvent.AppPromoShow(campaign)
                }
                "app.status.changed" -> {
                    val src = if (payload.length() > 0) payload else json
                    RealtimeEvent.AppStatusChanged(
                        com.pc.fash_android_mobile.data.appstatus.AppMaintenanceStatus.parse(src),
                    )
                }
                "pong" -> RealtimeEvent.Pong
                else -> RealtimeEvent.Unknown(type)
            }
        }.getOrNull() ?: return

        _events.tryEmit(event)

        if (event is RealtimeEvent.Connected) {
            reconnectDelay = BASE_RECONNECT_DELAY_MS
            // BUG FIX (§2.1 Step 5 + §3.3): re-subscribe to all tracked rooms after every
            // connect/reconnect so typing events work even after network interruptions
            resubscribeAll()
        }
    }

    companion object {
        private const val CLOSE_NORMAL = 1000
        private const val BASE_RECONNECT_DELAY_MS = 2_000L
        private const val MAX_RECONNECT_DELAY_MS = 30_000L
    }
}
