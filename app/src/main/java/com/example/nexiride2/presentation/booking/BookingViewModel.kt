package com.example.nexiride2.presentation.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexiride2.domain.model.*
import com.example.nexiride2.domain.usecase.CreateBookingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookingUiState(
    val selectedSeats: List<Seat> = emptyList(),
    val passengers: List<Passenger> = emptyList(),
    val baggage: BaggageInfo = BaggageInfo(),
    val selectedPaymentMethod: String = "MTN MoMo",
    val isLoading: Boolean = false,
    val booking: Booking? = null,
    val error: String? = null,
    val routeId: String = ""
)

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val createBookingUseCase: CreateBookingUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState = _uiState.asStateFlow()

    fun setRouteId(id: String) { _uiState.value = _uiState.value.copy(routeId = id) }

    fun toggleSeat(seat: Seat) {
        val current = _uiState.value.selectedSeats.toMutableList()
        val existing = current.find { it.id == seat.id }
        if (existing != null) current.remove(existing) else current.add(seat.copy(status = SeatStatus.SELECTED))
        _uiState.value = _uiState.value.copy(selectedSeats = current)
    }

    fun updatePassengers(passengers: List<Passenger>) { _uiState.value = _uiState.value.copy(passengers = passengers) }
    fun updateBaggage(baggage: BaggageInfo) { _uiState.value = _uiState.value.copy(baggage = baggage) }
    fun updatePaymentMethod(method: String) { _uiState.value = _uiState.value.copy(selectedPaymentMethod = method) }

    fun createBooking() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        createBookingUseCase(_uiState.value.routeId, _uiState.value.selectedSeats, _uiState.value.passengers,
            _uiState.value.baggage, _uiState.value.selectedPaymentMethod).fold(
            onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, booking = it) },
            onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        )
    }

    fun reset() { _uiState.value = BookingUiState() }
}
