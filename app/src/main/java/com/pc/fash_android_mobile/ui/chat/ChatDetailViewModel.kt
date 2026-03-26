package com.pc.fash_android_mobile.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.BuildConfig
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.chat.ChatMessage
import com.pc.fash_android_mobile.data.chat.ChatRepository
import com.pc.fash_android_mobile.data.chat.OutboundSendState
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
import java.util.UUID

/**
 * Polling is a fallback when `message.new` is missed or the WS is down.
 * When the WS is healthy, `message.new` triggers immediate HTTP refreshes.
 */
private const val POLL_INTERVAL_FALLBACK_MS = 5_000L

/** Coalesce rapid `message.new` / read-receipt bursts into one GET /messages (reduces load). */
private const val SILENT_POLL_DEBOUNCE_MS = 400L

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
    private var silentPollDebounceJob: Job? = null

    override fun onCleared() {
        super.onCleared()
        wsJob?.cancel()
        pollingJob?.cancel()
        typingTimeoutJob?.cancel()
        silentPollDebounceJob?.cancel()
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
                        if (sameConversation(event.conversationId, conversationId)) {
                            when {
                                event.systemSubtype.equals("conversation.closed", ignoreCase = true) ->
                                    applyConversationClosedFromRealtime(conversationId)
                                event.systemSubtype.equals("conversation.reopened", ignoreCase = true) ->
                                    applyConversationReopenedFromRealtime(conversationId)
                                messageNewShouldRefreshChat(event, conversationId) ->
                                    scheduleDebouncedSilentPoll(conversationId)
                            }
                        } else if (messageNewShouldRefreshChat(event, conversationId)) {
                            scheduleDebouncedSilentPoll(conversationId)
                        }
                    }
                    is RealtimeEvent.ReadReceipts -> {
                        // INTEGRATION.md §5: other participant read our messages — refresh to show
                        // updated readAt timestamps on sent bubbles
                        if (sameConversation(event.conversationId, conversationId)) {
                            scheduleDebouncedSilentPoll(conversationId)
                        }
                    }
                    is RealtimeEvent.TypingStart -> {
                        if (sameConversation(event.conversationId, conversationId)) {
                            val myId = sessionStore.read()?.userId?.trim().orEmpty()
                            val other = event.userId.trim()
                            if (other.isNotBlank() && !other.equals(myId, ignoreCase = true)) {
                                _isOtherTyping.value = true
                                scheduleTypingTimeout()
                            }
                        }
                    }
                    is RealtimeEvent.TypingStop -> {
                        if (sameConversation(event.conversationId, conversationId)) {
                            val myId = sessionStore.read()?.userId?.trim().orEmpty()
                            val uid = event.userId.trim()
                            // Ignore stop events that refer to us (echo); otherwise hide "other typing"
                            if (uid.isNotBlank() && uid.equals(myId, ignoreCase = true)) {
                                Unit
                            } else {
                                _isOtherTyping.value = false
                                typingTimeoutJob?.cancel()
                            }
                        }
                    }
                    is RealtimeEvent.OrderStatusChanged -> {
                        val knownOrderId = _orderId.value
                        when {
                            knownOrderId != null && event.orderId == knownOrderId ->
                                _orderStatus.value = event.newStatus
                            sameConversation(event.conversationId, conversationId) && knownOrderId == null ->
                                viewModelScope.launch { checkForOrderId(conversationId) }
                        }
                    }
                    is RealtimeEvent.OfferLimitReset -> {
                        if (sameConversation(event.conversationId, conversationId)) {
                            val d = _detail.value ?: return@collect
                            _detail.value = d.copy(
                                offerCount = 0,
                                product = d.product?.copy(priceVnd = event.newPriceVnd) ?: d.product,
                            )
                            _events.tryEmit(
                                getApplication<Application>().getString(
                                    R.string.chat_offer_limit_reset_banner,
                                    formatVnd(event.newPriceVnd),
                                ),
                            )
                        }
                    }
                    is RealtimeEvent.ListingReserved -> {
                        if (listingIdMatches(event.listingId)) {
                            _detail.value = _detail.value?.copy(
                                isClosed = true,
                                product = _detail.value?.product?.copy(listingStatus = "reserved"),
                            )
                            discardDraftAndStopTyping(conversationId)
                        }
                    }
                    is RealtimeEvent.ListingAvailable -> {
                        if (listingIdMatches(event.listingId)) {
                            val d = _detail.value
                            _detail.value = d?.copy(
                                isClosed = false,
                                offerCount = 0,
                                product = d.product?.copy(listingStatus = "active"),
                            )
                            _events.tryEmit(
                                getApplication<Application>().getString(R.string.chat_reopened_snackbar),
                            )
                        }
                    }
                    is RealtimeEvent.ListingSold -> {
                        if (listingIdMatches(event.listingId)) {
                            _detail.value = _detail.value?.copy(
                                product = _detail.value?.product?.copy(listingStatus = "sold"),
                            )
                            discardDraftAndStopTyping(conversationId)
                        }
                    }
                    is RealtimeEvent.ConversationClosed -> {
                        if (sameConversation(event.conversationId, conversationId)) {
                            applyConversationClosedFromRealtime(conversationId)
                        }
                    }
                    is RealtimeEvent.ConversationReopened -> {
                        if (sameConversation(event.conversationId, conversationId)) {
                            applyConversationReopenedFromRealtime(conversationId)
                        }
                    }
                    else -> Unit
                }
            }
        }

        // Periodic fallback when WS signals are missed
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

    /**
     * Batches rapid WebSocket-driven refreshes so we do not issue back-to-back GET /messages
     * when Redis emits several events in a row.
     */
    private fun scheduleDebouncedSilentPoll(conversationId: String) {
        silentPollDebounceJob?.cancel()
        silentPollDebounceJob = viewModelScope.launch {
            delay(SILENT_POLL_DEBOUNCE_MS)
            silentPoll(conversationId)
        }
    }

    /**
     * Collapses duplicate [ChatMessage.messageId] rows (race: silentPoll + send success both add
     * the same server id). Last row wins so the newest snapshot is kept.
     */
    private fun dedupeMessagesByIdPreferLast(messages: List<ChatMessage>): List<ChatMessage> =
        messages
            .groupBy { it.messageId }
            .map { (_, rows) -> rows.last() }
            .sortedBy { it.timestamp }

    /**
     * Server list is authoritative. Keeps in-flight optimistic rows (`local-*` ids) until the
     * same text appears from the API (then the duplicate pending row is dropped).
     */
    private fun mergeServerWithPendingLocal(
        server: List<ChatMessage>,
        current: List<ChatMessage>,
    ): List<ChatMessage> {
        val pending = current.filter { it.messageId.startsWith("local-") }
        if (pending.isEmpty()) return dedupeMessagesByIdPreferLast(server)
        val merged = server.toMutableList()
        for (p in pending) {
            val superseded = server.any { s ->
                s.isFromMe &&
                    s.messageType == p.messageType &&
                    s.text == p.text &&
                    p.messageType == "text"
            }
            if (!superseded) merged.add(p)
        }
        return dedupeMessagesByIdPreferLast(merged)
    }

    private suspend fun silentPoll(conversationId: String) {
        coroutineScope {
            val detailDeferred = async(Dispatchers.IO) { chatRepository.getConversationDetail(conversationId) }
            val msgDeferred = async(Dispatchers.IO) { chatRepository.getMessages(conversationId) }
            detailDeferred.await().getOrNull()?.let { applyConversationDetail(it) }
            msgDeferred.await().getOrNull()?.let { newMsgs ->
                val merged = mergeServerWithPendingLocal(newMsgs, _messages.value)
                if (merged != _messages.value) {
                    _messages.value = merged
                    syncPendingOfferFromMessages(merged, conversationId)
                    syncDetailClosedStateFromMessages(merged)
                }
            }
        }
    }

    /** WebSocket payloads may use different casing; backend UUID strings should still match. */
    private fun sameConversation(eventConvId: String, openConvId: String): Boolean =
        eventConvId.isNotBlank() && eventConvId.equals(openConvId, ignoreCase = true)

    /**
     * Triggers REST refresh when `message.new` applies to this thread.
     * Primary match: [RealtimeEvent.MessageNew.conversationId] (now parsed with PascalCase too).
     * Fallback: if the id is missing in the payload, match sender/recipient to this chat's peer.
     */
    private fun messageNewShouldRefreshChat(
        event: RealtimeEvent.MessageNew,
        openConversationId: String,
    ): Boolean {
        if (sameConversation(event.conversationId, openConversationId)) return true
        if (event.conversationId.isNotBlank() &&
            !event.conversationId.equals(openConversationId, ignoreCase = true)
        ) {
            return false
        }
        val myId = sessionStore.read()?.userId?.trim().orEmpty()
        val otherId = _detail.value?.otherUser?.userId?.trim().orEmpty()
        if (myId.isBlank() || otherId.isBlank()) return false
        val fromOther = event.senderId.equals(otherId, ignoreCase = true) &&
            event.senderId.isNotBlank() &&
            !event.senderId.equals(myId, ignoreCase = true)
        val forMe = event.recipientId.isBlank() ||
            event.recipientId.equals(myId, ignoreCase = true)
        return fromOther && forMe
    }

    private fun listingIdMatches(listingId: String): Boolean {
        val lid = _detail.value?.product?.listingId?.trim().orEmpty()
        return lid.isNotBlank() && lid.equals(listingId.trim(), ignoreCase = true)
    }

    private fun discardDraftAndStopTyping(conversationId: String) {
        _inputText.value = ""
        _showOfferDialog.value = false
        realtimeManager.sendTypingStop(conversationId)
    }

    private fun applyConversationClosedFromRealtime(conversationId: String) {
        _detail.value = _detail.value?.copy(isClosed = true)
        discardDraftAndStopTyping(conversationId)
    }

    private fun applyConversationReopenedFromRealtime(conversationId: String) {
        _detail.value = _detail.value?.copy(isClosed = false, offerCount = 0)
        _events.tryEmit(getApplication<Application>().getString(R.string.chat_reopened_snackbar))
    }

    private fun isComposerReadOnly(): Boolean {
        val d = _detail.value ?: return true
        if (d.isClosed) return true
        if (d.product?.listingStatus == "sold") return true
        return false
    }

    /**
     * Derives read-only state from persisted system rows (polling), without snackbars
     * (those are only fired from explicit realtime frames).
     */
    private fun syncDetailClosedStateFromMessages(messages: List<ChatMessage>) {
        val d = _detail.value ?: return
        val closedTs = messages.filter { it.systemSubtype.equals("conversation.closed", ignoreCase = true) }
            .maxOfOrNull { it.timestamp }.orEmpty()
        val reopenTs = messages.filter { it.systemSubtype.equals("conversation.reopened", ignoreCase = true) }
            .maxOfOrNull { it.timestamp }.orEmpty()
        if (closedTs.isBlank() && reopenTs.isBlank()) return
        val shouldClose = when {
            reopenTs.isNotBlank() && closedTs.isNotBlank() -> closedTs > reopenTs
            reopenTs.isNotBlank() -> false
            else -> true
        }
        if (d.isClosed != shouldClose) {
            _detail.value = d.copy(isClosed = shouldClose)
            if (shouldClose) discardDraftAndStopTyping(d.conversationId)
        }
    }

    private fun formatVnd(amount: Long): String {
        val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN"))
        return "₫${formatter.format(amount)}"
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
        silentPollDebounceJob?.cancel()
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
                    syncDetailClosedStateFromMessages(msgs)
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
            // Already showing this thread (e.g. after [loadFromItem]); WS + polling refresh messages.
            return
        }
        pollingJob?.cancel()
        silentPollDebounceJob?.cancel()
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
                        syncDetailClosedStateFromMessages(msgs)
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
        if (isComposerReadOnly()) return
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
        if (isComposerReadOnly()) return
        val convId = _detail.value?.conversationId ?: return
        val text = _inputText.value.trim()
        if (text.isBlank() || _isSending.value) return
        viewModelScope.launch {
            val tempId = "local-${UUID.randomUUID()}"
            val now = java.time.Instant.now().toString()
            val myId = sessionStore.read()?.userId.orEmpty()
            val optimistic = ChatMessage(
                messageId = tempId,
                text = text,
                isFromMe = true,
                timestamp = now,
                isRead = false,
                senderId = myId,
                messageType = "text",
                outboundState = OutboundSendState.SENDING,
            )
            _isSending.value = true
            _inputText.value = ""
            // Field cleared programmatically — OutlinedTextField may not call onValueChange; tell peer we stopped typing
            realtimeManager.sendTypingStop(convId)
            _messages.value = _messages.value + optimistic
            val result = withContext(Dispatchers.IO) { chatRepository.sendMessage(convId, text) }
            _isSending.value = false
            result.fold(
                onSuccess = { msg ->
                    val withoutTemp = _messages.value.filter { it.messageId != tempId }
                    // Race: silentPoll may already have inserted this server id — avoid duplicate keys in LazyColumn
                    val withoutDup = withoutTemp.filter { it.messageId != msg.messageId }
                    _messages.value = (withoutDup + msg).sortedBy { it.timestamp }
                },
                onFailure = {
                    _messages.value = _messages.value.map { row ->
                        if (row.messageId == tempId) row.copy(outboundState = OutboundSendState.FAILED) else row
                    }
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
        if (d.offerCount >= BuildConfig.CHAT_MAX_OFFERS_PER_CONVERSATION) {
            _events.tryEmit(
                getApplication<Application>().getString(
                    R.string.chat_error_offer_limit,
                    BuildConfig.CHAT_MAX_OFFERS_PER_CONVERSATION,
                ),
            )
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
                    _detail.value = d.copy(
                        pendingOffer = offer.copy(proposedByMe = true),
                        offerCount = d.offerCount + 1,
                    )
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
        if (message.messageId.startsWith("local-")) {
            _messages.value = _messages.value.filter { it.messageId != message.messageId }
            return
        }
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
     *
     * Presigned avatar/product URLs often get new query params on every GET; we keep the
     * previous URL when the path matches so [StateFlow] does not emit and Coil does not reload.
     */
    private fun applyConversationDetail(d: ConversationDetail) {
        val current = _detail.value
        val merged: ConversationDetail = if (current != null) {
            val mergedOther = mergeOtherUserStable(current.otherUser, d.otherUser)
            val mergedProduct = mergeProductStable(current.product, d.product ?: current.product)
            current.copy(
                otherUser = mergedOther,
                product = mergedProduct,
                isBuyer = d.isBuyer,
                orderId = d.orderId ?: current.orderId,
                offerCount = d.offerCount,
                isClosed = d.isClosed,
            )
        } else {
            d
        }
        if (current != null && merged == current) {
            // Skip emission — same logical content (avoids image flicker from URL rotation).
        } else {
            _detail.value = merged
        }
        val newOrderId = d.orderId
        if (newOrderId != null && _orderId.value == null) {
            _orderId.value = newOrderId
            viewModelScope.launch { fetchOrderStatus(newOrderId) }
        }
    }

    /** Same file path as [preferred] when query tokens rotate (e.g. presigned URLs). */
    private fun stableImageUrl(preferred: String, candidate: String): String {
        when {
            candidate.isBlank() -> return preferred
            preferred.isBlank() -> return candidate
            preferred == candidate -> return preferred
            else -> {
                val p = preferred.substringBefore('?')
                val c = candidate.substringBefore('?')
                if (p.isNotBlank() && p == c) return preferred
                return candidate
            }
        }
    }

    private fun mergeOtherUserStable(current: OtherUser, fresh: OtherUser): OtherUser {
        val avatar = stableImageUrl(current.avatarUrl, fresh.avatarUrl)
        return fresh.copy(avatarUrl = avatar)
    }

    private fun mergeProductStable(current: ProductCard?, fresh: ProductCard?): ProductCard? {
        if (fresh == null) return current
        if (current == null) return fresh
        if (!current.listingId.equals(fresh.listingId, ignoreCase = true)) return fresh
        return fresh.copy(imageUrl = stableImageUrl(current.imageUrl, fresh.imageUrl))
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
            val merged = mergeServerWithPendingLocal(msgs, _messages.value)
            _messages.value = merged
            syncPendingOfferFromMessages(merged, conversationId)
            syncDetailClosedStateFromMessages(merged)
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
            msg.contains("409") && msg.contains("OFFER_LIMIT_REACHED", ignoreCase = true) ->
                getApplication<Application>().getString(
                    R.string.chat_error_offer_limit,
                    BuildConfig.CHAT_MAX_OFFERS_PER_CONVERSATION,
                )
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
