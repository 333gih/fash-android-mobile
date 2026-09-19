package com.pc.fash_android_mobile.data.user

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-scoped in-memory store for the canonical own-user [ProfileInfo].
 * Written by [ProfileViewModel] and [PersonalizationViewModel] after every successful profile load or mutation.
 * Read by [HomeViewModel] (progress card, sizing banner) to avoid redundant getMeProfile() network calls.
 */
class UserProfileStore {
    private val _profile = MutableStateFlow<ProfileInfo?>(null)
    val profile: StateFlow<ProfileInfo?> = _profile.asStateFlow()

    fun update(profile: ProfileInfo) {
        _profile.value = profile
    }

    fun clear() {
        _profile.value = null
    }
}
