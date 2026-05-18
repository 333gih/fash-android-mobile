package com.pc.fash_android_mobile.data.sellerpackages

/**
 * Mirrors future `GET /api/v1/app/advertising/product-packages` (core-service + admin CMS).
 */
data class SellerProductPackagesResponse(
    val packages: List<SellerProductPackage>,
    val serverNowUtc: String? = null,
)

enum class PackageTier {
    STARTER,
    GROWTH,
    PREMIUM,
}

data class SellerProductPackage(
    val id: String,
    val code: String,
    val name: String,
    val description: String,
    val priceVnd: Long,
    val durationDays: Int,
    val tier: PackageTier,
    /** When true, checkout allows payment; when false, checkout shows "Coming soon". */
    val isReleased: Boolean,
    val isBestSeller: Boolean,
    val badgeLabel: String?,
    /** When false, package must not appear on the list (server filters with active_only=true). */
    val active: Boolean,
    val features: List<SellerPackageFeature>,
)

/** [id] maps to string resources on the UI layer. */
data class SellerPackageFeature(
    val id: String,
    val included: Boolean,
    val highlight: String? = null,
)
