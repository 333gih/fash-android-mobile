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
    private var confirmedFromNetwork = false

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
            // Keep a persisted lock (user can retry). Only fail-open when we were not locked.
            if (!confirmedFromNetwork && !_status.value.isLocked) {
                applyInternal(AppMaintenanceStatus.Open, fromNetwork = false)
            }
        }
    }

    private fun applyInternal(next: AppMaintenanceStatus, fromNetwork: Boolean) {
        val prev = _status.value
        _status.value = next
        _ready.value = true
        if (fromNetwork) {
            confirmedFromNetwork = true
        }
        if (next.sawRestricted) {
            sawRestrictedThisSession = true
        }
        if (fromNetwork) {
            maybeQueueResume(prev, next)
        }
        persistSnapshot(next)
    }

    private fun persistSnapshot(next: AppMaintenanceStatus) {
        // Warning is a 60s in-session state — persisting it makes the next cold start look locked
        // after the countdown has elapsed.
        val persistLocked = next.isLocked
        prefs.edit()
            .putBoolean(KEY_LAST_ON, persistLocked)
            .putString(KEY_LAST_PHASE, if (persistLocked) "maintenance" else "open")
            .putString(KEY_STARTS_AT, next.startsAtIso)
            .putInt(KEY_COUNTDOWN, next.countdownSeconds)
            .apply()
    }

    private fun maybeQueueResume(prev: AppMaintenanceStatus, next: AppMaintenanceStatus) {
        if (!sawRestrictedThisSession) return
        val moment = next.inferredResumeMoment(prev) ?: return
        val token = next.resumeDedupeToken(prev)
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
        val startsAt = prefs.getString(KEY_STARTS_AT, null)?.trim()?.ifEmpty { null }
        val countdown = prefs.getInt(KEY_COUNTDOWN, 0)
        val locked = prefs.getBoolean(KEY_LAST_ON, false) || phase.equals("maintenance", ignoreCase = true)
        if (locked) {
            return AppMaintenanceStatus(
                maintenance = true,
                phase = "maintenance",
                mode = "none",
                startsAtIso = startsAt,
                countdownSeconds = countdown,
                title = null,
                message = null,
                updatedAtIso = null,
                resumeMoment = null,
                releaseNotesTitle = null,
                releaseNotes = null,
            )
        }
        // Stale warning snapshots from older builds must not lock the next launch.
        return AppMaintenanceStatus.Open
    }

    companion object {
        private const val KEY_LAST_ON = "maintenance_last_on"
        private const val KEY_LAST_PHASE = "maintenance_last_phase"
        private const val KEY_STARTS_AT = "maintenance_starts_at"
        private const val KEY_COUNTDOWN = "maintenance_countdown"
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
