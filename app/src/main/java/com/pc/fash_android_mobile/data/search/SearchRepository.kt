package com.pc.fash_android_mobile.data.search

import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.listing.ListingFeedJsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/** Global trending query row from `GET /search/trending-queries`. */
data class TrendingQueryItem(
    val query: String,
    val count: Int,
)

/**
 * Search API (`/search/listings`, `/search/autocomplete`, `/search/trending-tags`, recent/trending queries).
 */
class SearchRepository(
    private val securedClient: OkHttpClient,
) {

    fun getTrendingTags(): Result<List<String>> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/search/trending-tags")
        val body = executeGet(url)
        parseStringArray(body)
    }

    /** `GET /search/recent-queries` — this user’s recent normalized queries (up to 20). */
    fun getRecentQueries(): Result<List<String>> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/search/recent-queries")
        val body = executeGet(url)
        parseStringArray(body)
    }

    /** `GET /search/trending-queries` — global top queries in the last 7 days. */
    fun getTrendingQueries(): Result<List<TrendingQueryItem>> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/search/trending-queries")
        val body = executeGet(url)
        parseTrendingQueriesArray(body)
    }

    /**
     * `GET /search/listings` — full-text search / browse.
     *
     * [aestheticTagIds] — comma-separated tag UUIDs (OR). Prefer over legacy [tags] names when both set.
     * [aestheticTagId] — single tag UUID (optional convenience).
     * [sizingMode] — `all` (omit) or `match_profile`.
     */
    fun searchListings(
        q: String = "",
        categoryId: String? = null,
        /** Comma-separated aesthetic tag UUIDs (OR). */
        aestheticTagIds: List<String>? = null,
        aestheticTagId: String? = null,
        sizingMode: String? = null,
        brandId: String? = null,
        /** Legacy: comma-separated aesthetic tag names (server may still accept as `tags`). */
        tags: String? = null,
        minPrice: Long? = null,
        maxPrice: Long? = null,
        condition: String? = null,
        sort: String = "recent",
        limit: Int = 20,
        offset: Int = 0,
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
        tags?.takeIf { it.isNotBlank() && idCsv == null }?.let { query.add("tags=${enc(it)}") }
        minPrice?.let { query.add("min_price=$it") }
        maxPrice?.let { query.add("max_price=$it") }
        condition?.takeIf { it.isNotBlank() }?.let { query.add("condition=${enc(it)}") }
        query.add("sort=${enc(sort)}")
        val url = AppEnvironment.apiPath("api/v1/search/listings") + "?" + query.joinToString("&")
        val body = executeGet(url)
        ListingFeedJsonParser.parseFeedArray(body)
    }

    /**
     * `GET /search/autocomplete?q=` — listing title suggestions.
     */
    fun autocompleteListingTitles(prefix: String): Result<List<String>> = runCatching {
        if (prefix.isBlank()) return@runCatching emptyList()
        val url = "${AppEnvironment.apiPath("api/v1/search/autocomplete")}?q=${java.net.URLEncoder.encode(prefix, "UTF-8")}"
        val body = executeGet(url)
        parseStringArray(body)
    }

    private fun executeGet(url: String): String {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("User-Agent", "FashAndroid/1.0")
            .build()
        return securedClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val msg = try { JSONObject(body).optString("error", body).ifBlank { body } } catch (_: Exception) { body }
                error("HTTP ${response.code}: $msg")
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
