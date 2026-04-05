package com.pc.fash_android_mobile.ui.listing

/**
 * Editable fields for [EditListingScreen] / [EditListingViewModel].
 * Measurements mirror create flow (free-text decimals); country from common-service catalog.
 */
data class EditListingFormState(
    val title: String = "",
    val description: String = "",
    val priceText: String = "",
    val condition: String = "",
    val size: String = "",
    val brandId: String? = null,
    val brandName: String = "",
    /** Selected aesthetic tag ids (max 5). */
    val selectedTagIds: Set<String> = emptySet(),
    val countryId: String? = null,
    val countryIso2: String = "",
    val countryName: String = "",
    val measurementUnit: String = "cm",
    val measurementHem: String = "",
    val measurementChest: String = "",
    val measurementLength: String = "",
    val measurementShoulders: String = "",
    val measurementSleeveLength: String = "",
    val acceptOffers: Boolean = false,
    val autoPriceDropEnabled: Boolean = false,
    val floorPriceText: String = "",
    /** Digits only, max 2 — same semantics as create listing draft. */
    val priceDropPercentInput: String = "10",
)
