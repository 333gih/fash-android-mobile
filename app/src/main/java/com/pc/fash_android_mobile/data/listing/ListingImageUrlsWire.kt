package com.pc.fash_android_mobile.data.listing

import org.json.JSONArray

/**
 * Parses `image_urls` from core-service: legacy `string[]` or JSON array of step objects
 * (see [LISTING_IMAGE_URLS_STEPS_CHANGELOG.md]).
 */
object ListingImageUrlsWire {

    private data class Row(val sortOrder: Int, val index: Int, val url: String)

    /**
     * URLs ordered by [sort_order] ascending (stable by original index), matching cover derivation
     * (smallest [sort_order] with a non-empty URL is the primary slot).
     */
    fun parseUrlStrings(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val rows = mutableListOf<Row>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i)
            if (o != null) {
                val url = o.optString("image_url", "")
                    .ifBlank { o.optString("ImageURL", "") }
                    .trim()
                if (url.isEmpty()) continue
                val order = when {
                    o.has("sort_order") && !o.isNull("sort_order") -> o.optInt("sort_order", i)
                    o.has("SortOrder") && !o.isNull("SortOrder") -> o.optInt("SortOrder", i)
                    else -> i
                }
                rows.add(Row(sortOrder = order, index = i, url = url))
            } else {
                val s = arr.optString(i, "").trim()
                if (s.isNotBlank() && !s.startsWith("{")) {
                    rows.add(Row(sortOrder = i, index = i, url = s))
                }
            }
        }
        return rows
            .sortedWith(compareBy<Row> { it.sortOrder }.thenBy { it.index })
            .map { it.url }
    }

    /** Prefer explicit listing cover; else first URL using the same ordering as [parseUrlStrings]. */
    fun resolveCoverUrl(coverFromRoot: String, arr: JSONArray?): String {
        val root = coverFromRoot.trim()
        if (root.isNotEmpty()) return root
        return parseUrlStrings(arr).firstOrNull().orEmpty()
    }
}
