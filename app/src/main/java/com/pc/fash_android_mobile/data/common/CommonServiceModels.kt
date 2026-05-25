package com.pc.fash_android_mobile.data.common

/**
 * DTOs for common-service JSON (see project ANDROID_API_INTEGRATION.md) — GET responses only.
 * Field names match typical Go JSON (`snake_case`); parsers accept common variants.
 */

data class AddressTreeNode(
    val id: String,
    val name: String,
    val code: String,
    val parentId: String?,
    val level: Int,
    val status: String,
    val effectiveFrom: String?,
    val effectiveTo: String?,
    val children: List<AddressTreeNode>,
)

data class CommonAddressDto(
    val id: String,
    val name: String,
    val code: String,
    val parentId: String?,
    val level: Int,
    val status: String,
    val effectiveFrom: String?,
    val effectiveTo: String?,
)

data class AddressHistoryPage(
    val items: List<CommonAddressDto>,
    val offset: Int,
    val limit: Int,
)

data class CommonBrandDto(
    val id: String,
    val name: String,
    val slug: String,
    val country: String,
    val logoUrl: String,
    val status: String,
    val createdAt: String?,
    val updatedAt: String?,
)

data class BrandsPage(
    val items: List<CommonBrandDto>,
    val total: Long,
    val offset: Int,
    val limit: Int,
    val hasMore: Boolean,
)

data class CategoryTreeNode(
    val id: String,
    val name: String,
    val slug: String,
    val parentId: String?,
    val sortOrder: Int,
    val status: String,
    val createdAt: String?,
    val updatedAt: String?,
    val children: List<CategoryTreeNode>,
)

data class CommonCategoryDto(
    val id: String,
    val name: String,
    val slug: String,
    val parentId: String?,
    val sortOrder: Int,
    val status: String,
    val createdAt: String?,
    val updatedAt: String?,
)

data class CategoriesPage(
    val items: List<CommonCategoryDto>,
    val total: Long,
    val offset: Int,
    val limit: Int,
    val hasMore: Boolean,
)

data class CommonAestheticTagDto(
    val id: String,
    val name: String,
    val displayName: String,
    val displayNameVi: String = "",
    val sortOrder: Int,
    val status: String,
    val createdAt: String?,
    val updatedAt: String?,
)

data class AestheticTagsPage(
    val items: List<CommonAestheticTagDto>,
    val total: Long,
    val offset: Int,
    val limit: Int,
    val hasMore: Boolean,
)

data class CommonCountryDto(
    val id: String,
    val iso2: String,
    val iso3: String,
    val name: String,
    val numericCode: Int?,
    val phonePrefix: String,
    val emoji: String,
    val sortOrder: Int,
    val status: String,
    val createdAt: String?,
    val updatedAt: String?,
)

data class CountriesPage(
    val items: List<CommonCountryDto>,
    val total: Long,
    val offset: Int,
    val limit: Int,
    val hasMore: Boolean,
)

/** One catalog step from `GET .../categories/{id}/listing-image-setup` (no [image_url] until upload). */
data class ListingImageStepCatalog(
    val stepKey: String,
    val label: String,
    val labelVi: String,
    val sortOrder: Int,
    val required: Boolean,
)

/** Normalized listing-image-setup for the create-listing photo wizard. */
data class ListingImageSetupDto(
    val categoryId: String,
    val steps: List<ListingImageStepCatalog>,
)

/** Public catalog row from `GET .../public/safe-meetup-zones`. */
data class SafeMeetupZoneDto(
    val id: String,
    val name: String,
    val nameVi: String,
    val zoneType: String,
    val provinceId: String,
    val districtId: String?,
    val addressLine: String,
    val locationUrl: String,
    val sortOrder: Int,
) {
    /** Locale-aware label: [nameVi] when Vietnamese UI, else [name]. */
    fun displayLabel(preferVi: Boolean): String {
        val vi = nameVi.trim()
        val en = name.trim()
        return if (preferVi && vi.isNotEmpty()) vi else en.ifBlank { vi.ifBlank { name } }
    }
}

/** Gen Z review badge from `GET .../public/review-badges?all=true`. */
data class ReviewBadgeDto(
    val id: String,
    val slug: String,
    val nameEn: String,
    val nameVi: String,
    val emoji: String,
    val sortOrder: Int,
) {
    fun displayName(isVi: Boolean): String {
        val vi = nameVi.trim()
        val en = nameEn.trim()
        return if (isVi && vi.isNotEmpty()) vi else en.ifBlank { vi.ifBlank { slug } }
    }
}

/** Default steps when common-service has no template (aligned with core DB backfill). */
fun defaultListingImageCatalogSteps(): List<ListingImageStepCatalog> = listOf(
    ListingImageStepCatalog(
        stepKey = "front",
        label = "Front view",
        labelVi = "Mặt trước",
        sortOrder = 0,
        required = true,
    ),
    ListingImageStepCatalog(
        stepKey = "step_2",
        label = "Back side",
        labelVi = "Mặt sau",
        sortOrder = 1,
        required = true,
    ),
)
