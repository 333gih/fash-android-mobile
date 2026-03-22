package com.pc.fash_android_mobile.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.chat.ChatRepository
import com.pc.fash_android_mobile.data.chat.ConversationItem
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class ChatFilter(val status: String?, val role: String?) {
    All(null, null),
    Unread("unread", null),
    Seller(null, "seller"),
    Buyer(null, "buyer"),
}

class ChatViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val chatRepository: ChatRepository =
        (application as FashApplication).chatRepository

    private val _conversations = MutableStateFlow<List<ConversationItem>>(emptyList())
    val conversations: StateFlow<List<ConversationItem>> = _conversations.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ChatFilter.All)
    val selectedFilter: StateFlow<ChatFilter> = _selectedFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun loadConversations() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            val filter = _selectedFilter.value
            val result = withContext(Dispatchers.IO) {
                chatRepository.getConversations(
                    limit = 50,
                    offset = 0,
                    status = filter.status,
                    role = filter.role,
                )
            }
            _isLoading.value = false
            result.fold(
                onSuccess = { _conversations.value = it },
                onFailure = {
                    _loadError.value = it.message ?: getApplication<Application>().getString(R.string.chat_load_error)
                    _events.tryEmit(_loadError.value!!)
                },
            )
        }
    }

    fun setFilter(filter: ChatFilter) {
        _selectedFilter.value = filter
        loadConversations()
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
