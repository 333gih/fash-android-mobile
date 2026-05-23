package com.pc.fash_android_mobile.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.user.InboxNotificationItem
import com.pc.fash_android_mobile.data.user.NotificationGroupSummaryItem
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository =
        (application as FashApplication).userRepository

    private val _groups = MutableStateFlow<List<NotificationGroupSummaryItem>>(emptyList())
    val groups: StateFlow<List<NotificationGroupSummaryItem>> = _groups.asStateFlow()

    private val _selectedGroup = MutableStateFlow<String?>(null)
    val selectedGroup: StateFlow<String?> = _selectedGroup.asStateFlow()

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

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _markAllReadBusy = MutableStateFlow(false)
    val markAllReadBusy: StateFlow<Boolean> = _markAllReadBusy.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        refreshUnreadSummary()
    }

    fun refreshUnreadSummary() {
        viewModelScope.launch {
            val n = withContext(Dispatchers.IO) {
                userRepository.getMyNotificationsUnreadCount().getOrElse { 0 }
            }
            _unreadCount.value = n
        }
    }

    fun clearCachesForSignedOutUser() {
        _groups.value = emptyList()
        _selectedGroup.value = null
        _items.value = emptyList()
        _unreadCount.value = 0
        _loadError.value = null
        _inboxUnavailable.value = false
        _hasMore.value = false
        _selectedDetailId.value = null
        _isLoading.value = false
        _isRefreshing.value = false
        _loadMoreBusy.value = false
        _markAllReadBusy.value = false
    }

    fun openGroup(group: String) {
        val g = group.trim().takeIf { it.isNotEmpty() } ?: return
        _selectedGroup.value = g
        _items.value = emptyList()
        _hasMore.value = false
        refreshGroupItems()
    }

    fun closeGroup() {
        _selectedGroup.value = null
        _items.value = emptyList()
        _hasMore.value = false
        _selectedDetailId.value = null
        refreshGroups()
    }

    fun markAllRead() {
        if (_markAllReadBusy.value || _inboxUnavailable.value) return
        val group = _selectedGroup.value?.trim()?.takeIf { it.isNotEmpty() } ?: return
        val groupUnread = _groups.value.find { it.group == group }?.unreadCount ?: 0
        if (_items.value.none { it.isUnread } && groupUnread <= 0) return
        viewModelScope.launch {
            _markAllReadBusy.value = true
            val result = withContext(Dispatchers.IO) {
                userRepository.markAllNotificationsRead(group = group)
            }
            _markAllReadBusy.value = false
            val app = getApplication<Application>()
            result.fold(
                onSuccess = { updated ->
                    val stamp = Instant.now().toString()
                    _items.update { list -> list.map { row -> row.copy(readAtIso = row.readAtIso ?: stamp) } }
                    _groups.update { list ->
                        list.map { row ->
                            if (row.group == group) row.copy(unreadCount = 0) else row
                        }
                    }
                    refreshUnreadSummary()
                    refreshGroups()
                    val msg = when {
                        updated > 0 -> app.getString(R.string.notification_mark_all_read_success, updated)
                        else -> app.getString(R.string.notification_mark_all_read_none)
                    }
                    _events.tryEmit(msg)
                },
                onFailure = { e ->
                    _events.tryEmit(
                        e.message?.takeIf { it.isNotBlank() }
                            ?: app.getString(R.string.notification_mark_all_read_error),
                    )
                },
            )
        }
    }

    fun refresh() {
        if (_selectedGroup.value == null) {
            refreshGroups()
        } else {
            refreshGroupItems()
        }
    }

    private fun refreshGroups() {
        viewModelScope.launch {
            if (_groups.value.isEmpty()) {
                _isLoading.value = true
            } else {
                _isRefreshing.value = true
            }
            _loadError.value = null
            _inboxUnavailable.value = false
            val result = withContext(Dispatchers.IO) { userRepository.listMyNotificationGroups() }
            result.onSuccess { page ->
                val merged = mergeGroupSummaries(page.groups)
                _groups.value = merged
                refreshUnreadSummary()
            }.onFailure { e ->
                val msg = e.message.orEmpty()
                _loadError.value = msg
                if (msg.contains("HTTP 404") || msg.contains("HTTP 503")) {
                    _inboxUnavailable.value = true
                }
                _groups.value = emptyList()
            }
            _isLoading.value = false
            _isRefreshing.value = false
        }
    }

    private fun refreshGroupItems() {
        val group = _selectedGroup.value ?: return
        viewModelScope.launch {
            if (_items.value.isEmpty()) {
                _isLoading.value = true
            } else {
                _isRefreshing.value = true
            }
            _loadError.value = null
            _inboxUnavailable.value = false
            val result = withContext(Dispatchers.IO) {
                userRepository.listMyNotifications(PAGE_LIMIT, beforeId = null, group = group)
            }
            result.onSuccess { page ->
                _items.value = page.items
                _hasMore.value = page.items.size >= PAGE_LIMIT
                refreshUnreadSummary()
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

    private fun mergeGroupSummaries(serverGroups: List<NotificationGroupSummaryItem>): List<NotificationGroupSummaryItem> {
        val byGroup = serverGroups.associateBy { it.group }
        val merged = NotificationGroups.displayOrder.map { code ->
            byGroup[code] ?: NotificationGroupSummaryItem(
                group = code,
                unreadCount = 0,
                latestId = null,
                latestTitle = null,
                latestBody = null,
                latestCreatedAtIso = null,
            )
        }
        return merged
    }

    fun retryAfterError() {
        refresh()
    }

    fun loadMore() {
        val group = _selectedGroup.value ?: return
        val last = _items.value.lastOrNull() ?: return
        if (!_hasMore.value || _loadMoreBusy.value || _isLoading.value) return
        viewModelScope.launch {
            _loadMoreBusy.value = true
            val r = withContext(Dispatchers.IO) {
                userRepository.listMyNotifications(PAGE_LIMIT, beforeId = last.id, group = group)
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

    fun openInboxDetailFromPush(notificationId: String) {
        val id = notificationId.trim()
        if (id.isEmpty()) return
        viewModelScope.launch {
            _selectedGroup.value = null
            if (_groups.value.isEmpty()) {
                refreshGroups()
            }
            if (_items.value.isEmpty()) {
                _isLoading.value = true
            }
            _loadError.value = null
            val first = withContext(Dispatchers.IO) {
                userRepository.listMyNotifications(PAGE_LIMIT, beforeId = null)
            }
            first.fold(
                onSuccess = { page ->
                    _items.value = page.items
                    _hasMore.value = page.items.size >= PAGE_LIMIT
                    page.items.find { it.id == id }?.notificationGroup?.let { openGroup(it) }
                },
                onFailure = { e ->
                    _loadError.value = e.message
                    _items.value = emptyList()
                    _hasMore.value = false
                    _isLoading.value = false
                    return@launch
                },
            )
            refreshUnreadSummary()
            var found = _items.value.any { it.id == id }
            var guard = 0
            while (!found && _hasMore.value && guard++ < 12) {
                val last = _items.value.lastOrNull() ?: break
                _loadMoreBusy.value = true
                val more = withContext(Dispatchers.IO) {
                    userRepository.listMyNotifications(PAGE_LIMIT, beforeId = last.id)
                }
                more.onSuccess { page ->
                    val have = _items.value.map { it.id }.toSet()
                    val appended = page.items.filter { it.id !in have }
                    _items.update { it + appended }
                    _hasMore.value = page.items.size >= PAGE_LIMIT
                }
                _loadMoreBusy.value = false
                found = _items.value.any { it.id == id }
            }
            _isLoading.value = false
            if (found) {
                val item = _items.value.find { it.id == id }
                item?.notificationGroup?.let { g ->
                    if (_selectedGroup.value != g) {
                        openGroup(g)
                    }
                }
                _selectedDetailId.value = id
                item?.let { markReadIfNeeded(it) }
            } else {
                openDetail(id)
            }
        }
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
                _groups.update { list ->
                    list.map { row ->
                        if (row.group == item.notificationGroup && row.unreadCount > 0) {
                            row.copy(unreadCount = (row.unreadCount - 1).coerceAtLeast(0))
                        } else {
                            row
                        }
                    }
                }
                refreshUnreadSummary()
            }
        }
    }

    companion object {
        private const val PAGE_LIMIT = 30
    }
}
