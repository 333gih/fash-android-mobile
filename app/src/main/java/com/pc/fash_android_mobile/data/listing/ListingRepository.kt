package com.pc.fash_android_mobile.data.listing

import android.net.Uri
import android.util.Log
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.network.PublicBrowseHttp
import com.pc.fash_android_mobile.data.http.CoreServiceHttpException
import com.pc.fash_android_mobile.data.http.CoreServiceErrors
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

/**
 * Listing/feed API client. Uses secured client for authenticated endpoints.
 * Token is injected via SecuredApiClient interceptor.
 */
class ListingRepository(
    private val securedClient: OkHttpClient,
    private val publicBrowseClient: OkHttpClient? = null,
) {

    private fun httpClient(publicBrowse: Boolean): OkHttpClient =
        if (publicBrowse) {
            publicBrowseClient ?: error("Public browse HTTP client is not configured")
        } else {
            securedClient
        }

    private val userIdUuidRegex =
        Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    private fun throwHttpError(httpCode: Int, body: String): Nothing =
        throw CoreServiceHttpException(httpCode, CoreServiceErrors.parseErrorMessage(httpCode, body))

    private fun encodeUserPathSegment(segment: String): String =
        if (userIdUuidRegex.matches(segment)) segment else Uri.encode(segment, null)

    fun getHomeFeed(limit: Int = 20, offset: Int = 0): Result<List<ListingFeedItem>> = runCatching {
        val url = "${AppEnvironment.apiPath("api/v1/listings/home")}?limit=$limit&offset=$offset"
        parseFeedResponse(executeGet(url))
    }

    fun getListingDetail(listingId: String, publicBrowse: Boolean = false): Result<ListingDetail> = runCatching {
        val url = if (publicBrowse) {
            PublicBrowseHttp.publicApiPath("listings/$listingId")
        } else {
            AppEnvironment.apiPath("api/v1/listings/$listingId")
        }
        parseListingDetail(executeGet(url, publicBrowse))
    }

    /**
     * Authenticated: `GET /api/v1/users/me/listings` — all statuses for the signed-in user (paginated).
     */
    fun getMyListings(limit: Int = 50, offset: Int = 0): Result<List<ListingFeedItem>> = runCatching {
        val url =
            "${AppEnvironment.apiPath("api/v1/users/me/listings")}?limit=$limit&offset=$offset"
        parseFeedResponse(executeGet(url))
    }

    /**
     * Core-service: `GET /users/{id}/listings` (public seller storefront).
     */
    fun getListingsBySeller(
        sellerId: String,
        status: String? = null,
        limit: Int = 50,
        offset: Int = 0,
    ): Result<List<ListingFeedItem>> = runCatching {
        val seg = encodeUserPathSegment(sellerId.trim())
        val q = mutableListOf<String>()
        q.add("limit=$limit")
        q.add("offset=$offset")
        status?.takeIf { it.isNotBlank() }?.let { q.add("status=${java.net.URLEncoder.encode(it, "UTF-8")}") }
        val primary = AppEnvironment.apiPath("api/v1/users/$seg/listings") + "?" + q.joinToString("&")
        parseFeedResponse(executeGet(primary, publicBrowse = false))
    }

    /** Guest storefront: `GET /api/v1/public/users/{id}/listings` (default status active). */
    fun getListingsBySellerPublic(
        sellerId: String,
        status: String? = null,
        limit: Int = 50,
        offset: Int = 0,
    ): Result<List<ListingFeedItem>> = runCatching {
        val seg = encodeUserPathSegment(sellerId.trim())
        val q = mutableListOf("limit=$limit", "offset=$offset")
        status?.takeIf { it.isNotBlank() }?.let {
            q.add("status=${java.net.URLEncoder.encode(it, "UTF-8")}")
        }
        val url = "${PublicBrowseHttp.publicApiPath("users/$seg/listings")}?${q.joinToString("&")}"
        parseFeedResponse(executeGet(url, publicBrowse = true))
    }

    /** `GET /listings/wishlist` → listing id list (legacy / alternate response shape). */
    fun getWishlistListingIds(limit: Int = 50, offset: Int = 0): Result<List<String>> = runCatching {
        val url = "${AppEnvironment.apiPath("api/v1/listings/wishlist")}?limit=$limit&offset=$offset"
        val body = executeGet(url)
        val obj = JSONObject(body.trim())
        val root = if (obj.has("data")) obj.getJSONObject("data") else obj
        val arr = root.optJSONArray("listing_ids") ?: JSONArray("[]")
        (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
    }

    /** `GET /listings/wishlist` — full listings + `total` (viewer flags; paginated). */
    fun getWishlistListings(limit: Int = 50, offset: Int = 0): Result<List<ListingFeedItem>> = runCatching {
        val url = "${AppEnvironment.apiPath("api/v1/listings/wishlist")}?limit=$limit&offset=$offset"
        parseFeedResponse(executeGet(url))
    }

    /**
     * `GET /listings/recently-viewed` — this user's recently viewed active listings (newest first).
     * Server table `listing_views` is refreshed by [recordView] (POST /listings/{id}/view).
     * Same JSON shape as wishlist/home feed (snake_case ListingResponse + total).
     */
    fun getRecentlyViewed(limit: Int = 12, offset: Int = 0): Result<List<ListingFeedItem>> = runCatching {
        val cappedLimit = limit.coerceIn(1, 50)
        val safeOffset = offset.coerceAtLeast(0)
        val url = "${AppEnvironment.apiPath("api/v1/listings/recently-viewed")}?limit=$cappedLimit&offset=$safeOffset"
        parseFeedResponse(executeGet(url))
    }

    /**
     * Saved-listings count for buyer stats. Uses response `total` when present, else `listings` length.
     */
    fun getWishlistSavedCount(limit: Int = 100, offset: Int = 0): Result<Int> = runCatching {
        val url = "${AppEnvironment.apiPath("api/v1/listings/wishlist")}?limit=$limit&offset=$offset"
        val body = executeGet(url)
        val obj = JSONObject(body.trim())
        val root = if (obj.has("data")) obj.getJSONObject("data") else obj
        val t = root.optInt("total", root.optInt("Total", -1))
        if (t >= 0) return@runCatching t
        val arr = root.optJSONArray("listings") ?: root.optJSONArray("Listings") ?: JSONArray("[]")
        arr.length()
    }

    /** `PUT /listings/{id}` — category cannot change (omit). */
    fun updateListing(listingId: String, update: UpdateListingRequest): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/listings/${listingId.trim()}")
        val json = JSONObject()
        update.title?.let { json.put("title", it) }
        update.condition?.let { json.put("condition", it) }
        update.priceVnd?.let { json.put("price", it) }
        update.description?.let { json.put("description", it) }
        if (update.brandId != null || update.brandName != null) {
            val brandObj = JSONObject()
            update.brandId?.takeIf { it.isNotBlank() }?.let { brandObj.put("id", it) }
            if (update.brandName != null) brandObj.put("name", update.brandName)
            json.put("brand", brandObj)
        }
        update.size?.let { json.put("size", it) }
        update.aestheticTags?.let { json.put("aesthetic_tags", JSONArray(it)) }
        update.acceptOffers?.let { json.put("accept_offers", it) }
        update.autoPriceDropEnabled?.let { json.put("auto_price_drop_enabled", it) }
        update.floorPriceVnd?.let { json.put("floor_price", it) }
        update.priceDropPercent?.let { json.put("price_drop_percent", it) }
        update.countryOfOrigin?.let { json.put("country_of_origin", it) }
        update.countryId?.let { json.put("country_id", it) }
        update.countryName?.let { json.put("country_name", it) }
        update.measurementUnit?.let { json.put("measurement_unit", it) }
        update.measurementHem?.let { json.put("measurement_hem", it) }
        update.measurementChest?.let { json.put("measurement_chest", it) }
        update.measurementLength?.let { json.put("measurement_length", it) }
        update.measurementShoulders?.let { json.put("measurement_shoulders", it) }
        update.measurementSleeveLength?.let { json.put("measurement_sleeve_length", it) }
        update.shippingAddressId?.let { json.put("shipping_address_id", it) }
        update.color?.let { json.put("color", it) }
        update.genderTarget?.let { json.put("gender_target", it) }
        if (json.length() == 0) return@runCatching
        executePutJson(url, json.toString())
    }

    /** `DELETE /listings/{id}` */
    fun deleteListing(listingId: String): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/listings/${listingId.trim()}")
        securedClient.newCall(
            Request.Builder()
                .url(url)
                .delete()
                .header("Accept", "application/json")
                .header("User-Agent", "FashAndroid/1.0")
                .build(),
        ).execute().use { response ->
            if (!response.isSuccessful) {
                val b = response.body?.string().orEmpty()
                throwHttpError(response.code, b)
            }
        }
    }

    /** `POST /listings/{id}/sold` */
    fun markListingSoldOutsidePlatform(listingId: String): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/listings/${listingId.trim()}/sold")
        executePost(url)
        Unit
    }

    fun getCategories(): Result<List<Category>> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/categories")
        val body = executeGet(url)
        parseCategories(body)
    }

    private fun parseCategories(json: String): List<Category> {
        val raw = json.trim()
        val arr = when {
            raw.startsWith("[") -> JSONArray(raw)
            else -> try {
                val obj = JSONObject(raw)
                if (obj.has("data")) obj.getJSONArray("data") else JSONArray("[]")
            } catch (_: Exception) { JSONArray("[]") }
        }
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val catId = o.optString("id", "")
                .ifBlank { o.optString("ID", "") }
                .ifBlank { o.optString("category_id", "") }
                .ifBlank { o.optString("uuid", "") }
            Category(
                id = catId,
                name = o.optString("name", o.optString("Name", "")),
                slug = o.optString("slug", o.optString("Slug", "")),
            )
        }
    }

    fun toggleLike(listingId: String): Result<Boolean> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/listings/$listingId/like")
        val body = executePost(url)
        parseToggleBooleanResponse(body, "liked")
    }

    /**
     * Save / unsave for the current user.
     * - When [currentlySaved] is false: **POST** `/listings/{id}/save` (toggle semantics — add to wishlist).
     * - When [currentlySaved] is true: **DELETE** `/listings/{id}/save` (idempotent unsave; response `{ "saved": false }`).
     */
    fun toggleSave(listingId: String, currentlySaved: Boolean): Result<Boolean> = runCatching {
        val id = listingId.trim()
        val url = AppEnvironment.apiPath("api/v1/listings/$id/save")
        if (currentlySaved) {
            val body = executeDelete(url)
            parseToggleBooleanResponse(body, "saved")
            false
        } else {
            val body = executePost(url)
            parseToggleBooleanResponse(body, "saved")
        }
    }

    fun recordView(listingId: String): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/listings/$listingId/view")
        executePost(url)
        Unit
    }

    fun uploadListingImage(file: File): Result<String> =
        uploadListingImage(file.readBytes(), file.name, "image/jpeg")

    fun uploadListingImage(
        bytes: ByteArray,
        filename: String = "image.jpg",
        mimeType: String = "image/jpeg",
    ): Result<String> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/listings/images")
        val safeMime = mimeType.takeIf { it.contains('/') && !it.contains('*') } ?: "image/jpeg"
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                filename,
                bytes.toRequestBody(safeMime.toMediaType()),
            )
            .build()
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Accept", "application/json")
            .header("User-Agent", "FashAndroid/1.0")
            .build()
        val bodyStr = securedClient.newCall(request).execute().use { response ->
            val b = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throwHttpError(response.code, b)
            }
            b
        }
        JSONObject(bodyStr).optString("image_url", "").ifBlank { error("No image_url in response") }
    }

    fun createListing(request: CreateListingRequest): Result<CreateListingResponse> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/listings")
        val json = JSONObject()
        json.put("title", request.title)
        json.put("image_urls", listingImageStepsToJsonArray(request.imageUrlSteps))
        json.put("price", request.priceVnd)
        json.put("condition", request.condition)
        json.put("category", namedRefToJson(request.category, maxNameLen = 100))
        request.parentCategory?.let { json.put("parent_category", namedRefToJson(it, maxNameLen = 100)) }
        if (request.description.isNotBlank()) json.put("description", request.description)
        if (request.size.isNotBlank()) json.put("size", request.size)
        request.color?.takeIf { it.isNotBlank() }?.let { json.put("color", it.trim().lowercase()) }
        request.genderTarget?.takeIf { it.isNotBlank() }?.let { json.put("gender_target", it.trim().lowercase()) }
        request.brand?.let { json.put("brand", namedRefToJson(it, maxNameLen = 255)) }
        if (request.aestheticTags.isNotEmpty()) {
            val tagsArr = JSONArray()
            for (t in request.aestheticTags) {
                tagsArr.put(namedRefToJson(t, maxNameLen = 100))
            }
            json.put("aesthetic_tags", tagsArr)
        }
        request.countryOfOrigin?.takeIf { it.isNotBlank() }?.let { json.put("country_of_origin", it) }
        request.countryId?.takeIf { it.isNotBlank() }?.let { json.put("country_id", it) }
        request.countryName?.takeIf { it.isNotBlank() }?.let { json.put("country_name", it) }
        request.measurementUnit?.takeIf { it.isNotBlank() }?.let { json.put("measurement_unit", it) }
        request.measurementHem?.let { json.put("measurement_hem", it) }
        request.measurementChest?.let { json.put("measurement_chest", it) }
        request.measurementLength?.let { json.put("measurement_length", it) }
        request.measurementShoulders?.let { json.put("measurement_shoulders", it) }
        request.measurementSleeveLength?.let { json.put("measurement_sleeve_length", it) }
        request.acceptOffers?.let { json.put("accept_offers", it) }
        request.autoPriceDropEnabled?.let { json.put("auto_price_drop_enabled", it) }
        request.floorPriceVnd?.let { json.put("floor_price", it) }
        request.priceDropPercent?.let { json.put("price_drop_percent", it) }
        request.shippingAddressId?.takeIf { it.isNotBlank() }?.let { json.put("shipping_address_id", it) }
        val payload = json.toString()
        logCreateListingChunked(Log.DEBUG, "createListing request url=$url payload=", payload)
        val body = executePostJsonWithLoggedResponse(url, payload)
        logCreateListingChunked(Log.DEBUG, "createListing success url=$url http=200 responseBody=", body)
        val o = JSONObject(body)
        val dataObj = if (o.has("data")) o.optJSONObject("data") else null
        val id = (dataObj ?: o).let { obj ->
            obj.optString("ID", "").ifBlank { obj.optString("id", "") }
        }
        if (id.isBlank()) {
            Log.e(TAG_CREATE_LISTING, "createListing HTTP 200 but no listing id in JSON (full body was logged above as success responseBody)")
            error("No id in response")
        }
        CreateListingResponse(id = id)
    }

    private fun executePostJson(url: String, json: String): String {
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(JSON_MEDIA))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", "FashAndroid/1.0")
            .build()
        return securedClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throwHttpError(response.code, body)
            }
            body.ifBlank { "{}" }
        }
    }

    /**
     * Same as [executePostJson] but logs the raw response body for create-listing debugging
     * (success and failure include full body; long bodies are split across log lines).
     */
    private fun executePostJsonWithLoggedResponse(url: String, json: String): String {
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(JSON_MEDIA))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", "FashAndroid/1.0")
            .build()
        return securedClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                logCreateListingChunked(
                    Log.ERROR,
                    "createListing failure url=$url http=${response.code} responseBody=",
                    body.ifBlank { "<empty>" },
                )
                throwHttpError(response.code, body)
            }
            body.ifBlank { "{}" }
        }
    }

    private fun logCreateListingChunked(level: Int, prefix: String, text: String) {
        val chunkSize = 3500
        if (text.length <= chunkSize) {
            when (level) {
                Log.ERROR -> Log.e(TAG_CREATE_LISTING, prefix + text)
                else -> Log.d(TAG_CREATE_LISTING, prefix + text)
            }
            return
        }
        var i = 0
        var part = 0
        while (i < text.length) {
            val end = minOf(i + chunkSize, text.length)
            val line = "$prefix[part $part] ${text.substring(i, end)}"
            when (level) {
                Log.ERROR -> Log.e(TAG_CREATE_LISTING, line)
                else -> Log.d(TAG_CREATE_LISTING, line)
            }
            i = end
            part++
        }
    }

    private fun executeGet(url: String, publicBrowse: Boolean = false): String {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("User-Agent", "FashAndroid/1.0")
            .build()
        return httpClient(publicBrowse).newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throwHttpError(response.code, body)
            }
            body
        }
    }

    /**
     * POST /listings/{id}/like|save — response may be `{ "liked": true }`, `{ "data": { "liked": true } }`,
     * PascalCase keys, or OpenAPI placeholder maps (first boolean wins).
     */
    private fun parseToggleBooleanResponse(body: String, vararg keys: String): Boolean {
        val raw = body.trim().ifBlank { "{}" }
        val root = try {
            JSONObject(raw)
        } catch (_: Exception) {
            return false
        }
        val data = root.optJSONObject("data") ?: root.optJSONObject("Data") ?: root
        val candidateKeys = buildSet {
            for (k in keys) {
                add(k)
                add(k.replaceFirstChar { it.titlecase(Locale.ROOT) })
            }
        }
        for (k in candidateKeys) {
            if (data.has(k)) return data.optBoolean(k, false)
        }
        val it = data.keys()
        while (it.hasNext()) {
            val name = it.next()
            if (data.isNull(name)) continue
            try {
                if (data.get(name) is Boolean) return data.getBoolean(name)
            } catch (_: Exception) {
                continue
            }
        }
        return false
    }

    private fun executePost(url: String): String {
        val request = Request.Builder()
            .url(url)
            .post(ByteArray(0).toRequestBody(null))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", "FashAndroid/1.0")
            .build()
        return securedClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throwHttpError(response.code, body)
            }
            body.ifBlank { "{}" }
        }
    }

    private fun executeDelete(url: String): String {
        val request = Request.Builder()
            .url(url)
            .delete()
            .header("Accept", "application/json")
            .header("User-Agent", "FashAndroid/1.0")
            .build()
        return securedClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throwHttpError(response.code, body)
            }
            body.ifBlank { "{}" }
        }
    }

    private fun parseFeedResponse(json: String): List<ListingFeedItem> =
        ListingFeedJsonParser.parseFeedArray(json)

    /**
     * Listing tags / aesthetic_tags may be plain strings or `{ "id", "name" }` objects.
     * Using [JSONArray.optString] on object elements stringifies the whole JSON (bad for UI).
     */
    private fun parseTagStringArray(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { o ->
                o.optString("name", "").ifBlank { null }
                    ?: o.optString("Name", "").ifBlank { null }
            } ?: run {
                val s = arr.optString(i, "").trim()
                when {
                    s.isEmpty() -> null
                    s.startsWith("{") -> tagNameFromEmbeddedJson(s)
                    else -> s
                }
            }
        }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun tagNameFromEmbeddedJson(s: String): String? =
        try {
            JSONObject(s).optString("name", "").ifBlank { null }
                ?: JSONObject(s).optString("Name", "").ifBlank { null }
        } catch (_: Exception) {
            null
        }

    /** `aesthetic_tags`: `{ "id", "name", "display_name" }[]` — prefer display_name; keeps id for Explore. */
    private fun parseAestheticTagRefs(arr: JSONArray?): List<AestheticTagRef> {
        if (arr == null) return emptyList()
        val out = mutableListOf<AestheticTagRef>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i)
            if (o != null) {
                val id = o.optString("id", "").ifBlank { o.optString("ID", "") }.ifBlank { null }
                val label = o.optString("display_name", "").ifBlank { null }
                    ?: o.optString("DisplayName", "").ifBlank { null }
                    ?: o.optString("name", "").ifBlank { null }
                    ?: o.optString("Name", "").ifBlank { null }
                val t = label?.trim()?.takeIf { it.isNotEmpty() } ?: continue
                out.add(AestheticTagRef(id = id, label = t))
                continue
            }
            val s = arr.optString(i, "").trim()
            if (s.isEmpty()) continue
            if (s.startsWith("{")) {
                tagNameFromEmbeddedJson(s)?.let { out.add(AestheticTagRef(id = null, label = it)) }
            } else {
                out.add(AestheticTagRef(id = null, label = s))
            }
        }
        return out.distinctBy { it.label }
    }

    private fun optMeasurement(o: JSONObject, key: String): Double? {
        if (!o.has(key) || o.isNull(key)) return null
        val d = o.optDouble(key, Double.NaN)
        return if (d.isNaN()) null else d
    }

    private fun executePutJson(url: String, json: String) {
        val request = Request.Builder()
            .url(url)
            .put(json.toRequestBody(JSON_MEDIA))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", "FashAndroid/1.0")
            .build()
        securedClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throwHttpError(response.code, body)
            }
        }
    }

    private fun parseListingDetail(json: String): ListingDetail {
        val raw = json.trim()
        val o = when {
            raw.startsWith("{") -> try {
                val obj = JSONObject(raw)
                if (obj.has("data")) obj.getJSONObject("data") else obj
            } catch (_: Exception) { JSONObject(raw) }
            else -> JSONObject("{}")
        }
        // Backend returns PascalCase ("Seller", "Category") for Go structs
        val seller = o.optJSONObject("seller") ?: o.optJSONObject("Seller")
        val categoryObj = o.optJSONObject("category") ?: o.optJSONObject("Category")
        val parentObj = o.optJSONObject("parent_category") ?: o.optJSONObject("ParentCategory")
        val brandObj = o.optJSONObject("brand") ?: o.optJSONObject("Brand")
        val countryObj = o.optJSONObject("country") ?: o.optJSONObject("Country")
        val shipObj = o.optJSONObject("shipping_address") ?: o.optJSONObject("ShippingAddress")
        val imageUrlsArr = o.optJSONArray("image_urls") ?: o.optJSONArray("ImageURLs")
        val imageUrls = ListingImageUrlsWire.parseUrlStrings(imageUrlsArr)
        val coverUrl = ListingImageUrlsWire.resolveCoverUrl(
            o.optString("cover_image_url", "")
                .ifBlank { o.optString("CoverImageURL", "") },
            imageUrlsArr,
        )
        val tagsArr = o.optJSONArray("tags") ?: o.optJSONArray("Tags")
        val aestheticArr = o.optJSONArray("aesthetic_tags") ?: o.optJSONArray("AestheticTags")
        val brandIdWire = brandObj?.optString("id", "")?.ifBlank { null }
            ?: brandObj?.optString("ID", "")?.ifBlank { null }
        val brandName = brandObj?.optString("name", "")?.ifBlank { null }
            ?: brandObj?.optString("Name", "")?.ifBlank { null }
            ?: o.optString("brand", "").ifBlank { null }
            ?: o.optString("Brand", "").ifBlank { null }
        val parentCategoryName = parentObj?.optString("name", "")?.ifBlank { null }
            ?: parentObj?.optString("Name", "")?.ifBlank { null }
        val categoryIdWire = categoryObj?.optString("id", "")?.ifBlank { null }
            ?: categoryObj?.optString("ID", "")?.ifBlank { null }
        val parentCategoryIdWire = parentObj?.optString("id", "")?.ifBlank { null }
            ?: parentObj?.optString("ID", "")?.ifBlank { null }
        val aestheticRefs = parseAestheticTagRefs(aestheticArr)
        val shippingAddress = shipObj?.let { s ->
            val line1 = s.optString("line1", s.optString("Line1", ""))
            val label = s.optString("label", s.optString("Label", "")).ifBlank { null }
            if (line1.isBlank() && label.isNullOrBlank()) null
            else {
                ListingShippingAddress(
                    label = label,
                    line1 = line1,
                    line2 = s.optString("line2", s.optString("Line2", "")).ifBlank { null },
                    city = s.optString("city", s.optString("City", "")).ifBlank { null },
                    region = s.optString("region", s.optString("Region", "")).ifBlank { null },
                    postalCode = s.optString("postal_code", s.optString("PostalCode", "")).ifBlank { null },
                    countryCode = s.optString("country_code", s.optString("CountryCode", "")).ifBlank { null },
                )
            }
        }
        val parsedFloorPrice: Long? = when {
            o.has("floor_price") -> o.optLong("floor_price", 0L)
            o.has("FloorPrice") -> o.optLong("FloorPrice", 0L)
            else -> null
        }
        val nextDrop = o.optString("next_price_drop_at", o.optString("NextPriceDropAt", "")).trim()
        val listPriceVnd: Long? = sequenceOf(
            "list_price",
            "ListPrice",
            "compare_at_price",
            "CompareAtPrice",
            "original_price",
            "OriginalPrice",
            "msrp",
            "MSRP",
        ).mapNotNull { key ->
            if (!o.has(key)) return@mapNotNull null
            val v = o.optLong(key, 0L)
            v.takeIf { it > 0L }
        }.firstOrNull()
        val estimatedShippingVnd: Long? = sequenceOf(
            "estimated_shipping_fee",
            "EstimatedShippingFee",
            "shipping_fee_estimate",
            "ShippingFeeEstimate",
            "shipping_fee",
            "ShippingFee",
        ).mapNotNull { key ->
            if (!o.has(key)) return@mapNotNull null
            val v = o.optLong(key, 0L)
            v.takeIf { it > 0L }
        }.firstOrNull()
        return ListingDetail(
            id = o.optString("id", o.optString("ID", "")),
            title = o.optString("title", o.optString("Title", "")),
            description = o.optString("description", o.optString("Description", "")),
            imageUrls = imageUrls.ifEmpty { listOf(coverUrl).filter { it.isNotBlank() } },
            priceVnd = o.optLong("price", o.optLong("Price", 0L)),
            listPriceVnd = listPriceVnd,
            condition = o.optString("condition", o.optString("Condition", "")),
            category = categoryObj?.optString("name", "")?.ifBlank { null }
                ?: categoryObj?.optString("Name", "")?.ifBlank { null }
                ?: o.optString("category", "").ifBlank { null },
            categoryId = categoryIdWire,
            parentCategoryName = parentCategoryName,
            parentCategoryId = parentCategoryIdWire,
            size = o.optString("size", "").ifBlank { o.optString("Size", "").ifBlank { null } },
            brand = brandName,
            brandId = brandIdWire,
            material = o.optString("material", "").ifBlank { null },
            tags = parseTagStringArray(tagsArr),
            aestheticTags = aestheticRefs.map { it.label },
            aestheticTagRefs = aestheticRefs,
            likeCount = o.optInt("like_count", o.optInt("LikeCount", 0)),
            saveCount = o.optInt("save_count", o.optInt("SaveCount", 0)),
            viewCount = o.optInt("view_count", o.optInt("ViewCount", 0)),
            measurementUnit = o.optString("measurement_unit", o.optString("MeasurementUnit", "")).ifBlank { null },
            measurementHem = optMeasurement(o, "measurement_hem"),
            measurementChest = optMeasurement(o, "measurement_chest"),
            measurementLength = optMeasurement(o, "measurement_length"),
            measurementShoulders = optMeasurement(o, "measurement_shoulders"),
            measurementSleeveLength = optMeasurement(o, "measurement_sleeve_length"),
            acceptOffers = o.optBoolean("accept_offers", o.optBoolean("AcceptOffers", false)),
            autoPriceDropEnabled = o.optBoolean(
                "auto_price_drop_enabled",
                o.optBoolean("AutoPriceDropEnabled", false),
            ),
            floorPriceVnd = parsedFloorPrice,
            priceDropPercent = when {
                o.has("price_drop_percent") ->
                    o.optInt("price_drop_percent", o.optInt("PriceDropPercent", 0))
                o.has("PriceDropPercent") -> o.optInt("PriceDropPercent", 0)
                else -> null
            },
            nextPriceDropAtIso = nextDrop.ifBlank { null },
            countryName = countryObj?.optString("name", "")?.ifBlank { null }
                ?: countryObj?.optString("Name", "")?.ifBlank { null },
            countryId = countryObj?.optString("id", "")?.ifBlank { null }
                ?: countryObj?.optString("ID", "")?.ifBlank { null }
                ?: o.optString("country_id", o.optString("CountryID", "")).ifBlank { null },
            countryIso2 = countryObj?.optString("iso2", "")?.ifBlank { null }
                ?: countryObj?.optString("ISO2", "")?.ifBlank { null },
            shippingAddress = shippingAddress,
            estimatedShippingVnd = estimatedShippingVnd,
            sellerId = seller?.optString("user_id", "")?.ifBlank { null }
                ?: seller?.optString("UserID", "")?.ifBlank { null }
                ?: o.optString("seller_id", "").ifBlank { null }
                ?: o.optString("SellerID", "").ifBlank { null },
            sellerUsername = seller?.optString("username", "")?.ifBlank { null }
                ?: seller?.optString("Username", "")?.ifBlank { null }
                ?: o.optString("seller_username", "").ifBlank { null },
            sellerAvatarUrl = seller?.optString("avatar_url", "")?.ifBlank { null }
                ?: seller?.optString("AvatarURL", "")?.ifBlank { null }
                ?: seller?.optString("profile_image_url", "")?.ifBlank { null }
                ?: seller?.optString("ProfileImageURL", "")?.ifBlank { null },
            sellerDisplayName = seller?.optString("display_name", "")?.ifBlank { null }
                ?: seller?.optString("DisplayName", "")?.ifBlank { null },
            sellerVerified = seller?.let { s ->
                s.optBoolean("verified", false) || s.optBoolean("Verified", false)
            } ?: false,
            sellerListingCount = seller?.let { s ->
                when {
                    s.has("listing_count") -> s.optInt("listing_count", 0)
                    s.has("ListingCount") -> s.optInt("ListingCount", 0)
                    else -> null
                }
            },
            sellerFollowerCount = seller?.let { s ->
                when {
                    s.has("follower_count") -> s.optInt("follower_count", 0)
                    s.has("FollowerCount") -> s.optInt("FollowerCount", 0)
                    else -> null
                }
            },
            sellerFollowingCount = seller?.let { s ->
                when {
                    s.has("following_count") -> s.optInt("following_count", 0)
                    s.has("FollowingCount") -> s.optInt("FollowingCount", 0)
                    else -> null
                }
            },
            sellerAverageRating = seller?.let { s ->
                when {
                    s.has("average_rating") -> s.optDouble("average_rating", 0.0).toFloat()
                    s.has("AverageRating") -> s.optDouble("AverageRating", 0.0).toFloat()
                    else -> null
                }
            },
            createdAtIso = o.optString("created_at", o.optString("CreatedAt", "")).trim().ifBlank { null },
            updatedAtIso = o.optString("updated_at", o.optString("UpdatedAt", "")).trim().ifBlank { null },
            sellerIsFollowing = seller?.let { s ->
                when {
                    s.has("is_following") -> s.optBoolean("is_following", false)
                    s.has("IsFollowing") -> s.optBoolean("IsFollowing", false)
                    else -> null
                }
            },
            isLiked = when {
                o.has("is_liked") -> o.optBoolean("is_liked", false)
                o.has("IsLiked") -> o.optBoolean("IsLiked", false)
                else -> false
            },
            isSaved = when {
                o.has("is_saved") -> o.optBoolean("is_saved", false)
                o.has("IsSaved") -> o.optBoolean("IsSaved", false)
                else -> false
            },
            status = o.optString("status", o.optString("Status", "active")).lowercase().ifBlank { "active" },
            color = o.optString("color", o.optString("Color", "")).trim().lowercase().ifBlank { null },
            genderTarget = o.optString("gender_target", o.optString("GenderTarget", "")).trim().lowercase().ifBlank { null },
        )
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
        private const val TAG_CREATE_LISTING = "FashCreateListingApi"
    }
}

/** One step in `POST /listings` body `image_urls` (core-service JSONB array). */
data class ListingImageStepPayload(
    val stepKey: String,
    val label: String,
    val labelVi: String?,
    val sortOrder: Int,
    val required: Boolean,
    val imageUrl: String,
)

/** Core `NamedRef` / `BrandRef` JSON: `{ "id", "name" }` (see listing_request.CreateListingRequest). */
private fun namedRefToJson(ref: NamedRefPayload, maxNameLen: Int): JSONObject =
    JSONObject().apply {
        put("id", ref.id.trim())
        put("name", ref.name.trim().take(maxNameLen))
    }

private fun listingImageStepsToJsonArray(steps: List<ListingImageStepPayload>): JSONArray {
    val arr = JSONArray()
    for (s in steps) {
        val o = JSONObject()
        o.put("step_key", s.stepKey.trim())
        o.put("label", s.label.trim())
        s.labelVi?.trim()?.takeIf { it.isNotEmpty() }?.let { o.put("label_vi", it) }
        o.put("sort_order", s.sortOrder)
        o.put("required", s.required)
        o.put("image_url", s.imageUrl.trim())
        arr.put(o)
    }
    return arr
}

/** Core-service `NamedRef`: required `id` (UUID) + `name` (max length enforced when encoding). */
data class NamedRefPayload(
    val id: String,
    val name: String,
)

/** `POST /api/v1/listings` body — matches core `listing_request.CreateListingRequest` (`snake_case` on wire). */
data class CreateListingRequest(
    val title: String,
    val imageUrlSteps: List<ListingImageStepPayload>,
    val priceVnd: Long,
    val condition: String,
    /** Leaf category — required; JSON key `category`, not `category_id`. */
    val category: NamedRefPayload,
    val description: String = "",
    val size: String = "",
    /** Primary product colour in lowercase English (e.g. "black", "navy blue"). */
    val color: String? = null,
    /** Target gender: "women" | "men" | "unisex" | "kids" | "baby". */
    val genderTarget: String? = null,
    /** Optional parent category in hierarchy. */
    val parentCategory: NamedRefPayload? = null,
    /** Optional; same `{id,name}` shape as category (brand `name` max 255 on wire). */
    val brand: NamedRefPayload? = null,
    /** Optional; each `{ "id", "name" }` from catalog (not `aesthetic_tag_ids`). */
    val aestheticTags: List<NamedRefPayload> = emptyList(),
    val countryOfOrigin: String? = null,
    val countryId: String? = null,
    val countryName: String? = null,
    val measurementUnit: String? = null,
    val measurementHem: Double? = null,
    val measurementChest: Double? = null,
    val measurementLength: Double? = null,
    val measurementShoulders: Double? = null,
    val measurementSleeveLength: Double? = null,
    val acceptOffers: Boolean? = null,
    val autoPriceDropEnabled: Boolean? = null,
    val floorPriceVnd: Long? = null,
    val priceDropPercent: Int? = null,
    val shippingAddressId: String? = null,
)

/** Partial update for `PUT /listings/{id}`. */
data class UpdateListingRequest(
    val title: String? = null,
    val condition: String? = null,
    val priceVnd: Long? = null,
    val description: String? = null,
    val brandId: String? = null,
    val brandName: String? = null,
    val size: String? = null,
    /** Tag names (not ids); `[]` clears when explicitly replacing tags. */
    val aestheticTags: List<String>? = null,
    val acceptOffers: Boolean? = null,
    val autoPriceDropEnabled: Boolean? = null,
    val floorPriceVnd: Long? = null,
    val priceDropPercent: Int? = null,
    /** ISO-3166 alpha-2 (e.g. `VN`). */
    val countryOfOrigin: String? = null,
    val countryId: String? = null,
    val countryName: String? = null,
    val measurementUnit: String? = null,
    val measurementHem: Double? = null,
    val measurementChest: Double? = null,
    val measurementLength: Double? = null,
    val measurementShoulders: Double? = null,
    val measurementSleeveLength: Double? = null,
    val shippingAddressId: String? = null,
    /** Primary colour keyword; "" = clear. Null = no change. */
    val color: String? = null,
    /** Intended wearer: women | men | unisex | kids | baby | "" to clear. Null = no change. */
    val genderTarget: String? = null,
)

data class CreateListingResponse(val id: String)

data class Category(
    val id: String,
    val name: String,
    val slug: String,
)
