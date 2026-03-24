package com.pc.fash_android_mobile.data.listing

import org.json.JSONArray
import org.json.JSONObject

/** Shared parser for listing arrays (home, explore, search, seller listings). */
internal object ListingFeedJsonParser {

    fun parseFeedArray(json: String): List<ListingFeedItem> {
        val raw = json.trim()
        val arr = when {
            raw.startsWith("[") -> JSONArray(raw)
            else -> try {
                val obj = JSONObject(raw)
                if (obj.has("data")) obj.getJSONArray("data") else JSONArray("[]")
            } catch (_: Exception) {
                JSONArray("[]")
            }
        }
        val list = mutableListOf<ListingFeedItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            // Backend returns PascalCase ("Seller") for Go structs; snake_case for older endpoints
            val seller = o.optJSONObject("seller") ?: o.optJSONObject("Seller")
            val tagsNode = seller?.optJSONArray("aesthetic_tags") ?: seller?.optJSONArray("AestheticTags")
            val firstTag = tagsNode?.optJSONObject(0)
            val imageUrlsArr = o.optJSONArray("image_urls") ?: o.optJSONArray("ImageURLs")
            list.add(
                ListingFeedItem(
                    id = o.optString("id", o.optString("ID", "")),
                    title = o.optString("title", o.optString("Title", "")),
                    coverImageUrl = o.optString("cover_image_url", "")
                        .ifBlank { o.optString("CoverImageURL", "") }
                        .ifBlank { imageUrlsArr?.optString(0) ?: "" },
                    imageUrls = parseStringArray(imageUrlsArr),
                    priceVnd = o.optLong("price", o.optLong("Price", 0L)),
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
                ),
            )
        }
        return list
    }

    private fun parseStringArray(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
    }
}
