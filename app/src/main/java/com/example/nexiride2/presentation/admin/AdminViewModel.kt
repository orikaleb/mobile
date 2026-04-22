package com.example.nexiride2.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexiride2.data.firebase.FirestoreSeed
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
    BROADCAST("Broadcast")
}

data class AdminUiState(
    val isLoading: Boolean = true,
    val currentUser: User? = null,
    val selectedTab: AdminTab = AdminTab.OVERVIEW,
    val cities: List<String> = emptyList(),
    val popularRoutes: List<Route> = emptyList(),
    val users: List<User> = emptyList(),
    val bookings: List<AdminBookingEntry> = emptyList(),
    val totalRevenueGhs: Double = 0.0,
    val isSeeding: Boolean = false,
    val isBroadcasting: Boolean = false,
    val cancellingBookingId: String? = null,
    val broadcastTitle: String = "",
    val broadcastMessage: String = "",
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
        val users = adminRepository.listAllUsers().getOrDefault(emptyList())
        val bookings = adminRepository.listAllBookings().getOrDefault(emptyList())
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
            totalRevenueGhs = revenue
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
}
