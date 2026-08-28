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
 * Seller utility packages from core-service CMS (single source of truth).
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
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    visible.forEach { p ->
                        Log.d(TAG, "package code=${p.code} isReleased=${p.isReleased} active=${p.active}")
                    }
                }
                return@runCatching parsed.copy(packages = visible)
            } catch (e: Exception) {
                last = e
            }
        }
        throw last ?: IllegalStateException("product-packages unavailable")
    }

    fun getPackage(code: String): Result<SellerProductPackage> = runCatching {
        val trimmed = code.trim()
        require(trimmed.isNotEmpty())
        listPackages(activeOnly = false).getOrThrow()
            .packages
            .firstOrNull { it.code.equals(trimmed, ignoreCase = true) }
            ?: error("package not found: $trimmed")
    }
}
