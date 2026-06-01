package com.pc.fash_android_mobile.ui.feed

import android.net.Uri

/** Feed grid image sizing — Coil decode size + Shopify `width` param (Pinterest-style loading). */
object FeedListingImageSizer {
    private const val MIN_FEED_WIDTH_PX = 320
    private const val MAX_FEED_WIDTH_PX = 800

    fun targetWidthPx(columnWidthDp: Float, density: Float): Int {
        val w = (columnWidthDp * density).toInt()
        return w.coerceIn(MIN_FEED_WIDTH_PX, MAX_FEED_WIDTH_PX)
    }

    fun pixelSize(columnWidthDp: Float, density: Float, aspectRatio: Float): Pair<Int, Int> {
        val w = targetWidthPx(columnWidthDp, density)
        val h = (w / aspectRatio.coerceAtLeast(0.01f)).toInt().coerceAtLeast(1)
        return w to h
    }

    fun urlForFeedGrid(path: String, columnWidthDp: Float, density: Float): String {
        val resolved = resolveListingImageUrl(path)
        if (resolved.isEmpty()) return ""
        val targetW = targetWidthPx(columnWidthDp, density)
        return applyShopifyWidthQuery(resolved, targetW)
    }

    private fun applyShopifyWidthQuery(url: String, widthPx: Int): String {
        val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull() ?: return url
        if (!host.contains("shopify")) return url
        val uri = Uri.parse(url)
        val builder = uri.buildUpon().clearQuery()
        for (name in uri.queryParameterNames) {
            if (name.equals("width", ignoreCase = true)) continue
            for (value in uri.getQueryParameters(name)) {
                builder.appendQueryParameter(name, value)
            }
        }
        builder.appendQueryParameter("width", widthPx.toString())
        return builder.build().toString()
    }
}
