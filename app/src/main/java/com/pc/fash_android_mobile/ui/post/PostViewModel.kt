package com.pc.fash_android_mobile.ui.post

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.BuildConfig
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.address.ShippingAddress
import com.pc.fash_android_mobile.data.address.mergeShippingAddressesWithLocal
import com.pc.fash_android_mobile.data.common.CommonAestheticTagDto
import com.pc.fash_android_mobile.data.common.CommonBrandDto
import com.pc.fash_android_mobile.data.common.CommonCountryDto
import com.pc.fash_android_mobile.data.common.CommonServiceRepository
import com.pc.fash_android_mobile.data.common.CategoryTreeNode
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FashApplication
    private val listingRepository: ListingRepository = app.listingRepository
    private val userRepository: UserRepository = app.userRepository
    private val commonServiceRepository: CommonServiceRepository = app.commonServiceRepository
    private val addressLocalStore = app.addressLocalStore
    private val userShippingAddressRepository = app.userShippingAddressRepository

    private val _draft = MutableStateFlow(CreateListingDraft())
    val draft: StateFlow<CreateListingDraft> = _draft.asStateFlow()

    private val _step = MutableStateFlow(1)
    val step: StateFlow<Int> = _step.asStateFlow()

    private val _categoryTree = MutableStateFlow<List<CategoryTreeNode>>(emptyList())
    val categoryTree: StateFlow<List<CategoryTreeNode>> = _categoryTree.asStateFlow()

    private val _aestheticTags = MutableStateFlow<List<CommonAestheticTagDto>>(emptyList())
    val aestheticTags: StateFlow<List<CommonAestheticTagDto>> = _aestheticTags.asStateFlow()

    private val _aestheticTagsById = MutableStateFlow<Map<String, CommonAestheticTagDto>>(emptyMap())
    val aestheticTagsById: StateFlow<Map<String, CommonAestheticTagDto>> = _aestheticTagsById.asStateFlow()

    private val _brandsFeatured = MutableStateFlow<List<CommonBrandDto>>(emptyList())
    val brandsFeatured: StateFlow<List<CommonBrandDto>> = _brandsFeatured.asStateFlow()

    private val _brandsSearch = MutableStateFlow<List<CommonBrandDto>>(emptyList())
    val brandsSearch: StateFlow<List<CommonBrandDto>> = _brandsSearch.asStateFlow()

    private val _countries = MutableStateFlow<List<CommonCountryDto>>(emptyList())
    val countries: StateFlow<List<CommonCountryDto>> = _countries.asStateFlow()

    private val _catalogLoading = MutableStateFlow(false)
    val catalogLoading: StateFlow<Boolean> = _catalogLoading.asStateFlow()

    private val _catalogReady = MutableStateFlow(false)
    val catalogReady: StateFlow<Boolean> = _catalogReady.asStateFlow()

    private val _meProfile = MutableStateFlow<ProfileInfo?>(null)
    val meProfile: StateFlow<ProfileInfo?> = _meProfile.asStateFlow()

    private val _localAddresses = MutableStateFlow<List<ShippingAddress>>(emptyList())
    val localAddresses: StateFlow<List<ShippingAddress>> = _localAddresses.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val events = _events.asSharedFlow()

    private fun publishUi(message: String) {
        viewModelScope.launch { _events.emit(message) }
    }

    fun loadCatalogIfNeeded() {
        if (_catalogReady.value || _catalogLoading.value) return
        _catalogLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                commonServiceRepository.getCategoryTree().onSuccess { _categoryTree.value = it }
                commonServiceRepository.getAestheticTags(all = true).onSuccess { tags ->
                    _aestheticTags.value = tags
                    _aestheticTagsById.value = tags.associateBy { it.id }
                }
                commonServiceRepository.getBrands(limit = 50).onSuccess { page ->
                    _brandsFeatured.value = page.items
                    _brandsSearch.value = page.items
                }
                commonServiceRepository.getCountries(all = true).onSuccess { _countries.value = it }
            } finally {
                _catalogLoading.value = false
                _catalogReady.value = true
            }
        }
    }

    fun searchBrands(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            commonServiceRepository.getBrands(q = query.takeIf { it.isNotBlank() }, limit = 50).onSuccess {
                _brandsSearch.value = it.items
            }
        }
    }

    fun loadProfileForPreview() {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.getMeProfile().fold(
                onSuccess = { _meProfile.value = it },
                onFailure = { _meProfile.value = null },
            )
        }
    }

    fun loadLocalShippingAddresses() {
        val uid = app.authManager.sessionStore.read()?.userId ?: return
        _localAddresses.value = addressLocalStore.listAddresses(uid)
    }

    /** Syncs saved ship-from addresses from core-service; falls back to local cache on failure. */
    fun loadShippingAddresses() {
        val uid = app.authManager.sessionStore.read()?.userId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            userShippingAddressRepository.listShippingAddresses().fold(
                onSuccess = { api ->
                    val local = addressLocalStore.listAddresses(uid)
                    val merged = mergeShippingAddressesWithLocal(api, local)
                    addressLocalStore.saveAddresses(uid, merged)
                    _localAddresses.value = merged
                },
                onFailure = {
                    _localAddresses.value = addressLocalStore.listAddresses(uid)
                },
            )
        }
    }

    fun selectShippingAddressForListing(addressId: String) {
        val uid = app.authManager.sessionStore.read()?.userId ?: return
        val addr = addressLocalStore.listAddresses(uid).find { it.id == addressId } ?: return
        _draft.value = _draft.value.copy(
            shippingAddressId = addr.id,
            shippingAddressLabel = formatAddressLabel(addr),
        )
    }

    fun applyDefaultShippingIfNeeded() {
        val uid = app.authManager.sessionStore.read()?.userId ?: return
        val d = _draft.value
        if (d.shippingAddressId != null) return
        val def = addressLocalStore.getDefaultOrFirst(uid) ?: return
        _draft.value = d.copy(
            shippingAddressId = def.id,
            shippingAddressLabel = formatAddressLabel(def),
        )
    }

    private fun formatAddressLabel(a: ShippingAddress): String = a.labelForDraft()

    fun setImageUris(uris: List<Uri>) {
        _draft.value = _draft.value.withImageUris(uris.take(6).map { it.toString() })
    }

    fun removeImage(index: Int) {
        _draft.value = _draft.value.removeImageAtIndex(index)
    }

    fun nextStep() {
        val s = _step.value
        if (!_draft.value.canProceedFromStep(s)) return
        if (s < TotalPostSteps) {
            _step.value = s + 1
            when (_step.value) {
                9 -> {
                    loadShippingAddresses()
                    applyDefaultShippingIfNeeded()
                }
                10 -> {
                    loadProfileForPreview()
                    loadShippingAddresses()
                }
                else -> {}
            }
        }
    }

    fun prevStep() {
        if (_step.value > 1) _step.value = _step.value - 1
    }

    fun goToStep(stepNum: Int) {
        if (stepNum in 1..TotalPostSteps) _step.value = stepNum
    }

    fun updateDraft(block: CreateListingDraft.() -> CreateListingDraft) {
        _draft.value = block(_draft.value)
    }

    /**
     * Uploads images from draft and stores URLs in [CreateListingDraft.imageUrls].
     */
    suspend fun uploadImages(uriResolver: (Uri) -> Pair<ByteArray, String>?): Boolean {
        val uris = _draft.value.imageUris
        if (uris.isEmpty()) return true
        _isUploading.value = true
        val urls = mutableListOf<String>()
        for ((i, uriStr) in uris.withIndex()) {
            val uri = Uri.parse(uriStr)
            val data = withContext(Dispatchers.IO) { uriResolver(uri) }
            if (data == null || data.first.isEmpty()) {
                publishUi(getApplication<Application>().getString(R.string.create_listing_image_error))
                _isUploading.value = false
                return false
            }
            val (bytes, mimeType) = data
            val ext = mimeTypeToExt(mimeType)
            val result = withContext(Dispatchers.IO) {
                listingRepository.uploadListingImage(bytes, "image_$i.$ext", mimeType)
            }
            result.fold(
                onSuccess = { urls.add(it) },
                onFailure = {
                    publishUi(it.message ?: getApplication<Application>().getString(R.string.create_listing_upload_error))
                    _isUploading.value = false
                    return false
                },
            )
        }
        _draft.value = _draft.value.withImageUrls(urls)
        _isUploading.value = false
        return true
    }

    fun submitListing(uriResolver: (Uri) -> Pair<ByteArray, String>?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val d = _draft.value
                val tagsMap = _aestheticTagsById.value
                val errKey = d.validationErrorKeyForSubmit()
                if (errKey != null) {
                    publishUi(resolveValidationString(errKey))
                    return@launch
                }
                val finalUrls = when {
                    !BuildConfig.POST_REQUIRE_LISTING_IMAGES && d.imageUris.isEmpty() -> emptyList()
                    d.imageUrls.size == d.imageUris.size && d.imageUrls.isNotEmpty() -> d.imageUrls
                    else -> {
                        if (!uploadImages(uriResolver ?: { null })) return@launch
                        _draft.value.imageUrls
                    }
                }
                if (finalUrls.isEmpty() && BuildConfig.POST_REQUIRE_LISTING_IMAGES) {
                    publishUi(getApplication<Application>().getString(R.string.create_listing_no_images))
                    return@launch
                }
                val req = _draft.value.toCreateListingRequest(finalUrls, tagsMap)
                val createResult = withContext(Dispatchers.IO) {
                    listingRepository.createListing(req)
                }
                createResult.fold(
                    onSuccess = {
                        publishUi(getApplication<Application>().getString(R.string.create_listing_success))
                        resetDraft()
                        onSuccess()
                    },
                    onFailure = {
                        publishUi(it.message ?: getApplication<Application>().getString(R.string.create_listing_error))
                    },
                )
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    private fun resolveValidationString(key: String): String {
        val resId = getApplication<Application>().resources.getIdentifier(
            key,
            "string",
            getApplication<Application>().packageName,
        )
        return if (resId != 0) {
            getApplication<Application>().getString(resId)
        } else {
            key
        }
    }

    private fun mimeTypeToExt(mimeType: String): String = when (mimeType.lowercase()) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        else -> "jpg"
    }

    fun cancel() {
        resetDraft()
    }

    private fun resetDraft() {
        _draft.value = CreateListingDraft()
        _step.value = 1
    }
}
