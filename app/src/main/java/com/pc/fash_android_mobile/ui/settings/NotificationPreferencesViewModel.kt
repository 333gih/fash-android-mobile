package com.pc.fash_android_mobile.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.pc.fash_android_mobile.data.user.NotificationPreferences
import com.pc.fash_android_mobile.data.user.NotificationPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationPreferencesViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository: NotificationPreferencesRepository =
        (application as FashApplication).notificationPreferencesRepository

    private val _prefs = MutableStateFlow<NotificationPreferences?>(null)
    val prefs: StateFlow<NotificationPreferences?> = _prefs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getNotificationPreferences()
                }
                result.fold(
                    onSuccess = { _prefs.value = it },
                    onFailure = {
                        _events.tryEmit(
                            getApplication<Application>().getString(R.string.notification_preferences_load_error),
                        )
                    },
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onRecommendationPushChanged(enabled: Boolean) {
        update { it.copy(recommendationPushEnabled = enabled) }
    }

    fun onRecommendationEmailChanged(enabled: Boolean) {
        update { it.copy(recommendationEmailEnabled = enabled) }
    }

    fun onQuietHoursEnabledChanged(enabled: Boolean) {
        update {
            if (enabled) {
                it.copy(quietHoursStart = it.quietHoursStart ?: 22, quietHoursEnd = it.quietHoursEnd ?: 8)
            } else {
                it.copy(quietHoursStart = null, quietHoursEnd = null)
            }
        }
    }

    fun onQuietHoursStartChanged(hour: Int) {
        update { it.copy(quietHoursStart = hour.coerceIn(0, 23)) }
    }

    fun onQuietHoursEndChanged(hour: Int) {
        update { it.copy(quietHoursEnd = hour.coerceIn(0, 23)) }
    }

    private fun update(transform: (NotificationPreferences) -> NotificationPreferences) {
        val current = _prefs.value ?: return
        val next = transform(current)
        _prefs.value = next
        persist(next)
    }

    private fun persist(next: NotificationPreferences) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.updateNotificationPreferences(next)
                }
                result.fold(
                    onSuccess = { _prefs.value = it },
                    onFailure = {
                        _events.tryEmit(
                            getApplication<Application>().getString(R.string.notification_preferences_save_error),
                        )
                        load()
                    },
                )
            } finally {
                _isSaving.value = false
            }
        }
    }
}
