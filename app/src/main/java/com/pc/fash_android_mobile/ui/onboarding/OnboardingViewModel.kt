package com.pc.fash_android_mobile.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.common.CommonAestheticTagDto
import com.pc.fash_android_mobile.data.user.AestheticTagPutItem
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

enum class OnboardingStep {
    AestheticTags,
    SizingReference,
    UsernameOnboard,
}

class OnboardingViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val userRepository: UserRepository =
        (application as FashApplication).userRepository
    private val commonServiceRepository =
        (application as FashApplication).commonServiceRepository
    private val onboardingLocalStore =
        (application as FashApplication).onboardingLocalStore
    private val sessionStore =
        (application as FashApplication).authManager.sessionStore

    private val _onboardingStep = MutableStateFlow(OnboardingStep.AestheticTags)
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

    private var lastAccessStatus: UserAccessStatus? = null
    private val backStack = mutableListOf<OnboardingStep>()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private fun currentUserId(): String = sessionStore.read()?.userId.orEmpty()

    /**
     * Align UI step with server flags and local skips. Clears back stack (cold entry / full refresh).
     */
    fun applyInitialStepFromAccessStatus(status: UserAccessStatus) {
        if (status.canAccessHome) return
        lastAccessStatus = status
        backStack.clear()
        _onboardingStep.value = resolveNextStep(status)
    }

    private fun resolveNextStep(status: UserAccessStatus): OnboardingStep {
        val uid = currentUserId()
        val skipSizingEnv = AppEnvironment.skipSizingReferenceCompleted
        return when {
            !status.aestheticTagsConfigured && !onboardingLocalStore.skippedAestheticTags(uid) ->
                OnboardingStep.AestheticTags
            !status.sizingReferenceCompleted && !skipSizingEnv && !onboardingLocalStore.skippedSizing(uid) ->
                OnboardingStep.SizingReference
            !status.onboardingDone ->
                OnboardingStep.UsernameOnboard
            else ->
                OnboardingStep.UsernameOnboard
        }
    }

    private fun advanceAfterStatus(
        status: UserAccessStatus,
        completedStep: OnboardingStep?,
    ) {
        lastAccessStatus = status
        val prev = _onboardingStep.value
        val next = resolveNextStep(status)
        if (completedStep != null && prev == completedStep) {
            when {
                completedStep == OnboardingStep.AestheticTags && next == OnboardingStep.SizingReference ->
                    backStack.add(OnboardingStep.AestheticTags)
                completedStep == OnboardingStep.SizingReference && next == OnboardingStep.UsernameOnboard ->
                    backStack.add(OnboardingStep.SizingReference)
            }
        }
        _onboardingStep.value = next
    }

    /**
     * @return true if navigated to previous step; false if caller should sign out / exit onboarding.
     */
    fun handleBack(): Boolean {
        val prev = backStack.removeLastOrNull() ?: return false
        _onboardingStep.value = prev
        return true
    }

    fun seedUsernameFromEmailIfEmpty(email: String) {
        if (_username.value.isBlank()) {
            _username.value = generateUsernameFromEmail(email)
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

    /** Optional step: may continue with zero tags (PUT empty) or use Skip to persist local only. */
    fun canContinueFromStyle(): Boolean = true

    fun onUsernameChange(value: String) {
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

    fun canSubmitSizing(): Boolean = isReferenceSizeValid()

    fun canSubmitUsername(): Boolean = isUsernameValid()

    fun getSelectedTagNames(): List<String> = _tags.value
        .filter { _selectedIds.value.contains(it.id) }
        .map { it.name }

    private fun buildSelectedTagPutItems(): List<AestheticTagPutItem> =
        _tags.value
            .filter { _selectedIds.value.contains(it.id) }
            .map { AestheticTagPutItem(id = it.id, name = it.name.ifBlank { it.displayName }) }

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

    fun submitAestheticTagsPut(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val putResult = withContext(Dispatchers.IO) {
                    userRepository.putUserAestheticTags(buildSelectedTagPutItems())
                }
                putResult.fold(
                    onSuccess = {
                        val status = withContext(Dispatchers.IO) {
                            userRepository.getUserAccessStatus().getOrNull()
                        }
                        // GET access-status often lags the PUT; merge success so we don't stay on this step.
                        val base = status ?: lastAccessStatus
                            ?: UserAccessStatus(
                                hasProfile = false,
                                aestheticTagsConfigured = true,
                                onboardingDone = false,
                                sizingReferenceCompleted = false,
                            )
                        advanceAfterStatus(
                            base.copy(aestheticTagsConfigured = true),
                            OnboardingStep.AestheticTags,
                        )
                        onSuccess()
                    },
                    onFailure = {
                        val msg = it.message?.takeIf { m -> m.isNotBlank() }
                            ?: getApplication<Application>().getString(R.string.onboarding_aesthetic_error)
                        _events.tryEmit(msg)
                    },
                )
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun skipAestheticTagsPersistLocal(onSuccess: () -> Unit) {
        val uid = currentUserId()
        if (uid.isNotBlank()) {
            onboardingLocalStore.setSkippedAestheticTags(uid, true)
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val status = withContext(Dispatchers.IO) {
                    userRepository.getUserAccessStatus().getOrNull()
                }
                if (status != null) {
                    advanceAfterStatus(status, null)
                }
                onSuccess()
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun submitSizingOnly(onSuccess: () -> Unit) {
        if (!canSubmitSizing()) return
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val sizingResult = withContext(Dispatchers.IO) {
                    userRepository.saveSizingReference(buildSizingRequest())
                }
                sizingResult.fold(
                    onSuccess = {
                        val status = withContext(Dispatchers.IO) {
                            userRepository.getUserAccessStatus().getOrNull()
                        }
                        val base = status ?: lastAccessStatus
                            ?: UserAccessStatus(
                                hasProfile = false,
                                aestheticTagsConfigured = true,
                                onboardingDone = false,
                                sizingReferenceCompleted = true,
                            )
                        advanceAfterStatus(
                            base.copy(sizingReferenceCompleted = true),
                            OnboardingStep.SizingReference,
                        )
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

    fun skipSizingPersistLocal(onSuccess: () -> Unit) {
        val uid = currentUserId()
        if (uid.isNotBlank()) {
            onboardingLocalStore.setSkippedSizing(uid, true)
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val status = withContext(Dispatchers.IO) {
                    userRepository.getUserAccessStatus().getOrNull()
                }
                if (status != null) {
                    advanceAfterStatus(status, null)
                }
                onSuccess()
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun submitUsernameOnboard(onSuccess: () -> Unit) {
        val u = _username.value.trim()
        if (!isUsernameValid()) return
        val selectedTags = getSelectedTagNames()
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val onboardResult = withContext(Dispatchers.IO) {
                    userRepository.onboard(u, selectedTags)
                }
                onboardResult.fold(
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
            } finally {
                _isSubmitting.value = false
            }
        }
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
