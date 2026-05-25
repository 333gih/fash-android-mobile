package com.pc.fash_android_mobile.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.data.home.HomeEditorialPostStub
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeEditorialListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as FashApplication).editorialGuideRepository

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    private val _error = MutableStateFlow(false)
    val error: StateFlow<Boolean> = _error.asStateFlow()

    private val _posts = MutableStateFlow<List<HomeEditorialPostStub>>(emptyList())
    val posts: StateFlow<List<HomeEditorialPostStub>> = _posts.asStateFlow()

    private var offset = 0
    private var hasMore = true

    fun loadInitial() {
        offset = 0
        hasMore = true
        viewModelScope.launch {
            _loading.value = true
            _error.value = false
            val result = withContext(Dispatchers.IO) { repository.listAll(limit = 20, offset = 0) }
            _loading.value = false
            result.fold(
                onSuccess = { page ->
                    _posts.value = page.items
                    hasMore = page.hasMore
                    offset = page.items.size
                },
                onFailure = {
                    _posts.value = emptyList()
                    _error.value = true
                },
            )
        }
    }

    fun loadMore() {
        if (!hasMore || _loadingMore.value || _loading.value) return
        viewModelScope.launch {
            _loadingMore.value = true
            val result = withContext(Dispatchers.IO) { repository.listAll(limit = 20, offset = offset) }
            _loadingMore.value = false
            result.onSuccess { page ->
                if (page.items.isNotEmpty()) {
                    _posts.value = _posts.value + page.items
                    offset += page.items.size
                }
                hasMore = page.hasMore
            }
        }
    }
}
