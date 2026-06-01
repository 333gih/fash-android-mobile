package com.pc.fash_android_mobile.data.listing

import org.json.JSONArray

/**
 * Parses `image_urls` from core-service: legacy `string[]` or JSON array of step objects
 * (see [LISTING_IMAGE_URLS_STEPS_CHANGELOG.md]).
 */
object ListingImageUrlsWire {

    private data class ImageStepRow(
        val sortOrder: Int,
        val index: Int,
        val url: String,
        val width: Int?,
        val height: Int?,
    )

    data class CoverImageMeta(
        val url: String,
        val width: Int?,
        val height: Int?,
    )

    /**
     * URLs ordered by [sort_order] ascending (stable by original index), matching cover derivation
     * (smallest [sort_order] with a non-empty URL is the primary slot).
     */
    fun parseUrlStrings(arr: JSONArray?): List<String> =
        parseImageSteps(arr).map { it.url }

    /** Prefer explicit listing cover; else first URL using the same ordering as [parseUrlStrings]. */
    fun resolveCoverUrl(coverFromRoot: String, arr: JSONArray?): String =
        resolveCoverMeta(coverFromRoot, arr).url

    /** Cover URL plus pixel dimensions from the matching `image_urls` step when present. */
    fun resolveCoverMeta(coverFromRoot: String, arr: JSONArray?): CoverImageMeta {
        val root = coverFromRoot.trim()
        val steps = parseImageSteps(arr)
        if (root.isNotEmpty()) {
            val match = steps.firstOrNull { imageUrlsReferToSameAsset(it.url, root) }
            if (match != null) {
                return CoverImageMeta(url = root, width = match.width, height = match.height)
            }
            val first = steps.firstOrNull()
            if (first != null && first.width != null && first.height != null) {
                return CoverImageMeta(url = root, width = first.width, height = first.height)
            }
            return CoverImageMeta(url = root, width = null, height = null)
        }
        val first = steps.firstOrNull()
        return CoverImageMeta(
            url = first?.url.orEmpty(),
            width = first?.width,
            height = first?.height,
        )
    }

    private fun parseImageSteps(arr: JSONArray?): List<ImageStepRow> {
        if (arr == null) return emptyList()
        val rows = mutableListOf<ImageStepRow>()
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
                val width = o.optInt("width", 0).takeIf { it > 0 }
                    ?: o.optInt("Width", 0).takeIf { it > 0 }
                val height = o.optInt("height", 0).takeIf { it > 0 }
                    ?: o.optInt("Height", 0).takeIf { it > 0 }
                rows.add(ImageStepRow(sortOrder = order, index = i, url = url, width = width, height = height))
            } else {
                val s = arr.optString(i, "").trim()
                if (s.isNotBlank() && !s.startsWith("{")) {
                    rows.add(ImageStepRow(sortOrder = i, index = i, url = s, width = null, height = null))
                }
            }
        }
        return rows.sortedWith(compareBy<ImageStepRow> { it.sortOrder }.thenBy { it.index })
    }

    private fun imageUrlsReferToSameAsset(lhs: String, rhs: String): Boolean {
        val a = normalizeImageUrlPath(lhs)
        val b = normalizeImageUrlPath(rhs)
        if (a.isEmpty() || b.isEmpty()) return false
        if (a == b) return true
        return a.endsWith(b) || b.endsWith(a)
    }

    private fun normalizeImageUrlPath(raw: String): String {
        val trimmed = raw.trim()
        return runCatching {
            val uri = android.net.Uri.parse(trimmed)
            uri.buildUpon().clearQuery().fragment("").build().toString()
        }.getOrDefault(trimmed)
    }
}
