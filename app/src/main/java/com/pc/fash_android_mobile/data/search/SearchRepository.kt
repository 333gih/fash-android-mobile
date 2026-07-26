package com.pc.fash_android_mobile.data.search

import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.network.PublicBrowseHttp
import com.pc.fash_android_mobile.data.http.CoreServiceErrors
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.listing.ListingFeedJsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Global trending query row from `GET /search/trending-queries`. */
data class TrendingQueryItem(
    val query: String,
    val count: Int,
)

/**
 * Aesthetic tag with both ID and display name, returned by
 * `GET /search/trending-tags?include_ids=true`.
 * Using the ID directly avoids the fragile name→UUID catalog lookup (Bug C fix).
 */
data class TrendingTagChip(
    val id: String,
    val name: String,
)

/**
 * Search API (`/search/listings`, `/search/autocomplete`, `/search/trending-tags`, recent/trending queries).
 */
/** Max style quick chips on Explore (core trending + catalog resolve). */
const val EXPLORE_STYLE_QUICK_CHIP_LIMIT = 8

class SearchRepository(
    private val securedClient: OkHttpClient,
    private val publicBrowseClient: OkHttpClient? = null,
) {

    private fun client(publicBrowse: Boolean): OkHttpClient =
        if (publicBrowse) {
            publicBrowseClient ?: error("Public browse HTTP client is not configured")
        } else {
            securedClient
        }

    fun getTrendingTags(limit: Int = EXPLORE_STYLE_QUICK_CHIP_LIMIT): Result<List<String>> = runCatching {
        val capped = limit.coerceIn(1, 20)
        val url = "${AppEnvironment.apiPath("api/v1/search/trending-tags")}?limit=$capped"
        val body = executeGet(url, publicBrowse = false)
        parseStringArray(body).take(capped)
    }

    /**
     * `GET /search/trending-tags?include_ids=true` — returns [{id, name}] objects.
     * Falls back to names-only (id = "") when the server does not support the param.
     */
    fun getTrendingTagsWithIds(limit: Int = EXPLORE_STYLE_QUICK_CHIP_LIMIT): Result<List<TrendingTagChip>> = runCatching {
        val capped = limit.coerceIn(1, 20)
        val url = "${AppEnvironment.apiPath("api/v1/search/trending-tags")}?limit=$capped&include_ids=true"
        val body = executeGet(url, publicBrowse = false)
        parseTrendingTagChips(body).ifEmpty {
            // Server returned a plain string array — wrap with empty id for graceful degradation.
            parseStringArray(body).take(capped).map { TrendingTagChip(id = "", name = it) }
        }.take(capped)
    }

    /** Guest browse: ranked aesthetic tag names from core listing activity (7-day window). */
    fun browseTrendingAestheticTags(limit: Int = EXPLORE_STYLE_QUICK_CHIP_LIMIT): Result<List<String>> = runCatching {
        val capped = limit.coerceIn(1, 20)
        val url = "${PublicBrowseHttp.publicApiPath("browse/trending-aesthetic-tags")}?limit=$capped"
        val body = executeGet(url, publicBrowse = true)
        parseStringArray(body).take(capped)
    }

    /** `GET /search/recent-queries` — this user’s recent normalized queries (up to 20). */
    fun getRecentQueries(): Result<List<String>> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/search/recent-queries")
        val body = executeGet(url, publicBrowse = false)
        parseStringArray(body)
    }

    /** `GET /search/trending-queries` — global top queries in the last 7 days. */
    fun getTrendingQueries(): Result<List<TrendingQueryItem>> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/search/trending-queries")
        val body = executeGet(url, publicBrowse = false)
        parseTrendingQueriesArray(body)
    }

    /**
     * `GET /search/listings` — full-text search / browse.
     *
     * [aestheticTagIds] — comma-separated tag UUIDs (OR). Prefer over legacy [tags] names when both set.
     * [aestheticTagId] — single tag UUID (optional convenience).
     * [sizingMode] — `all` (omit) or `match_profile`.
     * [countryId] — catalog UUID (`country_id`).
     * [countryIso2] — two-letter A–Z (`country_iso2`).
     */
    fun searchListings(
        q: String = "",
        categoryId: String? = null,
        /** Comma-separated aesthetic tag UUIDs (OR). */
        aestheticTagIds: List<String>? = null,
        aestheticTagId: String? = null,
        sizingMode: String? = null,
        brandId: String? = null,
        countryId: String? = null,
        countryIso2: String? = null,
        /** Legacy: comma-separated aesthetic tag names (server may still accept as `tags`). */
        tags: String? = null,
        minPrice: Long? = null,
        maxPrice: Long? = null,
        condition: String? = null,
        sort: String = "recent",
        limit: Int = 20,
        offset: Int = 0,
        sellerProvinceId: String? = null,
        sellerDistrictId: String? = null,
        sellerWardId: String? = null,
    ): Result<List<ListingFeedItem>> = runCatching {
        val enc = { s: String -> java.net.URLEncoder.encode(s, "UTF-8") }
        val query = mutableListOf<String>()
        query.add("limit=$limit")
        query.add("offset=$offset")
        query.add("q=${enc(q)}")
        categoryId?.takeIf { it.isNotBlank() }?.let { query.add("category_id=${enc(it.trim())}") }
        val idCsv = aestheticTagIds
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(",")
        idCsv?.let { query.add("aesthetic_tag_ids=${enc(it)}") }
        aestheticTagId?.takeIf { it.isNotBlank() }?.let { query.add("aesthetic_tag_id=${enc(it.trim())}") }
        sizingMode?.takeIf { it.isNotBlank() && !it.equals("all", ignoreCase = true) }
            ?.let { query.add("sizing_mode=${enc(it.trim().lowercase())}") }
        brandId?.takeIf { it.isNotBlank() }?.let { query.add("brand_id=${enc(it.trim())}") }
        countryId?.takeIf { it.isNotBlank() }?.let { query.add("country_id=${enc(it.trim())}") }
        countryIso2?.trim()?.uppercase(Locale.US)?.takeIf { it.length == 2 && it.all { c -> c in 'A'..'Z' } }
            ?.let { query.add("country_iso2=${enc(it)}") }
        tags?.takeIf { it.isNotBlank() && idCsv == null }?.let { query.add("tags=${enc(it)}") }
        minPrice?.let { query.add("min_price=$it") }
        maxPrice?.let { query.add("max_price=$it") }
        sellerProvinceId?.trim()?.takeIf { it.isNotEmpty() }?.let { query.add("seller_province_id=${enc(it)}") }
        sellerDistrictId?.trim()?.takeIf { it.isNotEmpty() }?.let { query.add("seller_district_id=${enc(it)}") }
        sellerWardId?.trim()?.takeIf { it.isNotEmpty() }?.let { query.add("seller_ward_id=${enc(it)}") }
        query.add("sort=${enc(sort)}")
        val url = AppEnvironment.apiPath("api/v1/search/listings") + "?" + query.joinToString("&")
        val body = executeGet(url, publicBrowse = false)
        ListingFeedJsonParser.parseFeedArray(body)
    }

    /** Guest browse: `GET /api/v1/public/browse/listings` (requires public client attestation headers). */
    fun browseListings(
        q: String = "",
        categoryId: String? = null,
        aestheticTagIds: List<String>? = null,
        brandId: String? = null,
        countryId: String? = null,
        countryIso2: String? = null,
        minPrice: Long? = null,
        maxPrice: Long? = null,
        condition: String? = null,
        sort: String = "popular",
        limit: Int = 20,
        offset: Int = 0,
        sellerProvinceId: String? = null,
        sellerDistrictId: String? = null,
        sellerWardId: String? = null,
    ): Result<List<ListingFeedItem>> = runCatching {
        val enc = { s: String -> java.net.URLEncoder.encode(s, "UTF-8") }
        val query = mutableListOf<String>()
        query.add("limit=$limit")
        query.add("offset=$offset")
        query.add("q=${enc(q)}")
        categoryId?.takeIf { it.isNotBlank() }?.let { query.add("category_id=${enc(it.trim())}") }
        val idCsv = aestheticTagIds
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(",")
        idCsv?.let { query.add("aesthetic_tag_ids=${enc(it)}") }
        brandId?.takeIf { it.isNotBlank() }?.let { query.add("brand_id=${enc(it.trim())}") }
        countryId?.takeIf { it.isNotBlank() }?.let { query.add("country_id=${enc(it.trim())}") }
        countryIso2?.trim()?.uppercase(java.util.Locale.US)?.takeIf { it.length == 2 && it.all { c -> c in 'A'..'Z' } }
            ?.let { query.add("country_iso2=${enc(it)}") }
        minPrice?.let { query.add("min_price=$it") }
        maxPrice?.let { query.add("max_price=$it") }
        condition?.takeIf { it.isNotBlank() }?.let { query.add("condition=${enc(it)}") }
        sellerProvinceId?.trim()?.takeIf { it.isNotEmpty() }?.let { query.add("seller_province_id=${enc(it)}") }
        sellerDistrictId?.trim()?.takeIf { it.isNotEmpty() }?.let { query.add("seller_district_id=${enc(it)}") }
        sellerWardId?.trim()?.takeIf { it.isNotEmpty() }?.let { query.add("seller_ward_id=${enc(it)}") }
        query.add("sort=${enc(sort)}")
        val url = "${PublicBrowseHttp.publicApiPath("browse/listings")}?" + query.joinToString("&")
        val body = executeGet(url, publicBrowse = true)
        ListingFeedJsonParser.parseFeedArray(body)
    }

    /**
     * `GET /search/autocomplete?q=` — listing title suggestions.
     */
    fun autocompleteListingTitles(prefix: String): Result<List<String>> = runCatching {
        if (prefix.isBlank()) return@runCatching emptyList()
        val url = "${AppEnvironment.apiPath("api/v1/search/autocomplete")}?q=${java.net.URLEncoder.encode(prefix, "UTF-8")}"
        val body = executeGet(url, publicBrowse = false)
        parseStringArray(body)
    }

    fun browseFeaturedSellersPage(limit: Int = 50, offset: Int = 0): Result<FeaturedSellersPage> = runCatching {
        val cappedLimit = limit.coerceIn(1, 50)
        val safeOffset = offset.coerceAtLeast(0)
        val url = "${PublicBrowseHttp.publicApiPath("browse/featured-sellers")}?limit=$cappedLimit&offset=$safeOffset"
        val body = executeGet(url, publicBrowse = true)
        parseFeaturedSellersPage(body)
    }

    /**
     * `GET /search/featured-sellers` — ranked seller profiles with preview listing UUIDs (max [limit] 50).
     * Convenience wrapper that drops `total` — prefer [getFeaturedSellersPage] when paginating.
     */
    fun getFeaturedSellers(limit: Int = 50, offset: Int = 0): Result<List<FeaturedSellerItem>> =
        getFeaturedSellersPage(limit, offset).map { it.items }

    /**
     * `GET /search/featured-sellers?limit&offset` — returns `{items, total}` for see-all pagination.
     */
    fun getFeaturedSellersPage(limit: Int = 50, offset: Int = 0): Result<FeaturedSellersPage> = runCatching {
        val cappedLimit = limit.coerceIn(1, 50)
        val safeOffset = offset.coerceAtLeast(0)
        val relative =
            "api/v1/search/featured-sellers?limit=$cappedLimit&offset=$safeOffset"
        val urls = AppEnvironment.coreApiCandidateUrls(relative)
        var last: Exception? = null
        for (url in urls) {
            try {
                val body = executeGet(url, publicBrowse = false)
                return@runCatching parseFeaturedSellersPage(body)
            } catch (e: Exception) {
                last = e
            }
        }
        throw last ?: IllegalStateException("featured-sellers: no candidate URL")
    }

    private fun executeGet(url: String, publicBrowse: Boolean): String {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("User-Agent", "FashAndroid/1.0")
            .build()
        return client(publicBrowse).newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw CoreServiceErrors.toHttpException(
                    response.code,
                    body,
                    response.header("Retry-After"),
                )
            }
            body
        }
    }

    private fun parseTrendingQueriesArray(json: String): List<TrendingQueryItem> {
        val raw = json.trim()
        if (!raw.startsWith("[")) return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).mapNotNull { i ->
            val el = arr.optJSONObject(i) ?: return@mapNotNull null
            val q = el.optString("query", el.optString("Query", "")).trim()
            if (q.isBlank()) return@mapNotNull null
            val c = el.optInt("count", el.optInt("Count", 0))
            TrendingQueryItem(query = q, count = c.coerceAtLeast(0))
        }
    }

    private fun parseTrendingTagChips(json: String): List<TrendingTagChip> {
        val raw = json.trim()
        // Expect a JSON array of {id, name} objects.
        if (!raw.startsWith("[")) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val el = arr.optJSONObject(i) ?: return@mapNotNull null
                val id = el.optString("id", "").trim()
                val name = el.optString("name", "").trim()
                if (name.isBlank()) null else TrendingTagChip(id = id, name = name)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseStringArray(json: String): List<String> {
        val raw = json.trim()
        return when {
            raw.startsWith("[") -> {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
            }
            else -> try {
                val obj = JSONObject(raw)
                if (obj.has("data")) {
                    val arr = obj.getJSONArray("data")
                    (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
                } else {
                    emptyList()
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
