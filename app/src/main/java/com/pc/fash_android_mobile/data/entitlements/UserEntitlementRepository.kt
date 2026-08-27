package com.pc.fash_android_mobile.data.entitlements

import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.http.CoreServiceErrors
import com.pc.fash_android_mobile.data.http.CoreServiceHttpException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

class UserEntitlementRepository(
    private val securedClient: OkHttpClient,
) {
    @Volatile
    private var cached: UserEntitlementSummary? = null

    fun peekCached(): UserEntitlementSummary? = cached

    fun clearCache() {
        cached = null
    }

    fun fetchEntitlements(): Result<UserEntitlementSummary> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/users/me/entitlements")
        val raw = executeGet(url)
        parseSummary(raw).also { cached = it }
    }

    fun mockPurchasePackage(packageId: String): Result<PackageActivationResult> = runCatching {
        val id = packageId.trim()
        require(id.isNotEmpty())
        val url = AppEnvironment.apiPath("api/v1/app/advertising/product-packages/$id/mock-purchase")
        val raw = executePost(url, "{}")
        parseActivation(raw).also { result ->
            result.entitlements?.let { cached = it }
        }
    }

    fun applyExploreBoost(listingId: String): Result<Unit> = runCatching {
        val id = listingId.trim()
        require(id.isNotEmpty())
        val url = AppEnvironment.apiPath("api/v1/listings/$id/explore-boost")
        executePost(url, "{}")
    }

    fun requestAuthenticity(listingId: String): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/seller/authenticity-requests")
        executePost(url, JSONObject().put("listing_id", listingId.trim()).toString())
    }

    fun requestFanpage(listingId: String, caption: String): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/seller/fanpage-requests")
        executePost(
            url,
            JSONObject()
                .put("listing_id", listingId.trim())
                .put("caption", caption.trim())
                .toString(),
        )
    }

    fun requestSocialPromo(listingId: String, caption: String): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/seller/social-promo-requests")
        executePost(
            url,
            JSONObject()
                .put("listing_id", listingId.trim())
                .put("caption", caption.trim())
                .toString(),
        )
    }

    private fun executeGet(url: String): String {
        val response = securedClient.newCall(
            Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .build(),
        ).execute()
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw CoreServiceHttpException(response.code, CoreServiceErrors.parseErrorMessage(response.code, body))
        }
        return body
    }

    private fun executePost(url: String, json: String): String {
        val response = securedClient.newCall(
            Request.Builder()
                .url(url)
                .post(json.toRequestBody(JSON_MEDIA))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build(),
        ).execute()
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw CoreServiceHttpException(response.code, CoreServiceErrors.parseErrorMessage(response.code, body))
        }
        return body
    }

    private fun parseSummary(raw: String): UserEntitlementSummary {
        val root = JSONObject(raw)
        val data = root.optJSONObject("data") ?: root
        val featuresObj = data.optJSONObject("features") ?: JSONObject()
        val features = mutableMapOf<String, FeatureUsageSummary>()
        featuresObj.keys().forEach { key ->
            val f = featuresObj.optJSONObject(key) ?: return@forEach
            features[key] = FeatureUsageSummary(
                enabled = f.optBoolean("enabled", false),
                used = f.optLong("used", 0),
                remaining = if (f.has("remaining") && !f.isNull("remaining")) f.optLong("remaining") else null,
                unlimited = f.optBoolean("unlimited", false),
                durationDays = f.optInt("duration_days", 0),
                priority = f.optBoolean("priority", false),
            )
        }
        return UserEntitlementSummary(
            packageId = data.optString("package_id", ""),
            packageCode = data.optString("package_code", ""),
            packageName = data.optString("package_name", ""),
            features = features,
        )
    }

    private fun parseActivation(raw: String): PackageActivationResult {
        val root = JSONObject(raw)
        val data = root.optJSONObject("data") ?: root
        val ent = data.optJSONObject("entitlements")?.let { parseSummary(it.toString()) }
        return PackageActivationResult(
            packageCode = data.optString("package_code", ent?.packageCode.orEmpty()),
            packageName = data.optString("package_name", ent?.packageName.orEmpty()),
            entitlements = ent,
        )
    }
}
