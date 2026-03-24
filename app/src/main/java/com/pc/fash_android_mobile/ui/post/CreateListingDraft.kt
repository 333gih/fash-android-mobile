package com.pc.fash_android_mobile.ui.post

import android.net.Uri

/**
 * Draft state for the 3-step create listing flow.
 * Step 1: image Uris (local) → uploaded URLs after upload
 * Step 2: title, price, condition, category, description, size, brand, tags
 * Step 3: submit
 */
data class CreateListingDraft(
    /** Local URIs from image picker (Step 1). First = cover. */
    val imageUris: List<Uri> = emptyList(),
    /** Uploaded image URLs (populated when user proceeds from Step 1 or at submit). */
    val imageUrls: List<String> = emptyList(),
    val title: String = "",
    val priceVnd: Long = 0L,
    val condition: String = "",
    val categoryId: String = "",
    val description: String = "",
    val size: String = "",
    val brand: String = "",
    val aestheticTags: List<String> = emptyList(),
) {
    fun withImageUris(uris: List<Uri>) = copy(imageUris = uris)
    fun withImageUrls(urls: List<String>) = copy(imageUrls = urls)
    fun withTitle(v: String) = copy(title = v)
    fun withPriceVnd(v: Long) = copy(priceVnd = v)
    fun withCondition(v: String) = copy(condition = v)
    fun withCategoryId(v: String) = copy(categoryId = v)
    fun withDescription(v: String) = copy(description = v)
    fun withSize(v: String) = copy(size = v)
    fun withBrand(v: String) = copy(brand = v)
    fun withAestheticTags(v: List<String>) = copy(aestheticTags = v)

    fun removeImageAtIndex(index: Int): CreateListingDraft {
        if (index !in imageUris.indices) return this
        val newUris = imageUris.toMutableList().apply { removeAt(index) }
        return copy(imageUris = newUris)
    }

    fun canProceedFromStep1(): Boolean = imageUris.isNotEmpty()

    fun canProceedFromStep2(): Boolean =
        title.length in 3..60 &&
            priceVnd in 1_000..100_000_000 &&
            condition.isNotBlank() &&
            categoryId.isNotBlank()

    /**
     * Human-readable reasons the Next button stays disabled (size/brand/tags are optional).
     */
    fun step2MissingRequirementKeys(): List<String> {
        val keys = mutableListOf<String>()
        if (title.length !in 3..60) keys.add("title")
        if (priceVnd !in 1_000L..100_000_000L) keys.add("price")
        if (condition.isBlank()) keys.add("condition")
        if (categoryId.isBlank()) keys.add("category")
        return keys
    }
}
