package com.pc.fash_android_mobile.ui.feed

import android.content.Context
import coil.imageLoader
import coil.request.ImageRequest
import com.pc.fash_android_mobile.data.listing.ListingFeedItem

/** Warms Coil cache after feed JSON arrives so masonry tiles paint without visible loading. */
object FeedListingImagePrefetch {
    private const val MAX_ITEMS = 28

    fun prefetch(context: Context, items: List<ListingFeedItem>, columnWidthDp: Float) {
        if (items.isEmpty()) return
        val density = context.resources.displayMetrics.density
        val loader = context.imageLoader
        items.take(MAX_ITEMS).forEach { item ->
            val raw = item.coverImageUrl.trim().ifEmpty { item.imageUrls.firstOrNull().orEmpty() }
            if (raw.isEmpty()) return@forEach
            val ratio = listingMasonryAspectRatio(item)
            val url = FeedListingImageSizer.urlForFeedGrid(raw, columnWidthDp, density)
            if (url.isEmpty()) return@forEach
            val (w, h) = FeedListingImageSizer.pixelSize(columnWidthDp, density, ratio)
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(w, h)
                .build()
            loader.enqueue(request)
        }
    }
}
