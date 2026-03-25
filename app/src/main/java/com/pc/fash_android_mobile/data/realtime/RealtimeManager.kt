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
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages the single long-lived WebSocket connection to the Fash realtime service.
 *
 * ### Connection lifecycle
 * - Call [connect] when the user is authenticated; the manager stores the token and opens the
 *   socket automatically.
 * - The socket reconnects with exponential back-off whenever it drops (network error or server
 *   close) until [disconnect] is called.
 * - Call [disconnect] on sign-out so stale tokens cannot be used.
 *
 * ### Chat integration
 * - [subscribeToConversation] and [unsubscribeFromConversation] send lightweight JSON frames so
 *   the server routes **typing** events and room-scoped events to this connection.
 * - `message.new` and `read.receipts` are delivered by **user id** — no subscription needed,
 *   but calling [subscribeToConversation] is still required for typing indicators.
 *
 * All parsed events are emitted on [events] as [RealtimeEvent] values.
 */
class RealtimeManager(
    private val sessionStore: AuthSessionStore,
    /** Base URL of the realtime service, e.g. `http://76.13.211.193/realtime-service/` */
    private val realtimeBaseUrl: String,
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

    /** True when the user is typing — toggle with [sendTypingStart] / [sendTypingStop]. */
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    // ── Internals ─────────────────────────────────────────────────────────────

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val intentionalDisconnect = AtomicBoolean(false)
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null

    private val client: OkHttpClient = OkHttpClient.Builder()
        // OkHttp handles WS ping/pong at transport level
        .pingInterval(25, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)  // no timeout — long-lived connection
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

    /** Closes the socket permanently (on sign-out). Cancels any pending reconnect. */
    fun disconnect() {
        intentionalDisconnect.set(true)
        reconnectJob?.cancel()
        webSocket?.close(CLOSE_NORMAL, "User signed out")
        webSocket = null
        _state.value = State.DISCONNECTED
    }

    /**
     * Sends `subscribe.conversation` so the server routes typing events and room-scoped events
     * to this connection. Safe to call while disconnected — the frame will be dropped silently.
     */
    fun subscribeToConversation(conversationId: String) {
        send(JSONObject().apply {
            put("type", "subscribe.conversation")
            put("conversation_id", conversationId)
        })
    }

    /** Sends `unsubscribe.conversation` to stop receiving typing events for this room. */
    fun unsubscribeFromConversation(conversationId: String) {
        send(JSONObject().apply {
            put("type", "unsubscribe.conversation")
            put("conversation_id", conversationId)
        })
    }

    /** Sends a client `ping` to verify the connection is alive. */
    fun sendPing() {
        send(JSONObject().put("type", "ping"))
    }

    /** Sends `typing.start` for the given conversation. */
    fun sendTypingStart(conversationId: String) {
        send(JSONObject().apply {
            put("type", "typing.start")
            put("conversation_id", conversationId)
        })
        _isTyping.value = true
    }

    /** Sends `typing.stop` for the given conversation. */
    fun sendTypingStop(conversationId: String) {
        send(JSONObject().apply {
            put("type", "typing.stop")
            put("conversation_id", conversationId)
        })
        _isTyping.value = false
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun openSocket() {
        val token = sessionStore.read()?.accessToken ?: run {
            // No session yet — stay disconnected; connect() will be called after login
            return
        }
        _state.value = State.CONNECTING

        val wsUrl = buildWsUrl(token)
        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(ws: WebSocket, response: Response) {
                webSocket = ws
                _state.value = State.CONNECTED
                reconnectJob?.cancel()
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
                if (!intentionalDisconnect.get()) scheduleReconnect()
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                _state.value = State.DISCONNECTED
                _events.tryEmit(RealtimeEvent.Disconnected(willReconnect = !intentionalDisconnect.get()))
                if (!intentionalDisconnect.get()) scheduleReconnect()
            }
        })
    }

    private var reconnectDelay = BASE_RECONNECT_DELAY_MS

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(reconnectDelay)
            reconnectDelay = minOf(reconnectDelay * 2, MAX_RECONNECT_DELAY_MS)
            if (isActive && !intentionalDisconnect.get()) openSocket()
        }
    }

    private fun send(json: JSONObject) {
        webSocket?.send(json.toString())
    }

    /**
     * Builds the WebSocket URL.
     * Converts `http://` → `ws://` and `https://` → `wss://`, then appends `/ws?token=...`.
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

            // First message after connect is not always in the standard envelope
            if (type == "connected") {
                return@runCatching RealtimeEvent.Connected(
                    userId = json.optString("user_id", ""),
                    connId = json.optString("conn_id", ""),
                )
            }

            // All other messages: envelope with type + payload
            val payload = json.optJSONObject("payload") ?: JSONObject()

            when (type) {
                "message.new" -> RealtimeEvent.MessageNew(
                    conversationId = payload.optString("conversation_id", ""),
                    messageId = payload.optString("message_id", ""),
                    senderId = payload.optString("sender_id", ""),
                    recipientId = payload.optString("recipient_id", ""),
                    preview = payload.optString("preview", ""),
                    messageType = payload.optString("message_type", "text"),
                )
                "read.receipts" -> RealtimeEvent.ReadReceipts(
                    conversationId = payload.optString("conversation_id", ""),
                    readerId = payload.optString("recipient_id", payload.optString("reader_id", "")),
                    notifyUserId = payload.optString("notify_user_id", ""),
                )
                "read.ack" -> RealtimeEvent.ReadReceipts(
                    conversationId = payload.optString("conversation_id", ""),
                    readerId = payload.optString("reader_id", ""),
                    notifyUserId = "",
                )
                "typing.start" -> RealtimeEvent.TypingStart(
                    conversationId = payload.optString("conversation_id", ""),
                    userId = payload.optString("user_id", payload.optString("sender_id", "")),
                )
                "typing.stop" -> RealtimeEvent.TypingStop(
                    conversationId = payload.optString("conversation_id", ""),
                    userId = payload.optString("user_id", payload.optString("sender_id", "")),
                )
                "order.status_changed" -> RealtimeEvent.OrderStatusChanged(
                    orderId = payload.optString("order_id", ""),
                    conversationId = payload.optString("conversation_id", ""),
                    newStatus = payload.optString("status", payload.optString("new_status", "")),
                )
                "feed.refresh" -> RealtimeEvent.FeedRefresh
                "pong" -> RealtimeEvent.Pong
                else -> RealtimeEvent.Unknown(type)
            }
        }.getOrNull() ?: return

        _events.tryEmit(event)

        // Reset reconnect delay on successful message
        if (event is RealtimeEvent.Connected) reconnectDelay = BASE_RECONNECT_DELAY_MS
    }

    companion object {
        private const val CLOSE_NORMAL = 1000
        private const val BASE_RECONNECT_DELAY_MS = 2_000L
        private const val MAX_RECONNECT_DELAY_MS = 30_000L
    }
}
