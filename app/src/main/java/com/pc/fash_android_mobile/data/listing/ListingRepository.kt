package com.pc.fash_android_mobile.data.listing

import android.net.Uri
import com.pc.fash_android_mobile.config.AppEnvironment
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

/**
 * Listing/feed API client. Uses secured client for authenticated endpoints.
 * Token is injected via SecuredApiClient interceptor.
 */
class ListingRepository(
    private val securedClient: OkHttpClient,
) {

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

    fun getListingDetail(listingId: String): Result<ListingDetail> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/listings/$listingId")
        parseListingDetail(executeGet(url))
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
        parseFeedResponse(executeGet(primary))
    }

    /** `GET /listings/wishlist` → listing id list. */
    fun getWishlistListingIds(limit: Int = 50, offset: Int = 0): Result<List<String>> = runCatching {
        val url = "${AppEnvironment.apiPath("api/v1/listings/wishlist")}?limit=$limit&offset=$offset"
        val body = executeGet(url)
        val obj = JSONObject(body.trim())
        val root = if (obj.has("data")) obj.getJSONObject("data") else obj
        val arr = root.optJSONArray("listing_ids") ?: JSONArray("[]")
        (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
    }

    /** `PUT /listings/{id}` — category cannot change (omit). */
    fun updateListing(listingId: String, update: UpdateListingRequest): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/listings/${listingId.trim()}")
        val json = JSONObject()
        update.title?.let { json.put("title", it) }
        update.condition?.let { json.put("condition", it) }
        update.priceVnd?.let { json.put("price", it) }
        update.description?.let { json.put("description", it) }
        update.brand?.let { json.put("brand", it) }
        update.size?.let { json.put("size", it) }
        update.aestheticTags?.let { json.put("aesthetic_tags", JSONArray(it)) }
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
        val obj = JSONObject(body)
        obj.optBoolean("liked", false)
    }

    fun toggleSave(listingId: String): Result<Boolean> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/listings/$listingId/save")
        val body = executePost(url)
        val obj = JSONObject(body)
        obj.optBoolean("saved", false)
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
        json.put("image_urls", JSONArray(request.imageUrls))
        json.put("price", request.priceVnd)
        json.put("condition", request.condition)
        json.put("category_id", request.categoryId)
        if (request.description.isNotBlank()) json.put("description", request.description)
        if (request.size.isNotBlank()) json.put("size", request.size)
        request.parentCategoryId?.takeIf { it.isNotBlank() }?.let { json.put("parent_category_id", it) }
        request.parentCategoryName?.takeIf { it.isNotBlank() }?.let { json.put("parent_category_name", it) }
        request.categoryName?.takeIf { it.isNotBlank() }?.let { json.put("category_name", it) }
        request.brandId?.takeIf { it.isNotBlank() }?.let { json.put("brand_id", it) }
        request.brandName?.takeIf { it.isNotBlank() }?.let { json.put("brand_name", it) }
        when {
            request.aestheticTagIds.isNotEmpty() ->
                json.put("aesthetic_tag_ids", JSONArray(request.aestheticTagIds))
            request.aestheticTagNames.isNotEmpty() ->
                json.put("aesthetic_tags", JSONArray(request.aestheticTagNames))
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
        val body = executePostJson(url, json.toString())
        val o = JSONObject(body)
        val dataObj = if (o.has("data")) o.optJSONObject("data") else null
        val id = (dataObj ?: o).let { obj ->
            obj.optString("ID", "").ifBlank { obj.optString("id", "") }
        }
        CreateListingResponse(id = id.ifBlank { error("No id in response") })
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
                throwHttpError(response.code, body)
            }
            body
        }
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

    private fun parseFeedResponse(json: String): List<ListingFeedItem> =
        ListingFeedJsonParser.parseFeedArray(json)

    private fun parseStringArray(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
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
        val imageUrls = parseStringArray(
            o.optJSONArray("image_urls") ?: o.optJSONArray("ImageURLs"),
        )
        val coverUrl = o.optString("cover_image_url", "")
            .ifBlank { o.optString("CoverImageURL", "") }
            .ifBlank { imageUrls.firstOrNull() ?: "" }
        val tagsArr = o.optJSONArray("tags")
            ?: o.optJSONArray("Tags")
            ?: o.optJSONArray("aesthetic_tags")
            ?: o.optJSONArray("AestheticTags")
        return ListingDetail(
            id = o.optString("id", o.optString("ID", "")),
            title = o.optString("title", o.optString("Title", "")),
            description = o.optString("description", o.optString("Description", "")),
            imageUrls = imageUrls.ifEmpty { listOf(coverUrl).filter { it.isNotBlank() } },
            priceVnd = o.optLong("price", o.optLong("Price", 0L)),
            condition = o.optString("condition", o.optString("Condition", "")),
            // Resolve category name from nested Category object or flat field
            category = categoryObj?.optString("name", "")?.ifBlank { null }
                ?: categoryObj?.optString("Name", "")?.ifBlank { null }
                ?: o.optString("category", "").ifBlank { null },
            size = o.optString("size", "").ifBlank { o.optString("Size", "").ifBlank { null } },
            brand = o.optString("brand", "").ifBlank { o.optString("Brand", "").ifBlank { null } },
            material = o.optString("material", "").ifBlank { null },
            tags = parseStringArray(tagsArr),
            likeCount = o.optInt("like_count", o.optInt("LikeCount", 0)),
            saveCount = o.optInt("save_count", o.optInt("SaveCount", 0)),
            // Prefer Seller.UserID (auth user) then top-level SellerID
            sellerId = seller?.optString("user_id", "")?.ifBlank { null }
                ?: seller?.optString("UserID", "")?.ifBlank { null }
                ?: o.optString("seller_id", "").ifBlank { null }
                ?: o.optString("SellerID", "").ifBlank { null },
            sellerUsername = seller?.optString("username", "")?.ifBlank { null }
                ?: seller?.optString("Username", "")?.ifBlank { null }
                ?: o.optString("seller_username", "").ifBlank { null },
            sellerAvatarUrl = seller?.optString("avatar_url", "")?.ifBlank { null }
                ?: seller?.optString("AvatarURL", "")?.ifBlank { null },
            sellerDisplayName = seller?.optString("display_name", "")?.ifBlank { null }
                ?: seller?.optString("DisplayName", "")?.ifBlank { null },
            isLiked = o.optBoolean("is_liked", false),
            isSaved = o.optBoolean("is_saved", false),
            status = o.optString("status", o.optString("Status", "active")).lowercase().ifBlank { "active" },
        )
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}

/** `POST /api/v1/listings` body — align with listings API doc (`snake_case` on wire). */
data class CreateListingRequest(
    val title: String,
    val imageUrls: List<String>,
    val priceVnd: Long,
    val condition: String,
    val categoryId: String,
    val description: String = "",
    val size: String = "",
    val parentCategoryId: String? = null,
    val parentCategoryName: String? = null,
    val categoryName: String? = null,
    val brandId: String? = null,
    val brandName: String? = null,
    /** Preferred when non-empty; else [aestheticTagNames]. */
    val aestheticTagIds: List<String> = emptyList(),
    val aestheticTagNames: List<String> = emptyList(),
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
    val brand: String? = null,
    val size: String? = null,
    /** Tag names (not ids); `[]` clears when explicitly replacing tags. */
    val aestheticTags: List<String>? = null,
)

data class CreateListingResponse(val id: String)

data class Category(
    val id: String,
    val name: String,
    val slug: String,
)
