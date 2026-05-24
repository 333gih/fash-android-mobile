package com.pc.fash_android_mobile.data.home

import android.util.Log
import com.pc.fash_android_mobile.data.editorial.EditorialGuideRepository
import com.pc.fash_android_mobile.data.listing.ListingRepository
import com.pc.fash_android_mobile.data.search.FeaturedSellerItem
import com.pc.fash_android_mobile.data.recommendation.RecommendationRepository
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
    private val recommendationRepository: RecommendationRepository,
    /** Home rail size for sellers; see-all screen handles bigger pages independently. */
    private val homeFeaturedSellersLimit: Int = 8,
    /** Home rail size for recently viewed. */
    private val homeRecentlyViewedLimit: Int = 12,
    private val guestBrowseProvider: () -> Boolean = { false },
    /**
     * Per-call sizing preference resolver. Returning `"match_profile"` propagates the user's
     * "Match my size" toggle from Explore through to the Home rails (for_you / style_picks /
     * similar_to_saved) so Home behaves consistently with Explore. Default `"all"`.
     */
    private val sizingModeProvider: () -> String = { "all" },
) : HomeDiscoveryRepository {

    override suspend fun loadShell(): Result<HomeDiscoveryBundle> = coroutineScope {
        val editorialAsync = async {
            editorialGuideRepository.listCarousel().getOrElse {
                Log.w(TAG, "editorial carousel failed: ${it.message}")
                emptyList()
            }
        }
        val guest = guestBrowseProvider()
        val sellersAsync = async {
            if (guest) {
                searchRepository.browseFeaturedSellersPage(limit = homeFeaturedSellersLimit, offset = 0)
                    .map { it.items }
                    .getOrElse {
                        Log.w(TAG, "public featured sellers failed: ${it.message}")
                        emptyList()
                    }
            } else {
                searchRepository.getFeaturedSellers(limit = homeFeaturedSellersLimit, offset = 0).getOrElse {
                    Log.w(TAG, "featured sellers failed: ${it.message}")
                    emptyList<FeaturedSellerItem>()
                }
            }
        }
        val trendingTagsAsync = async {
            searchRepository.getTrendingTagsWithIds(limit = 10).getOrElse {
                Log.w(TAG, "trending tags failed: ${it.message}")
                emptyList()
            }
        }
        val tags = trendingTagsAsync.await()
        Result.success(
            HomeDiscoveryBundle(
                editorialPosts = editorialAsync.await(),
                recommendedSellers = sellersAsync.await(),
                trendingStyleTagChips = tags,
                trendingStyleTags = tags.map { it.name },
            ),
        )
    }

    override suspend fun loadDiscoveryBundle(): Result<HomeDiscoveryBundle> = coroutineScope {
        val editorialAsync = async {
            editorialGuideRepository.listCarousel().getOrElse {
                Log.w(TAG, "editorial carousel failed: ${it.message}")
                emptyList()
            }
        }
        val guest = guestBrowseProvider()
        val sellersAsync = async {
            if (guest) {
                searchRepository.browseFeaturedSellersPage(limit = homeFeaturedSellersLimit, offset = 0)
                    .map { it.items }
                    .getOrElse {
                        Log.w(TAG, "public featured sellers failed: ${it.message}")
                        emptyList()
                    }
            } else {
                searchRepository.getFeaturedSellers(limit = homeFeaturedSellersLimit, offset = 0).getOrElse {
                    Log.w(TAG, "featured sellers failed: ${it.message}")
                    emptyList<FeaturedSellerItem>()
                }
            }
        }
        val recentlyViewedAsync = async {
            if (guest) {
                emptyList()
            } else {
                listingRepository.getRecentlyViewed(limit = homeRecentlyViewedLimit, offset = 0).getOrElse {
                    Log.w(TAG, "recently viewed failed: ${it.message}")
                    emptyList()
                }
            }
        }
        val recSectionsAsync = async {
            recommendationRepository.homeSections(
                publicBrowse = guest,
                huntTodayLimit = 12,
                forYouLimit = 16,
                sectionLimit = 12,
                sizingMode = sizingModeProvider().takeIf { it.equals("match_profile", ignoreCase = true) },
            ).getOrElse {
                Log.w(TAG, "home-sections failed: ${it.message}")
                null
            }
        }
        val trendingTagsAsync = async {
            searchRepository.getTrendingTagsWithIds(limit = 10).getOrElse {
                Log.w(TAG, "trending tags failed: ${it.message}")
                emptyList()
            }
        }
        val rec = recSectionsAsync.await()
        Result.success(
            HomeDiscoveryBundle(
                editorialPosts = editorialAsync.await(),
                trendingCategories = emptyList(),
                recommendedSellers = sellersAsync.await(),
                recentlyViewed = rec?.continueBrowsing?.takeIf { it.isNotEmpty() }
                    ?: recentlyViewedAsync.await(),
                huntToday = rec?.huntToday.orEmpty(),
                stylePicks = rec?.stylePicks.orEmpty(),
                similarToSaved = rec?.similarToSaved.orEmpty(),
                forYou = rec?.forYou.orEmpty(),
                trendingStyleTagChips = trendingTagsAsync.await(),
                trendingStyleTags = trendingTagsAsync.await().map { it.name },
            ),
        )
    }
}
