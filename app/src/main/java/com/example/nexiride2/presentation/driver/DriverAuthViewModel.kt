package com.example.nexiride2.presentation.driver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexiride2.domain.model.Driver
import com.example.nexiride2.domain.repository.AuthRepository
import com.example.nexiride2.domain.repository.DriverRepository
import com.example.nexiride2.notifications.FcmTokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DriverAuthUiState(
    val isLoading: Boolean = false,
    val driver: Driver? = null,
    val error: String? = null,
    /** Flips true once sign-up / login succeeds so the UI can navigate. */
    val signedIn: Boolean = false
)

/**
 * ViewModel for the driver portal (sign-in + sign-up). Kept separate from
 * [com.example.nexiride2.presentation.auth.AuthViewModel] so passenger-side
 * state transitions don't accidentally route a driver into the user home.
 */
@HiltViewModel
class DriverAuthViewModel @Inject constructor(
    private val driverRepository: DriverRepository,
    private val authRepository: AuthRepository,
    private val fcmTokenManager: FcmTokenManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(DriverAuthUiState())
    val uiState = _uiState.asStateFlow()

    fun signIn(email: String, password: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        driverRepository.loginDriver(email, password).fold(
            onSuccess = { d ->
                fcmTokenManager.registerCurrentDevice()
                _uiState.value = DriverAuthUiState(driver = d, signedIn = true)
            },
            onFailure = {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = it.message ?: "Could not sign in"
                )
            }
        )
    }

    fun signUp(
        fullName: String,
        email: String,
        phone: String,
        licenseNumber: String,
        companyName: String,
        busType: String,
        busCapacity: Int,
        serviceStations: List<String>,
        password: String
    ) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        driverRepository.signUpDriver(
            fullName = fullName,
            email = email,
            phone = phone,
            licenseNumber = licenseNumber,
            companyName = companyName,
            busType = busType,
            busCapacity = busCapacity,
            serviceStations = serviceStations,
            password = password
        ).fold(
            onSuccess = { d ->
                fcmTokenManager.registerCurrentDevice()
                _uiState.value = DriverAuthUiState(driver = d, signedIn = true)
            },
            onFailure = {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = it.message ?: "Could not register"
                )
            }
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun consumeSignedIn() {
        _uiState.value = _uiState.value.copy(signedIn = false)
    }

    fun signOut() = viewModelScope.launch {
        fcmTokenManager.unregisterCurrentDevice()
        authRepository.logout()
        _uiState.value = DriverAuthUiState()
    }
}
