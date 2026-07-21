package com.pc.fash_android_mobile.ui.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.data.advertising.AdvertisingPlacements
import com.pc.fash_android_mobile.data.advertising.AppAdvertisingSlideItem
import com.pc.fash_android_mobile.notifications.GuestLocalReengagementScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "PromoSlidesViewModel"

/**
 * Loads promo carousel from core-service CMS only. Empty list when API fails or no live slides.
 * Guest browse merges [AdvertisingPlacements.GUEST_HOME_BANNER] ahead of [AdvertisingPlacements.PROMO_SLIDER_MAIN].
 */
class PromoSlidesViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as FashApplication).advertisingRepository

    private val _remoteSlides = MutableStateFlow<List<AppAdvertisingSlideItem>>(emptyList())
    val remoteSlides: StateFlow<List<AppAdvertisingSlideItem>> = _remoteSlides.asStateFlow()

    private val _guestReengagementSlides = MutableStateFlow<List<AppAdvertisingSlideItem>>(emptyList())
    val guestReengagementSlides: StateFlow<List<AppAdvertisingSlideItem>> = _guestReengagementSlides.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val fashApp = getApplication<FashApplication>()
        val publicBrowse = fashApp.isGuestBrowseActive
        viewModelScope.launch(Dispatchers.IO) {
            if (publicBrowse) {
                val main = repo.getSlides(AdvertisingPlacements.PROMO_SLIDER_MAIN, publicBrowse = true)
                val guestBanner = repo.getSlides(AdvertisingPlacements.GUEST_HOME_BANNER, publicBrowse = true)
                val reengagement = repo.getSlides(AdvertisingPlacements.GUEST_REENGAGEMENT, publicBrowse = true)
                val merged = guestBanner.getOrNull()?.items.orEmpty() + main.getOrNull()?.items.orEmpty()
                Log.i(TAG, "guest CMS slides: banner=${guestBanner.getOrNull()?.items?.size ?: 0} main=${main.getOrNull()?.items?.size ?: 0}")
                _remoteSlides.value = merged
                val reSlides = reengagement.getOrNull()?.items.orEmpty()
                _guestReengagementSlides.value = reSlides
                reSlides.firstOrNull()?.let { slide ->
                    GuestLocalReengagementScheduler.updateReminderCopy(
                        fashApp,
                        slide.title,
                        slide.subtitle,
                    )
                }
            } else {
                repo.getSlides(AdvertisingPlacements.PROMO_SLIDER_MAIN, publicBrowse = false).fold(
                    onSuccess = { res ->
                        Log.i(TAG, "CMS slides: ${res.items.size} item(s)")
                        _remoteSlides.value = res.items
                    },
                    onFailure = { e ->
                        Log.w(TAG, "CMS slides failed — carousel hidden", e)
                        _remoteSlides.value = emptyList()
                    },
                )
                _guestReengagementSlides.value = emptyList()
            }
        }
    }
}
