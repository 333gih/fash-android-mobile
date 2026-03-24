package com.pc.fash_android_mobile.data.search

import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.listing.ListingFeedJsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * Search API (`/search/listings`, `/search/autocomplete`, `/search/trending-tags`).
 */
class SearchRepository(
    private val securedClient: OkHttpClient,
) {

    fun getTrendingTags(): Result<List<String>> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/search/trending-tags")
        val body = executeGet(url)
        parseStringArray(body)
    }

    /**
     * `GET /search/listings` — full-text search / browse.
     */
    fun searchListings(
        q: String = "",
        categoryId: String? = null,
        tags: String? = null,
        minPrice: Long? = null,
        maxPrice: Long? = null,
        condition: String? = null,
        sort: String = "recent",
        limit: Int = 20,
        offset: Int = 0,
    ): Result<List<ListingFeedItem>> = runCatching {
        val query = mutableListOf<String>()
        query.add("limit=$limit")
        query.add("offset=$offset")
        query.add("q=${java.net.URLEncoder.encode(q, "UTF-8")}")
        categoryId?.takeIf { it.isNotBlank() }?.let { query.add("category_id=$it") }
        tags?.takeIf { it.isNotBlank() }?.let { query.add("tags=${java.net.URLEncoder.encode(it, "UTF-8")}") }
        minPrice?.let { query.add("min_price=$it") }
        maxPrice?.let { query.add("max_price=$it") }
        condition?.takeIf { it.isNotBlank() }?.let { query.add("condition=${java.net.URLEncoder.encode(it, "UTF-8")}") }
        query.add("sort=${java.net.URLEncoder.encode(sort, "UTF-8")}")
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
