package com.pc.fash_android_mobile.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.listing.ListingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeInReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val listingRepository: ListingRepository =
        (application as FashApplication).listingRepository

    private val _listings = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val listings: StateFlow<List<ListingFeedItem>> = _listings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    fun clearCachesForSignedOutUser() {
        _listings.value = emptyList()
        _isLoading.value = false
        _isRefreshing.value = false
        _loadError.value = null
    }

    fun loadIfNeeded() {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            fetchInReview()
            _isLoading.value = false
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _loadError.value = null
            fetchInReview()
            _isRefreshing.value = false
        }
    }

    private suspend fun fetchInReview() {
        val result = withContext(Dispatchers.IO) {
            listingRepository.getMyListings(status = "in_review", limit = 50, offset = 0)
        }
        result.fold(
            onSuccess = { list -> _listings.value = list },
            onFailure = { e ->
                _loadError.value = e.message?.takeIf { it.isNotBlank() }
                _listings.value = emptyList()
            },
        )
    }
}
