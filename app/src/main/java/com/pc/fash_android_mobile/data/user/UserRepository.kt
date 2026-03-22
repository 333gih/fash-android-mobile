package com.pc.fash_android_mobile.data.user

import android.net.Uri
import com.pc.fash_android_mobile.config.AppEnvironment
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * User/profile API client for onboarding and profile operations.
 * @param publicClient For unauthenticated calls (e.g. aesthetic-tags).
 * @param securedClient For authenticated calls (e.g. onboard); must inject Bearer token.
 */
class UserRepository(
    private val publicClient: OkHttpClient,
    private val securedClient: OkHttpClient,
) {

    companion object {
        /** core-service follow/unfollow expect `user_id` (UUID) in path. */
        private val USER_ID_UUID_REGEX =
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Encodes a user id or username for use in URL path segments (e.g. dots in username).
     * UUIDs are left as-is (safe in paths).
     */
    private fun encodePathSegment(segment: String): String =
        if (USER_ID_UUID_REGEX.matches(segment)) segment else Uri.encode(segment, null)

    /**
     * Resolves follow target to canonical `user_id` (UUID).
     * - If [raw] is already a UUID, returns it.
     * - Otherwise treats [raw] as username and loads public profile to get [ProfileInfo.userId].
     */
    private fun resolveFollowTargetUserId(raw: String): String {
        val t = raw.trim()
        if (t.isEmpty()) error("user id or username required")
        if (USER_ID_UUID_REGEX.matches(t)) return t
        val profile = getProfile(t).getOrElse { e -> throw e }
        return profile.userId.takeIf { it.isNotBlank() }
            ?: error("Could not resolve user id for follow")
    }

    fun getAestheticTags(): Result<List<AestheticTag>> = runCatching {
        val client = publicClient
        val url = AppEnvironment.apiPath("api/v1/aesthetic-tags")
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("User-Agent", "FashAndroid/1.0")
            .build()
        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            response.body?.string().orEmpty()
        }
        parseAestheticTags(body)
    }

    fun onboard(username: String, aestheticTags: List<String>): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/users/onboard")
        val json = JSONObject()
            .put("username", username)
            .put("aesthetic_tags", JSONArray(aestheticTags))
            .toString()
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(JSON_MEDIA))
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

    /**
     * Follow user (core-service: `POST /api/v1/users/:id/follow`).
     * Accepts UUID or username; returns canonical user id (UUID) on success.
     */
    fun follow(userIdOrUsername: String): Result<String> = runCatching {
        val targetId = resolveFollowTargetUserId(userIdOrUsername)
        val seg = encodePathSegment(targetId)
        val url = AppEnvironment.apiPath("api/v1/users/$seg/follow")
        securedClient.newCall(
            Request.Builder()
                .url(url)
                .post("{}".toRequestBody(JSON_MEDIA))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "FashAndroid/1.0")
                .build(),
        ).execute().use { response ->
            if (!response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                val msg = try { JSONObject(body).optString("error", body).ifBlank { body } } catch (_: Exception) { body }
                error("HTTP ${response.code}: $msg")
            }
        }
        targetId
    }

    /**
     * Unfollow user (core-service: `DELETE /api/v1/users/:id/follow`).
     * Accepts UUID or username; returns canonical user id (UUID) on success.
     */
    fun unfollow(userIdOrUsername: String): Result<String> = runCatching {
        val targetId = resolveFollowTargetUserId(userIdOrUsername)
        val seg = encodePathSegment(targetId)
        val url = AppEnvironment.apiPath("api/v1/users/$seg/follow")
        securedClient.newCall(
            Request.Builder()
                .url(url)
                .delete()
                .header("Accept", "application/json")
                .header("User-Agent", "FashAndroid/1.0")
                .build(),
        ).execute().use { response ->
            if (!response.isSuccessful && response.code != 404) {
                val body = response.body?.string().orEmpty()
                val msg = try { JSONObject(body).optString("error", body).ifBlank { body } } catch (_: Exception) { body }
                error("HTTP ${response.code}: $msg")
            }
        }
        targetId
    }

    fun searchUsers(query: String, limit: Int = 20): Result<List<UserSearchResult>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList<UserSearchResult>()
        val url = "${AppEnvironment.apiPath("api/v1/users/search")}?q=${java.net.URLEncoder.encode(query, "UTF-8")}&limit=$limit"
        val body = securedClient.newCall(
            Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .header("User-Agent", "FashAndroid/1.0")
                .build(),
        ).execute().use { response ->
            if (!response.isSuccessful) {
                val b = response.body?.string().orEmpty()
                val msg = try { JSONObject(b).optString("error", b).ifBlank { b } } catch (_: Exception) { b }
                error("HTTP ${response.code}: $msg")
            }
            response.body?.string().orEmpty()
        }
        parseUserSearchResults(body)
    }

    fun getProfile(userIdOrUsername: String): Result<ProfileInfo> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/users/${userIdOrUsername.trim()}")
        val body = publicClient.newCall(
            Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .header("User-Agent", "FashAndroid/1.0")
                .build(),
        ).execute().use { response ->
            if (response.code == 404) error("Profile not found")
            if (!response.isSuccessful) {
                val b = response.body?.string().orEmpty()
                val msg = try { JSONObject(b).optString("error", b).ifBlank { b } } catch (_: Exception) { b }
                error("HTTP ${response.code}: $msg")
            }
            response.body?.string().orEmpty()
        }
        val obj = JSONObject(body.trim())
        val profileJson = if (obj.has("data")) obj.getJSONObject("data").toString() else body
        parseProfileInfo(profileJson)
    }

    /** Checks if username is available. Returns true when available, false when taken. */
    fun checkUsername(username: String): Result<Boolean> = runCatching {
        val q = username.trim()
        if (q.isBlank()) return@runCatching false
        val url = "${AppEnvironment.apiPath("api/v1/users/check-username")}?q=${java.net.URLEncoder.encode(q, "UTF-8")}"
        val body = securedClient.newCall(
            Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .header("User-Agent", "FashAndroid/1.0")
                .build(),
        ).execute().use { response ->
            if (!response.isSuccessful) {
                val b = response.body?.string().orEmpty()
                val msg = try { JSONObject(b).optString("error", b).ifBlank { b } } catch (_: Exception) { b }
                error("HTTP ${response.code}: $msg")
            }
            response.body?.string().orEmpty()
        }
        val obj = JSONObject(body.trim())
        val inner = if (obj.has("data")) obj.getJSONObject("data") else obj
        inner.optBoolean("available", !inner.optBoolean("taken", false))
    }

    /** Updates current user's profile. Use null to leave a field unchanged. */
    fun updateProfile(
        displayName: String? = null,
        username: String? = null,
        bio: String? = null,
        avatarUrl: String? = null,
        coverImageUrl: String? = null,
        aestheticTags: List<String>? = null,
    ): Result<Unit> = runCatching {
        val json = JSONObject()
        displayName?.let { json.put("display_name", it) }
        username?.let { json.put("username", it) }
        bio?.let { json.put("bio", it) }
        avatarUrl?.let { json.put("avatar_url", it) }
        coverImageUrl?.let { json.put("cover_image_url", it) }
        aestheticTags?.let { json.put("aesthetic_tags", JSONArray(it)) }
        val body = json.toString()
        val url = AppEnvironment.apiPath("api/v1/users/me")
        val request = Request.Builder()
            .url(url)
            .patch(body.toRequestBody(JSON_MEDIA))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", "FashAndroid/1.0")
            .build()
        securedClient.newCall(request).execute().use { response ->
            val resBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val msg = try { JSONObject(resBody).optString("error", resBody).ifBlank { resBody } } catch (_: Exception) { resBody }
                error("HTTP ${response.code}: $msg")
            }
        }
    }

    /** Uploads profile avatar or cover image. Returns the image URL. */
    fun uploadProfileImage(
        bytes: ByteArray,
        filename: String = "image.jpg",
        type: String = "avatar",
    ): Result<String> = runCatching {
        val path = when (type.lowercase()) {
            "cover" -> "api/v1/users/me/cover"
            else -> "api/v1/users/me/avatar"
        }
        val url = AppEnvironment.apiPath(path)
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", filename, bytes.toRequestBody("image/*".toMediaType()))
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
        val obj = JSONObject(bodyStr.trim())
        when (type.lowercase()) {
            "cover" -> obj.optString("cover_image_url", obj.optString("image_url", "")).ifBlank { error("No cover_image_url in response") }
            else -> obj.optString("avatar_url", obj.optString("image_url", "")).ifBlank { error("No avatar_url in response") }
        }
    }

    private fun parseUserSearchResults(json: String): List<UserSearchResult> {
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
            UserSearchResult(
                userId = o.optString("user_id", o.optString("UserID", "")),
                username = o.optString("username", o.optString("Username", "")),
                displayName = o.optString("display_name", o.optString("DisplayName", "")),
                avatarUrl = o.optString("avatar_url", o.optString("AvatarURL", "")),
                followerCount = o.optInt("follower_count", o.optInt("FollowerCount", 0)),
                verified = o.optBoolean("verified", false),
            )
        }
    }

    private fun parseProfileInfo(json: String): ProfileInfo {
        val o = JSONObject(json.trim())
        val tagsArr = o.optJSONArray("aesthetic_tags") ?: o.optJSONArray("tags")
        val tagList = mutableListOf<String>()
        if (tagsArr != null) {
            for (i in 0 until tagsArr.length()) {
                val item = tagsArr.opt(i)
                when (item) {
                    is String -> tagList.add(item)
                    is JSONObject -> tagList.add(item.optString("name", item.optString("display_name", "")).ifBlank { item.optString("id", "") })
                    else -> { }
                }
            }
        }
        return ProfileInfo(
            userId = o.optString("user_id", o.optString("UserID", "")),
            username = o.optString("username", o.optString("Username", "")),
            displayName = o.optString("display_name", o.optString("DisplayName", "")),
            avatarUrl = o.optString("avatar_url", o.optString("AvatarURL", "")),
            followerCount = o.optInt("follower_count", o.optInt("FollowerCount", 0)),
            rating = o.optDouble("rating", -1.0).takeIf { it >= 0 }?.toFloat(),
            reviewCount = o.optInt("review_count", -1).takeIf { it >= 0 },
            isFollowing = o.optBoolean("is_following", false).takeIf { o.has("is_following") },
            bio = o.optString("bio", o.optString("Bio", "")),
            coverImageUrl = o.optString("cover_image_url", o.optString("coverImageUrl", "")),
            followingCount = o.optInt("following_count", o.optInt("FollowingCount", 0)),
            productCount = o.optInt("product_count", o.optInt("ProductCount", o.optInt("listing_count", 0))),
            soldCount = o.optInt("sold_count", o.optInt("SoldCount", 0)),
            aestheticTags = tagList,
            hasFastDelivery = o.optBoolean("has_fast_delivery", o.optBoolean("hasFastDelivery", false)),
        )
    }

    fun getMe(): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/users/me")
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("User-Agent", "FashAndroid/1.0")
            .build()
        val response = securedClient.newCall(request).execute()
        if (response.code == 404) error("Profile not found")
        if (!response.isSuccessful) error("HTTP ${response.code}")
        Unit
    }

    /**
     * Fetches current user's profile. Uses authenticated client.
     * Tries api/v1/users/me first (core-service with APP_API_PREFIX=/api), then v1/users/me if 404.
     */
    fun getMeProfile(): Result<ProfileInfo> = runCatching {
        fun doGet(url: String): String {
            val response = securedClient.newCall(
                Request.Builder()
                    .url(url)
                    .get()
                    .header("Accept", "application/json")
                    .header("User-Agent", "FashAndroid/1.0")
                    .build(),
            ).execute()
            val b = response.body?.string().orEmpty()
            if (response.code == 404) error("HTTP 404: $b")
            if (!response.isSuccessful) {
                val msg = try { JSONObject(b).optString("error", b).ifBlank { b } } catch (_: Exception) { b }
                error("HTTP ${response.code}: $msg")
            }
            return b
        }
        val body = try {
            doGet(AppEnvironment.apiPath("api/v1/users/me"))
        } catch (e: Exception) {
            if (e.message?.contains("404") == true) {
                doGet(AppEnvironment.apiPath("v1/users/me"))
            } else {
                throw e
            }
        }
        val obj = JSONObject(body.trim())
        val profileJson = if (obj.has("data")) obj.getJSONObject("data").toString() else body
        parseProfileInfo(profileJson)
    }

    private fun parseAestheticTags(json: String): List<AestheticTag> {
        val raw = json.trim()
        if (raw.isEmpty()) return emptyList()
        val arr = when {
            raw.startsWith("[") -> JSONArray(raw)
            else -> try {
                val obj = JSONObject(raw)
                if (obj.has("data")) obj.getJSONArray("data") else JSONArray("[]")
            } catch (_: Exception) { JSONArray("[]") }
        }
        val list = mutableListOf<AestheticTag>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                AestheticTag(
                    id = o.optString("id", o.optString("ID", "")),
                    name = o.optString("name", o.optString("Name", "")),
                    displayName = o.optString("display_name", o.optString("DisplayName", o.optString("name", o.optString("Name", "")))),
                    sortOrder = o.optInt("sort_order", o.optInt("SortOrder", 0)),
                ),
            )
        }
        return list
    }
}

data class AestheticTag(
    val id: String,
    val name: String,
    val displayName: String,
    val sortOrder: Int,
)

data class UserSearchResult(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val followerCount: Int,
    val verified: Boolean = false,
)

data class ProfileInfo(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val followerCount: Int,
    val rating: Float? = null,
    val reviewCount: Int? = null,
    val isFollowing: Boolean? = null,
    val bio: String = "",
    val coverImageUrl: String = "",
    val followingCount: Int = 0,
    val productCount: Int = 0,
    val soldCount: Int = 0,
    val aestheticTags: List<String> = emptyList(),
    val hasFastDelivery: Boolean = false,
)
