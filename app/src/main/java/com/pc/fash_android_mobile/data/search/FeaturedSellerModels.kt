package com.pc.fash_android_mobile.data.search

import com.pc.fash_android_mobile.data.user.UserSearchResult
import org.json.JSONArray
import org.json.JSONObject

/**
 * One row from `GET /api/v1/search/featured-sellers` (ranked sellers + preview listing UUIDs).
 */
data class FeaturedSellerItem(
    val userId: String,
    val username: String,
    val displayName: String,
    val bio: String,
    val avatarUrl: String,
    val followerCount: Int,
    val listingCount: Int,
    /** Null when the backend sends no rating yet. */
    val averageRating: Float?,
    val verified: Boolean,
    /** Up to 3 active listing UUIDs for horizontal previews. */
    val previewListingIds: List<String>,
)

fun FeaturedSellerItem.isShopReady(): Boolean =
    username.isNotBlank() && listingCount > 0 && previewListingIds.isNotEmpty()

fun List<FeaturedSellerItem>.shopReadyOnly(): List<FeaturedSellerItem> =
    filter { it.isShopReady() }

fun FeaturedSellerItem.toUserSearchResult(): UserSearchResult =
    UserSearchResult(
        userId = userId,
        username = username,
        displayName = displayName,
        avatarUrl = avatarUrl,
        followerCount = followerCount,
        verified = verified,
        followingCount = 0,
        listingCount = listingCount,
        followedAtIso = null,
        coverUrl = "",
    )

/**
 * Wraps `GET /search/featured-sellers` so the see-all screen can drive pagination.
 *
 * Server now returns `{items, total}` (see-all needs `total`); legacy clients that look for an array still parse
 * via [parseFeaturedSellersResponse] which falls back to the array form when present.
 */
data class FeaturedSellersPage(
    val items: List<FeaturedSellerItem>,
    val total: Int,
)

internal fun parseFeaturedSellersPage(json: String): FeaturedSellersPage {
    val items = parseFeaturedSellersResponse(json)
    val raw = json.trim()
    val total = if (raw.startsWith("{")) {
        try {
            val root = JSONObject(raw)
            pageTotalFromRoot(root, items.size)
        } catch (_: Exception) {
            items.size
        }
    } else {
        items.size
    }
    return FeaturedSellersPage(items = items, total = total)
}

private fun pageTotalFromRoot(root: JSONObject, itemCount: Int): Int {
    root.optInt("total", -1).takeIf { it >= 0 }?.let { return it }
    root.optInt("Total", -1).takeIf { it >= 0 }?.let { return it }
    val data = root.optJSONObject("data") ?: root.optJSONObject("Data")
    if (data != null) {
        data.optInt("total", -1).takeIf { it >= 0 }?.let { return it }
        data.optInt("Total", -1).takeIf { it >= 0 }?.let { return it }
    }
    return itemCount
}

internal fun parseFeaturedSellersResponse(json: String): List<FeaturedSellerItem> {
    val raw = json.trim()
    if (raw.isEmpty()) return emptyList()

    val arr: JSONArray = when {
        raw.startsWith("[") -> try {
            JSONArray(raw)
        } catch (_: Exception) {
            JSONArray("[]")
        }
        else -> try {
            resolveFeaturedSellerItemsArray(JSONObject(raw))
        } catch (_: Exception) {
            JSONArray("[]")
        }
    }

    return (0 until arr.length()).mapNotNull { i ->
        val o = arr.optJSONObject(i) ?: return@mapNotNull null
        val previewArr = o.optJSONArray("preview_listing_ids")
            ?: o.optJSONArray("PreviewListingIDs")
            ?: o.optJSONArray("previewListingIds")
            ?: JSONArray("[]")
        val previewIds = (0 until previewArr.length()).map { j ->
            previewArr.optString(j, "").trim()
        }.filter { it.isNotBlank() }.take(3)

        val avgRating = o.optNullableFloat("average_rating", "AverageRating")

        val userId = o.optStringOrNumber("user_id", "UserID", "userId", "userID")

        FeaturedSellerItem(
            userId = userId,
            username = o.optStringOrNumber("username", "Username"),
            displayName = o.optString("display_name", o.optString("DisplayName", "")).trim(),
            bio = o.optString("bio", o.optString("Bio", "")).trim(),
            avatarUrl = o.optString("avatar_url", o.optString("AvatarURL", "")).trim(),
            followerCount = o.optInt("follower_count", o.optInt("FollowerCount", 0)),
            listingCount = o.optInt("listing_count", o.optInt("ListingCount", 0)),
            averageRating = avgRating,
            verified = o.optBoolean("verified", o.optBoolean("Verified", false)),
            previewListingIds = previewIds,
        ).takeIf { it.userId.isNotBlank() || it.username.isNotBlank() }
    }
}

private fun resolveFeaturedSellerItemsArray(root: JSONObject): JSONArray {
    val itemKeys = listOf(
        "items",
        "Items",
        "featured_sellers",
        "featuredSellers",
        "sellers",
        "results",
        "users",
        "data",
    )
    for (k in itemKeys) {
        root.optJSONArray(k)?.let { return it }
    }
    val data = root.optJSONObject("data") ?: root.optJSONObject("Data")
    if (data != null) {
        for (k in itemKeys) {
            if (k == "data") continue
            data.optJSONArray(k)?.let { return it }
        }
    }
    return JSONArray("[]")
}

private fun JSONObject.optStringOrNumber(vararg keys: String): String {
    for (key in keys) {
        if (!has(key) || isNull(key)) continue
        val asString = optString(key, "").trim()
        if (asString.isNotEmpty()) return asString
        when (val raw = opt(key)) {
            is Number -> return raw.toString().trim()
        }
    }
    return ""
}

private fun JSONObject.optNullableFloat(vararg keys: String): Float? {
    for (k in keys) {
        if (!has(k) || isNull(k)) continue
        val v = optDouble(k, Double.NaN)
        if (!v.isNaN()) return v.toFloat()
    }
    return null
}
