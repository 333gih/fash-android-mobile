package com.pc.fash_android_mobile.ui.listing

import com.pc.fash_android_mobile.data.listing.ListingDetail
import com.pc.fash_android_mobile.data.listing.ListingFeedItem

enum class ProductDiscoveryRelation {
    SELLER,
    CATEGORY,
    BRAND,
    STYLE,
}

data class ProductDiscoveryFeedEntry(
    val item: ListingFeedItem,
    val relation: ProductDiscoveryRelation,
    val relationLabel: String,
)

object ProductDiscoveryFeedBuilder {
    const val MERGED_DISCOVERY_LIMIT = 24
    const val SELLER_RAIL_LIMIT = 10
    const val RELATED_RAIL_LIMIT = 8

    fun merge(
        detail: ListingDetail,
        sellerLabel: String,
        sellerItems: List<ListingFeedItem>,
        categoryLabel: String?,
        categoryItems: List<ListingFeedItem>,
        brandLabel: String?,
        brandItems: List<ListingFeedItem>,
        styleItems: List<ListingFeedItem>,
        styleFallbackLabel: String,
        limit: Int = MERGED_DISCOVERY_LIMIT,
    ): List<ProductDiscoveryFeedEntry> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<ProductDiscoveryFeedEntry>()

        fun append(items: List<ListingFeedItem>, relation: ProductDiscoveryRelation, label: String) {
            val badge = label.trim()
            if (badge.isEmpty()) return
            for (item in items) {
                val key = item.id.lowercase()
                if (!seen.add(key)) continue
                result.add(ProductDiscoveryFeedEntry(item, relation, badge))
                if (result.size >= limit) return
            }
        }

        append(sellerItems, ProductDiscoveryRelation.SELLER, sellerLabel)
        if (result.size < limit) {
            categoryLabel?.trim()?.takeIf { it.isNotEmpty() }?.let {
                append(categoryItems, ProductDiscoveryRelation.CATEGORY, it)
            }
        }
        if (result.size < limit) {
            brandLabel?.trim()?.takeIf { it.isNotEmpty() }?.let {
                append(brandItems, ProductDiscoveryRelation.BRAND, it)
            }
        }
        if (result.size < limit) {
            val styleBadge = detail.aestheticTagRefs.firstOrNull()?.label?.trim()?.takeIf { it.isNotEmpty() }
                ?: styleFallbackLabel
            append(styleItems, ProductDiscoveryRelation.STYLE, styleBadge)
        }
        return result
    }
}
