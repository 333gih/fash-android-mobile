package com.pc.fash_android_mobile.data.listing

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

    fun getListingsBySeller(
        sellerId: String,
        status: String? = null,
        limit: Int = 50,
        offset: Int = 0,
    ): Result<List<ListingFeedItem>> = runCatching {
        val q = mutableListOf<String>()
        q.add("limit=$limit")
        q.add("offset=$offset")
        q.add("seller_id=$sellerId")
        status?.takeIf { it.isNotBlank() }?.let { q.add("status=${java.net.URLEncoder.encode(it, "UTF-8")}") }
        val url = AppEnvironment.apiPath("api/v1/listings/explore") + "?" + q.joinToString("&")
        parseFeedResponse(executeGet(url))
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
            Category(
                id = o.optString("id", o.optString("ID", "")),
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
        uploadListingImage(file.readBytes(), file.name)

    fun uploadListingImage(bytes: ByteArray, filename: String = "image.jpg"): Result<String> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/listings/images")
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                filename,
                bytes.toRequestBody("image/*".toMediaType()),
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
        val id = when {
            o.has("data") -> o.getJSONObject("data").optString("id", "").ifBlank { o.optString("id", "") }
            else -> o.optString("id", "")
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

    private fun parseFeedResponse(json: String): List<ListingFeedItem> {
        val raw = json.trim()
        val arr = when {
            raw.startsWith("[") -> JSONArray(raw)
            else -> try {
                val obj = JSONObject(raw)
                if (obj.has("data")) obj.getJSONArray("data") else JSONArray("[]")
            } catch (_: Exception) { JSONArray("[]") }
        }
        val list = mutableListOf<ListingFeedItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val seller = o.optJSONObject("seller")
            val firstTag = seller?.optJSONArray("aesthetic_tags")?.optJSONObject(0)
            list.add(
                ListingFeedItem(
                    id = o.optString("id", o.optString("ID", "")),
                    title = o.optString("title", o.optString("Title", "")),
                    coverImageUrl = (o.optString("cover_image_url", "")
                        .ifBlank { o.optString("CoverImageURL", "") })
                        .ifBlank { o.optJSONArray("image_urls")?.optString(0) ?: o.optJSONArray("ImageURLs")?.optString(0) ?: "" },
                    imageUrls = parseStringArray(o.optJSONArray("image_urls")),
                    priceVnd = o.optLong("price", o.optLong("Price", 0L)),
                    condition = o.optString("condition", o.optString("Condition", "")),
                    likeCount = o.optInt("like_count", o.optInt("LikeCount", 0)),
                    saveCount = o.optInt("save_count", o.optInt("SaveCount", 0)),
                    sellerId = o.optString("seller_id", "").ifBlank { null }
                        ?: o.optString("SellerID", "").ifBlank { null }
                        ?: seller?.optString("user_id", "")?.ifBlank { null },
                    sellerUsername = (seller?.optString("username", "")?.takeIf { it.isNotBlank() }
                        ?: o.optString("seller_id", "").take(8).let { if (it.isNotBlank()) "user_$it" else "user" }),
                    sellerAvatarUrl = seller?.optString("avatar_url", "")?.ifBlank { null },
                    sellerStyleTag = firstTag?.let { it.optString("name", it.optString("display_name", "")) }?.ifBlank { null },
                    createdAt = o.optString("created_at", "")?.ifBlank { null },
                )
            )
        }
        return list
    }

    private fun parseStringArray(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
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
        val seller = o.optJSONObject("seller")
        val imageUrls = parseStringArray(o.optJSONArray("image_urls"))
        val coverUrl = o.optString("cover_image_url", "").ifBlank {
            o.optString("CoverImageURL", "").ifBlank { imageUrls.firstOrNull() ?: "" }
        }
        val tagsArr = o.optJSONArray("tags")
        val tags = parseStringArray(tagsArr)
        return ListingDetail(
            id = o.optString("id", o.optString("ID", "")),
            title = o.optString("title", o.optString("Title", "")),
            description = o.optString("description", o.optString("Description", "")),
            imageUrls = imageUrls.ifEmpty { listOf(coverUrl).filter { it.isNotBlank() } },
            priceVnd = o.optLong("price", o.optLong("Price", 0L)),
            condition = o.optString("condition", o.optString("Condition", "")),
            category = o.optString("category", "").ifBlank { null },
            size = o.optString("size", "").ifBlank { null },
            brand = o.optString("brand", "").ifBlank { null },
            material = o.optString("material", "").ifBlank { null },
            tags = tags,
            likeCount = o.optInt("like_count", o.optInt("LikeCount", 0)),
            saveCount = o.optInt("save_count", o.optInt("SaveCount", 0)),
            sellerId = seller?.optString("user_id", "")?.ifBlank { null }
                ?: o.optString("seller_id", "").ifBlank { null },
            sellerUsername = seller?.optString("username", "")?.ifBlank { null }
                ?: o.optString("seller_username", "").ifBlank { null },
            sellerAvatarUrl = seller?.optString("avatar_url", "")?.ifBlank { null },
            sellerDisplayName = seller?.optString("display_name", "")?.ifBlank { null },
            isLiked = o.optBoolean("is_liked", false),
            isSaved = o.optBoolean("is_saved", false),
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

data class CreateListingResponse(val id: String)

data class Category(
    val id: String,
    val name: String,
    val slug: String,
)
