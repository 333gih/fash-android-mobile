package com.pc.fash_android_mobile.ui.feed

import org.json.JSONObject
import java.util.Locale

/**
 * Strips accidental JSON string blobs (`{ "id": ... }`) and raw UUID-only values so listing UI never shows wire noise.
 */
fun sanitizeListingUiText(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val t = raw.trim()
    if (t.startsWith("{")) {
        return try {
            val o = JSONObject(t)
            o.optString("display_name", "")
                .ifBlank { o.optString("DisplayName", "") }
                .ifBlank { o.optString("name", "") }
                .ifBlank { o.optString("Name", "") }
                .ifBlank { o.optString("title", "") }
                .ifBlank { o.optString("Title", "") }
                .trim()
        } catch (_: Exception) {
            ""
        }
    }
    if (ListingUiUuidRegex.matches(t)) return ""
    return t
}

private val ListingUiUuidRegex = Regex(
    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
)

/** VND display aligned with INTEGRATION.md / app convention (đ + dot thousands). */
fun formatListingPriceVnd(vnd: Long): String =
    "đ ${"%,d".format(vnd).replace(',', '.')}"

/** Likes / saves on a grid card — short for TikTok-style social proof. */
fun formatListingEngagementShort(count: Int): String =
    when {
        count <= 0 -> ""
        count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000f)
        count >= 1_000 -> String.format(Locale.US, "%.1fk", count / 1_000f)
        else -> count.toString()
    }

fun formatFollowerCountShort(count: Int): String =
    when {
        count >= 1_000_000 -> "${count / 1_000_000}.${(count % 1_000_000) / 100_000}M"
        count >= 1_000 -> "${count / 1_000}.${(count % 1_000) / 100}k"
        else -> count.toString()
    }
