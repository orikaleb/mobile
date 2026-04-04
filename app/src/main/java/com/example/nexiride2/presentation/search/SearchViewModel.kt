package com.example.nexiride2.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexiride2.domain.model.Route
import com.example.nexiride2.domain.model.Seat
import com.example.nexiride2.domain.model.SeatStatus
import com.example.nexiride2.domain.repository.BusRepository
import com.example.nexiride2.domain.usecase.SearchBusesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

data class SearchUiState(
    val isLoading: Boolean = false,
    val results: List<Route> = emptyList(),
    val filteredResults: List<Route> = emptyList(),
    val selectedRoute: Route? = null,
    val seats: List<Seat> = emptyList(),
    val error: String? = null,
    val origin: String = "",
    val destination: String = "",
    val date: String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()),
    val passengers: Int = 1,
    val cities: List<String> = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val busRepository: BusRepository,
    private val searchUseCase: SearchBusesUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    init { viewModelScope.launch { _uiState.value = _uiState.value.copy(cities = busRepository.getAvailableCities()) } }

    fun updateOrigin(origin: String) { _uiState.value = _uiState.value.copy(origin = origin) }
    fun updateDestination(dest: String) { _uiState.value = _uiState.value.copy(destination = dest) }
    fun updateDate(date: String) { _uiState.value = _uiState.value.copy(date = date) }
    fun updatePassengers(p: Int) { _uiState.value = _uiState.value.copy(passengers = p) }

    fun search() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        searchUseCase(_uiState.value.origin, _uiState.value.destination, _uiState.value.date, _uiState.value.passengers).fold(
            onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, results = it, filteredResults = it) },
            onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        )
    }

    fun searchWithParams(origin: String, destination: String) {
        _uiState.value = _uiState.value.copy(origin = origin, destination = destination)
        search()
    }

    fun applyFilters(priceRange: ClosedFloatingPointRange<Float>, timeFilter: String, company: String) {
        val filtered = _uiState.value.results.filter { route ->
            route.price >= priceRange.start && route.price <= priceRange.endInclusive &&
            (company == "All" || route.bus.companyName == company) &&
            when (timeFilter) {
                "Morning" -> route.departureTime.substringBefore(":").toIntOrNull()?.let { it in 5..11 } ?: true
                "Afternoon" -> route.departureTime.substringBefore(":").toIntOrNull()?.let { it in 12..16 } ?: true
                "Evening" -> route.departureTime.substringBefore(":").toIntOrNull()?.let { it in 17..23 } ?: true
                else -> true
            }
        }
        _uiState.value = _uiState.value.copy(filteredResults = filtered)
    }

    fun selectRoute(routeId: String) = viewModelScope.launch {
        busRepository.getRouteById(routeId).onSuccess { route ->
            _uiState.value = _uiState.value.copy(selectedRoute = route)
        }
    }

    fun loadSeats(routeId: String) = viewModelScope.launch {
        busRepository.getSeatsForRoute(routeId).onSuccess { seats ->
            _uiState.value = _uiState.value.copy(seats = seats)
        }

        // Mock "WebSocket" seat updates: periodically reserve a few seats to simulate real-time changes.
        viewModelScope.launch {
            while (isActive) {
                delay(2_500)
                val current = _uiState.value.seats
                if (current.isEmpty()) continue
                val available = current.filter { it.status == SeatStatus.AVAILABLE }
                if (available.isEmpty()) continue
                val toReserve = available.shuffled().take(Random.nextInt(1, 4))
                val updated = current.map { seat ->
                    if (toReserve.any { it.id == seat.id }) seat.copy(status = SeatStatus.RESERVED) else seat
                }
                _uiState.value = _uiState.value.copy(seats = updated)
            }
        }
    }

    fun getCompanies(): List<String> = _uiState.value.results.map { it.bus.companyName }.distinct()
}
