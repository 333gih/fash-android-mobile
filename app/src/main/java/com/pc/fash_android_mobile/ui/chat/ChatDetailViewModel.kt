package com.pc.fash_android_mobile.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.chat.ChatMessage
import com.pc.fash_android_mobile.data.chat.ChatRepository
import com.pc.fash_android_mobile.data.chat.ConversationDetail
import com.pc.fash_android_mobile.data.chat.PriceOffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatDetailViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val chatRepository: ChatRepository =
        (application as FashApplication).chatRepository

    private val _detail = MutableStateFlow<ConversationDetail?>(null)
    val detail: StateFlow<ConversationDetail?> = _detail.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _isRespondingToOffer = MutableStateFlow(false)
    val isRespondingToOffer: StateFlow<Boolean> = _isRespondingToOffer.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _showOfferDialog = MutableStateFlow(false)
    val showOfferDialog: StateFlow<Boolean> = _showOfferDialog.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun loadConversation(conversationId: String) {
        if (conversationId.isBlank()) {
            _loadError.value = getApplication<Application>().getString(R.string.chat_load_error)
            _isLoading.value = false
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            _detail.value = null
            _messages.value = emptyList()
            val result = withContext(Dispatchers.IO) {
                chatRepository.getConversationDetail(conversationId)
            }
            _isLoading.value = false
            result.fold(
                onSuccess = { d ->
                    _detail.value = d
                    _messages.value = d.messages
                },
                onFailure = {
                    _loadError.value = it.message ?: getApplication<Application>().getString(R.string.chat_load_error)
                    _events.tryEmit(_loadError.value!!)
                },
            )
        }
    }

    fun onInputChange(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        val convId = _detail.value?.conversationId ?: return
        val text = _inputText.value.trim()
        if (text.isBlank() || _isSending.value) return
        viewModelScope.launch {
            _isSending.value = true
            _inputText.value = ""
            val result = withContext(Dispatchers.IO) {
                chatRepository.sendMessage(convId, text)
            }
            _isSending.value = false
            result.fold(
                onSuccess = { msg ->
                    _messages.value = _messages.value + msg
                },
                onFailure = {
                    _inputText.value = text
                    _events.tryEmit(it.message ?: getApplication<Application>().getString(R.string.chat_send_error))
                },
            )
        }
    }

    fun onSetPriceClick() {
        _showOfferDialog.value = true
    }

    fun dismissOfferDialog() {
        _showOfferDialog.value = false
    }

    fun createOffer(amountVnd: Long) {
        val d = _detail.value ?: return
        val listingId = d.product?.listingId ?: return
        viewModelScope.launch {
            _showOfferDialog.value = false
            val result = withContext(Dispatchers.IO) {
                chatRepository.createOffer(d.conversationId, listingId, amountVnd)
            }
            result.fold(
                onSuccess = { offer ->
                    _detail.value = d.copy(pendingOffer = offer)
                },
                onFailure = {
                    _events.tryEmit(it.message ?: getApplication<Application>().getString(R.string.chat_offer_error))
                },
            )
        }
    }

    fun acceptOffer(offer: PriceOffer) {
        viewModelScope.launch {
            _isRespondingToOffer.value = true
            val result = withContext(Dispatchers.IO) {
                chatRepository.respondToOffer(offer.offerId, true)
            }
            _isRespondingToOffer.value = false
            result.fold(
                onSuccess = {
                    _detail.value = _detail.value?.copy(pendingOffer = null)
                    _events.tryEmit(getApplication<Application>().getString(R.string.chat_offer_accepted))
                },
                onFailure = {
                    _events.tryEmit(it.message ?: getApplication<Application>().getString(R.string.chat_offer_error))
                },
            )
        }
    }

    fun declineOffer(offer: PriceOffer) {
        viewModelScope.launch {
            _isRespondingToOffer.value = true
            val result = withContext(Dispatchers.IO) {
                chatRepository.respondToOffer(offer.offerId, false)
            }
            _isRespondingToOffer.value = false
            result.fold(
                onSuccess = {
                    _detail.value = _detail.value?.copy(pendingOffer = null)
                },
                onFailure = {
                    _events.tryEmit(it.message ?: getApplication<Application>().getString(R.string.chat_offer_error))
                },
            )
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
