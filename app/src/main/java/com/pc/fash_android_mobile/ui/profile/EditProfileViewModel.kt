package com.pc.fash_android_mobile.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.common.CommonAestheticTagDto
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val BIO_MAX_LENGTH = 150
private const val USERNAME_CHECK_DEBOUNCE_MS = 400L

class EditProfileViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val userRepository: UserRepository =
        (application as FashApplication).userRepository
    private val commonServiceRepository =
        (application as FashApplication).commonServiceRepository

    private val _profile = MutableStateFlow<ProfileInfo?>(null)
    val profile: StateFlow<ProfileInfo?> = _profile.asStateFlow()

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _bio = MutableStateFlow("")
    val bio: StateFlow<String> = _bio.asStateFlow()

    private val _selectedTagNames = MutableStateFlow<Set<String>>(emptySet())
    val selectedTagNames: StateFlow<Set<String>> = _selectedTagNames.asStateFlow()

    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()

    private val _coverImageUrl = MutableStateFlow<String?>(null)
    val coverImageUrl: StateFlow<String?> = _coverImageUrl.asStateFlow()

    private val _tags = MutableStateFlow<List<CommonAestheticTagDto>>(emptyList())
    val tags: StateFlow<List<CommonAestheticTagDto>> = _tags.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _usernameAvailable = MutableStateFlow<Boolean?>(null)
    val usernameAvailable: StateFlow<Boolean?> = _usernameAvailable.asStateFlow()

    private val _isCheckingUsername = MutableStateFlow(false)
    val isCheckingUsername: StateFlow<Boolean> = _isCheckingUsername.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var usernameCheckJob: Job? = null

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) {
                userRepository.getMeProfile()
            }
            _isLoading.value = false
            result.fold(
                onSuccess = { p ->
                    _profile.value = p
                    _displayName.value = p.displayName
                    _username.value = p.username
                    _bio.value = p.bio.take(BIO_MAX_LENGTH)
                    _selectedTagNames.value = p.aestheticTags.toSet()
                    _avatarUrl.value = p.avatarUrl.takeIf { it.isNotBlank() }
                    _coverImageUrl.value = p.coverImageUrl.takeIf { it.isNotBlank() }
                    loadTags()
                },
                onFailure = {
                    _events.tryEmit(
                        getApplication<Application>().getString(R.string.profile_load_error),
                    )
                },
            )
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                commonServiceRepository.getAestheticTags(all = true)
            }
            result.fold(
                onSuccess = { _tags.value = it },
                onFailure = { },
            )
        }
    }

    fun onDisplayNameChange(value: String) {
        _displayName.value = value
    }

    fun onUsernameChange(value: String) {
        val normalized = value
            .lowercase()
            .replace(Regex("[^a-z0-9_.]"), "")
            .take(30)
        _username.value = normalized
        _usernameAvailable.value = null

        usernameCheckJob?.cancel()
        usernameCheckJob = viewModelScope.launch {
            delay(USERNAME_CHECK_DEBOUNCE_MS)
            val u = normalized.trim()
            if (u.length !in 3..30) {
                _usernameAvailable.value = false
                return@launch
            }
            val original = _profile.value?.username?.lowercase()?.trim()
            if (u == original) {
                _usernameAvailable.value = true
                return@launch
            }
            _isCheckingUsername.value = true
            val r = withContext(Dispatchers.IO) {
                userRepository.checkUsername(u)
            }
            _isCheckingUsername.value = false
            _usernameAvailable.value = r.getOrElse { false }
        }
    }

    fun onBioChange(value: String) {
        _bio.value = value.take(BIO_MAX_LENGTH)
    }

    fun toggleTag(tag: CommonAestheticTagDto) {
        _selectedTagNames.value = if (_selectedTagNames.value.contains(tag.name)) {
            _selectedTagNames.value - tag.name
        } else {
            _selectedTagNames.value + tag.name
        }
    }

    fun setAvatarFromBytes(bytes: ByteArray, mimeType: String = "image/jpeg") {
        val ext = mimeTypeToExt(mimeType)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                userRepository.uploadProfileImage(bytes, "avatar.$ext", "avatar", mimeType)
            }
            result.fold(
                onSuccess = { _avatarUrl.value = it },
                onFailure = {
                    _events.tryEmit(
                        getApplication<Application>().getString(R.string.edit_profile_upload_error),
                    )
                },
            )
        }
    }

    fun setCoverFromBytes(bytes: ByteArray, mimeType: String = "image/jpeg") {
        val ext = mimeTypeToExt(mimeType)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                userRepository.uploadProfileImage(bytes, "cover.$ext", "cover", mimeType)
            }
            result.fold(
                onSuccess = { _coverImageUrl.value = it },
                onFailure = {
                    _events.tryEmit(
                        getApplication<Application>().getString(R.string.edit_profile_upload_error),
                    )
                },
            )
        }
    }

    private fun mimeTypeToExt(mimeType: String): String = when (mimeType.lowercase()) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        else -> "jpg"
    }

    fun isUsernameValid(): Boolean {
        val u = _username.value.trim()
        return u.length in 3..30 && u.matches(Regex("^[a-z0-9_.]+\$"))
    }

    fun hasChanges(): Boolean {
        val p = _profile.value ?: return false
        if (_displayName.value != p.displayName) return true
        if (_username.value.trim() != p.username) return true
        if (_bio.value != p.bio) return true
        val currentTags = _selectedTagNames.value
        val origTags = p.aestheticTags.toSet()
        if (currentTags != origTags) return true
        if (_avatarUrl.value != (p.avatarUrl.takeIf { it.isNotBlank() })) return true
        if (_coverImageUrl.value != (p.coverImageUrl.takeIf { it.isNotBlank() })) return true
        return false
    }

    fun canSave(): Boolean {
        if (!hasChanges()) return false
        if (!isUsernameValid()) return false
        val u = _username.value.trim()
        val original = _profile.value?.username?.lowercase()?.trim()
        return if (u == original) true else (_usernameAvailable.value == true)
    }

    fun save(onSuccess: () -> Unit) {
        if (!canSave() || _isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = withContext(Dispatchers.IO) {
                userRepository.updateProfile(
                    displayName = _displayName.value.trim(),
                    username = _username.value.trim(),
                    bio = _bio.value,
                    avatarUrl = _avatarUrl.value,
                    coverImageUrl = _coverImageUrl.value,
                    aestheticTags = _selectedTagNames.value.toList(),
                )
            }
            _isSubmitting.value = false
            result.fold(
                onSuccess = {
                    _profile.value = _profile.value?.copy(
                        displayName = _displayName.value.trim(),
                        username = _username.value.trim(),
                        bio = _bio.value,
                        avatarUrl = _avatarUrl.value ?: "",
                        coverImageUrl = _coverImageUrl.value ?: "",
                        aestheticTags = _selectedTagNames.value.toList(),
                    )
                    onSuccess()
                },
                onFailure = {
                    _events.tryEmit(
                        it.message ?: getApplication<Application>().getString(R.string.edit_profile_save_error),
                    )
                },
            )
        }
    }
}
