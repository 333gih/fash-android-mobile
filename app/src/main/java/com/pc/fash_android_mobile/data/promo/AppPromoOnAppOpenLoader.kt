package com.pc.fash_android_mobile.data.promo

import android.content.Context
import com.pc.fash_android_mobile.FashApplication
import kotlinx.coroutines.delay

/**
 * Pulls admin `on_app_open` campaigns from core-service catalog (Redis snapshot)
 * and returns the highest-priority campaign the user may still see.
 */
object AppPromoOnAppOpenLoader {

    suspend fun fetchAndEnqueue(app: FashApplication) {
        var result = app.appPromoInterstitialRepository.fetchActiveCampaigns()
        if (result.isFailure) {
            delay(400)
            result = app.appPromoInterstitialRepository.fetchActiveCampaigns()
        }
        val campaigns = result.getOrNull().orEmpty()
        campaigns
            .filter { isOnAppOpenSchedule(it) }
            .forEach { AppPromoPendingQueue.enqueue(it) }
    }

    fun resolvePresentable(appContext: Context): AppPromoCampaign? {
        while (true) {
            val remote = AppPromoPendingQueue.pollHighest() ?: return null
            if (AppPromoCampaignStore.isDialogConsumed(appContext, remote)) continue
            return if (AppPromoCampaignStore.canShow(appContext, remote)) remote else continue
        }
    }

    suspend fun syncAndResolve(
        app: FashApplication,
        appContext: Context,
        isGuestMode: Boolean,
        blockBecauseOtherUi: Boolean,
        incrementOpenCount: Boolean = false,
    ): AppPromoCampaign? {
        if (isGuestMode || blockBecauseOtherUi) return null
        if (incrementOpenCount) {
            AppPromoCampaignStore.incrementAppOpenCount(appContext)
        }
        fetchAndEnqueue(app)
        resolvePresentable(appContext)?.let { return it }
        slideFallback(app, appContext)?.let { return it }
        return AppPromoDefaultFallback.resolve(appContext)
    }

    private suspend fun slideFallback(app: FashApplication, appContext: Context): AppPromoCampaign? {
        val slide = app.advertisingRepository.getSlides("promo_slider_main").getOrNull()?.items?.firstOrNull()
            ?: return null
        val campaign = AppPromoSlideFallback.fromSlide(slide) ?: return null
        return if (AppPromoCampaignStore.canShow(appContext, campaign)) campaign else null
    }

    private fun isOnAppOpenSchedule(campaign: AppPromoCampaign): Boolean {
        val type = campaign.scheduleType?.trim()?.lowercase().orEmpty()
        return type.isEmpty() || type == "on_app_open"
    }
}
