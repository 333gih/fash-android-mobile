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
    private val _status = MutableStateFlow(
        if (prefs.getBoolean(KEY_LAST_ON, false)) {
            AppMaintenanceStatus(maintenance = true, title = null, message = null)
        } else {
            AppMaintenanceStatus.Open
        },
    )
    val status: StateFlow<AppMaintenanceStatus> = _status.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    fun apply(next: AppMaintenanceStatus) {
        _status.value = next
        _ready.value = true
        prefs.edit().putBoolean(KEY_LAST_ON, next.maintenance).apply()
    }

    fun applyFromPushData(data: Map<String, String>): Boolean {
        val next = AppMaintenanceStatus.fromPushData(data) ?: return false
        apply(next)
        return true
    }

    suspend fun refresh() {
        val result = withContext(Dispatchers.IO) { repository.fetch() }
        result.onSuccess { apply(it) }
        result.onFailure { _ready.value = true }
    }

    companion object {
        private const val KEY_LAST_ON = "maintenance_last_on"
    }
}
