package com.pc.fash_android_mobile.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.data.advertising.AppAdvertisingSlideItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Loads promo carousel copy from core-service CMS. [remoteSlides] null means "not loaded yet or error — use UI defaults";
 * non-null empty list means server returned no live items (also falls back to defaults in the UI layer if desired).
 */
class PromoSlidesViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as FashApplication).advertisingRepository

    private val _remoteSlides = MutableStateFlow<List<AppAdvertisingSlideItem>?>(null)
    val remoteSlides: StateFlow<List<AppAdvertisingSlideItem>?> = _remoteSlides.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.getSlides("promo_slider_main").fold(
                onSuccess = { res -> _remoteSlides.value = res.items },
                onFailure = { _remoteSlides.value = null },
            )
        }
    }
}
