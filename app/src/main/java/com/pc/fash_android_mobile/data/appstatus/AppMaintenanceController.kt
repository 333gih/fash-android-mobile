package com.pc.fash_android_mobile.data.appstatus

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AppMaintenanceController(
    private val repository: AppStatusRepository,
    private val prefs: SharedPreferences,
) {
    private var sawRestrictedThisSession = false

    private val _status = MutableStateFlow(loadPersistedOrOpen())
    val status: StateFlow<AppMaintenanceStatus> = _status.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _pendingResume = MutableStateFlow<MaintenanceResumePresentation?>(null)
    val pendingResume: StateFlow<MaintenanceResumePresentation?> = _pendingResume.asStateFlow()

    init {
        if (_status.value.sawRestricted) {
            sawRestrictedThisSession = true
        }
    }

    fun apply(next: AppMaintenanceStatus) {
        val prev = _status.value
        _status.value = next
        _ready.value = true
        if (next.sawRestricted) {
            sawRestrictedThisSession = true
        }
        maybeQueueResume(prev, next)
        prefs.edit()
            .putBoolean(KEY_LAST_ON, next.isLocked)
            .putString(KEY_LAST_PHASE, next.phase)
            .apply()
    }

    fun dismissResumePresentation() {
        _pendingResume.value?.updatedAtToken?.takeIf { it.isNotBlank() }?.let { markResumeSeen(it) }
        _pendingResume.value = null
    }

    fun applyFromPushData(data: Map<String, String>): Boolean {
        val next = AppMaintenanceStatus.fromPushData(data) ?: return false
        apply(next)
        return true
    }

    suspend fun refresh() {
        val result = withContext(Dispatchers.IO) { repository.fetch() }
        result.onSuccess { apply(it) }
        result.onFailure {
            _ready.value = true
            if (!sawRestrictedThisSession && !_status.value.sawRestricted) {
                apply(AppMaintenanceStatus.Open)
            }
        }
    }

    private fun maybeQueueResume(prev: AppMaintenanceStatus, next: AppMaintenanceStatus) {
        if (!sawRestrictedThisSession) return
        if (!prev.sawRestricted || next.sawRestricted) return
        val moment = next.resumeMoment?.trim().orEmpty()
        if (moment.isEmpty()) return
        val token = next.updatedAtIso?.trim().orEmpty()
        if (token.isEmpty() || hasSeenResume(token)) return
        _pendingResume.value = MaintenanceResumePresentation(
            moment = moment,
            releaseNotesTitle = next.releaseNotesTitle,
            releaseNotes = next.releaseNotes,
            updatedAtToken = token,
        )
    }

    private fun hasSeenResume(token: String): Boolean =
        prefs.getString(KEY_SEEN_RESUME, null) == token

    private fun markResumeSeen(token: String) {
        prefs.edit().putString(KEY_SEEN_RESUME, token).apply()
    }

    private fun loadPersistedOrOpen(): AppMaintenanceStatus {
        val phase = prefs.getString(KEY_LAST_PHASE, null)?.trim().orEmpty()
        val locked = prefs.getBoolean(KEY_LAST_ON, false) || phase.equals("maintenance", ignoreCase = true)
        if (locked) {
            return AppMaintenanceStatus(
                maintenance = true,
                phase = "maintenance",
                mode = "none",
                startsAtIso = null,
                countdownSeconds = 0,
                title = null,
                message = null,
                updatedAtIso = null,
                resumeMoment = null,
                releaseNotesTitle = null,
                releaseNotes = null,
            )
        }
        if (phase.equals("warning", ignoreCase = true)) {
            return AppMaintenanceStatus(
                maintenance = false,
                phase = "warning",
                mode = "none",
                startsAtIso = null,
                countdownSeconds = 0,
                title = null,
                message = null,
                updatedAtIso = null,
                resumeMoment = null,
                releaseNotesTitle = null,
                releaseNotes = null,
            )
        }
        return AppMaintenanceStatus.Open
    }

    companion object {
        private const val KEY_LAST_ON = "maintenance_last_on"
        private const val KEY_LAST_PHASE = "maintenance_last_phase"
        private const val KEY_SEEN_RESUME = "maintenance_seen_resume"
    }
}

data class MaintenanceResumePresentation(
    val moment: String,
    val releaseNotesTitle: String?,
    val releaseNotes: String?,
    val updatedAtToken: String,
) {
    val isWarningCleared: Boolean get() = moment == "warning_cleared"
    val isBackOnline: Boolean get() = moment == "back_online"

    val noteLines: List<String>
        get() {
            val raw = releaseNotes?.trim().orEmpty()
            if (raw.isEmpty()) return emptyList()
            return raw.lineSequence()
                .map { line -> line.trim().trimStart('-', '•', '*').trim() }
                .filter { it.isNotEmpty() }
                .toList()
        }
}
