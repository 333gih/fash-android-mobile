package com.pc.fash_android_mobile.ui.feed

import android.net.Uri
import com.pc.fash_android_mobile.config.AppEnvironment
import java.net.URLEncoder

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
        val uri = runCatching { Uri.parse(resolved) }.getOrNull() ?: return resolved
        val host = uri.host?.lowercase() ?: ""

        // Shopify CDN: use native ?width= resize param
        if (host.contains("shopify")) return applyShopifyWidthQuery(resolved, uri, targetW)

        // Self-hosted SeaweedFS: route through imgproxy for on-the-fly WebP thumbnail.
        // Path always starts with /fash-uploads/ (the bucket name is part of the URL path).
        val urlPath = uri.path ?: ""
        val resizeBase = AppEnvironment.imageResizeBaseUrl
        if (resizeBase.isNotEmpty() && urlPath.startsWith("/fash-uploads/")) {
            // /fash-uploads/listings/... → s3://fash-uploads/listings/...
            val s3Source = "s3:$urlPath"
            val encoded = URLEncoder.encode(s3Source, "UTF-8")
            return "$resizeBase/unsafe/rs:fit:$targetW:0:0/plain/$encoded"
        }

        return resolved
    }

    private fun applyShopifyWidthQuery(url: String, uri: Uri, widthPx: Int): String {
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
