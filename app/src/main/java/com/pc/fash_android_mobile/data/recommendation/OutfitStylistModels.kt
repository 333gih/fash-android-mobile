package com.pc.fash_android_mobile.data.recommendation

import org.json.JSONArray
import org.json.JSONObject

data class OutfitSetItem(
    val listingId: String,
    val slotRole: String,
    val coverImageUrl: String = "",
    val title: String = "",
    val priceVnd: Long = 0L,
    val hasExploreBoost: Boolean = false,
    val hasRealBadge: Boolean = false,
)

data class OutfitSetCard(
    val id: String,
    val title: String,
    val reasonLabel: String = "",
    val items: List<OutfitSetItem> = emptyList(),
)

object OutfitStylistJsonParser {
    fun parseSetsArray(arr: JSONArray?): List<OutfitSetCard> {
        if (arr == null || arr.length() == 0) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                parseSet(o)?.let { add(it) }
            }
        }
    }

    fun parseSet(obj: JSONObject?): OutfitSetCard? {
        if (obj == null) return null
        val id = obj.optString("id").trim()
        if (id.isEmpty()) return null
        val titleVi = obj.optString("title_vi").trim()
        val titleEn = obj.optString("title_en").trim()
        val title = titleVi.ifEmpty { titleEn }.ifEmpty { obj.optString("title").trim() }
        val itemsArr = obj.optJSONArray("items")
        val items = buildList {
            if (itemsArr != null) {
                for (j in 0 until itemsArr.length()) {
                    val it = itemsArr.optJSONObject(j) ?: continue
                    val listingId = it.optString("listing_id").trim()
                    if (listingId.isEmpty()) continue
                    add(
                        OutfitSetItem(
                            listingId = listingId,
                            slotRole = it.optString("slot_role"),
                            coverImageUrl = it.optString("cover_image_url"),
                            title = it.optString("title"),
                            priceVnd = it.optLong("price"),
                            hasExploreBoost = it.optBoolean("has_explore_boost"),
                            hasRealBadge = it.optBoolean("has_real_badge"),
                        ),
                    )
                }
            }
        }
        return OutfitSetCard(
            id = id,
            title = title,
            reasonLabel = obj.optString("reason_label"),
            items = items,
        )
    }

    fun parseCompleteTheLookResponse(body: String): OutfitSetCard? {
        val root = JSONObject(body)
        val data = root.optJSONObject("data") ?: root
        val setObj = data.optJSONObject("set") ?: return null
        return parseSet(setObj)
    }
}
