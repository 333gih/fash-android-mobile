package com.pc.fash_android_mobile.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.data.model.*
import com.pc.fash_android_mobile.data.repository.RecommendationRepository
import com.pc.fash_android_mobile.data.repository.AdvertisingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UnifiedHomeViewModel @Inject constructor(
    private val recommendationRepository: RecommendationRepository,
    private val advertisingRepository: AdvertisingRepository
) : ViewModel() {

    // State
    var rails by mutableStateOf<List<HomeRail>>(emptyList())
        private set

    var heroCard by mutableStateOf<HeroCard?>(null)
        private set

    var promoSlides by mutableStateOf<List<AdvertisingBanner>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isLoadingMore by mutableStateOf(false)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    var hasMore by mutableStateOf(true)
        private set

    var showOnlyMySize by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    // Configuration
    private val railLimit = 8
    private val itemsPerRail = 6

    init {
        loadInitialFeed()
    }

    fun loadInitialFeed() {
        if (isLoading || rails.isNotEmpty()) return

        viewModelScope.launch {
            isLoading = true
            error = null

            try {
                // Load unified feed and promo slides in parallel
                val feed = loadUnifiedFeed()
                val promos = loadPromoSlides()

                if (feed != null) {
                    rails = feed.rails
                    heroCard = feed.hero
                }

                if (promos != null) {
                    promoSlides = promos
                }
            } catch (e: Exception) {
                error = e.message ?: "Failed to load feed"
            } finally {
                isLoading = false
            }

            // Track rail impressions
            trackRailImpressions()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing = true

            try {
                // Reset state
                rails = emptyList()
                heroCard = null
                hasMore = true

                // Load fresh feed
                val feed = loadUnifiedFeed()
                if (feed != null) {
                    rails = feed.rails
                    heroCard = feed.hero
                }

                // Refresh promo slides
                val promos = loadPromoSlides()
                if (promos != null) {
                    promoSlides = promos
                }
            } catch (e: Exception) {
                error = e.message ?: "Failed to refresh feed"
            } finally {
                isRefreshing = false
            }

            // Track rail impressions
            trackRailImpressions()
        }
    }

    fun loadMore() {
        if (isLoadingMore || !hasMore) return

        viewModelScope.launch {
            isLoadingMore = true

            try {
                // For now, load more is not implemented (would need pagination support)
                // TODO: Implement pagination when backend supports it
                hasMore = false
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun toggleSizeFilter() {
        showOnlyMySize = !showOnlyMySize

        // Reload feed with size filter
        viewModelScope.launch {
            isLoading = true

            try {
                val feed = loadUnifiedFeed()
                if (feed != null) {
                    rails = feed.rails
                    heroCard = feed.hero
                }
            } catch (e: Exception) {
                error = e.message ?: "Failed to apply filter"
            } finally {
                isLoading = false
            }

            trackRailImpressions()
        }
    }

    private suspend fun loadUnifiedFeed(): UnifiedHomeFeedResponse? {
        val sizingMode = if (showOnlyMySize) "match_profile" else "all"

        return try {
            val response = recommendationRepository.getUnifiedHomeFeed(
                railLimit = railLimit,
                itemsPerRail = itemsPerRail,
                sizingMode = sizingMode
            )
            response.getOrNull()
        } catch (e: Exception) {
            error = e.message ?: "Failed to load unified home feed"
            null
        }
    }

    private suspend fun loadPromoSlides(): List<AdvertisingBanner>? {
        return try {
            val response = advertisingRepository.getPromoSlides()
            response.getOrNull()
        } catch (e: Exception) {
            null
        }
    }

    private fun trackRailImpressions() {
        // Track impressions for all visible rails
        rails.forEach { rail ->
            rail.items.take(6).forEach { item ->
                // TODO: Implement impression tracking
                // feedEventReporter.trackImpression(item.listing.id, rail.railId, null)
            }
        }
    }

    fun trackRailClick(rail: HomeRail, item: ListingWithMatch, position: Int) {
        // TODO: Implement click tracking
        // feedEventReporter.trackClick(item.listing.id, rail.railId, position)
    }

    fun trackSizeMatchBadgeClick(item: ListingWithMatch) {
        // TODO: Implement size badge interaction tracking
        // feedEventReporter.trackCustomEvent("size_badge_click", item.listing.id, metadata)
    }
}
