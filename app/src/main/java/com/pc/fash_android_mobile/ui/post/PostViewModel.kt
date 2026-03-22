package com.pc.fash_android_mobile.ui.post

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.listing.Category
import com.pc.fash_android_mobile.data.listing.CreateListingRequest
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.user.AestheticTag
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.net.Uri

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val listingRepository: ListingRepository =
        (application as FashApplication).listingRepository
    private val userRepository: UserRepository =
        (application as FashApplication).userRepository

    private val _draft = MutableStateFlow(CreateListingDraft())
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()
    private val _aestheticTags = MutableStateFlow<List<AestheticTag>>(emptyList())
    val aestheticTags: StateFlow<List<AestheticTag>> = _aestheticTags.asStateFlow()
    private val _meProfile = MutableStateFlow<ProfileInfo?>(null)
    val meProfile: StateFlow<ProfileInfo?> = _meProfile.asStateFlow()
    val draft: StateFlow<CreateListingDraft> = _draft.asStateFlow()

    private val _step = MutableStateFlow(1)
    val step: StateFlow<Int> = _step.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    fun setImageUris(uris: List<Uri>) {
        _draft.value = _draft.value.withImageUris(uris.take(6))
    }

    fun removeImage(index: Int) {
        _draft.value = _draft.value.removeImageAtIndex(index)
    }

    fun nextStep() {
        val s = _step.value
        when (s) {
            1 -> if (_draft.value.canProceedFromStep1()) _step.value = 2
            2 -> if (_draft.value.canProceedFromStep2()) _step.value = 3
            else -> { }
        }
    }

    fun loadStep2Data() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                listingRepository.getCategories().fold(
                    onSuccess = { _categories.value = it },
                    onFailure = { },
                )
                userRepository.getAestheticTags().fold(
                    onSuccess = { _aestheticTags.value = it },
                    onFailure = { },
                )
            }
        }
    }

    fun loadStep3Data() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                userRepository.getMeProfile().fold(
                    onSuccess = { _meProfile.value = it },
                    onFailure = { _meProfile.value = null },
                )
            }
        }
    }

    fun prevStep() {
        when (_step.value) {
            2 -> _step.value = 1
            3 -> _step.value = 2
            else -> { }
        }
    }

    fun goToStep(stepNum: Int) {
        if (stepNum in 1..3) _step.value = stepNum
    }

    fun updateDraft(block: CreateListingDraft.() -> CreateListingDraft) {
        _draft.value = block(_draft.value)
    }

    /** Uploads images from draft and stores URLs. Call before proceeding to step 2 or at submit. */
    suspend fun uploadImages(uriResolver: (Uri) -> ByteArray?): Boolean {
        val uris = _draft.value.imageUris
        if (uris.isEmpty()) return true
        _isUploading.value = true
        val urls = mutableListOf<String>()
        for ((i, uri) in uris.withIndex()) {
            val bytes = withContext(Dispatchers.IO) { uriResolver(uri) }
            if (bytes == null || bytes.isEmpty()) {
                _events.tryEmit(getApplication<Application>().getString(R.string.create_listing_image_error))
                _isUploading.value = false
                return false
            }
            listingRepository.uploadListingImage(bytes, "image_$i.jpg").fold(
                onSuccess = { urls.add(it) },
                onFailure = {
                    _events.tryEmit(it.message ?: getApplication<Application>().getString(R.string.create_listing_upload_error))
                    _isUploading.value = false
                    return false
                },
            )
        }
        _draft.value = _draft.value.withImageUrls(urls)
        _isUploading.value = false
        return true
    }

    fun submitListing(uriResolver: (Uri) -> ByteArray?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val d = _draft.value
                val urls = d.imageUrls
                val uris = d.imageUris
                val finalUrls = when {
                    urls.size == uris.size -> urls
                    else -> {
                        if (!uploadImages(uriResolver)) return@launch
                        _draft.value.imageUrls
                    }
                }
                if (finalUrls.isEmpty()) {
                    _events.tryEmit(getApplication<Application>().getString(R.string.create_listing_no_images))
                    return@launch
                }
                val req = CreateListingRequest(
                    title = d.title,
                    imageUrls = finalUrls,
                    priceVnd = d.priceVnd,
                    condition = d.condition,
                    categoryId = d.categoryId,
                    description = d.description,
                    size = d.size,
                    brand = d.brand,
                    aestheticTags = d.aestheticTags,
                )
                listingRepository.createListing(req).fold(
                    onSuccess = {
                        _events.tryEmit(getApplication<Application>().getString(R.string.create_listing_success))
                        resetDraft()
                        onSuccess()
                    },
                    onFailure = {
                        _events.tryEmit(it.message ?: getApplication<Application>().getString(R.string.create_listing_error))
                    },
                )
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun cancel() {
        resetDraft()
    }

    private fun resetDraft() {
        _draft.value = CreateListingDraft()
        _step.value = 1
    }
}
