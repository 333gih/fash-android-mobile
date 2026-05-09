package com.pc.fash_android_mobile.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.data.user.InboxNotificationItem
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository =
        (application as FashApplication).userRepository

    private val _items = MutableStateFlow<List<InboxNotificationItem>>(emptyList())
    val items: StateFlow<List<InboxNotificationItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _loadMoreBusy = MutableStateFlow(false)
    val loadMoreBusy: StateFlow<Boolean> = _loadMoreBusy.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _inboxUnavailable = MutableStateFlow(false)
    val inboxUnavailable: StateFlow<Boolean> = _inboxUnavailable.asStateFlow()

    private val _selectedDetailId = MutableStateFlow<String?>(null)
    val selectedDetailId: StateFlow<String?> = _selectedDetailId.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            if (_items.value.isEmpty()) {
                _isLoading.value = true
            } else {
                _isRefreshing.value = true
            }
            _loadError.value = null
            _inboxUnavailable.value = false
            val result = withContext(Dispatchers.IO) {
                userRepository.listMyNotifications(PAGE_LIMIT, beforeId = null)
            }
            result.onSuccess { page ->
                _items.value = page.items
                _hasMore.value = page.items.size >= PAGE_LIMIT
            }.onFailure { e ->
                val msg = e.message.orEmpty()
                _loadError.value = msg
                if (msg.contains("HTTP 404") || msg.contains("HTTP 503")) {
                    _inboxUnavailable.value = true
                }
                _items.value = emptyList()
                _hasMore.value = false
            }
            _isLoading.value = false
            _isRefreshing.value = false
        }
    }

    fun retryAfterError() {
        refresh()
    }

    fun loadMore() {
        val last = _items.value.lastOrNull() ?: return
        if (!_hasMore.value || _loadMoreBusy.value || _isLoading.value) return
        viewModelScope.launch {
            _loadMoreBusy.value = true
            val r = withContext(Dispatchers.IO) {
                userRepository.listMyNotifications(PAGE_LIMIT, beforeId = last.id)
            }
            r.onSuccess { page ->
                val have = _items.value.map { it.id }.toSet()
                val appended = page.items.filter { it.id !in have }
                _items.update { it + appended }
                _hasMore.value = page.items.size >= PAGE_LIMIT
            }
            _loadMoreBusy.value = false
        }
    }

    fun openDetail(id: String) {
        _selectedDetailId.value = id.trim().takeIf { it.isNotEmpty() }
    }

    fun closeDetail() {
        _selectedDetailId.value = null
    }

    fun markReadIfNeeded(item: InboxNotificationItem) {
        if (!item.isUnread) return
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { userRepository.markNotificationRead(item.id) }
            val stamp = Instant.now().toString()
            r.onSuccess {
                _items.update { list ->
                    list.map { row ->
                        if (row.id == item.id) row.copy(readAtIso = stamp) else row
                    }
                }
            }
        }
    }

    companion object {
        private const val PAGE_LIMIT = 30
    }
}
