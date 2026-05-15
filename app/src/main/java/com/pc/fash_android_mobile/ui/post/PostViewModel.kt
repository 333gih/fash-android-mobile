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
import com.pc.fash_android_mobile.data.common.defaultListingImageCatalogSteps
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.data.user.UserRepository
import com.pc.fash_android_mobile.ui.common.showUiDialogSuccess
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

    /** True while bottom-nav re-tap reloads catalog / profile for the Post tab. */
    private val _navReselectLoading = MutableStateFlow(false)
    val navReselectLoading: StateFlow<Boolean> = _navReselectLoading.asStateFlow()

    private val _catalogReady = MutableStateFlow(false)
    val catalogReady: StateFlow<Boolean> = _catalogReady.asStateFlow()

    private val _meProfile = MutableStateFlow<ProfileInfo?>(null)
    val meProfile: StateFlow<ProfileInfo?> = _meProfile.asStateFlow()

    private val _localAddresses = MutableStateFlow<List<ShippingAddress>>(emptyList())
    val localAddresses: StateFlow<List<ShippingAddress>> = _localAddresses.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _listingPhotoSetupLoading = MutableStateFlow(false)
    val listingPhotoSetupLoading: StateFlow<Boolean> = _listingPhotoSetupLoading.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val events = _events.asSharedFlow()

    private fun publishUi(message: String) {
        viewModelScope.launch { _events.emit(message) }
    }

    fun loadCatalogIfNeeded() {
        if (_catalogReady.value || _catalogLoading.value) return
        viewModelScope.launch { loadCatalogInternal() }
    }

    /** Bottom nav re-tap on Post — refresh listing metadata without leaving the current step. */
    fun reloadOnNavReselect() {
        viewModelScope.launch {
            _navReselectLoading.value = true
            try {
                loadCatalogInternal(force = true)
                loadProfileForPreview()
                loadLocalShippingAddresses()
                loadShippingAddresses()
            } finally {
                _navReselectLoading.value = false
            }
        }
    }

    private suspend fun loadCatalogInternal(force: Boolean = false) {
        if (!force && (_catalogReady.value || _catalogLoading.value)) return
        _catalogLoading.value = true
        withContext(Dispatchers.IO) {
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

    /** Clears in-progress listing draft and user-specific preview state after logout. */
    fun clearCachesForSignedOutUser() {
        _draft.value = CreateListingDraft()
        _step.value = 1
        _meProfile.value = null
        _localAddresses.value = emptyList()
        _isUploading.value = false
        _isSubmitting.value = false
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

    /**
     * Loads common-service listing image steps for the draft leaf [CreateListingDraft.categoryId]
     * and merges any existing picks by [ListingPhotoSlotDraft.stepKey].
     */
    suspend fun ensureListingPhotoSlotsLoaded() {
        val cat = _draft.value.categoryId.trim()
        if (cat.isEmpty()) return
        if (_draft.value.listingPhotoSlotsCategoryId == cat && _draft.value.listingPhotoSlots.isNotEmpty()) return
        _listingPhotoSetupLoading.value = true
        try {
            val catalog = withContext(Dispatchers.IO) {
                commonServiceRepository.getListingImageSetup(cat).getOrNull()?.steps
                    ?: defaultListingImageCatalogSteps()
            }
            _draft.value = _draft.value.withListingPhotoSlotsFromCatalog(cat, catalog)
        } finally {
            _listingPhotoSetupLoading.value = false
        }
    }

    fun setListingPhotoForStep(stepKey: String, uriString: String?) {
        _draft.value = _draft.value.copy(
            listingPhotoSlots = _draft.value.listingPhotoSlots.map { s ->
                if (s.stepKey != stepKey) {
                    s
                } else {
                    s.copy(
                        localImageUri = uriString?.takeIf { it.isNotBlank() },
                        uploadedImageUrl = null,
                    )
                }
            },
        )
    }

    fun clearListingPhotoForStep(stepKey: String) {
        setListingPhotoForStep(stepKey, null)
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
     * Uploads each slot that has a new local [ListingPhotoSlotDraft.localImageUri] via `POST /listings/images`.
     */
    suspend fun uploadImages(uriResolver: (Uri) -> Pair<ByteArray, String>?): Boolean {
        val slots = _draft.value.listingPhotoSlots
        if (slots.isEmpty()) return true
        val toUpload = slots.filter {
            it.localImageUri?.isNotBlank() == true && it.uploadedImageUrl.isNullOrBlank()
        }
        if (toUpload.isEmpty()) return true
        _isUploading.value = true
        try {
            var fileIndex = 0
            for (slot in toUpload.sortedBy { it.sortOrder }) {
                val local = slot.localImageUri ?: continue
                val uri = Uri.parse(local)
                val data = withContext(Dispatchers.IO) { uriResolver(uri) }
                if (data == null || data.first.isEmpty()) {
                    publishUi(getApplication<Application>().getString(R.string.create_listing_image_error))
                    return false
                }
                val (bytes, mimeType) = data
                val ext = mimeTypeToExt(mimeType)
                val slug = slot.stepKey.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(32).ifBlank { "img" }
                val result = withContext(Dispatchers.IO) {
                    listingRepository.uploadListingImage(bytes, "${slug}_$fileIndex.$ext", mimeType)
                }
                fileIndex++
                val uploaded = result.fold(
                    onSuccess = { it },
                    onFailure = {
                        publishUi(it.message ?: getApplication<Application>().getString(R.string.create_listing_upload_error))
                        return false
                    },
                )
                _draft.value = _draft.value.copy(
                    listingPhotoSlots = _draft.value.listingPhotoSlots.map { s ->
                        if (s.stepKey != slot.stepKey) {
                            s
                        } else {
                            s.copy(uploadedImageUrl = uploaded, localImageUri = null)
                        }
                    },
                )
            }
            return true
        } finally {
            _isUploading.value = false
        }
    }

    fun submitListing(uriResolver: (Uri) -> Pair<ByteArray, String>?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                ensureListingPhotoSlotsLoaded()
                val d = _draft.value
                val tagsMap = _aestheticTagsById.value
                val errKey = d.validationErrorKeyForSubmit()
                if (errKey != null) {
                    publishUi(resolveValidationString(errKey))
                    return@launch
                }
                val anyPhoto = d.listingPhotoSlots.any { it.hasImageSelected() }
                val skipImagesEntirely = !BuildConfig.POST_REQUIRE_LISTING_IMAGES && !anyPhoto
                val stepsPayload = when {
                    skipImagesEntirely -> emptyList()
                    else -> {
                        val needsUpload = d.listingPhotoSlots.any {
                            it.localImageUri?.isNotBlank() == true && it.uploadedImageUrl.isNullOrBlank()
                        }
                        if (needsUpload) {
                            if (!uploadImages(uriResolver ?: { null })) return@launch
                        }
                        _draft.value.buildListingImageStepPayloads()
                    }
                }
                if (stepsPayload.isEmpty() && BuildConfig.POST_REQUIRE_LISTING_IMAGES) {
                    publishUi(getApplication<Application>().getString(R.string.create_listing_no_images))
                    return@launch
                }
                val req = _draft.value.toCreateListingRequest(stepsPayload, tagsMap)
                val createResult = withContext(Dispatchers.IO) {
                    listingRepository.createListing(req)
                }
                createResult.fold(
                    onSuccess = {
                        val appCtx = getApplication<Application>()
                        showUiDialogSuccess(
                            message = appCtx.getString(R.string.create_listing_success_dialog_message),
                            title = appCtx.getString(R.string.create_listing_success_dialog_title),
                        )
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
