package com.pc.fash_android_mobile.data.listing

/**
 * Parsed feed item from GET /api/v1/listings/home or /search/listings.
 * Backend may include seller when preloaded; otherwise seller fields are null.
 */
data class ListingFeedItem(
    val id: String,
    val title: String,
    val coverImageUrl: String,
    val imageUrls: List<String>,
    val priceVnd: Long,
    val condition: String,
    val likeCount: Int,
    val saveCount: Int,
    val sellerId: String?,
    val sellerUsername: String?,
    val sellerAvatarUrl: String?,
    val sellerStyleTag: String?,
    val createdAt: String?,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
)

/** Ship-from address on listing detail (wire: `shipping_address`). */
data class ListingShippingAddress(
    val label: String?,
    val line1: String,
    val line2: String?,
    val city: String?,
    val region: String?,
    val postalCode: String?,
    val countryCode: String?,
)

/** Full listing detail for product detail screen. */
data class ListingDetail(
    val id: String,
    val title: String,
    val description: String,
    val imageUrls: List<String>,
    val priceVnd: Long,
    /** Optional compare-at / original price when API sends it. */
    val listPriceVnd: Long? = null,
    val condition: String,
    val category: String?,
    /** Parent category name when API returns `parent_category`. */
    val parentCategoryName: String? = null,
    val size: String?,
    val brand: String?,
    val material: String?,
    val tags: List<String>,
    val likeCount: Int,
    val saveCount: Int,
    val viewCount: Int = 0,
    val measurementUnit: String? = null,
    val measurementHem: Double? = null,
    val measurementChest: Double? = null,
    val measurementLength: Double? = null,
    val measurementShoulders: Double? = null,
    val measurementSleeveLength: Double? = null,
    /** From `aesthetic_tags[]` — prefer [display_name] on wire. */
    val aestheticTags: List<String> = emptyList(),
    val acceptOffers: Boolean = false,
    val autoPriceDropEnabled: Boolean = false,
    val floorPriceVnd: Long? = null,
    val priceDropPercent: Int? = null,
    /** Raw ISO-8601 from API (`next_price_drop_at`). */
    val nextPriceDropAtIso: String? = null,
    val countryName: String? = null,
    val countryIso2: String? = null,
    val shippingAddress: ListingShippingAddress? = null,
    /** Optional estimated shipping in VND (`estimated_shipping_fee`, `shipping_fee`, …). */
    val estimatedShippingVnd: Long? = null,
    val sellerId: String?,
    val sellerUsername: String?,
    val sellerAvatarUrl: String?,
    val sellerDisplayName: String?,
    val sellerVerified: Boolean = false,
    /** Denormalized from `seller` on listing response. */
    val sellerListingCount: Int? = null,
    val sellerFollowerCount: Int? = null,
    val sellerFollowingCount: Int? = null,
    val sellerAverageRating: Float? = null,
    val createdAtIso: String? = null,
    val updatedAtIso: String? = null,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    /** `active` | `reserved` | `sold` — from listing API / realtime. */
    val status: String = "active",
)
