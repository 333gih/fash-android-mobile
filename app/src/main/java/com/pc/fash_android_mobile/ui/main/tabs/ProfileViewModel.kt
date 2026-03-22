package com.pc.fash_android_mobile.ui.main.tabs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository =
        (application as FashApplication).userRepository
    private val listingRepository: ListingRepository =
        (application as FashApplication).listingRepository

    private val _profile = MutableStateFlow<ProfileInfo?>(null)
    val profile: StateFlow<ProfileInfo?> = _profile.asStateFlow()

    private val _sellingListings = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val sellingListings: StateFlow<List<ListingFeedItem>> = _sellingListings.asStateFlow()

    private val _soldListings = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val soldListings: StateFlow<List<ListingFeedItem>> = _soldListings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow(false)
    val loadError: StateFlow<Boolean> = _loadError.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = false
            withContext(Dispatchers.IO) {
                userRepository.getMeProfile().fold(
                    onSuccess = { _profile.value = it },
                    onFailure = { _loadError.value = true },
                )
            }
            _profile.value?.userId?.let { loadListings(it) }
            _isLoading.value = false
        }
    }

    private suspend fun loadListings(sellerId: String) {
        withContext(Dispatchers.IO) {
            listingRepository.getListingsBySeller(
                sellerId = sellerId,
                status = null,
                limit = 50,
            ).fold(
                onSuccess = { _sellingListings.value = it },
                onFailure = { _sellingListings.value = emptyList() },
            )
            listingRepository.getListingsBySeller(
                sellerId = sellerId,
                status = "sold",
                limit = 50,
            ).fold(
                onSuccess = { _soldListings.value = it },
                onFailure = { _soldListings.value = emptyList() },
            )
        }
    }

    fun retryLoad() = loadProfile()
}
