package com.pc.fash_android_mobile.data.home

import com.pc.fash_android_mobile.data.listing.Category
import com.pc.fash_android_mobile.data.listing.ListingFeedItem
import com.pc.fash_android_mobile.data.search.FeaturedSellerItem
import com.pc.fash_android_mobile.data.search.TrendingTagChip
import kotlinx.coroutines.delay

/**
 * Home discovery payload (editorial posts, trending, sellers, recents).
 * Replace [StubHomeDiscoveryRepository] with a real HTTP-backed implementation when APIs exist.
 */
data class HomeDiscoveryBundle(
    /** Short admin-written posts (future CMS); tap maps to Explore hints until in-app reader exists. */
    val editorialPosts: List<HomeEditorialPostStub> = emptyList(),
    val trendingCategories: List<Category> = emptyList(),
    val recommendedSellers: List<FeaturedSellerItem> = emptyList(),
    val recentlyViewed: List<ListingFeedItem> = emptyList(),
    /** Personalized rails from GET /recommendations/home-sections (auth or public). */
    val huntToday: List<ListingFeedItem> = emptyList(),
    val stylePicks: List<ListingFeedItem> = emptyList(),
    val similarToSaved: List<ListingFeedItem> = emptyList(),
    /** Personalized for-you from home-sections (shown only when signals ≥ 3). */
    val forYou: List<ListingFeedItem> = emptyList(),
    val seasonalNearYou: List<ListingFeedItem> = emptyList(),
    val dailyOutfitDrop: List<com.pc.fash_android_mobile.data.recommendation.OutfitSetCard> = emptyList(),
    val shoppingContext: com.pc.fash_android_mobile.data.recommendation.ShoppingContext? = null,
    /** Trending aesthetic tag names from /search/trending-tags for the style chips row (display only). */
    val trendingStyleTags: List<String> = emptyList(),
    /**
     * Same tags with IDs from /search/trending-tags?include_ids=true.
     * Used for direct ID-based filter navigation (Bug C fix).
     * Falls back to name-only chips (id = "") when the server returns a plain string array.
     */
    val trendingStyleTagChips: List<TrendingTagChip> = emptyList(),
)

/**
 * Wire shape mirrors a future `GET …/home/editorial-posts` (id, hero image, optional deep-link hints).
 */
data class HomeEditorialPostStub(
    val id: String,
    val slug: String,
    val title: String,
    val summary: String,
    val coverImageUrl: String?,
    val exploreCategoryId: String? = null,
    val exploreSearchQuery: String? = null,
)

interface HomeDiscoveryRepository {
    /** Header rails only — featured sellers, editorial, trending tags (no feed tab sections). */
    suspend fun loadShell(): Result<HomeDiscoveryBundle>

    /** Full bundle including all recommendation sections (legacy / refresh-all). */
    suspend fun loadDiscoveryBundle(): Result<HomeDiscoveryBundle>
}

/**
 * Hard-coded discovery payload aligned with seeded [Category] UUIDs in `core-service/migrations/init.sql`
 * and listing / seller shapes from production parsers ([FeaturedSellerItem], [ListingFeedItem]).
 */
class StubHomeDiscoveryRepository : HomeDiscoveryRepository {

    override suspend fun loadShell(): Result<HomeDiscoveryBundle> = loadDiscoveryBundle()

    override suspend fun loadDiscoveryBundle(): Result<HomeDiscoveryBundle> {
        // Tiny delay keeps the same “async boundary” as a future network call (easy to swap impl).
        delay(32)
        return Result.success(
            HomeDiscoveryBundle(
                editorialPosts = stubEditorialPosts,
                trendingCategories = stubTrendingCategories,
                recommendedSellers = stubRecommendedSellers,
                recentlyViewed = stubRecentlyViewed,
            ),
        )
    }

    companion object {
        /** Seeded taxonomy — `slug` matches core-service init seed. */
        private val stubTrendingCategories: List<Category> = listOf(
            Category(
                id = "a1111111-1111-1111-1111-111111111102",
                name = "Tops",
                slug = "tops",
            ),
            Category(
                id = "a1111111-1111-1111-1111-111111111103",
                name = "Bottoms",
                slug = "bottoms",
            ),
            Category(
                id = "a1111111-1111-1111-1111-111111111104",
                name = "Dresses",
                slug = "dresses",
            ),
            Category(
                id = "a1111111-1111-1111-1111-111111111107",
                name = "Shoes",
                slug = "shoes",
            ),
            Category(
                id = "a1111111-1111-1111-1111-111111111108",
                name = "Bags",
                slug = "bags",
            ),
            Category(
                id = "a1111111-1111-1111-1111-111111111106",
                name = "Accessories",
                slug = "accessories",
            ),
        )

        /** Offline fallback only — [HttpHomeDiscoveryRepository] replaces with CMS data when API is up. */
        private val stubEditorialPosts: List<HomeEditorialPostStub> = emptyList()

        private val stubRecommendedSellers: List<FeaturedSellerItem> = listOf(
            FeaturedSellerItem(
                userId = "e5555555-5555-5555-5555-555555555501",
                username = "demo_user",
                displayName = "Demo Seller",
                bio = "Fashion enthusiast — pre-loved & curated picks.",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&q=80",
                followerCount = 1280,
                listingCount = 24,
                averageRating = 4.8f,
                verified = true,
                previewListingIds = emptyList(),
            ),
            FeaturedSellerItem(
                userId = "b0000002-0002-0002-0002-000000000002",
                username = "fash_curator_hn",
                displayName = "Curator HN",
                bio = "Local picks — same-day meetup in Hà Nội.",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&q=80",
                followerCount = 642,
                listingCount = 18,
                averageRating = 4.6f,
                verified = false,
                previewListingIds = emptyList(),
            ),
            FeaturedSellerItem(
                userId = "b0000003-0003-0003-0003-000000000003",
                username = "saigon_thrift",
                displayName = "Sài Gòn Thrift",
                bio = "Vintage & streetwear — ship toàn quốc.",
                avatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200&q=80",
                followerCount = 2104,
                listingCount = 56,
                averageRating = 4.9f,
                verified = true,
                previewListingIds = emptyList(),
            ),
        )

        private val stubRecentlyViewed: List<ListingFeedItem> = listOf(
            ListingFeedItem(
                id = "b1000001-1001-1001-1001-000000000001",
                title = "Vintage Denim Jacket",
                coverImageUrl = "https://images.unsplash.com/photo-1525450824789-227cbef6a01d?w=400&q=80",
                imageUrls = emptyList(),
                priceVnd = 450_000L,
                brand = "Levi's",
                size = "M",
                categoryName = "Outerwear",
                listingAestheticTag = "Vintage",
                condition = "like_new",
                likeCount = 42,
                saveCount = 18,
                sellerId = "e5555555-5555-5555-5555-555555555501",
                sellerUsername = "demo_user",
                sellerAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&q=80",
                sellerStyleTag = "Street",
                createdAt = null,
                isLiked = false,
                isSaved = false,
                sellerIsFollowing = false,
                listingStatus = "active",
            ),
            ListingFeedItem(
                id = "b1000002-1002-1002-1002-000000000002",
                title = "Minimalist White Sneakers",
                coverImageUrl = "https://images.unsplash.com/photo-1600269452121-4f2416e55c28?w=400&q=80",
                imageUrls = emptyList(),
                priceVnd = 380_000L,
                brand = null,
                size = "40",
                categoryName = "Shoes",
                listingAestheticTag = "Minimal",
                condition = "good",
                likeCount = 31,
                saveCount = 9,
                sellerId = "b0000002-0002-0002-0002-000000000002",
                sellerUsername = "fash_curator_hn",
                sellerAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&q=80",
                sellerStyleTag = null,
                createdAt = null,
                isLiked = false,
                isSaved = false,
                sellerIsFollowing = false,
                listingStatus = "active",
            ),
            ListingFeedItem(
                id = "b1000003-1003-1003-1003-000000000003",
                title = "Y2K Cargo Pants",
                coverImageUrl = "https://images.unsplash.com/photo-1541099649102-fbad7c800737?w=400&q=80",
                imageUrls = emptyList(),
                priceVnd = 520_000L,
                brand = "Thrifted",
                size = "S",
                categoryName = "Bottoms",
                listingAestheticTag = "Y2K",
                condition = "like_new",
                likeCount = 67,
                saveCount = 22,
                sellerId = "b0000003-0003-0003-0003-000000000003",
                sellerUsername = "saigon_thrift",
                sellerAvatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200&q=80",
                sellerStyleTag = "Y2K",
                createdAt = null,
                isLiked = false,
                isSaved = false,
                sellerIsFollowing = false,
                listingStatus = "active",
            ),
        )
    }
}
