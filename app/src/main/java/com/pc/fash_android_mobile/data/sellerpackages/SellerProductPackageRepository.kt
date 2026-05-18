package com.pc.fash_android_mobile.data.sellerpackages

/**
 * Product packages for sellers. Today: local catalog; later: core-service GET.
 */
class SellerProductPackageRepository {

    fun listPackages(activeOnly: Boolean = true): Result<SellerProductPackagesResponse> = runCatching {
        val all = SellerProductPackageCatalog.defaultPackages()
        val filtered = if (activeOnly) all.filter { it.active } else all
        SellerProductPackagesResponse(
            packages = filtered.sortedBy { tierOrder(it.tier) },
        )
    }

    fun getPackage(code: String): Result<SellerProductPackage> = runCatching {
        SellerProductPackageCatalog.findByCode(code)
            ?: error("package not found: $code")
    }

    private fun tierOrder(tier: PackageTier): Int = when (tier) {
        PackageTier.STARTER -> 0
        PackageTier.GROWTH -> 1
        PackageTier.PREMIUM -> 2
    }
}
