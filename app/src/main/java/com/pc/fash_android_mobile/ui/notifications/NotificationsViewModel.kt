package com.pc.fash_android_mobile.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.user.InboxNotificationGroupsPage
import com.pc.fash_android_mobile.data.user.InboxNotificationItem
import com.pc.fash_android_mobile.data.user.InboxNotificationsPage
import com.pc.fash_android_mobile.data.user.NotificationGroupSummaryItem
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    /** Row resolved from FCM / tray tap before group list finishes loading. */
    private val _pushDetailItem = MutableStateFlow<InboxNotificationItem?>(null)
    val pushDetailItem: StateFlow<InboxNotificationItem?> = _pushDetailItem.asStateFlow()

    private val _pushDetailLoading = MutableStateFlow(false)
    val pushDetailLoading: StateFlow<Boolean> = _pushDetailLoading.asStateFlow()

    private val _pushDetailNotFound = MutableStateFlow(false)
    val pushDetailNotFound: StateFlow<Boolean> = _pushDetailNotFound.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _markAllReadBusy = MutableStateFlow(false)
    val markAllReadBusy: StateFlow<Boolean> = _markAllReadBusy.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _inboxApiReady = MutableStateFlow(false)
    val inboxApiReady: StateFlow<Boolean> = _inboxApiReady.asStateFlow()

    init {
        // Defer inbox API until [onAuthenticatedSessionReady] — avoids 401s during splash / token refresh.
    }

    private var inboxSessionReady = false
    private val inboxRefreshMutex = Mutex()
    private var groupsRefreshJob: Job? = null
    private var itemsRefreshJob: Job? = null

    /** Call after splash session validation (same timing as profile shell). */
    fun onAuthenticatedSessionReady() {
        if (_inboxApiReady.value) return
        _inboxApiReady.value = true
        inboxSessionReady = true
        refreshUnreadSummary()
        prefetchInboxGroups()
        if (_loadError.value != null && _groups.value.isEmpty() && _items.value.isEmpty()) {
            refresh()
        }
    }

    /** First paint when the inbox overlay opens — retries if a prior load failed transiently. */
    fun ensureInboxLoadedOnScreenOpen() {
        if (!inboxSessionReady) return
        if (_pushDetailLoading.value) return
        when {
            _loadError.value != null -> refresh()
            _selectedGroup.value == null && _groups.value.isEmpty() && !_isLoading.value -> prefetchInboxGroups()
            _selectedGroup.value != null && _items.value.isEmpty() && !_isLoading.value -> refreshGroupItems()
        }
    }

    fun refreshUnreadSummary() {
        if (!inboxSessionReady) return
        viewModelScope.launch {
            val n = withContext(Dispatchers.IO) {
                userRepository.getMyNotificationsUnreadCount().getOrElse { 0 }
            }
            _unreadCount.value = n
        }
    }

    fun clearCachesForSignedOutUser() {
        groupsRefreshJob?.cancel()
        itemsRefreshJob?.cancel()
        groupsRefreshJob = null
        itemsRefreshJob = null
        inboxSessionReady = false
        _inboxApiReady.value = false
        _groups.value = emptyList()
        _selectedGroup.value = null
        _items.value = emptyList()
        _unreadCount.value = 0
        _loadError.value = null
        _inboxUnavailable.value = false
        _hasMore.value = false
        _selectedDetailId.value = null
        _pushDetailItem.value = null
        _pushDetailLoading.value = false
        _pushDetailNotFound.value = false
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
        if (!inboxSessionReady) return
        if (_pushDetailLoading.value) return
        if (_selectedGroup.value == null) {
            prefetchInboxGroups(force = true)
        } else {
            refreshGroupItems()
        }
    }

    private fun prefetchInboxGroups(force: Boolean = false) {
        if (!inboxSessionReady) return
        if (!force && _groups.value.isNotEmpty()) return
        if (groupsRefreshJob?.isActive == true) return
        groupsRefreshJob = viewModelScope.launch {
            inboxRefreshMutex.withLock {
                refreshGroupsInternal()
            }
        }
    }

    private fun refreshGroups() {
        prefetchInboxGroups(force = true)
    }

    private fun scheduleInboxGroupsRetry() {
        if (groupsRefreshJob?.isActive == true) return
        groupsRefreshJob = viewModelScope.launch {
            delay(INBOX_RETRY_BASE_DELAY_MS * 2)
            prefetchInboxGroups(force = true)
        }
    }

    private suspend fun refreshGroupsInternal() {
        if (!inboxSessionReady) return
        if (_groups.value.isEmpty()) {
            _isLoading.value = true
        } else {
            _isRefreshing.value = true
        }
        _loadError.value = null
        _inboxUnavailable.value = false
        val result = listNotificationGroupsWithRetry()
        result.onSuccess { page ->
            val merged = mergeGroupSummaries(page.groups)
            _groups.value = merged
            refreshUnreadSummary()
        }.onFailure { e ->
            val msg = inboxFailureMessage(e)
            if (isTransientInboxFailure(msg, e)) {
                _loadError.value = null
                if (_groups.value.isEmpty()) {
                    scheduleInboxGroupsRetry()
                }
                return@onFailure
            }
            _loadError.value = msg
            if (msg.contains("HTTP 404") || msg.contains("HTTP 503")) {
                _inboxUnavailable.value = true
            }
            _groups.value = emptyList()
        }
        _isLoading.value = false
        _isRefreshing.value = false
    }

    private fun refreshGroupItems() {
        val group = _selectedGroup.value ?: return
        if (!inboxSessionReady) return
        if (itemsRefreshJob?.isActive == true) return
        itemsRefreshJob = viewModelScope.launch {
            if (_items.value.isEmpty()) {
                _isLoading.value = true
            } else {
                _isRefreshing.value = true
            }
            _loadError.value = null
            _inboxUnavailable.value = false
            val result = listNotificationsWithRetry(group = group, beforeId = null)
            result.onSuccess { page ->
                _items.value = page.items
                _hasMore.value = page.items.size >= PAGE_LIMIT
                refreshUnreadSummary()
            }.onFailure { e ->
                val msg = inboxFailureMessage(e)
                if (isTransientInboxFailure(msg, e)) {
                    _loadError.value = null
                    if (_items.value.isEmpty()) {
                        viewModelScope.launch {
                            delay(INBOX_RETRY_BASE_DELAY_MS * 2)
                            refreshGroupItems()
                        }
                    }
                    return@onFailure
                }
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
        if (!inboxSessionReady) return
        val fromTrayTap = (getApplication<Application>() as FashApplication).consumeInboxOpenFromTrayTap()
        _pushDetailLoading.value = true
        _pushDetailNotFound.value = false
        _pushDetailItem.value = null
        _selectedDetailId.value = id
        _loadError.value = null
        viewModelScope.launch {
            if (_groups.value.isEmpty()) {
                inboxRefreshMutex.withLock {
                    refreshGroupsInternal()
                }
            }
            val item = findNotificationInInbox(id)
            refreshUnreadSummary()
            _pushDetailLoading.value = false
            if (item != null) {
                _pushDetailItem.value = item
                item.notificationGroup?.let { group ->
                    _selectedGroup.value = group
                    loadGroupItemsForPushDetail(group, item)
                }
                markReadIfNeeded(item)
            } else if (fromTrayTap) {
                _pushDetailNotFound.value = true
            } else {
                _selectedDetailId.value = null
            }
        }
    }

    private suspend fun findNotificationInInbox(id: String): InboxNotificationItem? {
        var beforeId: String? = null
        repeat(PUSH_DETAIL_MAX_PAGES) {
            val page = listNotificationsWithRetry(beforeId = beforeId, group = null)
                .getOrNull() ?: return null
            page.items.find { it.id == id }?.let { return it }
            if (page.items.size < PAGE_LIMIT) return null
            beforeId = page.items.lastOrNull()?.id ?: return null
        }
        return null
    }

    private fun loadGroupItemsForPushDetail(group: String, resolved: InboxNotificationItem) {
        viewModelScope.launch {
            val page = withContext(Dispatchers.IO) {
                userRepository.listMyNotifications(PAGE_LIMIT, beforeId = null, group = group)
            }.getOrNull() ?: return@launch
            val merged = if (page.items.any { it.id == resolved.id }) {
                page.items
            } else {
                listOf(resolved) + page.items
            }
            _items.value = merged
            _hasMore.value = page.items.size >= PAGE_LIMIT
        }
    }

    fun closeDetail() {
        _selectedDetailId.value = null
        _pushDetailItem.value = null
        _pushDetailNotFound.value = false
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
        /** Up to 450 inbox rows scanned when opening detail from a push tap. */
        private const val PUSH_DETAIL_MAX_PAGES = 15
        private const val INBOX_LOAD_RETRY_ATTEMPTS = 4
        private const val INBOX_RETRY_BASE_DELAY_MS = 350L
    }

    private suspend fun listNotificationGroupsWithRetry() = withContext(Dispatchers.IO) {
        var last: Result<InboxNotificationGroupsPage>? = null
        repeat(INBOX_LOAD_RETRY_ATTEMPTS) { attempt ->
            val result = userRepository.listMyNotificationGroups()
            if (result.isSuccess) return@withContext result
            last = result
            val err = result.exceptionOrNull()
            val msg = inboxFailureMessage(err)
            if (!isTransientInboxFailure(msg, err) || attempt == INBOX_LOAD_RETRY_ATTEMPTS - 1) {
                return@withContext result
            }
            delay(INBOX_RETRY_BASE_DELAY_MS * (attempt + 1))
        }
        last ?: Result.failure(IllegalStateException("inbox groups load failed"))
    }

    private suspend fun listNotificationsWithRetry(
        beforeId: String? = null,
        group: String? = null,
    ) = withContext(Dispatchers.IO) {
        var last: Result<InboxNotificationsPage>? = null
        repeat(INBOX_LOAD_RETRY_ATTEMPTS) { attempt ->
            val result = userRepository.listMyNotifications(PAGE_LIMIT, beforeId = beforeId, group = group)
            if (result.isSuccess) return@withContext result
            last = result
            val err = result.exceptionOrNull()
            val msg = inboxFailureMessage(err)
            if (!isTransientInboxFailure(msg, err) || attempt == INBOX_LOAD_RETRY_ATTEMPTS - 1) {
                return@withContext result
            }
            delay(INBOX_RETRY_BASE_DELAY_MS * (attempt + 1))
        }
        last ?: Result.failure(IllegalStateException("inbox list load failed"))
    }

    private fun inboxFailureMessage(error: Throwable?): String = error?.message.orEmpty()

    private fun isTransientInboxFailure(message: String, error: Throwable?): Boolean {
        if (message.contains("HTTP 401")) return true
        if (message.contains("Transient token refresh", ignoreCase = true)) return true
        if (message.contains("timeout", ignoreCase = true)) return true
        if (message.contains("Unable to resolve host", ignoreCase = true)) return true
        if (message.contains("Connection reset", ignoreCase = true)) return true
        if (message.contains("Connection refused", ignoreCase = true)) return true
        val cause = error?.cause
        if (cause != null && cause !== error) {
            return isTransientInboxFailure(cause.message.orEmpty(), cause)
        }
        return false
    }
}
