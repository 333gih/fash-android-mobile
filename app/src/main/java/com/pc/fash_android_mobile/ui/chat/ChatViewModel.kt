package com.pc.fash_android_mobile.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.chat.ChatRepository
import com.pc.fash_android_mobile.data.chat.ConversationItem
import com.pc.fash_android_mobile.data.realtime.RealtimeEvent
import com.pc.fash_android_mobile.data.realtime.RealtimeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Chat list filter — applied client-side after fetching all conversations from the API.
 * The API (GET /chat/conversations) only supports limit/offset; it has no status or role filter.
 */
enum class ChatFilter {
    All,
    Unread,
    Seller,  // conversations where the current user is the seller
    Buyer,   // conversations where the current user is the buyer
}

class ChatViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val chatRepository: ChatRepository =
        (application as FashApplication).chatRepository
    private val realtimeManager: RealtimeManager =
        (application as FashApplication).realtimeManager
    private val sessionStore =
        (application as FashApplication).authManager.sessionStore

    init {
        // Refresh conversation list in real-time when a new message arrives for this user
        viewModelScope.launch {
            realtimeManager.events.collect { event ->
                when (event) {
                    is RealtimeEvent.MessageNew -> silentRefreshConversations()
                    is RealtimeEvent.ReadReceipts -> silentRefreshConversations()
                    else -> Unit
                }
            }
        }
    }

    /** Full unfiltered list returned by the API. */
    private val _allConversations = MutableStateFlow<List<ConversationItem>>(emptyList())

    /** Filtered view shown in the UI. */
    private val _conversations = MutableStateFlow<List<ConversationItem>>(emptyList())
    val conversations: StateFlow<List<ConversationItem>> = _conversations.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ChatFilter.All)
    val selectedFilter: StateFlow<ChatFilter> = _selectedFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun loadConversations() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            val result = withContext(Dispatchers.IO) {
                // API only accepts limit + offset — no status or role params
                chatRepository.getConversations(limit = 50, offset = 0)
            }
            _isLoading.value = false
            result.fold(
                onSuccess = { all ->
                    _allConversations.value = all
                    applyFilter(_selectedFilter.value, all)
                },
                onFailure = {
                    _loadError.value = it.message ?: getApplication<Application>().getString(R.string.chat_load_error)
                    _events.tryEmit(_loadError.value!!)
                },
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            silentRefreshConversations()
            _isRefreshing.value = false
        }
    }

    /** Quietly re-fetches conversations without showing a loading indicator. */
    private suspend fun silentRefreshConversations() {
        val result = withContext(Dispatchers.IO) {
            chatRepository.getConversations(limit = 50, offset = 0)
        }
        result.getOrNull()?.let { all ->
            _allConversations.value = all
            applyFilter(_selectedFilter.value, all)
        }
    }

    fun setFilter(filter: ChatFilter) {
        _selectedFilter.value = filter
        applyFilter(filter, _allConversations.value)
    }

    /**
     * Client-side filter applied after fetching all conversations.
     * - [ChatFilter.All]    — show everything
     * - [ChatFilter.Unread] — items where isUnread == true
     * - [ChatFilter.Seller] — conversations where current user is the seller
     * - [ChatFilter.Buyer]  — conversations where current user is the buyer
     */
    private fun applyFilter(filter: ChatFilter, all: List<ConversationItem>) {
        val myId = sessionStore.read()?.userId.orEmpty()
        _conversations.value = when (filter) {
            ChatFilter.All -> all
            ChatFilter.Unread -> all.filter { it.isUnread }
            ChatFilter.Seller -> if (myId.isBlank()) all else all.filter { it.sellerUserId == myId }
            ChatFilter.Buyer -> if (myId.isBlank()) all else all.filter { it.buyerUserId == myId }
        }
    }

    suspend fun startConversation(listingId: String): Result<String> =
        withContext(Dispatchers.IO) {
            chatRepository.startConversation(listingId)
        }

    fun formatTimestamp(raw: String): String {
        if (raw.isBlank()) return ""
        return try {
            val toParse = when {
                raw.contains("T") -> raw
                raw.contains(" ") -> raw.replace(" ", "T")
                raw.length == 10 -> "${raw}T00:00:00Z"
                else -> raw
            }
            val instant = Instant.parse(toParse)
            val zone = ZoneId.systemDefault()
            val now = Instant.now()
            val diffMs = now.toEpochMilli() - instant.toEpochMilli()
            val diffMins = TimeUnit.MILLISECONDS.toMinutes(diffMs)
            val diffHours = TimeUnit.MILLISECONDS.toHours(diffMs)
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)
            when {
                diffMins < 1 -> getApplication<Application>().getString(R.string.chat_just_now)
                diffMins < 60 -> getApplication<Application>().getString(R.string.chat_mins_ago, diffMins.toInt())
                diffHours < 24 -> getApplication<Application>().getString(R.string.chat_hours_ago, diffHours.toInt())
                diffDays < 7 -> {
                    val date = instant.atZone(zone).toLocalDate()
                    when (date.dayOfWeek.value) {
                        1 -> getApplication<Application>().getString(R.string.chat_day_mon)
                        2 -> getApplication<Application>().getString(R.string.chat_day_tue)
                        3 -> getApplication<Application>().getString(R.string.chat_day_wed)
                        4 -> getApplication<Application>().getString(R.string.chat_day_thu)
                        5 -> getApplication<Application>().getString(R.string.chat_day_fri)
                        6 -> getApplication<Application>().getString(R.string.chat_day_sat)
                        7 -> getApplication<Application>().getString(R.string.chat_day_sun)
                        else -> date.format(DateTimeFormatter.ofPattern("d/M", Locale.getDefault()))
                    }
                }
                else -> instant.atZone(zone).format(DateTimeFormatter.ofPattern("d/M", Locale.getDefault()))
            }
        } catch (_: Exception) {
            raw.take(16)
        }
    }
}
