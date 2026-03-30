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
