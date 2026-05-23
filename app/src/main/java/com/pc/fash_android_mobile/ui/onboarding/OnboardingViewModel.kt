package com.pc.fash_android_mobile.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.data.auth.AuthTokenRefreshCoordinator
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.config.AppEnvironment
import com.pc.fash_android_mobile.data.common.CommonAestheticTagDto
import com.pc.fash_android_mobile.data.user.AestheticTagPutItem
import com.pc.fash_android_mobile.data.user.ProfileInfo
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
    ShoppingPreferences,
    ProfilePhoto,
    SizingReference,
    UsernameOnboard,
    SetupPassword,
    /** All gates passed; host should hide onboarding (e.g. [UserAccessStatus.canAccessHome]). */
    Completed,
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
    private val authManager =
        (application as FashApplication).authManager

    private val _onboardingStep = MutableStateFlow(OnboardingStep.AestheticTags)
    val onboardingStep: StateFlow<OnboardingStep> = _onboardingStep.asStateFlow()

    private val _tags = MutableStateFlow<List<CommonAestheticTagDto>>(emptyList())
    val tags: StateFlow<List<CommonAestheticTagDto>> = _tags.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _setupPassword = MutableStateFlow("")
    val setupPassword: StateFlow<String> = _setupPassword.asStateFlow()

    private val _setupPasswordConfirm = MutableStateFlow("")
    val setupPasswordConfirm: StateFlow<String> = _setupPasswordConfirm.asStateFlow()

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

    private val _shoppingBuy = MutableStateFlow(true)
    val shoppingBuy: StateFlow<Boolean> = _shoppingBuy.asStateFlow()
    private val _shoppingSell = MutableStateFlow(false)
    val shoppingSell: StateFlow<Boolean> = _shoppingSell.asStateFlow()

    /**
     * Gender preference collected on the ShoppingPreferences step.
     * Maps to profile.gender on the server; empty = user didn't choose (prefer_not_to_say assumed).
     * Allowed values: "women" | "men" | "non_binary" | "prefer_not_to_say" | "" (not set).
     */
    private val _genderPreference = MutableStateFlow("")
    val genderPreference: StateFlow<String> = _genderPreference.asStateFlow()

    /** Optional height in cm (100–250). Empty string = not provided. */
    private val _heightCm = MutableStateFlow("")
    val heightCm: StateFlow<String> = _heightCm.asStateFlow()

    /** Optional weight in kg (20–300). Empty string = not provided. */
    private val _weightKg = MutableStateFlow("")
    val weightKg: StateFlow<String> = _weightKg.asStateFlow()

    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()

    private val _avatarUploading = MutableStateFlow(false)
    val avatarUploading: StateFlow<Boolean> = _avatarUploading.asStateFlow()

    /** Progress bar index (0 = empty track at first step). Updates when navigating forward or back. */
    private val _uiProgressStep = MutableStateFlow(0)
    val uiProgressStep: StateFlow<Int> = _uiProgressStep.asStateFlow()

    private val _progressTotalSteps = MutableStateFlow(OnboardingFlowProgress.TOTAL_STEPS)
    val progressTotalSteps: StateFlow<Int> = _progressTotalSteps.asStateFlow()

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
        viewModelScope.launch {
            val profile = withContext(Dispatchers.IO) {
                userRepository.getMeProfile().getOrNull()
            }
            profile?.let { applyProfileToState(it) }
            backStack.clear()
            val current = firstIncompleteStep(status)
            _onboardingStep.value = current
            seedBackStackForStep(current, status)
            syncProgressFromCurrentStep(status)
        }
    }

    private fun stepCompletionContext(status: UserAccessStatus): StepCompletionContext {
        val uid = currentUserId()
        return StepCompletionContext(
            status = status,
            skippedAestheticTags = onboardingLocalStore.skippedAestheticTags(uid),
            skippedProfilePhoto = onboardingLocalStore.skippedProfilePhoto(uid),
            skippedSizing = onboardingLocalStore.skippedSizing(uid),
            hasAvatar = !_avatarUrl.value.isNullOrBlank(),
            skipSizingEnv = AppEnvironment.skipSizingReferenceCompleted,
        )
    }

    private fun canonicalFlow(status: UserAccessStatus): List<OnboardingStep> =
        OnboardingFlowProgress.buildCanonicalSteps(
            includePassword = OnboardingFlowProgress.includesPasswordStep(status),
            includeSizing = !AppEnvironment.skipSizingReferenceCompleted,
        )

    private fun firstIncompleteStep(status: UserAccessStatus): OnboardingStep {
        val ctx = stepCompletionContext(status)
        return OnboardingFlowProgress.firstIncompleteStep(
            flow = canonicalFlow(status),
            status = ctx.status,
            skippedAestheticTags = ctx.skippedAestheticTags,
            skippedProfilePhoto = ctx.skippedProfilePhoto,
            skippedSizing = ctx.skippedSizing,
            hasAvatar = ctx.hasAvatar,
            skipSizingEnv = ctx.skipSizingEnv,
        )
    }

    private fun syncProgressFromCurrentStep(status: UserAccessStatus) {
        val flow = canonicalFlow(status)
        _progressTotalSteps.value = flow.size
        _uiProgressStep.value = OnboardingFlowProgress.progressDisplayIndex(_onboardingStep.value, flow)
    }

    /**
     * Setup gate says the user can use the main shell without this flow. Aligns [onboardingStep]
     * so shell chrome (promos / tour) is not blocked for returning users who never opened VM steps.
     */
    fun markProfileSetupGateSkippedForSession() {
        lastAccessStatus = null
        backStack.clear()
        _onboardingStep.value = OnboardingStep.Completed
    }

    private fun resolveNextStep(status: UserAccessStatus): OnboardingStep =
        firstIncompleteStep(status)

    private fun advanceAfterStatus(
        status: UserAccessStatus,
        completedStep: OnboardingStep?,
    ) {
        lastAccessStatus = status
        val prev = _onboardingStep.value
        val flow = canonicalFlow(status)
        val next = when {
            completedStep == OnboardingStep.UsernameOnboard -> OnboardingStep.Completed
            completedStep != null -> {
                OnboardingFlowProgress.nextStepInFlow(flow, completedStep)
                    ?: if (status.canAccessHome || status.onboardingDone) {
                        OnboardingStep.Completed
                    } else {
                        firstIncompleteStep(status)
                    }
            }
            else -> {
                OnboardingFlowProgress.nextStepInFlow(flow, prev)
                    ?: firstIncompleteStep(status)
            }
        }
        if (next != prev) {
            backStack.add(prev)
        }
        _onboardingStep.value = next
        syncProgressFromCurrentStep(status)
    }

    /** Steps the user can navigate back to before [current], including after app restart mid-flow. */
    private fun seedBackStackForStep(current: OnboardingStep, status: UserAccessStatus) {
        backStack.clear()
        backStack.addAll(priorStepsFor(current, status))
    }

    private fun priorStepsFor(current: OnboardingStep, status: UserAccessStatus): List<OnboardingStep> =
        OnboardingFlowProgress.buildPriorSteps(canonicalFlow(status), current)

    /**
     * Navigate to the previous onboarding step. Never signs the user out.
     * @return true when navigation happened; false on the first step (no-op).
     */
    fun goBack(): Boolean {
        if (backStack.isEmpty()) {
            val status = lastAccessStatus ?: return false
            seedBackStackForStep(_onboardingStep.value, status)
        }
        val previous = backStack.removeLastOrNull() ?: return false
        _onboardingStep.value = previous
        lastAccessStatus?.let { syncProgressFromCurrentStep(it) }
        hydrateSavedProgressFromServer()
        if (previous == OnboardingStep.AestheticTags && _tags.value.isEmpty()) {
            loadTags()
        }
        return true
    }

    /** @deprecated Use [goBack] — kept for call sites migrating off sign-out fallback. */
    fun handleBack(): Boolean = goBack()

    fun canNavigateBack(): Boolean {
        if (backStack.isNotEmpty()) return true
        val status = lastAccessStatus ?: return false
        return priorStepsFor(_onboardingStep.value, status).isNotEmpty()
    }

    fun hydrateSavedProgressFromServer() {
        viewModelScope.launch {
            val profile = withContext(Dispatchers.IO) {
                userRepository.getMeProfile().getOrNull()
            } ?: return@launch
            applyProfileToState(profile)
        }
    }

    private fun applyProfileToState(profile: ProfileInfo) {
        if (profile.aestheticTagSnapshots.isNotEmpty()) {
            _selectedIds.value = profile.aestheticTagSnapshots.map { it.id }.toSet()
        }
        if (profile.gender.isNotBlank()) {
            _genderPreference.value = profile.gender
        }
        if (profile.shoppingIntents.isNotEmpty()) {
            _shoppingBuy.value = profile.shoppingIntents.any { it.equals("buy", ignoreCase = true) }
            _shoppingSell.value = profile.shoppingIntents.any { it.equals("sell", ignoreCase = true) }
        }
        profile.referenceSize?.let { _referenceSize.value = it }
        profile.referenceMeasurementUnit?.let { onMeasurementUnitChange(it) }
        profile.referenceMeasurementChest?.let { _measurementChest.value = formatMeasurement(it) }
        profile.referenceMeasurementHem?.let { _measurementHem.value = formatMeasurement(it) }
        profile.referenceMeasurementLength?.let { _measurementLength.value = formatMeasurement(it) }
        profile.referenceMeasurementShoulders?.let { _measurementShoulders.value = formatMeasurement(it) }
        profile.referenceMeasurementSleeveLength?.let { _measurementSleeve.value = formatMeasurement(it) }
        profile.heightCm?.let { _heightCm.value = it.toString() }
        profile.weightKg?.let { _weightKg.value = formatMeasurement(it) }
        if (profile.username.isNotBlank() && _username.value.isBlank()) {
            _username.value = profile.username
        }
        if (profile.avatarUrl.isNotBlank()) {
            _avatarUrl.value = profile.avatarUrl
        }
    }

    private fun formatMeasurement(value: Double): String {
        val rounded = kotlin.math.round(value * 10.0) / 10.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toInt().toString()
        } else {
            rounded.toString()
        }
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

    fun onSetupPasswordChange(value: String) {
        _setupPassword.value = value.take(72)
    }

    fun onSetupPasswordConfirmChange(value: String) {
        _setupPasswordConfirm.value = value.take(72)
    }

    fun canSubmitSetupPassword(): Boolean {
        val p = _setupPassword.value
        val c = _setupPasswordConfirm.value
        return p.length in 8..72 && p == c
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

    private fun buildSizingRequest(): SizingReferenceRequest {
        val heightVal = _heightCm.value.trim().toIntOrNull()?.takeIf { it in 100..250 }
        val weightVal = _weightKg.value.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in 20.0..300.0 }
        return SizingReferenceRequest(
            referenceSize = _referenceSize.value.trim(),
            referenceMeasurementUnit = _measurementUnit.value,
            referenceMeasurementChest = parseMeasurementToDouble(_measurementChest.value),
            referenceMeasurementHem = parseMeasurementToDouble(_measurementHem.value),
            referenceMeasurementLength = parseMeasurementToDouble(_measurementLength.value),
            referenceMeasurementShoulders = parseMeasurementToDouble(_measurementShoulders.value),
            referenceMeasurementSleeveLength = parseMeasurementToDouble(_measurementSleeve.value),
            heightCm = heightVal,
            weightKg = weightVal,
        )
    }

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
                                passwordSet = null,
                                isChangePassword = null,
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

    fun toggleShoppingBuy() {
        _shoppingBuy.value = !_shoppingBuy.value
    }

    fun toggleShoppingSell() {
        _shoppingSell.value = !_shoppingSell.value
    }

    fun setGenderPreference(gender: String) {
        _genderPreference.value = gender
    }

    fun onHeightCmChange(value: String) { _heightCm.value = value }
    fun onWeightKgChange(value: String) { _weightKg.value = value }

    fun setAvatarFromBytes(bytes: ByteArray, mimeType: String = "image/jpeg") {
        val ext = when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        viewModelScope.launch {
            _avatarUploading.value = true
            try {
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
            } finally {
                _avatarUploading.value = false
            }
        }
    }

    fun canContinueFromProfilePhoto(): Boolean = !_avatarUrl.value.isNullOrBlank()

    fun completeProfilePhotoStep(onSuccess: () -> Unit) {
        if (!canContinueFromProfilePhoto()) {
            _events.tryEmit(getApplication<Application>().getString(R.string.onboarding_profile_photo_required))
            return
        }
        advanceAfterProfilePhoto(onSuccess)
    }

    fun skipProfilePhoto(onSuccess: () -> Unit) {
        val uid = currentUserId()
        if (uid.isNotBlank()) {
            onboardingLocalStore.setSkippedProfilePhoto(uid, true)
        }
        advanceAfterProfilePhoto(onSuccess)
    }

    private fun advanceAfterProfilePhoto(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val status = withContext(Dispatchers.IO) {
                    userRepository.getUserAccessStatus().getOrNull()
                }
                if (status != null) {
                    advanceAfterStatus(status, OnboardingStep.ProfilePhoto)
                }
                onSuccess()
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun submitShoppingPreferences(onSuccess: () -> Unit) {
        val intents = buildList {
            if (_shoppingBuy.value) add("buy")
            if (_shoppingSell.value) add("sell")
        }
        if (intents.isEmpty()) {
            _events.tryEmit(getApplication<Application>().getString(R.string.onboarding_shopping_error))
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val gender = _genderPreference.value.trim().lowercase().ifBlank { null }
                val result = withContext(Dispatchers.IO) {
                    userRepository.saveShoppingPreferences(shoppingIntents = intents, gender = gender)
                }
                result.fold(
                    onSuccess = {
                        val status = withContext(Dispatchers.IO) {
                            userRepository.getUserAccessStatus().getOrNull()
                        }
                        val base = status ?: lastAccessStatus
                            ?: UserAccessStatus(
                                hasProfile = false,
                                aestheticTagsConfigured = true,
                                onboardingDone = false,
                                sizingReferenceCompleted = false,
                                shoppingPreferencesConfigured = true,
                            )
                        advanceAfterStatus(
                            base.copy(shoppingPreferencesConfigured = true),
                            OnboardingStep.ShoppingPreferences,
                        )
                        onSuccess()
                    },
                    onFailure = {
                        _events.tryEmit(
                            getApplication<Application>().getString(R.string.onboarding_shopping_error),
                        )
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
                    advanceAfterStatus(status, OnboardingStep.AestheticTags)
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
                                passwordSet = null,
                                isChangePassword = null,
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
                    advanceAfterStatus(status, OnboardingStep.SizingReference)
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
        val fashApp = getApplication<FashApplication>()
        val refTok = fashApp.pendingReferralToken.value?.trim()?.takeIf { it.isNotEmpty() }
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val onboardResult = withContext(Dispatchers.IO) {
                    userRepository.onboard(u, selectedTags, refTok)
                }
                onboardResult.fold(
                    onSuccess = {
                        fashApp.pendingReferralToken.value = null
                        fashApp.pendingReferrerUsername.value = null
                        val status = withContext(Dispatchers.IO) {
                            userRepository.getUserAccessStatus().getOrNull()
                        }
                        val base = (status ?: lastAccessStatus
                            ?: UserAccessStatus(
                                hasProfile = false,
                                aestheticTagsConfigured = true,
                                onboardingDone = true,
                                sizingReferenceCompleted = true,
                                passwordSet = null,
                                isChangePassword = null,
                            )).copy(
                            onboardingDone = true,
                            hasProfile = true,
                        )
                        advanceAfterStatus(base, OnboardingStep.UsernameOnboard)
                        onSuccess()
                    },
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

    fun submitSetupPassword(onSuccess: () -> Unit) {
        if (!canSubmitSetupPassword()) return
        val pwd = _setupPassword.value
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val putResult = withContext(Dispatchers.IO) {
                    userRepository.putUserPassword(pwd, currentPassword = null)
                }
                putResult.fold(
                    onSuccess = {
                        _setupPassword.value = ""
                        _setupPasswordConfirm.value = ""
                        val status = withContext(Dispatchers.IO) {
                            AuthTokenRefreshCoordinator.refreshIfStillCurrent(
                                authManager.sessionStore,
                                authManager.authRepository,
                                "",
                            )
                            userRepository.getUserAccessStatus().getOrNull()
                        }
                        val base = status ?: lastAccessStatus?.copy(passwordSet = true, isChangePassword = false)
                            ?: UserAccessStatus(
                                hasProfile = true,
                                aestheticTagsConfigured = true,
                                onboardingDone = true,
                                sizingReferenceCompleted = true,
                                passwordSet = true,
                                isChangePassword = false,
                            )
                        advanceAfterStatus(base, OnboardingStep.SetupPassword)
                        onSuccess()
                    },
                    onFailure = { e ->
                        val raw = e.message.orEmpty()
                        val msg = when {
                            raw.contains("PASSWORD_LENGTH") ->
                                getApplication<Application>().getString(R.string.password_error_length)
                            raw.contains("INVALID_CURRENT_PASSWORD") ->
                                getApplication<Application>().getString(R.string.password_error_invalid_current)
                            raw.contains("CURRENT_PASSWORD_REQUIRED") ->
                                getApplication<Application>().getString(R.string.password_error_current_required)
                            else ->
                                raw.ifBlank {
                                    getApplication<Application>().getString(R.string.password_change_error_generic)
                                }
                        }
                        _events.tryEmit(msg)
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

private data class StepCompletionContext(
    val status: UserAccessStatus,
    val skippedAestheticTags: Boolean,
    val skippedProfilePhoto: Boolean,
    val skippedSizing: Boolean,
    val hasAvatar: Boolean,
    val skipSizingEnv: Boolean,
)
