package com.pc.fash_android_mobile.data.recommendation

import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.http.CoreServiceErrors
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.listing.ListingFeedJsonParser
import com.pc.fash_android_mobile.network.PublicBrowseHttp
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/** Home sections from GET /recommendations/home-sections (or public browse variant). */
data class HomeRecommendationSections(
    val huntToday: List<ListingFeedItem> = emptyList(),
    val forYou: List<ListingFeedItem> = emptyList(),
    val stylePicks: List<ListingFeedItem> = emptyList(),
    val continueBrowsing: List<ListingFeedItem> = emptyList(),
    val similarToSaved: List<ListingFeedItem> = emptyList(),
    val seasonalNearYou: List<ListingFeedItem> = emptyList(),
    val shoppingContext: ShoppingContext? = null,
)

// Personalized discovery: /recommendations/... and /public/browse/recommendations/...
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
        countryIso2: String? = null,
        limit: Int = 20,
        offset: Int = 0,
        /**
         * "all" (default) | "match_profile" — when "match_profile" the backend filters by viewer
         * profile reference size/measurements via Typesense. Empty profile silently degrades to
         * "all" so the request still succeeds; the UI nudges users to set up sizing.
         */
        sizingMode: String? = null,
        sellerProvinceId: String? = null,
        sellerDistrictId: String? = null,
        sellerWardId: String? = null,
        surface: String? = null,
        seasonKey: String? = null,
        excludeListingIds: List<String>? = null,
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
        countryIso2?.trim()?.uppercase(java.util.Locale.US)
            ?.takeIf { it.length == 2 && it.all { c -> c in 'A'..'Z' } }
            ?.let { q.add("country_iso2=${enc(it)}") }
        sizingMode?.takeIf { it.isNotBlank() && !it.equals("all", ignoreCase = true) }
            ?.let { q.add("sizing_mode=${enc(it.trim())}") }
        sellerProvinceId?.trim()?.takeIf { it.isNotEmpty() }?.let { q.add("seller_province_id=${enc(it)}") }
        sellerDistrictId?.trim()?.takeIf { it.isNotEmpty() }?.let { q.add("seller_district_id=${enc(it)}") }
        sellerWardId?.trim()?.takeIf { it.isNotEmpty() }?.let { q.add("seller_ward_id=${enc(it)}") }
        surface?.trim()?.takeIf { it.isNotEmpty() }?.let { q.add("surface=${enc(it)}") }
        seasonKey?.trim()?.takeIf { it.isNotEmpty() }?.let { q.add("season_key=${enc(it)}") }
        excludeListingIds
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(",")
            ?.let { q.add("exclude_listing_ids=${enc(it)}") }
        val path = if (publicBrowse) {
            PublicBrowseHttp.publicApiPath("browse/recommendations/explore-listings")
        } else {
            AppEnvironment.apiPath("api/v1/recommendations/explore-listings")
        }
        val response = executeGetWithResponse("$path?${q.joinToString("&")}", publicBrowse)
        RecExperimentContext.applyResponseHeaders(response.headers)
        ListingFeedJsonParser.parseFeedArray(response.body)
    }

    fun homeSections(
        publicBrowse: Boolean,
        huntTodayLimit: Int = 12,
        forYouLimit: Int = 16,
        sectionLimit: Int = 12,
        sizingMode: String? = null,
    ): Result<HomeRecommendationSections> = runCatching {
        val path = if (publicBrowse) {
            PublicBrowseHttp.publicApiPath("browse/recommendations/home-sections")
        } else {
            AppEnvironment.apiPath("api/v1/recommendations/home-sections")
        }
        val enc = { s: String -> java.net.URLEncoder.encode(s, "UTF-8") }
        val q = mutableListOf(
            "hunt_today_limit=$huntTodayLimit",
            "for_you_limit=$forYouLimit",
            "section_limit=$sectionLimit",
        )
        sizingMode?.takeIf { it.isNotBlank() && !it.equals("all", ignoreCase = true) }
            ?.let { q.add("sizing_mode=${enc(it.trim())}") }
        val url = "$path?${q.joinToString("&")}"
        val response = executeGetWithResponse(url, publicBrowse)
        val root = JSONObject(response.body)
        val data = root.optJSONObject("data") ?: root
        RecExperimentContext.parseMeta(data)
        RecExperimentContext.applyResponseHeaders(response.headers)
        HomeRecommendationSections(
            huntToday = ListingFeedJsonParser.parseItemsArray(data.optJSONArray("hunt_today")),
            forYou = ListingFeedJsonParser.parseItemsArray(data.optJSONArray("for_you")),
            stylePicks = ListingFeedJsonParser.parseItemsArray(data.optJSONArray("style_picks")),
            continueBrowsing = ListingFeedJsonParser.parseItemsArray(data.optJSONArray("continue_browsing")),
            similarToSaved = ListingFeedJsonParser.parseItemsArray(data.optJSONArray("similar_to_saved")),
            seasonalNearYou = ListingFeedJsonParser.parseItemsArray(data.optJSONArray("seasonal_near_you")),
            shoppingContext = ShoppingContext.fromJson(data.optJSONObject("shopping_context")),
        )
    }

    fun shoppingContext(publicBrowse: Boolean): Result<ShoppingContext> = runCatching {
        val path = if (publicBrowse) {
            PublicBrowseHttp.publicApiPath("browse/recommendations/context")
        } else {
            AppEnvironment.apiPath("api/v1/recommendations/context")
        }
        val body = executeGet(path, publicBrowse)
        val root = JSONObject(body)
        val data = root.optJSONObject("data") ?: root
        ShoppingContext.fromJson(data) ?: ShoppingContext()
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
                    .apply {
                        e.dwellMs?.let { put("dwell_ms", it) }
                        e.experimentId?.let { put("experiment_id", it) }
                    },
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

    private data class GetResponse(val body: String, val headers: okhttp3.Headers)

    private fun executeGetWithResponse(url: String, publicBrowse: Boolean): GetResponse {
        val req = Request.Builder().url(url).get().build()
        return client(publicBrowse).newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw CoreServiceErrors.toHttpException(
                    resp.code,
                    text,
                    resp.header("Retry-After"),
                )
            }
            GetResponse(body = text, headers = resp.headers)
        }
    }

    private fun executeGet(url: String, publicBrowse: Boolean): String =
        executeGetWithResponse(url, publicBrowse).body

    fun getUnifiedHomeFeed(
        publicBrowse: Boolean = false,
        railLimit: Int = 8,
        itemsPerRail: Int = 6,
        sizingMode: String = "all",
    ): Result<com.pc.fash_android_mobile.data.model.UnifiedHomeFeedResponse> = runCatching {
        val enc = { s: String -> java.net.URLEncoder.encode(s, "UTF-8") }
        val q = mutableListOf(
            "rail_limit=$railLimit",
            "items_per_rail=$itemsPerRail",
            "sizing_mode=${enc(sizingMode)}"
        )
        val path = if (publicBrowse) {
            PublicBrowseHttp.publicApiPath("browse/recommendations/unified-home-feed")
        } else {
            AppEnvironment.apiPath("api/v1/recommendations/unified-home-feed")
        }
        val response = executeGetWithResponse("$path?${q.joinToString("&")}", publicBrowse)
        val json = JSONObject(response.body)

        // Parse rails
        val railsArray = json.optJSONArray("rails") ?: JSONArray()
        val rails = (0 until railsArray.length()).map { i ->
            val railObj = railsArray.getJSONObject(i)
            parseHomeRail(railObj)
        }

        // Parse hero card
        val heroCard = json.optJSONObject("hero")?.let { parseHeroCard(it) }

        // Parse meta
        val metaObj = json.optJSONObject("recommendation_meta")
        val meta = metaObj?.let {
            com.pc.fash_android_mobile.data.model.RecommendationMeta(
                activeExperiments = it.optJSONArray("active_experiments")
                    ?.let { arr -> (0 until arr.length()).map { i -> arr.optString(i) } }
                    ?: emptyList()
            )
        }

        com.pc.fash_android_mobile.data.model.UnifiedHomeFeedResponse(
            rails = rails,
            hero = heroCard,
            meta = meta
        )
    }

    private fun parseHomeRail(json: JSONObject): com.pc.fash_android_mobile.data.model.HomeRail {
        val itemsArray = json.optJSONArray("items") ?: JSONArray()
        val items = (0 until itemsArray.length()).map { i ->
            val itemObj = itemsArray.getJSONObject(i)
            parseListingWithMatch(itemObj)
        }

        return com.pc.fash_android_mobile.data.model.HomeRail(
            railId = json.optString("rail_id"),
            railType = json.optString("rail_type"),
            title = json.optString("title"),
            subtitle = json.optString("subtitle").takeIf { it.isNotBlank() },
            items = items,
            seeAllUrl = json.optString("see_all_url").takeIf { it.isNotBlank() },
            reasonLabel = json.optString("reason_label").takeIf { it.isNotBlank() }
        )
    }

    private fun parseListingWithMatch(json: JSONObject): com.pc.fash_android_mobile.data.model.ListingWithMatch {
        val listingObj = json.getJSONObject("listing")
        val listing = ListingFeedJsonParser.parseListingFeedItem(listingObj)

        val sizeMatch = json.optJSONObject("size_match")?.let {
            com.pc.fash_android_mobile.data.model.SizeMatchInfo(
                badge = it.optString("badge"),
                confidence = it.optDouble("confidence"),
                reason = it.optString("reason")
            )
        }

        val measurements = json.optJSONObject("measurements")?.let { obj ->
            buildMap {
                obj.keys().forEach { key ->
                    put(key, obj.optDouble(key))
                }
            }
        }

        return com.pc.fash_android_mobile.data.model.ListingWithMatch(
            listing = listing,
            sizeMatch = sizeMatch,
            measurements = measurements,
            recommendReason = json.optString("recommend_reason").takeIf { it.isNotBlank() },
            imageAspectRatio = json.optString("image_aspect_ratio").takeIf { it.isNotBlank() }
        )
    }

    private fun parseHeroCard(json: JSONObject): com.pc.fash_android_mobile.data.model.HeroCard {
        return com.pc.fash_android_mobile.data.model.HeroCard(
            type = json.optString("type"),
            imageURL = json.optString("image_url").takeIf { it.isNotBlank() },
            title = json.optString("title"),
            subtitle = json.optString("subtitle").takeIf { it.isNotBlank() },
            ctaURL = json.optString("cta_url").takeIf { it.isNotBlank() },
            aspectRatio = json.optString("aspect_ratio", "3:4")
        )
    }

    fun uxPersonalization(clientHour: Int? = null): Result<UxPersonalizationBundle> = runCatching {
        val path = AppEnvironment.apiPath("api/v1/recommendations/ux-personalization")
        val url = if (clientHour != null) "$path?client_hour=$clientHour" else path
        val body = executeGet(url, publicBrowse = false)
        parseUxPersonalization(body)
    }

    fun recordUxEvents(events: List<UxEventPayload>): Result<Unit> = runCatching {
        if (events.isEmpty()) return@runCatching
        val path = AppEnvironment.apiPath("api/v1/recommendations/ux-events")
        val arr = JSONArray()
        for (e in events.take(50)) {
            arr.put(
                JSONObject()
                    .put("scope", e.scope)
                    .put("tab_key", e.tabKey)
                    .apply {
                        e.clientHour?.let { put("client_hour", it) }
                        e.dwellMs?.let { put("dwell_ms", it) }
                    },
            )
        }
        val json = JSONObject().put("events", arr)
        val reqBody = json.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(path).post(reqBody).build()
        securedClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("ux-events HTTP ${resp.code}")
        }
    }

    private fun parseUxPersonalization(body: String): UxPersonalizationBundle {
        val root = JSONObject(body)
        val data = root.optJSONObject("data") ?: root
        val home = data.optJSONObject("home") ?: JSONObject()
        val profile = data.optJSONObject("profile") ?: JSONObject()
        val shortcutObj = home.optJSONObject("explore_shortcut")
        val shortcut = shortcutObj?.let {
            HomeExploreShortcut(
                labelKey = it.optString("label_key"),
                aestheticTagId = it.optString("aesthetic_tag_id").takeIf { id -> id.isNotBlank() },
                aestheticTagName = it.optString("aesthetic_tag_name").takeIf { n -> n.isNotBlank() },
                categoryId = it.optString("category_id").takeIf { id -> id.isNotBlank() },
                brandId = it.optString("brand_id").takeIf { id -> id.isNotBlank() },
            )
        }
        val sectionLimits = mutableMapOf<String, Int>()
        home.optJSONObject("section_limits")?.let { limits ->
            limits.keys().forEach { key ->
                sectionLimits[key] = limits.optInt(key)
            }
        }
        return UxPersonalizationBundle(
            home = HomeUxPersonalization(
                defaultTabKey = home.optString("default_tab", HomeFeedTabKeys.HUNT_TODAY),
                tabOrder = home.optJSONArray("tab_order").toStringList(),
                prefetchTabs = home.optJSONArray("prefetch_tabs").toStringList(),
                sectionLimits = sectionLimits,
                exploreShortcut = shortcut,
            ),
            profile = ProfileUxPersonalization(
                defaultTabKey = profile.optString("default_tab_key", ProfileTabKeys.SELLING),
                tabOrderKeys = profile.optJSONArray("tab_order_keys").toStringList(),
                primaryMode = profile.optString("primary_mode", "balanced"),
            ),
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList(length()) {
            for (i in 0 until length()) {
                val v = optString(i).trim()
                if (v.isNotEmpty()) add(v)
            }
        }
    }
}

data class FeedEventPayload(
    val listingId: String,
    val surface: String,
    val eventType: String,
    val position: Int = 0,
    val dwellMs: Int? = null,
    val experimentId: String? = null,
)
