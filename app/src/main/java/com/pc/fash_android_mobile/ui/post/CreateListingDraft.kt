package com.pc.fash_android_mobile.ui.post

import com.pc.fash_android_mobile.BuildConfig
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.common.CommonAestheticTagDto
import com.pc.fash_android_mobile.data.common.CommonBrandDto
import com.pc.fash_android_mobile.data.common.CommonCountryDto
import com.pc.fash_android_mobile.data.common.CategoryTreeNode
import com.pc.fash_android_mobile.data.common.ListingImageStepCatalog
import com.pc.fash_android_mobile.data.listing.CreateListingRequest
import com.pc.fash_android_mobile.data.listing.ListingImageStepPayload
import com.pc.fash_android_mobile.data.listing.NamedRefPayload
import com.pc.fash_android_mobile.data.user.ProfileInfo

/** How the seller chose to fill listing metadata at the start of the flow. */
enum class CreateListingFillMode {
    MANUAL,
    FROM_PROFILE_STYLE,
}

/** Internal step before category selection — not counted in [TotalPostSteps]. */
const val CreateListingModeStep = 0

/**
 * One photo slot in the create-listing wizard (definition from common-service + local/uploaded image).
 */
data class ListingPhotoSlotDraft(
    val stepKey: String,
    val label: String,
    val labelVi: String,
    val sortOrder: Int,
    val required: Boolean,
    /** Picked gallery/camera `Uri` string — cleared after successful upload when only server URL is kept. */
    val localImageUri: String? = null,
    val uploadedImageUrl: String? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
)

fun ListingPhotoSlotDraft.hasImageSelected(): Boolean =
    !localImageUri.isNullOrBlank() || !uploadedImageUrl.isNullOrBlank()

/**
 * In-memory draft for the multi-step create listing flow.
 * [listingPhotoSlots] are filled from common-service `listing-image-setup` for the chosen leaf category.
 */
data class CreateListingDraft(
    val fillMode: CreateListingFillMode = CreateListingFillMode.MANUAL,
    val categoryId: String = "",
    val categoryName: String = "",
    val parentCategoryId: String? = null,
    val parentCategoryName: String? = null,
    val selectedAestheticTagIds: Set<String> = emptySet(),
    val condition: String = "",
    val size: String = "",
    /** Primary product colour in lowercase English (e.g. "black"). Optional. */
    val color: String = "",
    /** Target gender: "women" | "men" | "unisex" | "kids" | "baby". Recommended. */
    val genderTarget: String = "",
    val brandId: String? = null,
    val brandName: String = "",
    val title: String = "",
    val description: String = "",
    val countryId: String? = null,
    val countryIso2: String = "",
    val countryName: String = "",
    val measurementUnit: String = "cm",
    val measurementHem: String = "",
    val measurementChest: String = "",
    val measurementLength: String = "",
    val measurementShoulders: String = "",
    val measurementSleeveLength: String = "",
    val listingPhotoSlots: List<ListingPhotoSlotDraft> = emptyList(),
    /** Leaf category id used to build [listingPhotoSlots] — cleared when category changes. */
    val listingPhotoSlotsCategoryId: String? = null,
    val priceVnd: String = "",
    val acceptOffers: Boolean = true,
    val autoPriceDropEnabled: Boolean = false,
    val floorPriceVnd: String = "",
    /** Digits only, max 2 chars — free typing; use [parsedPriceDropPercent] for API and validation. */
    val priceDropPercentInput: String = "10",
    val shippingAddressId: String? = null,
    val shippingAddressLabel: String = "",
    val onsiteInspectionCommitment: Boolean = false,
    /** 80–99 when set; sent as `condition_score`. */
    val conditionScore: Int = 90,
    val conditionDefects: List<String> = emptyList(),
)

/** Known defect keys for Step 5 checklist (wire values). */
val ListingConditionDefectOptions = listOf(
    "stains",
    "worn",
    "missing_button",
    "fading",
    "pilling",
    "odor",
)

fun CreateListingDraft.toggleConditionDefect(key: String): CreateListingDraft {
    val next = conditionDefects.toMutableList()
    if (next.contains(key)) next.remove(key) else next.add(key)
    return copy(conditionDefects = next)
}

fun CreateListingDraft.withListingPhotoSlotsFromCatalog(
    categoryId: String,
    catalog: List<ListingImageStepCatalog>,
): CreateListingDraft {
    val old = listingPhotoSlots.associateBy { it.stepKey }
    val capped = catalog.sortedBy { it.sortOrder }.take(20)
    val slots = capped.map { c ->
        val o = old[c.stepKey]
        ListingPhotoSlotDraft(
            stepKey = c.stepKey,
            label = c.label,
            labelVi = c.labelVi,
            sortOrder = c.sortOrder,
            required = c.required,
            localImageUri = o?.localImageUri,
            uploadedImageUrl = o?.uploadedImageUrl,
            imageWidth = o?.imageWidth,
            imageHeight = o?.imageHeight,
        )
    }
    return copy(
        listingPhotoSlots = slots,
        listingPhotoSlotsCategoryId = categoryId,
    )
}

private fun parsePositiveLong(s: String): Long? =
    s.trim().replace(".", "").replace(",", "").toLongOrNull()?.takeIf { it > 0 }

private fun parseDoubleOrNull(s: String): Double? =
    s.trim().replace(",", ".").toDoubleOrNull()

/** Parsed 1–50 for `price_drop_percent`, or null if incomplete or invalid. */
fun CreateListingDraft.parsedPriceDropPercent(): Int? {
    val d = priceDropPercentInput.filter { it.isDigit() }.take(2)
    if (d.isEmpty()) return null
    val n = d.toIntOrNull() ?: return null
    return n.takeIf { it in 1..50 }
}

fun CreateListingDraft.toCreateListingRequest(
    imageUrlSteps: List<ListingImageStepPayload>,
    aestheticTagsById: Map<String, CommonAestheticTagDto>,
): CreateListingRequest {
    val leafId = categoryId.trim()
    val leafName = categoryName.trim()
    val aestheticTagRefs = selectedAestheticTagIds.mapNotNull { tid ->
        val dto = aestheticTagsById[tid] ?: return@mapNotNull null
        val n = dto.displayName.trim().ifBlank { dto.name.trim() }.ifBlank { return@mapNotNull null }
        NamedRefPayload(id = tid.trim(), name = n)
    }
    val parentRef = parentCategoryId?.trim()?.takeIf { it.isNotEmpty() }?.let { pid ->
        val pn = parentCategoryName?.trim().orEmpty().ifBlank { return@let null }
        NamedRefPayload(id = pid, name = pn)
    }
    val brandRef = brandId?.trim()?.takeIf { it.isNotEmpty() }?.let { bid ->
        val bn = brandName.trim().ifBlank { return@let null }
        NamedRefPayload(id = bid, name = bn)
    }
    val price = parsePositiveLong(priceVnd) ?: 1_000L
    val floor = floorPriceVnd.trim().let { if (it.isEmpty()) null else parsePositiveLong(it) }
    return CreateListingRequest(
        title = title.trim(),
        imageUrlSteps = imageUrlSteps,
        priceVnd = price,
        condition = condition.trim(),
        category = NamedRefPayload(id = leafId, name = leafName),
        description = description.trim(),
        size = size.trim(),
        color = color.trim().lowercase().ifBlank { null },
        genderTarget = genderTarget.trim().lowercase().ifBlank { null },
        parentCategory = parentRef,
        brand = brandRef,
        aestheticTags = aestheticTagRefs,
        countryOfOrigin = countryIso2.takeIf { it.length == 2 },
        countryId = countryId?.takeIf { !it.isNullOrBlank() },
        countryName = countryName.takeIf { it.isNotBlank() },
        measurementUnit = measurementUnit.takeIf { it.isNotBlank() },
        measurementHem = parseDoubleOrNull(measurementHem),
        measurementChest = parseDoubleOrNull(measurementChest),
        measurementLength = parseDoubleOrNull(measurementLength),
        measurementShoulders = parseDoubleOrNull(measurementShoulders),
        measurementSleeveLength = parseDoubleOrNull(measurementSleeveLength),
        acceptOffers = acceptOffers,
        autoPriceDropEnabled = autoPriceDropEnabled,
        floorPriceVnd = if (autoPriceDropEnabled) floor else null,
        priceDropPercent = if (autoPriceDropEnabled) parsedPriceDropPercent() else null,
        shippingAddressId = shippingAddressId?.takeIf { !it.isNullOrBlank() },
        onsiteInspectionCommitment = onsiteInspectionCommitment,
        conditionScore = conditionScore.coerceIn(80, 99),
        conditionDefects = conditionDefects,
    )
}

/** Max aesthetic tags per API. */
const val MaxAestheticTags = 5

const val MinListingTitleLength = 3
const val MaxListingTitleLength = 60
const val MaxListingDescriptionLength = 500
const val MinPriceVnd = 1_000L
const val MaxPriceVnd = 100_000_000L

const val TotalPostSteps = 10

/** From env `POST_REQUIRE_LISTING_IMAGES` (default true): at least one photo required for listing. */
fun postRequireListingImages(): Boolean = BuildConfig.POST_REQUIRE_LISTING_IMAGES

/** @return string resource name for [com.pc.fash_android_mobile.R.string], or null if valid. */
fun CreateListingDraft.validationErrorKeyForSubmit(): String? {
    if (categoryId.isBlank() || categoryName.isBlank()) return "post_validation_category"
    if (title.trim().length < MinListingTitleLength) return "post_validation_title_short"
    if (title.trim().length > MaxListingTitleLength) return "post_validation_title_long"
    if (description.length > MaxListingDescriptionLength) return "post_validation_description_long"
    if (condition.isBlank()) return "post_validation_condition"
    if (postRequireListingImages()) {
        val missing = listingPhotoSlots.isEmpty() ||
            listingPhotoSlots.any { it.required && !it.hasImageSelected() }
        if (missing) return "post_validation_photos"
    }
    val p = parsePositiveLong(priceVnd) ?: return "post_validation_price"
    if (p < MinPriceVnd || p > MaxPriceVnd) return "post_validation_price_range"
    if (selectedAestheticTagIds.size > MaxAestheticTags) return "post_validation_tags_max"
    if (!onsiteInspectionCommitment) return "post_validation_commitment"
    if (autoPriceDropEnabled) {
        val floor = parsePositiveLong(floorPriceVnd)
        if (floor == null || floor < MinPriceVnd || floor > MaxPriceVnd) return "post_validation_floor"
        if (floor >= p) return "post_validation_floor_below_price"
        if (parsedPriceDropPercent() == null) return "post_validation_drop_percent"
    }
    return null
}

fun CategoryTreeNode.findLeaf(id: String): CategoryTreeNode? {
    if (this.id == id) {
        return if (children.isEmpty()) this else null
    }
    children.forEach { ch ->
        val found = ch.findLeaf(id)
        if (found != null) return found
    }
    return null
}

fun CategoryTreeNode.findNode(id: String): CategoryTreeNode? {
    if (this.id == id) return this
    children.forEach { ch ->
        val found = ch.findNode(id)
        if (found != null) return found
    }
    return null
}

fun CategoryTreeNode.findParentOf(childId: String): CategoryTreeNode? {
    children.forEach { ch ->
        if (ch.id == childId) return this
        val inner = ch.findParentOf(childId)
        if (inner != null) return inner
    }
    return null
}

fun List<CategoryTreeNode>.findParentOfLeaf(leafId: String): CategoryTreeNode? {
    for (root in this) {
        val p = root.findParentOf(leafId)
        if (p != null) return p
    }
    return null
}

fun CreateListingDraft.withLeafCategory(
    treeRoots: List<CategoryTreeNode>,
    leaf: CategoryTreeNode,
): CreateListingDraft {
    val parent = treeRoots.findParentOfLeaf(leaf.id)
    return copy(
        categoryId = leaf.id,
        categoryName = leaf.name,
        parentCategoryId = parent?.id,
        parentCategoryName = parent?.name,
        listingPhotoSlots = emptyList(),
        listingPhotoSlotsCategoryId = null,
    )
}

fun CreateListingDraft.toggleAestheticTag(id: String): CreateListingDraft {
    val next = selectedAestheticTagIds.toMutableSet()
    when {
        next.contains(id) -> next.remove(id)
        next.size < MaxAestheticTags -> next.add(id)
    }
    return copy(selectedAestheticTagIds = next)
}

fun CreateListingDraft.canProceedFromStep(step: Int): Boolean = when (step) {
    1 -> categoryId.isNotBlank()
    2 -> selectedAestheticTagIds.size <= MaxAestheticTags
    3 -> true
    4 -> true
    5 -> condition.isNotBlank() &&
        title.trim().length in MinListingTitleLength..MaxListingTitleLength &&
        description.length <= MaxListingDescriptionLength
    6 -> true
    7 -> !postRequireListingImages() || (
        listingPhotoSlots.isNotEmpty() &&
            listingPhotoSlots.all { !it.required || it.hasImageSelected() }
        )
    8 -> {
        val p = parsePositiveLong(priceVnd)
        p != null && p in MinPriceVnd..MaxPriceVnd &&
            if (autoPriceDropEnabled) {
                val f = parsePositiveLong(floorPriceVnd)
                val pct = parsedPriceDropPercent()
                f != null && f in MinPriceVnd..MaxPriceVnd && f < p && pct != null && pct in 1..50
            } else {
                true
            }
    }
    9 -> onsiteInspectionCommitment
    10 -> true
    else -> false
}

/**
 * When [canProceedFromStep] is false, returns a string resource explaining why **Next** is disabled.
 */
fun CreateListingDraft.nextStepBlockedReasonRes(step: Int): Int? {
    if (canProceedFromStep(step)) return null
    return when (step) {
        1 -> when {
            categoryId.isBlank() -> R.string.post_next_blocked_category
            else -> R.string.post_next_blocked_generic
        }
        2 -> when {
            selectedAestheticTagIds.size > MaxAestheticTags -> R.string.post_next_blocked_tags
            else -> R.string.post_next_blocked_generic
        }
        5 -> when {
            condition.isBlank() -> R.string.post_next_blocked_condition
            title.trim().length < MinListingTitleLength -> R.string.post_next_blocked_title_short
            title.trim().length > MaxListingTitleLength -> R.string.post_next_blocked_title_long
            description.length > MaxListingDescriptionLength -> R.string.post_next_blocked_description_long
            else -> R.string.post_next_blocked_generic
        }
        7 -> R.string.post_next_blocked_photos
        8 -> step8NextBlockedReason()
        9 -> when {
            !onsiteInspectionCommitment -> R.string.post_next_blocked_commitment
            else -> R.string.post_next_blocked_generic
        }
        else -> R.string.post_next_blocked_generic
    }
}

private fun CreateListingDraft.step8NextBlockedReason(): Int {
    val p = parsePositiveLong(priceVnd)
    if (p == null) return R.string.post_next_blocked_price
    if (p !in MinPriceVnd..MaxPriceVnd) return R.string.post_next_blocked_price_range
    if (!autoPriceDropEnabled) return R.string.post_next_blocked_generic
    val f = parsePositiveLong(floorPriceVnd)
    if (f == null || f !in MinPriceVnd..MaxPriceVnd) return R.string.post_next_blocked_floor
    if (f >= p) return R.string.post_next_blocked_floor_below
    val pct = parsedPriceDropPercent()
    if (pct == null || pct !in 1..50) return R.string.post_next_blocked_drop_percent
    return R.string.post_next_blocked_generic
}

fun CommonBrandDto.matchesQuery(q: String): Boolean {
    if (q.isBlank()) return true
    val n = q.trim().lowercase()
    return name.lowercase().contains(n)
}

fun CommonCountryDto.matchesQuery(q: String): Boolean {
    if (q.isBlank()) return true
    val n = q.trim().lowercase()
    return name.lowercase().contains(n) || iso2.lowercase().contains(n)
}

fun CommonAestheticTagDto.matchesTagQuery(q: String): Boolean {
    if (q.isBlank()) return true
    val n = q.trim().lowercase()
    return name.lowercase().contains(n) || displayName.lowercase().contains(n) || displayNameVi.lowercase().contains(n)
}

/** Builds core-service `image_urls` JSON array after uploads filled [ListingPhotoSlotDraft.uploadedImageUrl]. */
fun CreateListingDraft.buildListingImageStepPayloads(): List<ListingImageStepPayload> =
    listingPhotoSlots
        .sortedBy { it.sortOrder }
        .mapNotNull { s ->
            val url = s.uploadedImageUrl?.trim().orEmpty()
            if (url.isEmpty()) return@mapNotNull null
            ListingImageStepPayload(
                stepKey = s.stepKey.trim(),
                label = s.label.trim().ifBlank { s.stepKey },
                labelVi = s.labelVi.trim().takeIf { it.isNotEmpty() },
                sortOrder = s.sortOrder,
                required = s.required,
                imageUrl = url,
                width = s.imageWidth,
                height = s.imageHeight,
            )
        }

fun ProfileInfo.hasStyleReferenceForListing(): Boolean =
    aestheticTagSnapshots.isNotEmpty() ||
        !referenceSize.isNullOrBlank() ||
        referenceMeasurementChest != null ||
        referenceMeasurementHem != null ||
        referenceMeasurementLength != null ||
        referenceMeasurementShoulders != null ||
        referenceMeasurementSleeveLength != null ||
        gender.trim().isNotEmpty()

fun mapProfileGenderToListingTarget(gender: String): String? = when (gender.trim().lowercase()) {
    "women" -> "women"
    "men" -> "men"
    "non_binary" -> "unisex"
    else -> null
}

/** Gender key for sizing charts in listing step 6 (women/men charts from onboarding). */
fun CreateListingDraft.sizingChartGender(profile: ProfileInfo? = null): String {
    genderTarget.trim().lowercase().takeIf { it == "women" || it == "men" }?.let { return it }
    mapProfileGenderToListingTarget(profile?.gender.orEmpty())?.takeIf { it == "women" || it == "men" }?.let { return it }
    return "women"
}

private fun formatMeasurementDraftValue(value: Double?): String {
    if (value == null) return ""
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        value.toString()
    }
}

/**
 * Pre-fills style/size fields from the signed-in user's profile.
 * Only writes into empty draft fields (brand, category, photos, etc. stay manual).
 */
fun CreateListingDraft.applyProfileStyleIfEmpty(profile: ProfileInfo): CreateListingDraft {
    var next = copy(fillMode = CreateListingFillMode.FROM_PROFILE_STYLE)
    if (selectedAestheticTagIds.isEmpty() && profile.aestheticTagSnapshots.isNotEmpty()) {
        next = next.copy(
            selectedAestheticTagIds = profile.aestheticTagSnapshots
                .map { it.id }
                .filter { it.isNotBlank() }
                .take(MaxAestheticTags)
                .toSet(),
        )
    }
    if (size.isBlank() && !profile.referenceSize.isNullOrBlank()) {
        next = next.copy(size = profile.referenceSize!!.trim().take(20))
    }
    val profileUnit = profile.referenceMeasurementUnit?.trim().orEmpty()
    if (profileUnit.isNotBlank()) {
        val normalized = if (profileUnit.equals("in", ignoreCase = true)) "in" else "cm"
        if (measurementUnit.isBlank() || measurementUnit == "cm") {
            next = next.copy(measurementUnit = normalized)
        }
    }
    if (measurementHem.isBlank()) {
        formatMeasurementDraftValue(profile.referenceMeasurementHem).takeIf { it.isNotBlank() }?.let {
            next = next.copy(measurementHem = it)
        }
    }
    if (measurementChest.isBlank()) {
        formatMeasurementDraftValue(profile.referenceMeasurementChest).takeIf { it.isNotBlank() }?.let {
            next = next.copy(measurementChest = it)
        }
    }
    if (measurementLength.isBlank()) {
        formatMeasurementDraftValue(profile.referenceMeasurementLength).takeIf { it.isNotBlank() }?.let {
            next = next.copy(measurementLength = it)
        }
    }
    if (measurementShoulders.isBlank()) {
        formatMeasurementDraftValue(profile.referenceMeasurementShoulders).takeIf { it.isNotBlank() }?.let {
            next = next.copy(measurementShoulders = it)
        }
    }
    if (measurementSleeveLength.isBlank()) {
        formatMeasurementDraftValue(profile.referenceMeasurementSleeveLength).takeIf { it.isNotBlank() }?.let {
            next = next.copy(measurementSleeveLength = it)
        }
    }
    if (genderTarget.isBlank()) {
        mapProfileGenderToListingTarget(profile.gender)?.let { target ->
            next = next.copy(genderTarget = target)
        }
    }
    return next
}
