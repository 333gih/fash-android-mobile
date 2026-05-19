package com.pc.fash_android_mobile.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.data.editorial.EditorialGuideDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeEditorialDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as FashApplication).editorialGuideRepository

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow(false)
    val error: StateFlow<Boolean> = _error.asStateFlow()

    private val _guide = MutableStateFlow<EditorialGuideDetail?>(null)
    val guide: StateFlow<EditorialGuideDetail?> = _guide.asStateFlow()

    fun load(slug: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = false
            val result = withContext(Dispatchers.IO) { repository.getBySlug(slug) }
            _loading.value = false
            result.fold(
                onSuccess = { _guide.value = it },
                onFailure = {
                    _guide.value = null
                    _error.value = true
                },
            )
        }
    }
}
