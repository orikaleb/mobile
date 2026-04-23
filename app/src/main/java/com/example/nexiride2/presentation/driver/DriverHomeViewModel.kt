package com.example.nexiride2.presentation.driver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexiride2.domain.model.Driver
import com.example.nexiride2.domain.model.Route
import com.example.nexiride2.domain.repository.AuthRepository
import com.example.nexiride2.domain.repository.BusRepository
import com.example.nexiride2.domain.repository.DriverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DriverHomeUiState(
    val isLoading: Boolean = true,
    val driver: Driver? = null,
    /** Routes operated by the driver's bus / company (best-effort filter). */
    val myRoutes: List<Route> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class DriverHomeViewModel @Inject constructor(
    private val driverRepository: DriverRepository,
    private val busRepository: BusRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DriverHomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Bind to auth changes so the screen auto-refreshes / clears like the
        // passenger-side ViewModels after our login-state fix.
        authRepository.observeCurrentUser()
            .onEach { user ->
                if (user == null) {
                    _uiState.value = DriverHomeUiState(isLoading = false)
                } else {
                    refresh()
                }
            }
            .launchIn(viewModelScope)
    }

    fun refresh() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        val driver = driverRepository.getCurrentDriver().getOrNull()
        if (driver == null) {
            _uiState.value = DriverHomeUiState(
                isLoading = false,
                error = "This account isn't registered as a driver."
            )
            return@launch
        }
        // Pull the popular routes and filter down to anything matching the
        // driver's bus registration or company. This is coarse but good enough
        // for a first pass until admin assigns routes explicitly.
        val popular = busRepository.getPopularRoutes().getOrElse { emptyList() }
        val mine = popular.filter { r ->
            val matchesBus = driver.assignedBusNumber?.let { num ->
                r.bus.busNumber?.equals(num, ignoreCase = true) == true
            } ?: false
            val matchesCompany = driver.companyName.isNotBlank() &&
                r.bus.companyName.equals(driver.companyName, ignoreCase = true)
            matchesBus || matchesCompany
        }
        _uiState.value = DriverHomeUiState(
            isLoading = false,
            driver = driver,
            myRoutes = mine
        )
    }

    fun signOut() = viewModelScope.launch {
        authRepository.logout()
    }
}
