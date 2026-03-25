package com.pc.fash_android_mobile.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.chat.ChatMessage
import com.pc.fash_android_mobile.data.chat.ChatRepository
import com.pc.fash_android_mobile.data.chat.ConversationDetail
import com.pc.fash_android_mobile.data.chat.ConversationItem
import com.pc.fash_android_mobile.data.chat.OtherUser
import com.pc.fash_android_mobile.data.chat.ProductCard
import com.pc.fash_android_mobile.data.chat.PriceOffer
import com.pc.fash_android_mobile.data.order.OrderRepository
import com.pc.fash_android_mobile.data.realtime.RealtimeEvent
import com.pc.fash_android_mobile.data.realtime.RealtimeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Polling is used only as a fallback when the WebSocket is not connected.
 * When the WS is healthy, message.new events trigger immediate HTTP refreshes.
 */
private const val POLL_INTERVAL_FALLBACK_MS = 30_000L

class ChatDetailViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val chatRepository: ChatRepository =
        (application as FashApplication).chatRepository
    private val orderRepository: OrderRepository =
        (application as FashApplication).orderRepository
    private val realtimeManager: RealtimeManager =
        (application as FashApplication).realtimeManager
    private val sessionStore =
        (application as FashApplication).authManager.sessionStore

    // ── UI state ──────────────────────────────────────────────────────────

    private val _detail = MutableStateFlow<ConversationDetail?>(null)
    val detail: StateFlow<ConversationDetail?> = _detail.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isMessagesLoading = MutableStateFlow(false)
    val isMessagesLoading: StateFlow<Boolean> = _isMessagesLoading.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _isRespondingToOffer = MutableStateFlow(false)
    val isRespondingToOffer: StateFlow<Boolean> = _isRespondingToOffer.asStateFlow()

    private val _isCreatingOffer = MutableStateFlow(false)
    val isCreatingOffer: StateFlow<Boolean> = _isCreatingOffer.asStateFlow()

    /**
     * Non-null when the conversation has an associated order (deal done — seller accepted).
     * Drives the STATE A → STATE B transition.
     */
    private val _orderId = MutableStateFlow<String?>(null)
    val orderId: StateFlow<String?> = _orderId.asStateFlow()

    /** Status of the order once [orderId] is known. */
    private val _orderStatus = MutableStateFlow<String?>(null)
    val orderStatus: StateFlow<String?> = _orderStatus.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _showOfferDialog = MutableStateFlow(false)
    val showOfferDialog: StateFlow<Boolean> = _showOfferDialog.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    // Kept for backward compat with MainActivity callers — never emitted under new offer flow.
    private val _acceptedOfferForCheckout = MutableStateFlow<AcceptedOfferForCheckout?>(null)
    val acceptedOfferForCheckout: StateFlow<AcceptedOfferForCheckout?> =
        _acceptedOfferForCheckout.asStateFlow()

    /** True while the OTHER participant is typing (for the typing indicator). */
    private val _isOtherTyping = MutableStateFlow(false)
    val isOtherTyping: StateFlow<Boolean> = _isOtherTyping.asStateFlow()

    // ── WebSocket + fallback polling ──────────────────────────────────────────

    private var wsJob: Job? = null
    private var pollingJob: Job? = null
    private var typingTimeoutJob: Job? = null

    override fun onCleared() {
        super.onCleared()
        wsJob?.cancel()
        pollingJob?.cancel()
        typingTimeoutJob?.cancel()
        _detail.value?.conversationId?.let { realtimeManager.unsubscribeFromConversation(it) }
    }

    /**
     * Starts the WebSocket listener for this conversation and a 30-second fallback poll.
     * WS events trigger immediate HTTP refreshes for full message data.
     */
    private fun startRealtimeAndPolling(conversationId: String) {
        realtimeManager.subscribeToConversation(conversationId)

        wsJob?.cancel()
        wsJob = viewModelScope.launch {
            realtimeManager.events.collect { event ->
                when (event) {
                    is RealtimeEvent.MessageNew -> {
                        if (event.conversationId == conversationId) {
                            // Lightweight signal — fetch full data via HTTP
                            silentPoll(conversationId)
                        }
                    }
                    is RealtimeEvent.TypingStart -> {
                        if (event.conversationId == conversationId) {
                            val myId = sessionStore.read()?.userId.orEmpty()
                            if (event.userId != myId) {
                                _isOtherTyping.value = true
                                scheduleTypingTimeout()
                            }
                        }
                    }
                    is RealtimeEvent.TypingStop -> {
                        if (event.conversationId == conversationId) {
                            _isOtherTyping.value = false
                            typingTimeoutJob?.cancel()
                        }
                    }
                    is RealtimeEvent.OrderStatusChanged -> {
                        val knownOrderId = _orderId.value
                        when {
                            knownOrderId != null && event.orderId == knownOrderId ->
                                _orderStatus.value = event.newStatus
                            event.conversationId == conversationId && knownOrderId == null ->
                                viewModelScope.launch { checkForOrderId(conversationId) }
                        }
                    }
                    else -> Unit
                }
            }
        }

        // 30-second fallback — keeps messages fresh when WS is disconnected
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_FALLBACK_MS)
                if (!isActive) break
                silentPoll(conversationId)
            }
        }
    }

    private fun scheduleTypingTimeout() {
        typingTimeoutJob?.cancel()
        typingTimeoutJob = viewModelScope.launch {
            delay(5_000L)
            _isOtherTyping.value = false
        }
    }

    private suspend fun silentPoll(conversationId: String) {
        val msgResult = withContext(Dispatchers.IO) { chatRepository.getMessages(conversationId) }
        msgResult.getOrNull()?.let { newMsgs ->
            if (newMsgs != _messages.value) {
                _messages.value = newMsgs
                syncPendingOfferFromMessages(newMsgs, conversationId)
            }
        }
    }

    // ── Entry points ──────────────────────────────────────────────────────

    /**
     * Primary entry when navigating from the conversation list.
     * 1. Instantly builds [ConversationDetail] from [item] (no network, fast display).
     * 2. Concurrently fetches messages + full conversation detail (for orderId, listing status).
     * 3. Starts the 3-second polling loop.
     */
    fun loadFromItem(item: ConversationItem) {
        pollingJob?.cancel()
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            _messages.value = emptyList()
            _orderId.value = null
            _orderStatus.value = null

            val myUserId = sessionStore.read()?.userId?.trim().orEmpty()
            val isBuyer = when {
                myUserId.isNotBlank() && item.buyerUserId.isNotBlank() -> myUserId == item.buyerUserId
                else -> true
            }

            // Instant display from list data
            _detail.value = ConversationDetail(
                conversationId = item.conversationId,
                otherUser = OtherUser(
                    userId = item.otherUserId,
                    displayName = item.displayName,
                    username = item.username,
                    avatarUrl = item.avatarUrl,
                ),
                product = if (item.productId.isNotBlank()) ProductCard(
                    listingId = item.productId,
                    title = item.productTitle,
                    priceVnd = item.productPrice,
                    imageUrl = item.productThumbnailUrl,
                ) else null,
                messages = emptyList(),
                pendingOffer = null,
                isBuyer = isBuyer,
                orderId = null,
            )

            // Parallel: (1) fetch messages, (2) fetch full detail for order_id + listing status
            val msgJob = launch {
                _isMessagesLoading.value = true
                val result = withContext(Dispatchers.IO) {
                    chatRepository.getMessages(item.conversationId)
                }
                _isMessagesLoading.value = false
                result.getOrNull()?.let { msgs ->
                    _messages.value = msgs
                    syncPendingOfferFromMessages(msgs, item.conversationId)
                }
                withContext(Dispatchers.IO) {
                    runCatching { chatRepository.markConversationRead(item.conversationId) }
                }
            }

            val detailJob = launch {
                val result = withContext(Dispatchers.IO) {
                    chatRepository.getConversationDetail(item.conversationId)
                }
                result.getOrNull()?.let { d -> applyConversationDetail(d) }
            }

            msgJob.join()
            detailJob.join()
            _isLoading.value = false
            startRealtimeAndPolling(item.conversationId)
        }
    }

    /**
     * Fallback loader used when only a conversation ID is available (deep link / notification).
     * If already pre-loaded via [loadFromItem], refreshes messages only.
     */
    fun loadConversation(conversationId: String) {
        if (conversationId.isBlank()) {
            _loadError.value = getApplication<Application>().getString(R.string.chat_load_error)
            _isLoading.value = false
            return
        }
        val existing = _detail.value
        if (existing?.conversationId == conversationId) {
            viewModelScope.launch {
                val msgResult = withContext(Dispatchers.IO) { chatRepository.getMessages(conversationId) }
                msgResult.getOrNull()?.let { msgs ->
                    _messages.value = msgs
                    syncPendingOfferFromMessages(msgs, conversationId)
                }
            }
            return
        }
        pollingJob?.cancel()
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            _detail.value = null
            _messages.value = emptyList()
            _orderId.value = null
            _orderStatus.value = null

            val result = withContext(Dispatchers.IO) {
                chatRepository.getConversationDetail(conversationId)
            }
            result.fold(
                onSuccess = { d ->
                    applyConversationDetail(d)
                    val msgResult = withContext(Dispatchers.IO) { chatRepository.getMessages(conversationId) }
                    msgResult.getOrNull()?.let { msgs ->
                        _messages.value = msgs
                        syncPendingOfferFromMessages(msgs, conversationId)
                    }
                    withContext(Dispatchers.IO) {
                        runCatching { chatRepository.markConversationRead(conversationId) }
                    }
                    _isLoading.value = false
                    startRealtimeAndPolling(conversationId)
                },
                onFailure = {
                    _isLoading.value = false
                    _loadError.value = it.message
                        ?: getApplication<Application>().getString(R.string.chat_load_error)
                    _events.tryEmit(_loadError.value!!)
                },
            )
        }
    }

    // ── Messaging ─────────────────────────────────────────────────────────

    fun onInputChange(text: String) {
        _inputText.value = text
        // Send typing indicators via WebSocket
        val convId = _detail.value?.conversationId ?: return
        if (text.isNotBlank()) {
            realtimeManager.sendTypingStart(convId)
        } else {
            realtimeManager.sendTypingStop(convId)
        }
    }

    fun sendMessage() {
        val convId = _detail.value?.conversationId ?: return
        val text = _inputText.value.trim()
        if (text.isBlank() || _isSending.value) return
        viewModelScope.launch {
            _isSending.value = true
            _inputText.value = ""
            val result = withContext(Dispatchers.IO) { chatRepository.sendMessage(convId, text) }
            _isSending.value = false
            result.fold(
                onSuccess = { msg -> _messages.value = _messages.value + msg },
                onFailure = {
                    _inputText.value = text
                    _events.tryEmit(
                        it.message ?: getApplication<Application>().getString(R.string.chat_send_error),
                    )
                },
            )
        }
    }

    // ── Offer actions ─────────────────────────────────────────────────────

    fun onSetPriceClick() { _showOfferDialog.value = true }
    fun dismissOfferDialog() { _showOfferDialog.value = false }

    fun createOffer(amountVnd: Long) {
        val d = _detail.value ?: return
        if (_orderId.value != null) {
            _events.tryEmit(getApplication<Application>().getString(R.string.chat_error_order_exists))
            return
        }
        viewModelScope.launch {
            _showOfferDialog.value = false
            _isCreatingOffer.value = true
            val result = withContext(Dispatchers.IO) {
                chatRepository.createOffer(d.conversationId, amountVnd)
            }
            result.fold(
                onSuccess = { offer ->
                    _detail.value = d.copy(pendingOffer = offer.copy(proposedByMe = true))
                    refreshMessages(d.conversationId)
                },
                onFailure = { e ->
                    _isCreatingOffer.value = false
                    _events.tryEmit(mapOfferError(e))
                },
            )
        }
    }

    fun acceptOffer(offer: PriceOffer) {
        val convId = _detail.value?.conversationId ?: return
        viewModelScope.launch {
            _isRespondingToOffer.value = true
            val result = withContext(Dispatchers.IO) {
                chatRepository.respondToOffer(convId, offer.offerId, true)
            }
            _isRespondingToOffer.value = false
            result.fold(
                onSuccess = {
                    _detail.value = _detail.value?.copy(pendingOffer = null)
                    _events.tryEmit(getApplication<Application>().getString(R.string.chat_offer_accepted))
                    refreshMessages(convId)
                    // Backend auto-creates order on acceptance — fetch to get orderId
                    checkForOrderId(convId)
                },
                onFailure = {
                    _events.tryEmit(
                        it.message ?: getApplication<Application>().getString(R.string.chat_offer_error),
                    )
                },
            )
        }
    }

    fun declineOffer(offer: PriceOffer) {
        val convId = _detail.value?.conversationId ?: return
        viewModelScope.launch {
            _isRespondingToOffer.value = true
            val result = withContext(Dispatchers.IO) {
                chatRepository.respondToOffer(convId, offer.offerId, false)
            }
            _isRespondingToOffer.value = false
            result.fold(
                onSuccess = {
                    _detail.value = _detail.value?.copy(pendingOffer = null)
                    refreshMessages(convId)
                },
                onFailure = {
                    _events.tryEmit(
                        it.message ?: getApplication<Application>().getString(R.string.chat_offer_error),
                    )
                },
            )
        }
    }

    fun deleteMessage(message: ChatMessage) {
        if (!message.isFromMe) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { chatRepository.deleteMessage(message.messageId) }
            result.fold(
                onSuccess = {
                    _messages.value = _messages.value.filter { it.messageId != message.messageId }
                },
                onFailure = {
                    _events.tryEmit(
                        it.message ?: getApplication<Application>().getString(R.string.chat_load_error),
                    )
                },
            )
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    /**
     * Merges fresh [ConversationDetail] into current state and triggers order fetching
     * when a new non-null [orderId] is discovered.
     */
    private fun applyConversationDetail(d: ConversationDetail) {
        val current = _detail.value
        _detail.value = if (current != null) {
            current.copy(
                otherUser = d.otherUser,
                product = d.product ?: current.product,
                isBuyer = d.isBuyer,
                orderId = d.orderId ?: current.orderId,
            )
        } else {
            d
        }
        val newOrderId = d.orderId
        if (newOrderId != null && _orderId.value == null) {
            _orderId.value = newOrderId
            viewModelScope.launch { fetchOrderStatus(newOrderId) }
        }
    }

    /**
     * Fetches full conversation detail to detect newly set [orderId].
     * Skips the network call if we already have an [orderId].
     */
    private suspend fun checkForOrderId(conversationId: String) {
        if (_orderId.value != null) return
        val result = withContext(Dispatchers.IO) {
            chatRepository.getConversationDetail(conversationId)
        }
        result.getOrNull()?.let { d ->
            val ordId = d.orderId ?: return
            if (_orderId.value == null) {
                _orderId.value = ordId
                _detail.value = _detail.value?.copy(orderId = ordId)
                fetchOrderStatus(ordId)
            }
            // Reflect any listing status change (e.g., sold/reserved)
            d.product?.let { fresh ->
                val cur = _detail.value?.product
                if (cur != null && fresh.listingStatus != cur.listingStatus) {
                    _detail.value = _detail.value?.copy(product = cur.copy(listingStatus = fresh.listingStatus))
                }
            }
        }
    }

    private suspend fun fetchOrderStatus(orderId: String) {
        val result = withContext(Dispatchers.IO) { orderRepository.getOrder(orderId) }
        result.getOrNull()?.let { order -> _orderStatus.value = order.status }
    }

    /** Refreshes the message list silently; resets loading flags on completion. */
    private suspend fun refreshMessages(conversationId: String) {
        _isMessagesLoading.value = true
        val result = withContext(Dispatchers.IO) { chatRepository.getMessages(conversationId) }
        _isMessagesLoading.value = false
        _isCreatingOffer.value = false
        result.getOrNull()?.takeIf { it.isNotEmpty() }?.let { msgs ->
            _messages.value = msgs
            syncPendingOfferFromMessages(msgs, conversationId)
        }
    }

    /**
     * Scans [messages] for the most-recent offer and syncs [ConversationDetail.pendingOffer]:
     * - Offer from other party + pending  → seller should see Accept/Decline
     * - Offer from me + pending           → buyer is waiting (offer button disabled)
     * - Any offer with accepted status     → trigger [checkForOrderId] to get the auto-created order
     * - Otherwise                          → clear pendingOffer
     */
    private fun syncPendingOfferFromMessages(messages: List<ChatMessage>, conversationId: String) {
        val detail = _detail.value ?: return
        val latestOffer = messages
            .filter { it.messageType == "offer" }
            .maxByOrNull { it.timestamp }
            ?: run {
                if (detail.pendingOffer != null) _detail.value = detail.copy(pendingOffer = null)
                return
            }

        val newPendingOffer: PriceOffer? = when {
            latestOffer.offerStatus == "pending" && !latestOffer.isFromMe ->
                PriceOffer(
                    offerId = latestOffer.messageId,
                    amountVnd = latestOffer.offerAmountVnd,
                    proposedByMe = false,
                    status = "pending",
                )
            latestOffer.offerStatus == "pending" && latestOffer.isFromMe ->
                PriceOffer(
                    offerId = latestOffer.messageId,
                    amountVnd = latestOffer.offerAmountVnd,
                    proposedByMe = true,
                    status = "pending",
                )
            latestOffer.offerStatus == "accepted" && _orderId.value == null -> {
                // Order was auto-created when seller accepted — fetch it
                viewModelScope.launch { checkForOrderId(conversationId) }
                null
            }
            else -> null
        }

        if (newPendingOffer != detail.pendingOffer) {
            _detail.value = detail.copy(pendingOffer = newPendingOffer)
        }
    }

    /** Maps HTTP error codes from the offer endpoints to user-facing Vietnamese strings. */
    private fun mapOfferError(e: Throwable): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("409") && msg.contains("PENDING_OFFER", ignoreCase = true) ->
                getApplication<Application>().getString(R.string.chat_error_pending_offer)
            msg.contains("409") && msg.contains("pending offer", ignoreCase = true) ->
                getApplication<Application>().getString(R.string.chat_error_pending_offer)
            msg.contains("409") ->
                getApplication<Application>().getString(R.string.chat_error_order_exists)
            msg.contains("403") ->
                getApplication<Application>().getString(R.string.chat_error_forbidden)
            msg.contains("404") ->
                getApplication<Application>().getString(R.string.chat_error_not_found)
            else -> msg.ifBlank { getApplication<Application>().getString(R.string.chat_offer_error) }
        }
    }

    fun formatTime(timestamp: String): String {
        if (timestamp.isBlank()) return ""
        return try {
            val instant = java.time.Instant.parse(
                when {
                    timestamp.contains("T") -> timestamp
                    timestamp.contains(" ") -> timestamp.replace(" ", "T")
                    else -> "${timestamp}T00:00:00Z"
                },
            )
            instant.atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm", java.util.Locale.getDefault()))
        } catch (_: Exception) {
            timestamp.take(5)
        }
    }
}

/** Kept for backward compat with MainActivity — not emitted under the new offer→order flow. */
data class AcceptedOfferForCheckout(
    val listingId: String,
    val acceptedAmountVnd: Long,
)
