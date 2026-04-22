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
)

fun ListingPhotoSlotDraft.hasImageSelected(): Boolean =
    !localImageUri.isNullOrBlank() || !uploadedImageUrl.isNullOrBlank()

/**
 * In-memory draft for the multi-step create listing flow.
 * [listingPhotoSlots] are filled from common-service `listing-image-setup` for the chosen leaf category.
 */
data class CreateListingDraft(
    val categoryId: String = "",
    val categoryName: String = "",
    val parentCategoryId: String? = null,
    val parentCategoryName: String? = null,
    val selectedAestheticTagIds: Set<String> = emptySet(),
    val condition: String = "",
    val size: String = "",
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
)

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
    val tagIds = selectedAestheticTagIds.toList()
    val tagNamesFromIds = tagIds.mapNotNull { id ->
        aestheticTagsById[id]?.name?.trim()?.takeIf { it.isNotEmpty() }
    }
    val price = parsePositiveLong(priceVnd) ?: 1_000L
    val floor = floorPriceVnd.trim().let { if (it.isEmpty()) null else parsePositiveLong(it) }
    return CreateListingRequest(
        title = title.trim(),
        imageUrlSteps = imageUrlSteps,
        priceVnd = price,
        condition = condition.trim(),
        categoryId = categoryId.trim(),
        description = description.trim(),
        size = size.trim(),
        parentCategoryId = parentCategoryId?.takeIf { !it.isNullOrBlank() },
        parentCategoryName = parentCategoryName?.takeIf { !it.isNullOrBlank() },
        categoryName = categoryName.takeIf { it.isNotBlank() },
        brandId = brandId?.takeIf { !it.isNullOrBlank() },
        brandName = brandName.takeIf { it.isNotBlank() },
        aestheticTagIds = tagIds,
        aestheticTagNames = if (tagIds.isEmpty()) tagNamesFromIds else emptyList(),
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
    if (categoryId.isBlank()) return "post_validation_category"
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
    9 -> true
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
    return name.lowercase().contains(n) || displayName.lowercase().contains(n)
}

/** Builds core-service `image_urls` JSON array after uploads filled [ListingPhotoSlotDraft.uploadedImageUrl]. */
fun CreateListingDraft.buildListingImageStepPayloads(): List<ListingImageStepPayload> =
    listingPhotoSlots.sortedBy { it.sortOrder }.map { s ->
        ListingImageStepPayload(
            stepKey = s.stepKey.trim(),
            label = s.label.trim().ifBlank { s.stepKey },
            labelVi = s.labelVi.trim().takeIf { it.isNotEmpty() },
            sortOrder = s.sortOrder,
            required = s.required,
            imageUrl = s.uploadedImageUrl?.trim().orEmpty(),
        )
    }
