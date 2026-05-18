package com.pc.fash_android_mobile.data.sellerpackages

/**
 * Hardcoded catalog until checkout + admin metadata are live.
 * Shape matches [AppAdProductPackage] + JSON `metadata` from admin portal.
 */
object SellerProductPackageCatalog {

    fun defaultPackages(): List<SellerProductPackage> = listOf(
        starter(),
        growth(),
        premium(),
    )

    private fun starter() = SellerProductPackage(
        id = "00000000-0000-4000-8000-000000000001",
        code = "seller_starter",
        name = "Gói Tiết kiệm",
        description = "Bắt đầu xác minh hàng Real/Fake và làm quen công cụ dành cho người bán.",
        priceVnd = 99_000L,
        durationDays = 30,
        tier = PackageTier.STARTER,
        isReleased = false,
        isBestSeller = false,
        badgeLabel = null,
        active = true,
        features = listOf(
            SellerPackageFeature(
                id = "authenticity_verify",
                included = true,
                highlight = "3 tin / tháng",
            ),
            SellerPackageFeature(id = "explore_boost", included = false),
            SellerPackageFeature(id = "fanpage_spotlight", included = false),
            SellerPackageFeature(id = "social_tiktok_instagram", included = false),
        ),
    )

    private fun growth() = SellerProductPackage(
        id = "00000000-0000-4000-8000-000000000002",
        code = "seller_growth",
        name = "Gói Phổ biến",
        description = "Cân bằng giá và hiển thị — phù hợp shop bán đều trên Fash.",
        priceVnd = 299_000L,
        durationDays = 30,
        tier = PackageTier.GROWTH,
        isReleased = false,
        isBestSeller = true,
        badgeLabel = "Phổ biến",
        active = true,
        features = listOf(
            SellerPackageFeature(
                id = "authenticity_verify",
                included = true,
                highlight = "Không giới hạn tin",
            ),
            SellerPackageFeature(
                id = "explore_boost",
                included = true,
                highlight = "7 ngày ưu tiên",
            ),
            SellerPackageFeature(
                id = "fanpage_spotlight",
                included = true,
                highlight = "1 bài / tháng",
            ),
            SellerPackageFeature(id = "social_tiktok_instagram", included = false),
        ),
    )

    private fun premium() = SellerProductPackage(
        id = "00000000-0000-4000-8000-000000000003",
        code = "seller_premium",
        name = "Gói Đầy đủ",
        description = "Toàn bộ tiện ích: xác minh, đẩy Khám phá, fanpage Fash và quảng bá mạng xã hội.",
        priceVnd = 699_000L,
        durationDays = 30,
        tier = PackageTier.PREMIUM,
        isReleased = false,
        isBestSeller = false,
        badgeLabel = "Đầy đủ",
        active = true,
        features = listOf(
            SellerPackageFeature(
                id = "authenticity_verify",
                included = true,
                highlight = "Ưu tiên xử lý",
            ),
            SellerPackageFeature(
                id = "explore_boost",
                included = true,
                highlight = "30 ngày",
            ),
            SellerPackageFeature(
                id = "fanpage_spotlight",
                included = true,
                highlight = "4 bài / tháng",
            ),
            SellerPackageFeature(
                id = "social_tiktok_instagram",
                included = true,
                highlight = "TikTok + Instagram",
            ),
        ),
    )

    fun findByCode(code: String): SellerProductPackage? =
        defaultPackages().firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
}
