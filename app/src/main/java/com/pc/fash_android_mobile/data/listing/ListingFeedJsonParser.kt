package com.pc.fash_android_mobile.data.listing

import org.json.JSONArray
import org.json.JSONObject

/** Shared parser for listing arrays (home, explore, search, seller listings). */
internal object ListingFeedJsonParser {

    fun parseFeedArray(json: String): List<ListingFeedItem> {
        val raw = json.trim()
        if (raw.isEmpty() || raw == "null") return emptyList()
        val arr: JSONArray = when {
            raw.startsWith("[") -> try {
                JSONArray(raw)
            } catch (_: Exception) {
                JSONArray("[]")
            }
            else -> try {
                val obj = JSONObject(raw)
                when {
                    obj.has("data") -> when (val d = obj.get("data")) {
                        is JSONArray -> d
                        is JSONObject -> extractListingsArray(d)
                        else -> JSONArray("[]")
                    }
                    else -> extractListingsArray(obj)
                }
            } catch (_: Exception) {
                JSONArray("[]")
            }
        }
        val list = mutableListOf<ListingFeedItem>()
        for (i in 0 until arr.length()) {
            val o = try {
                arr.optJSONObject(i) ?: continue
            } catch (_: Exception) {
                continue
            }
            // Backend returns PascalCase ("Seller") for Go structs; snake_case for older endpoints
            val seller = o.optJSONObject("seller") ?: o.optJSONObject("Seller")
            val tagsNode = seller?.optJSONArray("aesthetic_tags") ?: seller?.optJSONArray("AestheticTags")
            val firstTag = tagsNode?.optJSONObject(0)
            val listingAestheticArr = o.optJSONArray("aesthetic_tags") ?: o.optJSONArray("AestheticTags")
            val firstListingAesthetic = listingAestheticArr?.optJSONObject(0)?.let { tag ->
                tag.optString("display_name", "")
                    .ifBlank { tag.optString("DisplayName", "") }
                    .ifBlank { tag.optString("name", "") }
                    .ifBlank { tag.optString("Name", "") }
            }?.ifBlank { null }
            val categoryObj = o.optJSONObject("category") ?: o.optJSONObject("Category")
            val categoryName = categoryObj?.optString("name", "")?.ifBlank { null }
                ?: categoryObj?.optString("Name", "")?.ifBlank { null }
                ?: o.optString("category_name", "").ifBlank { null }
                ?: o.optString("CategoryName", "").ifBlank { null }
            val imageUrlsArr = o.optJSONArray("image_urls") ?: o.optJSONArray("ImageURLs")
            val listingStatusWire = o.optString("status", "")
                .ifBlank { o.optString("Status", "") }
                .ifBlank { null }
            list.add(
                ListingFeedItem(
                    id = o.optString("id", o.optString("ID", "")),
                    title = o.optString("title", o.optString("Title", "")),
                    coverImageUrl = ListingImageUrlsWire.resolveCoverUrl(
                        o.optString("cover_image_url", "")
                            .ifBlank { o.optString("CoverImageURL", "") },
                        imageUrlsArr,
                    ),
                    imageUrls = ListingImageUrlsWire.parseUrlStrings(imageUrlsArr),
                    priceVnd = o.optLong("price", o.optLong("Price", 0L)),
                    brand = o.optString("brand", "")
                        .ifBlank { o.optString("Brand", "") }
                        .takeIf { it.isNotBlank() }
                        ?: o.optJSONObject("brand")?.optString("name", "")?.takeIf { it.isNotBlank() }
                        ?: o.optJSONObject("Brand")?.optString("name", "")?.takeIf { it.isNotBlank() },
                    size = o.optString("size", "")
                        .ifBlank { o.optString("Size", "") }
                        .ifBlank { null },
                    categoryName = categoryName,
                    listingAestheticTag = firstListingAesthetic,
                    condition = o.optString("condition", o.optString("Condition", "")),
                    likeCount = o.optInt("like_count", o.optInt("LikeCount", 0)),
                    saveCount = o.optInt("save_count", o.optInt("SaveCount", 0)),
                    // Prefer Seller.UserID (the auth user's UUID) then top-level SellerID
                    sellerId = seller?.optString("user_id", "")?.ifBlank { null }
                        ?: seller?.optString("UserID", "")?.ifBlank { null }
                        ?: o.optString("seller_id", "").ifBlank { null }
                        ?: o.optString("SellerID", "").ifBlank { null },
                    sellerUsername = seller?.optString("username", "")?.takeIf { it.isNotBlank() }
                        ?: seller?.optString("Username", "")?.takeIf { it.isNotBlank() }
                        ?: "user",
                    sellerAvatarUrl = seller?.optString("avatar_url", "")?.ifBlank { null }
                        ?: seller?.optString("AvatarURL", "")?.ifBlank { null },
                    sellerStyleTag = firstTag?.let {
                        it.optString("name", "")
                            .ifBlank { it.optString("display_name", "") }
                            .ifBlank { it.optString("Name", "") }
                    }?.ifBlank { null },
                    createdAt = o.optString("created_at", "")
                        .ifBlank { o.optString("CreatedAt", "") }
                        .ifBlank { null },
                    isLiked = listingWireBool(o, "is_liked", "IsLiked"),
                    isSaved = listingWireBool(o, "is_saved", "IsSaved"),
                    sellerIsFollowing = sellerFollowingWireBool(seller),
                    listingStatus = listingStatusWire,
                ),
            )
        }
        return list
    }

    private fun listingWireBool(o: JSONObject, snake: String, pascal: String): Boolean = when {
        o.has(snake) -> o.optBoolean(snake, false)
        o.has(pascal) -> o.optBoolean(pascal, false)
        else -> false
    }

    private fun sellerFollowingWireBool(seller: JSONObject?): Boolean {
        if (seller == null) return false
        return when {
            seller.has("is_following") -> seller.optBoolean("is_following", false)
            seller.has("IsFollowing") -> seller.optBoolean("IsFollowing", false)
            else -> false
        }
    }

    /** When `data` is an object, find the first JSONArray of listing-like objects. */
    private fun extractListingsArray(data: JSONObject): JSONArray {
        val keys = listOf(
            "listings", "Listings", "items", "Items", "results", "Results", "rows", "Rows",
        )
        for (k in keys) {
            if (data.has(k)) {
                when (val v = data.opt(k)) {
                    is JSONArray -> if (v.length() > 0) return v
                    else -> Unit
                }
            }
        }
        return JSONArray("[]")
    }
}
