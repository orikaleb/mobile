package com.example.nexiride2.presentation.mybookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexiride2.domain.model.Booking
import com.example.nexiride2.domain.repository.DownloadedTicket
import com.example.nexiride2.domain.repository.DownloadedTicketRepository
import com.example.nexiride2.domain.usecase.CancelBookingUseCase
import com.example.nexiride2.domain.usecase.GetBookingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyBookingsUiState(
    val isLoading: Boolean = true,
    val upcoming: List<Booking> = emptyList(),
    val past: List<Booking> = emptyList(),
    val cancelled: List<Booking> = emptyList(),
    val selectedBooking: Booking? = null,
    val lastDownloadedTicket: DownloadedTicket? = null,
    val error: String? = null,
    val cancelSuccess: Boolean = false
)

@HiltViewModel
class MyBookingsViewModel @Inject constructor(
    private val getBookings: GetBookingsUseCase,
    private val cancelBooking: CancelBookingUseCase,
    private val downloadedTicketRepository: DownloadedTicketRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyBookingsUiState())
    val uiState = _uiState.asStateFlow()

    val downloadedTickets: StateFlow<Map<String, DownloadedTicket>> =
        downloadedTicketRepository.observeDownloadedTickets()
            .map { list -> list.associateBy { it.bookingId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init { loadBookings() }

    fun loadBookings() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)
        val up = getBookings.getUpcoming().getOrDefault(emptyList())
        val past = getBookings.getPast().getOrDefault(emptyList())
        val cancelled = getBookings.getCancelled().getOrDefault(emptyList())
        _uiState.value = MyBookingsUiState(false, up, past, cancelled)
    }

    fun selectBooking(booking: Booking) { _uiState.value = _uiState.value.copy(selectedBooking = booking) }

    fun cancelBooking(bookingId: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)
        cancelBooking.invoke(bookingId).fold(
            onSuccess = { _uiState.value = _uiState.value.copy(cancelSuccess = true); loadBookings() },
            onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        )
    }

    fun downloadTicketPdf(bookingId: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null, lastDownloadedTicket = null)
        downloadedTicketRepository.downloadTicketPdf(bookingId).fold(
            onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, lastDownloadedTicket = it) },
            onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        )
    }

    suspend fun getDownloadedTicket(bookingId: String) = downloadedTicketRepository.getDownloadedTicket(bookingId)
}
