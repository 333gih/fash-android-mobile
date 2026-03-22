package com.pc.fash_android_mobile.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.user.AestheticTag
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MIN_SELECTIONS = 3

enum class OnboardingStep {
    StyleSelection,
    ProfileSetup,
}

class OnboardingViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val userRepository: UserRepository =
        (application as FashApplication).userRepository

    private val _onboardingStep = MutableStateFlow(OnboardingStep.StyleSelection)
    val onboardingStep: StateFlow<OnboardingStep> = _onboardingStep.asStateFlow()

    private val _tags = MutableStateFlow<List<AestheticTag>>(emptyList())
    val tags: StateFlow<List<AestheticTag>> = _tags.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun loadTags() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) {
                userRepository.getAestheticTags()
            }
            _isLoading.value = false
            result.fold(
                onSuccess = { _tags.value = it },
                onFailure = {
                    _events.tryEmit(
                        getApplication<Application>().getString(R.string.onboarding_load_error),
                    )
                },
            )
        }
    }

    fun toggleSelection(tag: AestheticTag) {
        _selectedIds.value = if (_selectedIds.value.contains(tag.id)) {
            _selectedIds.value - tag.id
        } else {
            _selectedIds.value + tag.id
        }
    }

    fun canContinueFromStyle(): Boolean = _selectedIds.value.size >= MIN_SELECTIONS

    fun goToProfileSetup(suggestedUsername: String = "") {
        _username.value = suggestedUsername.ifBlank { generateUsernameFromEmail("") }
        _onboardingStep.value = OnboardingStep.ProfileSetup
    }

    fun goBackToStyle() {
        _onboardingStep.value = OnboardingStep.StyleSelection
    }

    fun onUsernameChange(value: String) {
        _username.value = value
            .lowercase()
            .replace(Regex("[^a-z0-9_.]"), "")
            .take(30)
    }

    fun isUsernameValid(): Boolean {
        val u = _username.value.trim()
        return u.length in 3..30 && u.matches(Regex("^[a-z0-9_.]+\$"))
    }

    fun getSelectedTagNames(): List<String> = _tags.value
        .filter { _selectedIds.value.contains(it.id) }
        .map { it.name }

    fun submitOnboard(onSuccess: () -> Unit) {
        val u = _username.value.trim()
        if (!isUsernameValid()) return
        val selectedTags = getSelectedTagNames()
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = withContext(Dispatchers.IO) {
                userRepository.onboard(u, selectedTags)
            }
            _isSubmitting.value = false
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = {
                    val msg = it.message?.takeIf { m -> m.isNotBlank() }
                        ?: getApplication<Application>().getString(R.string.onboarding_submit_error)
                    val displayMsg = if (msg.contains("409") || msg.contains("taken") || msg.contains("Username")) {
                        getApplication<Application>().getString(R.string.profile_setup_username_taken)
                    } else {
                        msg
                    }
                    _events.tryEmit(displayMsg)
                },
            )
        }
    }

    fun skipToProfileSetup(email: String) {
        _selectedIds.value = emptySet()
        _username.value = generateUsernameFromEmail(email)
        _onboardingStep.value = OnboardingStep.ProfileSetup
    }

    fun generateUsernameFromEmail(email: String): String {
        val prefix = email
            .substringBefore('@')
            .lowercase()
            .replace(Regex("[^a-z0-9_.]"), "")
            .take(20)
        return when {
            prefix.length >= 3 -> prefix
            prefix.isNotEmpty() -> "${prefix}_${(1000..9999).random()}"
            else -> "user_${(100000..999999).random()}"
        }
    }
}
