package com.pc.fash_android_mobile.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.common.CommonAestheticTagDto
import com.pc.fash_android_mobile.data.common.displayLabel
import com.pc.fash_android_mobile.data.locale.AppLocale
import com.pc.fash_android_mobile.data.user.AestheticTagPutItem
import com.pc.fash_android_mobile.data.user.ProfileInfo
import com.pc.fash_android_mobile.data.user.ProfilePatch
import com.pc.fash_android_mobile.data.user.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs

private const val BIO_MAX_LENGTH = 150
private const val DISPLAY_NAME_MAX_LENGTH = 50
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

    private val _selectedTagIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedTagIds: StateFlow<Set<String>> = _selectedTagIds.asStateFlow()

    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()

    private val _coverImageUrl = MutableStateFlow<String?>(null)
    val coverImageUrl: StateFlow<String?> = _coverImageUrl.asStateFlow()

    private val _tags = MutableStateFlow<List<CommonAestheticTagDto>>(emptyList())
    val tags: StateFlow<List<CommonAestheticTagDto>> = _tags.asStateFlow()

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

    /** Clothing gender preference: "women"|"men"|"non_binary"|"prefer_not_to_say"|"". */
    private val _gender = MutableStateFlow("")
    val gender: StateFlow<String> = _gender.asStateFlow()

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

    /** Bumped on every editable field change so [canSave] recomputes reactively. */
    private val _formEpoch = MutableStateFlow(0)

    /** Reactive save eligibility — UI must collect this instead of calling [canSave] directly. */
    val canSave: StateFlow<Boolean> = combine(
        _formEpoch,
        _profile,
        _usernameAvailable,
        _isSubmitting,
    ) { _, _, usernameAvailable, isSubmitting ->
        evaluateCanSave(usernameAvailable, isSubmitting)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var usernameCheckJob: Job? = null

    private fun touchForm() {
        _formEpoch.value++
    }

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
                    _displayName.value = p.displayName.take(DISPLAY_NAME_MAX_LENGTH)
                    _username.value = p.username
                    _bio.value = p.bio.take(BIO_MAX_LENGTH)
                    _selectedTagIds.value = emptySet()
                    _avatarUrl.value = p.avatarUrl.takeIf { it.isNotBlank() }
                    _coverImageUrl.value = p.coverImageUrl.takeIf { it.isNotBlank() }
                    _referenceSize.value = p.referenceSize.orEmpty()
                    _measurementUnit.value = normalizeUnitFromProfile(p.referenceMeasurementUnit)
                    _measurementHem.value = formatMeasurement(p.referenceMeasurementHem)
                    _measurementChest.value = formatMeasurement(p.referenceMeasurementChest)
                    _measurementLength.value = formatMeasurement(p.referenceMeasurementLength)
                    _measurementShoulders.value = formatMeasurement(p.referenceMeasurementShoulders)
                    _measurementSleeve.value = formatMeasurement(p.referenceMeasurementSleeveLength)
                    _gender.value = normalizeGender(p.gender)
                    loadTags()
                    if (_tags.value.isNotEmpty()) applyInitialTagSelection()
                    touchForm()
                },
                onFailure = {
                    _events.tryEmit(
                        getApplication<Application>().getString(R.string.profile_load_error),
                    )
                },
            )
        }
    }

    private fun normalizeGender(raw: String): String =
        raw.trim().lowercase(Locale.ROOT)

    private fun normalizeUnitFromProfile(raw: String?): String {
        val t = raw?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return when (t) {
            "in", "inch", "inches" -> "in"
            "st", "stone" -> "st"
            "cm", "" -> "cm"
            else -> t.ifEmpty { "cm" }
        }
    }

    private fun formatMeasurement(d: Double?): String {
        if (d == null || abs(d) < 1e-9) return ""
        val v = d
        return if (abs(v - v.toInt().toDouble()) < 1e-6) {
            v.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", v).trimEnd('0').trimEnd('.')
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                commonServiceRepository.getAestheticTags(all = true)
            }
            result.fold(
                onSuccess = {
                    _tags.value = it
                    applyInitialTagSelection()
                },
                onFailure = { },
            )
        }
    }

    private fun applyInitialTagSelection() {
        val p = _profile.value ?: return
        val catalog = _tags.value
        if (p.aestheticTagSnapshots.isNotEmpty()) {
            _selectedTagIds.value = p.aestheticTagSnapshots.map { it.id }.filter { it.isNotEmpty() }.toSet()
            return
        }
        if (p.aestheticTags.isNotEmpty() && catalog.isNotEmpty()) {
            _selectedTagIds.value = catalog.filter { t ->
                p.aestheticTags.any { n ->
                    n.equals(t.name, ignoreCase = true) ||
                        n.equals(t.displayName, ignoreCase = true) ||
                        n.equals(t.displayNameVi, ignoreCase = true)
                }
            }.map { it.id }.toSet()
        }
    }

    fun resolveTagLabel(tagId: String): String {
        val isVi = AppLocale.currentTag(getApplication()) != AppLocale.TAG_EN
        val fromCatalog = _tags.value.find { it.id == tagId }
        if (fromCatalog != null) {
            return fromCatalog.displayLabel(isVi)
        }
        return _profile.value?.aestheticTagSnapshots?.find { it.id == tagId }?.name?.ifBlank { tagId }
            ?: tagId
    }

    fun onDisplayNameChange(value: String) {
        _displayName.value = value.take(DISPLAY_NAME_MAX_LENGTH)
        touchForm()
    }

    fun onUsernameChange(value: String) {
        val normalized = value
            .lowercase()
            .replace(Regex("[^a-z0-9_.]"), "")
            .take(30)
        _username.value = normalized
        _usernameAvailable.value = null
        touchForm()

        usernameCheckJob?.cancel()
        usernameCheckJob = viewModelScope.launch {
            delay(USERNAME_CHECK_DEBOUNCE_MS)
            val u = normalized.trim()
            if (u.length !in 3..30) {
                _usernameAvailable.value = false
                touchForm()
                return@launch
            }
            val original = _profile.value?.username?.lowercase()?.trim()
            if (u == original) {
                _usernameAvailable.value = true
                touchForm()
                return@launch
            }
            _isCheckingUsername.value = true
            val r = withContext(Dispatchers.IO) {
                userRepository.checkUsername(u)
            }
            _isCheckingUsername.value = false
            _usernameAvailable.value = r.getOrElse { false }
            touchForm()
        }
    }

    fun onBioChange(value: String) {
        _bio.value = value.take(BIO_MAX_LENGTH)
        touchForm()
    }

    fun onReferenceSizeChange(value: String) {
        _referenceSize.value = value
        touchForm()
    }

    fun onMeasurementUnitChange(value: String) {
        _measurementUnit.value = value
        touchForm()
    }

    fun onMeasurementHemChange(value: String) {
        _measurementHem.value = filterMeasurementInput(value)
        touchForm()
    }

    fun onMeasurementChestChange(value: String) {
        _measurementChest.value = filterMeasurementInput(value)
        touchForm()
    }

    fun onMeasurementLengthChange(value: String) {
        _measurementLength.value = filterMeasurementInput(value)
        touchForm()
    }

    fun onMeasurementShouldersChange(value: String) {
        _measurementShoulders.value = filterMeasurementInput(value)
        touchForm()
    }

    fun onMeasurementSleeveChange(value: String) {
        _measurementSleeve.value = filterMeasurementInput(value)
        touchForm()
    }

    fun onGenderChange(value: String) {
        _gender.value = normalizeGender(value)
        touchForm()
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

    fun toggleTag(tag: CommonAestheticTagDto) {
        val id = tag.id
        _selectedTagIds.value = if (id in _selectedTagIds.value) {
            _selectedTagIds.value - id
        } else {
            _selectedTagIds.value + id
        }
        touchForm()
    }

    fun removeTag(id: String) {
        _selectedTagIds.value = _selectedTagIds.value - id
        touchForm()
    }

    fun clearAllStyles() {
        _selectedTagIds.value = emptySet()
        touchForm()
    }

    fun setAvatarFromBytes(bytes: ByteArray, mimeType: String = "image/jpeg") {
        val ext = mimeTypeToExt(mimeType)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                userRepository.uploadProfileImage(bytes, "avatar.$ext", "avatar", mimeType)
            }
            result.fold(
                onSuccess = {
                    _avatarUrl.value = it
                    touchForm()
                },
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
                onSuccess = {
                    _coverImageUrl.value = it
                    touchForm()
                },
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

    private fun normalizeUnit(u: String): String {
        val t = u.trim().lowercase(Locale.ROOT)
        return when (t) {
            "in", "inch", "inches" -> "in"
            "st", "stone" -> "st"
            else -> if (t.isEmpty()) "cm" else t
        }
    }

    private fun buildPutItems(): List<AestheticTagPutItem> {
        val catalog = _tags.value.associateBy { it.id }
        return _selectedTagIds.value.mapNotNull { id ->
            catalog[id]?.let { t ->
                AestheticTagPutItem(t.id, t.name.ifBlank { t.displayName })
            } ?: _profile.value?.aestheticTagSnapshots?.find { it.id == id }?.takeIf { it.id.isNotEmpty() }
        }
    }

    /**
     * Server may return tag names only; [ProfileInfo.aestheticTagSnapshots] is then empty until we resolve via catalog.
     */
    private fun canonicalOriginalTags(p: ProfileInfo): List<AestheticTagPutItem> {
        if (p.aestheticTagSnapshots.isNotEmpty()) {
            return p.aestheticTagSnapshots.sortedBy { it.id }
        }
        if (p.aestheticTags.isEmpty() || _tags.value.isEmpty()) return emptyList()
        return _tags.value
            .filter { t ->
                p.aestheticTags.any { n ->
                    n.equals(t.name, ignoreCase = true) ||
                        n.equals(t.displayName, ignoreCase = true) ||
                        n.equals(t.displayNameVi, ignoreCase = true)
                }
            }
            .map { t -> AestheticTagPutItem(t.id, t.name.ifBlank { t.displayName }) }
            .sortedBy { it.id }
    }

    private fun tagsChanged(): Boolean {
        val p = _profile.value ?: return false
        val current = buildPutItems().sortedBy { it.id }
        return current != canonicalOriginalTags(p)
    }

    private fun doubleEq(a: Double?, b: Double?): Boolean {
        if (a == null && (b == null || abs(b) < 1e-9)) return true
        if (a == null || b == null) return false
        return abs(a - b) < 1e-3
    }

    private fun sizingChanged(): Boolean {
        val p = _profile.value ?: return false
        if (_referenceSize.value.trim() != (p.referenceSize ?: "")) return true
        if (normalizeUnit(_measurementUnit.value) != normalizeUnitFromProfile(p.referenceMeasurementUnit)) return true
        if (!doubleEq(parseMeasurementToDouble(_measurementChest.value), p.referenceMeasurementChest)) return true
        if (!doubleEq(parseMeasurementToDouble(_measurementHem.value), p.referenceMeasurementHem)) return true
        if (!doubleEq(parseMeasurementToDouble(_measurementLength.value), p.referenceMeasurementLength)) return true
        if (!doubleEq(parseMeasurementToDouble(_measurementShoulders.value), p.referenceMeasurementShoulders)) return true
        if (!doubleEq(parseMeasurementToDouble(_measurementSleeve.value), p.referenceMeasurementSleeveLength)) return true
        return false
    }

    fun hasChanges(): Boolean {
        val p = _profile.value ?: return false
        if (_displayName.value.trim() != p.displayName) return true
        if (_username.value.trim() != p.username) return true
        if (_bio.value != p.bio) return true
        if (tagsChanged()) return true
        if (_avatarUrl.value != (p.avatarUrl.takeIf { it.isNotBlank() })) return true
        if (_coverImageUrl.value != (p.coverImageUrl.takeIf { it.isNotBlank() })) return true
        if (sizingChanged()) return true
        if (normalizeGender(_gender.value) != normalizeGender(p.gender)) return true
        return false
    }

    private fun buildPatch(): ProfilePatch {
        val p = _profile.value ?: return ProfilePatch()
        val genderNorm = normalizeGender(_gender.value)
        val profileGenderNorm = normalizeGender(p.gender)
        return ProfilePatch(
            displayName = if (_displayName.value.trim() != p.displayName) _displayName.value.trim() else null,
            username = if (_username.value.trim() != p.username) _username.value.trim() else null,
            bio = if (_bio.value != p.bio) _bio.value else null,
            avatarUrl = if (_avatarUrl.value != p.avatarUrl.takeIf { it.isNotBlank() }) _avatarUrl.value else null,
            coverImageUrl = if (_coverImageUrl.value != p.coverImageUrl.takeIf { it.isNotBlank() }) _coverImageUrl.value else null,
            aestheticTags = if (tagsChanged()) buildPutItems() else null,
            gender = if (genderNorm != profileGenderNorm) genderNorm else null,
            referenceSize = if (sizingChanged()) _referenceSize.value.trim() else null,
            referenceMeasurementUnit = if (sizingChanged()) normalizeUnit(_measurementUnit.value) else null,
            referenceMeasurementChest = if (sizingChanged()) parseMeasurementToDouble(_measurementChest.value) else null,
            referenceMeasurementHem = if (sizingChanged()) parseMeasurementToDouble(_measurementHem.value) else null,
            referenceMeasurementLength = if (sizingChanged()) parseMeasurementToDouble(_measurementLength.value) else null,
            referenceMeasurementShoulders = if (sizingChanged()) parseMeasurementToDouble(_measurementShoulders.value) else null,
            referenceMeasurementSleeveLength = if (sizingChanged()) parseMeasurementToDouble(_measurementSleeve.value) else null,
        )
    }

    private fun evaluateCanSave(usernameAvailable: Boolean?, isSubmitting: Boolean): Boolean {
        if (isSubmitting) return false
        if (!hasChanges()) return false
        if (!isUsernameValid()) return false
        val u = _username.value.trim()
        val original = _profile.value?.username?.lowercase()?.trim()
        return if (u == original) true else (usernameAvailable == true)
    }

    fun save(onSuccess: () -> Unit) {
        if (!evaluateCanSave(_usernameAvailable.value, _isSubmitting.value) || _isSubmitting.value) return
        val patch = buildPatch()
        if (patch.isEmpty()) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = withContext(Dispatchers.IO) {
                userRepository.updateProfile(patch)
            }
            _isSubmitting.value = false
            result.fold(
                onSuccess = {
                    val put = buildPutItems()
                    val chest = parseMeasurementToDouble(_measurementChest.value)
                    val hem = parseMeasurementToDouble(_measurementHem.value)
                    val len = parseMeasurementToDouble(_measurementLength.value)
                    val sh = parseMeasurementToDouble(_measurementShoulders.value)
                    val sl = parseMeasurementToDouble(_measurementSleeve.value)
                    _profile.value = _profile.value?.copy(
                        displayName = _displayName.value.trim(),
                        username = _username.value.trim(),
                        bio = _bio.value,
                        avatarUrl = _avatarUrl.value ?: "",
                        coverImageUrl = _coverImageUrl.value ?: "",
                        aestheticTags = put.map { it.name },
                        aestheticTagSnapshots = put,
                        gender = normalizeGender(_gender.value),
                        referenceSize = _referenceSize.value.takeIf { it.isNotBlank() },
                        referenceMeasurementUnit = normalizeUnit(_measurementUnit.value),
                        referenceMeasurementChest = chest.takeIf { it > 0 },
                        referenceMeasurementHem = hem.takeIf { it > 0 },
                        referenceMeasurementLength = len.takeIf { it > 0 },
                        referenceMeasurementShoulders = sh.takeIf { it > 0 },
                        referenceMeasurementSleeveLength = sl.takeIf { it > 0 },
                    )
                    touchForm()
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
