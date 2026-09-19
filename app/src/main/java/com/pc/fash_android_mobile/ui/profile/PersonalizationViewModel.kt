package com.pc.fash_android_mobile.ui.profile

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.data.user.ProfilePatch
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PersonalizationViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository = (application as FashApplication).userRepository

    private val _profile = MutableStateFlow<ProfileInfo?>(null)
    val profile: StateFlow<ProfileInfo?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto.asStateFlow()

    private val _photoError = MutableStateFlow<String?>(null)
    val photoError: StateFlow<String?> = _photoError.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) { userRepository.getMeProfile() }
            _isLoading.value = false
            result.onSuccess { _profile.value = it }
        }
    }

    fun handlePhotoSelection(
        uri: Uri,
        contentResolver: ContentResolver,
        uploadErrorMessage: String,
    ) {
        viewModelScope.launch {
            _isUploadingPhoto.value = true
            _photoError.value = null
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                    val ext = if (mimeType.contains("png")) "png" else "jpg"
                    val bytes = contentResolver.openInputStream(uri)?.readBytes()
                        ?: error("Cannot read image")
                    val uploadResult = userRepository.uploadProfileImage(
                        bytes = bytes,
                        filename = "avatar.$ext",
                        type = "avatar",
                        mimeType = mimeType,
                    )
                    val url = uploadResult.getOrThrow()
                    userRepository.updateProfile(ProfilePatch(avatarUrl = url)).getOrThrow()
                    userRepository.getMeProfile().getOrThrow()
                }
            }
            result.onSuccess { _profile.value = it }
                .onFailure { _photoError.value = uploadErrorMessage }
            _isUploadingPhoto.value = false
        }
    }
}
