package com.pc.fash_android_mobile.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.pc.fash_android_mobile.BuildConfig
import com.pc.fash_android_mobile.FashApplication
import com.pc.fash_android_mobile.R
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.pc.fash_android_mobile.data.auth.AppAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val OTP_LENGTH = 6
private const val RESEND_COOLDOWN_SEC = 60

enum class LoginStep {
    Email,
    Otp,
}

class LoginViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val authManager: AppAuthManager =
        (application as FashApplication).authManager
    private val authRepository = authManager.authRepository
    private val sessionStore = authManager.sessionStore

    private val _loginStep = MutableStateFlow(LoginStep.Email)
    val loginStep: StateFlow<LoginStep> = _loginStep.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()

    private val _isOtpLoading = MutableStateFlow(false)
    val isOtpLoading: StateFlow<Boolean> = _isOtpLoading.asStateFlow()

    private val _isVerifyLoading = MutableStateFlow(false)
    val isVerifyLoading: StateFlow<Boolean> = _isVerifyLoading.asStateFlow()

    private val _isSocialLoading = MutableStateFlow(false)
    val isSocialLoading: StateFlow<Boolean> = _isSocialLoading.asStateFlow()

    private val _resendCooldownSec = MutableStateFlow(0)
    val resendCooldownSec: StateFlow<Int> = _resendCooldownSec.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _usePasswordLogin = MutableStateFlow(false)
    val usePasswordLogin: StateFlow<Boolean> = _usePasswordLogin.asStateFlow()

    private val _isPasswordLoading = MutableStateFlow(false)
    val isPasswordLoading: StateFlow<Boolean> = _isPasswordLoading.asStateFlow()

    private var resendCooldownJob: Job? = null

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    override fun onCleared() {
        resendCooldownJob?.cancel()
        super.onCleared()
    }

    fun onEmailChange(value: String) {
        _email.update { value }
    }

    fun onOtpChange(value: String) {
        _otpCode.value = value.filter { it.isDigit() }.take(OTP_LENGTH)
    }

    fun onPasswordChange(value: String) {
        _password.value = value
    }

    fun togglePasswordLogin() {
        _usePasswordLogin.value = !_usePasswordLogin.value
    }

    fun backFromOtp() {
        resendCooldownJob?.cancel()
        _resendCooldownSec.value = 0
        _otpCode.value = ""
        _loginStep.value = LoginStep.Email
    }

    fun requestEmailOtp() {
        postEmailOtpAndGoToOtpStep(resendOnly = false)
    }

    fun resendEmailOtp() {
        if (_resendCooldownSec.value > 0 || _isOtpLoading.value) return
        postEmailOtpAndGoToOtpStep(resendOnly = true)
    }

    fun loginWithPassword() {
        val app = getApplication<Application>()
        val rawEmail = _email.value.trim()
        val pwd = _password.value
        if (!isValidEmail(rawEmail)) {
            _events.tryEmit(app.getString(R.string.login_email_invalid))
            return
        }
        if (pwd.isBlank()) {
            _events.tryEmit(app.getString(R.string.login_password_invalid))
            return
        }
        viewModelScope.launch {
            _isPasswordLoading.value = true
            val result = withContext(Dispatchers.IO) {
                authRepository.login(rawEmail, pwd)
            }
            _isPasswordLoading.value = false
            result.fold(
                onSuccess = { session ->
                    sessionStore.save(session)
                    authManager.onSessionSaved()
                    _events.tryEmit(app.getString(R.string.otp_verify_success))
                    resetAfterVerified()
                },
                onFailure = { e ->
                    _events.tryEmit(
                        e.message?.takeIf { it.isNotBlank() }
                            ?: app.getString(R.string.login_password_failed),
                    )
                },
            )
        }
    }

    private fun postEmailOtpAndGoToOtpStep(resendOnly: Boolean) {
        val raw = _email.value.trim()
        val app = getApplication<Application>()
        if (!isValidEmail(raw)) {
            _events.tryEmit(app.getString(R.string.login_email_invalid))
            return
        }
        viewModelScope.launch {
            _isOtpLoading.value = true
            val result = withContext(Dispatchers.IO) {
                authRepository.requestEmailOtp(raw)
            }
            _isOtpLoading.value = false
            result.fold(
                onSuccess = {
                    if (resendOnly) {
                        _events.tryEmit(app.getString(R.string.login_otp_resent))
                    } else {
                        _events.tryEmit(app.getString(R.string.login_otp_sent))
                        _loginStep.value = LoginStep.Otp
                        _otpCode.value = ""
                    }
                    restartResendCooldown()
                },
                onFailure = { e ->
                    _events.tryEmit(
                        e.message?.takeIf { it.isNotBlank() }
                            ?: app.getString(R.string.login_otp_failed),
                    )
                },
            )
        }
    }

    fun verifyOtpCode() {
        val app = getApplication<Application>()
        val raw = _email.value.trim()
        val code = _otpCode.value.trim()
        if (code.length != OTP_LENGTH) {
            _events.tryEmit(app.getString(R.string.otp_invalid_length))
            return
        }
        viewModelScope.launch {
            _isVerifyLoading.value = true
            val result = withContext(Dispatchers.IO) {
                authRepository.verifyEmailOtp(raw, code)
            }
            _isVerifyLoading.value = false
            result.fold(
                onSuccess = { session ->
                    sessionStore.save(session)
                    authManager.onSessionSaved()
                    _events.tryEmit(app.getString(R.string.otp_verify_success))
                    resetAfterVerified()
                },
                onFailure = { e ->
                    _events.tryEmit(
                        e.message?.takeIf { it.isNotBlank() }
                            ?: app.getString(R.string.otp_verify_failed),
                    )
                },
            )
        }
    }

    private fun restartResendCooldown() {
        resendCooldownJob?.cancel()
        _resendCooldownSec.value = RESEND_COOLDOWN_SEC
        resendCooldownJob = viewModelScope.launch {
            while (_resendCooldownSec.value > 0) {
                delay(1_000)
                _resendCooldownSec.update { (it - 1).coerceAtLeast(0) }
            }
        }
    }

    private fun resetAfterVerified() {
        resendCooldownJob?.cancel()
        _resendCooldownSec.value = 0
        _otpCode.value = ""
        _loginStep.value = LoginStep.Email
    }

    fun onFacebookSuccess(result: LoginResult) {
        val app = getApplication<Application>()
        val token = result.accessToken?.token
        if (token.isNullOrBlank()) {
            _events.tryEmit(app.getString(R.string.login_facebook_error))
            return
        }
        viewModelScope.launch {
            _isSocialLoading.value = true
            val apiResult = withContext(Dispatchers.IO) {
                authRepository.socialLogin("facebook", token)
            }
            _isSocialLoading.value = false
            apiResult.fold(
                onSuccess = { session ->
                    sessionStore.save(session)
                    authManager.onSessionSaved()
                    _events.tryEmit(app.getString(R.string.login_facebook_success))
                },
                onFailure = { e ->
                    _events.tryEmit(
                        e.message?.takeIf { it.isNotBlank() }
                            ?: app.getString(R.string.login_facebook_error),
                    )
                },
            )
        }
    }

    fun onFacebookCancel() {
        // Silent cancel is fine; optional toast could be added.
    }

    fun onFacebookError(error: FacebookException) {
        val app = getApplication<Application>()
        _events.tryEmit(
            error.message?.takeIf { it.isNotBlank() }
                ?: app.getString(R.string.login_facebook_error),
        )
    }

    fun warnFacebookNotConfigured() {
        _events.tryEmit(getApplication<Application>().getString(R.string.login_facebook_not_configured))
    }

    /**
     * @param idToken Send this to your backend to verify and create a session (never trust the client alone).
     */
    fun onGoogleSignInSuccess(idToken: String?, email: String?) {
        val app = getApplication<Application>()
        if (idToken.isNullOrBlank()) {
            _events.tryEmit(app.getString(R.string.login_google_error))
            return
        }
        viewModelScope.launch {
            _isSocialLoading.value = true
            val result = withContext(Dispatchers.IO) {
                authRepository.socialLogin("google", idToken)
            }
            _isSocialLoading.value = false
            result.fold(
                onSuccess = { session ->
                    sessionStore.save(session)
                    authManager.onSessionSaved()
                    _events.tryEmit(app.getString(R.string.login_google_success))
                },
                onFailure = { e ->
                    _events.tryEmit(
                        e.message?.takeIf { it.isNotBlank() }
                            ?: app.getString(R.string.login_google_error),
                    )
                },
            )
        }
    }

    fun onGoogleSignInFailure(exception: Exception) {
        if (exception is ApiException) {
            if (exception.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) return
            val detail = exception.message?.takeIf { it.isNotBlank() }
            _events.tryEmit(
                detail ?: getApplication<Application>().getString(
                    R.string.login_google_error_code,
                    exception.statusCode,
                ),
            )
            return
        }
        _events.tryEmit(
            exception.message?.takeIf { it.isNotBlank() }
                ?: getApplication<Application>().getString(R.string.login_google_error),
        )
    }

    fun warnGoogleNotConfigured() {
        _events.tryEmit(getApplication<Application>().getString(R.string.login_google_not_configured))
    }

    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut: StateFlow<Boolean> = _isLoggingOut.asStateFlow()

    fun logout() {
        val app = getApplication<Application>()
        val session = sessionStore.read() ?: run {
            authManager.onSessionCleared()
            return
        }
        viewModelScope.launch {
            _isLoggingOut.value = true
            val result = withContext(Dispatchers.IO) {
                authManager.logout(session.accessToken)
            }
            _isLoggingOut.value = false
            result.fold(
                onSuccess = { _events.tryEmit(app.getString(R.string.logout_success)) },
                onFailure = { _events.tryEmit(app.getString(R.string.logout_failed)) },
            )
        }
    }

    fun logoutAll() {
        val app = getApplication<Application>()
        val session = sessionStore.read() ?: run {
            authManager.onSessionCleared()
            return
        }
        viewModelScope.launch {
            _isLoggingOut.value = true
            val result = withContext(Dispatchers.IO) {
                authManager.logoutAll(session.accessToken)
            }
            _isLoggingOut.value = false
            result.fold(
                onSuccess = { _events.tryEmit(app.getString(R.string.logout_success)) },
                onFailure = { _events.tryEmit(app.getString(R.string.logout_failed)) },
            )
        }
    }

    companion object {
        fun isFacebookConfigured(): Boolean {
            val id = BuildConfig.FACEBOOK_APP_ID.trim()
            if (id.isEmpty() || id == "0") return false
            if (id.equals("YOUR_FACEBOOK_APP_ID", ignoreCase = true)) return false
            val token = BuildConfig.FACEBOOK_CLIENT_TOKEN.trim()
            if (token.isEmpty() || token == "unset") return false
            return true
        }

        fun isGoogleConfigured(): Boolean {
            val id = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
            if (id.isEmpty()) return false
            if (id.equals("YOUR_GOOGLE_WEB_CLIENT_ID", ignoreCase = true)) return false
            return true
        }
    }
}

fun isValidEmail(email: String): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
