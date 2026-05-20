package com.pc.fash_android_mobile.ui.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.data.advertising.AppAdvertisingSlideItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "PromoSlidesViewModel"

/**
 * Loads promo carousel from core-service CMS only. Empty list when API fails or no live slides.
 */
class PromoSlidesViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as FashApplication).advertisingRepository

    private val _remoteSlides = MutableStateFlow<List<AppAdvertisingSlideItem>>(emptyList())
    val remoteSlides: StateFlow<List<AppAdvertisingSlideItem>> = _remoteSlides.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val fashApp = getApplication<FashApplication>()
        val publicBrowse = fashApp.isGuestBrowseActive
        viewModelScope.launch(Dispatchers.IO) {
            repo.getSlides("promo_slider_main", publicBrowse = publicBrowse).fold(
                onSuccess = { res ->
                    Log.i(TAG, "CMS slides: ${res.items.size} item(s)")
                    _remoteSlides.value = res.items
                },
                onFailure = { e ->
                    Log.w(TAG, "CMS slides failed — carousel hidden", e)
                    _remoteSlides.value = emptyList()
                },
            )
        }
    }
}
