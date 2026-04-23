package com.example.nexiride2.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexiride2.data.firebase.FirestoreSeed
import com.example.nexiride2.domain.model.Driver
import com.example.nexiride2.domain.model.FleetBus
import com.example.nexiride2.domain.model.Route
import com.example.nexiride2.domain.model.User
import com.example.nexiride2.domain.repository.AdminBookingEntry
import com.example.nexiride2.domain.repository.AdminRepository
import com.example.nexiride2.domain.repository.AuthRepository
import com.example.nexiride2.domain.repository.BusRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Top-level tabs on the Admin console. */
enum class AdminTab(val label: String) {
    OVERVIEW("Overview"),
    USERS("Users"),
    BOOKINGS("Bookings"),
    FLEET("Fleet"),
    BROADCAST("Broadcast")
}

/** Form state for registering a new fleet bus from the admin screen. */
data class BusFormState(
    val busNumber: String = "",
    val companyName: String = "",
    val busType: String = "Standard",
    val totalSeats: String = "45"
) {
    fun isValid(): Boolean =
        busNumber.isNotBlank() && companyName.isNotBlank() &&
            busType.isNotBlank() && (totalSeats.toIntOrNull() ?: 0) > 0
}

data class AdminUiState(
    val isLoading: Boolean = true,
    val currentUser: User? = null,
    val selectedTab: AdminTab = AdminTab.OVERVIEW,
    val cities: List<String> = emptyList(),
    val popularRoutes: List<Route> = emptyList(),
    val users: List<User> = emptyList(),
    val bookings: List<AdminBookingEntry> = emptyList(),
    val drivers: List<Driver> = emptyList(),
    val buses: List<FleetBus> = emptyList(),
    val totalRevenueGhs: Double = 0.0,
    val isSeeding: Boolean = false,
    val isBroadcasting: Boolean = false,
    val isSavingBus: Boolean = false,
    val cancellingBookingId: String? = null,
    val mutatingDriverId: String? = null,
    val broadcastTitle: String = "",
    val broadcastMessage: String = "",
    val busForm: BusFormState = BusFormState(),
    val message: String? = null,
    val error: String? = null
)

/**
 * Backs the Admin screen. Hilt injects the admin-only cross-collection
 * repository; Firestore rules enforce that only allow-listed emails can
 * actually read/write through it.
 */
@HiltViewModel
class AdminViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val busRepository: BusRepository,
    private val adminRepository: AdminRepository,
    private val firestore: FirebaseFirestore,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun selectTab(tab: AdminTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun load() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        val current = authRepository.getCurrentUser()
        val cities = busRepository.getAvailableCities()
        val popular = busRepository.getPopularRoutes().getOrDefault(emptyList())

        // Collect failures as we go instead of silently swallowing them so the
        // admin actually sees why a collection is empty (e.g. permission denied
        // because rules haven't been pushed yet).
        val failures = mutableListOf<String>()

        val usersResult = adminRepository.listAllUsers()
        val users = usersResult.getOrElse {
            failures += "users: ${it.message ?: it.javaClass.simpleName}"
            emptyList()
        }
        val bookingsResult = adminRepository.listAllBookings()
        val bookings = bookingsResult.getOrElse {
            failures += "bookings: ${it.message ?: it.javaClass.simpleName}"
            emptyList()
        }
        val driversResult = adminRepository.listAllDrivers()
        val drivers = driversResult.getOrElse {
            failures += "drivers: ${it.message ?: it.javaClass.simpleName}"
            emptyList()
        }
        val busesResult = adminRepository.listAllBuses()
        val buses = busesResult.getOrElse {
            failures += "buses: ${it.message ?: it.javaClass.simpleName}"
            emptyList()
        }

        val revenue = bookings
            .filter { it.booking.status.name != "CANCELLED" }
            .sumOf { it.booking.totalPrice }
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            currentUser = current,
            cities = cities,
            popularRoutes = popular,
            users = users,
            bookings = bookings,
            drivers = drivers,
            buses = buses,
            totalRevenueGhs = revenue,
            error = if (failures.isEmpty()) null
            else "Some collections failed to load — ${failures.joinToString("; ")}"
        )
    }

    fun reseedData() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isSeeding = true, message = null, error = null)
        runCatching { FirestoreSeed.seedIfEmpty(firestore, gson) }
            .onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSeeding = false,
                    message = "Seed check complete (writes only when empty)"
                )
                load()
            }
            .onFailure {
                _uiState.value = _uiState.value.copy(
                    isSeeding = false,
                    error = it.message ?: "Seeding failed"
                )
            }
    }

    fun cancelBookingAsAdmin(bookingId: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(cancellingBookingId = bookingId, message = null, error = null)
        adminRepository.cancelBookingAsAdmin(bookingId).fold(
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    cancellingBookingId = null,
                    message = "Booking $bookingId cancelled"
                )
                refreshBookings()
            },
            onFailure = {
                _uiState.value = _uiState.value.copy(
                    cancellingBookingId = null,
                    error = it.message ?: "Cancel failed"
                )
            }
        )
    }

    fun updateBroadcastTitle(value: String) {
        _uiState.value = _uiState.value.copy(broadcastTitle = value)
    }

    fun updateBroadcastMessage(value: String) {
        _uiState.value = _uiState.value.copy(broadcastMessage = value)
    }

    fun sendBroadcast() = viewModelScope.launch {
        val title = _uiState.value.broadcastTitle.trim()
        val body = _uiState.value.broadcastMessage.trim()
        if (title.isEmpty() || body.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Both title and message are required")
            return@launch
        }
        _uiState.value = _uiState.value.copy(isBroadcasting = true, message = null, error = null)
        adminRepository.broadcastNotification(title, body).fold(
            onSuccess = { result ->
                _uiState.value = _uiState.value.copy(
                    isBroadcasting = false,
                    broadcastTitle = "",
                    broadcastMessage = "",
                    message = "Broadcast sent to ${result.recipients} user(s)"
                )
            },
            onFailure = {
                _uiState.value = _uiState.value.copy(
                    isBroadcasting = false,
                    error = it.message ?: "Broadcast failed"
                )
            }
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    private suspend fun refreshBookings() {
        val bookings = adminRepository.listAllBookings().getOrDefault(emptyList())
        val revenue = bookings
            .filter { it.booking.status.name != "CANCELLED" }
            .sumOf { it.booking.totalPrice }
        _uiState.value = _uiState.value.copy(
            bookings = bookings,
            totalRevenueGhs = revenue
        )
    }

    // ── Fleet management ─────────────────────────────────────────────────────

    fun updateBusForm(update: (BusFormState) -> BusFormState) {
        _uiState.value = _uiState.value.copy(busForm = update(_uiState.value.busForm))
    }

    fun registerBus() = viewModelScope.launch {
        val form = _uiState.value.busForm
        if (!form.isValid()) {
            _uiState.value = _uiState.value.copy(
                error = "Bus number, company, type, and seat count are required."
            )
            return@launch
        }
        _uiState.value = _uiState.value.copy(isSavingBus = true, message = null, error = null)
        val bus = FleetBus(
            id = "",
            busNumber = form.busNumber.trim(),
            companyName = form.companyName.trim(),
            busType = form.busType.trim(),
            totalSeats = form.totalSeats.toIntOrNull() ?: 0
        )
        adminRepository.upsertBus(bus).fold(
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    isSavingBus = false,
                    busForm = BusFormState(),
                    message = "Bus ${it.busNumber} registered"
                )
                refreshFleet()
            },
            onFailure = {
                _uiState.value = _uiState.value.copy(
                    isSavingBus = false,
                    error = it.message ?: "Could not save bus"
                )
            }
        )
    }

    fun deleteBus(busId: String) = viewModelScope.launch {
        adminRepository.deleteBus(busId).fold(
            onSuccess = {
                _uiState.value = _uiState.value.copy(message = "Bus removed")
                refreshFleet()
            },
            onFailure = {
                _uiState.value = _uiState.value.copy(
                    error = it.message ?: "Could not delete bus"
                )
            }
        )
    }

    fun setDriverActive(driver: Driver, active: Boolean) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(mutatingDriverId = driver.id, error = null)
        adminRepository.setDriverActive(driver.id, active).fold(
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    mutatingDriverId = null,
                    message = if (active) "${driver.fullName} enabled" else "${driver.fullName} disabled"
                )
                refreshFleet()
            },
            onFailure = {
                _uiState.value = _uiState.value.copy(
                    mutatingDriverId = null,
                    error = it.message ?: "Could not update driver"
                )
            }
        )
    }

    fun assignBusToDriver(driver: Driver, bus: FleetBus?) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(mutatingDriverId = driver.id, error = null)
        adminRepository.assignDriverBus(driver.id, bus).fold(
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    mutatingDriverId = null,
                    message = if (bus == null) "${driver.fullName}: bus cleared"
                    else "${driver.fullName} assigned to ${bus.busNumber}"
                )
                refreshFleet()
            },
            onFailure = {
                _uiState.value = _uiState.value.copy(
                    mutatingDriverId = null,
                    error = it.message ?: "Could not assign bus"
                )
            }
        )
    }

    private suspend fun refreshFleet() {
        val drivers = adminRepository.listAllDrivers().getOrDefault(emptyList())
        val buses = adminRepository.listAllBuses().getOrDefault(emptyList())
        _uiState.value = _uiState.value.copy(drivers = drivers, buses = buses)
    }
}
