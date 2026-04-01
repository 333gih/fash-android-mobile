package com.pc.fash_android_mobile.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.common.CommonAestheticTagDto
import com.pc.fash_android_mobile.data.user.SizingReferenceRequest
import com.pc.fash_android_mobile.data.user.UserAccessStatus
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
    private val commonServiceRepository =
        (application as FashApplication).commonServiceRepository

    private val _onboardingStep = MutableStateFlow(OnboardingStep.StyleSelection)
    val onboardingStep: StateFlow<OnboardingStep> = _onboardingStep.asStateFlow()

    private val _tags = MutableStateFlow<List<CommonAestheticTagDto>>(emptyList())
    val tags: StateFlow<List<CommonAestheticTagDto>> = _tags.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _referenceSize = MutableStateFlow("")
    val referenceSize: StateFlow<String> = _referenceSize.asStateFlow()

    private val _measurementUnit = MutableStateFlow("cm")
    val measurementUnit: StateFlow<String> = _measurementUnit.asStateFlow()

    private val _measurementHem = MutableStateFlow("")
    val measurementHem: StateFlow<String> = _measurementHem.asStateFlow()

    private val _measurementChest = MutableStateFlow("")
    val measurementChest: StateFlow<String> = _measurementChest.asStateFlow()

    private val _measurementLength = MutableStateFlow("")
    val measurementLength: StateFlow<String> = _measurementLength.asStateFlow()

    private val _measurementShoulders = MutableStateFlow("")
    val measurementShoulders: StateFlow<String> = _measurementShoulders.asStateFlow()

    private val _measurementSleeve = MutableStateFlow("")
    val measurementSleeve: StateFlow<String> = _measurementSleeve.asStateFlow()

    /** After a successful [UserRepository.onboard], sizing PUT can be retried without re-posting onboard. */
    private var onboardSucceededPendingSizing: Boolean = false

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    /**
     * Align UI step with server flags: style tags first if not configured; else profile/sizing step.
     */
    fun applyInitialStepFromAccessStatus(status: UserAccessStatus) {
        val hint = status.nextStep?.lowercase().orEmpty()
        _onboardingStep.value = when {
            hint in setOf("profile", "sizing", "sizing_reference", "profile_setup") -> OnboardingStep.ProfileSetup
            hint in setOf("style", "tags", "aesthetic", "aesthetic_tags", "onboard") -> OnboardingStep.StyleSelection
            !status.aestheticTagsConfigured -> OnboardingStep.StyleSelection
            else -> OnboardingStep.ProfileSetup
        }
    }

    fun loadTags() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) {
                commonServiceRepository.getAestheticTags(all = true)
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

    fun toggleSelection(tag: CommonAestheticTagDto) {
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
        onboardSucceededPendingSizing = false
        _onboardingStep.value = OnboardingStep.StyleSelection
    }

    fun onUsernameChange(value: String) {
        onboardSucceededPendingSizing = false
        _username.value = value
            .lowercase()
            .replace(Regex("[^a-z0-9_.]"), "")
            .take(30)
    }

    fun onReferenceSizeChange(value: String) {
        _referenceSize.value = value.take(40)
    }

    fun onMeasurementUnitChange(unit: String) {
        val u = unit.lowercase().trim()
        _measurementUnit.value = if (u == "in") "in" else "cm"
    }

    fun onMeasurementHemChange(value: String) {
        _measurementHem.value = filterMeasurementInput(value)
    }

    fun onMeasurementChestChange(value: String) {
        _measurementChest.value = filterMeasurementInput(value)
    }

    fun onMeasurementLengthChange(value: String) {
        _measurementLength.value = filterMeasurementInput(value)
    }

    fun onMeasurementShouldersChange(value: String) {
        _measurementShoulders.value = filterMeasurementInput(value)
    }

    fun onMeasurementSleeveChange(value: String) {
        _measurementSleeve.value = filterMeasurementInput(value)
    }

    private fun filterMeasurementInput(raw: String): String {
        val b = StringBuilder()
        var dotSeen = false
        for (c in raw.replace(',', '.')) {
            when {
                c.isDigit() && b.length < 8 -> b.append(c)
                c == '.' && !dotSeen && b.isNotEmpty() -> {
                    b.append('.')
                    dotSeen = true
                }
            }
        }
        return b.toString()
    }

    private fun parseMeasurementToDouble(s: String): Double {
        val t = s.trim().replace(',', '.')
        if (t.isEmpty()) return 0.0
        return t.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    }

    fun isUsernameValid(): Boolean {
        val u = _username.value.trim()
        return u.length in 3..30 && u.matches(Regex("^[a-z0-9_.]+\$"))
    }

    fun isReferenceSizeValid(): Boolean = _referenceSize.value.trim().isNotEmpty()

    fun canSubmitProfile(): Boolean = isUsernameValid() && isReferenceSizeValid()

    fun getSelectedTagNames(): List<String> = _tags.value
        .filter { _selectedIds.value.contains(it.id) }
        .map { it.name }

    private fun buildSizingRequest(): SizingReferenceRequest =
        SizingReferenceRequest(
            referenceSize = _referenceSize.value.trim(),
            referenceMeasurementUnit = _measurementUnit.value,
            referenceMeasurementChest = parseMeasurementToDouble(_measurementChest.value),
            referenceMeasurementHem = parseMeasurementToDouble(_measurementHem.value),
            referenceMeasurementLength = parseMeasurementToDouble(_measurementLength.value),
            referenceMeasurementShoulders = parseMeasurementToDouble(_measurementShoulders.value),
            referenceMeasurementSleeveLength = parseMeasurementToDouble(_measurementSleeve.value),
        )

    fun submitOnboard(onSuccess: () -> Unit) {
        val u = _username.value.trim()
        if (!isUsernameValid() || !isReferenceSizeValid()) return
        val selectedTags = getSelectedTagNames()
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                if (!onboardSucceededPendingSizing) {
                    val onboardResult = withContext(Dispatchers.IO) {
                        userRepository.onboard(u, selectedTags)
                    }
                    onboardResult.fold(
                        onSuccess = { onboardSucceededPendingSizing = true },
                        onFailure = {
                            val msg = it.message?.takeIf { m -> m.isNotBlank() }
                                ?: getApplication<Application>().getString(R.string.onboarding_submit_error)
                            val displayMsg = if (msg.contains("409") || msg.contains("taken") || msg.contains("Username")) {
                                getApplication<Application>().getString(R.string.profile_setup_username_taken)
                            } else {
                                msg
                            }
                            _events.tryEmit(displayMsg)
                            return@launch
                        },
                    )
                }
                val sizingResult = withContext(Dispatchers.IO) {
                    userRepository.saveSizingReference(buildSizingRequest())
                }
                sizingResult.fold(
                    onSuccess = {
                        onboardSucceededPendingSizing = false
                        onSuccess()
                    },
                    onFailure = {
                        val msg = it.message?.takeIf { m -> m.isNotBlank() }
                            ?: getApplication<Application>().getString(R.string.onboarding_sizing_error)
                        _events.tryEmit(msg)
                    },
                )
            } finally {
                _isSubmitting.value = false
            }
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
