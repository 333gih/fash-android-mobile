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

    init {
        if (_status.value.sawRestricted) {
            sawRestrictedThisSession = true
        }
    }

    fun apply(next: AppMaintenanceStatus) {
        _status.value = next
        _ready.value = true
        if (next.sawRestricted) {
            sawRestrictedThisSession = true
        }
        prefs.edit()
            .putBoolean(KEY_LAST_ON, next.isLocked)
            .putString(KEY_LAST_PHASE, next.phase)
            .apply()
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
            )
        }
        return AppMaintenanceStatus.Open
    }

    companion object {
        private const val KEY_LAST_ON = "maintenance_last_on"
        private const val KEY_LAST_PHASE = "maintenance_last_phase"
    }
}
