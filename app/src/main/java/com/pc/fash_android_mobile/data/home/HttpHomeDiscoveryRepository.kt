package com.pc.fash_android_mobile.data.home

import android.util.Log
import com.pc.fash_android_mobile.data.editorial.EditorialGuideRepository
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.search.FeaturedSellerItem
import com.pc.fash_android_mobile.data.search.SearchRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async

private const val TAG = "HomeDiscoveryHttp"

/**
 * Real implementation of [HomeDiscoveryRepository] backed by core-service + common-service.
 *
 * - Editorial posts: `common-service` public `/editorial-guides` (locale-aware).
 * - Featured sellers: `core-service` `GET /search/featured-sellers` (home rail = small limit;
 *   see-all screen drives its own pagination via [SearchRepository.getFeaturedSellersPage]).
 * - Recently viewed: `core-service` `GET /listings/recently-viewed` — per-user history.
 *
 * Each section degrades independently: a failed call returns empty for that section
 * (matches search/home semantics — empty section is hidden by the UI).
 */
class HttpHomeDiscoveryRepository(
    private val editorialGuideRepository: EditorialGuideRepository,
    private val searchRepository: SearchRepository,
    private val listingRepository: ListingRepository,
    /** Home rail size for sellers; see-all screen handles bigger pages independently. */
    private val homeFeaturedSellersLimit: Int = 8,
    /** Home rail size for recently viewed. */
    private val homeRecentlyViewedLimit: Int = 12,
) : HomeDiscoveryRepository {

    override suspend fun loadDiscoveryBundle(): Result<HomeDiscoveryBundle> = coroutineScope {
        val editorialAsync = async {
            editorialGuideRepository.listCarousel().getOrElse {
                Log.w(TAG, "editorial carousel failed: ${it.message}")
                emptyList()
            }
        }
        val sellersAsync = async {
            searchRepository.getFeaturedSellers(limit = homeFeaturedSellersLimit, offset = 0).getOrElse {
                Log.w(TAG, "featured sellers failed: ${it.message}")
                emptyList<FeaturedSellerItem>()
            }
        }
        val recentlyViewedAsync = async {
            listingRepository.getRecentlyViewed(limit = homeRecentlyViewedLimit, offset = 0).getOrElse {
                Log.w(TAG, "recently viewed failed: ${it.message}")
                emptyList()
            }
        }
        // Keep trendingCategories empty — section was removed from the home feed; data is still
        // exposed for any future surface (e.g. Explore re-use).
        Result.success(
            HomeDiscoveryBundle(
                editorialPosts = editorialAsync.await(),
                trendingCategories = emptyList(),
                recommendedSellers = sellersAsync.await(),
                recentlyViewed = recentlyViewedAsync.await(),
            ),
        )
    }
}
