package com.pc.fash_android_mobile.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pc.fash_android_mobile.data.auth.AuthTokenRefreshCoordinator
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
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

class ChangePasswordViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val userRepository: UserRepository =
        (application as FashApplication).userRepository
    private val authManager =
        (application as FashApplication).authManager

    private val _currentPassword = MutableStateFlow("")
    val currentPassword: StateFlow<String> = _currentPassword.asStateFlow()

    private val _newPassword = MutableStateFlow("")
    val newPassword: StateFlow<String> = _newPassword.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun onCurrentPasswordChange(value: String) {
        _currentPassword.value = value.take(72)
    }

    fun onNewPasswordChange(value: String) {
        _newPassword.value = value.take(72)
    }

    fun onConfirmPasswordChange(value: String) {
        _confirmPassword.value = value.take(72)
    }

    fun canSubmit(): Boolean {
        val cur = _currentPassword.value
        val n = _newPassword.value
        val c = _confirmPassword.value
        return cur.isNotBlank() && n.length in 8..72 && n == c
    }

    fun submit() {
        if (!canSubmit()) return
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    userRepository.putUserPassword(_newPassword.value, _currentPassword.value)
                }
                result.fold(
                    onSuccess = {
                        withContext(Dispatchers.IO) {
                            AuthTokenRefreshCoordinator.refreshIfStillCurrent(
                                authManager.sessionStore,
                                authManager.authRepository,
                                "",
                            )
                        }
                        _currentPassword.value = ""
                        _newPassword.value = ""
                        _confirmPassword.value = ""
                        _events.tryEmit(getApplication<Application>().getString(R.string.password_change_success))
                    },
                    onFailure = { e ->
                        _events.tryEmit(mapPasswordError(e))
                    },
                )
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    private fun mapPasswordError(e: Throwable): String {
        val raw = e.message.orEmpty()
        val app = getApplication<Application>()
        return when {
            raw == "PASSWORD_LENGTH" || raw.contains("PASSWORD_LENGTH") ->
                app.getString(R.string.password_error_length)
            raw.contains("INVALID_CURRENT_PASSWORD") ->
                app.getString(R.string.password_error_invalid_current)
            raw.contains("CURRENT_PASSWORD_REQUIRED") ->
                app.getString(R.string.password_error_current_required)
            else -> raw.ifBlank { app.getString(R.string.password_change_error_generic) }
        }
    }
}
