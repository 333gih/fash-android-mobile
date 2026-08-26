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

    /**
     * Persisted snapshot is enough to paint the first frame — do not block Home on GET /app/status.
     */
    private val _ready = MutableStateFlow(true)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _pendingResume = MutableStateFlow<MaintenanceResumePresentation?>(null)
    val pendingResume: StateFlow<MaintenanceResumePresentation?> = _pendingResume.asStateFlow()

    init {
        if (_status.value.sawRestricted) {
            sawRestrictedThisSession = true
        }
    }

    fun apply(next: AppMaintenanceStatus) {
        applyInternal(next, fromNetwork = true)
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
        val result = runCatching {
            withContext(Dispatchers.IO) { repository.fetch().getOrThrow() }
        }
        result.onSuccess { applyInternal(it, fromNetwork = true) }
        result.onFailure {
            _ready.value = true
            // Keep last snapshot (including persisted lock/warning). Never fail-open to Home.
        }
    }

    private fun applyInternal(next: AppMaintenanceStatus, fromNetwork: Boolean) {
        val prev = _status.value
        _status.value = next
        _ready.value = true
        if (next.sawRestricted) {
            sawRestrictedThisSession = true
        }
        if (fromNetwork) {
            maybeQueueResume(prev, next)
        }
        persistSnapshot(next)
    }

    private fun persistSnapshot(next: AppMaintenanceStatus) {
        val phase = when {
            next.isLocked -> "maintenance"
            next.isWarning -> "warning"
            else -> "open"
        }
        prefs.edit()
            .putBoolean(KEY_LAST_ON, next.isLocked)
            .putString(KEY_LAST_PHASE, phase)
            .putString(KEY_STARTS_AT, next.startsAtIso)
            .putInt(KEY_COUNTDOWN, next.countdownSeconds)
            .putString(KEY_TITLE, next.title)
            .putString(KEY_MESSAGE, next.message)
            .apply()
    }

    private fun maybeQueueResume(prev: AppMaintenanceStatus, next: AppMaintenanceStatus) {
        if (!sawRestrictedThisSession) return
        val moment = next.inferredResumeMoment(prev) ?: return
        val token = next.resumeDedupeToken(prev)
        if (token.isEmpty() || hasSeenResume(token)) return
        _pendingResume.value = MaintenanceResumePresentation(
            moment = moment,
            releaseNotesTitle = next.resumeTitle(prev),
            releaseNotes = next.resumeBody(prev),
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
        val startsAt = prefs.getString(KEY_STARTS_AT, null)?.trim()?.ifEmpty { null }
        val countdown = prefs.getInt(KEY_COUNTDOWN, 0)
        val title = prefs.getString(KEY_TITLE, null)?.trim()?.ifEmpty { null }
        val message = prefs.getString(KEY_MESSAGE, null)?.trim()?.ifEmpty { null }
        val locked = prefs.getBoolean(KEY_LAST_ON, false) || phase.equals("maintenance", ignoreCase = true)
        if (locked) {
            return AppMaintenanceStatus(
                maintenance = true,
                phase = "maintenance",
                mode = "none",
                startsAtIso = startsAt,
                countdownSeconds = countdown,
                title = title,
                message = message,
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
                startsAtIso = startsAt,
                countdownSeconds = countdown,
                title = title,
                message = message,
                updatedAtIso = null,
                resumeMoment = null,
                releaseNotesTitle = null,
                releaseNotes = null,
            )
        }
        return AppMaintenanceStatus.Open
    }

    companion object {
        const val FCM_TOPIC = "fash_app_status"
        private const val KEY_LAST_ON = "maintenance_last_on"
        private const val KEY_LAST_PHASE = "maintenance_last_phase"
        private const val KEY_STARTS_AT = "maintenance_starts_at"
        private const val KEY_COUNTDOWN = "maintenance_countdown"
        private const val KEY_TITLE = "maintenance_title"
        private const val KEY_MESSAGE = "maintenance_message"
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
