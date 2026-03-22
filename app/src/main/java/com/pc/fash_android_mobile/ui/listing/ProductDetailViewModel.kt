package com.pc.fash_android_mobile.ui.listing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.ListingDetail
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val listingRepository: ListingRepository =
        (application as FashApplication).listingRepository
    private val userRepository: UserRepository =
        (application as FashApplication).userRepository

    private val _detail = MutableStateFlow<ListingDetail?>(null)
    val detail: StateFlow<ListingDetail?> = _detail.asStateFlow()

    private val _sellerProfile = MutableStateFlow<ProfileInfo?>(null)
    val sellerProfile: StateFlow<ProfileInfo?> = _sellerProfile.asStateFlow()

    private val _moreFromSeller = MutableStateFlow<List<ListingFeedItem>>(emptyList())
    val moreFromSeller: StateFlow<List<ListingFeedItem>> = _moreFromSeller.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    fun loadDetail(listingId: String) {
        if (listingId.isBlank()) {
            _loadError.value = getApplication<Application>().getString(R.string.product_detail_error)
            _isLoading.value = false
            return
        }
        viewModelScope.launch {
            _detail.value = null
            _sellerProfile.value = null
            _moreFromSeller.value = emptyList()
            _isLoading.value = true
            _loadError.value = null
            withContext(Dispatchers.IO) {
                val detailResult = listingRepository.getListingDetail(listingId)
                detailResult.fold(
                    onSuccess = { d ->
                        _detail.value = d
                        val sid = d.sellerId?.takeIf { it.isNotBlank() }
                            ?: d.sellerUsername?.takeIf { it.isNotBlank() }
                        sid?.let { loadSellerAndMore(it, listingId) }
                        listingRepository.recordView(listingId)
                    },
                    onFailure = {
                        _loadError.value = it.message?.takeIf { m -> m.isNotBlank() }
                            ?: getApplication<Application>().getString(R.string.product_detail_error)
                    },
                )
            }
            _isLoading.value = false
        }
    }

    private suspend fun loadSellerAndMore(sellerKey: String, excludeListingId: String) {
        val d = _detail.value
        val profileId = d?.sellerUsername?.takeIf { it.isNotBlank() } ?: sellerKey
        val profileResult = userRepository.getProfile(profileId)
        profileResult.fold(
            onSuccess = {
                _sellerProfile.value = it
                it.isFollowing?.let { following -> _isFollowing.value = following }
            },
            onFailure = { },
        )
        val moreResult = listingRepository.getListingsBySeller(sellerKey, limit = 5)
        moreResult.fold(
            onSuccess = { list ->
                _moreFromSeller.value = list.filter { it.id != excludeListingId }.take(5)
            },
            onFailure = { _moreFromSeller.value = emptyList() },
        )
    }

    /** Resolves seller id or username from current listing detail. */
    private fun followTargetOrNull(): String? {
        val d = _detail.value ?: return null
        return d.sellerId?.takeIf { it.isNotBlank() }
            ?: d.sellerUsername?.takeIf { it.isNotBlank() }
    }

    fun follow(sellerId: String?) {
        val target = sellerId?.takeIf { it.isNotBlank() } ?: followTargetOrNull()
        if (target.isNullOrBlank()) return
        viewModelScope.launch {
            userRepository.follow(target).fold(
                onSuccess = {
                    _isFollowing.update { true }
                    _events.tryEmit(getApplication<Application>().getString(R.string.follow_success))
                },
                onFailure = {
                    _events.tryEmit(
                        it.message?.takeIf { m -> m.isNotBlank() }
                            ?: getApplication<Application>().getString(R.string.feed_action_error),
                    )
                },
            )
        }
    }

    fun unfollow(sellerId: String?) {
        val target = sellerId?.takeIf { it.isNotBlank() } ?: followTargetOrNull()
        if (target.isNullOrBlank()) return
        viewModelScope.launch {
            userRepository.unfollow(target).fold(
                onSuccess = { _isFollowing.update { false } },
                onFailure = {
                    _events.tryEmit(
                        it.message?.takeIf { m -> m.isNotBlank() }
                            ?: getApplication<Application>().getString(R.string.feed_action_error),
                    )
                },
            )
        }
    }

    fun setFollowing(following: Boolean) {
        _isFollowing.value = following
    }

    fun toggleSave() {
        val d = _detail.value ?: return
        viewModelScope.launch {
            listingRepository.toggleSave(d.id).fold(
                onSuccess = { saved ->
                    _detail.update { it?.copy(isSaved = saved) }
                },
                onFailure = {
                    _events.tryEmit(
                        it.message?.takeIf { m -> m.isNotBlank() }
                            ?: getApplication<Application>().getString(R.string.feed_action_error),
                    )
                },
            )
        }
    }

    fun retryLoad(listingId: String) {
        loadDetail(listingId)
    }

    fun clearError() {
        _loadError.value = null
    }
}
