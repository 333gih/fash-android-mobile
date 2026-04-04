package com.pc.fash_android_mobile.ui.explore

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.search.FeaturedSellerItem
import com.pc.fash_android_mobile.data.search.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeaturedSellersViewModel(application: Application) : AndroidViewModel(application) {

    private val searchRepository: SearchRepository =
        (application as FashApplication).searchRepository
    private val listingRepository: ListingRepository =
        (application as FashApplication).listingRepository

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

    /** `userId` (or username fallback) → up to 3 cover image URLs for preview tiles. */
    private val _previewCoverUrlsBySellerKey = MutableStateFlow<Map<String, List<String?>>>(emptyMap())
    val previewCoverUrlsBySellerKey: StateFlow<Map<String, List<String?>>> =
        _previewCoverUrlsBySellerKey.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = false
            _loadErrorDetail.value = null
            val result = withContext(Dispatchers.IO) {
                searchRepository.getFeaturedSellers(limit = 50)
            }
            result.fold(
                onSuccess = { _items.value = it },
                onFailure = { e ->
                    Log.e(TAG, "getFeaturedSellers failed", e)
                    _items.value = emptyList()
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
            val result = withContext(Dispatchers.IO) {
                searchRepository.getFeaturedSellers(limit = 50)
            }
            result.fold(
                onSuccess = { _items.value = it },
                onFailure = { e ->
                    Log.e(TAG, "getFeaturedSellers refresh failed", e)
                    _loadError.value = true
                    _loadErrorDetail.value = e.message ?: e.toString()
                },
            )
            _previewCoverUrlsBySellerKey.value = emptyMap()
            _isRefreshing.value = false
        }
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

        fun sellerKey(seller: FeaturedSellerItem): String =
            seller.userId.trim().ifBlank { seller.username.trim() }
    }
}
