package com.pc.fash_android_mobile.ui.explore

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.search.FeaturedSellerItem
import com.pc.fash_android_mobile.data.search.FeaturedSellersPage
import com.pc.fash_android_mobile.data.search.SearchRepository
import com.pc.fash_android_mobile.data.search.shopReadyOnly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeaturedSellersViewModel(application: Application) : AndroidViewModel(application) {

    private val fashApp = application as FashApplication
    private val searchRepository: SearchRepository = fashApp.searchRepository
    private val listingRepository: ListingRepository = fashApp.listingRepository

    private fun isGuestBrowse(): Boolean = fashApp.isGuestBrowseActive

    private val _items = MutableStateFlow<List<FeaturedSellerItem>>(emptyList())
    val items: StateFlow<List<FeaturedSellerItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow(false)
    val loadError: StateFlow<Boolean> = _loadError.asStateFlow()

    /** Set when [load] / [refresh] fail — often HTTP status text; helps debug API mismatches. */
    private val _loadErrorDetail = MutableStateFlow<String?>(null)
    val loadErrorDetail: StateFlow<String?> = _loadErrorDetail.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    /** Server-reported eligible count after exclusions. */
    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    /** `userId` (or username fallback) → up to 3 cover image URLs for preview tiles. */
    private val _previewCoverUrlsBySellerKey = MutableStateFlow<Map<String, List<String?>>>(emptyMap())
    val previewCoverUrlsBySellerKey: StateFlow<Map<String, List<String?>>> =
        _previewCoverUrlsBySellerKey.asStateFlow()

    private val seenKeys = LinkedHashSet<String>()
    private var nextOffset = 0
    private var lastLoadedAtMs = 0L

    private suspend fun fetchPage(offset: Int): Result<FeaturedSellersPage> {
        return if (isGuestBrowse()) {
            searchRepository.browseFeaturedSellersPage(limit = PAGE_SIZE, offset = offset)
        } else {
            searchRepository.getFeaturedSellersPage(limit = PAGE_SIZE, offset = offset)
        }
    }

    private fun mergePage(page: FeaturedSellersPage, replace: Boolean) {
        if (replace) {
            seenKeys.clear()
        }
        val acc = if (replace) ArrayList<FeaturedSellerItem>() else ArrayList(_items.value)
        for (s in page.items.shopReadyOnly()) {
            val k = sellerKey(s)
            if (k.isBlank()) continue
            if (seenKeys.add(k)) acc.add(s)
        }
        _items.value = acc
        _totalCount.value = page.total
        nextOffset += page.items.size
        _hasMore.value = page.items.size >= PAGE_SIZE &&
            (page.total <= 0 || nextOffset < page.total)
        lastLoadedAtMs = System.currentTimeMillis()
    }

    /** First paint: reuse in-memory page if fresh; otherwise load first offset page. */
    fun ensureLoaded() {
        if (_isLoading.value || _isRefreshing.value) return
        val cached = _items.value.isNotEmpty() &&
            System.currentTimeMillis() - lastLoadedAtMs < MEMORY_CACHE_TTL_MS
        if (cached) return
        if (_items.value.isEmpty()) {
            load()
        } else {
            refresh()
        }
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = false
            _loadErrorDetail.value = null
            nextOffset = 0
            val result = withContext(Dispatchers.IO) { fetchPage(0) }
            result.fold(
                onSuccess = { page ->
                    nextOffset = 0
                    mergePage(page, replace = true)
                },
                onFailure = { e ->
                    Log.e(TAG, "getFeaturedSellers failed", e)
                    _items.value = emptyList()
                    _totalCount.value = 0
                    _hasMore.value = false
                    nextOffset = 0
                    seenKeys.clear()
                    _loadError.value = true
                    _loadErrorDetail.value = e.message ?: e.toString()
                },
            )
            _isLoading.value = false
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _loadError.value = false
            _loadErrorDetail.value = null
            val result = withContext(Dispatchers.IO) { fetchPage(0) }
            result.fold(
                onSuccess = { page ->
                    nextOffset = 0
                    mergePage(page, replace = true)
                    _previewCoverUrlsBySellerKey.value = emptyMap()
                },
                onFailure = { e ->
                    Log.e(TAG, "getFeaturedSellers refresh failed", e)
                    _loadError.value = true
                    _loadErrorDetail.value = e.message ?: e.toString()
                },
            )
            _isRefreshing.value = false
        }
    }

    fun loadMore() {
        if (!_hasMore.value || _isLoadingMore.value || _isLoading.value || _isRefreshing.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            val offset = nextOffset
            val result = withContext(Dispatchers.IO) { fetchPage(offset) }
            result.fold(
                onSuccess = { page -> mergePage(page, replace = false) },
                onFailure = { e ->
                    Log.e(TAG, "getFeaturedSellers loadMore failed", e)
                    _hasMore.value = false
                },
            )
            _isLoadingMore.value = false
        }
    }

    fun clearCachesForSignedOutUser() {
        _items.value = emptyList()
        _previewCoverUrlsBySellerKey.value = emptyMap()
        _loadError.value = false
        _loadErrorDetail.value = null
        _isLoading.value = false
        _isRefreshing.value = false
        _isLoadingMore.value = false
        _hasMore.value = false
        _totalCount.value = 0
        nextOffset = 0
        lastLoadedAtMs = 0L
        seenKeys.clear()
    }

    fun ensurePreviewCoversLoaded(seller: FeaturedSellerItem) {
        val key = sellerKey(seller)
        if (key.isBlank()) return
        if (_previewCoverUrlsBySellerKey.value.containsKey(key)) return
        viewModelScope.launch {
            val urls = withContext(Dispatchers.IO) {
                seller.previewListingIds.take(3).map { lid ->
                    if (lid.isBlank()) {
                        null
                    } else {
                        listingRepository.getListingDetail(lid).getOrNull()
                            ?.imageUrls
                            ?.firstOrNull()
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                    }
                }
            }
            _previewCoverUrlsBySellerKey.update { it + (key to urls) }
        }
    }

    companion object {
        private const val TAG = "FeaturedSellersVM"
        private const val PAGE_SIZE = 20
        private const val MEMORY_CACHE_TTL_MS = 60_000L

        fun sellerKey(seller: FeaturedSellerItem): String =
            seller.userId.trim().ifBlank { seller.username.trim() }
    }
}
