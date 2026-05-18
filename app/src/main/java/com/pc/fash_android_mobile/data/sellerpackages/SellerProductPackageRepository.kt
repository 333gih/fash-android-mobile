package com.pc.fash_android_mobile.data.sellerpackages

import android.util.Log
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.http.CoreServiceErrors
import com.pc.fash_android_mobile.data.http.CoreServiceHttpException
import okhttp3.OkHttpClient
import okhttp3.Request

private const val TAG = "SellerProductPackageRepo"
private const val USER_AGENT = "FashAndroid/1.0"

/**
 * Seller utility packages from core-service CMS, with local catalog fallback.
 */
class SellerProductPackageRepository(
    private val securedClient: OkHttpClient,
    private val localeTagProvider: () -> String = { "vi" },
) {

    private fun throwHttp(httpCode: Int, body: String): Nothing =
        throw CoreServiceHttpException(httpCode, CoreServiceErrors.parseErrorMessage(httpCode, body))

    private fun executeGet(url: String): String {
        val locale = localeTagProvider().trim().ifBlank { "vi" }
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("Accept-Language", locale)
            .header("X-Fash-Lang", locale)
            .header("User-Agent", USER_AGENT)
            .build()
        return securedClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throwHttp(response.code, body)
            body
        }
    }

    fun listPackages(activeOnly: Boolean = true): Result<SellerProductPackagesResponse> = runCatching {
        val qs = if (activeOnly) "active_only=true" else "active_only=false"
        val relative = "api/v1/app/advertising/product-packages?$qs"
        val urls = AppEnvironment.coreApiCandidateUrls(relative)
        var last: Exception? = null
        for (url in urls) {
            try {
                val raw = executeGet(url)
                val parsed = parseSellerProductPackagesResponse(raw)
                val visible = if (activeOnly) parsed.packages.filter { it.active } else parsed.packages
                if (visible.isNotEmpty()) {
                    return@runCatching parsed.copy(packages = visible)
                }
            } catch (e: Exception) {
                last = e
            }
        }
        Log.w(TAG, "listPackages API empty/failed, using local catalog urls=$urls", last)
        localCatalog(activeOnly)
    }

    fun getPackage(code: String): Result<SellerProductPackage> = runCatching {
        val fromList = listPackages(activeOnly = false).getOrNull()
            ?.packages
            ?.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
        fromList ?: SellerProductPackageCatalog.findByCode(code)
            ?: error("package not found: $code")
    }

    private fun localCatalog(activeOnly: Boolean): SellerProductPackagesResponse {
        val all = SellerProductPackageCatalog.defaultPackages()
        val filtered = if (activeOnly) all.filter { it.active } else all
        return SellerProductPackagesResponse(
            packages = filtered.sortedBy { tierOrder(it.tier) },
        )
    }

    private fun tierOrder(tier: PackageTier): Int = when (tier) {
        PackageTier.STARTER -> 0
        PackageTier.GROWTH -> 1
        PackageTier.PREMIUM -> 2
    }
}
