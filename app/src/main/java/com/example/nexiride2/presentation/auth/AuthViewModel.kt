package com.example.nexiride2.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexiride2.domain.model.User
import com.example.nexiride2.domain.repository.AuthRepository
import com.example.nexiride2.domain.repository.DriverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    /**
     * True when the signed-in account has a matching `drivers/{uid}` document.
     * Controls whether the splash screen routes to the passenger home or the
     * driver portal on app restart.
     */
    val isDriver: Boolean = false,
    val otpSent: Boolean = false,
    val otpVerified: Boolean = false,
    val passwordReset: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val driverRepository: DriverRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState(isLoggedIn = authRepository.isLoggedIn(), user = authRepository.getCurrentUser()))
    val uiState = _uiState.asStateFlow()

    init {
        // Mirror the true Firebase auth state into the UI state so that any
        // external change (logout from another device, token expiry, etc.) flips
        // the nav gate correctly and downstream ViewModels can react too.
        authRepository.observeCurrentUser()
            .onEach { user ->
                _uiState.value = _uiState.value.copy(
                    user = user,
                    isLoggedIn = user != null,
                    isDriver = if (user == null) false else _uiState.value.isDriver
                )
            }
            .launchIn(viewModelScope)

        // In parallel, track whether the signed-in account is a driver so the
        // splash can route bus drivers straight to the driver portal instead
        // of the passenger home after an app restart.
        driverRepository.observeCurrentDriver()
            .onEach { driver ->
                _uiState.value = _uiState.value.copy(isDriver = driver != null)
            }
            .launchIn(viewModelScope)
    }

    fun login(email: String, password: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        authRepository.login(email, password).fold(
            onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, user = it, isLoggedIn = true) },
            onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        )
    }

    fun signUp(name: String, email: String, phone: String, password: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        authRepository.signUp(name, email, phone, password).fold(
            onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, user = it, isLoggedIn = true) },
            onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        )
    }

    fun forgotPassword(email: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        authRepository.forgotPassword(email).fold(
            onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, otpSent = true) },
            onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        )
    }

    fun verifyOtp(email: String, otp: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        authRepository.verifyOtp(email, otp).fold(
            onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, otpVerified = it, error = if (!it) "Invalid OTP" else null) },
            onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        )
    }

    fun resetPassword(email: String, newPassword: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        authRepository.resetPassword(email, newPassword).fold(
            onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, passwordReset = true) },
            onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        )
    }

    fun logout() = viewModelScope.launch {
        authRepository.logout()
        _uiState.value = AuthUiState()
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
