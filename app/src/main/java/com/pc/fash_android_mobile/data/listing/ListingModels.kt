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
    /** When the API omits these, UI falls back to title + condition only. */
    val brand: String? = null,
    val size: String? = null,
    /** Listing category (e.g. `category.name`). Shown next to condition when present. */
    val categoryName: String? = null,
    /** First `aesthetic_tags[]` on the listing (not seller). */
    val listingAestheticTag: String? = null,
    val condition: String,
    val likeCount: Int,
    val saveCount: Int,
    val sellerId: String?,
    val sellerUsername: String?,
    val sellerAvatarUrl: String?,
    val sellerStyleTag: String?,
    val createdAt: String?,
    /** Viewer-specific: `listing_likes` row exists (GET home/search/seller listings when authed). */
    val isLiked: Boolean = false,
    /** Viewer-specific: wishlist row exists. */
    val isSaved: Boolean = false,
    /** From `seller.is_following` — viewer follows this listing's seller (batched on listing pages). */
    val sellerIsFollowing: Boolean = false,
    /** Marketplace lifecycle: `in_review`, `rejected`, `active`, `inactive`, `sold`, `reserved`, `deleted` (when API sends `status`). */
    val listingStatus: String? = null,
    val onsiteInspectionCommitment: Boolean = false,
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

/** `aesthetic_tags[]` entry with optional id for Explore filters. */
data class AestheticTagRef(
    val id: String?,
    val label: String,
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
    /** Leaf category id when API returns `category.id` (Explore filter). */
    val categoryId: String? = null,
    /** Parent category name when API returns `parent_category`. */
    val parentCategoryName: String? = null,
    /** Parent category id when API returns `parent_category.id`. */
    val parentCategoryId: String? = null,
    val size: String?,
    val brand: String?,
    /** Brand id when API returns `brand.id`. */
    val brandId: String? = null,
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
    /** Parsed `aesthetic_tags[]` with ids for PDP → Explore. */
    val aestheticTagRefs: List<AestheticTagRef> = emptyList(),
    val acceptOffers: Boolean = false,
    val autoPriceDropEnabled: Boolean = false,
    val floorPriceVnd: Long? = null,
    val priceDropPercent: Int? = null,
    /** Raw ISO-8601 from API (`next_price_drop_at`). */
    val nextPriceDropAtIso: String? = null,
    val countryName: String? = null,
    /** Catalog UUID from nested `country.id` or root `country_id`. */
    val countryId: String? = null,
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
    /** Viewer-specific: `listing_likes` (optional JWT on GET /listings/:id). */
    val isLiked: Boolean = false,
    /** Viewer-specific: wishlist. */
    val isSaved: Boolean = false,
    /** From nested `seller` on listing response — same meaning as profile `is_following`. */
    val sellerIsFollowing: Boolean? = null,
    /** Marketplace status wire (`active`, `in_review`, `rejected`, `inactive`, `sold`, `reserved`, `deleted`, …). */
    val status: String = "active",
    /** Primary colour keyword for the item (e.g. "black", "white", …). */
    val color: String? = null,
    /** Intended wearer: women | men | unisex | kids | baby. */
    val genderTarget: String? = null,
)
