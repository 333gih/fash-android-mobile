package com.pc.fash_android_mobile.ui.chat

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.chat.ChatRepository
import com.pc.fash_android_mobile.data.chat.ConversationItem
import com.pc.fash_android_mobile.data.chat.ConversationListingGroup
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.realtime.RealtimeEvent
import com.pc.fash_android_mobile.data.realtime.RealtimeManager
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
import java.text.NumberFormat
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

/** Seller-only segmented control: flat inbox vs `group_by=listing`. */
enum class SellerInboxGroupMode {
    /** Default — same as today (Flat list). */
    AllConversations,
    /** GET …?group_by=listing */
    ByProduct,
}

class ChatViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val chatRepository: ChatRepository =
        (application as FashApplication).chatRepository
    private val listingRepository: ListingRepository =
        (application as FashApplication).listingRepository
    private val realtimeManager: RealtimeManager =
        (application as FashApplication).realtimeManager
    private val sessionStore =
        (application as FashApplication).authManager.sessionStore

    init {
        viewModelScope.launch {
            realtimeManager.events.collect { event ->
                when (event) {
                    is RealtimeEvent.MessageNew,
                    is RealtimeEvent.ReadReceipts,
                    -> {
                        silentRefreshConversations()
                        refreshUnreadCount()
                    }
                    else -> Unit
                }
            }
        }
    }

    /** Full unfiltered list returned by the API (flat). */
    private val _allConversations = MutableStateFlow<List<ConversationItem>>(emptyList())

    /** Raw grouped payload when seller selects “Theo sản phẩm”. */
    private val _conversationGroups = MutableStateFlow<List<ConversationListingGroup>>(emptyList())

    /** Filtered view shown in the UI (flat). */
    private val _conversations = MutableStateFlow<List<ConversationItem>>(emptyList())
    val conversations: StateFlow<List<ConversationItem>> = _conversations.asStateFlow()

    /** Filtered groups for grouped inbox. */
    private val _displayGroups = MutableStateFlow<List<ConversationListingGroup>>(emptyList())
    val displayGroups: StateFlow<List<ConversationListingGroup>> = _displayGroups.asStateFlow()

    /** Expanded group headers (listing ids); default = all expanded. */
    private val _expandedGroupListingIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedGroupListingIds: StateFlow<Set<String>> = _expandedGroupListingIds.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ChatFilter.All)
    val selectedFilter: StateFlow<ChatFilter> = _selectedFilter.asStateFlow()

    private val _sellerInboxGroupMode = MutableStateFlow(SellerInboxGroupMode.AllConversations)
    val sellerInboxGroupMode: StateFlow<SellerInboxGroupMode> = _sellerInboxGroupMode.asStateFlow()

    private val _sellerHasActiveListings = MutableStateFlow(false)
    val sellerHasActiveListings: StateFlow<Boolean> = _sellerHasActiveListings.asStateFlow()

    private val _unreadBadgeCount = MutableStateFlow(0)
    val unreadBadgeCount: StateFlow<Int> = _unreadBadgeCount.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    /** Monotonic time of last successful inbox GET; used to skip redundant tab re-entry fetches. */
    private var lastSuccessfulInboxFetchMs: Long = 0L

    private fun isGroupedInbox(): Boolean =
        _sellerHasActiveListings.value &&
            _sellerInboxGroupMode.value == SellerInboxGroupMode.ByProduct

    fun loadConversations() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            refreshSellerListingEligibilityInternal()
            var fetchSucceeded = false
            if (isGroupedInbox()) {
                withContext(Dispatchers.IO) {
                    chatRepository.getConversationsGroupedByListing(limit = 50, offset = 0)
                }.fold(
                    onSuccess = { groups ->
                        fetchSucceeded = true
                        _conversationGroups.value = groups
                        _expandedGroupListingIds.setAll(groups.map { it.listingId })
                        applyCurrentViewFilter()
                    },
                    onFailure = {
                        _loadError.value = it.message
                            ?: getApplication<Application>().getString(R.string.chat_load_error)
                        _events.tryEmit(_loadError.value!!)
                    },
                )
            } else {
                withContext(Dispatchers.IO) {
                    chatRepository.getConversations(limit = 50, offset = 0)
                }.fold(
                    onSuccess = { all ->
                        fetchSucceeded = true
                        _allConversations.value = all
                        applyCurrentViewFilter()
                    },
                    onFailure = {
                        _loadError.value = it.message
                            ?: getApplication<Application>().getString(R.string.chat_load_error)
                        _events.tryEmit(_loadError.value!!)
                    },
                )
            }
            if (fetchSucceeded) {
                lastSuccessfulInboxFetchMs = SystemClock.elapsedRealtime()
            }
            _isLoading.value = false
            refreshUnreadCount()
        }
    }

    /**
     * Loads inbox unless we already fetched successfully recently (tab re-entry).
     * Pull-to-refresh and [loadConversations] still force a full load.
     */
    fun loadConversationsWhenNeeded(staleAfterMs: Long = 60_000L) {
        if (_loadError.value != null) {
            loadConversations()
            return
        }
        val now = SystemClock.elapsedRealtime()
        val everFetched = lastSuccessfulInboxFetchMs > 0L
        val stale = (now - lastSuccessfulInboxFetchMs) >= staleAfterMs
        if (everFetched && !stale) {
            return
        }
        loadConversations()
    }

    private fun MutableStateFlow<Set<String>>.setAll(ids: List<String>) {
        value = ids.toSet()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            silentRefreshConversations()
            refreshUnreadCount()
            _isRefreshing.value = false
        }
    }

    private suspend fun silentRefreshConversations() {
        if (isGroupedInbox()) {
            val r = withContext(Dispatchers.IO) {
                chatRepository.getConversationsGroupedByListing(limit = 50, offset = 0)
            }
            r.getOrNull()?.let { groups ->
                _conversationGroups.value = groups
                if (_expandedGroupListingIds.value.isEmpty()) {
                    _expandedGroupListingIds.setAll(groups.map { it.listingId })
                }
            }
            applyCurrentViewFilter()
        } else {
            val result = withContext(Dispatchers.IO) {
                chatRepository.getConversations(limit = 50, offset = 0)
            }
            result.getOrNull()?.let { all ->
                _allConversations.value = all
                applyCurrentViewFilter()
            }
        }
    }

    fun refreshSellerListingEligibility() {
        viewModelScope.launch { refreshSellerListingEligibilityInternal() }
    }

    private suspend fun refreshSellerListingEligibilityInternal() {
        val myId = sessionStore.read()?.userId?.trim().orEmpty()
        if (myId.isBlank()) {
            _sellerHasActiveListings.value = false
            return
        }
        val r = withContext(Dispatchers.IO) {
            listingRepository.getListingsBySeller(myId, status = "active", limit = 1, offset = 0)
        }
        _sellerHasActiveListings.value = r.getOrNull()?.isNotEmpty() == true
    }

    fun refreshUnreadCount() {
        viewModelScope.launch {
            val n = withContext(Dispatchers.IO) { chatRepository.getUnreadCount() }.getOrNull() ?: 0
            _unreadBadgeCount.value = n
        }
    }

    fun setFilter(filter: ChatFilter) {
        _selectedFilter.value = filter
        applyCurrentViewFilter()
    }

    fun setSellerInboxGroupMode(mode: SellerInboxGroupMode) {
        if (_sellerInboxGroupMode.value == mode) return
        _sellerInboxGroupMode.value = mode
        viewModelScope.launch {
            _loadError.value = null
            silentRefreshConversations()
            refreshUnreadCount()
        }
    }

    fun toggleListingGroupExpanded(listingId: String) {
        _expandedGroupListingIds.update { cur ->
            if (listingId in cur) cur - listingId else cur + listingId
        }
    }

    private fun applyCurrentViewFilter() {
        if (isGroupedInbox()) {
            applyGroupFilter(_selectedFilter.value, _conversationGroups.value)
        } else {
            _displayGroups.value = emptyList()
            applyFilter(_selectedFilter.value, _allConversations.value)
        }
    }

    private fun applyFilter(filter: ChatFilter, all: List<ConversationItem>) {
        val myId = sessionStore.read()?.userId.orEmpty()
        _conversations.value = when (filter) {
            ChatFilter.All -> all
            ChatFilter.Unread -> all.filter { it.isUnread }
            ChatFilter.Seller -> if (myId.isBlank()) all else all.filter { it.sellerUserId == myId }
            ChatFilter.Buyer -> if (myId.isBlank()) all else all.filter { it.buyerUserId == myId }
        }
    }

    private fun applyGroupFilter(filter: ChatFilter, groups: List<ConversationListingGroup>) {
        val myId = sessionStore.read()?.userId.orEmpty()
        fun passItem(item: ConversationItem): Boolean = when (filter) {
            ChatFilter.All -> true
            ChatFilter.Unread -> item.isUnread
            ChatFilter.Seller -> myId.isBlank() || item.sellerUserId == myId
            ChatFilter.Buyer -> myId.isBlank() || item.buyerUserId == myId
        }
        _displayGroups.value = groups.map { g ->
            val convs = g.conversations.filter(::passItem)
            g.copy(
                conversations = convs,
                conversationCountBadge = convs.size,
            )
        }.filter { it.conversations.isNotEmpty() }
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

    fun formatPriceVnd(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        return "₫${formatter.format(amount)}"
    }

    /**
     * Inbox subtitle: pending buyer offer (seller), last offer line, or last message text.
     */
    fun conversationPreviewLine(item: ConversationItem): String {
        val app = getApplication<Application>()
        val myId = sessionStore.read()?.userId?.trim().orEmpty()
        val isSeller = myId.isNotBlank() && item.sellerUserId == myId
        val isBuyer = myId.isNotBlank() && item.buyerUserId == myId
        if (item.pendingOfferAmountVnd > 0L && isSeller) {
            return app.getString(
                R.string.chat_inbox_preview_offer_pending_seller,
                formatPriceVnd(item.pendingOfferAmountVnd),
            )
        }
        val isOfferRow = item.lastMessageType.equals("offer", ignoreCase = true) ||
            item.lastOfferAmountVnd > 0L
        if (isOfferRow) {
            val amtStr = when {
                item.lastOfferAmountVnd > 0L -> formatPriceVnd(item.lastOfferAmountVnd)
                else -> item.lastMessageText.trim().ifBlank { formatPriceVnd(0L) }
            }
            return when {
                isBuyer && item.lastOfferFromBuyer ->
                    app.getString(R.string.chat_inbox_preview_offer_you, amtStr)
                isBuyer && !item.lastOfferFromBuyer ->
                    app.getString(R.string.chat_inbox_preview_offer_from_seller, amtStr)
                isSeller && item.lastOfferFromBuyer ->
                    app.getString(R.string.chat_inbox_preview_offer_from_buyer, amtStr)
                else -> app.getString(R.string.chat_inbox_preview_offer_generic, amtStr)
            }
        }
        return item.lastMessageText.ifBlank { " " }
    }
}
