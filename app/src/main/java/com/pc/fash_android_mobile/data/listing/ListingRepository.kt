package com.pc.fash_android_mobile.data.listing

import android.net.Uri
import com.pc.fash_android_mobile.config.AppEnvironment
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

    private fun encodeUserPathSegment(segment: String): String =
        if (userIdUuidRegex.matches(segment)) segment else Uri.encode(segment, null)

    fun getHomeFeed(limit: Int = 20, offset: Int = 0): Result<List<ListingFeedItem>> = runCatching {
        val url = "${AppEnvironment.apiPath("api/v1/listings/home")}?limit=$limit&offset=$offset"
        parseFeedResponse(executeGet(url))
    }

    fun getExploreFeed(
        limit: Int = 20,
        offset: Int = 0,
        categoryId: String? = null,
        tags: String? = null,
        sellerId: String? = null,
        minPrice: Long? = null,
        maxPrice: Long? = null,
        condition: String? = null,
    ): Result<List<ListingFeedItem>> = runCatching {
        val q = mutableListOf<String>()
        q.add("limit=$limit")
        q.add("offset=$offset")
        categoryId?.let { q.add("category_id=$it") }
        tags?.let { q.add("tags=${java.net.URLEncoder.encode(it, "UTF-8")}") }
        sellerId?.let { q.add("seller_id=$it") }
        minPrice?.let { q.add("min_price=$it") }
        maxPrice?.let { q.add("max_price=$it") }
        condition?.let { q.add("condition=$it") }
        val url = AppEnvironment.apiPath("api/v1/listings/explore") + "?" + q.joinToString("&")
        parseFeedResponse(executeGet(url))
    }

    fun getListingDetail(listingId: String): Result<ListingDetail> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/listings/$listingId")
        parseListingDetail(executeGet(url))
    }

    /**
     * Core-service: `GET /users/{id}/listings`. Falls back to explore `seller_id` if needed.
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
        try {
            parseFeedResponse(executeGet(primary))
        } catch (_: Exception) {
            val fb = mutableListOf<String>()
            fb.add("limit=$limit")
            fb.add("offset=$offset")
            fb.add("seller_id=$sellerId")
            status?.takeIf { it.isNotBlank() }?.let { fb.add("status=${java.net.URLEncoder.encode(it, "UTF-8")}") }
            val url = AppEnvironment.apiPath("api/v1/listings/explore") + "?" + fb.joinToString("&")
            parseFeedResponse(executeGet(url))
        }
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
                val msg = try { JSONObject(b).optString("error", b).ifBlank { b } } catch (_: Exception) { b }
                error("HTTP ${response.code}: $msg")
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
                val msg = try { JSONObject(b).optString("error", b).ifBlank { b } } catch (_: Exception) { b }
                error("HTTP ${response.code}: $msg")
            }
            b
        }
        JSONObject(bodyStr).optString("image_url", "").ifBlank { error("No image_url in response") }
    }

    fun createListing(request: CreateListingRequest): Result<CreateListingResponse> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/listings")
        val json = JSONObject()
            .put("title", request.title)
            .put("image_urls", JSONArray(request.imageUrls))
            .put("price", request.priceVnd)
            .put("condition", request.condition)
            .put("category_id", request.categoryId)
            .put("description", request.description.ifBlank { JSONObject.NULL })
            .put("size", request.size.ifBlank { JSONObject.NULL })
            .put("brand", request.brand.ifBlank { JSONObject.NULL })
            .put("aesthetic_tags", JSONArray(request.aestheticTags))
            .toString()
        val body = executePostJson(url, json)
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
                val msg = try { JSONObject(body).optString("error", body).ifBlank { body } } catch (_: Exception) { body }
                error("HTTP ${response.code}: $msg")
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
                val msg = try { JSONObject(body).optString("error", body).ifBlank { body } } catch (_: Exception) { body }
                error("HTTP ${response.code}: $msg")
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
                val msg = try { JSONObject(body).optString("error", body).ifBlank { body } } catch (_: Exception) { body }
                error("HTTP ${response.code}: $msg")
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
                val msg = try { JSONObject(body).optString("error", body).ifBlank { body } } catch (_: Exception) { body }
                error("HTTP ${response.code}: $msg")
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

data class CreateListingRequest(
    val title: String,
    val imageUrls: List<String>,
    val priceVnd: Long,
    val condition: String,
    val categoryId: String,
    val description: String = "",
    val size: String = "",
    val brand: String = "",
    val aestheticTags: List<String> = emptyList(),
)

/** Partial update for `PUT /listings/{id}`. */
data class UpdateListingRequest(
    val title: String? = null,
    val condition: String? = null,
    val priceVnd: Long? = null,
    val description: String? = null,
    val brand: String? = null,
    val size: String? = null,
    val aestheticTags: List<String>? = null,
)

data class CreateListingResponse(val id: String)

data class Category(
    val id: String,
    val name: String,
    val slug: String,
)
