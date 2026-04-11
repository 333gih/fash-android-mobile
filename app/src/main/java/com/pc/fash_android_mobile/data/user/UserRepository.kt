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
import java.util.Locale
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

    /**
     * `PUT /users/me/aesthetic-tags` — sets user's aesthetic tag selections (ids + names).
     */
    fun putUserAestheticTags(tags: List<AestheticTagPutItem>): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/users/me/aesthetic-tags")
        val arr = JSONArray()
        tags.forEach { t ->
            arr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("name", t.name)
                },
            )
        }
        val json = JSONObject().put("aesthetic_tags", arr).toString()
        securedClient.newCall(
            Request.Builder()
                .url(url)
                .put(json.toRequestBody(JSON_MEDIA))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "FashAndroid/1.0")
                .build(),
        ).execute().use { response ->
            val resBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val msg = try {
                    JSONObject(resBody).optString("error", resBody).ifBlank { resBody }
                } catch (_: Exception) {
                    resBody
                }
                error("HTTP ${response.code}: $msg")
            }
        }
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
     * `PUT /api/v1/users/me/password` — set first password (omit [currentPassword]) or change password.
     * [newPassword] length 8–72 per API. Errors: `INVALID_CURRENT_PASSWORD`, `CURRENT_PASSWORD_REQUIRED`.
     */
    fun putUserPassword(newPassword: String, currentPassword: String?): Result<Unit> = runCatching {
        require(newPassword.length in 8..72) { "PASSWORD_LENGTH" }
        val url = AppEnvironment.apiPath("api/v1/users/me/password")
        val json = JSONObject().put("new_password", newPassword)
        val cur = currentPassword?.trim().orEmpty()
        if (cur.isNotEmpty()) {
            json.put("current_password", cur)
        }
        securedClient.newCall(
            Request.Builder()
                .url(url)
                .put(json.toString().toRequestBody(JSON_MEDIA))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "FashAndroid/1.0")
                .build(),
        ).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val obj = runCatching { JSONObject(body) }.getOrNull()
                val code = obj?.optString("code")?.trim().orEmpty()
                    .ifBlank { obj?.optString("error_code")?.trim().orEmpty() }
                val err = obj?.optString("error")?.trim().orEmpty()
                    .ifBlank { obj?.optString("message")?.trim().orEmpty() }
                    .ifBlank { body }
                val codeOrErr = "$code $err"
                when {
                    code.equals("INVALID_CURRENT_PASSWORD", ignoreCase = true) ||
                        err.contains("INVALID_CURRENT_PASSWORD", ignoreCase = true) ||
                        codeOrErr.contains("INVALID_CURRENT_PASSWORD", ignoreCase = true) ->
                        error("INVALID_CURRENT_PASSWORD")
                    code.equals("CURRENT_PASSWORD_REQUIRED", ignoreCase = true) ||
                        err.contains("CURRENT_PASSWORD_REQUIRED", ignoreCase = true) ||
                        codeOrErr.contains("CURRENT_PASSWORD_REQUIRED", ignoreCase = true) ->
                        error("CURRENT_PASSWORD_REQUIRED")
                    else -> error("HTTP ${response.code}: $err")
                }
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
    /** `POST /users/{id}/block` */
    fun blockUser(userIdOrUsername: String): Result<Unit> = runCatching {
        val targetId = resolveFollowTargetUserId(userIdOrUsername)
        val seg = encodePathSegment(targetId)
        val url = AppEnvironment.apiPath("api/v1/users/$seg/block")
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
    }

    /** `DELETE /users/{id}/block` */
    fun unblockUser(userIdOrUsername: String): Result<Unit> = runCatching {
        val targetId = resolveFollowTargetUserId(userIdOrUsername)
        val seg = encodePathSegment(targetId)
        val url = AppEnvironment.apiPath("api/v1/users/$seg/block")
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
    }

    /** `GET /users/suggested-username?phone=` */
    fun getSuggestedUsername(phone: String): Result<String> = runCatching {
        val q = phone.trim()
        if (q.isBlank()) error("phone required")
        val url = "${AppEnvironment.apiPath("api/v1/users/suggested-username")}?phone=${Uri.encode(q)}"
        val body = publicClient.newCall(
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
        val root = if (obj.has("data")) obj.getJSONObject("data") else obj
        root.optString("suggested_username", "").ifBlank { error("No suggested_username") }
    }

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

    /**
     * Authenticated user’s following list (`GET /api/v1/users/me/following`).
     * Server envelope: `{ "items": [...], "limit", "offset", "total" }`.
     */
    fun getMyFollowing(limit: Int = 20, offset: Int = 0): Result<FollowListPage> = runCatching {
        getMyFollowList(pathSegment = "following", limit = limit, offset = offset)
    }

    /**
     * Users who follow the authenticated user (`GET /api/v1/users/me/followers`).
     * Same envelope as [getMyFollowing].
     */
    fun getMyFollowers(limit: Int = 20, offset: Int = 0): Result<FollowListPage> = runCatching {
        getMyFollowList(pathSegment = "followers", limit = limit, offset = offset)
    }

    private fun getMyFollowList(pathSegment: String, limit: Int, offset: Int): FollowListPage {
        val capped = limit.coerceIn(1, 100)
        val url = "${AppEnvironment.apiPath("api/v1/users/me/$pathSegment")}?limit=$capped&offset=${offset.coerceAtLeast(0)}"
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
        return parseFollowListEnvelope(body, requestLimit = capped, requestOffset = offset.coerceAtLeast(0))
    }

    /**
     * Parses paginated follow/followers JSON (`items` + `total`, or legacy arrays / `data` / `users`).
     */
    private fun parseFollowListEnvelope(body: String, requestLimit: Int, requestOffset: Int): FollowListPage {
        val raw = body.trim()
        if (raw.isEmpty()) {
            return FollowListPage(items = emptyList(), total = 0, limit = requestLimit, offset = requestOffset)
        }
        if (raw.startsWith("[")) {
            val items = parseUserSearchResults(raw)
            return FollowListPage(
                items = items,
                total = items.size,
                limit = requestLimit,
                offset = requestOffset,
            )
        }
        val obj = try {
            JSONObject(raw)
        } catch (_: Exception) {
            return FollowListPage(items = emptyList(), total = 0, limit = requestLimit, offset = requestOffset)
        }
        val items = mutableListOf<UserSearchResult>()
        for (k in listOf("items", "data", "users", "following", "followers")) {
            val a = obj.optJSONArray(k)
            if (a != null) {
                items.addAll(parseUserSearchResults(a.toString()))
                break
            }
        }
        val total = obj.optInt("total", items.size)
        val lim = obj.optInt("limit", requestLimit).takeIf { it > 0 } ?: requestLimit
        val off = obj.optInt("offset", requestOffset)
        return FollowListPage(items = items, total = total, limit = lim, offset = off)
    }

    /** Parses a user list from home-feed-style wrappers or a raw array. */
    private fun parseUserListResponse(body: String): List<UserSearchResult> {
        val raw = body.trim()
        if (raw.isEmpty()) return emptyList()
        if (raw.startsWith("[")) return parseUserSearchResults(raw)
        val obj = try {
            JSONObject(raw)
        } catch (_: Exception) {
            return emptyList()
        }
        for (k in listOf("data", "users", "following", "followers", "items")) {
            val a = obj.optJSONArray(k)
            if (a != null) return parseUserSearchResults(a.toString())
        }
        return emptyList()
    }

    /**
     * Public profile (`GET /api/v1/users/{id|username}`).
     * Tries [securedClient] first so an authenticated viewer receives viewer-specific fields (e.g. `is_following`);
     * falls back to [publicClient] on 401/403 (guest / expired token).
     */
    fun getProfile(userIdOrUsername: String): Result<ProfileInfo> = runCatching {
        val raw = userIdOrUsername.trim().removePrefix("@")
        if (raw.isBlank()) error("Profile id required")
        val seg = encodePathSegment(raw)
        val url = AppEnvironment.apiPath("api/v1/users/$seg")
        val body = fetchProfileBody(url)
        val obj = JSONObject(body.trim())
        val profileJson = if (obj.has("data")) obj.getJSONObject("data").toString() else body
        parseProfileInfo(profileJson)
    }

    private fun fetchProfileBody(url: String): String {
        val req = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("User-Agent", "FashAndroid/1.0")
            .build()
        securedClient.newCall(req).execute().use { response ->
            val body = response.body?.string().orEmpty()
            when (response.code) {
                200 -> return body
                401, 403 -> { /* guest — try public */ }
                404 -> error("Profile not found")
                else -> {
                    val msg = try {
                        JSONObject(body).optString("error", body).ifBlank { body }
                    } catch (_: Exception) {
                        body
                    }
                    error("HTTP ${response.code}: $msg")
                }
            }
        }
        return publicClient.newCall(req).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (response.code == 404) error("Profile not found")
            if (!response.isSuccessful) {
                val msg = try {
                    JSONObject(body).optString("error", body).ifBlank { body }
                } catch (_: Exception) {
                    body
                }
                error("HTTP ${response.code}: $msg")
            }
            body
        }
    }

    /**
     * Seller shop focus (`GET /api/v1/users/{username|uuid}/seller-focus`).
     * Requires Bearer; [securedClient] only. 403 → [SellerFocusForbiddenException];
     * 401 → [SellerFocusUnauthorizedException].
     */
    fun getSellerListingFocus(userIdOrUsername: String): Result<SellerListingFocus> = runCatching {
        val raw = userIdOrUsername.trim().removePrefix("@")
        if (raw.isBlank()) error("Profile id required")
        val seg = encodePathSegment(raw)
        val url = AppEnvironment.apiPath("api/v1/users/$seg/seller-focus")
        val req = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("User-Agent", "FashAndroid/1.0")
            .build()
        securedClient.newCall(req).execute().use { response ->
            val body = response.body?.string().orEmpty()
            when (response.code) {
                200 -> {
                    val obj = JSONObject(body.trim())
                    val inner = if (obj.has("data")) obj.getJSONObject("data").toString() else body
                    parseSellerListingFocus(inner)
                }
                403 -> throw SellerFocusForbiddenException()
                401 -> throw SellerFocusUnauthorizedException()
                else -> {
                    val msg = try {
                        JSONObject(body).optString("error", body).ifBlank { body }
                    } catch (_: Exception) {
                        body
                    }
                    error("HTTP ${response.code}: $msg")
                }
            }
        }
    }

    private fun parseSellerListingFocus(json: String): SellerListingFocus {
        val obj = JSONObject(json)
        val categories = mutableListOf<SellerFocusCategory>()
        obj.optJSONArray("categories")?.let { arr ->
            for (i in 0 until arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                val id = c.optString("id", "").trim()
                val name = c.optString("name", "").trim()
                if (id.isEmpty() && name.isEmpty()) continue
                val parentId = c.optString("parent_id").trim().takeIf { it.isNotEmpty() }
                val parentName = c.optString("parent_name").trim().takeIf { it.isNotEmpty() }
                categories.add(
                    SellerFocusCategory(
                        id = id,
                        name = name.ifEmpty { "—" },
                        parentId = parentId,
                        parentName = parentName,
                    ),
                )
            }
        }
        val brands = mutableListOf<SellerFocusBrand>()
        obj.optJSONArray("brands")?.let { arr ->
            for (i in 0 until arr.length()) {
                val b = arr.optJSONObject(i) ?: continue
                val id = b.optString("id", "").trim()
                val name = b.optString("name", "").trim()
                if (id.isEmpty() && name.isEmpty()) continue
                brands.add(SellerFocusBrand(id = id, name = name.ifEmpty { "—" }))
            }
        }
        val tags = mutableListOf<SellerFocusTag>()
        obj.optJSONArray("aesthetic_tags")?.let { arr ->
            for (i in 0 until arr.length()) {
                val t = arr.optJSONObject(i) ?: continue
                val id = t.optString("id", "").trim()
                val name = t.optString("name", "").trim()
                if (id.isEmpty() && name.isEmpty()) continue
                tags.add(SellerFocusTag(id = id, name = name.ifEmpty { "—" }))
            }
        }
        return SellerListingFocus(
            categories = categories,
            brands = brands,
            aestheticTags = tags,
        )
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

    /**
     * `PUT /users/me/sizing-reference` — saves reference size, unit, optional body measurements;
     * marks sizing reference complete (required before home feed).
     */
    fun saveSizingReference(request: SizingReferenceRequest): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/users/me/sizing-reference")
        val json = JSONObject().apply {
            put("reference_size", request.referenceSize.trim())
            put("reference_measurement_unit", request.referenceMeasurementUnit.trim().lowercase())
            put("reference_measurement_chest", request.referenceMeasurementChest)
            put("reference_measurement_hem", request.referenceMeasurementHem)
            put("reference_measurement_length", request.referenceMeasurementLength)
            put("reference_measurement_shoulders", request.referenceMeasurementShoulders)
            put("reference_measurement_sleeve_length", request.referenceMeasurementSleeveLength)
        }.toString()
        securedClient.newCall(
            Request.Builder()
                .url(url)
                .put(json.toRequestBody(JSON_MEDIA))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "FashAndroid/1.0")
                .build(),
        ).execute().use { response ->
            val resBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val msg = try {
                    JSONObject(resBody).optString("error", resBody).ifBlank { resBody }
                } catch (_: Exception) {
                    resBody
                }
                error("HTTP ${response.code}: $msg")
            }
        }
    }

    /**
     * PATCH `/users/me` — only non-null fields are sent.
     * [ProfilePatch.aestheticTags] `null` = omit key (leave unchanged); empty list = clear.
     */
    fun updateProfile(patch: ProfilePatch): Result<Unit> = runCatching {
        val json = JSONObject()
        patch.displayName?.let { json.put("display_name", it) }
        patch.username?.let { json.put("username", it) }
        patch.bio?.let { json.put("bio", it) }
        patch.avatarUrl?.let { json.put("avatar_url", it) }
        patch.coverImageUrl?.let {
            json.put("cover_image_url", it)
            json.put("cover_url", it)
        }
        patch.aestheticTags?.let { list ->
            val arr = JSONArray()
            list.forEach { t ->
                arr.put(
                    JSONObject().apply {
                        put("id", t.id)
                        put("name", t.name)
                    },
                )
            }
            json.put("aesthetic_tags", arr)
        }
        patch.referenceSize?.let { json.put("reference_size", it) }
        patch.referenceMeasurementUnit?.let { u ->
            json.put("reference_measurement_unit", u.trim().lowercase(Locale.ROOT))
        }
        patch.referenceMeasurementChest?.let { json.put("reference_measurement_chest", it) }
        patch.referenceMeasurementHem?.let { json.put("reference_measurement_hem", it) }
        patch.referenceMeasurementLength?.let { json.put("reference_measurement_length", it) }
        patch.referenceMeasurementShoulders?.let { json.put("reference_measurement_shoulders", it) }
        patch.referenceMeasurementSleeveLength?.let { json.put("reference_measurement_sleeve_length", it) }
        if (json.length() == 0) return@runCatching Unit
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
        mimeType: String = "image/jpeg",
    ): Result<String> = runCatching {
        val path = when (type.lowercase()) {
            "cover" -> "api/v1/users/me/cover"
            else -> "api/v1/users/me/avatar"
        }
        val url = AppEnvironment.apiPath(path)
        val safeMime = mimeType.takeIf { it.contains('/') && !it.contains('*') } ?: "image/jpeg"
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", filename, bytes.toRequestBody(safeMime.toMediaType()))
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
                verified = o.optBoolean("verified", o.optBoolean("Verified", false)),
                followingCount = o.optInt("following_count", o.optInt("FollowingCount", 0)),
                listingCount = o.optInt("listing_count", o.optInt("ListingCount", 0)),
                followedAtIso = o.optString("followed_at", o.optString("FollowedAt", "")).trim().takeIf { it.isNotEmpty() },
                coverUrl = o.optString("cover_url", o.optString("CoverURL", "")),
            )
        }
    }

    private fun parseProfileInfo(json: String): ProfileInfo {
        val o = JSONObject(json.trim())
        val tagsArr = o.optJSONArray("aesthetic_tags") ?: o.optJSONArray("tags")
        val tagList = mutableListOf<String>()
        val snapshotList = mutableListOf<AestheticTagPutItem>()
        if (tagsArr != null) {
            for (i in 0 until tagsArr.length()) {
                val item = tagsArr.opt(i)
                when (item) {
                    is String -> tagList.add(item)
                    is JSONObject -> {
                        val id = item.optString("id", "").trim()
                        val name = item.optString("name", item.optString("display_name", "")).trim()
                        val label = name.ifBlank { item.optString("id", "") }
                        tagList.add(label)
                        if (id.isNotEmpty()) {
                            snapshotList.add(AestheticTagPutItem(id, name.ifBlank { id }))
                        }
                    }
                    else -> { }
                }
            }
        }
        fun optDoubleIfPresent(key: String): Double? {
            if (!o.has(key)) return null
            val v = o.optDouble(key, Double.NaN)
            return if (v.isNaN()) null else v
        }
        val ratingVal = listOf(
            o.optDouble("rating", -1.0),
            o.optDouble("Rating", -1.0),
            o.optDouble("average_rating", -1.0),
            o.optDouble("AverageRating", -1.0),
        ).firstOrNull { it >= 0 }?.toFloat()
        return ProfileInfo(
            userId = o.optString("user_id", o.optString("UserID", "")),
            username = o.optString("username", o.optString("Username", "")),
            displayName = o.optString("display_name", o.optString("DisplayName", "")),
            avatarUrl = o.optString("avatar_url", o.optString("AvatarURL", "")),
            followerCount = o.optInt("follower_count", o.optInt("FollowerCount", 0)),
            rating = ratingVal,
            reviewCount = o.optInt("review_count", -1).takeIf { it >= 0 },
            isFollowing = when {
                o.has("is_following") -> o.optBoolean("is_following", false)
                o.has("IsFollowing") -> o.optBoolean("IsFollowing", false)
                else -> null
            },
            bio = o.optString("bio", o.optString("Bio", "")),
            coverImageUrl = o.optString("cover_image_url", o.optString("coverImageUrl", o.optString("CoverURL", ""))),
            followingCount = o.optInt("following_count", o.optInt("FollowingCount", 0)),
            productCount = o.optInt(
                "product_count",
                o.optInt("ProductCount", o.optInt("listing_count", o.optInt("ListingCount", 0))),
            ),
            soldCount = o.optInt("sold_count", o.optInt("SoldCount", 0)),
            aestheticTags = tagList,
            aestheticTagSnapshots = snapshotList,
            referenceSize = o.optString("reference_size", "").trim().takeIf { it.isNotEmpty() },
            referenceMeasurementUnit = o.optString("reference_measurement_unit", "").trim().takeIf { it.isNotEmpty() },
            referenceMeasurementChest = optDoubleIfPresent("reference_measurement_chest"),
            referenceMeasurementHem = optDoubleIfPresent("reference_measurement_hem"),
            referenceMeasurementLength = optDoubleIfPresent("reference_measurement_length"),
            referenceMeasurementShoulders = optDoubleIfPresent("reference_measurement_shoulders"),
            referenceMeasurementSleeveLength = optDoubleIfPresent("reference_measurement_sleeve_length"),
            hasFastDelivery = o.optBoolean("has_fast_delivery", o.optBoolean("hasFastDelivery", false)),
            reputationPoints = when {
                o.has("reputation_points") -> o.optInt("reputation_points", 0)
                o.has("ReputationPoints") -> o.optInt("ReputationPoints", 0)
                o.has("reputationPoints") -> o.optInt("reputationPoints", 0)
                else -> null
            },
            meetingNoShowWarning = o.optBoolean(
                "meeting_no_show_warning",
                o.optBoolean("MeetingNoShowWarning", false),
            ),
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
     * Secured GET — onboarding/home gate (path from [AppEnvironment.userAccessStatusPath], e.g. `.../setup-status`).
     * Errors: JSON `{ "code": <http>, "error": "<message>" }` (typical core-service shape).
     */
    /**
     * `POST /users/me/meeting-trust/ack-identity-reverify` — call after the user completes out-of-band KYC /
     * identity re-verification; clears `meeting_scheduling_reverify_required` and `meeting_scheduling_suspended_until`
     * when the server accepts.
     */
    fun ackMeetingIdentityReverify(): Result<Unit> = runCatching {
        val url = AppEnvironment.apiPath("api/v1/users/me/meeting-trust/ack-identity-reverify")
        securedClient.newCall(
            Request.Builder()
                .url(url)
                .post("{}".toRequestBody(JSON_MEDIA))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "FashAndroid/1.0")
                .build(),
        ).execute().use { response ->
            val resBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val msg = try {
                    JSONObject(resBody).optString("error", resBody).ifBlank { resBody }
                } catch (_: Exception) {
                    resBody
                }
                error("HTTP ${response.code}: $msg")
            }
        }
    }

    fun getUserAccessStatus(): Result<UserAccessStatus> = runCatching {
        val path = AppEnvironment.userAccessStatusPath.trim().trimStart('/')
        val url = AppEnvironment.apiPath(path)
        val body = securedClient.newCall(
            Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .header("User-Agent", "FashAndroid/1.0")
                .build(),
        ).execute().use { response ->
            val b = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val msg = try {
                    JSONObject(b).optString("error", b).ifBlank { b }
                } catch (_: Exception) {
                    b
                }
                error("HTTP ${response.code}: $msg")
            }
            b
        }
        parseUserAccessStatus(body)
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

    private fun parseUserAccessStatus(json: String): UserAccessStatus {
        val o = JSONObject(json.trim())
        val root = when {
            o.has("data") && o.get("data") is JSONObject -> o.getJSONObject("data")
            else -> o
        }
        val serverGate = parseServerCanAccessHome(root)
        val passwordSet = when {
            root.has("password_set") -> root.getBoolean("password_set")
            root.has("passwordSet") -> root.getBoolean("passwordSet")
            else -> null
        }
        val isChangePassword = when {
            root.has("is_change_password") -> root.getBoolean("is_change_password")
            root.has("isChangePassword") -> root.getBoolean("isChangePassword")
            else -> null
        }
        val reverifyRequired = parseJsonBoolean(root, "meeting_scheduling_reverify_required")
            ?: parseJsonBoolean(root, "meetingSchedulingReverifyRequired")
            ?: false
        val suspendedUntil = root.optString("meeting_scheduling_suspended_until", "")
            .ifBlank { root.optString("meetingSchedulingSuspendedUntil", "") }
            .trim()
            .takeIf { it.isNotEmpty() }
        return UserAccessStatus(
            hasProfile = root.optBoolean("has_profile", root.optBoolean("hasProfile", false)),
            aestheticTagsConfigured = root.optBoolean(
                "aesthetic_tags_configured",
                root.optBoolean("aestheticTagsConfigured", false),
            ),
            onboardingDone = root.optBoolean("onboarding_done", root.optBoolean("onboardingDone", false)),
            sizingReferenceCompleted = root.optBoolean(
                "sizing_reference_completed",
                root.optBoolean("sizingReferenceCompleted", false),
            ),
            serverCanAccessHome = serverGate,
            nextStep = root.optString("next_step", root.optString("nextStep", "")).trim().takeIf { it.isNotEmpty() },
            passwordSet = passwordSet,
            isChangePassword = isChangePassword,
            meetingSchedulingReverifyRequired = reverifyRequired,
            meetingSchedulingSuspendedUntil = suspendedUntil,
        )
    }

    /**
     * Wire may send booleans as JSON true/false, strings (`"true"`), or numbers; [JSONObject.optBoolean] only handles JSON booleans.
     */
    private fun parseServerCanAccessHome(root: JSONObject): Boolean? {
        val key = when {
            root.has("can_access_home") -> "can_access_home"
            root.has("canAccessHome") -> "canAccessHome"
            else -> return null
        }
        return parseJsonBoolean(root, key)
    }

    private fun parseJsonBoolean(obj: JSONObject, key: String): Boolean? {
        if (!obj.has(key)) return null
        val v = obj.get(key)
        if (v == null || v === JSONObject.NULL) return null
        return when (v) {
            is Boolean -> v
            is String -> v.trim().equals("true", ignoreCase = true) || v == "1"
            is Number -> v.toDouble() != 0.0
            else -> null
        }
    }
}

/**
 * Core `GET …/access-status` (see [UserRepository.getUserAccessStatus]).
 * When [serverCanAccessHome] is set from JSON, it is authoritative for [canAccessHome] (matches server `can_access_home`).
 */
data class UserAccessStatus(
    val hasProfile: Boolean,
    val aestheticTagsConfigured: Boolean,
    val onboardingDone: Boolean,
    val sizingReferenceCompleted: Boolean,
    /** If present in JSON (`can_access_home`), overrides the four-flag AND for home access. */
    val serverCanAccessHome: Boolean? = null,
    /** e.g. `password`, `onboard`, `sizing_reference`, `none` — from setup-status. */
    val nextStep: String? = null,
    /** `true` when `password_set_at` is set; `null` if JSON omitted (legacy clients assume no password gate). */
    val passwordSet: Boolean? = null,
    /** `true` when first-time password step still needed (same signal as `!password_set` when set). */
    val isChangePassword: Boolean? = null,
    /** Meetup scheduling blocked until identity is re-verified (e.g. 3rd no-show in 30 days). */
    val meetingSchedulingReverifyRequired: Boolean = false,
    /** RFC3339 / server ISO timestamp until meetup scheduling is suspended, if any. */
    val meetingSchedulingSuspendedUntil: String? = null,
) {
    /** First-time password step: after username ([onboardingDone]), before home. */
    fun needsPasswordSetup(): Boolean {
        if (!onboardingDone) return false
        if (passwordSet == true) return false
        if (passwordSet == false) return true
        if (isChangePassword == true) return true
        if (nextStep?.trim()?.equals("password", ignoreCase = true) == true) return true
        return false
    }

    /**
     * Prefer [serverCanAccessHome] when the API sends `can_access_home` (authoritative).
     * If that key is absent but `next_step` is `none` and core profile steps are done, treat as home
     * (server may omit `can_access_home` or send `aesthetic_tags_configured: false` while still allowing home).
     * Otherwise require all four flags (legacy client-side gate).
     * [needsPasswordSetup] blocks home until first password is set when API reports it.
     */
    val canAccessHome: Boolean
        get() {
            if (needsPasswordSetup()) return false
            serverCanAccessHome?.let { return it }
            if (nextStep?.equals("none", ignoreCase = true) == true &&
                hasProfile && onboardingDone && sizingReferenceCompleted
            ) {
                return true
            }
            return hasProfile && aestheticTagsConfigured && onboardingDone && sizingReferenceCompleted
        }
}

data class AestheticTagPutItem(
    val id: String,
    val name: String,
)

/**
 * PATCH `/users/me` body. Only non-null fields are serialized.
 * [aestheticTags] `null` = omit (unchanged); `emptyList()` = clear.
 */
data class ProfilePatch(
    val displayName: String? = null,
    val username: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val coverImageUrl: String? = null,
    val aestheticTags: List<AestheticTagPutItem>? = null,
    val referenceSize: String? = null,
    val referenceMeasurementUnit: String? = null,
    val referenceMeasurementChest: Double? = null,
    val referenceMeasurementHem: Double? = null,
    val referenceMeasurementLength: Double? = null,
    val referenceMeasurementShoulders: Double? = null,
    val referenceMeasurementSleeveLength: Double? = null,
) {
    fun isEmpty(): Boolean =
        displayName == null && username == null && bio == null && avatarUrl == null && coverImageUrl == null &&
            aestheticTags == null &&
            referenceSize == null && referenceMeasurementUnit == null &&
            referenceMeasurementChest == null && referenceMeasurementHem == null &&
            referenceMeasurementLength == null && referenceMeasurementShoulders == null &&
            referenceMeasurementSleeveLength == null
}

data class SizingReferenceRequest(
    val referenceSize: String,
    val referenceMeasurementUnit: String,
    val referenceMeasurementChest: Double = 0.0,
    val referenceMeasurementHem: Double = 0.0,
    val referenceMeasurementLength: Double = 0.0,
    val referenceMeasurementShoulders: Double = 0.0,
    val referenceMeasurementSleeveLength: Double = 0.0,
)

data class AestheticTag(
    val id: String,
    val name: String,
    val displayName: String,
    val sortOrder: Int,
)

/** One row from user search or from `GET …/users/me/following` / `…/followers` `items[]`. */
data class UserSearchResult(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val followerCount: Int,
    val verified: Boolean = false,
    val followingCount: Int = 0,
    val listingCount: Int = 0,
    val followedAtIso: String? = null,
    val coverUrl: String = "",
)

/** Paginated envelope for [getMyFollowing] / [getMyFollowers]. */
data class FollowListPage(
    val items: List<UserSearchResult>,
    val total: Int,
    val limit: Int,
    val offset: Int,
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
    /** id+name from API when tags are objects — used for PATCH round-trip. */
    val aestheticTagSnapshots: List<AestheticTagPutItem> = emptyList(),
    val referenceSize: String? = null,
    val referenceMeasurementUnit: String? = null,
    val referenceMeasurementChest: Double? = null,
    val referenceMeasurementHem: Double? = null,
    val referenceMeasurementLength: Double? = null,
    val referenceMeasurementShoulders: Double? = null,
    val referenceMeasurementSleeveLength: Double? = null,
    val hasFastDelivery: Boolean = false,
    /** Trust / gamification when API sends it. */
    val reputationPoints: Int? = null,
    /** Public warning flag from core-service (meetup reliability). */
    val meetingNoShowWarning: Boolean = false,
)
