package com.pc.fash_android_mobile.data.appstatus

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AppMaintenanceController(
    private val repository: AppStatusRepository,
) {
    private val _status = MutableStateFlow(AppMaintenanceStatus.Open)
    val status: StateFlow<AppMaintenanceStatus> = _status.asStateFlow()

    suspend fun refresh() {
        val result = withContext(Dispatchers.IO) { repository.fetch() }
        result.onSuccess { _status.value = it }
    }
}
