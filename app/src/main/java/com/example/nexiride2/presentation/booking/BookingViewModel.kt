package com.example.nexiride2.presentation.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexiride2.domain.model.BaggageInfo
import com.example.nexiride2.domain.model.Booking
import com.example.nexiride2.domain.model.Passenger
import com.example.nexiride2.domain.model.Seat
import com.example.nexiride2.domain.model.SeatStatus
import com.example.nexiride2.domain.repository.WalletRepository
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
    val selectedPaymentMethod: String = "In-app balance",
    val walletBalanceGhs: Double? = null,
    /** Total due for the current trip (set from Review screen from route × seats). */
    val tripTotalGhs: Double = 0.0,
    val isLoading: Boolean = false,
    val booking: Booking? = null,
    val error: String? = null,
    val routeId: String = ""
)

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val createBookingUseCase: CreateBookingUseCase,
    private val walletRepository: WalletRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState = _uiState.asStateFlow()

    fun setRouteId(id: String) {
        _uiState.value = _uiState.value.copy(routeId = id)
    }

    fun setTripTotalGhs(total: Double) {
        _uiState.value = _uiState.value.copy(tripTotalGhs = total)
    }

    fun toggleSeat(seat: Seat) {
        val current = _uiState.value.selectedSeats.toMutableList()
        val existing = current.find { it.id == seat.id }
        if (existing != null) current.remove(existing) else current.add(seat.copy(status = SeatStatus.SELECTED))
        _uiState.value = _uiState.value.copy(selectedSeats = current)
    }

    fun updatePassengers(passengers: List<Passenger>) {
        _uiState.value = _uiState.value.copy(passengers = passengers)
    }

    fun updateBaggage(baggage: BaggageInfo) {
        _uiState.value = _uiState.value.copy(baggage = baggage)
    }

    fun refreshWallet() = viewModelScope.launch {
        walletRepository.getBalanceGhs().fold(
            onSuccess = { bal -> _uiState.value = _uiState.value.copy(walletBalanceGhs = bal) },
            onFailure = { _uiState.value = _uiState.value.copy(walletBalanceGhs = null) }
        )
    }

    fun createBooking() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        val total = _uiState.value.tripTotalGhs
        walletRepository.tryDebit(total).fold(
            onFailure = { err ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = err.message)
            },
            onSuccess = {
                createBookingUseCase(
                    _uiState.value.routeId,
                    _uiState.value.selectedSeats,
                    _uiState.value.passengers,
                    _uiState.value.baggage,
                    _uiState.value.selectedPaymentMethod
                ).fold(
                    onSuccess = { booking ->
                        _uiState.value = _uiState.value.copy(isLoading = false, booking = booking)
                        refreshWallet()
                    },
                    onFailure = { err ->
                        walletRepository.topUp(total)
                        refreshWallet()
                        _uiState.value = _uiState.value.copy(isLoading = false, error = err.message)
                    }
                )
            }
        )
    }

    fun reset() {
        _uiState.value = BookingUiState()
    }
}
