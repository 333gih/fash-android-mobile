package com.pc.fash_android_mobile.data.recommendation

import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.listing.ListingFeedJsonParser
import com.pc.fash_android_mobile.network.PublicBrowseHttp
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Home sections from GET /recommendations/home-sections (or public browse variant). */
data class HomeRecommendationSections(
    val forYou: List<ListingFeedItem> = emptyList(),
    val stylePicks: List<ListingFeedItem> = emptyList(),
    val continueBrowsing: List<ListingFeedItem> = emptyList(),
    val similarToSaved: List<ListingFeedItem> = emptyList(),
)

/**
 * Personalized discovery (`/recommendations/*` and `/public/browse/recommendations/*`).
 */
class RecommendationRepository(
    private val securedClient: OkHttpClient,
    private val publicBrowseClient: OkHttpClient? = null,
) {

    private fun client(publicBrowse: Boolean): OkHttpClient =
        if (publicBrowse) {
            publicBrowseClient ?: error("Public browse HTTP client is not configured")
        } else {
            securedClient
        }

    fun exploreListings(
        publicBrowse: Boolean,
        categoryId: String? = null,
        aestheticTagIds: List<String>? = null,
        brandId: String? = null,
        minPrice: Long? = null,
        maxPrice: Long? = null,
        condition: String? = null,
        limit: Int = 20,
        offset: Int = 0,
    ): Result<List<ListingFeedItem>> = runCatching {
        val enc = { s: String -> java.net.URLEncoder.encode(s, "UTF-8") }
        val q = mutableListOf("limit=$limit", "offset=$offset")
        categoryId?.takeIf { it.isNotBlank() }?.let { q.add("category_id=${enc(it.trim())}") }
        val idCsv = aestheticTagIds
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(",")
        idCsv?.let { q.add("aesthetic_tag_ids=${enc(it)}") }
        brandId?.takeIf { it.isNotBlank() }?.let { q.add("brand_id=${enc(it.trim())}") }
        minPrice?.let { q.add("min_price=$it") }
        maxPrice?.let { q.add("max_price=$it") }
        condition?.takeIf { it.isNotBlank() }?.let { q.add("condition=${enc(it)}") }
        val path = if (publicBrowse) {
            PublicBrowseHttp.publicApiPath("browse/recommendations/explore-listings")
        } else {
            AppEnvironment.apiPath("api/v1/recommendations/explore-listings")
        }
        val body = executeGet("$path?${q.joinToString("&")}", publicBrowse)
        ListingFeedJsonParser.parseFeedArray(body)
    }

    fun homeSections(
        publicBrowse: Boolean,
        forYouLimit: Int = 12,
        sectionLimit: Int = 8,
    ): Result<HomeRecommendationSections> = runCatching {
        val path = if (publicBrowse) {
            PublicBrowseHttp.publicApiPath("browse/recommendations/home-sections")
        } else {
            AppEnvironment.apiPath("api/v1/recommendations/home-sections")
        }
        val url = "$path?for_you_limit=$forYouLimit&section_limit=$sectionLimit"
        val body = executeGet(url, publicBrowse)
        val root = JSONObject(body)
        val data = root.optJSONObject("data") ?: root
        HomeRecommendationSections(
            forYou = ListingFeedJsonParser.parseItemsArray(data.optJSONArray("for_you")),
            stylePicks = ListingFeedJsonParser.parseItemsArray(data.optJSONArray("style_picks")),
            continueBrowsing = ListingFeedJsonParser.parseItemsArray(data.optJSONArray("continue_browsing")),
            similarToSaved = ListingFeedJsonParser.parseItemsArray(data.optJSONArray("similar_to_saved")),
        )
    }

    fun recordFeedEvents(
        publicBrowse: Boolean,
        sessionId: String,
        events: List<FeedEventPayload>,
    ): Result<Unit> = runCatching {
        if (events.isEmpty()) return@runCatching
        val path = if (publicBrowse) {
            PublicBrowseHttp.publicApiPath("browse/recommendations/feed-events")
        } else {
            AppEnvironment.apiPath("api/v1/recommendations/feed-events")
        }
        val arr = JSONArray()
        for (e in events.take(50)) {
            arr.put(
                JSONObject()
                    .put("listing_id", e.listingId)
                    .put("surface", e.surface)
                    .put("event_type", e.eventType)
                    .put("position", e.position)
                    .apply { e.dwellMs?.let { put("dwell_ms", it) } },
            )
        }
        val json = JSONObject()
            .put("session_id", sessionId)
            .put("events", arr)
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(path).post(body).build()
        client(publicBrowse).newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("feed-events HTTP ${resp.code}")
        }
    }

    private fun executeGet(url: String, publicBrowse: Boolean): String {
        val req = Request.Builder().url(url).get().build()
        return client(publicBrowse).newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("HTTP ${resp.code}: $text")
            text
        }
    }
}

data class FeedEventPayload(
    val listingId: String,
    val surface: String,
    val eventType: String,
    val position: Int = 0,
    val dwellMs: Int? = null,
)
