package com.pc.fash_android_mobile.ui.listing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.http.CoreServiceHttpException
import com.pc.fash_android_mobile.data.listing.ListingDetail
import com.pc.fash_android_mobile.data.common.CommonAestheticTagDto
import com.pc.fash_android_mobile.data.common.CommonBrandDto
import com.pc.fash_android_mobile.data.common.CommonCountryDto
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.listing.UpdateListingRequest
import com.pc.fash_android_mobile.data.listing.isListingStatusSellerPutAllowed
import com.pc.fash_android_mobile.data.user.UserRepository
import com.pc.fash_android_mobile.ui.post.ListingConditionOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

private const val TITLE_MIN = 3
private const val TITLE_MAX = 60
private const val DESC_MAX = 500
private const val CONDITION_MAX = 30
private const val SIZE_MAX = 20
private const val BRAND_MAX = 50
private const val MAX_TAGS = 5
private const val PRICE_MIN = 1_000L
private const val PRICE_MAX = 100_000_000L

private fun parsePositiveLong(s: String): Long? =
    s.trim().replace(".", "").replace(",", "").toLongOrNull()?.takeIf { it > 0 }

private fun parseDoubleField(s: String): Double? =
    s.trim().replace(",", ".").toDoubleOrNull()

private fun formatMeasurementField(d: Double?): String {
    if (d == null) return ""
    val v = d
    return if (abs(v % 1.0) < 1e-6) {
        v.toInt().toString()
    } else {
        String.format(java.util.Locale.US, "%.1f", v)
    }
}

private fun normalizeChoice(raw: String?): String =
    raw?.trim()?.lowercase(java.util.Locale.ROOT).orEmpty()

private fun originalPriceText(d: ListingDetail): String =
    if (d.priceVnd > 0) d.priceVnd.toString() else ""

private fun parsedPriceDropPercent(input: String): Int? {
    val d = input.filter { it.isDigit() }.take(2)
    if (d.isEmpty()) return null
    val n = d.toIntOrNull() ?: return null
    return n.takeIf { it in 1..50 }
}

/** Prefer form text; if blank, use listing detail (API may omit floor or send 0 while auto-drop is on). */
private fun effectiveFloorPriceForAutoDrop(f: EditListingFormState, d: ListingDetail): Long? {
    val fromForm = parsePositiveLong(f.floorPriceText)
    if (fromForm != null) return fromForm
    return d.floorPriceVnd?.takeIf { it > 0 }
}

private fun effectivePriceDropPercentForAutoDrop(f: EditListingFormState, d: ListingDetail): Int? {
    val fromForm = parsedPriceDropPercent(f.priceDropPercentInput)
    if (fromForm != null) return fromForm
    return d.priceDropPercent?.takeIf { it in 1..50 }
}

class EditListingViewModel(application: Application) : AndroidViewModel(application) {

    private val listingRepository: ListingRepository =
        (application as FashApplication).listingRepository
    private val userRepository: UserRepository =
        (application as FashApplication).userRepository
    private val commonServiceRepository =
        (application as FashApplication).commonServiceRepository

    private val _detail = MutableStateFlow<ListingDetail?>(null)
    val detail: StateFlow<ListingDetail?> = _detail.asStateFlow()

    private val _form = MutableStateFlow(EditListingFormState())
    val form: StateFlow<EditListingFormState> = _form.asStateFlow()

    private val _catalogTags = MutableStateFlow<List<CommonAestheticTagDto>>(emptyList())
    val catalogTags: StateFlow<List<CommonAestheticTagDto>> = _catalogTags.asStateFlow()

    private val _brandsFeatured = MutableStateFlow<List<CommonBrandDto>>(emptyList())
    val brandsFeatured: StateFlow<List<CommonBrandDto>> = _brandsFeatured.asStateFlow()

    private val _brandsSearch = MutableStateFlow<List<CommonBrandDto>>(emptyList())
    val brandsSearch: StateFlow<List<CommonBrandDto>> = _brandsSearch.asStateFlow()

    private val _countries = MutableStateFlow<List<CommonCountryDto>>(emptyList())
    val countries: StateFlow<List<CommonCountryDto>> = _countries.asStateFlow()

    private val _countrySearch = MutableStateFlow<List<CommonCountryDto>>(emptyList())
    val countrySearch: StateFlow<List<CommonCountryDto>> = _countrySearch.asStateFlow()

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

    /** Reactive save eligibility — UI must collect this instead of calling [canSave] directly. */
    val canSave: StateFlow<Boolean> = combine(
        _detail,
        _form,
        _baselineTagIds,
        _isSaving,
    ) { detail, form, baselineTags, saving ->
        computeCanSave(detail, form, baselineTags, saving)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

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
                commonServiceRepository.getBrands(limit = 50).onSuccess { page ->
                    _brandsFeatured.value = page.items
                    _brandsSearch.value = page.items
                }
                commonServiceRepository.getCountries(all = true).onSuccess { list ->
                    _countries.value = list
                    _countrySearch.value = list
                }
                val catalog = commonServiceRepository.getAestheticTags(all = true).getOrNull().orEmpty()
                _catalogTags.value = catalog
                listingRepository.getListingDetail(activeListingId).fold(
                    onSuccess = { d ->
                        _detail.value = d
                        val selectedIds = baselineTagIdsFromDetail(d, catalog)
                        _baselineTagIds.value = selectedIds
                        val pct = d.priceDropPercent?.takeIf { it in 1..50 }?.toString() ?: "10"
                        _form.value = EditListingFormState(
                            title = d.title,
                            description = d.description.take(DESC_MAX),
                            priceText = if (d.priceVnd > 0) d.priceVnd.toString() else "",
                            condition = ListingConditionOptions.normalizeApiToUi(d.condition).take(CONDITION_MAX),
                            size = d.size.orEmpty().take(SIZE_MAX),
                            brandId = d.brandId,
                            brandName = d.brand.orEmpty().take(BRAND_MAX),
                            selectedTagIds = selectedIds,
                            countryId = d.countryId,
                            countryIso2 = d.countryIso2.orEmpty(),
                            countryName = d.countryName.orEmpty(),
                            measurementUnit = d.measurementUnit?.trim()?.takeIf { it.isNotEmpty() } ?: "cm",
                            measurementHem = formatMeasurementField(d.measurementHem),
                            measurementChest = formatMeasurementField(d.measurementChest),
                            measurementLength = formatMeasurementField(d.measurementLength),
                            measurementShoulders = formatMeasurementField(d.measurementShoulders),
                            measurementSleeveLength = formatMeasurementField(d.measurementSleeveLength),
                            acceptOffers = d.acceptOffers,
                            autoPriceDropEnabled = d.autoPriceDropEnabled,
                            floorPriceText = d.floorPriceVnd?.takeIf { it > 0 }?.toString() ?: "",
                            priceDropPercentInput = pct,
                            color = normalizeChoice(d.color),
                            genderTarget = normalizeChoice(d.genderTarget),
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

    private fun baselineTagIdsFromDetail(d: ListingDetail, catalog: List<CommonAestheticTagDto>): Set<String> {
        if (catalog.isEmpty()) return emptySet()
        if (d.aestheticTagRefs.isNotEmpty()) {
            val fromIds = d.aestheticTagRefs.mapNotNull { ref ->
                ref.id?.takeIf { id -> catalog.any { it.id == id } }
            }.toSet()
            if (fromIds.isNotEmpty()) return fromIds
        }
        val strings = if (d.aestheticTags.isNotEmpty()) d.aestheticTags else d.tags
        return matchTagIds(strings, catalog)
    }

    private fun matchTagIds(listingTagStrings: List<String>, catalog: List<CommonAestheticTagDto>): Set<String> {
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
            val byName = catalog.find { tag ->
                tag.name.equals(t, ignoreCase = true) || tag.displayName.equals(t, ignoreCase = true)
            }
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

    fun searchBrands(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            commonServiceRepository.getBrands(q = query.takeIf { it.isNotBlank() }, limit = 50).onSuccess {
                _brandsSearch.value = it.items
            }
        }
    }

    fun selectBrand(brand: CommonBrandDto?) {
        _form.update { cur ->
            if (brand == null) {
                cur.copy(brandId = null, brandName = "")
            } else {
                cur.copy(brandId = brand.id, brandName = brand.name.take(BRAND_MAX))
            }
        }
    }

    fun searchCountries(query: String) {
        val base = _countries.value
        if (query.isBlank()) {
            _countrySearch.value = base
            return
        }
        val q = query.trim().lowercase()
        _countrySearch.value = base.filter {
            it.name.lowercase().contains(q) ||
                it.iso2.lowercase().contains(q) ||
                it.iso3.lowercase().contains(q)
        }
    }

    fun selectCountry(country: CommonCountryDto?) {
        _form.update { cur ->
            if (country == null) {
                cur.copy(countryId = null, countryIso2 = "", countryName = "")
            } else {
                cur.copy(
                    countryId = country.id,
                    countryIso2 = country.iso2,
                    countryName = country.name,
                )
            }
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
        if (!hasFormChanges(d, f, baselineTags)) return null

        val title = f.title.trim()
        val desc = f.description.take(DESC_MAX)
        val price = f.priceText.trim().toLongOrNull() ?: return null
        val size = f.size.trim().take(SIZE_MAX)
        val bn = f.brandName.trim().take(BRAND_MAX)
        val dDesc = d.description.take(DESC_MAX)
        val dSize = (d.size ?: "").trim().take(SIZE_MAX)
        val dBrand = (d.brand ?: "").trim().take(BRAND_MAX)
        val dBrandId = d.brandId
        val titleP = if (title != d.title) title else null
        val descP = if (desc != dDesc) desc else null
        val priceP = if (price != d.priceVnd) price else null
        val formCondCanon = ListingConditionOptions.canonicalApi(f.condition)
        val detailCondCanon = ListingConditionOptions.canonicalApi(d.condition)
        val condP = if (formCondCanon != detailCondCanon) {
            ListingConditionOptions.normalizeUiToApi(f.condition.trim())
        } else {
            null
        }
        val sizeP = if (size != dSize) size else null
        val brandChanged = bn != dBrand || f.brandId != dBrandId
        val brandIdP = if (brandChanged) f.brandId?.takeIf { it.isNotBlank() } else null
        val brandNameP = if (brandChanged) bn else null
        val tagsP = if (f.selectedTagIds != baselineTags) {
            f.selectedTagIds
                .mapNotNull { id -> _catalogTags.value.find { it.id == id }?.name?.trim() }
                .filter { it.isNotBlank() }
                .sorted()
        } else {
            null
        }

        val coIso = f.countryIso2.trim().uppercase()
        val dCo = d.countryIso2?.trim()?.uppercase().orEmpty()
        val countryOfOriginP = if (coIso != dCo) coIso.takeIf { it.length == 2 } else null
        val countryIdP = if (f.countryId != d.countryId) f.countryId else null
        val countryNameP = if (f.countryName.trim() != (d.countryName ?: "").trim()) f.countryName.trim() else null

        val unitForm = f.measurementUnit.trim().lowercase()
        val unitD = d.measurementUnit?.trim()?.lowercase().orEmpty().ifEmpty { "cm" }
        val measurementUnitP = if (unitForm != unitD) unitForm else null

        fun deltaMeasurement(form: String, detail: Double?): Double? {
            val parsed = parseDoubleField(form)
            if (measurementEq(parsed, detail)) return null
            return parsed ?: 0.0
        }
        val hemP = deltaMeasurement(f.measurementHem, d.measurementHem)
        val chestP = deltaMeasurement(f.measurementChest, d.measurementChest)
        val lenP = deltaMeasurement(f.measurementLength, d.measurementLength)
        val shP = deltaMeasurement(f.measurementShoulders, d.measurementShoulders)
        val slP = deltaMeasurement(f.measurementSleeveLength, d.measurementSleeveLength)

        val acceptP = if (f.acceptOffers != d.acceptOffers) f.acceptOffers else null
        val autoP = if (f.autoPriceDropEnabled != d.autoPriceDropEnabled) f.autoPriceDropEnabled else null
        val floorParsed = parsePositiveLong(f.floorPriceText)
        val floorP = if (f.autoPriceDropEnabled && floorParsed != d.floorPriceVnd) floorParsed else null
        val percentP = if (f.autoPriceDropEnabled) {
            val p = parsedPriceDropPercent(f.priceDropPercentInput)
            if (p != null && p != d.priceDropPercent) p else null
        } else {
            null
        }

        val colorP = normalizeChoice(f.color).takeIf { it != normalizeChoice(d.color) }
        val genderTargetP = normalizeChoice(f.genderTarget).takeIf { it != normalizeChoice(d.genderTarget) }

        return UpdateListingRequest(
            title = titleP,
            condition = condP,
            priceVnd = priceP,
            description = descP,
            brandId = brandIdP,
            brandName = brandNameP,
            size = sizeP,
            aestheticTags = tagsP,
            acceptOffers = acceptP,
            autoPriceDropEnabled = autoP,
            floorPriceVnd = floorP,
            priceDropPercent = percentP,
            countryOfOrigin = countryOfOriginP,
            countryId = countryIdP,
            countryName = countryNameP,
            measurementUnit = measurementUnitP,
            measurementHem = hemP,
            measurementChest = chestP,
            measurementLength = lenP,
            measurementShoulders = shP,
            measurementSleeveLength = slP,
            color = colorP,
            genderTarget = genderTargetP,
        )
    }

    /** Detects any editable delta without requiring a parseable price (used for save-button state). */
    private fun hasFormChanges(
        d: ListingDetail,
        f: EditListingFormState,
        baselineTags: Set<String>,
    ): Boolean {
        val title = f.title.trim()
        val desc = f.description.take(DESC_MAX)
        val size = f.size.trim().take(SIZE_MAX)
        val bn = f.brandName.trim().take(BRAND_MAX)
        val dDesc = d.description.take(DESC_MAX)
        val dSize = (d.size ?: "").trim().take(SIZE_MAX)
        val dBrand = (d.brand ?: "").trim().take(BRAND_MAX)
        if (title != d.title) return true
        if (desc != dDesc) return true
        if (f.priceText.trim() != originalPriceText(d)) return true
        if (ListingConditionOptions.canonicalApi(f.condition) != ListingConditionOptions.canonicalApi(d.condition)) {
            return true
        }
        if (size != dSize) return true
        if (bn != dBrand || f.brandId != d.brandId) return true
        if (f.selectedTagIds != baselineTags) return true
        val coIso = f.countryIso2.trim().uppercase()
        val dCo = d.countryIso2?.trim()?.uppercase().orEmpty()
        if (coIso != dCo) return true
        if (f.countryId != d.countryId) return true
        if (f.countryName.trim() != (d.countryName ?: "").trim()) return true
        val unitForm = f.measurementUnit.trim().lowercase()
        val unitD = d.measurementUnit?.trim()?.lowercase().orEmpty().ifEmpty { "cm" }
        if (unitForm != unitD) return true
        if (!measurementEq(parseDoubleField(f.measurementHem), d.measurementHem)) return true
        if (!measurementEq(parseDoubleField(f.measurementChest), d.measurementChest)) return true
        if (!measurementEq(parseDoubleField(f.measurementLength), d.measurementLength)) return true
        if (!measurementEq(parseDoubleField(f.measurementShoulders), d.measurementShoulders)) return true
        if (!measurementEq(parseDoubleField(f.measurementSleeveLength), d.measurementSleeveLength)) return true
        if (f.acceptOffers != d.acceptOffers) return true
        if (f.autoPriceDropEnabled != d.autoPriceDropEnabled) return true
        if (f.autoPriceDropEnabled) {
            val floorParsed = parsePositiveLong(f.floorPriceText)
            if (floorParsed != d.floorPriceVnd) return true
            val p = parsedPriceDropPercent(f.priceDropPercentInput)
            if (p != null && p != d.priceDropPercent) return true
        }
        if (normalizeChoice(f.color) != normalizeChoice(d.color)) return true
        if (normalizeChoice(f.genderTarget) != normalizeChoice(d.genderTarget)) return true
        return false
    }

    private fun measurementEq(a: Double?, b: Double?): Boolean {
        if (a == null && b == null) return true
        if (a == null || b == null) return false
        return abs(a - b) < 1e-6
    }

    private fun hasChanges(d: ListingDetail, f: EditListingFormState, baselineTags: Set<String>): Boolean =
        hasFormChanges(d, f, baselineTags)

    private fun computeCanSave(
        detail: ListingDetail?,
        form: EditListingFormState,
        baselineTags: Set<String>,
        saving: Boolean,
    ): Boolean {
        val d = detail ?: return false
        if (!isListingStatusSellerPutAllowed(d.status)) return false
        val title = form.title.trim()
        val price = form.priceText.trim().toLongOrNull()
        if (title.length !in TITLE_MIN..TITLE_MAX) return false
        if (price == null || price !in PRICE_MIN..PRICE_MAX) return false
        if (form.condition.isBlank()) return false
        if (form.autoPriceDropEnabled) {
            val floor = effectiveFloorPriceForAutoDrop(form, d)
            if (floor == null || floor !in PRICE_MIN..PRICE_MAX) return false
            if (floor >= price) return false
            if (effectivePriceDropPercentForAutoDrop(form, d) == null) return false
        }
        if (!hasChanges(d, form, baselineTags)) return false
        return !saving
    }

    /** After a successful save, align local detail + baseline so the next save is a delta. */
    private fun snapDetailAndBaselineFromForm() {
        val d = _detail.value ?: return
        val f = _form.value
        val title = f.title.trim()
        val desc = f.description.take(DESC_MAX)
        val price = f.priceText.trim().toLongOrNull() ?: return
        val size = f.size.trim().take(SIZE_MAX).ifBlank { null }
        val brand = f.brandName.trim().take(BRAND_MAX).ifBlank { null }
        val condApi = ListingConditionOptions.normalizeUiToApi(f.condition.trim())
        val tagNames = f.selectedTagIds.mapNotNull { id -> _catalogTags.value.find { it.id == id }?.name }
        val floorSnap = if (f.autoPriceDropEnabled) parsePositiveLong(f.floorPriceText) else null
        val pctSnap = if (f.autoPriceDropEnabled) parsedPriceDropPercent(f.priceDropPercentInput) else null
        _detail.value = d.copy(
            title = title,
            description = desc,
            priceVnd = price,
            condition = condApi,
            size = size,
            brand = brand,
            brandId = f.brandId,
            tags = tagNames,
            aestheticTags = tagNames,
            countryId = f.countryId,
            countryName = f.countryName.trim().ifBlank { null },
            countryIso2 = f.countryIso2.trim().takeIf { it.length == 2 },
            measurementUnit = f.measurementUnit.trim().ifBlank { null },
            measurementHem = parseDoubleField(f.measurementHem),
            measurementChest = parseDoubleField(f.measurementChest),
            measurementLength = parseDoubleField(f.measurementLength),
            measurementShoulders = parseDoubleField(f.measurementShoulders),
            measurementSleeveLength = parseDoubleField(f.measurementSleeveLength),
            acceptOffers = f.acceptOffers,
            autoPriceDropEnabled = f.autoPriceDropEnabled,
            floorPriceVnd = floorSnap,
            priceDropPercent = pctSnap,
            color = normalizeChoice(f.color).ifBlank { null },
            genderTarget = normalizeChoice(f.genderTarget).ifBlank { null },
        )
        _baselineTagIds.value = f.selectedTagIds.toSet()
    }

    fun save() {
        val d = _detail.value ?: return
        if (!isListingStatusSellerPutAllowed(d.status)) {
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
                _events.emit(getApplication<Application>().getString(R.string.post_validation_condition))
            }
            return
        }
        if (f.autoPriceDropEnabled) {
            val floor = effectiveFloorPriceForAutoDrop(f, d)
            if (floor == null || floor !in PRICE_MIN..PRICE_MAX) {
                viewModelScope.launch {
                    _events.emit(getApplication<Application>().getString(R.string.post_validation_floor))
                }
                return
            }
            if (floor >= price) {
                viewModelScope.launch {
                    _events.emit(getApplication<Application>().getString(R.string.post_validation_floor_below_price))
                }
                return
            }
            if (effectivePriceDropPercentForAutoDrop(f, d) == null) {
                viewModelScope.launch {
                    _events.emit(getApplication<Application>().getString(R.string.post_validation_drop_percent))
                }
                return
            }
        }
        if (!hasFormChanges(d, f, _baselineTagIds.value)) {
            viewModelScope.launch {
                _events.emit(getApplication<Application>().getString(R.string.edit_listing_no_changes))
            }
            return
        }
        val update = buildDeltaUpdate(d, f, _baselineTagIds.value)
        if (update == null) {
            viewModelScope.launch {
                _events.emit(getApplication<Application>().getString(R.string.edit_listing_save_error))
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
}
