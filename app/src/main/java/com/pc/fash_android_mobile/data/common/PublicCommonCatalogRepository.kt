package com.pc.fash_android_mobile.data.common

import android.util.Log
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.http.CoreServiceErrors
import com.pc.fash_android_mobile.data.http.CoreServiceHttpException
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "PublicCommonCatalog"
private const val USER_AGENT = "FashAndroid/1.0"

/**
 * Public common-service catalog for guest Explore filters — same call pattern as
 * [com.pc.fash_android_mobile.data.editorial.EditorialGuideRepository]:
 * `GET https://api-common.<domain>/api/v1/public/...` with no Bearer.
 */
class PublicCommonCatalogRepository(
    private val localeTagProvider: () -> String,
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun localeSegment(): String {
        val tag = localeTagProvider().trim().lowercase()
        return if (tag.startsWith("en")) "en" else "vi"
    }

    private fun throwHttp(code: Int, body: String): Nothing =
        throw CoreServiceHttpException(code, CoreServiceErrors.parseErrorMessage(code, body))

    private fun publicPath(pathAfterPublic: String): String {
        val rel = pathAfterPublic.trim().trimStart('/')
        return AppEnvironment.commonServicePath("api/v1/public/$rel")
    }

    private fun executeGet(url: String): String {
        val locale = localeSegment()
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("Accept-Language", locale)
            .header("X-Fash-Lang", locale)
            .header("User-Agent", USER_AGENT)
            .build()
        return client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.w(TAG, "GET $url failed code=${response.code} body=${body.take(500)}")
                throwHttp(response.code, body)
            }
            body
        }
    }

    fun getCategoryTree(): Result<List<CategoryTreeNode>> = runCatching {
        val url = publicPath("categories/tree")
        Log.d(TAG, "getCategoryTree GET $url")
        val root = JSONObject(executeGet(url).trim())
        val arr = root.optJSONArray("categories") ?: JSONArray()
        (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { parseCategoryTreeNode(it) }
        }
    }

    fun getBrands(q: String? = null, offset: Int = 0, limit: Int = 20): Result<BrandsPage> = runCatching {
        val url = publicPath("brands").toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("offset", offset.coerceAtLeast(0).toString())
            ?.addQueryParameter("limit", limit.coerceAtLeast(1).toString())
            ?.apply { q?.trim()?.takeIf { it.isNotEmpty() }?.let { addQueryParameter("q", it) } }
            ?.build()
            ?.toString()
            ?: error("invalid brands url")
        Log.d(TAG, "getBrands GET $url")
        parseBrandsPage(JSONObject(executeGet(url).trim()))
    }

    fun getAestheticTags(all: Boolean = false, q: String? = null, status: String? = null, offset: Int = 0, limit: Int = 20): Result<List<CommonAestheticTagDto>> = runCatching {
        val builder = publicPath("aesthetic-tags").toHttpUrlOrNull()?.newBuilder()
            ?: error("invalid aesthetic-tags url")
        if (all) {
            builder.addQueryParameter("all", "true")
        } else {
            builder.addQueryParameter("offset", offset.coerceAtLeast(0).toString())
            builder.addQueryParameter("limit", limit.coerceAtLeast(1).toString())
            q?.trim()?.takeIf { it.isNotEmpty() }?.let { builder.addQueryParameter("q", it) }
            status?.trim()?.takeIf { it.isNotEmpty() }?.let { builder.addQueryParameter("status", it) }
        }
        val url = builder.build().toString()
        Log.d(TAG, "getAestheticTags GET $url")
        val obj = JSONObject(executeGet(url).trim())
        if (all) {
            val arr = obj.optJSONArray("tags") ?: JSONArray()
            (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let { parseAestheticTag(it) } }
        } else {
            parseAestheticTagsPage(obj).items
        }
    }

    fun getSafeMeetupZones(
        provinceId: String? = null,
        districtId: String? = null,
        limit: Int = 30,
    ): Result<List<SafeMeetupZoneDto>> = runCatching {
        val builder = publicPath("safe-meetup-zones").toHttpUrlOrNull()?.newBuilder()
            ?: error("invalid safe-meetup-zones url")
        provinceId?.trim()?.takeIf { it.isNotEmpty() }?.let { builder.addQueryParameter("province_id", it) }
        districtId?.trim()?.takeIf { it.isNotEmpty() }?.let { builder.addQueryParameter("district_id", it) }
        builder.addQueryParameter("limit", limit.coerceIn(1, 100).toString())
        val url = builder.build().toString()
        Log.d(TAG, "getSafeMeetupZones GET $url")
        val body = executeGet(url).trim()
        val obj = JSONObject(body)
        val arr = obj.optJSONArray("zones") ?: JSONArray()
        val zones = (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { parseSafeMeetupZone(it) }
        }
        Log.d(TAG, "getSafeMeetupZones parsed ${zones.size} zones")
        zones
    }.onFailure { e ->
        Log.w(TAG, "getSafeMeetupZones failed: ${e.message}", e)
    }

    fun getReviewBadges(): Result<List<ReviewBadgeDto>> = runCatching {
        val url = publicPath("review-badges").toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("all", "true")
            ?.build()
            ?.toString()
            ?: error("invalid review-badges url")
        Log.d(TAG, "getReviewBadges GET $url")
        val obj = JSONObject(executeGet(url).trim())
        val arr = obj.optJSONArray("badges") ?: JSONArray()
        (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { parseReviewBadge(it) }
        }.sortedBy { it.sortOrder }
    }.onFailure { e ->
        Log.w(TAG, "getReviewBadges failed: ${e.message}", e)
    }

    fun getCountries(all: Boolean = false, q: String? = null, status: String? = null, offset: Int = 0, limit: Int = 20): Result<List<CommonCountryDto>> = runCatching {
        val builder = publicPath("countries").toHttpUrlOrNull()?.newBuilder()
            ?: error("invalid countries url")
        if (all) {
            builder.addQueryParameter("all", "true")
        } else {
            builder.addQueryParameter("offset", offset.coerceAtLeast(0).toString())
            builder.addQueryParameter("limit", limit.coerceAtLeast(1).toString())
            q?.trim()?.takeIf { it.isNotEmpty() }?.let { builder.addQueryParameter("q", it) }
            status?.trim()?.takeIf { it.isNotEmpty() }?.let { builder.addQueryParameter("status", it) }
        }
        val url = builder.build().toString()
        Log.d(TAG, "getCountries GET $url")
        val obj = JSONObject(executeGet(url).trim())
        if (all) {
            val arr = obj.optJSONArray("countries") ?: JSONArray()
            (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let { parseCountry(it) } }
        } else {
            parseCountriesPage(obj).items
        }
    }
}

private fun parseSafeMeetupZone(o: JSONObject): SafeMeetupZoneDto =
    SafeMeetupZoneDto(
        id = o.optString("id"),
        name = o.optString("name"),
        nameVi = o.optString("name_vi", o.optString("nameVi", "")),
        zoneType = o.optString("zone_type", o.optString("zoneType", "")),
        provinceId = o.optString("province_id", o.optString("provinceId", "")),
        districtId = o.optString("district_id", o.optString("districtId", "")).takeIf { it.isNotBlank() },
        addressLine = o.optString("address_line", o.optString("addressLine", "")),
        locationUrl = o.optString("location_url", o.optString("locationUrl", "")),
        sortOrder = o.optInt("sort_order", o.optInt("sortOrder", 0)),
    )

private fun parseReviewBadge(o: JSONObject): ReviewBadgeDto =
    ReviewBadgeDto(
        id = o.optString("id"),
        slug = o.optString("slug"),
        nameEn = o.optString("name_en", o.optString("nameEn", "")),
        nameVi = o.optString("name_vi", o.optString("nameVi", "")),
        emoji = o.optString("emoji"),
        sortOrder = o.optInt("sort_order", o.optInt("sortOrder", 0)),
    )

private fun parseCategoryTreeNode(o: JSONObject): CategoryTreeNode {
    val children = mutableListOf<CategoryTreeNode>()
    val arr = o.optJSONArray("children") ?: JSONArray()
    for (i in 0 until arr.length()) {
        arr.optJSONObject(i)?.let { children.add(parseCategoryTreeNode(it)) }
    }
    return CategoryTreeNode(
        id = o.optString("id"),
        name = o.optString("name"),
        slug = o.optString("slug"),
        parentId = o.optString("parent_id").takeIf { it.isNotBlank() },
        sortOrder = o.optInt("sort_order", 0),
        status = o.optString("status"),
        createdAt = o.optString("created_at").takeIf { it.isNotBlank() },
        updatedAt = o.optString("updated_at").takeIf { it.isNotBlank() },
        children = children,
    )
}

private fun parseBrandsPage(obj: JSONObject): BrandsPage {
    val arr = obj.optJSONArray("items") ?: JSONArray()
    val items = (0 until arr.length()).mapNotNull { i ->
        arr.optJSONObject(i)?.let { parseBrand(it) }
    }
    return BrandsPage(
        items = items,
        total = obj.optLong("total", items.size.toLong()),
        offset = obj.optInt("offset", 0),
        limit = obj.optInt("limit", 20),
        hasMore = obj.optBoolean("has_more", false),
    )
}

private fun parseBrand(o: JSONObject): CommonBrandDto =
    CommonBrandDto(
        id = o.optString("id"),
        name = o.optString("name"),
        slug = o.optString("slug"),
        country = o.optString("country"),
        logoUrl = o.optString("logo_url"),
        status = o.optString("status"),
        createdAt = o.optString("created_at").takeIf { it.isNotBlank() },
        updatedAt = o.optString("updated_at").takeIf { it.isNotBlank() },
    )

private fun parseAestheticTag(o: JSONObject): CommonAestheticTagDto =
    CommonAestheticTagDto(
        id = o.optString("id"),
        name = o.optString("name"),
        displayName = o.optString("display_name").ifBlank { o.optString("displayName") },
        displayNameVi = o.optString("display_name_vi", o.optString("displayNameVi", "")),
        sortOrder = o.optInt("sort_order", 0),
        status = o.optString("status"),
        createdAt = o.optString("created_at").takeIf { it.isNotBlank() },
        updatedAt = o.optString("updated_at").takeIf { it.isNotBlank() },
    )

private fun parseAestheticTagsPage(obj: JSONObject): AestheticTagsPage {
    val arr = obj.optJSONArray("items") ?: JSONArray()
    val items = (0 until arr.length()).mapNotNull { i ->
        arr.optJSONObject(i)?.let { parseAestheticTag(it) }
    }
    return AestheticTagsPage(
        items = items,
        total = obj.optLong("total", items.size.toLong()),
        offset = obj.optInt("offset", 0),
        limit = obj.optInt("limit", 20),
        hasMore = obj.optBoolean("has_more", false),
    )
}

private fun parseCountry(o: JSONObject): CommonCountryDto =
    CommonCountryDto(
        id = o.optString("id"),
        iso2 = o.optString("iso2"),
        iso3 = o.optString("iso3"),
        name = o.optString("name"),
        numericCode = when {
            !o.has("numeric_code") || o.isNull("numeric_code") -> null
            else -> o.optInt("numeric_code")
        },
        phonePrefix = o.optString("phone_prefix"),
        emoji = o.optString("emoji"),
        sortOrder = o.optInt("sort_order", 0),
        status = o.optString("status"),
        createdAt = o.optString("created_at").takeIf { it.isNotBlank() },
        updatedAt = o.optString("updated_at").takeIf { it.isNotBlank() },
    )

private fun parseCountriesPage(obj: JSONObject): CountriesPage {
    val arr = obj.optJSONArray("items") ?: JSONArray()
    val items = (0 until arr.length()).mapNotNull { i ->
        arr.optJSONObject(i)?.let { parseCountry(it) }
    }
    return CountriesPage(
        items = items,
        total = obj.optLong("total", items.size.toLong()),
        offset = obj.optInt("offset", 0),
        limit = obj.optInt("limit", 20),
        hasMore = obj.optBoolean("has_more", false),
    )
}
