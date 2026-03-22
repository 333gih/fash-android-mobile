package com.pc.fash_android_mobile.data.listing

/**
 * Parsed feed item from GET /api/v1/listings/home or /explore.
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

/** Full listing detail for product detail screen. */
data class ListingDetail(
    val id: String,
    val title: String,
    val description: String,
    val imageUrls: List<String>,
    val priceVnd: Long,
    val condition: String,
    val category: String?,
    val size: String?,
    val brand: String?,
    val material: String?,
    val tags: List<String>,
    val likeCount: Int,
    val saveCount: Int,
    val sellerId: String?,
    val sellerUsername: String?,
    val sellerAvatarUrl: String?,
    val sellerDisplayName: String?,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
)
