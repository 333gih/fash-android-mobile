package com.pc.fash_android_mobile.ui.login

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

private const val TAG = "LoginHeroSlidesViewModel"
private const val LOGIN_SLIDER_PLACEMENT = "login_slider"

/**
 * Loads login-screen hero slides from app advertising CMS using a dedicated
 * placement key. Login flow always runs in pre-auth mode, so this uses
 * public-browse endpoint semantics.
 */
class LoginHeroSlidesViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as FashApplication).advertisingRepository

    private val _remoteSlides = MutableStateFlow<List<AppAdvertisingSlideItem>>(emptyList())
    val remoteSlides: StateFlow<List<AppAdvertisingSlideItem>> = _remoteSlides.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.getSlides(LOGIN_SLIDER_PLACEMENT, publicBrowse = true).fold(
                onSuccess = { res ->
                    Log.i(TAG, "CMS login slides: ${res.items.size} item(s)")
                    _remoteSlides.value = res.items
                },
                onFailure = { e ->
                    Log.w(TAG, "CMS login slides failed - fallback to local hero", e)
                    _remoteSlides.value = emptyList()
                },
            )
        }
    }
}
