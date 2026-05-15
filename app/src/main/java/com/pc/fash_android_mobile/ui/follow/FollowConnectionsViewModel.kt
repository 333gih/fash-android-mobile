package com.pc.fash_android_mobile.ui.follow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.data.user.UserRepository
import com.pc.fash_android_mobile.data.user.UserSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Tab 0: [UserRepository.getMyFollowing] — `GET …/users/me/following`.
 * Tab 1: [UserRepository.getMyFollowers] — `GET …/users/me/followers`.
 */
class FollowConnectionsViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository =
        (application as FashApplication).userRepository

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _following = MutableStateFlow<List<UserSearchResult>>(emptyList())
    val following: StateFlow<List<UserSearchResult>> = _following.asStateFlow()

    private val _followers = MutableStateFlow<List<UserSearchResult>>(emptyList())
    val followers: StateFlow<List<UserSearchResult>> = _followers.asStateFlow()

    private val _followingTotal = MutableStateFlow(0)
    val followingTotal: StateFlow<Int> = _followingTotal.asStateFlow()

    private val _followersTotal = MutableStateFlow(0)
    val followersTotal: StateFlow<Int> = _followersTotal.asStateFlow()

    private val _followingLoading = MutableStateFlow(false)
    val followingLoading: StateFlow<Boolean> = _followingLoading.asStateFlow()

    private val _followersLoading = MutableStateFlow(false)
    val followersLoading: StateFlow<Boolean> = _followersLoading.asStateFlow()

    private val _followingLoadingMore = MutableStateFlow(false)
    val followingLoadingMore: StateFlow<Boolean> = _followingLoadingMore.asStateFlow()

    private val _followersLoadingMore = MutableStateFlow(false)
    val followersLoadingMore: StateFlow<Boolean> = _followersLoadingMore.asStateFlow()

    private val _followingFailed = MutableStateFlow(false)
    val followingFailed: StateFlow<Boolean> = _followingFailed.asStateFlow()

    private val _followersFailed = MutableStateFlow(false)
    val followersFailed: StateFlow<Boolean> = _followersFailed.asStateFlow()

    fun show(initialTabIndex: Int) {
        _selectedTab.value = initialTabIndex.coerceIn(0, 1)
        viewModelScope.launch {
            loadTab(_selectedTab.value, refresh = true)
        }
    }

    fun selectTab(index: Int) {
        val i = index.coerceIn(0, 1)
        if (_selectedTab.value == i) return
        _selectedTab.value = i
        viewModelScope.launch {
            loadTab(i, refresh = false)
        }
    }

    fun retryActiveTab() {
        viewModelScope.launch {
            loadTab(_selectedTab.value, refresh = true)
        }
    }

    fun loadMoreFollowing() {
        if (_followingLoading.value || _followingLoadingMore.value) return
        if (_following.value.size >= _followingTotal.value) return
        viewModelScope.launch {
            _followingLoadingMore.value = true
            _followingFailed.value = false
            withContext(Dispatchers.IO) {
                userRepository.getMyFollowing(limit = PAGE_SIZE, offset = _following.value.size).fold(
                    onSuccess = { page ->
                        _following.value = _following.value + page.items
                        _followingTotal.value = page.total
                    },
                    onFailure = {
                        _followingFailed.value = true
                    },
                )
            }
            _followingLoadingMore.value = false
        }
    }

    fun loadMoreFollowers() {
        if (_followersLoading.value || _followersLoadingMore.value) return
        if (_followers.value.size >= _followersTotal.value) return
        viewModelScope.launch {
            _followersLoadingMore.value = true
            _followersFailed.value = false
            withContext(Dispatchers.IO) {
                userRepository.getMyFollowers(limit = PAGE_SIZE, offset = _followers.value.size).fold(
                    onSuccess = { page ->
                        _followers.value = _followers.value + page.items
                        _followersTotal.value = page.total
                    },
                    onFailure = {
                        _followersFailed.value = true
                    },
                )
            }
            _followersLoadingMore.value = false
        }
    }

    fun clearCachesForSignedOutUser() {
        _selectedTab.value = 0
        _following.value = emptyList()
        _followers.value = emptyList()
        _followingTotal.value = 0
        _followersTotal.value = 0
        _followingLoading.value = false
        _followersLoading.value = false
        _followingLoadingMore.value = false
        _followersLoadingMore.value = false
        _followingFailed.value = false
        _followersFailed.value = false
    }

    private suspend fun loadTab(tab: Int, refresh: Boolean) {
        if (tab == 0) loadFollowing(refresh) else loadFollowers(refresh)
    }

    private suspend fun loadFollowing(refresh: Boolean) {
        if (!refresh && _following.value.isNotEmpty()) return
        _followingLoading.value = true
        _followingFailed.value = false
        withContext(Dispatchers.IO) {
            userRepository.getMyFollowing(limit = PAGE_SIZE, offset = 0).fold(
                onSuccess = { page ->
                    _following.value = page.items
                    _followingTotal.value = page.total
                },
                onFailure = {
                    _following.value = emptyList()
                    _followingFailed.value = true
                },
            )
        }
        _followingLoading.value = false
    }

    private suspend fun loadFollowers(refresh: Boolean) {
        if (!refresh && _followers.value.isNotEmpty()) return
        _followersLoading.value = true
        _followersFailed.value = false
        withContext(Dispatchers.IO) {
            userRepository.getMyFollowers(limit = PAGE_SIZE, offset = 0).fold(
                onSuccess = { page ->
                    _followers.value = page.items
                    _followersTotal.value = page.total
                },
                onFailure = {
                    _followers.value = emptyList()
                    _followersFailed.value = true
                },
            )
        }
        _followersLoading.value = false
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
