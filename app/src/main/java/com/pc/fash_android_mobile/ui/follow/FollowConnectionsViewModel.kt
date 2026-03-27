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

class FollowConnectionsViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository =
        (application as FashApplication).userRepository

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _following = MutableStateFlow<List<UserSearchResult>>(emptyList())
    val following: StateFlow<List<UserSearchResult>> = _following.asStateFlow()

    private val _followers = MutableStateFlow<List<UserSearchResult>>(emptyList())
    val followers: StateFlow<List<UserSearchResult>> = _followers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _followingFailed = MutableStateFlow(false)
    val followingFailed: StateFlow<Boolean> = _followingFailed.asStateFlow()

    private val _followersFailed = MutableStateFlow(false)
    val followersFailed: StateFlow<Boolean> = _followersFailed.asStateFlow()

    fun show(initialTabIndex: Int) {
        _selectedTab.value = initialTabIndex.coerceIn(0, 1)
        load()
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index.coerceIn(0, 1)
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _followingFailed.value = false
            _followersFailed.value = false
            val followingResult = withContext(Dispatchers.IO) { userRepository.getMyFollowing() }
            val followersResult = withContext(Dispatchers.IO) { userRepository.getMyFollowers() }
            followingResult.fold(
                onSuccess = {
                    _following.value = it
                    _followingFailed.value = false
                },
                onFailure = {
                    _following.value = emptyList()
                    _followingFailed.value = true
                },
            )
            followersResult.fold(
                onSuccess = {
                    _followers.value = it
                    _followersFailed.value = false
                },
                onFailure = {
                    _followers.value = emptyList()
                    _followersFailed.value = true
                },
            )
            _isLoading.value = false
        }
    }
}
