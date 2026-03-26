package com.pc.fash_android_mobile.ui.listing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.http.CoreServiceHttpException
import com.pc.fash_android_mobile.data.listing.ListingDetail
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.listing.UpdateListingRequest
import com.pc.fash_android_mobile.data.user.AestheticTag
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

private const val TITLE_MIN = 3
private const val TITLE_MAX = 60
private const val DESC_MAX = 500
private const val CONDITION_MAX = 30
private const val SIZE_MAX = 20
private const val BRAND_MAX = 50
private const val MAX_TAGS = 5
private const val PRICE_MIN = 1_000L
private const val PRICE_MAX = 100_000_000L

data class EditListingFormState(
    val title: String = "",
    val description: String = "",
    val priceText: String = "",
    val condition: String = "",
    val size: String = "",
    val brand: String = "",
    /** Selected aesthetic tag ids (max [MAX_TAGS]). */
    val selectedTagIds: Set<String> = emptySet(),
)

class EditListingViewModel(application: Application) : AndroidViewModel(application) {

    private val listingRepository: ListingRepository =
        (application as FashApplication).listingRepository
    private val userRepository: UserRepository =
        (application as FashApplication).userRepository

    private val _detail = MutableStateFlow<ListingDetail?>(null)
    val detail: StateFlow<ListingDetail?> = _detail.asStateFlow()

    private val _form = MutableStateFlow(EditListingFormState())
    val form: StateFlow<EditListingFormState> = _form.asStateFlow()

    private val _catalogTags = MutableStateFlow<List<AestheticTag>>(emptyList())
    val catalogTags: StateFlow<List<AestheticTag>> = _catalogTags.asStateFlow()

    /** Tag ids from the server when the form was last loaded or saved — for delta PUT semantics. */
    private val _baselineTagIds = MutableStateFlow<Set<String>>(emptySet())
    val baselineTagIds: StateFlow<Set<String>> = _baselineTagIds.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    private var activeListingId: String = ""

    fun load(listingId: String) {
        if (listingId.isBlank()) {
            _loadError.value = getApplication<Application>().getString(R.string.edit_listing_load_error)
            _isLoading.value = false
            return
        }
        activeListingId = listingId.trim()
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            _detail.value = null
            withContext(Dispatchers.IO) {
                val catalog = userRepository.getAestheticTags().getOrNull().orEmpty()
                _catalogTags.value = catalog
                listingRepository.getListingDetail(activeListingId).fold(
                    onSuccess = { d ->
                        _detail.value = d
                        val selectedIds = matchTagIds(d.tags, catalog)
                        _baselineTagIds.value = selectedIds
                        _form.value = EditListingFormState(
                            title = d.title,
                            description = d.description.take(DESC_MAX),
                            priceText = if (d.priceVnd > 0) d.priceVnd.toString() else "",
                            condition = d.condition.take(CONDITION_MAX),
                            size = d.size.orEmpty().take(SIZE_MAX),
                            brand = d.brand.orEmpty().take(BRAND_MAX),
                            selectedTagIds = selectedIds,
                        )
                    },
                    onFailure = {
                        _loadError.value = it.message?.takeIf { m -> m.isNotBlank() }
                            ?: getApplication<Application>().getString(R.string.edit_listing_load_error)
                    },
                )
            }
            _isLoading.value = false
        }
    }

    private fun matchTagIds(listingTagStrings: List<String>, catalog: List<AestheticTag>): Set<String> {
        if (listingTagStrings.isEmpty() || catalog.isEmpty()) return emptySet()
        val out = mutableSetOf<String>()
        for (s in listingTagStrings) {
            val t = s.trim()
            if (t.isEmpty()) continue
            val byId = catalog.find { it.id.equals(t, ignoreCase = true) }
            if (byId != null) {
                out.add(byId.id)
                continue
            }
            val byName = catalog.find { it.name.equals(t, ignoreCase = true) }
            if (byName != null) out.add(byName.id)
        }
        return out
    }

    fun updateForm(transform: EditListingFormState.() -> EditListingFormState) {
        _form.update(transform)
    }

    fun toggleTag(tagId: String) {
        _form.update { cur ->
            val next = cur.selectedTagIds.toMutableSet()
            if (tagId in next) next.remove(tagId)
            else if (next.size < MAX_TAGS) next.add(tagId)
            cur.copy(selectedTagIds = next)
        }
    }

    /**
     * core-service: `PUT` accepts optional fields only; omit unchanged fields.
     * Omit `aesthetic_tags` when unchanged; send `[]` to clear; send **tag names** (not ids) to replace.
     */
    private fun buildDeltaUpdate(
        d: ListingDetail,
        f: EditListingFormState,
        baselineTags: Set<String>,
    ): UpdateListingRequest? {
        val title = f.title.trim()
        val desc = f.description.take(DESC_MAX)
        val price = f.priceText.trim().toLongOrNull() ?: return null
        val cond = f.condition.trim().take(CONDITION_MAX)
        val size = f.size.trim().take(SIZE_MAX)
        val brand = f.brand.trim().take(BRAND_MAX)
        val dDesc = d.description.take(DESC_MAX)
        val dSize = (d.size ?: "").trim().take(SIZE_MAX)
        val dBrand = (d.brand ?: "").trim().take(BRAND_MAX)
        val dCond = d.condition.trim().take(CONDITION_MAX)

        val titleP = if (title != d.title) title else null
        val descP = if (desc != dDesc) desc else null
        val priceP = if (price != d.priceVnd) price else null
        val condP = if (cond != dCond) cond else null
        val sizeP = if (size != dSize) size else null
        val brandP = if (brand != dBrand) brand else null
        val tagsP = if (f.selectedTagIds != baselineTags) {
            f.selectedTagIds
                .mapNotNull { id -> _catalogTags.value.find { it.id == id }?.name?.trim() }
                .filter { it.isNotBlank() }
                .sorted()
        } else {
            null
        }

        if (titleP == null && descP == null && priceP == null && condP == null &&
            sizeP == null && brandP == null && tagsP == null
        ) {
            return null
        }
        return UpdateListingRequest(
            title = titleP,
            condition = condP,
            priceVnd = priceP,
            description = descP,
            brand = brandP,
            size = sizeP,
            aestheticTags = tagsP,
        )
    }

    private fun hasChanges(): Boolean {
        val d = _detail.value ?: return false
        return buildDeltaUpdate(d, _form.value, _baselineTagIds.value) != null
    }

    /** After a successful save, align local detail + baseline so the next save is a delta. */
    private fun snapDetailAndBaselineFromForm() {
        val d = _detail.value ?: return
        val f = _form.value
        val title = f.title.trim()
        val desc = f.description.take(DESC_MAX)
        val price = f.priceText.trim().toLongOrNull() ?: return
        val cond = f.condition.trim().take(CONDITION_MAX)
        val size = f.size.trim().take(SIZE_MAX).ifBlank { null }
        val brand = f.brand.trim().take(BRAND_MAX).ifBlank { null }
        val tagNames = f.selectedTagIds.mapNotNull { id -> _catalogTags.value.find { it.id == id }?.name }
        _detail.value = d.copy(
            title = title,
            description = desc,
            priceVnd = price,
            condition = cond,
            size = size,
            brand = brand,
            tags = tagNames,
        )
        _baselineTagIds.value = f.selectedTagIds.toSet()
    }

    fun save() {
        val d = _detail.value ?: return
        if (!d.status.equals("active", ignoreCase = true)) {
            viewModelScope.launch {
                _events.emit(getApplication<Application>().getString(R.string.edit_listing_not_editable))
            }
            return
        }
        val f = _form.value
        val title = f.title.trim()
        if (title.length !in TITLE_MIN..TITLE_MAX) {
            viewModelScope.launch {
                _events.emit(getApplication<Application>().getString(R.string.edit_listing_title_invalid))
            }
            return
        }
        val price = f.priceText.trim().toLongOrNull()
        if (price == null || price !in PRICE_MIN..PRICE_MAX) {
            viewModelScope.launch {
                _events.emit(getApplication<Application>().getString(R.string.edit_listing_price_invalid))
            }
            return
        }
        if (f.condition.isBlank()) {
            viewModelScope.launch {
                _events.emit(getApplication<Application>().getString(R.string.create_listing_step2_need_condition))
            }
            return
        }
        val update = buildDeltaUpdate(d, f, _baselineTagIds.value)
        if (update == null) {
            viewModelScope.launch {
                _events.emit(getApplication<Application>().getString(R.string.edit_listing_no_changes))
            }
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            val result = withContext(Dispatchers.IO) {
                listingRepository.updateListing(activeListingId, update)
            }
            _isSaving.value = false
            result.fold(
                onSuccess = {
                    snapDetailAndBaselineFromForm()
                    _events.emit(getApplication<Application>().getString(R.string.edit_listing_saved))
                },
                onFailure = { e ->
                    val msg = when (e) {
                        is CoreServiceHttpException -> when (e.httpCode) {
                            409 -> e.message?.takeIf { m -> m.isNotBlank() }
                                ?: getApplication<Application>().getString(R.string.edit_listing_not_editable)
                            else -> e.message?.takeIf { m -> m.isNotBlank() }
                                ?: getApplication<Application>().getString(R.string.edit_listing_save_error)
                        }
                        else -> e.message?.takeIf { m -> m.isNotBlank() }
                            ?: getApplication<Application>().getString(R.string.edit_listing_save_error)
                    }
                    _events.emit(msg)
                },
            )
        }
    }

    fun delete() {
        viewModelScope.launch {
            _isDeleting.value = true
            val result = withContext(Dispatchers.IO) {
                listingRepository.deleteListing(activeListingId)
            }
            _isDeleting.value = false
            result.fold(
                onSuccess = {
                    _events.emit(getApplication<Application>().getString(R.string.edit_listing_deleted))
                },
                onFailure = { e ->
                    val msg = when (e) {
                        is CoreServiceHttpException -> e.message?.takeIf { m -> m.isNotBlank() }
                            ?: getApplication<Application>().getString(R.string.edit_listing_delete_error)
                        else -> e.message?.takeIf { m -> m.isNotBlank() }
                            ?: getApplication<Application>().getString(R.string.edit_listing_delete_error)
                    }
                    _events.emit(msg)
                },
            )
        }
    }

    fun canSave(): Boolean {
        val d = _detail.value ?: return false
        if (!d.status.equals("active", ignoreCase = true)) return false
        val f = _form.value
        val title = f.title.trim()
        val price = f.priceText.trim().toLongOrNull()
        if (title.length !in TITLE_MIN..TITLE_MAX) return false
        if (price == null || price !in PRICE_MIN..PRICE_MAX) return false
        if (f.condition.isBlank()) return false
        if (!hasChanges()) return false
        return !_isSaving.value
    }
}
